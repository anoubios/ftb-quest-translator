package com.fqt.fqtmod;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> TARGET_LANGUAGE = BUILDER
            .comment("Target language for translation (ISO 639-1 code).",
                     "Set to 'auto' to use Minecraft's language setting.",
                     "Examples: uk, ru, de, fr, es, ja, zh, ko, pl, pt")
            .define("targetLanguage", "auto");

    public static final ModConfigSpec.BooleanValue ENABLE_CACHING = BUILDER
            .comment("Enable translation caching to avoid re-translating the same text.",
                     "Cached translations are stored in memory for the current session.")
            .define("enableCaching", true);

    public static final ModConfigSpec.EnumValue<TranslationProvider> TRANSLATION_PROVIDER = BUILDER
            .comment("Translation API provider.",
                     "GOOGLE - Google Translate (unofficial, free, no key needed)",
                     "MYMEMORY - MyMemory API (free, 5000 chars/day limit)")
            .defineEnum("translationProvider", TranslationProvider.GOOGLE);

    static final ModConfigSpec SPEC = BUILDER.build();

    public enum TranslationProvider {
        GOOGLE,
        MYMEMORY
    }
}
