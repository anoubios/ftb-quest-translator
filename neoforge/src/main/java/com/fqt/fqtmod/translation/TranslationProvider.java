package com.fqt.fqtmod.translation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for translation providers.
 */
public interface TranslationProvider {
    /**
     * Translate a single text string.
     */
    CompletableFuture<String> translate(String text, String targetLang);

    /**
     * Translate multiple texts in batch. Default implementation translates one by one.
     */
    default CompletableFuture<List<String>> translateBatch(List<String> texts, String targetLang) {
        if (texts.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        // Combine all texts with a unique separator for single API call
        String separator = "\n§§§\n";
        String combined = String.join(separator, texts);

        return translate(combined, targetLang).thenApply(result -> {
            String[] parts = result.split("\\s*§§§\\s*");
            if (parts.length == texts.size()) {
                return List.of(parts);
            }
            // Fallback: if separator got mangled, try newline-based split
            parts = result.split("\n§§§\n|§§§");
            if (parts.length == texts.size()) {
                return List.of(parts);
            }
            // Last resort: return as single block
            return List.of(result);
        });
    }

    /**
     * Get the name of this provider for display.
     */
    String getName();
}
