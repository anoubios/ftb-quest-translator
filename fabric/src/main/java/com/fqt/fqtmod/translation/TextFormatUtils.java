package com.fqt.fqtmod.translation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility to handle FTB Quests text formatting during translation.
 * Strips ALL formatting codes. Preserves structural elements as-is.
 */
public class TextFormatUtils {

    private static final Pattern ALL_FORMAT_CODES = Pattern.compile(
            "[§&]([0-9a-fk-or]|#[0-9A-Fa-f]{6})", Pattern.CASE_INSENSITIVE);

    public static boolean shouldSkipLine(String line) {
        if (line == null) return true;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return true;
        if (trimmed.equals("{@pagebreak}")) return true;
        if (trimmed.startsWith("{image:") || trimmed.matches(".*\\bimage\\s*:.*")) return true;
        if ((trimmed.startsWith("[") && trimmed.endsWith("]")) ||
            (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return true;
        }
        String stripped = stripAllFormatting(trimmed);
        if (stripped.trim().isEmpty()) return true;
        return false;
    }

    public static String stripAllFormatting(String text) {
        if (text == null) return "";
        return ALL_FORMAT_CODES.matcher(text).replaceAll("");
    }

    public static TranslatableText prepareForTranslation(String rawText) {
        if (shouldSkipLine(rawText)) {
            return new TranslatableText(rawText, rawText, true);
        }
        String plainText = stripAllFormatting(rawText).trim();
        if (plainText.isEmpty()) {
            return new TranslatableText(rawText, rawText, true);
        }
        return new TranslatableText(rawText, plainText, false);
    }

    public static List<TranslatableText> prepareDescriptionForTranslation(List<String> rawDescription) {
        List<TranslatableText> result = new ArrayList<>();
        for (String line : rawDescription) {
            result.add(prepareForTranslation(line));
        }
        return result;
    }

    public static record TranslatableText(String original, String plainText, boolean skip) {
        public String reconstruct(String translated) {
            if (skip) return original;
            return translated;
        }
    }
}
