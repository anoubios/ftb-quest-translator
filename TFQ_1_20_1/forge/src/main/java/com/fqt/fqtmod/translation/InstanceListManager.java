package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class InstanceListManager {
    private static final String GIST_URL = "https://gist.githubusercontent.com/anoubios/ftq-instances/raw/instances.json";
    private static final String[] HARDCODED_LINGVA = { "https://lingva.ml", "https://lingva.thedaviddelta.com", "https://translate.plausibility.cloud" };
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final long REFRESH_INTERVAL_MS = 30 * 60 * 1000;
    private static String[] cachedInstances = null;
    private static long lastFetchTime = 0;
    private static boolean fetchInProgress = false;

    public static String[] getLingvaInstances() {
        long now = System.currentTimeMillis();
        if (cachedInstances == null || now - lastFetchTime > REFRESH_INTERVAL_MS) {
            if (!fetchInProgress) {
                fetchInProgress = true;
                QuestTranslationManager.HTTP_EXECUTOR.submit(() -> { try { fetchFromGist(); } finally { fetchInProgress = false; } });
            }
        }
        return cachedInstances != null ? cachedInstances : HARDCODED_LINGVA;
    }

    public static String[] getHardcodedInstances() { return HARDCODED_LINGVA.clone(); }

    private static void fetchFromGist() {
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(GIST_URL)).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                if (json.has("lingva")) {
                    JsonArray arr = json.getAsJsonArray("lingva");
                    List<String> instances = new ArrayList<>();
                    for (JsonElement el : arr) {
                        String url = el.getAsString().trim();
                        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
                        if (!url.isEmpty()) instances.add(url);
                    }
                    if (!instances.isEmpty()) {
                        cachedInstances = instances.toArray(new String[0]);
                        lastFetchTime = System.currentTimeMillis();
                        FTBQuestTranslator.LOGGER.info("Updated Lingva instances from Gist: {} instances", instances.size());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.debug("Failed to fetch Lingva instance list from Gist: {}", e.getMessage());
        }
        if (cachedInstances == null) { cachedInstances = HARDCODED_LINGVA; lastFetchTime = System.currentTimeMillis(); }
    }
}
