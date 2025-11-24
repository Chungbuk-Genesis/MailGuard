package com.example.MailGuard.repo;

import com.example.MailGuard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

// 유저 정보에 대한 repository 파일

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
	User findByUsername(String username);
	
}
