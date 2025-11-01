package com.example.MailGuard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {
    
    // 기본 정보
    private String messageId;
    private String from;
    private String to;
    private String subject;
    private Date receivedDate;
    
    // 내용
    private String content;
    private String htmlContent;
    
    // 첨부파일
    private boolean hasAttachments;
    @Builder.Default
    private List<String> attachmentNames = new ArrayList<>();
    
    // URL
    @Builder.Default
    private List<String> extractedUrls = new ArrayList<>();
    
    // 분석 결과 (피싱 탐지)
    private String riskLevel; // "SAFE", "SUSPICIOUS", "DANGEROUS"
    private Integer riskScore; // 0-100
    @Builder.Default
    private List<String> detectedPatterns = new ArrayList<>();
    private String analysisMessage;
}
