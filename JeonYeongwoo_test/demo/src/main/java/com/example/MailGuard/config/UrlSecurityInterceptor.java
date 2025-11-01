// ============================================
// 6. src/main/java/com/example/urlsecurity/config/UrlSecurityInterceptor.java
// ============================================
package com.example.MailGuard.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.MailGuard.service.UrlSecurityService;

@Component
public class UrlSecurityInterceptor implements HandlerInterceptor {
    
    @Autowired
    private UrlSecurityService urlSecurityService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) throws Exception {
        
        String redirectUrl = request.getParameter("redirect");
        
        if (redirectUrl != null) {
            if (!urlSecurityService.isSafeUrl(redirectUrl)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain; charset=UTF-8");
                response.getWriter().write("차단된 URL입니다: " + redirectUrl);
                return false;
            }
        }
        
        return true;
    }
}