import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.TrainingEmail;
import rf_classifier.RFPhishingDetectionSystem;
import svm_classifier.SVMPhishingDetectionSystem;
import xgboost_classifier.XGBoostPhishingDetectionSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrainingModels {
    public static void main(String[] args) {
        try {
            // Configurazione
//            String umbertoApiUrl = "http://localhost:8000/analyze";
            String datasetPath = "src/main/resources/dataset/processed";
            String inputJsonPath = "src/main/resources/dataset/training_emails_400.json";

            // Random Forest
            RFPhishingDetectionSystem system = new RFPhishingDetectionSystem(datasetPath);

            //SVM
            SVMPhishingDetectionSystem svmSystem = new SVMPhishingDetectionSystem(datasetPath);

            //XGBoost
            XGBoostPhishingDetectionSystem xgboostSystem = new XGBoostPhishingDetectionSystem(datasetPath);

            // Leggi il JSON originale
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(inputJsonPath));
            JsonNode emails = root.get("emails");

            // Converti nel formato atteso dal sistema
            List<TrainingEmail> trainingEmails = new ArrayList<>();

            for (JsonNode email : emails) {
                TrainingEmail trainingEmail = new TrainingEmail();
                trainingEmail.setText(email.get("text").asText());
                trainingEmail.setPhishing(email.get("type").asText().toLowerCase().contains("phishing"));
                trainingEmails.add(trainingEmail);
            }

            // Salva le email convertite in un formato temporaneo
            String tempJsonPath = "temp_training_data.json";
            mapper.writeValue(new File(tempJsonPath), trainingEmails);

//             Processa le email e addestra il modello
            System.out.println("Training Random Forest...");
            system.trainFromFile(tempJsonPath);
            // Salva il modello addestrato
            system.saveModel("rf_model_test_new.model");

            System.out.println("\nTraining SVM...");
            svmSystem.trainFromFile(tempJsonPath);
            svmSystem.saveModel("svm_model_test.model");

            System.out.println("\nTraining XGBoost...");
            xgboostSystem.trainFromFile(tempJsonPath);
            xgboostSystem.saveModel("xgboost_model_test.model");

            System.out.println("Elaborazione completata!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
