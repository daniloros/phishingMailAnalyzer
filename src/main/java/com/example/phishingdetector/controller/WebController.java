package com.example.phishingdetector.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "Phishing Detection System");
        return "index";
    }

    @GetMapping("/analyzer")
    public String analyzer(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "Email Analyzer");

        // Recupera i dati dalla sessione se disponibili
        if (session.getAttribute("emailContent") != null) {
            model.addAttribute("emailContent", session.getAttribute("emailContent"));
            model.addAttribute("emailSubject", session.getAttribute("emailSubject"));
            model.addAttribute("urls", session.getAttribute("urls"));

            // Pulisci la sessione
            session.removeAttribute("emailContent");
            session.removeAttribute("emailSubject");
            session.removeAttribute("urls");
        }

        return "analyzer";
    }

    @GetMapping("/comparison")
    public String comparison(Model model, HttpSession session) {
        model.addAttribute("pageTitle", "Classifier Comparison");

        // Recupera i dati dalla sessione se disponibili
        if (session.getAttribute("emailContent") != null) {
            model.addAttribute("emailContent", session.getAttribute("emailContent"));
            model.addAttribute("emailSubject", session.getAttribute("emailSubject"));
            model.addAttribute("urls", session.getAttribute("urls"));

            // Pulisci la sessione
            session.removeAttribute("emailContent");
            session.removeAttribute("emailSubject");
            session.removeAttribute("urls");
        }

        return "comparison";
    }


}