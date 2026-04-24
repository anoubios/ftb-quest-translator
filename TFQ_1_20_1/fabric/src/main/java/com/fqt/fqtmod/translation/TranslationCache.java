package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TranslationCache {
    private static final Gson GSON;
    private static final TranslationCache INSTANCE;

    static {
        GSON = new GsonBuilder().setPrettyPrinting().create();
        INSTANCE = new TranslationCache();
    }
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final File cacheFile;

    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FTB-Translator-Cache-Save");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private ScheduledFuture<?> pendingSaveTask = null;
    private final AtomicBoolean isDirty = new AtomicBoolean(false);

    private TranslationCache() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        cacheFile = new File(configDir, "ftbqt-cache.json");
        load();
        // Ensure cache is flushed when game exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            flush();
            saveExecutor.shutdown();
        }, "FTB-Translator-Cache-Shutdown"));
    }

    private void load() {
        if (cacheFile.exists()) {
            try (FileReader reader = new FileReader(cacheFile)) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    cache.putAll(loaded);
                }
                FTBQuestTranslator.LOGGER.info("Loaded {} translations from cache.", cache.size());
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("Failed to load translation cache: {}", e.getMessage());
            }
        }
    }

    public synchronized void flush() {
        if (!isDirty.get()) return;
        try {
            if (pendingSaveTask != null && !pendingSaveTask.isDone()) {
                pendingSaveTask.cancel(false);
            }
            try (FileWriter writer = new FileWriter(cacheFile)) {
                GSON.toJson(cache, writer);
            }
            isDirty.set(false);
            FTBQuestTranslator.LOGGER.info("Translation cache saved to disk.");
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to save translation cache: {}", e.getMessage());
        }
    }

    private synchronized void scheduleSave() {
        isDirty.set(true);
        if (pendingSaveTask != null && !pendingSaveTask.isDone()) {
            pendingSaveTask.cancel(false);
        }
        // Save 5 seconds after the last cache modification
        pendingSaveTask = saveExecutor.schedule(this::flush, 5, TimeUnit.SECONDS);
    }

    public static TranslationCache getInstance() { return INSTANCE; }

    private String makeKey(String text, String targetLang) { return targetLang + ":" + text; }
    
    public String get(String text, String targetLang) { return cache.get(makeKey(text, targetLang)); }
    
    public void put(String text, String targetLang, String translated) { 
        cache.put(makeKey(text, targetLang), translated); 
        scheduleSave();
    }
    
    public boolean has(String text, String targetLang) { return cache.containsKey(makeKey(text, targetLang)); }

    public void remove(String text, String targetLang) {
        if(text == null) return;
        cache.remove(makeKey(text, targetLang));
        scheduleSave();
    }

    public void clear() {
        cache.clear();
        scheduleSave();
        FTBQuestTranslator.LOGGER.info("Translation cache cleared");
    }

    public int size() { return cache.size(); }
}
