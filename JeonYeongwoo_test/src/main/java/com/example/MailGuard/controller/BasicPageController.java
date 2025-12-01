package com.example.MailGuard.controller;

import com.example.MailGuard.entity.User;
import com.google.api.client.auth.oauth2.Credential;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// 웹페이지 리다이렉션 하는 컨트롤러


@Controller
public class BasicPageController {
	private static final String CREDENTIAL_SESSION_KEY = "google_credential";

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

	@GetMapping("/file")
	public String showGmailIndex(HttpSession session, Model model) {
		User user = (User) session.getAttribute("user");
		if (user == null) return "redirect:/login";
		Credential credential = (Credential) session.getAttribute(CREDENTIAL_SESSION_KEY);
		if (credential == null) return "redirect:/api/gmail/login";

		model.addAttribute("user", user);
		return "Basic/GmailPage";
	}

	@GetMapping("/mail")
	public String showMailIndex(HttpSession session, Model model) {
		User user = (User) session.getAttribute("user");
		if(user == null) return "redirect:/login";
		Credential credential = (Credential) session.getAttribute(CREDENTIAL_SESSION_KEY);
		if (credential == null) return "redirect:/api/gmail/login";


		model.addAttribute("user", user);
		return "Basic/mail-viewer";
	}
	
	
	

}
