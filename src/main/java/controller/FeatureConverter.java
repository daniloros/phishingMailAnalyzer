package controller;

import model.MailData;

import java.util.ArrayList;
import java.util.List;

public class FeatureConverter {
    public static float[] convertMailDataToFeatures(MailData mailData) {
        // Lista delle feature da estrarre da MailData
        List<Float> features = new ArrayList<>();

        // Aggiungiamo le feature numeriche
        features.add(mailData.getLinks().isEmpty() ? 0.0f : 1.0f);
//        features.add(mailData.getSuspiciousUrls().isEmpty() ? 0.0f : 1.0f)
        features.add(mailData.isContainsNonAsciiChars() ? 1.0f : 0.0f);
        features.add(mailData.isContainsIpAsUrl() ? 1.0f : 0.0f);
        features.add(mailData.isContainsSpam() ? 1.0f : 0.0f);

        features.add(mailData.getSentimentMagnitude());
        features.add(mailData.getSentimentScore());

        // Convertiamo la List<Float> in float[]
        float[] featureArray = new float[features.size()];
        for (int i = 0; i < features.size(); i++) {
            featureArray[i] = features.get(i);
        }

        return featureArray;
    }

    public static float[] combineFeatures(float[] bertEmbedding, MailData mailData) {
        float[] additionalFeatures = convertMailDataToFeatures(mailData);

        // Creiamo il nuovo array combinato
        float[] combinedFeatures = new float[bertEmbedding.length + additionalFeatures.length];

        // Copiamo l'embedding BERT
        System.arraycopy(bertEmbedding, 0, combinedFeatures, 0, bertEmbedding.length);

        // Aggiungiamo le nuove feature
        System.arraycopy(additionalFeatures, 0, combinedFeatures,
                bertEmbedding.length, additionalFeatures.length);

        return combinedFeatures;
    }
}
