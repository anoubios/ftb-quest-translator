package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import com.fqt.fqtmod.translation.TextFormatUtils.TranslatableText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchTranslationManager {
    private static final BatchTranslationManager INSTANCE = new BatchTranslationManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private File progressFile;
    private BatchProgress progress = new BatchProgress();
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private boolean isBatchTranslationActive = false; // State for toggling translated view
    private CompletableFuture<Void> workerFuture;

    public static class BatchProgress {
        public String targetLang = "";
        public List<Long> pendingQuestIds = new ArrayList<>();
        public int totalQuests = 0;
        public int completedQuests = 0;
    }

    public static BatchTranslationManager getInstance() { return INSTANCE; }

    public void init() {
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!configDir.exists()) configDir.mkdirs();
        progressFile = new File(configDir, "tfq_batch_progress.json");
        loadProgress();
    }

    private void loadProgress() {
        if (progressFile != null && progressFile.exists()) {
            try (FileReader reader = new FileReader(progressFile)) {
                Type type = new TypeToken<BatchProgress>(){}.getType();
                BatchProgress loaded = GSON.fromJson(reader, type);
                if (loaded != null) progress = loaded;
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("Failed to load batch progress", e);
            }
        }
    }

    private void saveProgress() {
        if (progressFile == null) return;
        try (FileWriter writer = new FileWriter(progressFile)) {
            GSON.toJson(progress, writer);
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to save batch progress", e);
        }
    }

    public void startBatch(String targetLang, List<Long> questIds) {
        if (isRunning.get()) return;
        
        progress.targetLang = targetLang;
        progress.pendingQuestIds = new ArrayList<>(questIds);
        progress.totalQuests = questIds.size();
        progress.completedQuests = 0;
        saveProgress();
        
        resumeBatch();
    }

    public void resumeBatch() {
        if (isRunning.get() || progress.pendingQuestIds.isEmpty()) return;
        
        isRunning.set(true);
        sendChat(Component.translatable("ftbquesttransl.batch.chat_started", progress.targetLang, progress.pendingQuestIds.size()));
        
        workerFuture = CompletableFuture.runAsync(() -> {
            TranslationProvider provider = QuestTranslationManager.getInstance().getActiveProvider();
            TranslationCache cache = TranslationCache.getInstance();
            
            while (isRunning.get() && !progress.pendingQuestIds.isEmpty()) {
                Long questId = progress.pendingQuestIds.get(0);
                
                try {
                    // We will inject a hook into FTB Quests to fetch quest text by ID here
                    // For now, just simulate work and remove from queue
                    
                    Thread.sleep(1500); // Smart Rate Limiting: 1.5s between requests
                    
                    progress.pendingQuestIds.remove(0);
                    progress.completedQuests++;
                    
                    if (progress.completedQuests % 10 == 0) {
                        saveProgress();
                        int percent = (int) ((progress.completedQuests / (float) progress.totalQuests) * 100);
                        sendChat(Component.translatable("ftbquesttransl.batch.chat_progress", percent, progress.completedQuests, progress.totalQuests));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    FTBQuestTranslator.LOGGER.error("Batch worker error", e);
                }
            }
            
            isRunning.set(false);
            saveProgress();
            
            if (progress.pendingQuestIds.isEmpty()) {
                sendChat(Component.translatable("ftbquesttransl.batch.chat_completed"));
            } else {
                sendChat(Component.translatable("ftbquesttransl.batch.chat_paused"));
            }
        }, QuestTranslationManager.HTTP_EXECUTOR);
    }

    public void pauseBatch() {
        isRunning.set(false);
        if (workerFuture != null) {
            workerFuture.cancel(true);
        }
    }

    public void cancelBatch() {
        pauseBatch();
        progress.pendingQuestIds.clear();
        progress.completedQuests = 0;
        progress.totalQuests = 0;
        saveProgress();
        sendChat(Component.translatable("ftbquesttransl.batch.chat_cancelled"));
    }

    public boolean isRunning() { return isRunning.get(); }
    public BatchProgress getProgress() { return progress; }

    public boolean isBatchTranslationActive() { return isBatchTranslationActive; }
    public void toggleBatchTranslationActive() { this.isBatchTranslationActive = !this.isBatchTranslationActive; }

    private void sendChat(Component msg) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(msg, false);
            }
        });
    }
}
