package com.mailguard.mailguard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 이메일 기본 정보
    @Column(name = "message_id", unique = true)
    private String messageId;
    
    @Column(name = "sender")
    private String sender;
    
    @Column(name = "receiver")
    private String receiver;
    
    @Column(name = "subject", length = 500)
    private String subject;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    
    // 첨부파일
    @Column(name = "has_attachments")
    private Boolean hasAttachments;
    
    @Column(name = "attachment_names", columnDefinition = "TEXT")
    private String attachmentNames;  // JSON 형태로 저장
    
    // URL 정보
    @Column(name = "extracted_urls", columnDefinition = "TEXT")
    private String extractedUrls;  // JSON 형태로 저장
    
    // 분석 결과
    @Column(name = "rule_based_score")
    private Integer ruleBasedScore;  // 규칙 기반 점수
    
    @Column(name = "final_score")
    private Integer finalScore;  // 최종 점수 (LLM 반영)
    
    @Column(name = "risk_level")
    private String riskLevel;  // SAFE, SUSPICIOUS, DANGEROUS
    
    @Column(name = "detected_patterns", columnDefinition = "TEXT")
    private String detectedPatterns;  // JSON 형태로 저장
    
    @Column(name = "analysis_message", columnDefinition = "TEXT")
    private String analysisMessage;
    
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;
    
    // 재평가 관련
    @Column(name = "is_reanalyzed")
    @Builder.Default
    private Boolean isReanalyzed = false;
    
    @Column(name = "reanalysis_count")
    @Builder.Default
    private Integer reanalysisCount = 0;
    
    @Column(name = "last_reanalyzed_at")
    private LocalDateTime lastReanalyzedAt;
}
