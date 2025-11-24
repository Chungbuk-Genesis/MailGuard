package com.example.MailGuard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 유저 정보 중 password 의 해시화 하기 위한 config 파일

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength 기본값 10, 높일수록 느려짐(보안↑, 성능↓)
        return new BCryptPasswordEncoder();
    }
}
