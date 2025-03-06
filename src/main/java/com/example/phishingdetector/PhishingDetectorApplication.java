package com.example.phishingdetector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.phishingdetector", "controller", "model", "rf_classifier", "svm_classifier", "xgboost_classifier"})
public class PhishingDetectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhishingDetectorApplication.class, args);
    }
}