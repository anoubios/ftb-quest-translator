package com.fqt.fqtmod.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fqt.fqtmod.FTBQuestTranslator;

public class GlossaryManager {
    private static final GlossaryManager INSTANCE = new GlossaryManager();
    
    public static class GlossaryTerm {
        public String original;
        public String translated;
        public String targetLang;
        
        public GlossaryTerm() {}
        public GlossaryTerm(String original, String translated, String targetLang) {
            this.original = original;
            this.translated = translated;
            this.targetLang = targetLang;
        }
    }

    private final List<GlossaryTerm> glossary = new ArrayList<>();
    private File configFile = null;
    private boolean initialized = false;

    public static GlossaryManager getInstance() { return INSTANCE; }

    public void init() {
        if (initialized) return;
        try {
            File configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config").toFile();
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            configFile = new File(configDir, "tfq_glossary.json");
            load();
            initialized = true;
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to initialize glossary manager", e);
        }
    }

    public void load() {
        if (configFile == null) return;
        if (!configFile.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            glossary.clear();
            if (root.isJsonObject()) {
                // Backwards compatibility for Map<String, String>
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loadedMap = new Gson().fromJson(root, type);
                if (loadedMap != null) {
                    for (Map.Entry<String, String> entry : loadedMap.entrySet()) {
                        glossary.add(new GlossaryTerm(entry.getKey(), entry.getValue(), "ALL"));
                    }
                }
                // Save the new format immediately
                save();
            } else if (root.isJsonArray()) {
                Type type = new TypeToken<List<GlossaryTerm>>(){}.getType();
                List<GlossaryTerm> loadedList = new Gson().fromJson(root, type);
                if (loadedList != null) {
                    glossary.addAll(loadedList);
                }
            }
            sortGlossary();
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to load glossary", e);
        }
    }

    public void sortGlossary() {
        // Sort by length descending to prevent partial matches
        glossary.sort((t1, t2) -> Integer.compare(t2.original.length(), t1.original.length()));
    }

    public void save() {
        if (configFile == null) return;
        try (FileWriter writer = new FileWriter(configFile)) {
            sortGlossary();
            new Gson().toJson(glossary, writer);
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to save glossary", e);
        }
    }
    
    public List<GlossaryTerm> getGlossary() {
        if (!initialized) init();
        return glossary;
    }
}
