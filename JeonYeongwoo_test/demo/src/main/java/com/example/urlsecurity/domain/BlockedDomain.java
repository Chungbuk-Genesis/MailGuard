// src/main/java/com/example/urlsecurity/domain/BlockedDomain.java
package com.example.urlsecurity.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
  name = "blocked_domain",
  uniqueConstraints = @UniqueConstraint(name="uk_scheme_domain", columnNames = {"scheme", "domain"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlockedDomain {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // HTTP / HTTPS / BOTH
    @Column(nullable = false, length = 10)
    private String scheme;

    @Column(nullable = false, length = 255)
    private String domain;

    @Column(length = 100)
    private String source;  // 파일명 등

    @Column(length = 500)
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}