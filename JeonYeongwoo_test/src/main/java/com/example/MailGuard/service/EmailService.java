package com.example.MailGuard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// 메일 인증 위한 서비스 파일입니다.
// 회원가입 시, 해당 이메일로 아래의 메일이 전송됩니다.

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String to, String token) {
        String subject = "[MailGuard] 이메일 인증을 완료해주세요";

        String verifyLink = baseUrl + "/verify-email?token=" + token;

        String text = """
                안녕하세요, MailGuard입니다.

                아래 링크를 클릭하여 이메일 인증을 완료해주세요.

                %s

                만약 본인이 요청한 적이 없다면 이 메일은 무시하셔도 됩니다.
                """.formatted(verifyLink);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(from);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }
}
