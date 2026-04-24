package com.fqt.fqtmod.translation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface TranslationProvider {
    CompletableFuture<String> translate(String text, String targetLang);

    /**
     * Quickly ping the translation server to check if it's reachable.
     */
    CompletableFuture<Boolean> checkConnectionAsync();

    /**
     * Check if a specific language code is supported by this provider.
     */
    boolean supportsLanguage(String langCode);

    /**
     * Translate multiple texts. Default: parallel requests.
     * Providers that rate-limit (e.g. Lingva) should override with sequential execution.
     */
    default CompletableFuture<List<String>> translateAll(List<String> texts, String targetLang) {
        if (texts.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<CompletableFuture<String>> futures = texts.stream()
                .map(text -> translate(text, targetLang))
                .collect(Collectors.toList());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    String getName();
}

