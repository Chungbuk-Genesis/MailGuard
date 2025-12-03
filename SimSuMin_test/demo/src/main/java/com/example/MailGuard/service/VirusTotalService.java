package com.example.MailGuard.service;

import com.example.MailGuard.dto.APIResponseDto;
import com.example.MailGuard.dto.LastAnalysisStatsDto;
import com.example.MailGuard.dto.ReportDto;
import com.example.MailGuard.dto.VIrusTotalAttributeDto;
import com.example.MailGuard.entity.VirusTotalReport;
import com.example.MailGuard.repo.VirusTotalReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class VirusTotalService {
    private final WebClient webClient;
    private final VirusTotalReportRepository reportRepository;

    @Value("${virustotal.api.key}")
    private String apiKey;
    public VirusTotalService(WebClient webClient, VirusTotalReportRepository reportRepository) {
        this.webClient = webClient;
        this.reportRepository = reportRepository;
    }

    public Mono<ReportDto> getFileReport(String fileId){
        return webClient.get()
                .uri("/files/{id}", fileId)
                .header("accept", "application/json") // accept 헤더 설정
                .header("x-apikey", apiKey) // VirusTotal API 키 헤더 설정
                .retrieve() // 요청을 실행하고 응답을 받음
                .bodyToMono(APIResponseDto.class)
                .map(this::transformToReport) // DTO로 변환
                .doOnNext(this::saveReport); // DB 저장
    }

    // JSON 결과 -> RerportDto로 변경
    private ReportDto transformToReport(APIResponseDto apiResponse){
        VIrusTotalAttributeDto attribute = apiResponse.getData().getAttributes();
        LastAnalysisStatsDto stats = Objects.requireNonNullElseGet(
                attribute.getLastAnalysisStats(), LastAnalysisStatsDto::new
        );

        ReportDto reportDto = new ReportDto();
        reportDto.setMd5(attribute.getMd5());
        reportDto.setSHA256(attribute.getSha256());
        reportDto.setMalicious(stats.getMalicious());
        reportDto.setMeaningfulName(attribute.getMeaningfulName());
        reportDto.setMalicious(stats.getMalicious());
        reportDto.setSuspicious(stats.getSuspicious());
        reportDto.setHarmless(stats.getHarmless());
        reportDto.setUndetected(stats.getUndetected());

        Instant instant = Instant.ofEpochSecond(attribute.getLastAnalysisDate());
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        reportDto.setLastAnalysisDate(formattedDate);

        return reportDto;
    }

    //DB(table name : virus_total_data)에 데이터 저장
    public void saveReport(ReportDto savedDto){

        boolean alreadyExists = reportRepository.existsBySha256(savedDto.getSHA256());

        if(!alreadyExists){
            VirusTotalReport report = new VirusTotalReport();
            report.setMd5(savedDto.getMd5());
            report.setSha256(savedDto.getSHA256());
            report.setMalicious(savedDto.getMalicious());
            report.setMeaningfulName(savedDto.getMeaningfulName());
            report.setSuspicious(savedDto.getSuspicious());
            report.setHarmless(savedDto.getHarmless());
            report.setUndetected(savedDto.getUndetected());
            report.setLastAnalysisDate(savedDto.getLastAnalysisDate());

            reportRepository.save(report);
        }
    }

}
