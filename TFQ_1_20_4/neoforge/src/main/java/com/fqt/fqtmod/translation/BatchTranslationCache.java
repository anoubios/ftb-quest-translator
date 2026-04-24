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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchTranslationCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final BatchTranslationCache INSTANCE = new BatchTranslationCache();

    public static class TranslatedQuest {
        public String title;
        public String subtitle;
        public List<String> description;
    }

    // Key: "chapterId:targetLang" -> Value: Map of questId -> TranslatedQuest
    private final Map<String, Map<Long, TranslatedQuest>> cache = new ConcurrentHashMap<>();
    
    // Metadata: "chapterId:targetLang" -> "Chapter Name - Language" (for UI list)
    private final Map<String, String> cacheNames = new ConcurrentHashMap<>();

    private final File cacheFile;
    private final File metadataFile;

    private final ScheduledExecutorService saveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FTB-BatchCache-Save");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private ScheduledFuture<?> pendingSaveTask = null;
    private final AtomicBoolean isDirty = new AtomicBoolean(false);

    private BatchTranslationCache() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!configDir.exists()) configDir.mkdirs();
        cacheFile = new File(configDir, "ftbqt-batch-cache.json");
        metadataFile = new File(configDir, "ftbqt-batch-metadata.json");
        load();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            flush();
            saveExecutor.shutdown();
        }, "FTB-BatchCache-Shutdown"));
    }

    public static BatchTranslationCache getInstance() { return INSTANCE; }

    private void load() {
        if (cacheFile.exists()) {
            try (FileReader reader = new FileReader(cacheFile)) {
                Type type = new TypeToken<Map<String, Map<Long, TranslatedQuest>>>(){}.getType();
                Map<String, Map<Long, TranslatedQuest>> loaded = GSON.fromJson(reader, type);
                if (loaded != null) cache.putAll(loaded);
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("Failed to load batch cache", e);
            }
        }
        if (metadataFile.exists()) {
            try (FileReader reader = new FileReader(metadataFile)) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loaded = GSON.fromJson(reader, type);
                if (loaded != null) cacheNames.putAll(loaded);
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("Failed to load batch metadata", e);
            }
        }
    }

    public synchronized void flush() {
        if (!isDirty.get()) return;
        try {
            if (pendingSaveTask != null && !pendingSaveTask.isDone()) pendingSaveTask.cancel(false);
            try (FileWriter writer = new FileWriter(cacheFile)) { GSON.toJson(cache, writer); }
            try (FileWriter writer = new FileWriter(metadataFile)) { GSON.toJson(cacheNames, writer); }
            isDirty.set(false);
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to save batch cache", e);
        }
    }

    private synchronized void scheduleSave() {
        isDirty.set(true);
        if (pendingSaveTask != null && !pendingSaveTask.isDone()) pendingSaveTask.cancel(false);
        pendingSaveTask = saveExecutor.schedule(this::flush, 5, TimeUnit.SECONDS);
    }

    public String makeKey(long chapterId, String targetLang) { return chapterId + ":" + targetLang; }

    public void putQuest(long chapterId, String chapterName, String targetLang, long questId, TranslatedQuest q) {
        String key = makeKey(chapterId, targetLang);
        cache.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(questId, q);
        cacheNames.put(key, chapterName + " - " + targetLang);
        scheduleSave();
    }

    public TranslatedQuest getQuest(long chapterId, String targetLang, long questId) {
        Map<Long, TranslatedQuest> chapterCache = cache.get(makeKey(chapterId, targetLang));
        if (chapterCache != null) return chapterCache.get(questId);
        return null;
    }

    public Map<Long, TranslatedQuest> getChapterCache(String key) {
        return cache.get(key);
    }

    public boolean hasChapter(long chapterId, String targetLang) {
        return cache.containsKey(makeKey(chapterId, targetLang));
    }

    public void removeChapter(String key) {
        cache.remove(key);
        cacheNames.remove(key);
        scheduleSave();
    }

    public Map<String, String> getAvailableCaches() {
        return cacheNames;
    }
}