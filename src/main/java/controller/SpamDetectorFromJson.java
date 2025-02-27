package controller;

import model.MailData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SpamDetectorFromJson {
    private static final String SPAMWORDSITA = "src/main/resources/dataset/spamWords/it.json";
    private static final String SPAMWORDSENG = "src/main/resources/dataset/spamWords/en.json";

    private String emailText;

    public SpamDetectorFromJson(String emailText) {
        this.emailText = emailText;
    }

    public void findSpamWord(MailData mailData) throws IOException {
        String detectedLanguage = detectLanguage(this.emailText);
//        System.out.println("Lingua rilevata: " + detectedLanguage);

        String jsonFile = detectedLanguage.equals("it") ? SPAMWORDSITA : SPAMWORDSENG;

        List<String> paroleChiave = loadKeywordsFromJson(jsonFile);

        Trie.TrieBuilder trieBuilder = Trie.builder().ignoreCase().onlyWholeWords();
        for (String parola : paroleChiave) {
            trieBuilder.addKeyword(parola);
        }

        Trie trie = trieBuilder.build();
        boolean containsSpam = containsSpamWord(trie, emailText);

        if(containsSpam) {
            mailData.setContainsSpam(true);
        }

    }


    private static boolean containsSpamWord(Trie trie, String emailText) {
        for (Emit match : trie.parseText(emailText)) {
            return true; // Non appena troviamo una parola chiave, restituiamo true
        }
        return false;
    }

    private static List<String> loadKeywordsFromJson(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filePath), List.class);
    }

    private static String detectLanguage(String emailText) throws IOException {
        LanguageDetector languageDetector = LanguageDetector.getDefaultLanguageDetector();
        languageDetector.loadModels();
        LanguageResult result = languageDetector.detect(emailText);
        return result.getLanguage();
    }


   /**
    public static void main(String[] args) throws IOException {
        String emailText = "This is a special offer just for you! Claim your free credit card now.";

        String detectedLanguage = detectLanguage(emailText);
        System.out.println("Lingua rilevata: " + detectedLanguage);

        String jsonFile = detectedLanguage.equals("it") ? SPAMWORDSITA : SPAMWORDSENG;

        List<String> paroleChiave = loadKeywordsFromJson(jsonFile);

        Trie.TrieBuilder trieBuilder = Trie.builder().ignoreCase().onlyWholeWords();
        for (String parola : paroleChiave) {
            trieBuilder.addKeyword(parola);
        }

        Trie trie = trieBuilder.build();

        boolean containsSpam = containsSpamWord(trie, emailText);
        if (containsSpam) {
            System.out.println("⚠️ L'email contiene parole sospette!");
        } else {
            System.out.println("✅ L'email è pulita.");
        }
    }
    */


}