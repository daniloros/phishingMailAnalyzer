package controller;

import model.EmailFromBert;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;
import java.io.IOException;

public class BERTEmbeddingClient {
    private static String API_URL = "http://localhost:8000/analyze";

    public BERTEmbeddingClient(String umbertoApiUrl) {
        API_URL = umbertoApiUrl;
    }

    public static EmailFromBert getEmbedding(String emailText) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(API_URL);
//            System.out.println("Attempting to connect to: " + API_URL);
            request.setHeader("Content-Type", "application/json");

            // Creiamo il payload JSON
            String json = new Gson().toJson(new EmailRequest(emailText));
            request.setEntity(new StringEntity(json, "UTF-8"));

            // Facciamo la richiesta
            try (CloseableHttpResponse response = httpClient.execute(request)) {
//                System.out.println("Response status: " + response.getStatusLine());
                String responseBody = EntityUtils.toString(response.getEntity());
//                System.out.println("Response body: " + responseBody);

                // Controlla se la risposta è valida
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Errore API: " + response.getStatusLine().getStatusCode() + " - " + responseBody);
                }

                EmailFromBert embeddingResponse = new Gson().fromJson(responseBody, EmailFromBert.class);
                return embeddingResponse;
            }
        }
    }

    // Classe per la richiesta
    static class EmailRequest {
        String text;
        EmailRequest(String text) { this.text = text; }
    }

}


