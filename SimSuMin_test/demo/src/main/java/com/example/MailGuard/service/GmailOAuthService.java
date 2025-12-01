package com.example.MailGuard.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.example.MailGuard.dto.EmailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailOAuthService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String USER_ID = "me";

    private final PhishingDetectionService phishingDetectionService;

    @Value("${google.application-name:MailGuard}")
    private String applicationName;

    /**
     * Credential을 받아서 Gmail Service 생성
     * (Controller에서 Session의 Credential을 전달받음)
     */
    public Gmail getGmailService(Credential credential) {
        NetHttpTransport httpTransport = new NetHttpTransport();

        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
    }

    /**
     * Gmail에서 이메일을 가져와서 피싱 분석 수행
     * @param credential - OAuth 인증된 Credential (Session에서 가져옴)
     * @param maxEmails - 가져올 최대 이메일 개수
     */
    public List<EmailDto> fetchAndAnalyzeEmails(Credential credential, int maxEmails) throws IOException {
        List<EmailDto> emails = new ArrayList<>();
        Gmail service = getGmailService(credential);

        log.info("📧 Gmail에서 최근 {} 개 메일 가져오는 중...", maxEmails);

        ListMessagesResponse listResponse = service.users().messages()
                .list(USER_ID)
                .setMaxResults((long) maxEmails)
                .execute();

        List<Message> messages = listResponse.getMessages();
        if (messages == null || messages.isEmpty()) {
            log.info("📭 받은 메일이 없습니다.");
            return emails;
        }

        log.info("📬 총 {} 개 메일 발견. 분석 시작...", messages.size());

        int count = 0;
        for (Message message : messages) {
            try {
                count++;
                log.info("   [{}/{}] 메일 처리 중...", count, messages.size());

                Message fullMessage = service.users().messages()
                        .get(USER_ID, message.getId())
                        .setFormat("full")
                        .execute();

                EmailDto emailDto = parseGmailMessage(fullMessage);
                phishingDetectionService.analyzeEmail(emailDto);
                emails.add(emailDto);

                log.info("    ✓ 제목: {} | 위험도: {} ({}점)",
                        emailDto.getSubject(),
                        emailDto.getRiskLevel(),
                        emailDto.getRiskScore());

            } catch (Exception e) {
                log.error("❌ 메일 파싱 실패: {}", e.getMessage());
            }
        }

        log.info("🎉 메일 분석 완료! 총 {} 개", emails.size());
        return emails;
    }

    /**
     * Gmail Message를 EmailDto로 변환
     */
    private EmailDto parseGmailMessage(Message message) {
        EmailDto.EmailDtoBuilder builder = EmailDto.builder();

        builder.messageId(message.getId());

        if (message.getInternalDate() != null) {
            builder.receivedDate(new java.util.Date(message.getInternalDate()));
        }

        // 헤더 파싱 (From, To, Subject)
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        for (MessagePartHeader header : headers) {
            String name = header.getName();
            String value = header.getValue();

            switch (name.toLowerCase()) {
                case "from":
                    builder.from(value);
                    break;
                case "to":
                    builder.to(value);
                    break;
                case "subject":
                    builder.subject(value != null ? value : "(제목 없음)");
                    break;
            }
        }

        // 본문 및 첨부파일 파싱
        List<String> attachments = new ArrayList<>();
        StringBuilder textContent = new StringBuilder();
        StringBuilder htmlContent = new StringBuilder();

        parseMessagePart(message.getPayload(), textContent, htmlContent, attachments);

        // 최종 본문 결정
        String finalContent = textContent.toString();
        if (finalContent.isEmpty() && htmlContent.length() > 0) {
            finalContent = Jsoup.parse(htmlContent.toString()).text();
        }

        builder.content(finalContent.length() > 500 ?
                finalContent.substring(0, 500) + "..." : finalContent);
        builder.htmlContent(htmlContent.toString());
        builder.attachmentNames(attachments);
        builder.hasAttachments(!attachments.isEmpty());

        // URL 추출
        List<String> urls = extractUrls(textContent.toString() + " " + htmlContent.toString());
        builder.extractedUrls(urls);

        return builder.build();
    }

    /**
     * 메시지 파트를 재귀적으로 파싱 (멀티파트 지원)
     */
    private void parseMessagePart(MessagePart part, StringBuilder textContent,
                                  StringBuilder htmlContent, List<String> attachments) {
        String mimeType = part.getMimeType();

        // 첨부파일 처리
        if (part.getFilename() != null && !part.getFilename().isEmpty()) {
            attachments.add(part.getFilename());
            return;
        }

        // 본문 처리
        if (mimeType != null && part.getBody() != null && part.getBody().getData() != null) {
            String decodedData = new String(Base64.decodeBase64(part.getBody().getData()));

            if (mimeType.equals("text/plain")) {
                textContent.append(decodedData).append("\n");
            } else if (mimeType.equals("text/html")) {
                htmlContent.append(decodedData).append("\n");
            }
        }

        // 멀티파트 재귀 처리
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                parseMessagePart(subPart, textContent, htmlContent, attachments);
            }
        }
    }

    /**
     * 본문에서 URL 추출
     */
    public List<String> extractUrls(String content) {
        List<String> urls = new ArrayList<>();
        String urlRegex = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            urls.add(matcher.group());
        }

        return urls;
    }
}