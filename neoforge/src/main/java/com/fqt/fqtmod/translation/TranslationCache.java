package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory translation cache.
 * Key format: "targetLang:originalText"
 */
public class TranslationCache {
    private static final TranslationCache INSTANCE = new TranslationCache();
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public static TranslationCache getInstance() {
        return INSTANCE;
    }

    private String makeKey(String text, String targetLang) {
        return targetLang + ":" + text;
    }

    public String get(String text, String targetLang) {
        return cache.get(makeKey(text, targetLang));
    }

    public void put(String text, String targetLang, String translated) {
        cache.put(makeKey(text, targetLang), translated);
    }

    public boolean has(String text, String targetLang) {
        return cache.containsKey(makeKey(text, targetLang));
    }

    public void clear() {
        cache.clear();
        FTBQuestTranslator.LOGGER.info("Translation cache cleared ({} entries removed)", cache.size());
    }

    public int size() {
        return cache.size();
    }
}
