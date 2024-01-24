package com.morze.morzetransfer.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MorseTranslator {

    private final Map<String, String> ruToMorse = new HashMap<>();
    private final Map<String, String> morseToRu = new HashMap<>();

    public MorseTranslator() {
        this.fillTables();
    }

    public String translate(String s) {
        if (s.length() != 0) {
            StringBuilder result = new StringBuilder();
            List<String> symbolsForTranslating = this.getListOfSymbols(s.toCharArray());

            for (String sym : symbolsForTranslating) {
                if (this.ruToMorse.containsKey(sym)) {
                    result.append(this.ruToMorse.get(sym));
                }
            }

            return result.toString();
        }

        return "";
    }

    private List<String> getListOfSymbols(char[] chars) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < chars.length; i++) {
            strings.add(String.valueOf(chars[i]));
        }

        return strings;
    }

    private void fillTables() {
        this.fillRuTable();
    }

    private void fillRuTable() {
        // Fill ruToMorse table with upper case russian letters
        ruToMorse.put("А", ".-/");
        ruToMorse.put("Б", "-.../");
        ruToMorse.put("В", ".--/");
        ruToMorse.put("Г", "--./");
        ruToMorse.put("Д", "-../");
        ruToMorse.put("Е", "./");
        ruToMorse.put("Ж", "...-/");
        ruToMorse.put("З", "--../");
        ruToMorse.put("И", "../");
        ruToMorse.put("Й", ".---/");
        ruToMorse.put("К", "-.-/");
        ruToMorse.put("Л", ".-../");
        ruToMorse.put("М", "--/");
        ruToMorse.put("Н", ".-/");
        ruToMorse.put("О", "---/");
        ruToMorse.put("П", ".--./");
        ruToMorse.put("Р", ".-./");
        ruToMorse.put("С", ".../");
        ruToMorse.put("Т", "-/");
        ruToMorse.put("У", "..-/");
        ruToMorse.put("Ф", "..-./");
        ruToMorse.put("Х", "..../");
        ruToMorse.put("Ц", "-.-./");
        ruToMorse.put("Ч", "---./");
        ruToMorse.put("Ш", "----/");
        ruToMorse.put("Щ", "--.-/");
        ruToMorse.put("Ъ", ".--.-./");
        ruToMorse.put("Ы", "-.--/");
        ruToMorse.put("Ь", "-..-/");
        ruToMorse.put("Э", "...-.../");
        ruToMorse.put("Ю", "..--/");
        ruToMorse.put("Я", ".-.-/");

        // Numbers and special symbols
        ruToMorse.put("1", ".----/");
        ruToMorse.put("2", "..---/");
        ruToMorse.put("3", "...--/");
        ruToMorse.put("4", "....-/");
        ruToMorse.put("5", "...../");
        ruToMorse.put("6", "..../");
        ruToMorse.put("7", "--.../");
        ruToMorse.put("8", "---../");
        ruToMorse.put("9", "----./");
        ruToMorse.put("0", "-----/");
        ruToMorse.put(".", "....../");
        ruToMorse.put(",", ".-.-.-/");
        ruToMorse.put("/", "-..-./");
        ruToMorse.put("?", "..--../");
        ruToMorse.put("!", "--..--/");
        ruToMorse.put("@", ".--.-./");

        // Fill ruToMorse with lower case russian letters
        ruToMorse.put("а", ".-/");
        ruToMorse.put("б", "-.../");
        ruToMorse.put("в", ".--/");
        ruToMorse.put("г", "--./");
        ruToMorse.put("д", "-../");
        ruToMorse.put("е", "./");
        ruToMorse.put("ж", "...-/");
        ruToMorse.put("з", "--../");
        ruToMorse.put("и", "../");
        ruToMorse.put("й", ".---/");
        ruToMorse.put("к", "-.-/");
        ruToMorse.put("л", ".-../");
        ruToMorse.put("м", "--/");
        ruToMorse.put("н", ".-/");
        ruToMorse.put("о", "---/");
        ruToMorse.put("п", ".--./");
        ruToMorse.put("р", ".-./");
        ruToMorse.put("с", ".../");
        ruToMorse.put("т", "-/");
        ruToMorse.put("у", "..-/");
        ruToMorse.put("ф", "..-./");
        ruToMorse.put("х", "..../");
        ruToMorse.put("ц", "-.-./");
        ruToMorse.put("ч", "---./");
        ruToMorse.put("ш", "----/");
        ruToMorse.put("щ", "--.-/");
        ruToMorse.put("ъ", ".--.-./");
        ruToMorse.put("ы", "-.--/");
        ruToMorse.put("ь", "-..-/");
        ruToMorse.put("э", "...-.../");
        ruToMorse.put("ю", "..--/");
        ruToMorse.put("я", ".-.-/");
    }
}
