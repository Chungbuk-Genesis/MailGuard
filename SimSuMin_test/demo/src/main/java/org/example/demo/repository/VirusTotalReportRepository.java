package org.example.demo.repository;

import org.example.demo.entity.VirusTotalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VirusTotalReportRepository extends JpaRepository<VirusTotalReport, Long> {

    boolean existsBySha256(String sha256);
}
