package com.example.MailGuard.service;

import com.example.MailGuard.dto.EmailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 간단한 룰 기반 피싱 탐지 서비스
 */
@Slf4j
@Service
public class PhishingDetectionService {
    
    // 피싱 의심 키워드 (한글)
    private static final List<String> PHISHING_KEYWORDS_KR = Arrays.asList(
        "긴급", "확인", "정지", "차단", "해지", "계좌", "이체", "송금",
        "환급", "당첨", "무료", "쿠폰", "혜택", "클릭", "링크",
        "비밀번호", "재설정", "인증", "본인확인", "카드정보", "개인정보"
    );
    
    // 피싱 의심 키워드 (영문)
    private static final List<String> PHISHING_KEYWORDS_EN = Arrays.asList(
        "urgent", "verify", "suspended", "blocked", "account", "payment",
        "refund", "winner", "free", "click here", "reset password",
        "confirm", "security alert", "unusual activity"
    );
    
    // 피싱 의심 도메인
    private static final List<String> SUSPICIOUS_DOMAINS = Arrays.asList(
        "bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly",
        ".xyz", ".top", ".click", ".loan"
    );
    
    /**
     * 메일 분석 및 위험도 판정
     */
    public void analyzeEmail(EmailDto email) {
        int riskScore = 0;
        List<String> detectedPatterns = new ArrayList<>();
        
        String fullText = (email.getSubject() + " " + email.getContent()).toLowerCase();
        
        // 1. 키워드 체크 (한글)
        int krKeywordCount = 0;
        for (String keyword : PHISHING_KEYWORDS_KR) {
            if (fullText.contains(keyword)) {
                krKeywordCount++;
            }
        }
        if (krKeywordCount > 0) {
            riskScore += krKeywordCount * 10;
            detectedPatterns.add("피싱 의심 키워드 " + krKeywordCount + "개 발견");
        }
        
        // 2. 키워드 체크 (영문)
        int enKeywordCount = 0;
        for (String keyword : PHISHING_KEYWORDS_EN) {
            if (fullText.contains(keyword)) {
                enKeywordCount++;
            }
        }
        if (enKeywordCount > 0) {
            riskScore += enKeywordCount * 10;
            detectedPatterns.add("Phishing keywords found: " + enKeywordCount);
        }
        
        // 3. URL 체크
        if (email.getExtractedUrls() != null && !email.getExtractedUrls().isEmpty()) {
            int suspiciousUrlCount = 0;
            for (String url : email.getExtractedUrls()) {
                for (String domain : SUSPICIOUS_DOMAINS) {
                    if (url.toLowerCase().contains(domain)) {
                        suspiciousUrlCount++;
                        break;
                    }
                }
            }
            if (suspiciousUrlCount > 0) {
                riskScore += suspiciousUrlCount * 15;
                detectedPatterns.add("의심스러운 단축 URL " + suspiciousUrlCount + "개 발견");
            }
            
            // URL이 많으면 의심
            if (email.getExtractedUrls().size() > 5) {
                riskScore += 10;
                detectedPatterns.add("다수의 링크 포함 (" + email.getExtractedUrls().size() + "개)");
            }
        }
        
        // 4. 발신자 체크
        if (email.getFrom() != null) {
            String from = email.getFrom().toLowerCase();
            
            // 유명 기관 사칭 체크
            if (from.contains("bank") || from.contains("은행") ||
                from.contains("카드") || from.contains("국세청")) {
                if (!from.contains("@bank") && !from.contains("@card")) {
                    riskScore += 30;
                    detectedPatterns.add("금융기관/정부기관 사칭 의심");
                }
            }
        }
        
        // 5. 첨부파일 체크
        if (email.isHasAttachments()) {
            for (String filename : email.getAttachmentNames()) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.endsWith(".exe") || lowerFilename.endsWith(".bat") ||
                    lowerFilename.endsWith(".cmd") || lowerFilename.endsWith(".scr") ||
                    lowerFilename.endsWith(".zip") || lowerFilename.endsWith(".rar")) {
                    riskScore += 25;
                    detectedPatterns.add("위험한 첨부파일 유형: " + filename);
                }
            }
        }
        
        // 6. 제목 체크 (ALL CAPS)
        if (email.getSubject() != null && email.getSubject().equals(email.getSubject().toUpperCase())) {
            riskScore += 5;
            detectedPatterns.add("제목이 전부 대문자 (스팸 특성)");
        }
        
        // 위험도 레벨 결정
        String riskLevel;
        String message;
        
        if (riskScore >= 50) {
            riskLevel = "DANGEROUS";
            message = "⚠️ 위험: 피싱 메일일 가능성이 매우 높습니다!";
        } else if (riskScore >= 25) {
            riskLevel = "SUSPICIOUS";
            message = "⚡ 주의: 의심스러운 내용이 포함되어 있습니다.";
        } else {
            riskLevel = "SAFE";
            message = "✅ 안전: 특이사항이 발견되지 않았습니다.";
            if (detectedPatterns.isEmpty()) {
                detectedPatterns.add("위험 요소 없음");
            }
        }
        
        // 결과 설정
        email.setRiskLevel(riskLevel);
        email.setRiskScore(Math.min(riskScore, 100));
        email.setDetectedPatterns(detectedPatterns);
        email.setAnalysisMessage(message);
        
        log.info("메일 분석 완료 - 제목: {}, 위험도: {} ({}점)", 
                email.getSubject(), riskLevel, email.getRiskScore());
    }
}