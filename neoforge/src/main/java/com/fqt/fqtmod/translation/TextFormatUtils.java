package com.fqt.fqtmod.translation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility to handle FTB Quests text formatting during translation.
 *
 * Strategy: Strip ALL formatting codes before translation.
 * The translated result will be clean plain text.
 * Structural elements ({@pagebreak}, {image:...}, JSON components) are preserved as-is (not translated).
 *
 * Handles:
 * - §x and &x formatting codes (colors 0-f, formatting k/l/m/n/o/r)
 * - §#RRGGBB and &#RRGGBB hex color codes
 * - {image:...} lines (preserved, not translated)
 * - {@pagebreak} lines (preserved, not translated)
 * - JSON text components [{...}] and {...} (preserved, not translated)
 * - {open_url:...} and other {key:value} substitutes (preserved, not translated)
 */
public class TextFormatUtils {

    // Pattern for all formatting codes: §x, &x, §#RRGGBB, &#RRGGBB
    private static final Pattern ALL_FORMAT_CODES = Pattern.compile(
            "[§&]([0-9a-fk-or]|#[0-9A-Fa-f]{6})", Pattern.CASE_INSENSITIVE);

    /**
     * Check if a line should be skipped entirely (not translated).
     * These lines are structural elements that must remain untouched.
     */
    public static boolean shouldSkipLine(String line) {
        if (line == null) return true;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return true;

        // {@pagebreak}
        if (trimmed.equals("{@pagebreak}")) return true;

        // {image:...} lines - any line containing image: in a property-style format
        if (trimmed.startsWith("{image:") || trimmed.matches(".*\\bimage\\s*:.*")) return true;

        // JSON text components: [{...}] or {"text":...}
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
            (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return true;
        }

        // Lines that are only formatting codes with no actual text
        String stripped = stripAllFormatting(trimmed);
        if (stripped.trim().isEmpty()) return true;

        return false;
    }

    /**
     * Strip ALL formatting codes from text, returning only plain text content.
     * Removes: §x, &x, §#RRGGBB, &#RRGGBB
     */
    public static String stripAllFormatting(String text) {
        if (text == null) return "";
        return ALL_FORMAT_CODES.matcher(text).replaceAll("");
    }

    /**
     * Prepare text for translation:
     * - If it should be skipped, return it as-is
     * - Otherwise strip ALL formatting and return clean text for translation
     */
    public static TranslatableText prepareForTranslation(String rawText) {
        if (shouldSkipLine(rawText)) {
            return new TranslatableText(rawText, rawText, true);
        }

        // Strip ALL formatting - translated text will be clean
        String plainText = stripAllFormatting(rawText).trim();

        if (plainText.isEmpty()) {
            return new TranslatableText(rawText, rawText, true);
        }

        return new TranslatableText(rawText, plainText, false);
    }

    /**
     * Process a list of raw description lines for translation.
     */
    public static List<TranslatableText> prepareDescriptionForTranslation(List<String> rawDescription) {
        List<TranslatableText> result = new ArrayList<>();
        for (String line : rawDescription) {
            result.add(prepareForTranslation(line));
        }
        return result;
    }

    /**
     * Container for text prepared for translation.
     */
    public static record TranslatableText(
            String original,    // Original raw text with formatting
            String plainText,   // Clean text for translation (or original if skipped)
            boolean skip        // If true, don't translate this line - it's a structural element
    ) {
        /**
         * Get the final result after translation.
         * If skipped, returns original. Otherwise returns the clean translated text.
         */
        public String reconstruct(String translated) {
            if (skip) {
                return original;
            }
            // Return clean translated text - no formatting codes
            return translated;
        }
    }
}
