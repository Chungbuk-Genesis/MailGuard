package org.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name="virus_total_data")
public class VirusTotalReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ID 자동 증가 (Auto Increment)
    private Long id;

    @Column(name = "md5", length = 32)
    private String md5;

    @Column(name = "sha256", length = 64, unique = true) // sha256은 고유해야 함
    private String sha256;

    @Column(name = "meaningful_name")
    private String meaningfulName;

    @Column(name = "malicious_count")
    private int malicious;

    @Column(name = "suspicious_count")
    private int suspicious;

    @Column(name = "harmless_count")
    private int harmless;

    @Column(name = "undetected_count")
    private int undetected;

    @Column(name = "last_analysis_date")
    private String lastAnalysisDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public VirusTotalReport() {}

    @PrePersist // 데이터가 처음 저장될 때 자동으로 호출
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getMeaningfulName() {
        return meaningfulName;
    }

    public void setMeaningfulName(String meaningfulName) {
        this.meaningfulName = meaningfulName;
    }

    public int getMalicious() {
        return malicious;
    }

    public void setMalicious(int malicious) {
        this.malicious = malicious;
    }

    public int getSuspicious() {
        return suspicious;
    }

    public void setSuspicious(int suspicious) {
        this.suspicious = suspicious;
    }

    public int getHarmless() {
        return harmless;
    }

    public void setHarmless(int harmless) {
        this.harmless = harmless;
    }

    public int getUndetected() {
        return undetected;
    }

    public void setUndetected(int undetected) {
        this.undetected = undetected;
    }

    public String getLastAnalysisDate() {
        return lastAnalysisDate;
    }

    public void setLastAnalysisDate(String lastAnalysisDate) {
        this.lastAnalysisDate = lastAnalysisDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
