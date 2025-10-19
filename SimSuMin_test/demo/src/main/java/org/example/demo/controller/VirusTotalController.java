package org.example.demo.controller;

import org.example.demo.dto.ReportDto;
import org.example.demo.service.VirusTotalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/virustotal")
public class VirusTotalController {
    private final VirusTotalService virusTotalService;

    public VirusTotalController(VirusTotalService virusTotalService) {
        this.virusTotalService = virusTotalService;
    }

    // http://localhost:8080/api/virustotal/files/d5e974a3386fc99d2932756ca165a451 이런식으로 테스트 가능.
    @GetMapping("/files/{id}")
    public Mono<ReportDto> getFileReport(@PathVariable String id) {
        return virusTotalService.getFileReport(id);
    }
}
