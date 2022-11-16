package org.hff.i18n;



import org.hff.api.ApiCode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LanguageManager {

    private static final Map<Locale, Language> languageMap = new ConcurrentHashMap<>();

    private static final Locale[] locales = Locale.class.getEnumConstants();

    private static final Locale defaultLocale = Locale.en;

    public static void register() {
        for (Locale locale : locales) {
            languageMap.put(locale, new Language(locale));
        }
    }

    public static Language getLanguage(Locale locale) {
        return languageMap.get(locale);
    }

    public static String getMsg(Locale locale, ApiCode code) {
        return languageMap.get(locale).getMsg(code);
    }

    public static String getMail(Locale locale, String key) {
        return languageMap.get(locale).getMail(key);
    }

    public static Locale getLocale(String desc) {
        if (desc == null) {
            return defaultLocale;
        }
        for (Locale locale : locales) {
            if (locale.getDesc().equals(desc)) {
                return locale;
            }
        }
        return defaultLocale;
    }
}
