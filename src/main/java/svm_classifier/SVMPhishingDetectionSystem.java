package svm_classifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import controller.BERTEmbeddingClient;
import controller.EmailFeatureExtractor;
import controller.FeatureConverter;
import controller.SpamDetectorFromJson;
import model.*;

import java.util.*;
import java.io.*;

/**
 * Questa classe implementa un sistema di rilevamento phishing basato su SVM.
 * Utilizza gli embedding BERT usando il classificatore con un Support Vector Machine (SVM).
 * L'SVM è particolarmente efficace con i nostri dati perché:
 * 1. Gestisce bene gli spazi ad alta dimensionalità (768D degli embedding BERT)
 * 2. È robusto con dataset di dimensioni ridotte
 * 3. Può essere ottimizzato per gestire lo sbilanciamento tra le lingue
 *       possiamo dare più peso per le mail italiane
 */
public class SVMPhishingDetectionSystem {
//    private final BERTEmbeddingClient umbertoClient;
    private final SVMPhishingClassifier classifier;
    private final String datasetPath;

    public SVMPhishingDetectionSystem(String datasetPath) {
        this.classifier = new SVMPhishingClassifier();
        this.datasetPath = datasetPath;
    }

    /**
     * Analizza una singola email utilizzando il classificatore SVM.
     */
    public PhishingResult analyzeEmail(String emailText) throws Exception {
        // Otteniamo l'embedding da BERT
        EmailFromBert emailFromBert = BERTEmbeddingClient.getEmbedding(emailText);

        // Classifichiamo usando SVM
        boolean isPhishing = classifier.classify(emailFromBert.getEmbedding());

        return new PhishingResult(emailText, isPhishing, emailFromBert.getEmbedding(), emailFromBert.getNum_tokens());
    }

    /**
     * Addestra il sistema SVM usando il dataset fornito.
     */
    public void trainFromFile(String trainingDataFile) throws Exception {
        List<float[]> embeddings = new ArrayList<>();
        List<Boolean> labels = new ArrayList<>();
        List<ProcessedEmailForJSON> allProcessedEmails = new ArrayList<>();
        MailData mailData = new MailData();

        // Leggiamo il file JSON delle email di training
        ObjectMapper mapper = new ObjectMapper();
        List<TrainingEmail> trainingEmails = mapper.readValue(
                new File(trainingDataFile),
                mapper.getTypeFactory().constructCollectionType(List.class, TrainingEmail.class)
        );

        System.out.println("Processando " + trainingEmails.size() + " email per il training SVM...");

        // Processiamo ogni email
        for (TrainingEmail email : trainingEmails) {
            try {
                EmailFromBert emailFromBert = BERTEmbeddingClient.getEmbedding(email.getText());
                float[] embedding = emailFromBert.getEmbedding();

                mailData.setSentimentMagnitude(email.getSentimentMagnitude());
                mailData.setSentimentScore(email.getSentimentScore());


                EmailFeatureExtractor featureExtractor = new EmailFeatureExtractor(email.getText());
                featureExtractor.extractLinkFeatures(mailData);

                SpamDetectorFromJson spamDetectorFromJson = new SpamDetectorFromJson(email.getText());
                spamDetectorFromJson.findSpamWord(mailData);

                float[] combinedFeature = FeatureConverter.combineFeatures(embedding, mailData);


                embeddings.add(combinedFeature);
                labels.add(email.isPhishing());

                // Salviamo l'email processata
                allProcessedEmails.add(new ProcessedEmailForJSON(
                        email.getText(),
                        email.isPhishing(),
                        combinedFeature,
                        emailFromBert.getNum_tokens(),
                        new Date()
                ));

            } catch (Exception e) {
                System.err.println("Errore nel processare l'email: " + e.getMessage());
            }
        }

        // Salviamo le email processate
        String filename = String.format("%s/svm_processed_emails.json", datasetPath);
        mapper.writeValue(new File(filename), allProcessedEmails);

        // Addestriamo il classificatore SVM
        System.out.println("Addestramento del classificatore SVM...");
        classifier.train(embeddings, labels);

        // Valutiamo le performance
        System.out.println("\nValutazione del modello SVM...");
        classifier.evaluate(embeddings, labels);
    }

    /**
     * Salva il modello SVM addestrato
     */
    public void saveModel(String filepath) throws Exception {
        classifier.saveModel(filepath);
    }

    /**
     * Carica un modello SVM precedentemente salvato
     */
    public void loadModel(String filepath) throws Exception {
        classifier.loadModel(filepath);
    }


}
