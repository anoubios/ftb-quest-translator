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
    private static final Path CONFIG_PATH = Path.of("config", "ftbquesttransl.json");

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

    public static boolean isEnableCaching() {
        return data.enableCaching;
    }

    public static String getTranslationProvider() {
        return data.translationProvider;
    }

    public static class ConfigData {
        public String targetLanguage = "auto";
        public boolean enableCaching = true;
        public String translationProvider = "GOOGLE";
    }
}
