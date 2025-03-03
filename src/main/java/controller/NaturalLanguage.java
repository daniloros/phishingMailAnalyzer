package controller;// Imports the Google Cloud client library

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.language.v2.*;
import com.google.gson.*;
import model.MailData;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


public class NaturalLanguage {

   public static void extractNaturalLanguage(MailData mailData, String mailText) throws IOException {
       // Instantiates a client
       GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("phishingmailanalyzer-c0031f866de9.json"));

       // Crea un client configurando le credenziali
       // Configura le impostazioni del client con il provider delle credenziali
       LanguageServiceSettings settings = LanguageServiceSettings.newBuilder()
               .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
               .build();
       try (LanguageServiceClient language = LanguageServiceClient.create(settings)) {

           Document doc = Document.newBuilder().setContent(mailText).setType(Document.Type.PLAIN_TEXT).build();

           // Detects the sentiment of the text
           Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();

//           System.out.printf("Sentiment magnitude: %.3f\n", sentiment.getMagnitude());
           mailData.setSentimentMagnitude(sentiment.getMagnitude());
//           System.out.printf("Sentiment score: %.3f\n", sentiment.getScore());
           mailData.setSentimentScore(sentiment.getScore());

           //TODO: Capire come integrare questa info
           /**
               ClassifyTextRequest request = ClassifyTextRequest.newBuilder().setDocument(doc).build();
               Map<String, Float> categoryMap = new HashMap<>();


               ClassifyTextResponse response = language.classifyText(request);
               for (ClassificationCategory category : response.getCategoriesList()) {
                   categoryMap.put(category.getName(), category.getConfidence());
                   System.out.printf(
                           "Category name : %s, Confidence : %.3f\n",
                           category.getName(), category.getConfidence());
               }

               // Trova l'entry con il valore di confidenza massimo
               Map.Entry<String, Float> bestEntry = categoryMap.entrySet()
                       .stream()
                       .max(Map.Entry.comparingByValue())
                       .orElse(null);

               // Stampa il risultato
               if (bestEntry != null) {
                   System.out.printf("BEST - Category name : %s, Confidence : %.3f\n",
                           bestEntry.getKey(), bestEntry.getValue());
               } else {
                   System.out.println("Nessuna categoria trovata.");
               }
           */

       } catch (IOException e) {
           throw new RuntimeException(e);
       }
   }

   public static void main(String... args) throws Exception {
       // Leggi il file JSON
       String jsonContent = new String(Files.readAllBytes(Paths.get("src/main/resources/dataset/training_emails_400.json")));

       // Usa Gson per il parsing
       JsonParser parser = new JsonParser();
       JsonObject root = parser.parse(jsonContent).getAsJsonObject();
       JsonArray emails = root.getAsJsonArray("emails");

       // Crea il client per l'analisi
       try (LanguageServiceClient language = LanguageServiceClient.create()) {
           int totalEmails = emails.size();
           int processedEmails = 0;
           int skippedEmails = 0;

           for (int i = 0; i < totalEmails; i++) {
               JsonObject email = emails.get(i).getAsJsonObject();
               String text = email.get("text").getAsString();
               JsonObject metadata = email.getAsJsonObject("metadata");

               System.out.println("Elaborando email " + (i+1) + "/" + totalEmails +
                       " (Tipo: " + email.get("type").getAsString() +
                       ", Lunghezza: " + text.length() + " caratteri)");

               try {
                   // Crea il documento per l'analisi
                   Document doc = Document.newBuilder()
                           .setContent(text)
                           .setType(Document.Type.PLAIN_TEXT)
                           .build();

                   // Analizza il sentiment
                   Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();

                   // Aggiungi i risultati al metadata
                   metadata.addProperty("sentiment_magnitude", sentiment.getMagnitude());
                   metadata.addProperty("sentiment_score", sentiment.getScore());

                   System.out.println("  Successo: Magnitude=" + sentiment.getMagnitude() +
                           ", Score=" + sentiment.getScore());

                   processedEmails++;
               } catch (Exception e) {
                   // Se si verifica un'eccezione, salta questa email e continua
                   skippedEmails++;
                   metadata.addProperty("sentiment_error", "Skipped: " + e.getMessage());
                   System.err.println("  Errore nell'analisi dell'email " + (i+1) +
                           ": " + e.getMessage() + ". Email saltata.");
               }
           }

           // Salva il JSON risultante usando Gson
           try (FileWriter writer = new FileWriter("output_not_clear.json")) {
               Gson gson = new GsonBuilder()
                       .setPrettyPrinting()
                       .disableHtmlEscaping()  // Impedisce l'escape di &, <, > ecc.
                       .create();

               writer.write(gson.toJson(root));

               System.out.println("\nAnalisi completata:");
               System.out.println("- Email totali: " + totalEmails);
               System.out.println("- Email analizzate con successo: " + processedEmails);
               System.out.println("- Email saltate: " + skippedEmails);
               System.out.println("File output.json creato con successo!");
           }
       }
   }
}
