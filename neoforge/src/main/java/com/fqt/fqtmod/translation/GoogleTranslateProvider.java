package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.google.gson.JsonArray;
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
 * Google Translate unofficial API provider.
 * Uses the free translate.googleapis.com endpoint.
 */
public class GoogleTranslateProvider implements TranslationProvider {

    private static final String API_URL = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s";
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
                String url = String.format(API_URL, targetLang, encoded);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0")
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    FTBQuestTranslator.LOGGER.error("Google Translate API returned status {}", response.statusCode());
                    return text;
                }

                return parseGoogleResponse(response.body());
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("Google Translate failed: {}", e.getMessage());
                return text;
            }
        });
    }

    private String parseGoogleResponse(String json) {
        try {
            // Response format: [[["translated","original","",""],...],...],...]
            JsonArray root = JsonParser.parseString(json).getAsJsonArray();
            JsonArray sentences = root.get(0).getAsJsonArray();
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < sentences.size(); i++) {
                var sentence = sentences.get(i);
                if (sentence.isJsonArray()) {
                    JsonArray sentenceArr = sentence.getAsJsonArray();
                    if (sentenceArr.size() > 0 && !sentenceArr.get(0).isJsonNull()) {
                        result.append(sentenceArr.get(0).getAsString());
                    }
                }
            }

            return result.toString();
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to parse Google Translate response: {}", e.getMessage());
            return json;
        }
    }

    @Override
    public String getName() {
        return "Google Translate";
    }
}
