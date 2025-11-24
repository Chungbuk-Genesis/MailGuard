package com.example.MailGuard.service;

import com.example.MailGuard.dto.AttachmentDto;
import com.example.MailGuard.dto.EmailDto;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class GmailService {
    private static final String APPLICATION_NAME = "GMAIL SERVICE";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String USER_ID = "me";

    private final HttpTransport httpTransport;

    @Autowired
    public GmailService(HttpTransport httpTransport) {
        this.httpTransport = httpTransport;
    }

    public EmailDto getLatestEmail(Credential credential) throws IOException {
        Gmail service = buildGmailService(credential);

        ListMessagesResponse listResponse = service.users().messages().list(USER_ID)
                .setQ("is:inbox category:primary")
                .setMaxResults(1L)
                .execute();

        List<Message> messages = listResponse.getMessages();
        if (messages == null || messages.isEmpty()) return null;

        String messageId = messages.get(0).getId();
        Message message = service.users().messages().get(USER_ID, messageId)
                .setFormat("full")
                .execute();

        return parseMessageToDto(message);
    }

    public List<AttachmentDto> listAttachments(Credential credential, String messageId) throws IOException {
        Gmail service = buildGmailService(credential);
        Message message = service.users().messages().get(USER_ID, messageId).setFormat("full").execute();
        return getAttachments(message.getPayload(), messageId);
    }

    public byte[] getAttachmentBytes(Credential credential, String messageId, String attachmentId) throws IOException {
        Gmail service = buildGmailService(credential);
        MessagePartBody body = service.users().messages().attachments()
                .get(USER_ID, messageId, attachmentId).execute();
        return Base64.decodeBase64(body.getData());
    }

    private Gmail buildGmailService(Credential credential) {
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private EmailDto parseMessageToDto(Message message) {
        EmailDto emailDto = new EmailDto();
        emailDto.setMessageId(message.getId());
        emailDto.setSubject(getHeader(message, "Subject"));
        emailDto.setFrom(getHeader(message, "From"));
        emailDto.setContent(getBody(message.getPayload()));
//        emailDto.setHtmlContent(getBody(message.getPayload()));
        emailDto.setAttachments(getAttachments(message.getPayload(), message.getId()));
        return emailDto;
    }

    private String getHeader(Message message, String name) {
        return message.getPayload().getHeaders().stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(h -> h.getValue())
                .orElse("N/A");
    }

    private String getBody(MessagePart payload) {
        if (payload == null) return "";
        if (payload.getMimeType().startsWith("text/")) {
            if (payload.getBody() != null && payload.getBody().getData() != null) {
                return new String(Base64.decodeBase64(payload.getBody().getData()), StandardCharsets.UTF_8);
            }
        }
        if (payload.getMimeType().startsWith("multipart/")) {
            List<MessagePart> parts = payload.getParts();
            if (parts == null) return "";
            if (payload.getMimeType().equalsIgnoreCase("multipart/alternative")) {
                for (MessagePart part : parts) if (part.getMimeType().equalsIgnoreCase("text/html")) return getBody(part);
                for (MessagePart part : parts) if (part.getMimeType().equalsIgnoreCase("text/plain")) return getBody(part);
            }
            StringBuilder sb = new StringBuilder();
            for (MessagePart part : parts) {
                if (part.getFilename() == null || part.getFilename().isEmpty()) {
                    sb.append(getBody(part));
                }
            }
            return sb.toString();
        }
        return "";
    }

    private List<AttachmentDto> getAttachments(MessagePart payload, String messageId) {
        List<AttachmentDto> attachments = new ArrayList<>();
        if (payload == null) return attachments;
        if (payload.getFilename() != null && !payload.getFilename().isEmpty()
                && payload.getBody() != null && payload.getBody().getAttachmentId() != null) {
            attachments.add(new AttachmentDto(payload.getFilename(), messageId, payload.getBody().getAttachmentId()));
        }
        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                attachments.addAll(getAttachments(part, messageId));
            }
        }
        return attachments;
    }
}
