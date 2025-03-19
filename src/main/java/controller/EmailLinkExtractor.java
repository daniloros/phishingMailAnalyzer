package controller;


import model.MailData;
import org.apache.commons.validator.routines.UrlValidator;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailLinkExtractor {
    private String emailText;


    public EmailLinkExtractor(String emailText) {
        this.emailText = emailText;
    }

    public void extractLinkFeatures(MailData mailData) {
        findLinkInText(emailText, mailData);
    }


    private static void findLinkInText(String text, MailData mailData) {

        String urlRegex = "(https?://|www\\.)([-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6})\\b[-a-zA-Z0-9()@:%_+.~#?&/=\\-]*";

        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String originalUrl = matcher.group();

            // Se il link è un IP, verifichiamo con il metodo isValidIP
            if (isValidIP(originalUrl)) {
//                System.out.println("Indirizzo IP valido trovato: " + originalUrl);
                mailData.setContainsIpAsUrl(true);
                continue;
            }


            try {
                if (isValidURL(originalUrl)) {
                    mailData.setLink(originalUrl);
                    if (containsNonASCIICharacters(originalUrl)) {
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

    private static boolean isValidIP(String url) {
        try {
            // Rimuove il protocollo e il percorso per ottenere solo l'host
            java.net.URL netUrl = new java.net.URL(url.startsWith("http") ? url : "http://" + url);
            String host = netUrl.getHost();

            // Regex per IPv4
            String ipPattern = "^(?:\\d{1,3}\\.){3}\\d{1,3}$";
            Pattern pattern = Pattern.compile(ipPattern);
            Matcher matcher = pattern.matcher(host);

            return matcher.matches();
        } catch (Exception e) {
            return false; // In caso di URL non valido
        }
    }

    //controllo per attacco omografico
    // ritorna true se trova caratteri strani
    private static boolean containsNonASCIICharacters(String domain) {
        return !Charset.forName("US-ASCII").newEncoder().canEncode(domain);
    }

    public String getEmailText() {
        return emailText;
    }

    public void setEmailText(String emailText) {
        this.emailText = emailText;
    }
}
