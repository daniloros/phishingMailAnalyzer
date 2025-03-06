package com.example.phishingdetector.dto;


public class EmailResponse {
    private String text;
    private boolean isPhishing;
    private String classifier;
    private String features;
    private Integer numTokens;
    private String resultId; // ID per riferimento nella cache

    public EmailResponse() {
    }

    public EmailResponse(String text, boolean isPhishing, String classifier, String features, Integer numTokens, String resultId) {
        this.text = text;
        this.isPhishing = isPhishing;
        this.classifier = classifier;
        this.features = features;
        this.numTokens = numTokens;
        this.resultId = resultId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isPhishing() {
        return isPhishing;
    }

    public void setPhishing(boolean phishing) {
        isPhishing = phishing;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }

    public Integer getNumTokens() {
        return numTokens;
    }

    public void setNumTokens(Integer numTokens) {
        this.numTokens = numTokens;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }
}