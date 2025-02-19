package controller;


import model.MailData;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailFeatureExtractor {
    private String emailText;


    public EmailFeatureExtractor(String emailText) {
        this.emailText = emailText;
    }

    public void extractLinkFeatures(MailData mailData) {
        findLinkInText(emailText, mailData);
    }


    private static void findLinkInText(String text, MailData mailData) {
        String urlRegex = "\\b((http(s)?://)?(www\\.)?[a-zA-Z0-9-]+(\\.[a-zA-Z]{2,})+(:\\d+)?(/[\\w\\-\\.~:/?#\\[\\]@!$&'()*+,;=%]*)?)\\b";

        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String originalUrl = matcher.group();

            // Se il link è un IP, verifichiamo con il metodo isValidIP
            if (isValidIP(originalUrl)) {
                System.out.println("Indirizzo IP valido trovato: " + originalUrl);
                mailData.setContainsIpAsUrl(true);
                continue;
            }


            try {
                if (isValidURL(originalUrl)) {
                    System.out.println("l'url è valido: " + originalUrl);
                    mailData.setLink(originalUrl);
                    if (containsNonASCIICharacters(originalUrl)) {
                        System.out.println("ATTENZIONE, l'url contiene caratteri sospetti");
//                        mailData.addSuspiciousUrl(originalUrl);
                        mailData.setContainsNonAsciiChars(true);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }
    }

    private static boolean isValidURL(String url) {
        // Abilita http e https
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);

        // Aggiungi http:// se manca il protocollo
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url; // se manca lo inserisco come  "http://"
        }

        return urlValidator.isValid(url);
    }

    private static boolean isValidIP(String ip) {
        String ipPattern = "^(?:\\d{1,3}\\.){3}\\d{1,3}$";  // Regex per IPv4
        Pattern pattern = Pattern.compile(ipPattern);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    //controllo per attacco omografico
    // ritorna true se trova caratteri strani
    private static boolean containsNonASCIICharacters(String domain) {
        // Regex per trovare caratteri non ASCII
        Pattern unicodePattern = Pattern.compile("[^\\x00-\\x7F]");     // cerca caratteri che non trovano nel renge 0 a 127
        return unicodePattern.matcher(domain).find();
    }

    public String getEmailText() {
        return emailText;
    }

    public void setEmailText(String emailText) {
        this.emailText = emailText;
    }
}
