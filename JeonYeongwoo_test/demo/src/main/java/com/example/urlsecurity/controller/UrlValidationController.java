// ============================================
// 5. src/main/java/com/example/urlsecurity/controller/UrlValidationController.java
// ============================================
package com.example.urlsecurity.controller;

import com.example.urlsecurity.model.UrlValidationResult;
import com.example.urlsecurity.service.UrlSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

// http://localhost:8080/api/url/check?url=https://naver.com 같은 형태로 인풋 넣어서 확인 가능

@RestController
@RequestMapping("/api/url")
public class UrlValidationController {
    
    @Autowired
    private UrlSecurityService urlSecurityService;
    
    @PostMapping("/validate")
    public ResponseEntity<UrlValidationResult> validateUrl(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        UrlValidationResult result = urlSecurityService.validateUrl(url);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/redirect")
    public ResponseEntity<?> safeRedirect(@RequestParam String url) {
        if (!urlSecurityService.isSafeUrl(url)) {
            return ResponseEntity.badRequest()
                    .body("차단된 URL입니다: " + url);
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
    
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> quickCheck(@RequestParam String url) {
        boolean safe = urlSecurityService.isSafeUrl(url);
        return ResponseEntity.ok(Map.of(
            "url", url,
            "safe", safe,
            "message", safe ? "안전한 URL입니다" : "위험한 URL입니다"
        ));
    }
}