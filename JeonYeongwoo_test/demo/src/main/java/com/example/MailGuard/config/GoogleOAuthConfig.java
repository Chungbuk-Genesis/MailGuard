package com.example.MailGuard.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
public class GoogleOAuthConfig {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.client-secret}")
    private String clientSecret;

    // "a,b,c" 형식이면 List<String>으로 자동 변환됨
    @Value("#{'${google.scopes}'.split('\\s*,\\s*')}")
    private List<String> scopes;

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow(HttpTransport httpTransport) throws IOException {
        // 로컬 OAuth(8888)로 돌릴 때는 installed 타입이 일반적
        String secretsJson = """
            {
              "installed": {
                "client_id": "%s",
                "client_secret": "%s",
                "redirect_uris": ["http://localhost:8888/Callback","http://127.0.0.1:8888/Callback"],
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token"
              }
            }
            """.formatted(clientId, clientSecret);

        GoogleClientSecrets secrets = GoogleClientSecrets.load(JSON_FACTORY, new StringReader(secretsJson));

        return new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, secrets, scopes)
                .setAccessType("offline")   // refresh token 발급
                .setApprovalPrompt("force") // 필요 시 동의 강제(옵션)
                .build();
    }
}
