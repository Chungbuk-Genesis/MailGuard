package com.example.MailGuard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mail")
public class MailController {
    
    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "MailGuard API");
        response.put("version", "1.0.0-PROTOTYPE");
        response.put("status", "running");
        response.put("message", "MailGuard 서버 실행 중! 🚀");
        response.put("description", "이메일 피싱/악성코드 탐지 시스템");
        
        response.put("endpoints", Map.of(
                "Gmail APIs", List.of(
                        "GET  /api/gmail/fetch?maxEmails=10 - Gmail 메일 가져오기 + 분석",
                        "GET  /api/gmail/dangerous - 위험 메일만 필터링",
                        "GET  /api/gmail/auth-status - 인증 상태 확인",
                        "DELETE /api/gmail/revoke - 인증 해제",
                        "GET  /api/gmail/help - 도움말"
                ),
                "Naver APIs", List.of(
                        "POST /api/naver/fetch - Naver 메일 가져오기 + 분석",
                        "POST /api/naver/dangerous - 위험 메일만 필터링",
                        "GET  /api/naver/help - 도움말"
                )
        ));
        
        response.put("team", Map.of(
                "project", "MailGuard",
                "members", List.of("고재현", "심수민", "전영우")
        ));
        
        return response;
    }
    
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "테스트 성공! ✅");
        response.put("java_version", System.getProperty("java.version"));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "MailGuard");
        health.put("timestamp", System.currentTimeMillis());
        
        // 메모리 정보
        Runtime runtime = Runtime.getRuntime();
        health.put("memory", Map.of(
                "total", runtime.totalMemory() / (1024 * 1024) + " MB",
                "free", runtime.freeMemory() / (1024 * 1024) + " MB",
                "used", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + " MB"
        ));
        
        return health;
    }
}
