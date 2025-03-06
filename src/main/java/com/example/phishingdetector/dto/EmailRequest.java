package com.example.phishingdetector.dto;

public class EmailRequest {
    private String text;

    public EmailRequest() {
    }

    public EmailRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

