package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
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
    private File selectionFile;
    private BatchProgress progress = new BatchProgress();
    private java.util.Set<String> selectedCachesToView = new java.util.HashSet<>();
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private boolean isBatchTranslationActive = false; // State for toggling translated view
    private CompletableFuture<Void> workerFuture;

    public static class BatchProgress {
        public String targetLang = "";
        public List<Long> pendingQuestIds = new ArrayList<>();
        public int totalQuests = 0;
        public int completedQuests = 0;
    }

    public static BatchTranslationManager getInstance() {
        if (!INSTANCE.initialized) INSTANCE.init();
        return INSTANCE;
    }

    private boolean initialized = false;
    public void init() {
        if (initialized) return;
        initialized = true;
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        if (!configDir.exists()) configDir.mkdirs();
        progressFile = new File(configDir, "tfq_batch_progress.json");
        selectionFile = new File(configDir, "tfq_batch_selection.json");
        loadProgress();
        loadSelection();
    }

    private void loadSelection() {
        if (selectionFile != null && selectionFile.exists()) {
            try (FileReader reader = new FileReader(selectionFile)) {
                Type type = new TypeToken<java.util.HashSet<String>>(){}.getType();
                java.util.Set<String> loaded = GSON.fromJson(reader, type);
                if (loaded != null) selectedCachesToView = loaded;
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("Failed to load batch selection", e);
            }
        }
    }

    private void saveSelection() {
        if (selectionFile == null) return;
        try (FileWriter writer = new FileWriter(selectionFile)) {
            GSON.toJson(selectedCachesToView, writer);
        } catch (Exception e) {
            FTBQuestTranslator.LOGGER.error("Failed to save batch selection", e);
        }
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


    public void stopBatch() {
        isRunning.set(false);
        if (workerFuture != null && !workerFuture.isDone()) {
            workerFuture.cancel(true);
        }
        sendChat(net.minecraft.network.chat.Component.translatable("ftbquesttransl.batch.chat_stopped"));
    }

    public int getProgressPercentage() {
        if (progress.totalQuests == 0) return 0;
        return (int) (((double) progress.completedQuests / progress.totalQuests) * 100);
    }
    
    public int getCompletedQuests() { return progress.completedQuests; }
    public int getTotalQuests() { return progress.totalQuests; }

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
                    // Smart Rate Limiting: 1.5s between requests
                    Thread.sleep(1500); 
                    
                    Quest quest = (Quest) ClientQuestFile.INSTANCE.get(questId);
                    if (quest != null) {
                        dev.ftb.mods.ftbquests.quest.Chapter chapter = quest.getQuestChapter();
                        if (chapter != null && BatchTranslationCache.getInstance().getQuest(chapter.id, progress.targetLang, chapter.id) == null) {
                            String chRaw = chapter.getRawTitle();
                            if (chRaw.isEmpty()) chRaw = chapter.getTitle().getString();
                            String chResolved = TextFormatUtils.resolveTranslationKeys(chRaw);
                            TranslatableText chPrep = TextFormatUtils.prepareForTranslation(chResolved, progress.targetLang);
                            List<String> toTranslate = chPrep.getTextsForTranslation();
                            if (!toTranslate.isEmpty()) {
                                try {
                                    List<String> translatedTexts = provider.translateAll(toTranslate, progress.targetLang).join();
                                    String transChTitle = chPrep.reconstruct(translatedTexts);
                                    BatchTranslationCache.TranslatedQuest tqCh = new BatchTranslationCache.TranslatedQuest();
                                    tqCh.title = transChTitle;
                                    BatchTranslationCache.getInstance().putQuest(chapter.id, chapter.getRawTitle(), progress.targetLang, chapter.id, tqCh);
                                } catch (Exception e) {}
                            } else if (!chPrep.skip()) {
                                BatchTranslationCache.TranslatedQuest tqCh = new BatchTranslationCache.TranslatedQuest();
                                tqCh.title = chResolved;
                                BatchTranslationCache.getInstance().putQuest(chapter.id, chapter.getRawTitle(), progress.targetLang, chapter.id, tqCh);
                            }
                        }

                        String rawT = quest.getRawTitle();
                        if (rawT.isEmpty()) rawT = quest.getTitle().getString();
                        String title = TextFormatUtils.resolveTranslationKeys(rawT);
                        
                        String rawS = quest.getRawSubtitle();
                        if (rawS.isEmpty() && quest.getSubtitle() != null) rawS = quest.getSubtitle().getString();
                        String subtitle = TextFormatUtils.resolveTranslationKeys(rawS);
                        List<String> desc = new ArrayList<>();
                        for (String str : quest.getRawDescription()) desc.add(TextFormatUtils.resolveTranslationKeys(str));
                        
                        TranslatableText titlePrep = TextFormatUtils.prepareForTranslation(title, progress.targetLang);
                        TranslatableText subtitlePrep = TextFormatUtils.prepareForTranslation(subtitle, progress.targetLang);
                        List<TranslatableText> descPreps = TextFormatUtils.prepareDescriptionForTranslation(desc, progress.targetLang);
                        
                        List<String> allTexts = new ArrayList<>();
                        List<int[]> textMapping = new ArrayList<>();
                        
                        Runnable collect = () -> {
                            List<String> t = titlePrep.getTextsForTranslation();
                            if (!t.isEmpty()) { textMapping.add(new int[]{0, allTexts.size(), t.size()}); allTexts.addAll(t); }
                            
                            t = subtitlePrep.getTextsForTranslation();
                            if (!t.isEmpty()) { textMapping.add(new int[]{1, allTexts.size(), t.size()}); allTexts.addAll(t); }
                            
                            for (int d = 0; d < descPreps.size(); d++) {
                                t = descPreps.get(d).getTextsForTranslation();
                                if (!t.isEmpty()) { textMapping.add(new int[]{d + 2, allTexts.size(), t.size()}); allTexts.addAll(t); }
                            }
                        };
                        collect.run();
                        
                        if (!allTexts.isEmpty()) {
                            try {
                                List<String> translatedTexts = provider.translateAll(allTexts, progress.targetLang).join();
                                
                                String transTitle = title;
                                String transSubtitle = subtitle;
                                List<String> transDesc = new ArrayList<>();
                                
                                for (int[] mapping : textMapping) {
                                    int sourceIndex = mapping[0];
                                    int start = mapping[1];
                                    int count = mapping[2];
                                    List<String> segmentTranslations = translatedTexts.subList(start, start + count);
                                    if (sourceIndex == 0) transTitle = titlePrep.reconstruct(segmentTranslations);
                                    else if (sourceIndex == 1) transSubtitle = subtitlePrep.reconstruct(segmentTranslations);
                                }
                                for (int d = 0; d < descPreps.size(); d++) {
                                    TranslatableText prep = descPreps.get(d);
                                    if (prep.skip()) { transDesc.add(prep.original()); }
                                    else {
                                        int sourceIndex = d + 2;
                                        boolean found = false;
                                        for (int[] mapping : textMapping) {
                                            if (mapping[0] == sourceIndex) {
                                                int start = mapping[1];
                                                int count = mapping[2];
                                                transDesc.add(prep.reconstruct(translatedTexts.subList(start, start + count)));
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) transDesc.add(prep.original());
                                    }
                                }
                                if (titlePrep.skip()) transTitle = titlePrep.original();
                                if (subtitlePrep.skip()) transSubtitle = subtitlePrep.original() != null ? subtitlePrep.original() : subtitle;
                                
                                BatchTranslationCache.TranslatedQuest tq = new BatchTranslationCache.TranslatedQuest();
                                tq.title = transTitle;
                                tq.subtitle = transSubtitle;
                                tq.description = transDesc;
                                
                                BatchTranslationCache.getInstance().putQuest(quest.getQuestChapter().id, quest.getQuestChapter().getRawTitle(), progress.targetLang, questId, tq);
                            } catch (Exception e) {
                                FTBQuestTranslator.LOGGER.error("Failed to translate quest " + questId, e);
                            }
                        } else {
                            BatchTranslationCache.TranslatedQuest tq = new BatchTranslationCache.TranslatedQuest();
                            tq.title = title;
                            tq.subtitle = subtitle;
                            tq.description = new ArrayList<>(desc);
                            BatchTranslationCache.getInstance().putQuest(quest.getQuestChapter().id, quest.getQuestChapter().getRawTitle(), progress.targetLang, questId, tq);
                        }
                    }
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
    public void setBatchTranslationActive(boolean active) { this.isBatchTranslationActive = active; }

    public boolean isCacheSelectedForView(String cacheKey) {
        return selectedCachesToView.contains(cacheKey);
    }

    public String getSelectedLangForChapter(long chapterId) {
        if (!isBatchTranslationActive) return null;
        for (String key : selectedCachesToView) {
            String prefix = chapterId + ":";
            if (key.startsWith(prefix)) {
                return key.substring(prefix.length());
            }
        }
        return null;
    }

    public void setSelectedCachesForView(java.util.Set<String> caches) {
        this.selectedCachesToView = new java.util.HashSet<>(caches);
        saveSelection();
    }

    public java.util.Set<String> getSelectedCachesForView() {
        return new java.util.HashSet<>(selectedCachesToView);
    }

    private void sendChat(Component msg) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(msg, false);
            }
        });
    }
}
