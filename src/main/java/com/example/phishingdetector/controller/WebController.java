package com.example.phishingdetector.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "Phishing Detection System");
        return "index";
    }

    @GetMapping("/analyzer")
    public String analyzer(Model model) {
        model.addAttribute("pageTitle", "Email Analyzer");
        return "analyzer";
    }

    @GetMapping("/comparison")
    public String comparison(Model model) {
        model.addAttribute("pageTitle", "Classifier Comparison");
        return "comparison";
    }
}