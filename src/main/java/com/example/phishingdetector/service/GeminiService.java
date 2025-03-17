package com.example.phishingdetector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);
    private static final String GEMINI_API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-thinking-exp-01-21:generateContent";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @Value("${app.gemini.api.key}")
    private String apiKeyFromProperties;

    private String apiKey;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    public void init() {
        // Prova prima a leggere direttamente dalla variabile d'ambiente
        String envApiKey = System.getenv("GEMINI_API_KEY");

        // Se disponibile dalla variabile d'ambiente, usa quella
        if (envApiKey != null && !envApiKey.isEmpty()) {
            this.apiKey = envApiKey;
            logger.info("Utilizzando API key Gemini dalla variabile d'ambiente");
        }
        // Altrimenti usa quella dalle proprietà Spring
        else if (apiKeyFromProperties != null && !apiKeyFromProperties.isEmpty()) {
            this.apiKey = apiKeyFromProperties;
            logger.info("Utilizzando API key Gemini dalle proprietà Spring");
        }
        // Se non è disponibile, log di warning
        else {
            logger.warn("Nessuna API key Gemini trovata! Le richieste API falliranno");
        }
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
                                                            boolean classification, String classifier, float[] embedding) {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                CompletableFuture<String> future = new CompletableFuture<>();
                future.complete("Errore: API key di Gemini non configurata");
                return future;
            }

            String prompt = buildPrompt(emailContent, urls, classification, classifier, embedding);

            logger.debug("PROMPT INVIATO A GEMINI:\n{}", prompt);


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
    private String buildPrompt(String emailContent, List<String> urls, boolean classification,
                              String classifier, float[] embedding) {
        StringBuilder prompt = new StringBuilder();

        // Introduzione e contesto
        prompt.append("Immagina di essere un esperto di sicurezza informatica che deve classificare una mail come phishing o legittima. ");
        prompt.append("La mail è stata già classificata come ");
        prompt.append(classification ? "PHISHING" : "LEGITTIMA");
        prompt.append(" dal classificatore ").append(classifier).append(", ");
        prompt.append("ma non devi farti influenzare da questa classificazione poiché il classificatore ha un margine d'errore.\n\n");

        // Contenuto della mail
        prompt.append("CONTENUTO DELLA MAIL:\n").append(emailContent).append("\n\n");

        // URL trovati
        prompt.append("URL TROVATI NELLA MAIL:\n");
        if (urls != null && !urls.isEmpty()) {
            for (String url : urls) {
                prompt.append("- ").append(url).append("\n");
            }
        } else {
            prompt.append("Nessun URL trovato.\n");
        }
        prompt.append("\n");

        // Informazioni tecniche sugli embedding e feature
        if (embedding != null && embedding.length > 0) {
            prompt.append("INFORMAZIONI TECNICHE:\n");
            prompt.append("La mail è stata analizzata con embedding BERT multilingual. ");


            if (embedding.length >= 774) {  // 768 dall'embedding BERT + 6 feature aggiuntive
                int embeddingSize = embedding.length - 6;

                prompt.append("L'embedding è un vettore di ").append(embedding.length).append(" elementi. ");
                prompt.append("I primi ").append(embeddingSize).append(" elementi sono i valori dell'embedding BERT, ");
                prompt.append("mentre gli ultimi 6 elementi sono feature specifiche:\n\n");

                // Valori delle feature speciali
                float containsUrl = embedding[embeddingSize];
                float containsIpUrl = embedding[embeddingSize + 1];
                float containsNonAscii = embedding[embeddingSize + 2];
                float containsSpamWords = embedding[embeddingSize + 3];
                float sentimentScore = embedding[embeddingSize + 4];
                float sentimentMagnitude = embedding[embeddingSize + 5];

                prompt.append("1. Presenza di URL: ").append(containsUrl > 0.5 ? "Sì" : "No").append(" (").append(String.format("%.2f", containsUrl)).append(")\n");
                prompt.append("2. Presenza di URL con indirizzi IP: ").append(containsIpUrl > 0.5 ? "Sì" : "No").append(" (").append(String.format("%.2f", containsIpUrl)).append(")\n");
                prompt.append("3. Presenza di URL con caratteri non ASCII: ").append(containsNonAscii > 0.5 ? "Sì" : "No").append(" (").append(String.format("%.2f", containsNonAscii)).append(")\n");
                prompt.append("4. Presenza di parole tipiche dello spam: ").append(containsSpamWords > 0.5 ? "Sì" : "No").append(" (").append(String.format("%.2f", containsSpamWords)).append(")\n");
                prompt.append("5. Sentiment score (API Natural Language): ").append(String.format("%.4f", sentimentScore));
                prompt.append("6. Sentiment magnitude (API Natural Language): ").append(String.format("%.4f", sentimentMagnitude));
            } else {
                // Se per qualche motivo l'embedding non ha le feature attese
                prompt.append("L'embedding ha una dimensione di ").append(embedding.length).append(" elementi.\n\n");
            }
        }

        // Richiesta di analisi
        prompt.append("RICHIESTA:\n");
        prompt.append("Fornisci un'analisi dettagliata di massimo 300 parole a questa email, considerando tutti gli elementi forniti. La tua analisi dovrebbe includere:\n");
        prompt.append("1. Se concordi o meno con la classificazione iniziale e perché\n");
        prompt.append("2. Elementi sospetti o indicatori di phishing presenti nel testo\n");
        prompt.append("3. Analisi degli URL (se presenti): sono legittimi o sospetti?\n");
        prompt.append("4. Considerazioni sulle feature tecniche estratte\n");
        prompt.append("5. Conclusione finale: classifichi questa mail come PHISHING o LEGITTIMA?\n\n");
        prompt.append("Rispondi in formato strutturato con titoli per ogni sezione della tua analisi. Mantieni l'analisi concisa ma completa.");

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