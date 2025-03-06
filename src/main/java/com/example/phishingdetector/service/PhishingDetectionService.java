package com.example.phishingdetector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.PhishingResult;
import model.ProcessedEmailForJSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rf_classifier.RFPhishingDetectionSystem;
import svm_classifier.SVMPhishingDetectionSystem;
import xgboost_classifier.XGBoostPhishingDetectionSystem;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PhishingDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(PhishingDetectionService.class);

    @Value("${app.dataset.path}")
    private String datasetPath;

    @Value("${app.model.rf.path}")
    private String rfModelPath;

    @Value("${app.model.svm.path}")
    private String svmModelPath;

    @Value("${app.model.xgboost.path}")
    private String xgboostModelPath;

    private RFPhishingDetectionSystem rfSystem;
    private SVMPhishingDetectionSystem svmSystem;
    private XGBoostPhishingDetectionSystem xgboostSystem;

    @PostConstruct
    public void init() {
        try {
            logger.info("Inizializzazione dei sistemi di rilevamento phishing...");

            // Inizializza Random Forest
            rfSystem = new RFPhishingDetectionSystem(datasetPath);
            rfSystem.loadModel(rfModelPath);
            logger.info("Sistema Random Forest inizializzato.");

            // Inizializza SVM
            svmSystem = new SVMPhishingDetectionSystem(datasetPath);
            svmSystem.loadModel(svmModelPath);
            logger.info("Sistema SVM inizializzato.");

            // Inizializza XGBoost
            xgboostSystem = new XGBoostPhishingDetectionSystem(datasetPath);
            xgboostSystem.loadModel(xgboostModelPath);
            logger.info("Sistema XGBoost inizializzato.");

        } catch (Exception e) {
            logger.error("Errore durante l'inizializzazione dei sistemi di rilevamento phishing", e);
        }
    }

    /**
     * Analizza una email utilizzando il sistema Random Forest
     */
    public PhishingResult analyzeWithRandomForest(String emailText) throws Exception {
        logger.debug("Analisi con Random Forest: {}", emailText);
        return rfSystem.analyzeEmail(emailText);
    }

    /**
     * Analizza una email utilizzando il sistema SVM
     */
    public PhishingResult analyzeWithSVM(String emailText) throws Exception {
        logger.debug("Analisi con SVM: {}", emailText);
        return svmSystem.analyzeEmail(emailText);
    }

    /**
     * Analizza una email utilizzando il sistema XGBoost
     */
    public PhishingResult analyzeWithXGBoost(String emailText) throws Exception {
        logger.debug("Analisi con XGBoost: {}", emailText);
        return xgboostSystem.analyzeEmail(emailText);
    }

    /**
     * Salva il feedback dell'utente per qualsiasi classificatore
     * Questo metodo unificato gestisce il salvataggio del feedback indipendentemente dal classificatore utilizzato
     *
     * @param emailText Testo dell'email analizzata
     * @param userFeedback Feedback dell'utente (true = phishing, false = legittima)
     * @param embedding Embedding BERT dell'email
     * @param numTokens Numero di token nell'email (come Integer invece di int)
     * @param classifier Identificatore del classificatore utilizzato (rf, svm, xgboost)
     */
    public void saveFeedback(String emailText, boolean userFeedback, float[] embedding, Integer numTokens, String classifier) throws Exception {
        logger.info("Salvando feedback per classificatore: {}", classifier);

        // Creiamo un oggetto feedback con tutti i dati necessari, incluso il classificatore
        ProcessedEmailForJSON feedback = new ProcessedEmailForJSON(
                emailText,
                userFeedback,
                embedding,
                numTokens,
                new Date(),
                classifier // Aggiungiamo il classificatore all'oggetto
        );

        // Salviamo il feedback in un file JSON unico
        saveFeedbackToJson(feedback, classifier);

        logger.info("Feedback salvato con successo");
    }

    /**
     * Salva il feedback in un unico file JSON, controllando per duplicati
     */
    private void saveFeedbackToJson(ProcessedEmailForJSON feedback, String classifier) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        String fileName = "unified_feedback_dataset.json";

        File feedbackFile = new File(datasetPath + "/" + fileName);

        // Assicuriamoci che la directory esista
        feedbackFile.getParentFile().mkdirs();

        List<ProcessedEmailForJSON> existingFeedback = new ArrayList<>();

        // Se il file esiste, leggiamo il contenuto esistente
        if (feedbackFile.exists()) {
            existingFeedback = mapper.readValue(feedbackFile,
                    mapper.getTypeFactory().constructCollectionType(List.class, ProcessedEmailForJSON.class));
        }

        // Controllo per evitare duplicati
        boolean isDuplicate = false;
        for (ProcessedEmailForJSON existing : existingFeedback) {
            // Controlliamo se c'è un'email con testo identico, stesso feedback e stesso classificatore
            if (existing.getText().equals(feedback.getText()) && existing.isPhishing() == feedback.isPhishing()) {
                logger.info("Feedback duplicato trovato per classificatore '{}', non verrà aggiunto", classifier);
                isDuplicate = true;
                break;
            }
        }

        // Aggiungiamo il nuovo feedback solo se non è un duplicato
        if (!isDuplicate) {
            existingFeedback.add(feedback);

            // Salviamo il file aggiornato
            mapper.writeValue(feedbackFile, existingFeedback);
            logger.debug("Feedback salvato nel file unificato: {}", feedbackFile.getAbsolutePath());
        }
    }

    /**
     * Crea un file JSON unificato con tutti i feedback per uso futuro
     * Questo può essere utilizzato per aggiornare tutti i classificatori con nuovi dati
     */
    public void consolidateFeedback() throws IOException {
        // Questo metodo non è più necessario poiché ora stiamo già salvando tutto in un unico file
        logger.info("Tutti i feedback sono già salvati in un file unificato");
    }
}