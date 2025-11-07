package com.example.MailGuard.controller;

import com.example.MailGuard.dto.AttachmentDto;
import com.example.MailGuard.dto.EmailDto;
import com.example.MailGuard.service.GmailService;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
// ❌ @RequestMapping("/") 제거 — 루트 스코프 점유 방지
public class GmailController {

    private final AuthorizationCodeFlow flow;
    private final GmailService gmailService;
    private final String redirectUri;

    private static final String USER_ID = "me";
    private static final String CREDENTIAL_SESSION_KEY = "google_credential";

    @Autowired
    public GmailController(
            AuthorizationCodeFlow flow,
            GmailService gmailService,
            @Value("${google.redirect-uri}") String redirectUri) {
        this.flow = flow;
        this.gmailService = gmailService;
        this.redirectUri = redirectUri;
    }

    // Google OAuth2 로그인
    @GetMapping("/login/google")
    public void login(HttpServletResponse response) throws IOException {
        String url = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();
        response.sendRedirect(url);
    }

    // OAuth2 콜백 (⚠️ 콘솔/환경설정의 redirect-uri와 반드시 동일 경로여야 함)
    @GetMapping("/login/oauth2/code/google")
    public void oauthCallback(@RequestParam("code") String code,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        Credential credential = flow.createAndStoreCredential(tokenResponse, USER_ID);

        request.getSession().setAttribute(CREDENTIAL_SESSION_KEY, credential);
        response.sendRedirect("/"); // 로그인 완료 후 메인으로
    }

    // 최신 메일 조회
    @GetMapping("/api/latest-email")
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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 첨부 목록
    @GetMapping("/api/attachments/{messageId}")
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
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 첨부 다운로드
    @GetMapping("/api/attachment/download")
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
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;
            return ResponseEntity.ok()
                    .header("Content-Disposition", contentDisposition)
                    .body(fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 토큰 갱신 헬퍼
    private Credential getRefreshedCredential(HttpSession session) {
        Credential credential = (Credential) session.getAttribute(CREDENTIAL_SESSION_KEY);
        if (credential == null) {
            return null;
        }
        try {
            if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
                if (!credential.refreshToken()) {
                    return null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return credential;
    }
}
