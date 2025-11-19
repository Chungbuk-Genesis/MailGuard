package com.example.MailGuard.controller;

import com.example.MailGuard.entity.User;
import com.example.MailGuard.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    
    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("user", new User());
        return "Basic/SignupPage";
    }

    @PostMapping("/signup")
    public String handleSignup(@ModelAttribute User user, Model model) {
        boolean success = userService.registerUser(user);
        if (!success) {
            model.addAttribute("error", "Username or Email already exists.");
            return "Basic/SignupPage";
        }
        return "redirect:/guest";
    }
    

    @GetMapping("/login")
    public String showLoginForm() {
        return "Basic/LoginPage";
    }

    // 얘가 안도는데?
    @PostMapping("/login")
    public String loginUser(@RequestParam("username") String username,
                            @RequestParam("password") String password,
                            HttpSession session,
                            Model model) {
        User user = userService.getUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("user", user);
            return "redirect:/home";
        } else {
            model.addAttribute("error", "Invalid credentials");
            return "Basic/LoginPage";
        }
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

    
}
