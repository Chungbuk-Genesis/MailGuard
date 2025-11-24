// ============================================
// 7. src/main/java/com/example/urlsecurity/config/WebMvcConfig.java
// ============================================
package com.example.MailGuard.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private UrlSecurityInterceptor urlSecurityInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(urlSecurityInterceptor)
                .addPathPatterns("/**");
    }
}