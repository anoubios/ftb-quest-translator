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
                     "AUTO - Automatic fallback: Google -> DoH bypass -> Lingva -> MyMemory (recommended)",
                     "GOOGLE - Google Translate (unofficial, free, no key needed)",
                     "MYMEMORY - MyMemory API (free, 5000 chars/day limit)",
                     "DEEPL - DeepL API (requires deepLApiKey)")
            .defineEnum("translationProvider", TranslationProvider.AUTO);

    public static final ModConfigSpec.BooleanValue ENABLE_TRANSLATING_ANIMATION = BUILDER
            .comment("Enable the yellow wave animation while a quest is being translated.")
            .define("enableTranslatingAnimation", true);

    public static final ModConfigSpec.BooleanValue ENABLE_TRANSLATED_ANIMATION = BUILDER
            .comment("Enable the green wave animation when a quest has been translated.")
            .define("enableTranslatedAnimation", true);

    public static final ModConfigSpec.BooleanValue ENABLE_LARGE_TEXT_WARNING = BUILDER
            .comment("Warn before translating quests with very large text to prevent accidental API quota usage.")
            .define("enableLargeTextWarning", true);

    public static final ModConfigSpec.ConfigValue<Integer> LARGE_TEXT_THRESHOLD = BUILDER
            .comment("The character count threshold used to trigger the large text warning.")
            .define("largeTextThreshold", 2000);

    public static final ModConfigSpec.BooleanValue ENABLE_TTS_LOADING_ANIMATION = BUILDER
            .comment("Enable the animated yellow rings while TTS is loading.")
            .define("enableTtsLoadingAnimation", true);

    public static final ModConfigSpec.BooleanValue ENABLE_TTS_PLAYING_ANIMATION = BUILDER
            .comment("Enable the animated reading rings while TTS is playing.")
            .define("enableTtsPlayingAnimation", true);

    public static final ModConfigSpec.BooleanValue ENABLE_TRANSLATE_BUTTON = BUILDER
            .comment("Enable the quest translate button.")
            .define("enableTranslateButton", true);

    public static final ModConfigSpec.BooleanValue ENABLE_TTS_BUTTON = BUILDER
            .comment("Enable the text-to-speech button.")
            .define("enableTtsButton", true);

    public static final ModConfigSpec.ConfigValue<Integer> TRANSLATION_ANIMATION_DURATION = BUILDER
            .comment("Duration of the translation animation for a single quest (in milliseconds).")
            .defineInRange("translationAnimationDuration", 5000, 500, 30000);

    public static final ModConfigSpec.ConfigValue<Double> TTS_PITCH = BUILDER
            .comment("Pitch factor for TTS voice (e.g. 1.0 is normal). MIN: 0.1, MAX: 5.0")
            .defineInRange("ttsPitch", 1.0, 0.1, 5.0);

    public static final ModConfigSpec.ConfigValue<Double> TTS_RATE = BUILDER
            .comment("Speed rate for TTS voice (e.g. 1.0 is normal). MIN: 0.1, MAX: 5.0")
            .defineInRange("ttsRate", 1.0, 0.1, 5.0);

    static final ModConfigSpec SPEC = BUILDER.build();

    public enum TranslationProvider {
        AUTO,
        GOOGLE,
        MYMEMORY,
        DEEPL
    }
}
