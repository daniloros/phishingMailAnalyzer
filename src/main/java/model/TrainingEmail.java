package model;

public class TrainingEmail {
    private String text;
    private boolean isPhishing;

    // Getter e setter
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isPhishing() { return isPhishing; }
    public void setPhishing(boolean phishing) { isPhishing = phishing; }
}
