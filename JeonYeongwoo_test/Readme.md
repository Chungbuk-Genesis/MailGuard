************* 2025-12-03 DB 쿼리 blocked-domain 부분에 수정 있으니까 참고해주세요 ********
# 필독 Application.properties 설정

카카오톡에 공유한 credentials.json 파일은 포함되지 않았습니다 << resources 위치에 넣어주세요



Application.properties 파일의 설정을 아래와 같은 형태로 맞춰주세요요 

```
openai.api.key= // 채워주세요

spring.application.name=MailGuard
spring.datasource.url=jdbc:mysql://localhost:3306/MailGuardDB
spring.datasource.username= // 채워주세요
spring.datasource.password= // 채워주세요
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.jdbc.batch_size=1000
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# 도메인 리스트 업데이트용
importer.files=classpath:/data/http_black_domain.csv,classpath:/data/https_black_domain.csv
importer.has-header=true
importer.delimiter=,
importer.domain-column=domain



# https://console.cloud.google.com/welcome?project=fleet-acumen-459615-q9
# 구글 메일 연동

# 위 링크 들어가서 -> 프로젝트 이름 클릭 후 -> uri 에 하단의 내용 추가해줘야 동작함.
google.client-id= // 채워주세요
google.client-secret= // 채워주세요
google.project_id= // 채워주세요
# google.redirect-uri=http://localhost:8080/login/oauth2/code/google
google.redirect-uri=http://localhost:8080/api/gmail/oauth2/callback
google.scopes=https://www.googleapis.com/auth/gmail.readonly

# https://www.virustotal.com/gui/home/upload -> 우측 상단 프로필 클릭 -> API Key -> 복사
virustotal.api.key= // 채워주세요
virustotal.api.base-url=https://www.virustotal.com/api/v3

###########################################
#             SMTP (보내기) - Gmail       
###########################################
# 메일 전송용 메일 설정입니다
# https://kincoding.com/entry/Google-Gmail-SMTP-%EC%82%AC%EC%9A%A9%EC%9D%84-%EC%9C%84%ED%95%9C-%EC%84%B8%ED%8C%85-2025%EB%85%84-%EB%B2%84%EC%A0%84
# ^^^ 참고해서 세팅해주셔야 됩니다. 아래 username 에 이메일, password에 나온 키값 넣어주세요
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username= // 채워주세요
spring.mail.password= // 채워주세요

spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true

spring.mail.default-encoding=UTF-8
spring.mail.protocol=smtp
spring.mail.debug=true

# ✅ 추가 1: 이 호스트 인증서는 신뢰하겠다고 명시
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com

# ✅ 추가 2: TLS 버전 명시 (환경에 따라 도움 되는 경우 있음)
spring.mail.properties.mail.smtp.ssl.protocols=TLSv1.2

###########################################
#             IMAP (받기) - Naver
###########################################

# 여기는 수정 필요합니다. 좀 바꿨는데 정상적으로 작동을 안해용
naver.imap.host=imap.naver.com
naver.imap.port=993
naver.imap.ssl.enable=true
naver.imap.ssl.trust=imap.naver.com

###########################################
# (선택) 데모 계정
###########################################
naver.demo.email= // 채워주세요
naver.demo.app-password= // 채워주세요

app.base-url=http://localhost:8080
```

실행환경<br>
--------------------------------------
jdk 버전 : java version "22.0.2" <br>
개발 툴 : Spring Tool Suite 4 (spring-tools-for-eclipse-4.32.0.RELEASE-e4.37.0-win32.win32.x86_64)


실행 방법<br>
--------------------------------------
### 1. 홈페이지 확인 방법

스프링 부트 파일 다운로드, 임포트 후 <br/>
http://localhost:8080/ <br/>
들어가시면 임시로 만든 템플릿 확인 가능합니다.

### 2. url blocker 기능 확인
0. application.properties 내 mySQL 아이디 및 비밀번호 입력

1. mysql 쿼리 실행
```
CREATE DATABASE IF NOT EXISTS MailGuardDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; 

USE MailGuardDB;

-- 차단용 도메인 저장
CREATE TABLE blocked_domain (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme VARCHAR(10) NOT NULL,           -- 'HTTP', 'HTTPS', 'BOTH' 등
    domain VARCHAR(255) NOT NULL,          -- 차단 도메인명
    source VARCHAR(100),                   -- 파일명 등 (optional)
    note VARCHAR(500),                     -- 비고/설명 (optional)
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_scheme_domain UNIQUE (blocked_domainscheme, domain)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;

-- db 드랍하고 다시 만들어주세요 순서 이 순서대로 실행 안하면 오류나요 fk 관계 있어서
drop table email_verification_token;
drop table oauth_tokens;
drop table user;

-- 유저 테이블
CREATE DATABASE IF NOT EXISTS MailGuardDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; 
USE MailGuardDB;

drop table email_verification_token;
drop table oauth_tokens;
drop table user;

-- 유저 테이블
CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    correct INT DEFAULT 0,
    incorrect INT DEFAULT 0,
    enabled bit(1) DEFAULT 0,
    admin_check boolean DEFAULT FALSE  -- 2025-12-03 admin 권한 업데이트
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 토큰 테이블
CREATE TABLE oauth_tokens (
    user_id INT PRIMARY KEY,
    access_token VARCHAR(2048),
    refresh_token VARCHAR(1024),
    id_token VARCHAR(2048),
    token_type VARCHAR(50),
    expires_in INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT user_id
    FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 여긴 username admin 으로 한 경우 실행해 주세요
Update user
Set admin_check = TRUE
where username = 'admin';

```
3. springboot 실행

4. cmd 에서 다음 문장 입력
curl -X POST http://localhost:8080/api/admin/import/all

  inserted 여부 관련 문장 나오면 정상적으로 동작 한 거에요

실행 후 
http://localhost:8080/api/url/check?url=https://naver.com << 위험 유무 판별
http://localhost:8080/api/url/check?url=https://{url} << url 위치에 접속 원하는 url 입력 시 차단알림 표시 

            
---------------------------------------------------
악성 이메일 검증 시스템(고재현 조원님 개발)
```
MailGuard
├── config
│   └── GmailConfig
├── controller
│   ├── MailController
│   ├── NaverMailController
│   └── GmailOAuthController
├── Dto
│   └── EmailDto
└── service
    ├── GmailOAuthService
    ├── NaverImapService
    └── PhishingDetectionService
```
URL Blocker
```
MailGuard
├── MailGuardApplication
├── config
│   ├── UrlSecurityIntercepter
│   └── WebMvcConfig
├── controller
│   └── UrlValidationController
└── service
    └── UrlSecurityService
```

----------------------------------------------------------

block domain update 
```
MailGuard
├── config
│   └── ImportProperties
├── controller
│   ├── ImportAdminController
│   └── ImporterConfigEcho
├── domain
│   └── BlockedDomain
├── Model
│   └── BlockedDomainRepository
└── service
    └── CsvImportService
```

실제 페이지 백엔드
```
MailGuard
├── controller
│   ├── BasicPageController (홈페이지 -> 다른 페이지 리다이렉션)
│   └── UserController (유저 데이터 컨트롤)
├── entity
│   └── User
├── Model
│   └── UserRepository
└── service
    └── UserService.java
```

... 추후 업데이트 하겠습니다
-------------------------------------------------------------





