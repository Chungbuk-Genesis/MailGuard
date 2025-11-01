// ============================================
// 5. src/main/java/com/example/urlsecurity/controller/UrlValidationController.java
// ============================================
package com.example.MailGuard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.MailGuard.model.UrlValidationResult;
import com.example.MailGuard.service.UrlSecurityService;

import org.springframework.http.MediaType;

import java.net.URI;
import java.util.Map;



// http://localhost:8080/api/url/check?url=https://naver.com 같은 형태로 인풋 넣어서 확인 가능
// http://localhost:8080/api/url/redirect?url=https://MaliciousExample.com 같은 형태로 인풋 넣으면 리다이렉션 확인 가능

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
    public ResponseEntity<?> safeRedirect(@RequestParam("url") String url) {
    	if (!urlSecurityService.isSafeUrl(url)) {
            String message = """
                <html>
                <head><meta charset='UTF-8'></head>
                <body>
                    <script>
                        window.onload = function() {
                            alert('%s 은(는) 악성 URL로 확인되어 접속이 차단되었습니다.');
                            window.location.href = '/warning'; // 차단 안내 페이지 등으로 이동
                        };
                    </script>
                </body>
                </html>
                """.formatted(url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);

            return new ResponseEntity<>(message, headers, HttpStatus.BAD_REQUEST);
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
    
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> quickCheck(@RequestParam("url") String url) {
        boolean safe = urlSecurityService.isSafeUrl(url);
        return ResponseEntity.ok(Map.of(
            "url", url,
            "safe", safe,
            "message", safe ? "안전한 URL입니다" : "위험한 URL입니다"
        ));
    }
}
