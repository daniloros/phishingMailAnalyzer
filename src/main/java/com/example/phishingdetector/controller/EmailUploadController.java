package com.example.phishingdetector.controller;

import com.example.phishingdetector.service.EmailParserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class EmailUploadController {
    private static final Logger logger = LoggerFactory.getLogger(EmailUploadController.class);

    @Autowired
    private EmailParserService emailParserService;

    @GetMapping("/upload")
    public String showUploadForm() {
        return "upload-form";
    }

    @PostMapping("/upload-email")
    public String handleEmailUpload(@RequestParam("emailFile") MultipartFile file, Model model, HttpSession session) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "Il file caricato è vuoto");
                return "upload-form";
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".eml")) {
                model.addAttribute("error", "Il file deve essere in formato .eml");
                return "upload-form";
            }

            // Estrai il contenuto dell'email
            Map<String, Object> parsedContent = emailParserService.parseEmlFile(file);

            // Salva i dati nella sessione
            session.setAttribute("emailContent", parsedContent.get("text"));
            session.setAttribute("emailSubject", parsedContent.get("subject"));
            session.setAttribute("urls", parsedContent.get("urls"));

            // Passa anche al model per la vista attuale
            model.addAttribute("emailContent", parsedContent.get("text"));
            model.addAttribute("emailSubject", parsedContent.get("subject"));
            model.addAttribute("urls", parsedContent.get("urls"));

            return "email-content";
        } catch (Exception e) {
            logger.error("Errore nell'elaborazione del file email", e);
            model.addAttribute("error", "Errore nell'elaborazione del file: " + e.getMessage());
            return "upload-form";
        }
    }

}