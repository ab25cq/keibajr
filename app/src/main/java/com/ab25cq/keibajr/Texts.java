package com.ab25cq.keibajr;

import java.util.Locale;

final class Texts {
    private Texts() {
    }

    static boolean english() {
        return !japanese();
    }

    static boolean japanese() {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        String country = locale.getCountry();
        return "ja".equalsIgnoreCase(language) || "JP".equalsIgnoreCase(country);
    }

    static String t(String ja, String en) {
        return japanese() ? ja : en;
    }
}
