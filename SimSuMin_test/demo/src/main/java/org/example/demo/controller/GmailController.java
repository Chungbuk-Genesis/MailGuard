package org.example.demo.controller;

import org.example.demo.dto.AttachmentDto;
import org.example.demo.dto.EmailDto;
import org.example.demo.dto.ReportDto;
import org.example.demo.service.GmailService;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.demo.service.VirusTotalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;


@RestController
@RequestMapping("/")
public class GmailController {

    private final AuthorizationCodeFlow flow; //Google OAuth 2.0 인증 관리 객체
    private final GmailService gmailService; //Gmail API 관련 로직 처리하는 서비스
    private final String redirectUri; //OAuth 2.0 인증 성공후 redirection될 URI

    private static final String USER_ID = "me"; //사용자 ID 임의배정
    private static final String CREDENTIAL_SESSION_KEY = "google_credential"; //세션에 credential 객체 저장하기 위한 키
    private final VirusTotalService virusTotalService;

    @Autowired
    public GmailController(
            AuthorizationCodeFlow flow,
            GmailService gmailService,
            @Value("${google.redirect-uri}") String redirectUri, VirusTotalService virusTotalService) {
        this.flow = flow;
        this.gmailService = gmailService;
        this.redirectUri = redirectUri;
        this.virusTotalService = virusTotalService;
    }

    //google Oauth2 로그인 - google 인증 페이지로 이동
    @GetMapping("/login/google")
    public void login(HttpServletResponse response) throws IOException {
        String url = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();
        response.sendRedirect(url);
    }

    /*
     * google 로그인 성공 후 인증 코드 받음
     * code: Google로부터 받은 인증 코드
     */
    @GetMapping("/login/oauth2/code/google")
    public void oauthCallback(@RequestParam("code") String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        Credential credential = flow.createAndStoreCredential(tokenResponse, USER_ID);

        request.getSession().setAttribute(CREDENTIAL_SESSION_KEY, credential); //session에 인증받은 credential 저장
        response.sendRedirect("/"); //인증 완료 후 메인 페이지('/')로 redirection
    }

    /*
     * 이메일 조회
     * session : credential 정보 가져오기 위한 세션
     * EmailDTO : 이메일 데이터 파싱위해 만들어 놓은 Data Transfer Object(객체)
     */
    @GetMapping("/api/latest-email")
    public ResponseEntity<EmailDto> getLatestEmail(HttpSession session) {
        Credential credential = getRefreshedCredential(session);
        if (credential == null) { //인증 정보 없으면 Unathorized 응답 : 에러코드 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            EmailDto email = gmailService.getLatestEmail(credential);
            if (email == null) { //email 없으면 notFound 응답 : 에러코드 404
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(email);
        } catch (IOException e) {
            // 서버 내부 오류 발생 시 : 에러코드 500
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); //EmailDTO를 담은 responseEntity을 return 한다.
        }
    }

    //특정 이메일의 첨부파일 목록을 조회하는 API - 첨부파일 여러개 일 수 있으므로 목록.
    @GetMapping("/api/attachments/{messageId}")
    public ResponseEntity<List<AttachmentDto>> getAttachmentsList(
            @PathVariable("messageId") String messageId,
            HttpSession session) {

        Credential credential = getRefreshedCredential(session);
        if (credential == null) { //인증 정보 없으면 Unathorized 응답 : 에러코드 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<AttachmentDto> attachments = gmailService.listAttachments(credential, messageId);
            return ResponseEntity.ok(attachments);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 메일의 첨부파일 vt api로 scan - sha256값으로 검색
    @GetMapping("/api/attachment/scan")
    public ResponseEntity<List<ReportDto>> scanAttachment(
            @RequestParam("msgId") String messageId,
            @RequestParam("attId") String attachmentId,
            @RequestParam("filename") String filename,
            @RequestParam(value = "password", required = false) String password,
            HttpSession session) {

        Credential credential = getRefreshedCredential(session);
        if (credential == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            byte[] fileBytes = gmailService.getAttachmentBytes(credential, messageId, attachmentId);
            List<ReportDto> reports = new ArrayList<>();

            if (filename != null && filename.toLowerCase().endsWith(".zip")) {
                reports.addAll(scanZipFileContents(fileBytes, password));
            } else {
                reports.add(scanSingleFile(fileBytes, filename));
            }

            return ResponseEntity.ok(reports);

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    private List<ReportDto> scanZipFileContents(byte[] zipBytes, String password) throws IOException, NoSuchAlgorithmException {
        List<ReportDto> reports = new ArrayList<>();
        File tempFile = null;

        try {
            // byte[]를 임시 파일로 변환
            tempFile = Files.createTempFile("temp-zip-", ".zip").toFile();
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(zipBytes);
            }

            // ZipFile 객체 생성
            ZipFile zipFile = new ZipFile(tempFile);

            // 암호화 여부 확인 및 암호 설정
            if (zipFile.isEncrypted()) {
                if (password == null || password.isEmpty()) {
                    // 암호가 필요한데 제공되지 않음
                    reports.add(createSpecialReport("암호 필요 (Password Required)", null));
                    return reports;
                }
                zipFile.setPassword(password.toCharArray());
            }

            // 압축 해제 및 내부 파일 스캔
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            for (FileHeader header : fileHeaders) {
                if (!header.isDirectory()) {
                    try (InputStream is = zipFile.getInputStream(header)) {
                        // InputStream을 byte[]로 변환
                        byte[] entryBytes = is.readAllBytes();
                        // 내부 파일 스캔
                        ReportDto report = scanSingleFile(entryBytes, header.getFileName());
                        reports.add(report);
                    }
                }
            }
        } catch (ZipException e) {
            // 암호가 틀렸거나 파일이 손상된 경우
            reports.clear(); // 부분 리포트 제거
            reports.add(createSpecialReport("암호 틀림 / Zip 파일 손상", null));
            return reports;
        } finally {
            // 임시 파일 삭제
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        return reports;
    }

    private ReportDto createSpecialReport(String status, String sha256) {
        ReportDto report = new ReportDto();
        report.setSHA256(sha256);
        report.setMeaningfulName(status);
        report.setLastAnalysisDate("N/A"); // 프론트에서 구분하기 위한 값
        report.setMalicious(0);
        report.setSuspicious(0);
        report.setHarmless(0);
        report.setUndetected(0);
        return report;
    }


    private ReportDto scanSingleFile(byte[] fileBytes, String filename) throws NoSuchAlgorithmException {
        try {
            String sha256Hash = calculateSHA256(fileBytes);
            ReportDto report = virusTotalService.getFileReport(sha256Hash).block();

            String vtName = report.getMeaningfulName();
            if (vtName != null && !vtName.isEmpty() && !vtName.equals(filename)) {
                report.setMeaningfulName(filename + " (as: " + vtName + ")");
            } else {
                report.setMeaningfulName(filename);
            }
            return report;

        } catch (WebClientResponseException.NotFound e) {
            // VirusTotal에 리포트가 없는 경우 (createSpecialReport 헬퍼 사용)
            String sha256 = calculateSHA256(fileBytes);
            ReportDto notFoundReport = createSpecialReport("Report Not Found", sha256);
            notFoundReport.setMeaningfulName(filename); // 상태 대신 파일명으로 덮어쓰기
            return notFoundReport;
        }
    }



    //첨부파일 다운로드 API
    @GetMapping("/api/attachment/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @RequestParam("msgId") String messageId, //첨부파일의 메일 ID
            @RequestParam("attId") String attachmentId, //첨부파일의 ID
            @RequestParam("filename") String filename, //파일명(다운로드시 저장명)
            HttpSession session) { //session에 저장된 credential 정보를 가져오기 위한 세션

        Credential credential = getRefreshedCredential(session);
        if (credential == null) { //인증 정보 없으면 Unathorized 응답 : 에러코드 401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            //
            byte[] fileBytes = gmailService.getAttachmentBytes(credential, messageId, attachmentId);

            //다국어 파일명을 지원하기 위해 파일명 인코딩
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;

            return ResponseEntity.ok()
                    .header("Content-Disposition", contentDisposition)
                    .body(fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //세션에서 Credential을 가져오고, 필요 시 토큰을 갱신하는 헬퍼 메서드
    private Credential getRefreshedCredential(HttpSession session) { //session은 현재 HttpSession
        Credential credential = (Credential) session.getAttribute(CREDENTIAL_SESSION_KEY);
        if (credential == null) {
            return null;
        }
        try { //access token 만료 시간이 60초 이내 남았다면 갱신
            if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
                if (!credential.refreshToken()) {
                    return null; // 리프레시 실패 null 반환
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return credential;
    }

    // 첨부파일 가져와 sha 256 값으로 연산
    private String calculateSHA256(byte[] fileBytes) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodehash = digest.digest(fileBytes);
        return bytesToHex(encodehash);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);

        }
        return hexString.toString();
    }

}