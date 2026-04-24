package com.fqt.fqtmod.translation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to handle FTB Quests text formatting during translation.
 *
 * Strategy: Parse text into formatted segments, translate each segment's text
 * separately, then reassemble with original formatting codes preserved.
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

    // Pattern for a single formatting code: §x, &x, §#RRGGBB, &#RRGGBB
    // Also captures FTB tags {image:...}, {@pagebreak}, {open_url:...}
    private static final Pattern FORMAT_CODE = Pattern.compile(
            "([§&][0-9a-fk-orA-FK-OR]|§#[0-9A-Fa-f]{6}|&#[0-9A-Fa-f]{6}|\\{[A-Za-z0-9_@:-]+[^}]*\\})"
    );

    /**
     * A segment of formatted text: a formatting prefix (zero or more format codes)
     * followed by the text content that the formatting applies to.
     */
    public static record FormatSegment(String formatPrefix, String textContent) {
        public boolean hasText() {
            return textContent != null && !textContent.trim().isEmpty();
        }
    }

    /**
     * Parse a line of text into format segments.
     * Each segment consists of consecutive formatting codes (the prefix) followed by
     * the text content until the next formatting code.
     */
    public static List<FormatSegment> parseSegments(String text) {
        List<FormatSegment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        Matcher matcher = FORMAT_CODE.matcher(text);

        List<int[]> codePositions = new ArrayList<>();
        while (matcher.find()) {
            codePositions.add(new int[]{matcher.start(), matcher.end()});
        }

        if (codePositions.isEmpty()) {
            segments.add(new FormatSegment("", text));
            return segments;
        }

        int pos = 0;
        StringBuilder currentPrefix = new StringBuilder();
        int i = 0;

        while (i < codePositions.size()) {
            int codeStart = codePositions.get(i)[0];
            int codeEnd = codePositions.get(i)[1];

            if (codeStart > pos && currentPrefix.isEmpty()) {
                String plainBefore = text.substring(pos, codeStart);
                if (!plainBefore.isEmpty()) {
                    segments.add(new FormatSegment("", plainBefore));
                }
            } else if (codeStart > pos && !currentPrefix.isEmpty()) {
                String content = text.substring(pos, codeStart);
                segments.add(new FormatSegment(currentPrefix.toString(), content));
                currentPrefix = new StringBuilder();
            }

            currentPrefix.append(text, codeStart, codeEnd);
            pos = codeEnd;

            if (i + 1 < codePositions.size() && codePositions.get(i + 1)[0] == codeEnd) {
                i++;
                continue;
            }

            i++;

            if (i < codePositions.size()) {
                int nextCodeStart = codePositions.get(i)[0];
                String content = text.substring(pos, nextCodeStart);
                segments.add(new FormatSegment(currentPrefix.toString(), content));
                currentPrefix = new StringBuilder();
                pos = nextCodeStart;
            }
        }

        if (pos < text.length()) {
            String remaining = text.substring(pos);
            segments.add(new FormatSegment(currentPrefix.toString(), remaining));
        } else if (!currentPrefix.isEmpty()) {
            segments.add(new FormatSegment(currentPrefix.toString(), ""));
        }

        return segments;
    }

    public static boolean shouldSkipLine(String line) {
        if (line == null) return true;
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return true;
        if (trimmed.equals("{@pagebreak}")) return true;
        if (isJsonComponent(trimmed)) return true;
        String stripped = stripAllFormatting(trimmed);
        if (stripped.trim().isEmpty()) return true;
        return false;
    }

    /**
     * Check if the text is a valid JSON component (not regular text in brackets).
     */
    private static boolean isJsonComponent(String text) {
        if ((text.startsWith("[") && text.endsWith("]")) ||
            (text.startsWith("{") && text.endsWith("}"))) {
            try {
                com.google.gson.JsonParser.parseString(text);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static String stripAllFormatting(String text) {
        if (text == null) return "";
        return FORMAT_CODE.matcher(text).replaceAll("");
    }

    public static String resolveTranslationKeys(String text) {
        if (text == null || text.isEmpty()) return text;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([A-Za-z0-9_\\.-]+)\\}").matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            if (key.startsWith("image:") || key.startsWith("open_url:") || key.startsWith("@pagebreak")) {
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group(0)));
                continue;
            }
            if (net.minecraft.client.resources.language.I18n.exists(key)) {
                String translated = net.minecraft.client.resources.language.I18n.get(key);
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(translated));
            } else {
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static TranslatableText prepareForTranslation(String rawText, String targetLang) {
        if (shouldSkipLine(rawText)) {
            return new TranslatableText(rawText, java.util.List.of(), true, java.util.Map.of());
        }
        java.util.List<FormatSegment> segments = parseSegments(rawText);
        boolean hasText = segments.stream().anyMatch(FormatSegment::hasText);
        
        java.util.Map<Integer, String> glossaryMap = new java.util.HashMap<>();
        if (hasText) {
            int gIndex = 0;
            for (int i = 0; i < segments.size(); i++) {
                FormatSegment seg = segments.get(i);
                if (seg.hasText()) {
                    String content = seg.textContent();
                    for (GlossaryManager.GlossaryTerm termEntry : GlossaryManager.getInstance().getGlossary()) {
                        if (!"ALL".equalsIgnoreCase(termEntry.targetLang) && !termEntry.targetLang.equalsIgnoreCase(targetLang)) continue;
                        String term = termEntry.original;
                        String pattern = "\\b" + java.util.regex.Pattern.quote(term) + "\\b";
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE).matcher(content);
                        StringBuffer sb = new StringBuffer();
                        while (m.find()) {
                            String token = "{{g" + gIndex + "}}";
                            m.appendReplacement(sb, token);
                            glossaryMap.put(gIndex, termEntry.translated);
                            gIndex++;
                        }
                        m.appendTail(sb);
                        content = sb.toString();
                    }
                    segments.set(i, new FormatSegment(seg.formatPrefix(), content));
                }
            }
        }
        
        if (!hasText) {
            return new TranslatableText(rawText, segments, true, glossaryMap);
        }
        return new TranslatableText(rawText, segments, false, glossaryMap);
    }

    public static List<TranslatableText> prepareDescriptionForTranslation(List<String> rawDescription, String targetLang) {
        List<TranslatableText> result = new ArrayList<>();
        for (String line : rawDescription) {
            result.add(prepareForTranslation(line, targetLang));
        }
        return result;
    }

    public static record TranslatableText(
            String original,
            List<FormatSegment> segments,
            boolean skip,
            java.util.Map<Integer, String> glossaryMap
    ) {
        public List<String> getTextsForTranslation() {
            if (skip) return List.of();
            
            StringBuilder masked = new StringBuilder();
            int mIndex = 0;
            
            for (FormatSegment seg : segments) {
                if (seg.formatPrefix() != null && !seg.formatPrefix().isEmpty()) {
                    masked.append("{{m").append(mIndex++).append("}}");
                }
                if (seg.hasText()) {
                    masked.append(seg.textContent());
                } else if (seg.textContent() != null && !seg.textContent().isEmpty()) {
                    // whitespace-only content
                    masked.append(seg.textContent());
                }
            }
            
            String result = masked.toString();
            // If the string is purely empty or just {{mX}} markers, skip translation
            if (result.trim().isEmpty() || result.matches("^(\\{\\{m\\d+\\}\\}|\\s)+$")) {
                return List.of();
            }
            return List.of(result);
        }

        public int getTranslatableCount() {
            if (skip) return 0;
            return getTextsForTranslation().size();
        }

        public String reconstruct(List<String> translatedTexts) {
            if (skip || translatedTexts.isEmpty()) return original;
            String translated = translatedTexts.get(0);

            // Cleanup API tag corruption
            translated = translated.replaceAll("\\{\\s*\\{\\s*m\\s*(\\d+)\\s*\\}\\s*\\}", "{{m$1}}");
            translated = translated.replaceAll("\\{\\s*\\{\\s*g\\s*(\\d+)\\s*\\}\\s*\\}", "{{g$1}}");

            // Fix punctuation spacing caused by API
            translated = translated.replaceAll("\\{\\{m(\\d+)\\}\\}\\s+([.,!?:;])", "{{m$1}}$2");
            translated = translated.replaceAll("\\{\\{g(\\d+)\\}\\}\\s+([.,!?:;])", "{{g$1}}$2");

            String finalStr = translated;
            int mIndex = 0;

            for (FormatSegment seg : segments) {
                if (seg.formatPrefix() != null && !seg.formatPrefix().isEmpty()) {
                    String marker = "{{m" + mIndex + "}}";
                    finalStr = finalStr.replace(marker, seg.formatPrefix());
                    mIndex++;
                }
            }

            for (java.util.Map.Entry<Integer, String> entry : glossaryMap.entrySet()) {
                String marker = "{{g" + entry.getKey() + "}}";
                finalStr = finalStr.replace(marker, entry.getValue());
            }

            // Strip any remaining unrelated or hallucinatory tags the API might have added
            finalStr = finalStr.replaceAll("\\{\\{m\\d+\\}\\}", "");
            finalStr = finalStr.replaceAll("\\{\\{g\\d+\\}\\}", "");

            // Decode HTML entities that translation APIs may introduce (e.g. MyMemory)
            // Must decode BEFORE sanitizeFormatCodes to avoid &quot; being treated as &q format code
            finalStr = decodeHtmlEntities(finalStr);

            // Sanitize invalid &x format codes that the translation API may have introduced.
            // Valid codes: &0-9, &a-f, &A-F, &k-o, &K-O, &r, &R, &#RRGGBB
            // Invalid codes like &q, &w, etc. cause "Invalid formatting!" errors in FTB Quests.
            finalStr = sanitizeFormatCodes(finalStr);

            return finalStr;
        }

        public String reconstruct(String translated) {
            return reconstruct(List.of(translated));
        }
    }

    private static String getLeadingWhitespace(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return s.substring(0, i);
    }

    private static String getTrailingWhitespace(String s) {
        int i = s.length();
        while (i > 0 && Character.isWhitespace(s.charAt(i - 1))) i--;
        return s.substring(i);
    }

    /**
     * Decode common HTML entities that translation APIs may return.
     * Order matters: &amp; must be decoded LAST to avoid double-decoding.
     */
    private static String decodeHtmlEntities(String text) {
        if (text == null || !text.contains("&")) return text;
        text = text.replace("&quot;", "\"");
        text = text.replace("&apos;", "'");
        text = text.replace("&#39;", "'");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&nbsp;", " ");
        // &amp; last to avoid double-decoding (e.g. &amp;quot; -> &quot; -> ")
        text = text.replace("&amp;", "&");
        return text;
    }

    /**
     * Sanitize invalid formatting codes in translated text.
     * FTB Quests uses & as formatting prefix. Valid codes after & are:
     * 0-9, a-f, A-F (colors), k-o, K-O (formatting), r, R (reset), #RRGGBB (hex)
     * Any other &x sequence is invalid and will cause "Invalid formatting!" error.
     * We escape invalid ones by removing the & prefix.
     */
    private static String sanitizeFormatCodes(String text) {
        if (text == null || !text.contains("&")) return text;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (isValidFormatChar(next)) {
                    result.append(c); // keep valid &x
                } else if (next == '#' && i + 8 < text.length()) {
                    // Check for &#RRGGBB hex color
                    String hex = text.substring(i + 2, i + 8);
                    if (hex.matches("[0-9A-Fa-f]{6}")) {
                        result.append(c); // keep valid &#RRGGBB
                    } else {
                        // Skip the & to prevent invalid formatting
                        continue;
                    }
                } else {
                    // Invalid format code - skip the &
                    continue;
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean isValidFormatChar(char c) {
        return (c >= '0' && c <= '9') || // colors 0-9
               (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || // colors a-f
               (c >= 'k' && c <= 'o') || (c >= 'K' && c <= 'O') || // formatting k-o
               c == 'r' || c == 'R'; // reset
    }
}
