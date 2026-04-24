package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class GoogleTranslateProvider implements TranslationProvider {

    private static final String API_URL = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s";
    private static final String GOOGLE_HOST = "translate.googleapis.com";
    private static final String[] PROXY_FALLBACKS = {
        "", // Direct first
        "https://api.codetabs.com/v1/proxy/?quest=" // Proxy fallback
    };
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public CompletableFuture<String> translate(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture(text);
        }
        return CompletableFuture.supplyAsync(() -> {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String directUrl = String.format(API_URL, targetLang, encodedText);
            try {
                String result = fetchDirect(directUrl, "direct");
                if (result != null) return result;
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.warn("Google Direct failed: {}", e.getMessage());
            }
            try {
                String result = translateViaDoH(text, targetLang);
                if (result != null) return result;
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.warn("Google DoH bypass failed: {}", e.getMessage());
            }
            try {
                String proxyUrl = PROXY_FALLBACKS[1] + URLEncoder.encode(directUrl, StandardCharsets.UTF_8);
                String result = fetchDirect(proxyUrl, "proxy");
                if (result != null) return result;
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.warn("Google proxy failed: {}", e.getMessage());
            }
            FTBQuestTranslator.LOGGER.error("All Google Translate attempts failed.");
            return text;
        }, QuestTranslationManager.HTTP_EXECUTOR);
    }

    private String fetchDirect(String url, String via) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseGoogleResponse(response.body());
            } else {
                FTBQuestTranslator.LOGGER.warn("Google Translate API returned status {} via {}", response.statusCode(), via);
            }
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.warn("Google Translate failed via {}: {}", via, e.getMessage());
        }
        return null;
    }

    private String translateViaDoH(String text, String targetLang) throws Exception {
        String ip = DoHResolver.resolve(GOOGLE_HOST);
        if (ip == null) {
            throw new Exception("DoH resolution failed for " + GOOGLE_HOST);
        }
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String path = "/translate_a/single?client=gtx&sl=auto&tl=" + targetLang + "&dt=t&q=" + encodedText;
        if (path.length() > 8000) {
            throw new Exception("URL too long for DoH (" + path.length() + " chars)");
        }

        // Create InetAddress with hostname (for SNI) but DoH-resolved IP (bypasses system DNS)
        String[] ipParts = ip.split("\\.");
        byte[] addr = new byte[4];
        for (int i = 0; i < 4; i++) addr[i] = (byte) Integer.parseInt(ipParts[i]);
        java.net.InetAddress address = java.net.InetAddress.getByAddress(GOOGLE_HOST, addr);

        FTBQuestTranslator.LOGGER.info("DoH bypass: connecting to {} ({}) via raw SSL socket", GOOGLE_HOST, ip);

        // Create SSL socket - Java automatically uses hostname from InetAddress for SNI
        javax.net.ssl.SSLContext ctx = javax.net.ssl.SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) ctx.getSocketFactory().createSocket(address, 443);
        socket.setSoTimeout(10000);

        try {
            socket.startHandshake();

            // Build raw HTTP/1.1 request with proper Host header
            String httpRequest = "GET " + path + " HTTP/1.1\r\n" +
                    "Host: " + GOOGLE_HOST + "\r\n" +
                    "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\r\n" +
                    "Accept: */*\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            socket.getOutputStream().write(httpRequest.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            // Read full response
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // Parse status line
            String statusLine = reader.readLine();
            if (statusLine == null) throw new Exception("No response from DoH connection");
            int statusCode = Integer.parseInt(statusLine.split(" ")[1]);

            // Read headers
            boolean chunked = false;
            int contentLength = -1;
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                String lower = headerLine.toLowerCase();
                if (lower.contains("transfer-encoding: chunked")) chunked = true;
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(headerLine.substring(15).trim());
                }
            }

            // Read body
            StringBuilder body = new StringBuilder();
            if (chunked) {
                String chunkLine;
                while ((chunkLine = reader.readLine()) != null) {
                    int chunkSize;
                    try { chunkSize = Integer.parseInt(chunkLine.trim(), 16); } catch (NumberFormatException e) { continue; }
                    if (chunkSize == 0) break;
                    char[] buf = new char[chunkSize];
                    int totalRead = 0;
                    while (totalRead < chunkSize) {
                        int r = reader.read(buf, totalRead, chunkSize - totalRead);
                        if (r == -1) break;
                        totalRead += r;
                    }
                    body.append(buf, 0, totalRead);
                    reader.readLine(); // consume trailing CRLF
                }
            } else {
                char[] buf = new char[4096];
                int read;
                while ((read = reader.read(buf)) != -1) {
                    body.append(buf, 0, read);
                }
            }

            if (statusCode == 200) {
                String result = parseGoogleResponse(body.toString());
                FTBQuestTranslator.LOGGER.info("Google DoH bypass SUCCESSFUL via IP {} (response: {} chars)", ip, body.length());
                return result;
            }

            FTBQuestTranslator.LOGGER.warn("DoH raw socket returned {} (body: {})", statusCode,
                    body.length() > 200 ? body.substring(0, 200) : body.toString());
            throw new Exception("DoH request returned " + statusCode);
        } finally {
            socket.close();
        }
    }

    private String parseGoogleResponse(String json) {
        try {
            // Clean raw response: trim whitespace and trailing non-JSON content (chunked encoding remnants)
            json = json.trim();
            // Find the end of the JSON array - match the outermost closing bracket
            int depth = 0;
            int jsonEnd = -1;
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '[') depth++;
                else if (c == ']') { depth--; if (depth == 0) { jsonEnd = i + 1; break; } }
                else if (c == '"') { // skip strings
                    i++;
                    while (i < json.length() && json.charAt(i) != '"') {
                        if (json.charAt(i) == '\\') i++; // skip escaped chars
                        i++;
                    }
                }
            }
            if (jsonEnd > 0 && jsonEnd < json.length()) {
                json = json.substring(0, jsonEnd);
            }

            // Parse with lenient reader
            com.google.gson.stream.JsonReader reader = new com.google.gson.stream.JsonReader(new java.io.StringReader(json));
            reader.setLenient(true);
            JsonArray root = JsonParser.parseReader(reader).getAsJsonArray();
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
            FTBQuestTranslator.LOGGER.error("Failed to parse Google Translate response: {} | raw: {}", e.getMessage(),
                    json.length() > 300 ? json.substring(0, 300) + "..." : json);
            return json;
        }
    }

    @Override
    public String getName() { return "Google Translate"; }

    @Override
    public CompletableFuture<Boolean> checkConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            String directUrl = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=test";
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(directUrl)).header("User-Agent", "Mozilla/5.0").timeout(Duration.ofSeconds(5)).GET().build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return true;
            } catch (Exception e) { /* Direct failed */ }
            try {
                String result = translateViaDoH("test", "en");
                if (result != null && !result.isEmpty()) return true;
            } catch (Exception e) { /* DoH failed */ }
            try {
                String proxyUrl = PROXY_FALLBACKS[1] + URLEncoder.encode(directUrl, StandardCharsets.UTF_8);
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(proxyUrl)).header("User-Agent", "Mozilla/5.0").timeout(Duration.ofSeconds(5)).GET().build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) return true;
            } catch (Exception e) { /* All failed */ }
            return false;
        }, QuestTranslationManager.HTTP_EXECUTOR);
    }

    public static final java.util.Set<String> SUPPORTED_LANGUAGES = java.util.Set.of(
        "af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs", "bg", "ca", "ceb", "ny", "zh", "co", "hr", "cs", "da", "nl", "en", "eo", "et", "tl", "fi", "fr", "fy", "gl", "ka", "de", "el", "gu", "ht", "ha", "haw", "he", "hi", "hmn", "hu", "is", "ig", "id", "ga", "it", "ja", "jv", "kn", "kk", "km", "rw", "ko", "ku", "ky", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mn", "my", "ne", "no", "or", "ps", "fa", "pl", "pt", "pa", "ro", "ru", "sm", "gd", "sr", "st", "sn", "sd", "si", "sk", "sl", "so", "es", "su", "sw", "sv", "tg", "ta", "tt", "te", "th", "tr", "tk", "uk", "ur", "ug", "uz", "vi", "cy", "xh", "yi", "yo", "zu"
    );

    @Override
    public boolean supportsLanguage(String langCode) {
        if (langCode == null) return false;
        String apiCode = LanguageMapper.getApiLanguageCode(langCode).toLowerCase();
        if (apiCode.equals("zh-cn") || apiCode.equals("zh-tw")) return true;
        return SUPPORTED_LANGUAGES.contains(apiCode.split("-")[0]);
    }

    /**
     * Batch translate: join all texts with newline, send 1 request, split result back.
     * Google Translate preserves newlines in translation, making this reliable and fast.
     */
    @Override
    public CompletableFuture<java.util.List<String>> translateAll(java.util.List<String> texts, String targetLang) {
        if (texts.isEmpty()) return CompletableFuture.completedFuture(java.util.List.of());
        if (texts.size() == 1) return translate(texts.get(0), targetLang).thenApply(java.util.List::of);
        String SEPARATOR = "\n\u2588\n"; // Using block character as unique separator
        String combined = String.join(SEPARATOR, texts);
        return translate(combined, targetLang).thenApply(result -> {
            // Try splitting by the separator (may be translated or preserved)
            String[] parts = result.split("\\s*\u2588\\s*", -1);
            if (parts.length == texts.size()) {
                java.util.List<String> results = new java.util.ArrayList<>();
                for (String part : parts) results.add(part.trim());
                return results;
            }
            // Fallback: try splitting by newline alone
            parts = result.split("\\n", -1);
            if (parts.length == texts.size()) {
                java.util.List<String> results = new java.util.ArrayList<>();
                for (String part : parts) results.add(part.trim());
                return results;
            }
            // Last resort: parallel individual requests
            FTBQuestTranslator.LOGGER.warn("Google batch split failed (expected {} got {}), falling back to individual requests", texts.size(), parts.length);
            java.util.List<CompletableFuture<String>> futures = new java.util.ArrayList<>();
            for (String text : texts) futures.add(translate(text, targetLang));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            java.util.List<String> results = new java.util.ArrayList<>();
            for (CompletableFuture<String> f : futures) results.add(f.join());
            return results;
        });
    }
}
