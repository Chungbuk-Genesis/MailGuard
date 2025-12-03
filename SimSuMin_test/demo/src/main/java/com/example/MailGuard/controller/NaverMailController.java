//package com.example.MailGuard.controller;
//import com.example.MailGuard.dto.EmailDto;
//import com.example.MailGuard.service.NaverImapService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/naver")
//@RequiredArgsConstructor
//public class NaverMailController {
//
//    private final NaverImapService naverImapService;
//
//    @PostMapping("/fetch")
//    public ResponseEntity<?> fetchAndAnalyzeNaver(@RequestBody Map<String, Object> request) {
//        try {
//            String email = (String) request.get("email");
//            String appPassword = (String) request.get("appPassword");
//            int maxEmails = request.get("maxEmails") != null ?
//                    (Integer) request.get("maxEmails") : 10;
//
//            log.info("=== Naver 메일 가져오기 + 피싱 분석 시작 ===");
//
//            List<EmailDto> emails = naverImapService.fetchAndAnalyzeEmails(email, appPassword, maxEmails);
//
//            long dangerousCount = emails.stream()
//                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel()))
//                    .count();
//            long suspiciousCount = emails.stream()
//                    .filter(e -> "SUSPICIOUS".equals(e.getRiskLevel()))
//                    .count();
//            long safeCount = emails.stream()
//                    .filter(e -> "SAFE".equals(e.getRiskLevel()))
//                    .count();
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("provider", "Naver");
//            response.put("totalCount", emails.size());
//            response.put("statistics", Map.of(
//                    "dangerous", dangerousCount,
//                    "suspicious", suspiciousCount,
//                    "safe", safeCount
//            ));
//            response.put("emails", emails);
//            response.put("message", "✅ Naver 메일 분석 완료!");
//
//            log.info("=== 분석 완료: 위험 {}, 의심 {}, 안전 {} ===",
//                    dangerousCount, suspiciousCount, safeCount);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("❌ Naver 메일 가져오기 실패", e);
//
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("success", false);
//            errorResponse.put("error", e.getMessage());
//
//            if (e.getMessage().contains("AuthenticationFailedException")) {
//                errorResponse.put("solution",
//                        "1. Naver 메일 설정에서 IMAP/SMTP 사용 활성화\n" +
//                        "2. 2단계 인증 활성화\n" +
//                        "3. 애플리케이션 비밀번호 생성\n" +
//                        "4. 생성된 비밀번호 사용 (공백 제거)");
//            }
//
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//    }
//
//    @PostMapping("/dangerous")
//    public ResponseEntity<?> getDangerousEmails(@RequestBody Map<String, Object> request) {
//        try {
//            String email = (String) request.get("email");
//            String appPassword = (String) request.get("appPassword");
//            int maxEmails = request.get("maxEmails") != null ?
//                    (Integer) request.get("maxEmails") : 20;
//
//            List<EmailDto> allEmails = naverImapService.fetchAndAnalyzeEmails(email, appPassword, maxEmails);
//
//            List<EmailDto> dangerousEmails = allEmails.stream()
//                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel()) ||
//                                 "SUSPICIOUS".equals(e.getRiskLevel()))
//                    .collect(Collectors.toList());
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("provider", "Naver");
//            response.put("totalScanned", allEmails.size());
//            response.put("dangerousCount", dangerousEmails.size());
//            response.put("emails", dangerousEmails);
//
//            if (dangerousEmails.isEmpty()) {
//                response.put("message", "🎉 위험한 메일이 발견되지 않았습니다!");
//            } else {
//                response.put("message", "⚠️ " + dangerousEmails.size() + " 개의 의심스러운 메일 발견!");
//            }
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("위험 메일 필터링 실패", e);
//            return ResponseEntity.badRequest()
//                    .body(Map.of("success", false, "error", e.getMessage()));
//        }
//    }
//
//    @GetMapping("/help")
//    public ResponseEntity<?> getHelp() {
//        Map<String, Object> help = new HashMap<>();
//        help.put("title", "MailGuard Naver IMAP API 사용법");
//        help.put("endpoints", List.of(
//                Map.of(
//                        "method", "POST",
//                        "path", "/api/naver/fetch",
//                        "description", "Naver 메일 가져오기 + 피싱 분석"
//                ),
//                Map.of(
//                        "method", "POST",
//                        "path", "/api/naver/dangerous",
//                        "description", "위험한 메일만 필터링"
//                ),
//                Map.of(
//                        "method", "GET",
//                        "path", "/api/naver/fetch-demo",
//                        "description", "Naver 메일 가져오기 데모 (GET 요청)"
//                )
//        ));
//
//        help.put("setup", Map.of(
//                "step1", "네이버 로그인 → 내정보 → 보안설정",
//                "step2", "2단계 인증 활성화",
//                "step3", "애플리케이션 비밀번호 생성",
//                "step4", "코드에 이메일과 앱 비밀번호 입력"
//        ));
//
//        return ResponseEntity.ok(help);
//    }
//
//    // ============== 새로 추가된 GET 엔드포인트 ==============
//
//    @GetMapping("/fetch-demo")
//    public ResponseEntity<?> fetchDemo() {
//        try {
//            // ⚠️ 여기에 본인의 네이버 계정 정보를 입력하세요!
//            String email = "albert0827@naver.com";        // ← 수정 필요!
//            String appPassword = "P3V344CC7FRS";             // ← 수정 필요!
//            int maxEmails = 5;
//
//            log.info("=== Naver 메일 가져오기 데모 시작 ===");
//            log.info("Email: {}", email);
//
//            List<EmailDto> emails = naverImapService.fetchAndAnalyzeEmails(email, appPassword, maxEmails);
//
//            long dangerousCount = emails.stream()
//                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel()))
//                    .count();
//            long suspiciousCount = emails.stream()
//                    .filter(e -> "SUSPICIOUS".equals(e.getRiskLevel()))
//                    .count();
//            long safeCount = emails.stream()
//                    .filter(e -> "SAFE".equals(e.getRiskLevel()))
//                    .count();
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("provider", "Naver (데모)");
//            response.put("method", "IMAP");
//            response.put("authentication", "앱 비밀번호");
//            response.put("totalCount", emails.size());
//            response.put("statistics", Map.of(
//                            "dangerous", dangerousCount,
//                            "suspicious", suspiciousCount,
//                    "safe", safeCount
//            ));
//            response.put("emails", emails);
//            response.put("message", "✅ Naver 메일 분석 완료!");
//
//            log.info("=== 분석 완료: 위험 {}, 의심 {}, 안전 {} ===",
//                    dangerousCount, suspiciousCount, safeCount);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("❌ Naver 메일 가져오기 실패", e);
//
//            Map<String, Object> errorResponse = new HashMap<>();
//            errorResponse.put("success", false);
//            errorResponse.put("error", e.getMessage());
//            errorResponse.put("errorType", e.getClass().getSimpleName());
//
//            if (e.getMessage() != null && e.getMessage().contains("Authentication")) {
//                errorResponse.put("solution", Map.of(
//                        "step1", "네이버 로그인 → 내정보 → 보안설정",
//                        "step2", "2단계 인증 활성화",
//                        "step3", "애플리케이션 비밀번호 생성",
//                        "step4", "생성된 16자리 비밀번호를 코드에 입력 (공백 제거)",
//                        "step5", "IMAP/SMTP 사용 설정 확인"
//                ));
//            } else {
//                errorResponse.put("solution", "코드의 email과 appPassword를 확인하세요.");
//            }
//
//            return ResponseEntity.badRequest().body(errorResponse);
//        }
//    }
//}

package com.example.MailGuard.controller;

import com.example.MailGuard.config.NaverConfig;
import com.example.MailGuard.dto.AttachmentDto;
import com.example.MailGuard.dto.EmailDto;
import com.example.MailGuard.dto.ReportDto;
import com.example.MailGuard.service.NaverImapService;
import com.example.MailGuard.service.VirusTotalService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/naver")
@RequiredArgsConstructor
public class NaverMailController {

    private final NaverImapService naverImapService;
    private final VirusTotalService virusTotalService;
    private final NaverConfig naverConfig;

    // 세션 키 상수
    private static final String NAVER_EMAIL_SESSION_KEY = "naver_email";
    private static final String NAVER_PASSWORD_SESSION_KEY = "naver_app_password";


    @PostMapping("/fetch")
    public ResponseEntity<?> fetchAndAnalyzeNaver(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            String email = (String) request.get("email");
            String appPassword = (String) request.get("appPassword");
            int maxEmails = request.get("maxEmails") != null ? (Integer) request.get("maxEmails") : 10;

            // 세션에 자격증명 저장
            session.setAttribute(NAVER_EMAIL_SESSION_KEY, email);
            session.setAttribute(NAVER_PASSWORD_SESSION_KEY, appPassword);

            log.info("=== Naver 메일 가져오기 + 피싱 분석 시작 ===");

            List<EmailDto> emails = naverImapService.fetchAndAnalyzeEmails(email, appPassword, maxEmails);

            return ResponseEntity.ok(createResponse(emails));

        } catch (Exception e) {
            log.error("❌ Naver 메일 가져오기 실패", e);
            return createErrorResponse(e);
        }
    }

    @GetMapping("/fetch-demo")
    public ResponseEntity<?> fetchDemo(HttpSession session) {
        try {
            // ⚠️ [설정 필요] 본인의 네이버 계정 정보 입력
            String email = naverConfig.naverUser;
            String appPassword = naverConfig.naverPassword;
            int maxEmails = 5;

            session.setAttribute(NAVER_EMAIL_SESSION_KEY, email);
            session.setAttribute(NAVER_PASSWORD_SESSION_KEY, appPassword);

            log.info("=== Naver 메일 가져오기 데모 시작 (계정: {}) ===", email);

            List<EmailDto> emails = naverImapService.fetchAndAnalyzeEmails(email, appPassword, maxEmails);

            return ResponseEntity.ok(createResponse(emails));

        } catch (Exception e) {
            log.error("❌ Naver 데모 실행 실패", e);
            return createErrorResponse(e);
        }
    }

    @GetMapping("/attachments/{messageId}")
    public ResponseEntity<List<AttachmentDto>> getAttachmentsList(
            @PathVariable("messageId") String messageId,
            HttpSession session) {

        String email = (String) session.getAttribute(NAVER_EMAIL_SESSION_KEY);
        String password = (String) session.getAttribute(NAVER_PASSWORD_SESSION_KEY);

        if (email == null || password == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<AttachmentDto> attachments = naverImapService.getAttachmentList(email, password, messageId);
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            log.error("첨부파일 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attachment/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @RequestParam("msgId") String messageId,
            @RequestParam("attId") String attachmentId,
            @RequestParam("filename") String filename,
            HttpSession session) {

        String email = (String) session.getAttribute(NAVER_EMAIL_SESSION_KEY);
        String password = (String) session.getAttribute(NAVER_PASSWORD_SESSION_KEY);

        if (email == null || password == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            byte[] fileBytes = naverImapService.getAttachmentBytes(email, password, messageId, filename);

            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;

            return ResponseEntity.ok()
                    .header("Content-Disposition", contentDisposition)
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("첨부파일 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/attachment/scan")
    public ResponseEntity<List<ReportDto>> scanAttachment(
            @RequestParam("msgId") String messageId,
            @RequestParam("attId") String attachmentId,
            @RequestParam("filename") String filename,
            @RequestParam(value = "password", required = false) String zipPassword,
            HttpSession session) {

        String email = (String) session.getAttribute(NAVER_EMAIL_SESSION_KEY);
        String password = (String) session.getAttribute(NAVER_PASSWORD_SESSION_KEY);

        if (email == null || password == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            byte[] fileBytes = naverImapService.getAttachmentBytes(email, password, messageId, filename);
            List<ReportDto> reports = new ArrayList<>();

            if (filename != null && filename.toLowerCase().endsWith(".zip")) {
                reports.addAll(scanZipFileContents(fileBytes, zipPassword));
            } else {
                reports.add(scanSingleFile(fileBytes, filename));
            }

            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            log.error("첨부파일 스캔 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<ReportDto> scanZipFileContents(byte[] zipBytes, String password)
            throws IOException, NoSuchAlgorithmException {
        List<ReportDto> reports = new ArrayList<>();
        File tempFile = null;

        try {
            tempFile = Files.createTempFile("temp-naver-zip-", ".zip").toFile();
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
            if (tempFile != null) tempFile.delete();
        }
        return reports;
    }

    private ReportDto scanSingleFile(byte[] fileBytes, String filename) throws NoSuchAlgorithmException {
        String sha256Hash = calculateSHA256(fileBytes);
        try {
            // 1. 기존 리포트 조회 시도
            ReportDto report = virusTotalService.getFileReport(sha256Hash).block();

            String vtName = report.getMeaningfulName();
            if (vtName != null && !vtName.isEmpty() && !vtName.equals(filename)) {
                report.setMeaningfulName(filename + " (as: " + vtName + ")");
            } else {
                report.setMeaningfulName(filename);
            }
            return report;

        } catch (WebClientResponseException.NotFound e) {
            // 2. 리포트가 없으면(404) 'Report Not Found' 반환 (업로드 요청 X)
            log.info("🔍 VirusTotal 리포트 없음: {}", filename);
            ReportDto notFoundReport = createSpecialReport("Report Not Found", sha256Hash);
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
        byte[] hash = digest.digest(fileBytes);
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private Map<String, Object> createResponse(List<EmailDto> emails) {
        long dangerousCount = emails.stream().filter(e -> "DANGEROUS".equals(e.getRiskLevel())).count();
        long suspiciousCount = emails.stream().filter(e -> "SUSPICIOUS".equals(e.getRiskLevel())).count();
        long safeCount = emails.stream().filter(e -> "SAFE".equals(e.getRiskLevel())).count();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("provider", "Naver");
        response.put("totalCount", emails.size());
        response.put("statistics", Map.of("dangerous", dangerousCount, "suspicious", suspiciousCount, "safe", safeCount));
        response.put("emails", emails);
        return response;
    }

    private ResponseEntity<?> createErrorResponse(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", e.getMessage());
        if (e.getMessage() != null && e.getMessage().contains("Authentication")) {
            errorResponse.put("solution", "네이버 2단계 인증 및 앱 비밀번호 설정을 확인하세요.");
        }
        return ResponseEntity.badRequest().body(errorResponse);
    }
}