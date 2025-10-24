package com.mailguard.mailguard.controller;

import com.mailguard.mailguard.dto.EmailDto;
import com.mailguard.mailguard.service.GmailOAuthService;
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
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
public class GmailOAuthController {
    
    private final GmailOAuthService gmailOAuthService;
    
    @GetMapping("/fetch")
    public ResponseEntity<?> fetchAndAnalyzeGmail(
            @RequestParam(defaultValue = "10") int maxEmails) {
        try {
            log.info("=== Gmail 메일 가져오기 + 피싱 분석 시작 ===");
            
            List<EmailDto> emails = gmailOAuthService.fetchAndAnalyzeEmails(maxEmails);
            
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
            
            log.info("=== 분석 완료: 위험 {}, 의심 {}, 안전 {} ===", 
                    dangerousCount, suspiciousCount, safeCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Gmail 메일 가져오기 실패", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            if (e.getMessage().contains("credentials.json")) {
                errorResponse.put("solution", 
                        "1. Google Cloud Console에서 credentials.json 다운로드\n" +
                        "2. src/main/resources/ 폴더에 복사\n" +
                        "3. 서버 재시작");
            }
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/dangerous")
    public ResponseEntity<?> getDangerousEmails(
            @RequestParam(defaultValue = "20") int maxEmails) {
        try {
            List<EmailDto> allEmails = gmailOAuthService.fetchAndAnalyzeEmails(maxEmails);
            
            List<EmailDto> dangerousEmails = allEmails.stream()
                    .filter(e -> "DANGEROUS".equals(e.getRiskLevel()) || 
                                 "SUSPICIOUS".equals(e.getRiskLevel()))
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
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
    
    @GetMapping("/auth-status")
    public ResponseEntity<?> checkAuthStatus() {
        try {
            java.io.File tokensDir = new java.io.File("tokens");
            boolean isAuthenticated = tokensDir.exists() && 
                    tokensDir.listFiles() != null && 
                    tokensDir.listFiles().length > 0;
            
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", isAuthenticated);
            
            if (isAuthenticated) {
                response.put("message", "✅ 이미 인증되었습니다.");
            } else {
                response.put("message", "🔐 인증이 필요합니다.");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/revoke")
    public ResponseEntity<?> revokeAuth() {
        try {
            java.io.File tokensDir = new java.io.File("tokens");
            if (tokensDir.exists()) {
                deleteDirectory(tokensDir);
                log.info("🗑️ 인증 토큰 삭제됨");
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "✅ 인증이 해제되었습니다."
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @GetMapping("/help")
    public ResponseEntity<?> getHelp() {
        Map<String, Object> help = new HashMap<>();
        help.put("title", "MailGuard Gmail API 사용법");
        help.put("endpoints", List.of(
                Map.of(
                        "method", "GET",
                        "path", "/api/gmail/fetch?maxEmails=10",
                        "description", "Gmail 메일 가져오기 + 피싱 분석"
                ),
                Map.of(
                        "method", "GET",
                        "path", "/api/gmail/dangerous",
                        "description", "위험한 메일만 필터링"
                ),
                Map.of(
                        "method", "GET",
                        "path", "/api/gmail/auth-status",
                        "description", "인증 상태 확인"
                )
        ));
        
        return ResponseEntity.ok(help);
    }
    
    private void deleteDirectory(java.io.File directory) {
        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
