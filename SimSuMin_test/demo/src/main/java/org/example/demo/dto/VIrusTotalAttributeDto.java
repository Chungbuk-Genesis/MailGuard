package org.example.demo.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VIrusTotalAttributeDto {
    private String md5;
    private String sha256;
    @JsonProperty("meaningful_name")
    private String meaningfulName;
    @JsonProperty("last_analysis_stats")
    private LastAnalysisStatsDto lastAnalysisStats;
    @JsonProperty("last_analysis_date")
    private long lastAnalysisDate;

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

    public LastAnalysisStatsDto getLastAnalysisStats() {
        return lastAnalysisStats;
    }

    public void setLastAnalysisStats(LastAnalysisStatsDto lastAnalysisStats) {
        this.lastAnalysisStats = lastAnalysisStats;
    }

    public long getLastAnalysisDate() {
        return lastAnalysisDate;
    }

    public void setLastAnalysisDate(long lastAnalysisDate) {
        this.lastAnalysisDate = lastAnalysisDate;
    }

}
