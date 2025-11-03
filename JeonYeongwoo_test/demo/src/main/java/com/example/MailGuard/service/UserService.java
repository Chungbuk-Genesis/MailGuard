package com.example.MailGuard.service;

import com.example.MailGuard.entity.User;
import com.example.MailGuard.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public boolean registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername()) ||
            userRepository.existsByEmail(user.getEmail())) {
            return false;
        }

        userRepository.save(user);
        return true;
    }

	public User getUserByUsername(String username) {
		  return userRepository.findByUsername(username);
	}

    public User findById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }

    public void updateUserProfile(User updatedUser) {
        User user = userRepository.findById(updatedUser.getId()).orElseThrow();
        userRepository.save(user);
    }
}
