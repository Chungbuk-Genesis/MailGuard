//package com.example.MailGuard.service;
//
//import com.example.MailGuard.dto.EmailDto;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.jsoup.Jsoup;
//import org.springframework.stereotype.Service;
//
//import javax.mail.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class NaverImapService {
//
//    private final PhishingDetectionService phishingDetectionService;
//
//    public List<EmailDto> fetchAndAnalyzeEmails(String email, String appPassword, int maxEmails) throws Exception {
//        List<EmailDto> emails = new ArrayList<>();
//
//        Properties props = new Properties();
//        props.put("mail.store.protocol", "imaps");
//        props.put("mail.imaps.host", "imap.naver.com");
//        props.put("mail.imaps.port", "993");
//        props.put("mail.imaps.ssl.enable", "true");
//        props.put("mail.imaps.ssl.trust", "imap.naver.com");
//
//        Session session = Session.getInstance(props);
//        Store store = null;
//        Folder inbox = null;
//
//        try {
//            log.info("📧 Naver 메일 연결 시작: {}", email);
//
//            store = session.getStore("imaps");
//            store.connect("imap.naver.com", email, appPassword);
//            log.info("✅ Naver 메일 연결 성공!");
//
//            inbox = store.getFolder("INBOX");
//            inbox.open(Folder.READ_ONLY);
//
//            int messageCount = inbox.getMessageCount();
//            int start = Math.max(1, messageCount - maxEmails + 1);
//            Message[] messages = inbox.getMessages(start, messageCount);
//
//            log.info("📬 총 {} 개 메일 중 {} 개 가져오기", messageCount, messages.length);
//
//            int count = 0;
//            for (int i = messages.length - 1; i >= 0; i--) {
//                try {
//                    count++;
//                    log.info("  ⏳ [{}/{}] 메일 처리 중...", count, messages.length);
//
//                    EmailDto emailDto = parseMessage(messages[i]);
//                    phishingDetectionService.analyzeEmail(emailDto);
//                    emails.add(emailDto);
//
//                    log.info("    ✓ 제목: {} | 위험도: {} ({}점)",
//                            emailDto.getSubject(),
//                            emailDto.getRiskLevel(),
//                            emailDto.getRiskScore());
//
//                } catch (Exception e) {
//                    log.error("❌ 메일 파싱 실패: {}", e.getMessage());
//                }
//            }
//
//            log.info("🎉 Naver 메일 분석 완료! 총 {} 개", emails.size());
//
//        } finally {
//            if (inbox != null && inbox.isOpen()) {
//                inbox.close(false);
//            }
//            if (store != null && store.isConnected()) {
//                store.close();
//            }
//        }
//
//        return emails;
//    }
//
//    private EmailDto parseMessage(Message message) throws Exception {
//        EmailDto.EmailDtoBuilder builder = EmailDto.builder();
//
//
//        builder.from(decodeHtmlEntities(getFrom(message)));
//        builder.to(decodeHtmlEntities(getTo(message)));
//        builder.subject(decodeHtmlEntities(
//                message.getSubject() != null ? message.getSubject() : "(제목 없음)"
//        ));
//        builder.receivedDate(message.getReceivedDate());
//
//        List<String> attachments = new ArrayList<>();
//        StringBuilder textContent = new StringBuilder();
//        StringBuilder htmlContent = new StringBuilder();
//
//        Object content = message.getContent();
//        if (content instanceof Multipart) {
//            Multipart multipart = (Multipart) content;
//            processMultipart(multipart, textContent, htmlContent, attachments);
//        } else if (content instanceof String) {
//            textContent.append(content.toString());
//        }
//
//        String finalContent = textContent.toString();
//        if (finalContent.isEmpty() && htmlContent.length() > 0) {
//            finalContent = Jsoup.parse(htmlContent.toString()).text();
//        }
//
//
//        finalContent = decodeHtmlEntities(finalContent);
//
//        builder.content(finalContent.length() > 500 ?
//                finalContent.substring(0, 500) + "..." : finalContent);
//        builder.htmlContent(htmlContent.toString());
//        builder.attachmentNames(attachments);
//        builder.hasAttachments(!attachments.isEmpty());
//
//        List<String> urls = extractUrls(textContent.toString() + " " + htmlContent.toString());
//        builder.extractedUrls(urls);
//
//        return builder.build();
//    }
//
//    private void processMultipart(Multipart multipart, StringBuilder textContent,
//                                  StringBuilder htmlContent, List<String> attachments) throws Exception {
//        for (int i = 0; i < multipart.getCount(); i++) {
//            BodyPart bodyPart = multipart.getBodyPart(i);
//            String disposition = bodyPart.getDisposition();
//
//            if (disposition != null && disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
//                String filename = bodyPart.getFileName();
//                if (filename != null) {
//                    filename = javax.mail.internet.MimeUtility.decodeText(filename);
//                    attachments.add(filename);
//                }
//            } else {
//                Object partContent = bodyPart.getContent();
//                if (partContent instanceof String) {
//                    String contentType = bodyPart.getContentType().toLowerCase();
//                    if (contentType.contains("text/plain")) {
//                        textContent.append(partContent.toString()).append("\n");
//                    } else if (contentType.contains("text/html")) {
//                        htmlContent.append(partContent.toString()).append("\n");
//                    }
//                } else if (partContent instanceof Multipart) {
//                    processMultipart((Multipart) partContent, textContent, htmlContent, attachments);
//                }
//            }
//        }
//    }
//
//    public List<String> extractUrls(String content) {
//        List<String> urls = new ArrayList<>();
//        String urlRegex = "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+";
//        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
//        Matcher matcher = pattern.matcher(content);
//
//        while (matcher.find()) {
//            urls.add(matcher.group());
//        }
//
//        return urls;
//    }
//
//    private String getFrom(Message message) throws Exception {
//        Address[] from = message.getFrom();
//        if (from != null && from.length > 0) {
//            String fromStr = from[0].toString();
//            return javax.mail.internet.MimeUtility.decodeText(fromStr);
//        }
//        return "Unknown";
//    }
//
//    private String getTo(Message message) throws Exception {
//        Address[] to = message.getRecipients(Message.RecipientType.TO);
//        if (to != null && to.length > 0) {
//            String toStr = to[0].toString();
//            return javax.mail.internet.MimeUtility.decodeText(toStr);
//        }
//        return "Unknown";
//    }
//
//
//    private String decodeHtmlEntities(String text) {
//        if (text == null) {
//            return null;
//        }
//
//        // Unicode escape sequences (#uXXXX 형식)
//        text = text.replaceAll("#u003C", "<")
//                .replaceAll("#u003E", ">")
//                .replaceAll("#u0026", "&")
//                .replaceAll("#u0027", "'")
//                .replaceAll("#u0022", "\"")
//                .replaceAll("#u003D", "=")
//                .replaceAll("#u002F", "/")
//                .replaceAll("#u003A", ":")
//                .replaceAll("#u003B", ";")
//                .replaceAll("#u0040", "@");
//
//        // entities
//        text = text.replace("&lt;", "<")
//                .replace("&gt;", ">")
//                .replace("&amp;", "&")
//                .replace("&quot;", "\"")
//                .replace("&apos;", "'")
//                .replace("&#60;", "<")
//                .replace("&#62;", ">")
//                .replace("&#38;", "&")
//                .replace("&#34;", "\"")
//                .replace("&#39;", "'");
//
//        return text;
//    }
//}

package com.example.MailGuard.service;

import com.example.MailGuard.dto.AttachmentDto;
import com.example.MailGuard.dto.EmailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    // ==================== 메일 목록 조회 및 분석 ====================
    public List<EmailDto> fetchAndAnalyzeEmails(String email, String appPassword, int maxEmails) throws Exception {
        List<EmailDto> emails = new ArrayList<>();

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.naver.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", "imap.naver.com");

        Session session = Session.getInstance(props);
        Store store = null;
        Folder inbox = null;

        try {
            log.info("📧 Naver 메일 연결 시작: {}", email);

            store = session.getStore("imaps");
            store.connect("imap.naver.com", email, appPassword);
            log.info("✅ Naver 메일 연결 성공!");

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            int messageCount = inbox.getMessageCount();
            int start = Math.max(1, messageCount - maxEmails + 1);
            Message[] messages = inbox.getMessages(start, messageCount);

            log.info("📬 총 {} 개 메일 중 {} 개 가져오기", messageCount, messages.length);

            // UIDFolder 인터페이스 확인 (메일 고유 ID 가져오기 위함)
            UIDFolder uidFolder = (inbox instanceof UIDFolder) ? (UIDFolder) inbox : null;

            int count = 0;
            for (int i = messages.length - 1; i >= 0; i--) {
                try {
                    count++;
                    log.info("  ⏳ [{}/{}] 메일 처리 중...", count, messages.length);

                    // 메시지 파싱 시 UIDFolder를 넘겨서 messageId를 설정
                    EmailDto emailDto = parseMessage(messages[i], uidFolder);

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

    // ==================== 첨부파일 다운로드 (byte[]) ====================
    public byte[] getAttachmentBytes(String email, String password, String messageIdStr, String filename) throws Exception {
        Store store = connectToImap(email, password);
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);

        byte[] data = null;

        try {
            // messageIdStr(String) -> UID(long) 변환
            long uid = Long.parseLong(messageIdStr);

            if (folder instanceof UIDFolder) {
                Message message = ((UIDFolder) folder).getMessageByUID(uid);
                if (message == null) {
                    throw new MessagingException("해당 ID의 메일을 찾을 수 없습니다: " + uid);
                }

                // 재귀적으로 첨부파일 데이터 찾기
                data = findAttachmentData(message, filename);
            } else {
                throw new MessagingException("IMAP UID 기능을 지원하지 않는 폴더입니다.");
            }
        } finally {
            folder.close(false);
            store.close();
        }

        if (data == null) {
            throw new IOException("첨부파일을 찾을 수 없습니다: " + filename);
        }
        return data;
    }

    // ==================== 첨부파일 목록 상세 조회 ====================
    public List<AttachmentDto> getAttachmentList(String email, String password, String messageIdStr) throws Exception {
        Store store = connectToImap(email, password);
        Folder folder = store.getFolder("INBOX");
        folder.open(Folder.READ_ONLY);

        List<AttachmentDto> attachments = new ArrayList<>();

        try {
            long uid = Long.parseLong(messageIdStr);

            if (folder instanceof UIDFolder) {
                Message message = ((UIDFolder) folder).getMessageByUID(uid);
                if (message != null) {
                    extractAttachments(message, attachments, messageIdStr);
                }
            }
        } finally {
            folder.close(false);
            store.close();
        }

        return attachments;
    }

    // ==================== Private Helpers ====================

    private Store connectToImap(String email, String password) throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.naver.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect("imap.naver.com", email, password);
        return store;
    }

    // 기존 parseMessage 수정: UIDFolder를 인자로 받아 ID 설정
    private EmailDto parseMessage(Message message, UIDFolder uidFolder) throws Exception {
        EmailDto.EmailDtoBuilder builder = EmailDto.builder();

        // 1. Message-ID 설정 (IMAP UID)
        if (uidFolder != null) {
            long uid = uidFolder.getUID(message);
            builder.messageId(String.valueOf(uid));
        }

        builder.from(decodeHtmlEntities(getFrom(message)));
        builder.to(decodeHtmlEntities(getTo(message)));
        builder.subject(decodeHtmlEntities(
                message.getSubject() != null ? message.getSubject() : "(제목 없음)"
        ));
        builder.receivedDate(message.getReceivedDate());

        List<String> attachmentNames = new ArrayList<>();
        List<AttachmentDto> attachmentDtos = new ArrayList<>(); // 상세 정보를 위한 리스트

        StringBuilder textContent = new StringBuilder();
        StringBuilder htmlContent = new StringBuilder();

        Object content = message.getContent();
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            // 첨부파일 이름 추출용으로 기존 메서드 활용 (단, DTO 매핑을 위해 약간 수정 필요할 수 있음)
            // 여기서는 getAttachmentList와 호환성을 위해 ID가 설정된 경우 DTO도 만듦
            String msgId = (uidFolder != null) ? String.valueOf(uidFolder.getUID(message)) : null;
            processMultipart(multipart, textContent, htmlContent, attachmentNames, attachmentDtos, msgId);
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

        builder.attachmentNames(attachmentNames);
        builder.attachments(attachmentDtos); // 상세 목록도 저장
        builder.hasAttachments(!attachmentNames.isEmpty());

        List<String> urls = extractUrls(textContent.toString() + " " + htmlContent.toString());
        builder.extractedUrls(urls);

        return builder.build();
    }

    // processMultipart 수정: 상세 DTO 리스트도 채움
    private void processMultipart(Multipart multipart, StringBuilder textContent,
                                  StringBuilder htmlContent, List<String> attachmentNames,
                                  List<AttachmentDto> attachmentDtos, String msgId) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String disposition = bodyPart.getDisposition();

            if (disposition != null && (disposition.equalsIgnoreCase(Part.ATTACHMENT) || disposition.equalsIgnoreCase(Part.INLINE))) {
                String filename = bodyPart.getFileName();
                if (filename != null) {
                    filename = MimeUtility.decodeText(filename);
                    attachmentNames.add(filename);
                    if (msgId != null) {
                        // Naver의 경우 attachmentId를 filename으로 사용
                        attachmentDtos.add(new AttachmentDto(filename, msgId, filename));
                    }
                }
            } else {
                // 재귀 탐색
                Object partContent = bodyPart.getContent();
                if (partContent instanceof String) {
                    String contentType = bodyPart.getContentType().toLowerCase();
                    if (contentType.contains("text/plain")) {
                        textContent.append(partContent.toString()).append("\n");
                    } else if (contentType.contains("text/html")) {
                        htmlContent.append(partContent.toString()).append("\n");
                    }
                } else if (partContent instanceof Multipart) {
                    processMultipart((Multipart) partContent, textContent, htmlContent, attachmentNames, attachmentDtos, msgId);
                }
            }
        }
    }

    // [헬퍼] 바이트 데이터 찾기
    private byte[] findAttachmentData(Part part, String targetFilename) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                byte[] data = findAttachmentData(multipart.getBodyPart(i), targetFilename);
                if (data != null) return data;
            }
        } else {
            String disposition = part.getDisposition();
            String fileName = part.getFileName();

            if (fileName != null) {
                fileName = MimeUtility.decodeText(fileName);
                // 첨부파일이면서 이름이 일치하는 경우
                if ((Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition))
                        && fileName.equals(targetFilename)) {
                    return inputStreamToBytes(part.getInputStream());
                }
            }
        }
        return null;
    }

    // [헬퍼] 목록 추출
    private void extractAttachments(Part part, List<AttachmentDto> attachments, String msgId) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                extractAttachments(multipart.getBodyPart(i), attachments, msgId);
            }
        } else {
            String disposition = part.getDisposition();
            String fileName = part.getFileName();

            if (fileName != null && (Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition))) {
                fileName = MimeUtility.decodeText(fileName);
                // Naver에서는 filename 자체를 attachmentId로 사용
                attachments.add(new AttachmentDto(fileName, msgId, fileName));
            }
        }
    }

    private byte[] inputStreamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
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
        if (text == null) return null;

        // Unicode escape sequences
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

        // HTML Entities
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