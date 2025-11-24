package com.example.MailGuard.service;

import com.example.MailGuard.dto.EmailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.MimeUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverImapService {

    private final PhishingDetectionService phishingDetectionService;

    // ✅ 하드코딩 제거하고 properties 에서 주입
    @Value("${naver.imap.host:imap.naver.com}")
    private String imapHost;

    @Value("${naver.imap.port:993}")
    private int imapPort;

    @Value("${naver.imap.ssl.enable:true}")
    private boolean imapSslEnable;

    @Value("${naver.imap.ssl.trust:imap.naver.com}")
    private String imapSslTrust;

    /**
     * @param email       네이버 메일 주소 (예: zbawoo2k@naver.com)
     * @param appPassword 네이버 비밀번호(또는 앱 비밀번호)
     * @param maxEmails   최근 몇 개까지 가져올지
     */
    public List<EmailDto> fetchAndAnalyzeEmails(String email, String appPassword, int maxEmails) throws Exception {
        List<EmailDto> emails = new ArrayList<>();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", String.valueOf(imapPort));
        props.put("mail.imaps.ssl.enable", String.valueOf(imapSslEnable));
        props.put("mail.imaps.ssl.trust", imapSslTrust);

        Session session = Session.getInstance(props);
        Store store = null;
        Folder inbox = null;

        try {
            log.info("📧 Naver 메일 연결 시작: {}", email);

            store = session.getStore("imaps");
            store.connect(imapHost, email, appPassword);   // ✅ 하드코딩 제거, imapHost 사용
            log.info("✅ Naver 메일 연결 성공!");

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int messageCount = inbox.getMessageCount();
            int start = Math.max(1, messageCount - maxEmails + 1);
            Message[] messages = inbox.getMessages(start, messageCount);

            log.info("📬 총 {} 개 메일 중 {} 개 가져오기", messageCount, messages.length);

            int count = 0;
            for (int i = messages.length - 1; i >= 0; i--) {
                try {
                    count++;
                    log.info("  ⏳ [{}/{}] 메일 처리 중...", count, messages.length);

                    EmailDto emailDto = parseMessage(messages[i]);
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

            log.info("🎉 Naver 메일 분석 완료! 총 {} 개", emails.size());

        } finally {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }

        return emails;
    }

    private EmailDto parseMessage(Message message) throws Exception {
        EmailDto.EmailDtoBuilder builder = EmailDto.builder();

        builder.from(decodeHtmlEntities(getFrom(message)));
        builder.to(decodeHtmlEntities(getTo(message)));
        builder.subject(decodeHtmlEntities(
                message.getSubject() != null ? message.getSubject() : "(제목 없음)"
        ));
        builder.receivedDate(message.getReceivedDate());

        List<String> attachments = new ArrayList<>();
        StringBuilder textContent = new StringBuilder();
        StringBuilder htmlContent = new StringBuilder();

        Object content = message.getContent();
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            processMultipart(multipart, textContent, htmlContent, attachments);
        } else if (content instanceof String) {
            textContent.append(content.toString());
        }

        String finalContent = textContent.toString();
        if (finalContent.isEmpty() && htmlContent.length() > 0) {
            finalContent = Jsoup.parse(htmlContent.toString()).text();
        }

        finalContent = decodeHtmlEntities(finalContent);

        builder.content(finalContent.length() > 500 ?
                finalContent.substring(0, 500) + "..." : finalContent);
        builder.htmlContent(htmlContent.toString());
        builder.attachmentNames(attachments);
        builder.hasAttachments(!attachments.isEmpty());

        List<String> urls = extractUrls(textContent.toString() + " " + htmlContent.toString());
        builder.extractedUrls(urls);

        return builder.build();
    }

    private void processMultipart(Multipart multipart,
                                  StringBuilder textContent,
                                  StringBuilder htmlContent,
                                  List<String> attachments) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String disposition = bodyPart.getDisposition();

            if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                String filename = bodyPart.getFileName();
                if (filename != null) {
                    filename = MimeUtility.decodeText(filename);
                    attachments.add(filename);
                }
            } else {
                Object partContent = bodyPart.getContent();
                if (partContent instanceof String) {
                    String contentType = bodyPart.getContentType().toLowerCase();
                    if (contentType.contains("text/plain")) {
                        textContent.append(partContent.toString()).append("\n");
                    } else if (contentType.contains("text/html")) {
                        htmlContent.append(partContent.toString()).append("\n");
                    }
                } else if (partContent instanceof Multipart) {
                    processMultipart((Multipart) partContent, textContent, htmlContent, attachments);
                }
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

    private String getFrom(Message message) throws Exception {
        Address[] from = message.getFrom();
        if (from != null && from.length > 0) {
            String fromStr = from[0].toString();
            return MimeUtility.decodeText(fromStr);
        }
        return "Unknown";
    }

    private String getTo(Message message) throws Exception {
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        if (to != null && to.length > 0) {
            String toStr = to[0].toString();
            return MimeUtility.decodeText(toStr);
        }
        return "Unknown";
    }

    private String decodeHtmlEntities(String text) {
        if (text == null) {
            return null;
        }

        text = text.replaceAll("#u003C", "<")
                .replaceAll("#u003E", ">")
                .replaceAll("#u0026", "&")
                .replaceAll("#u0027", "'")
                .replaceAll("#u0022", "\"")
                .replaceAll("#u003D", "=")
                .replaceAll("#u002F", "/")
                .replaceAll("#u003A", ":")
                .replaceAll("#u003B", ";")
                .replaceAll("#u0040", "@");

        text = text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&#60;", "<")
                .replace("&#62;", ">")
                .replace("&#38;", "&")
                .replace("&#34;", "\"")
                .replace("&#39;", "'");

        return text;
    }
}
