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


-- 유저 테이블
CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    correct INT DEFAULT 0,
    incorrect INT DEFAULT 0
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


-------------------------------------------------------------

