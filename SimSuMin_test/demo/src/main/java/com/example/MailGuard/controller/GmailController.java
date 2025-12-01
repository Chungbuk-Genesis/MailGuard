package com.example.MailGuard.controller;

import com.example.MailGuard.dto.AttachmentDto;
import com.example.MailGuard.dto.EmailDto;
import com.example.MailGuard.dto.ReportDto;
import com.example.MailGuard.service.GmailService;
import com.example.MailGuard.service.VirusTotalService;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.example.MailGuard.service.GmailOAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/gmail")
@CrossOrigin(origins = "*")
public class GmailController {

    private final AuthorizationCodeFlow flow;
    private final GmailService gmailService;
    private final GmailOAuthService gmailOAuthService;
    private final VirusTotalService virusTotalService;
    private final String redirectUri;

    private static final String USER_ID = "me";
    private static final String CREDENTIAL_SESSION_KEY = "google_credential";

    @Autowired
    public GmailController(
            AuthorizationCodeFlow flow,
            GmailService gmailService,
            GmailOAuthService gmailOAuthService,
            VirusTotalService virusTotalService,
            @Value("${google.redirect-uri}") String redirectUri) {
        this.flow = flow;
        this.gmailService = gmailService;
        this.gmailOAuthService = gmailOAuthService;
        this.virusTotalService = virusTotalService;
        this.redirectUri = redirectUri;
    }

    // ==================== OAuth 인증 ====================

    @GetMapping("/login")
    public void login(HttpServletResponse response) throws IOException {
        log.info("🔐 Google OAuth 로그인 시작");
        String url = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();
        response.sendRedirect(url);
    }

    @GetMapping("/oauth2/callback")
    public void oauthCallback(@RequestParam("code") String code,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        log.info("✅ OAuth 콜백 수신");
        TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        Credential credential = flow.createAndStoreCredential(tokenResponse, USER_ID);

        request.getSession().setAttribute(CREDENTIAL_SESSION_KEY, credential);
        log.info("✅ 인증 완료 - 세션에 저장됨");
        response.sendRedirect("/");
    }

    @GetMapping("/auth-status")
    public ResponseEntity<?> checkAuthStatus(HttpSession session) {
        Credential credential = (Credential) session.getAttribute(CREDENTIAL_SESSION_KEY);
        boolean isAuthenticated = (credential != null);

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", isAuthenticated);
        response.put("message", isAuthenticated ? "✅ 인증됨" : "🔐 인증 필요");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/revoke")
    public ResponseEntity<?> revokeAuth(HttpSession session) {
        session.removeAttribute(CREDENTIAL_SESSION_KEY);
        log.info("🗑️ 인증 토큰 삭제됨");

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "✅ 인증이 해제되었습니다."
        ));
    }

    // ==================== 메일 조회 ====================

    @GetMapping("/fetch-demo")
    public ResponseEntity<?> fetchGmailDemo(HttpSession session) {
        Credential credential = getRefreshedCredential(session);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "인증이 필요합니다."));
        }

        try {
            log.info("🚀 Gmail 메일 가져오기 시작 (Demo - 10개)");

            // ✅ 소문자 변수명 사용
            List<EmailDto> emails = gmailOAuthService.fetchAndAnalyzeEmails(credential, 5);

            long dangerousCount = emails.stream()
                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel()))
                    .count();
            long suspiciousCount = emails.stream()
                    .filter(e -> "SUSPICIOUS".equals(e.getRiskLevel()))
                    .count();
            long safeCount = emails.stream()
                    .filter(e -> "SAFE".equals(e.getRiskLevel()))
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", emails.size());
            response.put("statistics", Map.of(
                    "dangerous", dangerousCount,
                    "suspicious", suspiciousCount,
                    "safe", safeCount
            ));
            response.put("emails", emails);

            log.info("✅ Gmail 메일 {}개 분석 완료", emails.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Gmail 메일 가져오기 실패: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "Gmail 연동 실패: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/fetch")
    public ResponseEntity<?> fetchAndAnalyzeGmail(
            @RequestParam(defaultValue = "10") int maxEmails,
            HttpSession session) {
        Credential credential = getRefreshedCredential(session);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "인증이 필요합니다."));
        }

        try {
            log.info("=== Gmail 메일 가져오기 + 피싱 분석 시작 ===");

            // ✅ 소문자 변수명 사용
            List<EmailDto> emails = gmailOAuthService.fetchAndAnalyzeEmails(credential, maxEmails);

            long dangerousCount = emails.stream()
                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel()))
                    .count();
            long suspiciousCount = emails.stream()
                    .filter(e -> "SUSPICIOUS".equals(e.getRiskLevel()))
                    .count();
            long safeCount = emails.stream()
                    .filter(e -> "SAFE".equals(e.getRiskLevel()))
                    .count();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalCount", emails.size());
            response.put("statistics", Map.of(
                    "dangerous", dangerousCount,
                    "suspicious", suspiciousCount,
                    "safe", safeCount
            ));
            response.put("emails", emails);
            response.put("message", "✅ Gmail 메일 분석 완료!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Gmail 메일 가져오기 실패", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/dangerous")
    public ResponseEntity<?> getDangerousEmails(
            @RequestParam(defaultValue = "20") int maxEmails,
            HttpSession session) {
        Credential credential = getRefreshedCredential(session);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "인증이 필요합니다."));
        }

        try {
            // ✅ 소문자 변수명 사용
            List<EmailDto> allEmails = gmailOAuthService.fetchAndAnalyzeEmails(credential, maxEmails);
            List<EmailDto> dangerousEmails = allEmails.stream()
                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel()) ||
                            "SUSPICIOUS".equals(e.getRiskLevel()))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalScanned", allEmails.size());
            response.put("dangerousCount", dangerousEmails.size());
            response.put("emails", dangerousEmails);
            response.put("message", dangerousEmails.isEmpty()
                    ? "🎉 위험한 메일이 발견되지 않았습니다!"
                    : "⚠️ " + dangerousEmails.size() + "개의 의심스러운 메일 발견!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("위험 메일 필터링 실패", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== 메일 상세 조회 ====================

    @GetMapping("/latest-email")
    public ResponseEntity<EmailDto> getLatestEmail(HttpSession session) {
        Credential credential = getRefreshedCredential(session);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            EmailDto email = gmailService.getLatestEmail(credential);
            if (email == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(email);
        } catch (IOException e) {
            log.error("최신 메일 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== 첨부파일 관련 ====================

    @GetMapping("/attachments/{messageId}")
    public ResponseEntity<List<AttachmentDto>> getAttachmentsList(
            @PathVariable("messageId") String messageId,
            HttpSession session) {
        Credential credential = getRefreshedCredential(session);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            List<AttachmentDto> attachments = gmailService.listAttachments(credential, messageId);
            return ResponseEntity.ok(attachments);
        } catch (IOException e) {
            log.error("첨부파일 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attachment/scan")
    public ResponseEntity<List<ReportDto>> scanAttachment(
            @RequestParam("msgId") String messageId,
            @RequestParam("attId") String attachmentId,
            @RequestParam("filename") String filename,
            @RequestParam(value = "password", required = false) String password,
            HttpSession session) {

        Credential credential = getRefreshedCredential(session);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            byte[] fileBytes = gmailService.getAttachmentBytes(credential, messageId, attachmentId);
            List<ReportDto> reports = new ArrayList<>();

            if (filename != null && filename.toLowerCase().endsWith(".zip")) {
                reports.addAll(scanZipFileContents(fileBytes, password));
            } else {
                reports.add(scanSingleFile(fileBytes, filename));
            }

            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            log.error("첨부파일 스캔 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attachment/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @RequestParam("msgId") String messageId,
            @RequestParam("attId") String attachmentId,
            @RequestParam("filename") String filename,
            HttpSession session) {
        Credential credential = getRefreshedCredential(session);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            byte[] fileBytes = gmailService.getAttachmentBytes(credential, messageId, attachmentId);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;

            return ResponseEntity.ok()
                    .header("Content-Disposition", contentDisposition)
                    .body(fileBytes);
        } catch (IOException e) {
            log.error("첨부파일 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== 도움말 ====================

    @GetMapping("/help")
    public ResponseEntity<?> getHelp() {
        Map<String, Object> help = new HashMap<>();
        help.put("title", "MailGuard Gmail API 통합 사용법");
        help.put("endpoints", List.of(
                Map.of("method", "GET", "path", "/api/gmail/login",
                        "description", "Google OAuth 로그인"),
                Map.of("method", "GET", "path", "/api/gmail/auth-status",
                        "description", "인증 상태 확인"),
                Map.of("method", "GET", "path", "/api/gmail/fetch-demo",
                        "description", "Gmail 메일 3개 가져오기 (Demo)"),
                Map.of("method", "GET", "path", "/api/gmail/fetch?maxEmails=10",
                        "description", "Gmail 메일 가져오기 + 피싱 분석"),
                Map.of("method", "GET", "path", "/api/gmail/dangerous",
                        "description", "위험한 메일만 필터링"),
                Map.of("method", "GET", "path", "/api/gmail/latest-email",
                        "description", "최신 메일 1개 조회"),
                Map.of("method", "GET", "path", "/api/gmail/attachments/{messageId}",
                        "description", "첨부파일 목록"),
                Map.of("method", "GET", "path", "/api/gmail/attachment/scan",
                        "description", "첨부파일 VirusTotal 스캔"),
                Map.of("method", "DELETE", "path", "/api/gmail/revoke",
                        "description", "인증 해제")
        ));
        return ResponseEntity.ok(help);
    }

    // ==================== Private 헬퍼 메서드 ====================

    private Credential getRefreshedCredential(HttpSession session) {
        Credential credential = (Credential) session.getAttribute(CREDENTIAL_SESSION_KEY);
        if (credential == null) {
            return null;
        }
        try {
            if (credential.getExpiresInSeconds() != null &&
                    credential.getExpiresInSeconds() <= 60) {
                if (!credential.refreshToken()) {
                    return null;
                }
            }
        } catch (IOException e) {
            log.error("토큰 갱신 실패", e);
            return null;
        }
        return credential;
    }

    private List<ReportDto> scanZipFileContents(byte[] zipBytes, String password)
            throws IOException, NoSuchAlgorithmException {
        List<ReportDto> reports = new ArrayList<>();
        File tempFile = null;

        try {
            tempFile = Files.createTempFile("temp-zip-", ".zip").toFile();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(zipBytes);
            }

            ZipFile zipFile = new ZipFile(tempFile);

            if (zipFile.isEncrypted()) {
                if (password == null || password.isEmpty()) {
                    reports.add(createSpecialReport("암호 필요 (Password Required)", null));
                    return reports;
                }
                zipFile.setPassword(password.toCharArray());
            }

            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            for (FileHeader header : fileHeaders) {
                if (!header.isDirectory()) {
                    try (InputStream is = zipFile.getInputStream(header)) {
                        byte[] entryBytes = is.readAllBytes();
                        ReportDto report = scanSingleFile(entryBytes, header.getFileName());
                        reports.add(report);
                    }
                }
            }
        } catch (ZipException e) {
            reports.clear();
            reports.add(createSpecialReport("암호 틀림 / Zip 파일 손상", null));
            return reports;
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        return reports;
    }

    private ReportDto scanSingleFile(byte[] fileBytes, String filename)
            throws NoSuchAlgorithmException {
        try {
            String sha256Hash = calculateSHA256(fileBytes);
            ReportDto report = virusTotalService.getFileReport(sha256Hash).block();

            String vtName = report.getMeaningfulName();
            if (vtName != null && !vtName.isEmpty() && !vtName.equals(filename)) {
                report.setMeaningfulName(filename + " (as: " + vtName + ")");
            } else {
                report.setMeaningfulName(filename);
            }
            return report;

        } catch (WebClientResponseException.NotFound e) {
            String sha256 = calculateSHA256(fileBytes);
            ReportDto notFoundReport = createSpecialReport("Report Not Found", sha256);
            notFoundReport.setMeaningfulName(filename);
            return notFoundReport;
        }
    }

    private ReportDto createSpecialReport(String status, String sha256) {
        ReportDto report = new ReportDto();
        report.setSHA256(sha256);
        report.setMeaningfulName(status);
        report.setLastAnalysisDate("N/A");
        report.setMalicious(0);
        report.setSuspicious(0);
        report.setHarmless(0);
        report.setUndetected(0);
        return report;
    }

    private String calculateSHA256(byte[] fileBytes) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodehash = digest.digest(fileBytes);
        return bytesToHex(encodehash);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}