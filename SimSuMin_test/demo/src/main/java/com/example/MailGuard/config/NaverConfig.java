package com.example.MailGuard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NaverConfig {

    @Value("${spring.mail.username}")
    public String naverUser;

    @Value("${spring.mail.password}")
    public String naverPassword;
}
