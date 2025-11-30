package com.example.MailGuard.repo;

import com.example.MailGuard.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//2025-12-01 추가
import com.example.MailGuard.entity.User;
import com.example.MailGuard.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;



// 메일 인증을 위한 토큰을 생성하는 repository 파일입니다.

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);
    
    //2025-12-01 추가 - 유저의 인증 토큰 전부 삭제
    void deleteAllByUser(User user);
}