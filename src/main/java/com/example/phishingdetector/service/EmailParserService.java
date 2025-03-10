package com.example.phishingdetector.service;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
public class EmailParserService {
    private static final Logger logger = LoggerFactory.getLogger(EmailParserService.class);

    /**
     * Estrae il contenuto testuale da un file .eml
     */
    public String parseEmlFile(MultipartFile file) throws MessagingException, IOException {
        logger.info("Parsing del file .eml: {}", file.getOriginalFilename());

        // Crea una sessione vuota per il parsing
        Session session = Session.getDefaultInstance(new Properties(), null);

        try (InputStream inputStream = file.getInputStream()) {
            // Crea un MimeMessage dal file
            MimeMessage message = new MimeMessage(session, inputStream);

            StringBuilder result = new StringBuilder();

            // Aggiungi l'oggetto dell'email
            if (message.getSubject() != null) {
                result.append("Oggetto: ").append(message.getSubject()).append("\n\n");
            }

            // Inizializza una variabile per memorizzare il contenuto HTML (se presente)
            String textContent = "";
            String htmlContent = "";

            // Controlla il tipo di contenuto e gestiscilo di conseguenza
            if (message.isMimeType("text/plain")) {
                textContent = (String) message.getContent();
                result.append(textContent);
            } else if (message.isMimeType("text/html")) {
                htmlContent = (String) message.getContent();
                result.append(Jsoup.parse(htmlContent).text());
            } else if (message.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        textContent = (String) part.getContent();
                    } else if (part.isMimeType("text/html")) {
                        htmlContent += (String) part.getContent();
                    }
                }

                // Preferisci il testo semplice se disponibile, altrimenti usa l'HTML convertito
                if (!textContent.isEmpty()) {
                    result.append(textContent);
                } else if (!htmlContent.isEmpty()) {
                    result.append(Jsoup.parse(htmlContent).text());
                }
            }

            return result.toString();
        }
    }
}