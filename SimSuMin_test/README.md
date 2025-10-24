
### 구현된 기능
---

1. Virus Total API 연동
2. Virus Total 결과값 파싱
3. Virus Total 결과값 DB에 저장 -> 이부분은 저장하는 기능은 구현했으나, 메일에서 받아온 첨부파일 스캔하는 부분과는 연동 X(요청보내고 받는 url이 다름. 추후 수정 예정)
4. gmail 메일에서 받아온 첨부파일 sha256 해시값 계산
5. Malicous | Supisus 값이 존재할 때, 위험하다고 판단
6. 압축파일이어도 분석가능 -> 만약 압축파일 해제시 비밀번호 필요하면 입력받도록 수정
<br></br>


### 개발환경
---

- JDK : zulu JDK 21, Oracl JDK 22
- IDE : Intellij
- DB : mysql
<br></br>

### 실행화면
---
![첨부파일 스캔](https://github.com/sim4110/MailGuard/blob/main/SimSuMin_test/filescan.png)



