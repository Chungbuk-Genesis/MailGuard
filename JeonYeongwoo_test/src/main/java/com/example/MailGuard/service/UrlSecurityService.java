package com.example.MailGuard.service;

import com.example.MailGuard.model.UrlValidationResult;
import com.example.MailGuard.repo.BlockedDomainRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

// db 내 url과 비교하여 안전/위험 여부 판별하는 서비스 파일

@Service
public class UrlSecurityService {
	private static final Logger log = LoggerFactory.getLogger(UrlSecurityService.class);
	
    private final BlockedDomainRepository blockedDomainRepository;

    // 위험한 문자열 패턴
    private static final List<String> DANGEROUS_PATTERNS = Arrays.asList(
            "javascript:", "data:", "vbscript:", "file:",
            "<script", "onerror=", "onclick=", "onload="
    );

    // 허용 도메인
    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
            "example.com", "trusted-site.com", "naver.com", "google.com"
    );

    // DB에서 로드될 차단 도메인 목록
    private List<String> blockedDomains = new ArrayList<>();

    public UrlSecurityService(BlockedDomainRepository blockedDomainRepository) {
        this.blockedDomainRepository = blockedDomainRepository;
    }

    // 앱 시작 시 1회 로드
    @PostConstruct
    public void loadBlockedDomains() {
        blockedDomains = blockedDomainRepository.findAllDomains();
        System.out.println("✅ 차단 도메인 로드 완료: " + blockedDomains.size() + "개");
    }

    public boolean isSafeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lowerUrl = url.toLowerCase();

        for (String pattern : DANGEROUS_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                return false;
            }
        }

        try {
            URI uri = new URI(url);
            String rawHost = uri.getHost();
            String host = rawHost == null ? null : rawHost.toLowerCase(Locale.ROOT);
            log.debug("Check URL={} host={}", url, host);

            for (String blocked : blockedDomains) {
                if (blocked != null && host.endsWith(blocked)) {
                    log.info("Blocked by domain list: host={} matched={}", host, blocked);
                    return false;
                }
            }

            return true;

        } catch (URISyntaxException e) {
            log.info("Invalid URL syntax: {}", url);
            return false;
        }
    }

    public UrlValidationResult validateUrl(String url) {
        UrlValidationResult result = new UrlValidationResult();
        result.setOriginalUrl(url);

        if (url == null || url.trim().isEmpty()) {
            result.setSafe(false);
            result.setReason("빈 URL입니다");
            return result;
        }

        String lowerUrl = url.toLowerCase();

        for (String pattern : DANGEROUS_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                result.setSafe(false);
                result.setReason("위험한 패턴 감지: " + pattern);
                return result;
            }
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            result.setHost(host);

            if (host == null) {
                result.setSafe(false);
                result.setReason("유효하지 않은 호스트");
                return result;
            }

            for (String blocked : blockedDomains) {
                if (host.endsWith(blocked)) {
                    result.setSafe(false);
                    result.setReason("차단된 도메인: " + blocked);
                    return result;
                }
            }

            result.setSafe(true);
            result.setReason("안전한 URL");

        } catch (URISyntaxException e) {
            result.setSafe(false);
            result.setReason("잘못된 URL 형식");
        }

        return result;
    }
}
