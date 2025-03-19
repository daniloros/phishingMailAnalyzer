package controller;

import com.google.cloud.language.v2.Document;
import com.google.cloud.language.v2.LanguageServiceClient;
import com.google.cloud.language.v2.LanguageServiceSettings;
import com.google.cloud.language.v2.Sentiment;
import model.MailData;

import java.io.IOException;


public class NaturalLanguage {

   public static void extractNaturalLanguage(MailData mailData, String mailText) throws IOException {
       LanguageServiceSettings settings = LanguageServiceSettings.newBuilder().build();
       try (LanguageServiceClient language = LanguageServiceClient.create(settings)) {


           Document doc = Document.newBuilder().setContent(mailText).setType(Document.Type.PLAIN_TEXT).build();

           // Detects the sentiment of the text
           Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();

           mailData.setSentimentMagnitude(sentiment.getMagnitude());
           mailData.setSentimentScore(sentiment.getScore());

       } catch (IOException e) {
           throw new RuntimeException(e);
       }
   }

}
