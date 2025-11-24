package com.example.MailGuard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component  // 빈 등록 (이렇게 하면 main에 @EnableConfigurationProperties 불필요)
@ConfigurationProperties(prefix = "importer")
public class ImporterProperties {

    private List<String> files;
    private boolean hasHeader;
    private String delimiter;
    private String domainColumn;

    public List<String> getFiles() { return files; }
    public void setFiles(List<String> files) { this.files = files; }

    public boolean isHasHeader() { return hasHeader; }
    public void setHasHeader(boolean hasHeader) { this.hasHeader = hasHeader; }

    public String getDelimiter() { return delimiter; }
    public void setDelimiter(String delimiter) { this.delimiter = delimiter; }

    public String getDomainColumn() { return domainColumn; }
    public void setDomainColumn(String domainColumn) { this.domainColumn = domainColumn; }
}