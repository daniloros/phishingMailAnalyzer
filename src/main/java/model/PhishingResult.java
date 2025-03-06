package model;

public class PhishingResult {
    private final String emailText;
    private final boolean isPhishing;
    private final float[] embedding;
    private final Integer num_token;

    public PhishingResult(String emailText, boolean isPhishing, float[] embedding, Integer numToken) {
        this.emailText = emailText;
        this.isPhishing = isPhishing;
        this.embedding = embedding;
        num_token = numToken;
    }


    public String getEmailText() { return emailText; }
    public boolean isPhishing() { return isPhishing; }
    public float[] getEmbedding() { return embedding; }

    public Integer getNum_token() {
        return num_token;
    }

}
