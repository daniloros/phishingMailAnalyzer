package com.example.phishingdetector.service;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParserService {
    private static final Logger logger = LoggerFactory.getLogger(EmailParserService.class);

    // Pattern per trovare URL in testo semplice
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://|www\\.)([-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6})\\b[-a-zA-Z0-9()@:%_+.~#?&/=\\-]*"
    );

    /**
     * Estrae il contenuto testuale da un file .eml e gli URL trovati
     */
    public Map<String, Object> parseEmlFile(MultipartFile file) throws MessagingException, IOException {
        logger.info("Parsing del file .eml: {}", file.getOriginalFilename());

        // Crea una sessione vuota per il parsing
        Session session = Session.getDefaultInstance(new Properties(), null);

        try (InputStream inputStream = file.getInputStream()) {
            // Crea un MimeMessage dal file
            MimeMessage message = new MimeMessage(session, inputStream);

            // Estrai il subject
            String subject = message.getSubject() != null ? message.getSubject() : "NO SUBJECT";

            // Estrai gli header
            Enumeration<String> headerLines = message.getAllHeaderLines();
            List<String> headers = new ArrayList<>();
            while (headerLines.hasMoreElements()) {
                headers.add(headerLines.nextElement());
            }

            // Inizializza variabili per memorizzare i contenuti
            String textContent = "";
            String htmlContent = "";
            List<String> extractedUrls = new ArrayList<>();

            // Controlla il tipo di contenuto e gestiscilo di conseguenza
            if (message.isMimeType("text/plain")) {
                textContent = (String) message.getContent();
                extractedUrls.addAll(extractUrlsFromText(textContent));
            } else if (message.isMimeType("text/html")) {
                htmlContent = (String) message.getContent();
                extractedUrls.addAll(extractUrlsFromHtml(htmlContent));
                textContent = Jsoup.parse(htmlContent).text();
            } else if (message.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        String content = (String) part.getContent();
                        textContent += content;
                        extractedUrls.addAll(extractUrlsFromText(content));
                    } else if (part.isMimeType("text/html")) {
                        String content = (String) part.getContent();
                        htmlContent += content;
                        extractedUrls.addAll(extractUrlsFromHtml(content));
                    }
                }

                // Se abbiamo solo contenuto HTML, estraiamo il testo
                if (textContent.isEmpty() && !htmlContent.isEmpty()) {
                    textContent = Jsoup.parse(htmlContent).text();
                }
            }

            // Preprocessa il contenuto per rimuovere spazi e newline duplicati
            textContent = textContent.replaceAll("\\s{2,}", " ").replaceAll("\\n{2,}", "\n").trim();

            // Gestisci la codifica quoted-printable se necessario
            if (textContent.contains("Content-Transfer-Encoding: quoted-printable")) {
                logger.debug("Rilevato contenuto quoted-printable, decodifica in corso...");
                textContent = decodeQuotedPrintable(textContent);
            }

            // Rimuovi duplicati dalla lista di URL
            List<String> uniqueUrls = new ArrayList<>(new LinkedHashSet<>(extractedUrls));

            // Crea la mappa di risposta
            Map<String, Object> result = new HashMap<>();
            result.put("text", textContent);
            result.put("subject", subject);
            result.put("urls", uniqueUrls);

            return result;
        }
    }

    /**
     * Estrae gli URL da contenuto HTML usando Jsoup
     */
    private List<String> extractUrlsFromHtml(String htmlContent) {
        List<String> urls = new ArrayList<>();

        Document doc = Jsoup.parse(htmlContent);

        // Estrai URL da tag a
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String url = link.attr("href");
            if (isValidUrl(url)) {
                urls.add(url);
            }
        }

        // Estrai URL da tag img
        Elements images = doc.select("img[src]");
        for (Element img : images) {
            String url = img.attr("src");
            if (isValidUrl(url)) {
                urls.add(url);
            }
        }

        // Cerca URL nel testo di tutti gli elementi
        String textContent = doc.text();
        urls.addAll(extractUrlsFromText(textContent));

        return urls;
    }

    /**
     * Estrae gli URL da testo semplice usando regex
     */
    public List<String> extractUrlsFromText(String text) {
        List<String> urls = new ArrayList<>();

        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group());
        }

        return urls;
    }

    /**
     * Verifica se una stringa è un URL valido
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // Ignora ancore interne e javascript
        if (url.startsWith("#") || url.startsWith("javascript:")) {
            return false;
        }

        // Accetta URL http/https o che iniziano con www.
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("www.");
    }

    /**
     * Decodifica testo in formato quoted-printable
     */
    private String decodeQuotedPrintable(String text) {
        // Implementazione semplificata della decodifica quoted-printable
        return text.replaceAll("=[0-9A-F]{2}", " ");
    }
}