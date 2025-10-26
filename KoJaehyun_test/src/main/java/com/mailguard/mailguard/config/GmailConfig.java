package com.mailguard.mailguard.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GmailConfig {
    
    // Gmail API 범위
    public static final String GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";
    
    // 애플리케이션 이름
    public static final String APPLICATION_NAME = "MailGuard";
    
    // credentials.json 파일 경로
    public static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    
    // 토큰 저장 디렉토리
    public static final String TOKENS_DIRECTORY_PATH = "tokens";
    
    // 로컬 서버 포트 (OAuth callback용)
    public static final int LOCAL_SERVER_PORT = 8888;
}
