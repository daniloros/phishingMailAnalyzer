package rf_classifier;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RFPhishingClassifier {
    // Classificatore
    private Classifier classifier;

    // La struttura del nostro dataset (definisce come sono organizzati i nostri dati)
    private Instances datasetStructure;

    public RFPhishingClassifier() {
        // Random Forest: robusto e gestisce bene dati ad alta dimensionalità
        classifier = new RandomForest();
        // Prepariamo la struttura che conterrà i nostri dati
        setupDatasetStructure();
    }

    /**
     * Configura la struttura del dataset che useremo.
     * Questa struttura deve rispecchiare i nostri dati:
     * - 768 attributi numerici (uno per ogni dimensione dell'embedding)
     * - 1 attributo categorico (la classe: phishing o legittimo)
     */
    private void setupDatasetStructure() {
        ArrayList<Attribute> attributes = new ArrayList<>();

        // Creiamo 768 attributi numerici per l'embedding
        for (int i = 0; i < 768; i++) {
            attributes.add(new Attribute("embedding_" + i));
        }

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
     * Addestra il classificatore sui dati forniti.
     * @param embeddings Lista degli embedding delle email
     * @param labels Lista delle etichette (true per phishing, false per legittime)
     */
    public void train(List<float[]> embeddings, List<Boolean> labels) throws Exception {
        // Verifichiamo che i dati siano coerenti
        if (embeddings.size() != labels.size()) {
            throw new IllegalArgumentException("Il numero di embedding e labels deve corrispondere");
        }

        // Creiamo il dataset di training
        Instances trainingData = new Instances(datasetStructure);

        // Per ogni email nel nostro dataset
        for (int i = 0; i < embeddings.size(); i++) {
            float[] embedding = embeddings.get(i);
            boolean isPhishing = labels.get(i);

            // Creiamo un array con tutti i valori dell'istanza
            double[] values = new double[769]; // 768 per l'embedding + 1 per la classe

            // Copiamo l'embedding
            for (int j = 0; j < embedding.length; j++) {
                values[j] = embedding[j];
            }

            // Aggiungiamo la classe (0 per phishing, 1 per legitimate)
            values[768] = isPhishing ? 0.0 : 1.0;

            // Creiamo l'istanza e la aggiungiamo al dataset
            trainingData.add(new DenseInstance(1.0, values));
        }

        // Addestriamo il classificatore
        classifier.buildClassifier(trainingData);
    }

    /**
     * Classifica un nuovo embedding come phishing o legittimo
     * @param embedding L'embedding da classificare
     * @return true se l'email è classificata come phishing, false se legittima
     */
    public boolean classify(float[] embedding) throws Exception {
        // Creiamo un'istanza per il nuovo embedding
        double[] values = new double[769];

        // Copiamo l'embedding
        for (int i = 0; i < embedding.length; i++) {
            values[i] = embedding[i];
        }

        // Creiamo l'istanza
        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(datasetStructure);

        // Classifichiamo l'istanza
        double prediction = classifier.classifyInstance(instance);

        System.out.println(prediction);
        // Convertiamo la predizione in boolean
        return prediction == 0.0; // 0.0 = phishing, 1.0 = legitimate
    }

    /**
     * Valuta le performance del modello usando cross-validation
     */
    public void evaluate(List<float[]> embeddings, List<Boolean> labels) throws Exception {
        // Creiamo il dataset completo
        Instances dataset = new Instances(datasetStructure);

        for (int i = 0; i < embeddings.size(); i++) {
            float[] embedding = embeddings.get(i);
            boolean isPhishing = labels.get(i);

            double[] values = new double[769];
            for (int j = 0; j < embedding.length; j++) {
                values[j] = embedding[j];
            }
            values[768] = isPhishing ? 0.0 : 1.0;

            dataset.add(new DenseInstance(1.0, values));
        }

        // Eseguiamo una 10-fold cross validation
        Evaluation eval = new Evaluation(dataset);
        eval.crossValidateModel(classifier, dataset, 10, new Random(1));

        // Stampiamo i risultati
        System.out.println("=== Risultati della validazione ===");
        System.out.println(eval.toSummaryString());
        System.out.println("\n=== Matrice di confusione ===");
        System.out.println(eval.toMatrixString());
    }

    /**
     * Salva il modello addestrato su file
     */
    public void saveModel(String filepath) throws Exception {
        SerializationHelper.write(filepath, classifier);
    }

    /**
     * Carica un modello precedentemente salvato
     */
    public void loadModel(String filepath) throws Exception {
        classifier = (Classifier) SerializationHelper.read(filepath);
    }
}
