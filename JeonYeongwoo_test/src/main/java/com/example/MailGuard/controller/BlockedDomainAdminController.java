package com.example.MailGuard.controller;

import com.example.MailGuard.domain.BlockedDomain;
import com.example.MailGuard.entity.User;
import com.example.MailGuard.repo.BlockedDomainRepository;
import com.example.MailGuard.service.UrlSecurityService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/blocked-domains")
@RequiredArgsConstructor
public class BlockedDomainAdminController {

    private final BlockedDomainRepository blockedDomainRepository;
    private final UrlSecurityService urlSecurityService;

    /** ✔ 관리자 체크 공통 메서드 */
    private boolean isNotAdmin(User user) {
        return (user == null || !user.getAdmin());
    }

    /** 목록 + 신규 등록 폼 */
    @GetMapping
    public String list(Model model, HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");

        // 관리자 체크
        if (isNotAdmin(sessionUser)) {
            return "redirect:/home";
        }

        model.addAttribute("user", sessionUser);

        List<BlockedDomain> domains = blockedDomainRepository.findAll();
        model.addAttribute("domains", domains);
        model.addAttribute("newDomain", new BlockedDomain());
        return "admin/blocked-domain-admin";
    }

    /** 등록 */
    @PostMapping("/add")
    public String add(@ModelAttribute("newDomain") BlockedDomain newDomain,
                      HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");
        if (isNotAdmin(sessionUser)) {
            return "redirect:/home";
        }

        if (newDomain.getScheme() == null || newDomain.getScheme().isBlank()) {
            newDomain.setScheme("HTTPS");
        }

        if (newDomain.getDomain() == null || newDomain.getDomain().isBlank()) {
            return "redirect:/admin/blocked-domains";
        }

        try {
            blockedDomainRepository.save(newDomain);
            log.info("🟢 차단 도메인 추가됨 : scheme={}, domain={}",
                    newDomain.getScheme(), newDomain.getDomain());
        } catch (Exception e) {
            log.warn("⚠️ 중복 도메인 등록 시도 : {}", e.getMessage());
        }

        urlSecurityService.reloadBlockedDomains();
        return "redirect:/admin/blocked-domains";
    }

    /** 삭제 */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");
        if (isNotAdmin(sessionUser)) {
            return "redirect:/home";
        }

        blockedDomainRepository.deleteById(id);
        urlSecurityService.reloadBlockedDomains();
        log.info("🗑️ 차단 도메인 삭제됨 : id={}", id);

        return "redirect:/admin/blocked-domains";
    }

    /** 수정 페이지 열기 */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");
        if (isNotAdmin(sessionUser)) {
            return "redirect:/home";
        }

        model.addAttribute("user", sessionUser);

        BlockedDomain domain = blockedDomainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ID: " + id));

        model.addAttribute("domain", domain);
        return "admin/blocked-domain-edit";
    }

    /** 수정 POST */
    @PostMapping("/edit/{id}")
    public String editSubmit(@PathVariable Long id,
                             @ModelAttribute("domain") BlockedDomain edited,
                             HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");
        if (isNotAdmin(sessionUser)) {
            return "redirect:/home";
        }

        BlockedDomain original = blockedDomainRepository.findById(id)
                .orElseThrow();

        original.setScheme(edited.getScheme());
        original.setDomain(edited.getDomain());
        original.setSource(edited.getSource());
        original.setNote(edited.getNote());

        blockedDomainRepository.save(original);
        urlSecurityService.reloadBlockedDomains();

        log.info("✏️ 차단 도메인 수정됨 : id={}", id);
        return "redirect:/admin/blocked-domains";
    }
}
