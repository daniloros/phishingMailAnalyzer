package com.example.phishingdetector.controller;

import com.example.phishingdetector.service.EmailParserService;
import com.example.phishingdetector.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/gemini")
public class GeminiControllerAPI {
    private static final Logger logger = LoggerFactory.getLogger(GeminiControllerAPI.class);

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private EmailParserService emailParserService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeWithGemini(@RequestBody Map<String, Object> request) {
        try {
            // Ottieni i parametri obbligatori dalla richiesta
            String emailContent = (String) request.get("emailText");
            Boolean classification = (Boolean) request.get("classification");
            String classifier = (String) request.get("classifier");

            // Verifica che i parametri obbligatori siano presenti
            if (emailContent == null || classification == null || classifier == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Parametri mancanti: emailText, classification e classifier sono richiesti"
                ));
            }

            // Ottieni la lista di URL dalla richiesta se fornita
            List<String> providedUrls = null;
            if (request.containsKey("urls") && request.get("urls") instanceof List) {
                providedUrls = (List<String>) request.get("urls");
            }

            // Usa gli URL forniti o estraili dal testo
            List<String> urls;
            if (providedUrls != null && !providedUrls.isEmpty()) {
                urls = providedUrls;
                logger.info("Usando {} URL forniti direttamente dalla richiesta", urls.size());
            } else {
                urls = emailParserService.extractUrlsFromText(emailContent);
                logger.info("Estratti {} URL dal testo dell'email", urls.size());
            }

            logger.info("Richiesta analisi Gemini per email classificata come {} da {}",
                    classification ? "PHISHING" : "LEGITTIMA", classifier);

            // Chiamata asincrona a Gemini
            CompletableFuture<String> geminiAnalysisFuture = geminiService.analyzeEmailWithGemini(
                    emailContent,
                    urls,
                    classification,
                    classifier
            );

            // Attendiamo la risposta di Gemini con un timeout
            String geminiAnalysis = geminiAnalysisFuture.get(15, TimeUnit.SECONDS);

            logger.info("Analisi Gemini completata");

            return ResponseEntity.ok().body(Map.of(
                    "status", "success",
                    "analysis", geminiAnalysis
            ));

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.warn("Problema nell'ottenere l'analisi da Gemini", e);
            return ResponseEntity.ok().body(Map.of(
                    "status", "error",
                    "message", "Non è stato possibile ottenere l'analisi da Gemini: " + e.getMessage()
            ));
        } catch (ClassCastException e) {
            logger.error("Errore di formato nei parametri della richiesta", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Errore nel formato dei parametri: gli URLs devono essere una lista di stringhe"
            ));
        } catch (Exception e) {
            logger.error("Errore durante l'analisi con Gemini", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Errore durante l'analisi: " + e.getMessage()
            ));
        }
    }
}