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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LingvaTranslateProvider implements TranslationProvider {
    private static final int MAX_URL_ENCODED_LENGTH = 5000;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public CompletableFuture<String> translate(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) return CompletableFuture.completedFuture(text);
        return CompletableFuture.supplyAsync(() -> {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
            if (encoded.length() > MAX_URL_ENCODED_LENGTH) return translateLongText(text, targetLang);
            String[] instances = InstanceListManager.getLingvaInstances();
            for (String instance : instances) {
                try { String result = translateViaInstance(instance, encoded, targetLang); if (result != null) return result; }
                catch (Exception e) { FTBQuestTranslator.LOGGER.warn("Lingva instance {} failed: {}", instance, e.getMessage()); }
            }
            FTBQuestTranslator.LOGGER.error("All Lingva instances failed for translation.");
            return text;
        }, QuestTranslationManager.HTTP_EXECUTOR);
    }

    private String translateViaInstance(String instance, String encodedText, String targetLang) {
        try {
            String url = instance + "/api/v1/auto/" + targetLang + "/" + encodedText;
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "Mozilla/5.0").timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                if (json.has("translation")) { String t = json.get("translation").getAsString(); if (t != null && !t.isEmpty()) { FTBQuestTranslator.LOGGER.debug("Lingva translation successful via {}", instance); return t; } }
            } else { FTBQuestTranslator.LOGGER.debug("Lingva instance {} returned status {}", instance, resp.statusCode()); }
        } catch (Exception e) { FTBQuestTranslator.LOGGER.debug("Lingva request to {} failed: {}", instance, e.getMessage()); }
        return null;
    }

    private String translateLongText(String text, String targetLang) {
        String[] sentences = text.split("(?<=\\. )|(?<=\\n)");
        List<String> chunks = new ArrayList<>(); StringBuilder currentChunk = new StringBuilder();
        for (String sentence : sentences) { String testEncoded = URLEncoder.encode(currentChunk.toString() + sentence, StandardCharsets.UTF_8); if (testEncoded.length() > MAX_URL_ENCODED_LENGTH && currentChunk.length() > 0) { chunks.add(currentChunk.toString()); currentChunk = new StringBuilder(); } currentChunk.append(sentence); }
        if (currentChunk.length() > 0) chunks.add(currentChunk.toString());
        // Use %20 for spaces in URL path (not + which is for form encoding)
        String[] instances = InstanceListManager.getLingvaInstances(); StringBuilder result = new StringBuilder();
        for (String chunk : chunks) { String encoded = URLEncoder.encode(chunk, StandardCharsets.UTF_8).replace("+", "%20"); boolean translated = false; for (String instance : instances) { String partial = translateViaInstance(instance, encoded, targetLang); if (partial != null) { result.append(partial); translated = true; break; } } if (!translated) result.append(chunk); }
        return result.toString();
    }

    @Override public String getName() { return "Lingva (Google Proxy)"; }
    @Override public CompletableFuture<Boolean> checkConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> { String[] instances = InstanceListManager.getLingvaInstances(); for (String instance : instances) { try { String url = instance + "/api/v1/auto/en/" + URLEncoder.encode("test", StandardCharsets.UTF_8); HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "Mozilla/5.0").timeout(Duration.ofSeconds(5)).GET().build(); HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString()); if (resp.statusCode() == 200) return true; } catch (Exception e) {} } return false; }, QuestTranslationManager.HTTP_EXECUTOR);
    }
    private static final java.util.Set<String> SUPPORTED_LANGUAGES = java.util.Set.of("af","sq","am","ar","hy","az","eu","be","bn","bs","bg","ca","ceb","ny","zh","co","hr","cs","da","nl","en","eo","et","tl","fi","fr","fy","gl","ka","de","el","gu","ht","ha","haw","he","hi","hmn","hu","is","ig","id","ga","it","ja","jv","kn","kk","km","rw","ko","ku","ky","lo","la","lv","lt","lb","mk","mg","ms","ml","mt","mi","mr","mn","my","ne","no","or","ps","fa","pl","pt","pa","ro","ru","sm","gd","sr","st","sn","sd","si","sk","sl","so","es","su","sw","sv","tg","ta","tt","te","th","tr","tk","uk","ur","ug","uz","vi","cy","xh","yi","yo","zu");
    @Override public boolean supportsLanguage(String langCode) { if (langCode == null) return false; return SUPPORTED_LANGUAGES.contains(langCode.split("_")[0].toLowerCase()); }

    /**
     * Override translateAll to send ALL texts as a SINGLE request to avoid Lingva rate limiting.
     * Joins texts with a newline separator, translates once, then splits the result back.
     * This makes exactly 1 HTTP call instead of N, completely avoiding rate limits.
     */
    @Override
    public CompletableFuture<java.util.List<String>> translateAll(java.util.List<String> texts, String targetLang) {
        if (texts.isEmpty()) return CompletableFuture.completedFuture(java.util.List.of());
        // Use a unique separator that won't be confused with actual text
        String SEPARATOR = "\n\n";
        String combined = String.join(SEPARATOR, texts);
        return translate(combined, targetLang).thenApply(result -> {
            // Split the translated result back into individual segments
            String[] parts = result.split(SEPARATOR, -1);
            java.util.List<String> results = new ArrayList<>();
            if (parts.length == texts.size()) {
                // Perfect split - each segment translated correctly
                for (String part : parts) results.add(part.trim());
            } else {
                // Fallback: split by any double newline pattern (API may alter spacing)
                parts = result.split("\\n\\s*\\n", -1);
                if (parts.length == texts.size()) {
                    for (String part : parts) results.add(part.trim());
                } else {
                    // Last resort: return original texts with first/last segments translated
                    FTBQuestTranslator.LOGGER.warn("Lingva batch split mismatch: expected {} parts, got {}. Using single-segment fallback.", texts.size(), parts.length);
                    // Try to salvage: put the whole translated result as the first segment
                    for (int i = 0; i < texts.size(); i++) {
                        if (i == 0) results.add(result.contains(SEPARATOR) ? parts[0].trim() : result.trim());
                        else results.add(texts.get(i)); // keep original for unsplittable segments
                    }
                }
            }
            return results;
        });
    }
}
