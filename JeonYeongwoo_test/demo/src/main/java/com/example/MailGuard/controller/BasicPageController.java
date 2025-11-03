package com.example.MailGuard.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.MailGuard.entity.User;

import jakarta.servlet.http.HttpSession;



@Controller
public class BasicPageController {
	

    @GetMapping("/")
    public String showWelcomePage() {
        return "Basic/WelcomePage";  
    }


	@GetMapping("/guest")
	public String showGuestIndex(Model model) {
	    return "Basic/GuestIndex";
	}
	
	@GetMapping("/home")
	public String showHomeIndex(HttpSession session, Model model) {
	    User user = (User) session.getAttribute("user");
	    if (user == null) return "redirect:/login"; 
	    session.setAttribute("userId", user.getId());

	    model.addAttribute("user", user);
	    return "Basic/HomeIndex";
	}
	
	

}
