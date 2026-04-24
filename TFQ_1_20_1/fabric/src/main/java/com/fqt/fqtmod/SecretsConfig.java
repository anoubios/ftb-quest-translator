package com.fqt.fqtmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SecretsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SECRETS_DIR = FabricLoader.getInstance().getGameDir().resolve("local");
    private static final Path CONFIG_PATH = SECRETS_DIR.resolve("ftbquesttranslator-secrets.json");

    private static SecretData data = new SecretData();

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                data = GSON.fromJson(json, SecretData.class);
                if (data == null) data = new SecretData();
            } else {
                save();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load secrets config: {}", e.getMessage());
            data = new SecretData();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data));
        } catch (IOException e) {
            LOGGER.error("Failed to save secrets config: {}", e.getMessage());
        }
    }

    public static String getDeepLApiKey() {
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
