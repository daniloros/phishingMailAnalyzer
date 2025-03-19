import controller.EmailFeatureProcessor;
import model.ProcessedEmailForJSON;
import rf_classifier.RFPhishingClassifier;
import svm_classifier.SVMPhishingClassifier;
import xgboost_classifier.XGBoostPhishingClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrainingModels {
    public static void main(String[] args) {
        try {
            // Configuration
            String datasetPath = "src/main/resources/dataset/processed";
            String inputJsonPath = "src/main/resources/dataset/emails_sentiment.json";

            System.out.println("Starting improved training pipeline...");

            // Step 1: Process all emails once
            EmailFeatureProcessor processor = new EmailFeatureProcessor(datasetPath);

            // Check if processed file already exists
            // for update models: create unified_processed_emails.json (old json file + new json from user feedback
            File processedFile = new File(datasetPath + "/unified_processed_emails.json");
            if (!processedFile.exists()) {
                //file for first train (File without embedding bert but with sentiment analysis)
                System.out.println("Processing emails and extracting features...");
                processor.processEmails(inputJsonPath);
            } else {
                System.out.println("Using existing processed email features...");
            }

            // Step 2: Load the processed data
            List<ProcessedEmailForJSON> processedEmails = processor.loadProcessedEmails();
            System.out.println("Loaded " + processedEmails.size() + " processed emails");

            // Step 3: Prepare data for training (same data for all classifiers)
            List<float[]> embeddings = new ArrayList<>();
            List<Boolean> labels = new ArrayList<>();

            for (ProcessedEmailForJSON email : processedEmails) {
                embeddings.add(email.getEmbedding());
                labels.add(email.isPhishing());
            }

            // Step 4: Train all models using the same processed data

            // Train Random Forest
            System.out.println("\nTraining Random Forest...");
            RFPhishingClassifier rfClassifier = new RFPhishingClassifier();
            rfClassifier.train(embeddings, labels);
            rfClassifier.evaluate(embeddings, labels);
            rfClassifier.saveModel("rf_model_test_march.model");
            System.out.println("Random Forest model saved successfully");

            // Train SVM
            System.out.println("\nTraining SVM...");
            SVMPhishingClassifier svmClassifier = new SVMPhishingClassifier();
            svmClassifier.train(embeddings, labels);
            svmClassifier.evaluate(embeddings, labels);
            svmClassifier.saveModel("svm_model_test_march.model");
            System.out.println("SVM model saved successfully");

            // Train XGBoost
            System.out.println("\nTraining XGBoost...");
            XGBoostPhishingClassifier xgbClassifier = new XGBoostPhishingClassifier();
            xgbClassifier.train(embeddings, labels);
            xgbClassifier.evaluate(embeddings, labels);
            xgbClassifier.saveModel("xgboost_model_march.model");
            System.out.println("XGBoost model saved successfully");

            System.out.println("\nAll models trained and saved successfully!");

        } catch (Exception e) {
            System.err.println("Error during training process:");
            e.printStackTrace();
        }
    }
}
