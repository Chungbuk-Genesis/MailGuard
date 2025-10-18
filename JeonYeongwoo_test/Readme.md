데이터 업데이트

application.properties 내 mySQL 아이디 및 비밀번호 입력

1. mysql 쿼리 실행
CREATE DATABASE IF NOT EXISTS MaliciousDomain CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE MaliciousDomain;

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

2. springboot 실행

3. cmd 에서 다음 문장 입력
curl -X POST http://localhost:8080/api/admin/import/all

  inserted 여부 관련 문장 나오면 정상적으로 동작 한 거에요

실행 후 
http://localhost:8080/api/url/check?url=https://naver.com << 위험 유무 판별
http://localhost:8080/api/url/check?url=https://{url} << url 위치에 접속 원하는 url 입력 시 차단알림 표시 

            
---------------------------------------------------

URL Blocker

urlsecurity 내
UrlSecurityApplication

config 내
UrlSecurityIntercepter
WebMvcConfig

controller 내
UrlValidationController

service 내 
UrlSecurityService

----------------------------------------------------------

block domain update 저장 관련 파일
config 내
ImportProperties

controller 내 
ImportAdminController
ImporterConfigEcho -> 로그 저장용

domain 내
BlockedDomain

Model내 
BlockedDomainRepository

service 내
UrlSecurityService.java

-------------------------------------------------------------

