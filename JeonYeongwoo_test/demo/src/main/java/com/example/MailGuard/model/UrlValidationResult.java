// ============================================
// 4. src/main/java/com/example/urlsecurity/model/UrlValidationResult.java
// ============================================
package com.example.MailGuard.model;

import lombok.Data;

@Data
public class UrlValidationResult {
    private String originalUrl;
    private String host;
    private boolean safe;
    private String reason;
}