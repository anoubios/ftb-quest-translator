package com.fqt.fqtmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SecretsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("local").resolve("ftbquesttranslator-secrets.json");
    }

    private static SecretData data = new SecretData();
    private static boolean loaded = false;

    public static void load() {
        try {
            Path path = getConfigPath();
            if (Files.exists(path)) {
                String json = Files.readString(path);
                data = GSON.fromJson(json, SecretData.class);
                if (data == null) data = new SecretData();
                LOGGER.info("Secrets config loaded from {}", path);
            } else {
                save();
            }
            loaded = true;
        } catch (Exception e) {
            LOGGER.error("Failed to load secrets config: {}", e.getMessage());
            data = new SecretData();
        }
    }

    public static void save() {
        try {
            Path path = getConfigPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(data));
        } catch (Exception e) {
            LOGGER.error("Failed to save secrets config: {}", e.getMessage());
        }
    }

    public static String getDeepLApiKey() {
        if (!loaded) load();
        return data.deepLApiKey != null ? data.deepLApiKey : "";
    }

    public static void setDeepLApiKey(String v) {
        data.deepLApiKey = v;
        save();
    }

    private static class SecretData {
        public String deepLApiKey = "";
    }
}
