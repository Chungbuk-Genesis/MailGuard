# MailGuard - 악성 이메일 검증 시스템

악성 이메일(피싱/스미싱/악성코드)을 자동으로 탐지하고 분석하는 PC 응용프로그램

---

## 구현된 기능

1. Gmail API 연동 (OAuth 2.0 인증)
2. Naver IMAP 연동
3. 메일 본문에서 URL 자동 추출
4. 단축 URL 및 의심 도메인 탐지 (.xyz, .top 등)
5. 한글/영문 피싱 키워드 탐지 ("긴급", "계좌", "urgent" 등)
6. 위험도 점수화 (0-100점) 및 3단계 등급 분류 (SAFE/SUSPICIOUS/DANGEROUS)
---

## 개발환경

- **JDK**: Java 25
- **Framework**: Spring Boot 3.2.0
- **IDE**: IntelliJ IDEA
- **Frontend**: React 18 (CDN)
