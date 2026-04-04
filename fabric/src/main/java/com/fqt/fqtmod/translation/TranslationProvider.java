package com.fqt.fqtmod.translation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface TranslationProvider {
    CompletableFuture<String> translate(String text, String targetLang);

    default CompletableFuture<List<String>> translateBatch(List<String> texts, String targetLang) {
        if (texts.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        String separator = "\n§§§\n";
        String combined = String.join(separator, texts);
        return translate(combined, targetLang).thenApply(result -> {
            String[] parts = result.split("\\s*§§§\\s*");
            if (parts.length == texts.size()) return List.of(parts);
            parts = result.split("\n§§§\n|§§§");
            if (parts.length == texts.size()) return List.of(parts);
            return List.of(result);
        });
    }

    String getName();
}
