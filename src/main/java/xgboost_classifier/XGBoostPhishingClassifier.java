package xgboost_classifier;


import weka.core.Attribute;
import weka.core.Instances;
import java.util.ArrayList;
import java.util.List;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class XGBoostPhishingClassifier {
    // Il modello XGBoost
    private Booster booster;

    // Parametri per XGBoost
    private Map<String, Object> params;

    // La struttura del nostro dataset (definisce come sono organizzati i nostri dati)
    private Instances datasetStructure;

    public XGBoostPhishingClassifier() {
        // Inizializzazione parametri XGBoost
        params = new HashMap<>();
        params.put("objective", "binary:logistic");  // classificazione binaria
        params.put("eval_metric", "error");          // metrica di valutazione
        params.put("eta", 0.1);                      // learning rate
        params.put("max_depth", 6);                  // profondità massima albero
        params.put("min_child_weight", 1);
        params.put("subsample", 0.8);
        params.put("colsample_bytree", 0.8);
        params.put("seed", 42);                      // per riproducibilità

        // Prepariamo la struttura che conterrà i nostri dati
        setupDatasetStructure();
    }

    /**
     * Configura la struttura del dataset che useremo.
     * Questa struttura deve rispecchiare i nostri dati:
     * - 768 attributi numerici (uno per ogni dimensione dell'embedding)
     * - Attributi aggiuntivi (contains_url, contains_ip, ecc.)
     * - 1 attributo categorico (la classe: phishing o legittimo)
     */
    private void setupDatasetStructure() {
        ArrayList<Attribute> attributes = new ArrayList<>();

        // Creiamo 768 attributi numerici per l'embedding
        for (int i = 0; i < 768; i++) {
            attributes.add(new Attribute("embedding_" + i));
        }

        // Attributi aggiuntivi
        attributes.add(new Attribute("contains_url"));
        attributes.add(new Attribute("contains_ip"));
        attributes.add(new Attribute("contains_non_ascii"));
        attributes.add(new Attribute("contains_spam_world"));

        attributes.add(new Attribute("sentiment_score"));
        attributes.add(new Attribute("sentiment_magnitude"));

        // Creiamo l'attributo classe (phishing o legitimate)
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("phishing");
        classValues.add("legitimate");
        attributes.add(new Attribute("class", classValues));

        // Creiamo la struttura del dataset
        datasetStructure = new Instances("EmailDataset", attributes, 0);
        // Indichiamo qual è l'attributo classe (l'ultimo)
        datasetStructure.setClassIndex(datasetStructure.numAttributes() - 1);
    }

    /**
     * Converte una lista di embedding e labels in una DMatrix per XGBoost
     */
    private DMatrix createDMatrix(List<float[]> embeddings, List<Boolean> labels) throws XGBoostError {
        int numRows = embeddings.size();
        int numCols = embeddings.get(0).length;

        // Converte la matrice 2D in un array 1D (row-major)
        float[] flatData = new float[numRows * numCols];
        float[] labelArray = new float[numRows];

        for (int i = 0; i < numRows; i++) {
            float[] row = embeddings.get(i);
            for (int j = 0; j < numCols; j++) {
                flatData[i * numCols + j] = row[j];
            }
            labelArray[i] = labels.get(i) ? 1.0f : 0.0f;  // true = phishing (1), false = legitimate (0)
        }

        DMatrix dMatrix = new DMatrix(flatData, numRows, numCols);
        dMatrix.setLabel(labelArray);
        return dMatrix;
    }

    /**
     * Addestra il classificatore sui dati forniti.
     * @param embeddings Lista degli embedding delle email
     * @param labels Lista delle etichette (true per phishing, false per legittime)
     */
    public void train(List<float[]> embeddings, List<Boolean> labels) throws Exception {
        // Verifichiamo che i dati siano coerenti
        if (embeddings.size() != labels.size()) {
            throw new IllegalArgumentException("Il numero di embedding e labels deve corrispondere");
        }

        // Convertiamo i dati nel formato richiesto da XGBoost
        DMatrix trainMat = createDMatrix(embeddings, labels);

        // Definiamo i watchlist per monitorare l'addestramento
        Map<String, DMatrix> watches = new HashMap<>();
        watches.put("train", trainMat);

        // Numero di round di boosting
        int numRounds = 100;

        // Addestriamo il modello
        booster = XGBoost.train(trainMat, params, numRounds, watches, null, null);

        System.out.println("Modello XGBoost addestrato con successo");
    }

    /**
     * Classifica un nuovo embedding come phishing o legittimo
     * @param embedding L'embedding da classificare
     * @return true se l'email è classificata come phishing, false se legittima
     */
    public boolean classify(float[] embedding) throws Exception {
        // Convertiamo l'embedding in formato 1D per la DMatrix
        DMatrix dTest = new DMatrix(embedding, 1, embedding.length);

        // Facciamo la predizione
        float[][] predictions = booster.predict(dTest);

        // XGBoost restituisce la probabilità di appartenere alla classe positiva
        float probability = predictions[0][0];

        // Se la probabilità è > 0.5, classifichiamo come phishing
        return probability > 0.5;
    }

    /**
     * Valuta le performance del modello
     */
    public void evaluate(List<float[]> embeddings, List<Boolean> labels) throws Exception {
        // Convertiamo i dati nel formato richiesto da XGBoost
        DMatrix evalMat = createDMatrix(embeddings, labels);

        // Facciamo le predizioni
        float[][] predictions = booster.predict(evalMat);

        // Calcoliamo le metriche manuali (accuracy, precision, recall, F1)
        int tp = 0, fp = 0, tn = 0, fn = 0;

        for (int i = 0; i < predictions.length; i++) {
            boolean predicted = predictions[i][0] > 0.5;
            boolean actual = labels.get(i);

            if (predicted && actual) tp++;      // True Positive
            else if (predicted && !actual) fp++; // False Positive
            else if (!predicted && !actual) tn++; // True Negative
            else if (!predicted && actual) fn++;  // False Negative
        }

        // Calcoliamo le metriche

        double accuracy = (double)(tp + tn) / (tp + fp + tn + fn);
        double precision = tp == 0 ? 0 : (double)tp / (tp + fp);
        double recall = tp == 0 ? 0 : (double)tp / (tp + fn);
        double f1 = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);

        // Stampiamo i risultati
        System.out.println("=== Risultati della validazione ===");
        System.out.println("Accuracy: " + accuracy);
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F1 Score: " + f1);

        System.out.println("\n=== Matrice di confusione ===");
        System.out.println("True Positives: " + tp);
        System.out.println("False Positives: " + fp);
        System.out.println("True Negatives: " + tn);
        System.out.println("False Negatives: " + fn);

       /**
            // Calcoli per metriche in stile Weka
            int totalInstances = tp + fp + tn + fn;
            int correctInstances = tp + tn;
            int incorrectInstances = fp + fn;
            double accuracyPercent = 100.0 * correctInstances / totalInstances;
            double errorPercent = 100.0 * incorrectInstances / totalInstances;

            // Calcolo del Kappa statistic
            double pe = ((double)(tp + fp) * (tp + fn) + (double)(tn + fn) * (tn + fp)) / (totalInstances * totalInstances);
            double kappa = (accuracyPercent/100.0 - pe) / (1 - pe);

            // Calcoliamo MAE e RMSE (approssimati)
            double sumAbsError = 0;
            double sumSquaredError = 0;
            for (int i = 0; i < predictions.length; i++) {
                double actual = labels.get(i) ? 1.0 : 0.0;
                double predicted = predictions[i][0];
                sumAbsError += Math.abs(predicted - actual);
                sumSquaredError += Math.pow(predicted - actual, 2);
            }
            double mae = sumAbsError / totalInstances;
            double rmse = Math.sqrt(sumSquaredError / totalInstances);

            // WEKA-style output
            System.out.println("\n=== Risultati della validazione ===\n");
            System.out.printf("Correctly Classified Instances       %d               %.4f %%\n",
                    correctInstances, accuracyPercent);
            System.out.printf("Incorrectly Classified Instances     %d               %.4f %%\n",
                    incorrectInstances, errorPercent);
            System.out.printf("Kappa statistic                          %.4f\n", kappa);
            System.out.printf("Mean absolute error                      %.4f\n", mae);
            System.out.printf("Root mean squared error                  %.4f\n", rmse);
            System.out.println("Total Number of Instances              " + totalInstances + "     \n");

            // Confusion Matrix (Weka-style)
            System.out.println("\n=== Matrice di confusione ===");
            System.out.println("=== Confusion Matrix ===\n");
            System.out.println("   a   b   <-- classified as");
            System.out.printf(" %3d %3d |   a = phishing\n", tp, fn);
            System.out.printf(" %3d %3d |   b = legitimate\n", fp, tn);
        */
    }

    /**
     * Salva il modello addestrato su file
     */
    public void saveModel(String filepath) throws Exception {
        if (booster != null) {
            booster.saveModel(new FileOutputStream(filepath));
        } else {
            throw new IllegalStateException("Modello non addestrato");
        }
    }

    /**
     * Carica un modello precedentemente salvato
     */
    public void loadModel(String filepath) throws Exception {
        File modelFile = new File(filepath);
        if (modelFile.exists()) {
            booster = XGBoost.loadModel(new FileInputStream(modelFile));
        } else {
            throw new IllegalArgumentException("File modello non trovato: " + filepath);
        }
    }
}
