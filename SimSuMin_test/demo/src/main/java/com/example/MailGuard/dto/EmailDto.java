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
    private List<AttachmentDto> attachments = new ArrayList<>();

    // URL
    @Builder.Default
    private List<String> extractedUrls = new ArrayList<>();
    
    // 분석 결과 (피싱 탐지)
    private String riskLevel; // "SAFE", "SUSPICIOUS", "DANGEROUS"
    private Integer riskScore; // 0-100
    @Builder.Default
    private List<String> detectedPatterns = new ArrayList<>();
    private String analysisMessage;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public boolean isHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public List<String> getAttachmentNames() {
        return attachmentNames;
    }

    public void setAttachmentNames(List<String> attachmentNames) {
        this.attachmentNames = attachmentNames;
    }

    public List<AttachmentDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDto> attachment) {
        this.attachments = attachment;
    }
}
