package com.example.MailGuard.controller;

import com.example.MailGuard.dto.EmailDto;
import com.example.MailGuard.service.NaverImapService;
import jakarta.mail.AuthenticationFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/naver")
@RequiredArgsConstructor
public class NaverMailController {

    private final NaverImapService naverImapService;

    // ✅ 데모용 계정(있으면 사용, 없으면 에러 리턴)
    @Value("${naver.demo.email:}")
    private String demoEmail;

    @Value("${naver.demo.app-password:}")
    private String demoAppPassword;

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchAndAnalyzeNaver(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String appPassword = (String) request.get("appPassword");

            int maxEmails = 10;
            if (request.get("maxEmails") != null) {
                maxEmails = Integer.parseInt(request.get("maxEmails").toString());
            }

            if (email == null || email.isBlank() || appPassword == null || appPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "email 또는 appPassword가 누락되었습니다."
                ));
            }

            log.info("=== Naver 메일 가져오기 + 피싱 분석 시작 === email={}, maxEmails={}", email, maxEmails);

            List<EmailDto> emails = naverImapService.fetchAndAnalyzeEmails(email, appPassword, maxEmails);

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
            response.put("provider", "Naver");
            response.put("totalCount", emails.size());
            response.put("statistics", Map.of(
                    "dangerous", dangerousCount,
                    "suspicious", suspiciousCount,
                    "safe", safeCount
            ));
            response.put("emails", emails);
            response.put("message", "✅ Naver 메일 분석 완료!");

            log.info("=== 분석 완료: 위험 {}, 의심 {}, 안전 {} ===",
                    dangerousCount, suspiciousCount, safeCount);

            return ResponseEntity.ok(response);

        } catch (AuthenticationFailedException e) {
            log.error("❌ Naver 인증 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Naver 인증 실패: " + e.getMessage());
            errorResponse.put("solution",
                    "1. Naver 메일 설정에서 IMAP/SMTP 사용 활성화\n" +
                    "2. 2단계 인증 활성화\n" +
                    "3. 애플리케이션 비밀번호 생성\n" +
                    "4. 생성된 비밀번호를 appPassword로 사용 (공백 제거)");

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("❌ Naver 메일 가져오기 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/dangerous")
    public ResponseEntity<?> getDangerousEmails(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String appPassword = (String) request.get("appPassword");

            int maxEmails = 20;
            if (request.get("maxEmails") != null) {
                maxEmails = Integer.parseInt(request.get("maxEmails").toString());
            }

            if (email == null || email.isBlank() || appPassword == null || appPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "email 또는 appPassword가 누락되었습니다."
                ));
            }

            List<EmailDto> allEmails =
                    naverImapService.fetchAndAnalyzeEmails(email, appPassword, maxEmails);

            List<EmailDto> dangerousEmails = allEmails.stream()
                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel())
                              || "SUSPICIOUS".equals(e.getRiskLevel()))
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("provider", "Naver");
            response.put("totalScanned", allEmails.size());
            response.put("dangerousCount", dangerousEmails.size());
            response.put("emails", dangerousEmails);

            if (dangerousEmails.isEmpty()) {
                response.put("message", "🎉 위험한 메일이 발견되지 않았습니다!");
            } else {
                response.put("message", "⚠️ " + dangerousEmails.size() + " 개의 의심스러운 메일 발견!");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("위험 메일 필터링 실패", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/help")
    public ResponseEntity<?> getHelp() {
        Map<String, Object> help = new HashMap<>();
        help.put("title", "MailGuard Naver IMAP API 사용법");
        help.put("endpoints", List.of(
                Map.of(
                        "method", "POST",
                        "path", "/api/naver/fetch",
                        "description", "Naver 메일 가져오기 + 피싱 분석"
                ),
                Map.of(
                        "method", "POST",
                        "path", "/api/naver/dangerous",
                        "description", "위험한 메일만 필터링"
                ),
                Map.of(
                        "method", "GET",
                        "path", "/api/naver/fetch-demo",
                        "description", "Naver 메일 가져오기 데모 (GET 요청, dev 전용)"
                )
        ));

        help.put("setup", Map.of(
                "step1", "네이버 로그인 → 내정보 → 보안설정",
                "step2", "2단계 인증 활성화",
                "step3", "애플리케이션 비밀번호 생성",
                "step4", "프론트엔드에서 email / appPassword 를 POST 로 전달"
        ));

        return ResponseEntity.ok(help);
    }

    // ============== 새로 정리된 GET 데모 엔드포인트 ==============

    @GetMapping("/fetch-demo")
    public ResponseEntity<?> fetchDemo() {
        try {
            // ✅ application.properties 또는 환경변수에서 주입받은 데모 계정 사용
            if (demoEmail == null || demoEmail.isBlank()
                    || demoAppPassword == null || demoAppPassword.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "데모용 email / app-password 설정이 되어 있지 않습니다.",
                        "hint", "application.properties 에 naver.demo.email / naver.demo.app-password 를 설정하거나, 이 엔드포인트를 삭제하세요."
                ));
            }

            int maxEmails = 5;

            log.info("=== Naver 메일 가져오기 데모 시작 === email={}, maxEmails={}", demoEmail, maxEmails);

            List<EmailDto> emails =
                    naverImapService.fetchAndAnalyzeEmails(demoEmail, demoAppPassword, maxEmails);

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
            response.put("provider", "Naver (데모)");
            response.put("method", "IMAP");
            response.put("authentication", "앱 비밀번호");
            response.put("totalCount", emails.size());
            response.put("statistics", Map.of(
                    "dangerous", dangerousCount,
                    "suspicious", suspiciousCount,
                    "safe", safeCount
            ));
            response.put("emails", emails);
            response.put("message", "✅ Naver 메일 분석 완료!");

            log.info("=== 분석 완료: 위험 {}, 의심 {}, 안전 {} ===",
                    dangerousCount, suspiciousCount, safeCount);

            return ResponseEntity.ok(response);

        } catch (AuthenticationFailedException e) {
            log.error("❌ Naver 인증 실패(데모)", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Naver 인증 실패: " + e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());
            errorResponse.put("solution", Map.of(
                    "step1", "네이버 로그인 → 내정보 → 보안설정",
                    "step2", "2단계 인증 활성화",
                    "step3", "애플리케이션 비밀번호 생성",
                    "step4", "생성된 16자리 비밀번호를 naver.demo.app-password 로 설정",
                    "step5", "IMAP/SMTP 사용 설정 확인"
            ));

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("❌ Naver 메일 가져오기 실패(데모)", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());
            errorResponse.put("solution", "demoEmail / demoAppPassword 설정을 확인하세요.");

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
