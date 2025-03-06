package com.example.phishingdetector.dto;


public class ComparisonResponse {
    private boolean rfPrediction;
    private boolean svmPrediction;
    private boolean xgboostPrediction;
    private String emailText;
    private Integer numTokens;
    private String rfResultId;
    private String svmResultId;
    private String xgboostResultId;

    public ComparisonResponse() {
    }

    public ComparisonResponse(boolean rfPrediction, boolean svmPrediction, boolean xgboostPrediction,
                              String emailText, Integer numTokens,
                              String rfResultId, String svmResultId, String xgboostResultId) {
        this.rfPrediction = rfPrediction;
        this.svmPrediction = svmPrediction;
        this.xgboostPrediction = xgboostPrediction;
        this.emailText = emailText;
        this.numTokens = numTokens;
        this.rfResultId = rfResultId;
        this.svmResultId = svmResultId;
        this.xgboostResultId = xgboostResultId;
    }

    public boolean isRfPrediction() {
        return rfPrediction;
    }

    public void setRfPrediction(boolean rfPrediction) {
        this.rfPrediction = rfPrediction;
    }

    public boolean isSvmPrediction() {
        return svmPrediction;
    }

    public void setSvmPrediction(boolean svmPrediction) {
        this.svmPrediction = svmPrediction;
    }

    public boolean isXgboostPrediction() {
        return xgboostPrediction;
    }

    public void setXgboostPrediction(boolean xgboostPrediction) {
        this.xgboostPrediction = xgboostPrediction;
    }

    public String getEmailText() {
        return emailText;
    }

    public void setEmailText(String emailText) {
        this.emailText = emailText;
    }

    public Integer getNumTokens() {
        return numTokens;
    }

    public void setNumTokens(Integer numTokens) {
        this.numTokens = numTokens;
    }

    public String getRfResultId() {
        return rfResultId;
    }

    public void setRfResultId(String rfResultId) {
        this.rfResultId = rfResultId;
    }

    public String getSvmResultId() {
        return svmResultId;
    }

    public void setSvmResultId(String svmResultId) {
        this.svmResultId = svmResultId;
    }

    public String getXgboostResultId() {
        return xgboostResultId;
    }

    public void setXgboostResultId(String xgboostResultId) {
        this.xgboostResultId = xgboostResultId;
    }
}