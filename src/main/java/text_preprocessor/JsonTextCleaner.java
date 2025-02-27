package text_preprocessor;

import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.*;
import java.nio.file.*;
import java.util.*;


public class JsonTextCleaner {

    private static String cleanText(String text) {

        // Decodifica quoted-printable se necessario
        if (text.contains("Content-Transfer-Encoding: quoted-printable")) {
            text = decodeQuotedPrintable(text);
        }

        // Usa Jsoup per il parsing HTML e l'estrazione degli URL
        if (text.contains("<")) {
            try {
                Document doc = Jsoup.parse(text);
                text = doc.text();
            } catch (Exception e) {
                System.err.println("Errore nel parsing HTML: " + e.getMessage());
            }
        }

        // Pulizia generale del testo
        text = text
                // Rimuove i caratteri di escape
                .replace("\\n", " ")
                .replace("\\r", " ")
                // Rimuove i caratteri speciali MIME
                .replaceAll("=\\w{2}", " ")
                // Rimuove JavaScript
                .replaceAll("(?s)<script.*?</script>", "")
                // Rimuove più spazi consecutivi
                .replaceAll("\\s+", " ")
                // Rimuove più newline consecutivi
                .replaceAll("\\n{2,}", "\n")
                .trim();

        return text;
    }

    private static String decodeQuotedPrintable(String text) {
        // Implementazione base della decodifica quoted-printable
        return text.replaceAll("=[0-9A-F]{2}", " ");
    }

    public static void main(String... args) throws Exception {
        // Leggi il file JSON
        String jsonContent = new String(Files.readAllBytes(Paths.get("src/main/resources/dataset/training_emails_400.json")));

        // Parse del JSON
        JSONObject root = new JSONObject(jsonContent);
        JSONArray emails = root.getJSONArray("emails");

        // Per ogni email, pulisci il testo e stampa un confronto
        for (int i = 0; i < emails.length(); i++) {
            JSONObject email = emails.getJSONObject(i);
            String originalText = email.getString("text");
            String cleanedText = cleanText(originalText);

            // Sostituisci il testo originale con quello pulito
            email.put("text", cleanedText);
        }

        // Salva il nuovo JSON con i testi puliti
        try (FileWriter writer = new FileWriter("cleaned_emails.json")) {
            writer.write(root.toString(2));
        }

        System.out.println("\nFile cleaned_emails.json creato con successo!");
    }
}
