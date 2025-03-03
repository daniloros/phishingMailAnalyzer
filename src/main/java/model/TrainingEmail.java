package model;

public class TrainingEmail {
    private String text;
    private boolean isPhishing;
    private float sentimentScore;
    private float sentimentMagnitude;


    // Getter e setter
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isPhishing() { return isPhishing; }
    public void setPhishing(boolean phishing) { isPhishing = phishing; }

    public float getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(float sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public float getSentimentMagnitude() {
        return sentimentMagnitude;
    }

    public void setSentimentMagnitude(float sentimentMagnitude) {
        this.sentimentMagnitude = sentimentMagnitude;
    }
}
