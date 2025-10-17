// src/main/java/com/example/urlsecurity/service/CsvImportService.java
package com.example.urlsecurity.service;

import com.example.urlsecurity.config.ImporterProperties;
import com.example.urlsecurity.domain.BlockedDomain;
import com.example.urlsecurity.repo.BlockedDomainRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.IDN;              // ✅ 추가
import java.net.URI;              // ✅ 추가
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class CsvImportService {

    private final ImporterProperties props;
    private final BlockedDomainRepository repo;
    private final ResourceLoader resourceLoader;

    public CsvImportService(ImporterProperties props,
                            BlockedDomainRepository repo,
                            ResourceLoader resourceLoader) {
        this.props = props;
        this.repo = repo;
        this.resourceLoader = resourceLoader;
    }

    private Resource resolve(String location) {
        // classpath:/..., file:///..., C:/..., ./data/... 모두 지원
        return resourceLoader.getResource(location);
    }

    /** 파일 목록 전체 Import */
    @Transactional
    public Map<String, Object> importAll() throws Exception {
        Map<String, Object> summary = new LinkedHashMap<>();
        int totalInserted = 0, totalUpdated = 0, totalSkipped = 0;

        for (String file : props.getFiles()) {
            Map<String, Integer> r = importOne(file);
            summary.put(file, r);
            totalInserted += r.getOrDefault("inserted", 0);
            totalUpdated  += r.getOrDefault("updated", 0);
            totalSkipped  += r.getOrDefault("skipped", 0);
        }
        summary.put("totalInserted", totalInserted);
        summary.put("totalUpdated", totalUpdated);
        summary.put("totalSkipped", totalSkipped);
        return summary;
    }

    /** 단일 파일 Import */
    @Transactional
    public Map<String, Integer> importOne(String location) throws Exception {
        Resource resource = resolve(location);
        if (!resource.exists()) {
            System.err.println("[CSV] Not found: " + location + " (resolved: " + resource.getDescription() + ")");
            throw new FileNotFoundException(location);
        }

        CSVFormat format = props.isHasHeader()
                ? CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setDelimiter(props.getDelimiter())
                    .build()
                : CSVFormat.DEFAULT.builder()
                    .setDelimiter(props.getDelimiter())
                    .build();

        int inserted = 0, updated = 0, skipped = 0;

        try (InputStream is = resource.getInputStream();
             BOMInputStream bis = new BOMInputStream(is);
             Reader reader = new InputStreamReader(bis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader);
             CSVParser parser = new CSVParser(br, format)) {

            // ✅ 파일 단위로 한 번만 계산
            String scheme = detectSchemeFromFilename(resource.getFilename());
            String source = resource.getFilename();

            for (CSVRecord rec : parser) {
                String domain = extractDomainFlexible(rec, props.getDomainColumn());
                if (domain.isEmpty()) { skipped++; continue; }

                var existing = repo.findBySchemeAndDomain(scheme, domain).orElse(null);
                if (existing == null) {
                    repo.save(BlockedDomain.builder()
                            .scheme(scheme)
                            .domain(domain)
                            .source(source)
                            .build());
                    inserted++;
                } else {
                    existing.setSource(source);
                    repo.save(existing);
                    updated++;
                }
            }
        }
        return Map.of("inserted", inserted, "updated", updated, "skipped", skipped);
    }

    // ====== 유틸들 ======

    // 헤더 우선 + 흔한 후보명 탐색 + 0번 컬럼 폴백
    private String extractDomainFlexible(CSVRecord rec, String preferredHeader) {
        var headers = rec.getParser().getHeaderNames();
        if (headers != null && !headers.isEmpty()) {
            String hit = headers.stream()
                .filter(h -> h != null && h.trim().equalsIgnoreCase(preferredHeader))
                .findFirst().orElse(null);
            if (hit != null && rec.isMapped(hit)) {
                String v = rec.get(hit);
                String d = toDomain(v);
                if (!d.isEmpty()) return d;
            }
            for (String cand : new String[]{"domain","host","url","address","fqdn"}) {
                String h = headers.stream()
                    .filter(x -> x != null && x.trim().equalsIgnoreCase(cand))
                    .findFirst().orElse(null);
                if (h != null && rec.isMapped(h)) {
                    String v = rec.get(h);
                    String d = toDomain(v);
                    if (!d.isEmpty()) return d;
                }
            }
        }
        if (rec.size() > 0) {
            String v = rec.get(0);
            String d = toDomain(v);
            if (!d.isEmpty()) return d;
        }
        return "";
    }

    // URL/호스트/도메인 문자열 → 도메인 정규화
    private String toDomain(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty() || s.startsWith("#")) return "";

        String candidate = s;
        try {
            URI uri = s.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*") ? URI.create(s) : URI.create("http://" + s);
            if (uri.getHost() != null) candidate = uri.getHost();
        } catch (Exception ignore) {}

        int colon = candidate.indexOf(':');
        if (colon > -1) candidate = candidate.substring(0, colon);

        while (candidate.startsWith("*.") || candidate.startsWith(".")) {
            candidate = candidate.substring(1);
        }

        if (candidate.startsWith("www.") && candidate.length() > 4) {
            candidate = candidate.substring(4);
        }

        try {
            candidate = IDN.toASCII(candidate);
        } catch (Exception ignore) {}

        candidate = candidate.toLowerCase();

        if (!candidate.matches("^[a-z0-9.-]+$")) return "";
        if (!candidate.contains(".")) return "";

        return candidate;
    }

    private String detectSchemeFromFilename(String filePath) {
        String lower = filePath == null ? "" : filePath.toLowerCase();
        if (lower.contains("https")) return "HTTPS";
        if (lower.contains("http"))  return "HTTP";
        return "BOTH";
    }

    private static final Pattern DOMAIN_RE = Pattern.compile("^[a-z0-9.-]+$");
    @SuppressWarnings("unused")
    private boolean isLikelyDomain(String s) {
        return s != null && DOMAIN_RE.matcher(s).matches() && s.contains(".");
    }
}
