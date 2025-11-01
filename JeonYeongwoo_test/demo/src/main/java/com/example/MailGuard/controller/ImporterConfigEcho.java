// src/main/java/com/example/urlsecurity/controller/ImporterConfigEcho.java
package com.example.MailGuard.controller;

import org.springframework.web.bind.annotation.*;

import com.example.MailGuard.config.ImporterProperties;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
public class ImporterConfigEcho {

    private final ImporterProperties props;

    public ImporterConfigEcho(ImporterProperties props) {
        this.props = props;
    }

    @GetMapping
    public Map<String, Object> echo() {
        return Map.of(
            "files", props.getFiles(),
            "hasHeader", props.isHasHeader(),
            "delimiter", props.getDelimiter(),
            "domainColumn", props.getDomainColumn()
        );
    }
}
