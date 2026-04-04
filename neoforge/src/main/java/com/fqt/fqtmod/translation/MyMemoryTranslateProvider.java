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

/**
 * MyMemory Translation API provider.
 * Free tier: up to 5000 characters/day without API key.
 */
public class MyMemoryTranslateProvider implements TranslationProvider {

    private static final String API_URL = "https://api.mymemory.translated.net/get?q=%s&langpair=en|%s";
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
        });
    }

    private String parseMyMemoryResponse(String json, String original) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject responseData = root.getAsJsonObject("responseData");

            if (responseData != null && responseData.has("translatedText")) {
                String translated = responseData.get("translatedText").getAsString();
                if (translated != null && !translated.isEmpty()) {
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
}
