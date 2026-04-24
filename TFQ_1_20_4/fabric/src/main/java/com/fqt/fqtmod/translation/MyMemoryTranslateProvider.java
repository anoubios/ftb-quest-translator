package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class MyMemoryTranslateProvider implements TranslationProvider {

    private static final String API_URL = "https://api.mymemory.translated.net/get?q=%s&langpair=Autodetect%%7C%s";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public CompletableFuture<String> translate(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture(text);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
                String url = String.format(API_URL, encoded, targetLang);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "FTBQuestTranslator/1.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    FTBQuestTranslator.LOGGER.error("MyMemory API returned status {}", response.statusCode());
                    return text;
                }
                return parseMyMemoryResponse(response.body(), text);
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("MyMemory translation failed: {}", e.getMessage());
                return text;
            }
        }, QuestTranslationManager.HTTP_EXECUTOR);
    }

    private String parseMyMemoryResponse(String json, String original) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject responseData = root.getAsJsonObject("responseData");
            if (responseData != null && responseData.has("translatedText")) {
                String translated = responseData.get("translatedText").getAsString();
                if (translated != null && !translated.isEmpty()) {
                    // Filter out MyMemory error messages returned as "translations"
                    String upper = translated.toUpperCase();
                    if (upper.contains("PLEASE SELECT TWO DISTINCT LANGUAGES") ||
                        upper.contains("MYMEMORY WARNING") ||
                        upper.contains("QUERY LENGTH LIMIT") ||
                        upper.contains("IS AN INVALID TARGET LANGUAGE")) {
                        FTBQuestTranslator.LOGGER.warn("MyMemory returned error message: {}", translated);
                        return original;
                    }
                    return translated;
                }
            }
            return original;
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to parse MyMemory response: {}", e.getMessage());
            return original;
        }
    }

    @Override
    public String getName() {
        return "MyMemory";
    }

    @Override
    public CompletableFuture<Boolean> checkConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mymemory.translated.net/get?q=test&langpair=Autodetect%7Cen"))
                        .header("User-Agent", "FTBQuestTranslator/1.0")
                        .timeout(Duration.ofSeconds(4))
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                return response.statusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        }, QuestTranslationManager.HTTP_EXECUTOR);
    }

    private static final java.util.Set<String> SUPPORTED_LANGUAGES = java.util.Set.of(
        "af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs", "bg", "ca", "ceb", "ny", "zh", "co", "hr", "cs", "da", "nl", "en", "eo", "et", "tl", "fi", "fr", "fy", "gl", "ka", "de", "el", "gu", "ht", "ha", "haw", "he", "hi", "hmn", "hu", "is", "ig", "id", "ga", "it", "ja", "jv", "kn", "kk", "km", "rw", "ko", "ku", "ky", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mn", "my", "ne", "no", "or", "ps", "fa", "pl", "pt", "pa", "ro", "ru", "sm", "gd", "sr", "st", "sn", "sd", "si", "sk", "sl", "so", "es", "su", "sw", "sv", "tg", "ta", "tt", "te", "th", "tr", "tk", "uk", "ur", "ug", "uz", "vi", "cy", "xh", "yi", "yo", "zu"
    );

    @Override
    public boolean supportsLanguage(String langCode) {
        if (langCode == null) return false;
        String apiCode = LanguageMapper.getApiLanguageCode(langCode).toLowerCase();
        if (apiCode.equals("zh-cn") || apiCode.equals("zh-tw")) return true;
        return SUPPORTED_LANGUAGES.contains(apiCode.split("-")[0]);
    }
}
