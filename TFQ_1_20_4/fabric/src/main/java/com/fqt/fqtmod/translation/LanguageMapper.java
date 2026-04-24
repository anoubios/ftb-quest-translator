package com.fqt.fqtmod.translation;

public class LanguageMapper {
    public static String getApiLanguageCode(String mcLangCode) {
        if (mcLangCode == null) return "en";
        mcLangCode = mcLangCode.toLowerCase(java.util.Locale.ROOT);
        if (mcLangCode.equals("zh_tw") || mcLangCode.equals("zh_hk")) return "zh-TW";
        if (mcLangCode.startsWith("zh_")) return "zh-CN";
        if (mcLangCode.contains("_")) return mcLangCode.split("_")[0];
        return mcLangCode;
    }

    public static boolean isRegionalVariant(String mcLangCode) {
        if (mcLangCode == null || !mcLangCode.contains("_")) return false;
        String lower = mcLangCode.toLowerCase(java.util.Locale.ROOT);
        
        // Explicitly check for known regional variants that translate to a generic base language
        // For example: Portuguese (Brazil) -> translates to generic Portuguese
        // Spanish (Argentina/Chile/Mexico etc) -> translates to generic Spanish
        // French (Canada) -> translates to generic French
        if (lower.equals("pt_br")) return true; // Portuguese (Brazil)
        if (lower.startsWith("es_") && !lower.equals("es_es")) return true; // Spanish variants (except Spain)
        if (lower.startsWith("fr_") && !lower.equals("fr_fr")) return true; // French variants (except France)
        if (lower.startsWith("de_") && !lower.equals("de_de")) return true; // German variants
        if (lower.startsWith("it_") && !lower.equals("it_it")) return true; // Italian variants
        if (lower.startsWith("nl_") && !lower.equals("nl_nl")) return true; // Dutch variants
        if (lower.startsWith("en_") && !lower.equals("en_us") && !lower.equals("en_gb")) return true; // English variants
        
        // For all other languages (ru_ru, uk_ua, pl_pl, tt_ru, etc.), consider them as base languages
        return false;
    }
}
