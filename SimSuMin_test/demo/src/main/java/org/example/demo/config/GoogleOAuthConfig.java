package org.example.demo.config;

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
import java.util.Collections;

@Configuration
public class GoogleOAuthConfig {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.client-id}")   //google cloud client ID
    private String clientId;

    @Value("${google.client-secret}") //google cloud client 비밀번호
    private String clientSecrets;

    @Value("${google.scopes}") //사용자에게 요청할 권한 범위(일단 readonly만 설정되어있음.)
    private String scope;

    @Bean
    public HttpTransport httpTransport() throws GeneralSecurityException, IOException{
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow(HttpTransport httpTransport) throws IOException {
        String secretsJson = String.format(
                "{\"web\":{\"client_id\":\"%s\",\"client_secret\":\"%s\"}}", clientId, clientSecrets); //클라이언트 ID와 보안 비밀로 JSON 문자열을 동적으로 생성
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new StringReader(secretsJson));// 생성된 JSON 문자열을 GoogleClientSecrets 객체로 로드

        return new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, Collections.singletonList(scope))
                .setAccessType("offline")  // refresh token 발급을 위해 'offline' 설정
                .build();
    }
}
