// src/main/java/com/example/urlsecurity/repo/BlockedDomainRepository.java
package com.example.MailGuard.repo;

import com.example.MailGuard.domain.BlockedDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedDomainRepository extends JpaRepository<BlockedDomain, Long> {
    Optional<BlockedDomain> findBySchemeAndDomain(String scheme, String domain);
    List<BlockedDomain> findAllByScheme(String scheme);
    
    // 전체 domain 값만 리스트로 가져오기
    @Query("SELECT b.domain FROM BlockedDomain b")
    List<String> findAllDomains();
    
}
