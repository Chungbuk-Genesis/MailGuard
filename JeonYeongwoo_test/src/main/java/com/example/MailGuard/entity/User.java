package com.example.MailGuard.entity;

// 유저 정보에 대한 파일입니다.

// 저장 정보 : id, username, password, email, createdAt, correct, incorrect
//		이에 대한 getter, setter 포함
/*
 * 사용 db
 * CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    correct INT DEFAULT 0,
    incorrect INT DEFAULT 0,
    enabled bit(1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * 
 * */
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    private LocalDateTime createdAt;

    private Integer correct = 0;

    private Integer incorrect = 0;
    
    @Column(nullable = false)
    private boolean admin_check = false;
    
    
    // ✅ 이메일 인증 여부 (기본 false)
    @Column(nullable = false)
    private boolean enabled = false;


    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCorrect() {
        return correct;
    }

    public void setCorrect(Integer correct) {
        this.correct = correct;
    }

    public Integer getIncorrect() {
        return incorrect;
    }

    public void setIncorrect(Integer incorrect) {
        this.incorrect = incorrect;
    }
    
    
    // 이메일 활성화 시 enabled 시키기 위함
    //2025-11-24 추가
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // admin_check 세팅
    public boolean getAdmin() {
        return admin_check;
    }
    
    
    
    public void setAdmin(boolean admin_check) {
        this.admin_check = admin_check;
    }
    
}

