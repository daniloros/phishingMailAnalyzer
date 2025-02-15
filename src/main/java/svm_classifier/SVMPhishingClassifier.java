package svm_classifier;


import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.supportVector.RBFKernel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SVMPhishingClassifier {
    private SMO classifier;
    private Instances datasetStructure;

    public SVMPhishingClassifier() {
        // Inizializziamo l'SVM
        classifier = new SMO();

        try {
            // Configuriamo il kernel RBF (Radial Basis Function)
            RBFKernel rbf = new RBFKernel();

            // Settiamo gamma - un parametro chiave per il kernel RBF
            // Un valore più basso di gamma crea un confine di decisione più smooth
            rbf.setGamma(0.01);
            classifier.setKernel(rbf);

            // Configuriamo i parametri di SMO
            // C è il parametro di regolarizzazione - bilancia l'errore di training
            // e la complessità del modello
            classifier.setC(1.0);

            // Utilizziamo la calibrazione di Platt per ottenere stime di probabilità
            classifier.setOptions(weka.core.Utils.splitOptions("-M"));

        } catch (Exception e) {
            System.err.println("Errore nella configurazione SVM: " + e.getMessage());
        }

        setupDatasetStructure();
    }

    private void setupDatasetStructure() {
        ArrayList<Attribute> attributes = new ArrayList<>();

        // 768 attributi per l'embedding BERT
        for (int i = 0; i < 768; i++) {
            attributes.add(new Attribute("embedding_" + i));
        }

        // Attributo classe (phishing o legitimate)
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("phishing");
        classValues.add("legitimate");
        attributes.add(new Attribute("class", classValues));

        datasetStructure = new Instances("EmailDataset", attributes, 0);
        datasetStructure.setClassIndex(datasetStructure.numAttributes() - 1);
    }

    public void train(List<float[]> embeddings, List<Boolean> labels) throws Exception {
        if (embeddings.size() != labels.size()) {
            throw new IllegalArgumentException("Numero di embedding e labels non corrispondente");
        }

        Instances trainingData = new Instances(datasetStructure);

        // Creiamo il dataset di training con pesi delle istanze
        for (int i = 0; i < embeddings.size(); i++) {
            double[] values = new double[769];
            float[] embedding = embeddings.get(i);

            // Copiamo l'embedding
            for (int j = 0; j < embedding.length; j++) {
                values[j] = embedding[j];
            }

            // Settiamo la classe
            values[768] = labels.get(i) ? 0.0 : 1.0;

            // Creiamo l'istanza con peso
            // Diamo più peso alle email in italiano (se necessario)
            // TODO: CAPIRE COME USARE IL PESO PER LA LINGUA IN ITALIANO
            Instance instance = new DenseInstance(1.0, values);
            trainingData.add(instance);
        }

        // Addestriamo il classificatore
        classifier.buildClassifier(trainingData);
    }

    public boolean classify(float[] embedding) throws Exception {
        double[] values = new double[embedding.length];
        int index = 0;
        for (float value : embedding) {
            values[index++] = value;
        }

        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(datasetStructure);

        // Classifichiamo l'istanza
        double prediction = classifier.classifyInstance(instance);
        return prediction == 0.0; // 0.0 = phishing, 1.0 = legitimate
    }

    public void evaluate(List<float[]> embeddings, List<Boolean> labels) throws Exception {
        Instances dataset = new Instances(datasetStructure);

        for (int i = 0; i < embeddings.size(); i++) {
            double[] values = new double[769];
            float[] embedding = embeddings.get(i);

            for (int j = 0; j < embedding.length; j++) {
                values[j] = embedding[j];
            }
            values[768] = labels.get(i) ? 0.0 : 1.0;

            dataset.add(new DenseInstance(1.0, values));
        }

        // Eseguiamo la cross-validation
        Evaluation eval = new Evaluation(dataset);
        eval.crossValidateModel(classifier, dataset, 10, new Random(1));

        // Stampiamo i risultati dettagliati
        System.out.println("=== Risultati Valutazione SVM ===");
        System.out.println(eval.toSummaryString());
        System.out.println("\n=== Matrice di Confusione ===");
        System.out.println(eval.toMatrixString());

        // Metriche aggiuntive particolarmente utili per dataset sbilanciati
        System.out.println("\n=== Metriche Dettagliate ===");
        System.out.println("F-Measure: " + eval.weightedFMeasure());
        System.out.println("ROC Area: " + eval.weightedAreaUnderROC());
        System.out.println("Precision: " + eval.weightedPrecision());
        System.out.println("Recall: " + eval.weightedRecall());
    }

    /**
     * Salvo il modello
     */
    public void saveModel(String filepath) throws Exception {
        weka.core.SerializationHelper.write(filepath, classifier);
    }

    /**
     * Carico il modello
     */
    public void loadModel(String filepath) throws Exception {
        classifier = (SMO) weka.core.SerializationHelper.read(filepath);
    }
}
