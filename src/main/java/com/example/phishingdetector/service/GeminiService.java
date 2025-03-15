package com.example.phishingdetector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-thinking-exp-01-21:generateContent";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @Value("${app.gemini.api.key}")
    private String apiKey;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Analizza i risultati della classificazione email insieme ai link trovati
     *
     * @param emailContent Contenuto dell'email analizzata
     * @param urls Lista di URL trovati nell'email
     * @param classification Risultato della classificazione (true = phishing, false = legittima)
     * @param classifier Nome del classificatore usato (RF, SVM, XGBoost)
     * @return CompletableFuture contenente l'analisi di Gemini come stringa
     */
    public CompletableFuture<String> analyzeEmailWithGemini(String emailContent, List<String> urls,
                                                            boolean classification, String classifier) {
        try {
            String prompt = buildPrompt(emailContent, urls, classification, classifier);

            // Costruisce il payload JSON per l'API Gemini
            ObjectNode contentNode = objectMapper.createObjectNode();
            contentNode.put("role", "user");
            contentNode.put("parts", objectMapper.createArrayNode().add(
                    objectMapper.createObjectNode().put("text", prompt)
            ));

            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contentsArray = objectMapper.createArrayNode();
            contentsArray.add(contentNode);
            requestBody.set("contents", contentsArray);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_ENDPOINT + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            try {
                                JsonNode responseJson = objectMapper.readTree(response.body());
                                return extractGeminiResponse(responseJson);
                            } catch (Exception e) {
                                logger.error("Errore durante il parsing della risposta di Gemini", e);
                                return "Errore nell'elaborazione della risposta: " + e.getMessage();
                            }
                        } else {
                            logger.error("Errore nella chiamata a Gemini API: " + response.statusCode() + " - " + response.body());
                            return "Errore nella chiamata a Gemini: " + response.statusCode();
                        }
                    })
                    .exceptionally(ex -> {
                        logger.error("Eccezione durante la chiamata a Gemini API", ex);
                        return "Errore di connessione: " + ex.getMessage();
                    });
        } catch (Exception e) {
            logger.error("Errore nella preparazione della richiesta a Gemini", e);
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Costruisce il prompt da inviare a Gemini
     */
    private String buildPrompt(String emailContent, List<String> urls, boolean classification, String classifier) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Comportati come un professionista di sicurezza informatica" +
                "analizza questa email che è stata classificata come ");
        prompt.append(classification ? "PHISHING" : "LEGITTIMA");
        prompt.append(" dal classificatore ").append(classifier).append(".\n\n");
        prompt.append(" Non farti però ingannare dalla classificazione dato che può avere un alto margine di errore").append(classifier).append(".\n\n");

        prompt.append("Contenuto dell'email:\n").append(emailContent).append("\n\n");

        prompt.append("URL trovati nell'email:\n");
        if (urls != null && !urls.isEmpty()) {
            for (String url : urls) {
                prompt.append("- ").append(url).append("\n");
            }
        } else {
            prompt.append("Nessun URL trovato.\n");
        }

        prompt.append("\nFornisci un'analisi di massimo 200 parole dei seguenti aspetti:\n");
        prompt.append("1. Se concordi con la classificazione e perché\n");
        prompt.append("2. Elementi sospetti o indicatori di phishing, se presenti\n");
        prompt.append("3. Analisi degli URL trovati, se presenti (sono sospetti? puntano a domini noti per phishing?)\n");

        return prompt.toString();
    }

    /**
     * Estrae il testo della risposta dal JSON restituito da Gemini
     */
    private String extractGeminiResponse(JsonNode responseJson) {
        try {
            // Naviga nel JSON per estrarre il testo della risposta
            JsonNode candidates = responseJson.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
            return "Nessuna risposta generata da Gemini";
        } catch (Exception e) {
            logger.error("Errore durante l'estrazione della risposta di Gemini", e);
            return "Errore nell'elaborazione della risposta: " + e.getMessage();
        }
    }
}