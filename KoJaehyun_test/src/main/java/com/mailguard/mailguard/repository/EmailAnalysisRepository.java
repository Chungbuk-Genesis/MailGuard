package com.mailguard.mailguard.repository;

import com.mailguard.mailguard.entity.EmailAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailAnalysisRepository extends JpaRepository<EmailAnalysis, Long> {
    
    Optional<EmailAnalysis> findByMessageId(String messageId);
    
    List<EmailAnalysis> findByRiskLevel(String riskLevel);
    
    List<EmailAnalysis> findBySender(String sender);
    
    List<EmailAnalysis> findByIsReanalyzed(Boolean isReanalyzed);
}
