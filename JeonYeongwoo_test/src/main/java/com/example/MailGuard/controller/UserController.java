package com.example.MailGuard.controller;

import com.example.MailGuard.entity.User;
import com.example.MailGuard.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

// 유저 정보 관리용 컨트롤러 
// 내용 : 회원가입, 로그인, 로그아웃, 프로필 수정 기능 포함. 

// 2025-11-24 해싱 이용한 비밀번호 암호화 진행함.

@Controller
public class UserController {

	 @Autowired
	 private UserService userService;

	 @Autowired
	 private PasswordEncoder passwordEncoder;
    
    @GetMapping("/signup")
    public String showSignupForm(Model model) {
    	System.out.println("회원가입 페이지로 이동합니다._1");
    	
        model.addAttribute("user", new User());
        return "Basic/SignupPage";
    }

    @PostMapping("/signup")
    public String handleSignup(@ModelAttribute User user, Model model) {
    	System.out.println("회원가입 페이지로 이동합니다._2");
        boolean success = userService.registerUser(user);
        if (!success) {
            model.addAttribute("error", "Username or Email already exists.");
            System.out.println("회원가입에 실패했습니다.");
            return "Basic/SignupPage";
        }
        // 회원가입 성공 후: 로그인 페이지로 이동 (이메일 인증 후 로그인)
        System.out.println("로그인 페이지로 이동합니다.");
        return "redirect:/login";
        
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "Basic/LoginPage";
    }

    @PostMapping("/login")
    public String loginUser(@RequestParam("username") String username,
                            @RequestParam("password") String password,
                            HttpSession session,
                            Model model) {

        User user = userService.getUserByUsername(username);

        if (user == null) {
            model.addAttribute("error", "존재하지 않는 사용자입니다.");
            return "Basic/LoginPage";
        }

        // ✅ 이메일 미인증 처리
        if (!user.isEnabled()) {
            model.addAttribute("error", "이메일 인증 후 로그인할 수 있습니다. 메일함을 확인해주세요.");
            return "Basic/LoginPage";
        }

        // ✅ 해시된 비밀번호 비교
        if (!passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "비밀번호가 올바르지 않습니다.");
            return "Basic/LoginPage";
        }

        session.setAttribute("user", user);
        return "redirect:/home";
    }

    

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId != null) {
        }
        session.invalidate();
        return "redirect:/";
    }
    


    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", userService.findById(user.getId()));
        return "PersonalProfile/ProfilePage";
    }

    @GetMapping("/edit")
    public String showEditProfileForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", userService.findById(user.getId()));
        return "PersonalProfile/EditProfilePage";
    }

    @PostMapping("/edit")
    public String handleEditProfile(@ModelAttribute User updatedUser, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/login";
        }
        updatedUser.setId(sessionUser.getId());  // 아이디 고정
        userService.updateUserProfile(updatedUser);
        session.setAttribute("user", userService.findById(sessionUser.getId()));
        return "redirect:/profile";
    }

    // 이메일 인증 결과를 보여주는 페이지
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token,
                              Model model) {
        String message = userService.verifyEmail(token);
        model.addAttribute("message", message);
        return "PersonalProfile/VerifyResultPage";
    }
    
    // 2025-12-01 계정삭제 로직 추가
    
	// ✅ 계정 탈퇴
	    @PostMapping("/delete-account")
	    public String deleteAccount(HttpSession session) {
	        User sessionUser = (User) session.getAttribute("user");
	        if (sessionUser == null) {
	            return "redirect:/login";
	        }
	
	        // DB에서 유저 삭제
	        userService.deleteUser(sessionUser.getId());
	
	        // 세션 비우기
	        session.invalidate();
	
	        // 홈(또는 메인 페이지)로 이동
	        return "redirect:/";
	    }

    
}
