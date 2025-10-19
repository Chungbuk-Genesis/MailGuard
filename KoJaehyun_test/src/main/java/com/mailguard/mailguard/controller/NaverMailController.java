package com.mailguard.mailguard.controller;

import com.mailguard.mailguard.dto.EmailDto;
import com.mailguard.mailguard.service.NaverImapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    @PostMapping("/fetch")
    public ResponseEntity<?> fetchAndAnalyzeNaver(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String appPassword = (String) request.get("appPassword");
            int maxEmails = request.get("maxEmails") != null ? 
                    (Integer) request.get("maxEmails") : 10;
            
            log.info("=== Naver 메일 가져오기 + 피싱 분석 시작 ===");
            
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
            
        } catch (Exception e) {
            log.error("❌ Naver 메일 가져오기 실패", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            if (e.getMessage().contains("AuthenticationFailedException")) {
                errorResponse.put("solution", 
                        "1. Naver 메일 설정에서 IMAP/SMTP 사용 활성화\n" +
                        "2. 2단계 인증 활성화\n" +
                        "3. 애플리케이션 비밀번호 생성\n" +
                        "4. 생성된 비밀번호 사용 (공백 제거)");
            }
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping("/dangerous")
    public ResponseEntity<?> getDangerousEmails(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String appPassword = (String) request.get("appPassword");
            int maxEmails = request.get("maxEmails") != null ? 
                    (Integer) request.get("maxEmails") : 20;
            
            List<EmailDto> allEmails = naverImapService.fetchAndAnalyzeEmails(email, appPassword, maxEmails);
            
            List<EmailDto> dangerousEmails = allEmails.stream()
                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel()) || 
                                 "SUSPICIOUS".equals(e.getRiskLevel()))
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
                )
        ));
        
        return ResponseEntity.ok(help);
    }
}
