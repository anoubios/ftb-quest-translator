package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.SecretsConfig;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.fqt.fqtmod.FabricConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DeepLTranslateProvider implements TranslationProvider {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();

    private String getEndpoint() {
        String key = SecretsConfig.getDeepLApiKey();
        if (key != null && key.endsWith(":fx")) {
            return "https://api-free.deepl.com/v2/translate";
        }
        return "https://api.deepl.com/v2/translate";
    }

    @Override
    public CompletableFuture<String> translate(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture(text);
        }
        
        String apiKey = SecretsConfig.getDeepLApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            FTBQuestTranslator.LOGGER.error("DeepL API key is missing in configuration!");
            return CompletableFuture.completedFuture(text);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject payload = new JsonObject();
                JsonArray textArr = new JsonArray();
                textArr.add(text);
                payload.add("text", textArr);
                // DeepL requires uppercase ISO language codes usually, e.g. "UK", "EN-US"
                payload.addProperty("target_lang", targetLang.replace("_", "-").toUpperCase());
                payload.addProperty("tag_handling", "xml");
                
                String jsonBody = GSON.toJson(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getEndpoint()))
                        .header("Authorization", "DeepL-Auth-Key " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "FTBQuestTranslator/1.3")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    FTBQuestTranslator.LOGGER.error("DeepL API returned status {}: {}", response.statusCode(), response.body());
                    return text;
                }

                return parseDeepLResponse(response.body(), text);
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("DeepL Translate failed: {}", e.getMessage());
                return text;
            }
        }, QuestTranslationManager.HTTP_EXECUTOR);
    }

    private String parseDeepLResponse(String json, String originalText) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray translations = root.getAsJsonArray("translations");
            if (translations != null && translations.size() > 0) {
                return translations.get(0).getAsJsonObject().get("text").getAsString();
            }
            return originalText;
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to parse DeepL response: {}", e.getMessage());
            return json;
        }
    }

    @Override
    public String getName() {
        return "DeepL API";
    }

    @Override
    public CompletableFuture<Boolean> checkConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = SecretsConfig.getDeepLApiKey();
                if (apiKey == null || apiKey.trim().isEmpty()) return false;
                
                // DeepL has a usage endpoint indicating connection is OK
                String endpoint = getEndpoint().replace("/translate", "/usage");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Authorization", "DeepL-Auth-Key " + apiKey)
                        .header("User-Agent", "FTBQuestTranslator/1.3")
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
        "ar", "bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "hu", "id", "it", "ja", "ko", "lt", "lv", "nb", "nl", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "uk", "zh"
    );

    @Override
    public boolean supportsLanguage(String langCode) {
        if (langCode == null) return false;
        String baseCode = langCode.split("_")[0].toLowerCase();
        return SUPPORTED_LANGUAGES.contains(baseCode);
    }
}
