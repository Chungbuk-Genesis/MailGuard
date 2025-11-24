package com.example.MailGuard.service;

import com.example.MailGuard.entity.EmailVerificationToken;
import com.example.MailGuard.entity.User;
import com.example.MailGuard.repo.EmailVerificationTokenRepository;
import com.example.MailGuard.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// 유저 로그인, 

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public boolean registerUser(User user) {

        // 1. 중복 체크
    	// System.out.println("이메일을 체크합니다 ");
        if (userRepository.existsByUsername(user.getUsername()) ||
            userRepository.existsByEmail(user.getEmail())) {
            return false;
        }
        

        // 2. 비밀번호 해시
        // System.out.println("비밀번호를 해시화합니다 ");
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 3. 이메일 미인증 상태로 저장
        // System.out.println("이메일을 저장합니다 ");
        user.setEnabled(false);
        userRepository.save(user);

        // 4. 이메일 인증 토큰 생성
        // System.out.println("이메일 인증 토큰을 생성합니다 ");
        String tokenValue = UUID.randomUUID().toString();

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken(tokenValue);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        token.setUsed(false);

        tokenRepository.save(token);

        // 5. 인증 메일 발송
        try {
        	// System.out.println("메일을 전송합니다 ");
            emailService.sendVerificationEmail(user.getEmail(), tokenValue);
        } catch (Exception e) {
            e.printStackTrace(); // 콘솔에 에러 출력
            // 필요하면 false 리턴해서 화면에 에러 띄워도 됨
            // return false;
        }

        return true;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User findById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public void updateUserProfile(User updatedUser) {
        User user = userRepository.findById(updatedUser.getId()).orElseThrow();
        // 필요한 필드만 갱신 (예: 이메일은 고정이라면 빼고)
        user.setUsername(updatedUser.getUsername());
        // 기타 필드 있으면 추가
        userRepository.save(user);
    }

    // ✅ 이메일 인증 처리 로직
    @Transactional
    public String verifyEmail(String tokenValue) {
        Optional<EmailVerificationToken> optionalToken =
                tokenRepository.findByToken(tokenValue);

        if (optionalToken.isEmpty()) {
            return "유효하지 않은 인증 링크입니다.";
        }

        EmailVerificationToken token = optionalToken.get();

        if (token.isUsed()) {
            return "이미 사용된 인증 링크입니다.";
        }

        if (token.isExpired()) {
            return "인증 링크가 만료되었습니다. 다시 회원가입 또는 인증을 요청해주세요.";
        }

        // 토큰 유효 → 유저 활성화
        User user = token.getUser();
        user.setEnabled(true);

        token.setUsed(true);

        return "이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다.";
    }
}
