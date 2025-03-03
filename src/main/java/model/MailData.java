package model;

import java.util.*;

public class MailData {

    private List<String> links;
    private boolean isPhishing;

    private List<String> suspiciousUrls;
    private boolean containsIpAsUrl;
    private boolean containsNonAsciiChars;
    private Set<String> attachmentTypes;
    private boolean containsSpam;
    private float sentimentScore;
    private float sentimentMagnitude;




    // Costruttore
    public MailData() {
        this.links = new ArrayList<>();
        this.suspiciousUrls = new ArrayList<>();
        this.attachmentTypes = new HashSet<>();
        this.containsSpam = false;
    }

    // Getter e Setter

    public List<String> getLinks() {
        return links;
    }

    public void setLink(String link) {
        this.links.add(link);
    }

    public boolean isPhishing() {
        return isPhishing;
    }

    public void setPhishing(boolean phishing) {
        isPhishing = phishing;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public List<String> getSuspiciousUrls() {
        return suspiciousUrls;
    }

    public void setSuspiciousUrls(List<String> suspiciousUrls) {
        this.suspiciousUrls = suspiciousUrls;
    }

    public boolean isContainsIpAsUrl() {
        return containsIpAsUrl;
    }

    public void setContainsIpAsUrl(boolean containsIpAsUrl) {
        this.containsIpAsUrl = containsIpAsUrl;
    }

    public boolean isContainsNonAsciiChars() {
        return containsNonAsciiChars;
    }

    public void setContainsNonAsciiChars(boolean containsNonAsciiChars) {
        this.containsNonAsciiChars = containsNonAsciiChars;
    }

    public void addSuspiciousUrl(String url) {
        this.suspiciousUrls.add(url);
    }

    public boolean isContainsSpam() {
        return containsSpam;
    }

    public void setContainsSpam(boolean containsSpam) {
        this.containsSpam = containsSpam;
    }

    public float getSentimentMagnitude() {
        return sentimentMagnitude;
    }

    public void setSentimentMagnitude(float sentimentMagnitude) {
        this.sentimentMagnitude = sentimentMagnitude;
    }

    public float getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(float sentimentScore) {
        this.sentimentScore = sentimentScore;
    }
}
