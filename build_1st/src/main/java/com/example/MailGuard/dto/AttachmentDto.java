package com.example.MailGuard.dto;

import java.nio.charset.StandardCharsets;

public class AttachmentDto {

    //첨부파일 이름, 다운로드 위한 링크
    private String filename;
    private String downloadUrl;

    //첨부파일을 고유하게 식별하기 위한 ID(messageId, attachmentId)
    private String messageId;
    private String attachmentId;

    // Spring이 JSON 변환 시 사용하는 기본 생성자
    public AttachmentDto() {
    }

     // AttachmentDTO: GmailService에서 첨부파일 정보를 받아 DTO를 생성하는 생성자
    public AttachmentDto(String filename, String messageId, String attachmentId) {
        this.filename = filename; //첨부파일 이름
        this.messageId = messageId; //첨부파일이 속한 이메일 ID
        this.attachmentId = attachmentId; //이메일에 속한 첨부파일 고유 ID

        // 프론트엔드에서 사용할 다운로드 URL을 동적으로 생성
        // 파일 이름에 한글이나 특수문자가 있을 수 있으므로 URL 인코딩 처리
        this.downloadUrl = String.format("/api/attachment/download?msgId=%s&attId=%s&filename=%s",
                messageId, attachmentId, java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8));
    }

    // Getters and Setters

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }
}
