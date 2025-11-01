// src/main/java/com/example/urlsecurity/controller/ImportAdminController.java
package com.example.MailGuard.controller;

import org.springframework.web.bind.annotation.*;

import com.example.MailGuard.service.CsvImportService;

import java.util.Map;


// 2. cmd 에서 다음 문장 입력
// curl -X POST http://localhost:8080/api/admin/import/all
@RestController
@RequestMapping("/api/admin/import")
public class ImportAdminController {

    private final CsvImportService importService;

    public ImportAdminController(CsvImportService importService) {
        this.importService = importService;
    }

    /** application.yml의 importer.files 전체 임포트 */
    @PostMapping("/all")
    public Map<String, Object> importAll() throws Exception {
    	
        return importService.importAll();
    }

    /** 특정 파일만 임포트 (임의 경로 허용) */
    @PostMapping("/one")
    public Map<String, Integer> importOne(@RequestParam String path) throws Exception {
        return importService.importOne(path);
    }
}
