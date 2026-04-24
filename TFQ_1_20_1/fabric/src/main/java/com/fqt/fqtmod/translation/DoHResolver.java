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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DoHResolver {
    private static final String CLOUDFLARE_DOH = "https://cloudflare-dns.com/dns-query";
    private static final String GOOGLE_DOH = "https://dns.google/resolve";
    private static final long CACHE_TTL_MS = 10 * 60 * 1000;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Map<String, CachedIP> cache = new ConcurrentHashMap<>();

    public static String resolve(String hostname) {
        CachedIP cached = cache.get(hostname);
        if (cached != null && !cached.isExpired()) return cached.ip;
        String ip = queryDoH(CLOUDFLARE_DOH, hostname);
        if (ip == null) ip = queryDoH(GOOGLE_DOH, hostname);
        if (ip != null) cache.put(hostname, new CachedIP(ip, System.currentTimeMillis()));
        return ip;
    }

    public static void clearCache() { cache.clear(); }

    private static String queryDoH(String dohServer, String hostname) {
        try {
            String url = dohServer + "?name=" + hostname + "&type=A";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("accept", "application/dns-json").timeout(Duration.ofSeconds(4)).GET().build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("Answer")) {
                    JsonArray answers = json.getAsJsonArray("Answer");
                    for (JsonElement element : answers) {
                        JsonObject answer = element.getAsJsonObject();
                        if (answer.has("type") && answer.get("type").getAsInt() == 1) {
                            String ip = answer.get("data").getAsString();
                            FTBQuestTranslator.LOGGER.debug("DoH resolved {} -> {} via {}", hostname, ip, dohServer.contains("cloudflare") ? "Cloudflare" : "Google");
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.debug("DoH query to {} for {} failed: {}", dohServer.contains("cloudflare") ? "Cloudflare" : "Google", hostname, e.getMessage());
        }
        return null;
    }

    private static class CachedIP {
        final String ip; final long timestamp;
        CachedIP(String ip, long timestamp) { this.ip = ip; this.timestamp = timestamp; }
        boolean isExpired() { return System.currentTimeMillis() - timestamp > CACHE_TTL_MS; }
    }
}
