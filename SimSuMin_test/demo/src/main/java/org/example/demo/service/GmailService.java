package org.example.demo.service;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;

import com.google.api.services.gmail.model.MessagePartBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.example.demo.dto.AttachmentDto;
import org.example.demo.dto.EmailDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class GmailService {
    private static final String APPLICATION_NAME = "GMAIL SERVICE"; //gmail api 클라이언트 라이브러리에 전달될 app 이름
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String USER_ID = "me"; // api 요청시 현재 인증된 사용자 ID 임의 지정

    private final HttpTransport httpTransport;

    @Autowired
    public GmailService(HttpTransport httpTransport) {
        this.httpTransport = httpTransport;
    }

    //가장 최근 이메일 하나 받아옴.
    public EmailDto getLatestEmail(Credential credential) throws IOException {
        Gmail service = buildGmailService(credential);

        // 'is:inbox category:primry'는 기본 받은 편지함의 메일만 조회한다. - 다른 설정으로 변경가능
        ListMessagesResponse listResponse = service.users().messages().list(USER_ID)
                .setQ("is:inbox category:primary")
                .setMaxResults(1L).execute();

        List<Message> messages = listResponse.getMessages();
        if (messages == null || messages.isEmpty()) {
            return null; //메일이 없으면 null 반환
        }

        //가장 최근 메일 ID 가져와 메일 정보를 요청
        String messageId = messages.get(0).getId();
        Message message = service.users().messages().get(USER_ID, messageId).setFormat("full").execute();

        //가져온 message 객체를 DTO로 변환
        return parseMessageToDto(message);
    }


    // 특정 메시지에 포함된 첨부파일의 메타데이터 목록을 반환
    public List<AttachmentDto> listAttachments(Credential credential, String messageId) throws IOException {
        Gmail service = buildGmailService(credential);
        Message message = service.users().messages().get(USER_ID, messageId).setFormat("full").execute();
        return getAttachments(message.getPayload(), messageId); //이메일 첨부파일 리스트 반환
    }

    //특정 첨부파일의 실제 데이터를 byte 배열로 반환
    public byte[] getAttachmentBytes(Credential credential, String messageId, String attachmentId) throws IOException {
        Gmail service = buildGmailService(credential);
        MessagePartBody body = service.users().messages().attachments()
                .get(USER_ID, messageId, attachmentId).execute();
        return Base64.decodeBase64(body.getData());
    }

    private Gmail buildGmailService(Credential credential) {
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();
    }

    private EmailDto parseMessageToDto(Message message) {
        EmailDto emailDto = new EmailDto();
        emailDto.setId(message.getId());
        emailDto.setSubject(getHeader(message, "Subject"));
        emailDto.setFrom(getHeader(message, "From"));
        emailDto.setBody(getBody(message.getPayload()));
        // getAttachments는 private 헬퍼 메서드로 유지하고, 목록 조회는 public listAttachments를 통해 제공
        emailDto.setAttachments(getAttachments(message.getPayload(), message.getId()));
        return emailDto;
    }

    private String getHeader(Message message, String name) {
        return message.getPayload().getHeaders().stream()
                .filter(header -> header.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(header -> header.getValue())
                .orElse("N/A");
    }

    private String getBody(MessagePart payload) {
        if (payload == null) return "";

        //body가 단순 텍스트/html 형식인 경우 바로 처리
        if (payload.getMimeType().startsWith("text/")) {
            if (payload.getBody() != null && payload.getBody().getData() != null) {
                return new String(Base64.decodeBase64(payload.getBody().getData()), StandardCharsets.UTF_8);
            }
        }

        //여러 부분으로 나눠진 형식인 경우
        if (payload.getMimeType().startsWith("multipart/")) {
            List<MessagePart> parts = payload.getParts();
            if (parts == null) {
                return "";
            }

            //multipart/alternative인 경우, 하나만 선택
            if(payload.getMimeType().equalsIgnoreCase("multipart/alternative")) {
                for(MessagePart part : parts) {
                    if(part.getMimeType().equalsIgnoreCase("text/html")) {
                        return getBody(part);
                    }
                }
                //html 없다면 text/plain 찾는다.
                for(MessagePart part : parts) {
                    if(part.getMimeType().equalsIgnoreCase("text/plain")) {
                        return getBody(part);
                    }
                }
            }
            //그외 - 첨부파일이 섞인경우(multipart/mixed)
            StringBuilder bodyBuilder = new StringBuilder();
            // 재귀적으로 본문 탐색
            for (MessagePart part : parts) { //첨부파일 있다면 건너뛴다.
                if(part.getFilename() == null || part.getFilename().isEmpty()) {
                    bodyBuilder.append(getBody(part));
                }
            }
            return bodyBuilder.toString();
        }
        return ""; // 이메일 본문을 찾지 못한 경우
    }

    private List<AttachmentDto> getAttachments(MessagePart payload, String messageId) {
        List<AttachmentDto> attachments = new ArrayList<>();
        if (payload == null) {
            return attachments;
        }

        // 현재 파트가 첨부파일인지 확인
        if (payload.getFilename() != null && !payload.getFilename().isEmpty() && payload.getBody() != null && payload.getBody().getAttachmentId() != null) {
            String attachmentId = payload.getBody().getAttachmentId();
            attachments.add(new AttachmentDto(payload.getFilename(), messageId, attachmentId));
        }

        // 중첩된 파트(parts)가 있다면 재귀적으로 탐색
        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                attachments.addAll(getAttachments(part, messageId));
            }
        }
        return attachments;
    }

}
