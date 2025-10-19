package org.example.demo.dto;

public class ReportDto {

    private String md5;
    private String SHA256;
    private String meaningfulName;
    private int malicious;
    private int suspicious;
    private int harmless;
    private int undetected;
    private String lastAnalysisDate;

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSHA256() {
        return SHA256;
    }

    public void setSHA256(String SHA256) {
        this.SHA256 = SHA256;
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
}

