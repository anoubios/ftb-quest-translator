package com.fqt.fqtmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON-based config for Fabric.
 * File: .minecraft/config/ftbquesttransl.json
 */
public class FabricConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("ftbquesttransl.json");

    private static ConfigData data = new ConfigData();

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                data = GSON.fromJson(json, ConfigData.class);
                if (data == null) data = new ConfigData();
                LOGGER.info("Config loaded from {}", CONFIG_PATH);
            } else {
                save(); // Create default
                LOGGER.info("Default config created at {}", CONFIG_PATH);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config: {}", e.getMessage());
            data = new ConfigData();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    public static String getTargetLanguage() {
        return data.targetLanguage;
    }
    public static void setTargetLanguage(String v) {
        data.targetLanguage = v;
        save();
    }

    public static boolean isEnableCaching() {
        return data.enableCaching;
    }
    public static void setEnableCaching(boolean v) {
        data.enableCaching = v;
        save();
    }

    public static TranslationProvider getTranslationProvider() {
        return data.translationProvider;
    }
    public static void setTranslationProvider(TranslationProvider v) {
        data.translationProvider = v;
        save();
    }

    public static boolean isEnableTranslatingAnimation() {
        return data.enableTranslatingAnimation;
    }
    public static void setEnableTranslatingAnimation(boolean v) {
        data.enableTranslatingAnimation = v;
        save();
    }

    public static boolean isEnableTranslatedAnimation() {
        return data.enableTranslatedAnimation;
    }
    public static void setEnableTranslatedAnimation(boolean v) {
        data.enableTranslatedAnimation = v;
        save();
    }

    public static boolean isEnableLargeTextWarning() {
        return data.enableLargeTextWarning;
    }
    public static void setEnableLargeTextWarning(boolean v) {
        data.enableLargeTextWarning = v;
        save();
    }

    public static int getLargeTextThreshold() {
        return data.largeTextThreshold;
    }
    public static void setLargeTextThreshold(int v) {
        data.largeTextThreshold = v;
        save();
    }

    public static boolean isEnableTtsLoadingAnimation() {
        return data.enableTtsLoadingAnimation;
    }
    public static void setEnableTtsLoadingAnimation(boolean v) {
        data.enableTtsLoadingAnimation = v;
        save();
    }

    public static boolean isEnableTtsPlayingAnimation() {
        return data.enableTtsPlayingAnimation;
    }
    public static void setEnableTtsPlayingAnimation(boolean v) {
        data.enableTtsPlayingAnimation = v;
        save();
    }

    public static boolean isEnableTranslateButton() {
        return data.enableTranslateButton;
    }
    public static void setEnableTranslateButton(boolean v) {
        data.enableTranslateButton = v;
        save();
    }

    public static boolean isEnableTtsButton() {
        return data.enableTtsButton;
    }
    public static void setEnableTtsButton(boolean v) {
        data.enableTtsButton = v;
        save();
    }

    public static int getTranslationAnimationDuration() {
        return data.translationAnimationDuration;
    }
    public static void setTranslationAnimationDuration(int v) {
        data.translationAnimationDuration = Math.max(500, Math.min(30000, v));
        save();
    }

    public static double getTtsPitch() {
        return data.ttsPitch;
    }
    public static void setTtsPitch(double v) {
        data.ttsPitch = Math.max(0.1, Math.min(5.0, v));
        save();
    }

    public static double getTtsRate() {
        return data.ttsRate;
    }
    public static void setTtsRate(double v) {
        data.ttsRate = Math.max(0.1, Math.min(5.0, v));
        save();
    }

    public enum TranslationProvider {
        AUTO,
        GOOGLE,
        MYMEMORY,
        DEEPL
    }

    public static class ConfigData {
        public String targetLanguage = "auto";
        public boolean enableCaching = true;
        public TranslationProvider translationProvider = TranslationProvider.AUTO;
        public boolean enableTranslatingAnimation = true;
        public boolean enableTranslatedAnimation = true;
        public boolean enableLargeTextWarning = true;
        public int largeTextThreshold = 2000;
        public boolean enableTtsLoadingAnimation = true;
        public boolean enableTtsPlayingAnimation = true;
        public boolean enableTranslateButton = true;
        public boolean enableTtsButton = true;
        public int translationAnimationDuration = 5000;
        public double ttsPitch = 1.0;
        public double ttsRate = 1.0;
    }
}
