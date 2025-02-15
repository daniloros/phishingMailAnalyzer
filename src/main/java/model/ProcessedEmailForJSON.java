package model;

import java.util.Date;

public class ProcessedEmailForJSON {
    private String text;
    private boolean isPhishing;
    private float[] embedding;
    private int num_token;
    private Date processedAt;

    // Costruttore esistente
    public ProcessedEmailForJSON(String text, boolean isPhishing, float[] embedding,int  num_token,Date processedAt) {
        this.text = text;
        this.isPhishing = isPhishing;
        this.embedding = embedding;
        this.num_token = num_token;
        this.processedAt = processedAt;
    }

    // Aggiungiamo un costruttore vuoto necessario per Jackson
    public ProcessedEmailForJSON() {
    }

    // Getter e Setter
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

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    public int getNum_token() {
        return num_token;
    }

    public void setNum_token(int num_token) {
        this.num_token = num_token;
    }
}
