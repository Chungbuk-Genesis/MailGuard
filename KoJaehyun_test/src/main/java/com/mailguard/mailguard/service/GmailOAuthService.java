package com.mailguard.mailguard.service;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.mailguard.mailguard.config.GmailConfig;
import com.mailguard.mailguard.dto.EmailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailOAuthService {
private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
private final PhishingDetectionService phishingDetectionService;

private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
    InputStream in = GmailOAuthService.class.getResourceAsStream(GmailConfig.CREDENTIALS_FILE_PATH);
    if (in == null) {
        throw new FileNotFoundException("❌ credentials.json 파일을 찾을 수 없습니다. " +
                "src/main/resources/credentials.json 경로에 파일을 복사하세요!");
    }
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
    
    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, 
            Collections.singletonList(GmailConfig.GMAIL_READONLY_SCOPE))
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(GmailConfig.TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build();
    
    LocalServerReceiver receiver = new LocalServerReceiver.Builder()
            .setPort(GmailConfig.LOCAL_SERVER_PORT)
            .build();
    
    log.info(" OAuth 인증 시작... 브라우저가 자동으로 열립니다.");
    return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
}

public Gmail getGmailService() throws Exception {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Credential credential = getCredentials(HTTP_TRANSPORT);
    log.info("Gmail 인증 완료!");
    
    return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(GmailConfig.APPLICATION_NAME)
            .build();
}

public List<EmailDto> fetchAndAnalyzeEmails(int maxEmails) throws Exception {
    List<EmailDto> emails = new ArrayList<>();
    Gmail service = getGmailService();
    String user = "me";
    
    log.info("📧 Gmail에서 최근 {} 개 메일 가져오는 중...", maxEmails);
    
    ListMessagesResponse listResponse = service.users().messages()
            .list(user)
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
                    .get(user, message.getId())
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

private EmailDto parseGmailMessage(Message message) {
    EmailDto.EmailDtoBuilder builder = EmailDto.builder();
    
    builder.messageId(message.getId());
    
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
    
    List<String> attachments = new ArrayList<>();
    StringBuilder textContent = new StringBuilder();
    StringBuilder htmlContent = new StringBuilder();
    
    parseMessagePart(message.getPayload(), textContent, htmlContent, attachments);
    
    String finalContent = textContent.toString();
    if (finalContent.isEmpty() && htmlContent.length() > 0) {
        finalContent = Jsoup.parse(htmlContent.toString()).text();
    }
    
    builder.content(finalContent.length() > 500 ? 
            finalContent.substring(0, 500) + "..." : finalContent);
    builder.htmlContent(htmlContent.toString());
    builder.attachmentNames(attachments);
    builder.hasAttachments(!attachments.isEmpty());
    
    List<String> urls = extractUrls(textContent.toString() + " " + htmlContent.toString());
    builder.extractedUrls(urls);
    
    return builder.build();
}

private void parseMessagePart(MessagePart part, StringBuilder textContent, 
                               StringBuilder htmlContent, List<String> attachments) {
    String mimeType = part.getMimeType();
    
    if (part.getFilename() != null && !part.getFilename().isEmpty()) {
        attachments.add(part.getFilename());
        return;
    }
    
    if (mimeType != null && part.getBody() != null && part.getBody().getData() != null) {
        String decodedData = new String(Base64.decodeBase64(part.getBody().getData()));
        
        if (mimeType.equals("text/plain")) {
            textContent.append(decodedData).append("\n");
        } else if (mimeType.equals("text/html")) {
            htmlContent.append(decodedData).append("\n");
        }
    }
    
    if (part.getParts() != null) {
        for (MessagePart subPart : part.getParts()) {
            parseMessagePart(subPart, textContent, htmlContent, attachments);
        }
    }
}

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
