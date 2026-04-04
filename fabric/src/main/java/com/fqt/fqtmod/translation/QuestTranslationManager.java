package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.FTBQuestTranslator;
import com.fqt.fqtmod.FabricConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuestTranslationManager {
    private static final QuestTranslationManager INSTANCE = new QuestTranslationManager();

    private long activeQuestId = 0;
    private String translatedTitle = null;
    private String translatedSubtitle = null;
    private List<String> translatedDescription = null;
    private boolean isTranslating = false;
    private boolean isTranslated = false;
    private Runnable onTranslationComplete = null;

    public static QuestTranslationManager getInstance() { return INSTANCE; }

    public String getTargetLanguage() {
        String lang = FabricConfig.getTargetLanguage();
        if ("auto".equalsIgnoreCase(lang)) {
            String mcLang = Minecraft.getInstance().options.languageCode;
            if (mcLang != null && mcLang.contains("_")) return mcLang.split("_")[0];
            return mcLang != null ? mcLang : "en";
        }
        return lang;
    }

    private TranslationProvider getProvider() {
        String provider = FabricConfig.getTranslationProvider();
        if ("MYMEMORY".equalsIgnoreCase(provider)) return new MyMemoryTranslateProvider();
        return new GoogleTranslateProvider();
    }

    public void toggleTranslation(long questId, String title, String subtitle, List<String> description, Runnable onComplete) {
        if (isTranslated && activeQuestId == questId) {
            clearTranslation();
            if (onComplete != null) onComplete.run();
            return;
        }
        if (isTranslating) return;

        activeQuestId = questId;
        isTranslating = true;
        isTranslated = false;
        onTranslationComplete = onComplete;

        String targetLang = getTargetLanguage();
        TranslationProvider provider = getProvider();
        TranslationCache cache = TranslationCache.getInstance();
        boolean useCache = FabricConfig.isEnableCaching();

        FTBQuestTranslator.LOGGER.info("Starting translation to '{}' using {} for quest {}",
                targetLang, provider.getName(), Long.toHexString(questId));

        TextFormatUtils.TranslatableText titlePrep = TextFormatUtils.prepareForTranslation(title);
        TextFormatUtils.TranslatableText subtitlePrep = TextFormatUtils.prepareForTranslation(subtitle);
        List<TextFormatUtils.TranslatableText> descPreps = TextFormatUtils.prepareDescriptionForTranslation(description);

        // Check cache
        if (useCache) {
            String cachedTitle = titlePrep.skip() ? titlePrep.original() : cache.get(titlePrep.plainText(), targetLang);
            String cachedSubtitle = subtitlePrep.skip() ? subtitlePrep.original() : cache.get(subtitlePrep.plainText(), targetLang);
            boolean allCached = (cachedTitle != null) && (cachedSubtitle != null || subtitlePrep.skip());
            List<String> cachedDescLines = new ArrayList<>();

            if (allCached) {
                for (TextFormatUtils.TranslatableText prep : descPreps) {
                    if (prep.skip()) { cachedDescLines.add(prep.original()); }
                    else {
                        String cached = cache.get(prep.plainText(), targetLang);
                        if (cached != null) { cachedDescLines.add(prep.reconstruct(cached)); }
                        else { allCached = false; break; }
                    }
                }
            }

            if (allCached && cachedTitle != null) {
                translatedTitle = titlePrep.reconstruct(cachedTitle);
                translatedSubtitle = cachedSubtitle != null ? subtitlePrep.reconstruct(cachedSubtitle) : subtitle;
                translatedDescription = cachedDescLines;
                isTranslating = false;
                isTranslated = true;
                FTBQuestTranslator.LOGGER.info("Translation loaded from cache");
                if (onTranslationComplete != null) Minecraft.getInstance().execute(onTranslationComplete);
                return;
            }
        }

        // Translate async
        CompletableFuture<String> titleFuture = titlePrep.skip()
                ? CompletableFuture.completedFuture(null)
                : provider.translate(titlePrep.plainText(), targetLang);
        CompletableFuture<String> subtitleFuture = subtitlePrep.skip()
                ? CompletableFuture.completedFuture(null)
                : provider.translate(subtitlePrep.plainText(), targetLang);

        List<CompletableFuture<String>> descFutures = new ArrayList<>();
        for (TextFormatUtils.TranslatableText prep : descPreps) {
            descFutures.add(prep.skip()
                    ? CompletableFuture.completedFuture(null)
                    : provider.translate(prep.plainText(), targetLang));
        }

        CompletableFuture.allOf(
                titleFuture, subtitleFuture,
                CompletableFuture.allOf(descFutures.toArray(new CompletableFuture[0]))
        ).thenRun(() -> {
            try {
                String rawTitle = titleFuture.join();
                translatedTitle = rawTitle != null ? titlePrep.reconstruct(rawTitle) : title;
                String rawSubtitle = subtitleFuture.join();
                translatedSubtitle = rawSubtitle != null ? subtitlePrep.reconstruct(rawSubtitle) : subtitle;

                translatedDescription = new ArrayList<>();
                for (int i = 0; i < descFutures.size(); i++) {
                    String raw = descFutures.get(i).join();
                    translatedDescription.add(raw != null ? descPreps.get(i).reconstruct(raw) : descPreps.get(i).original());
                }

                if (useCache) {
                    if (rawTitle != null && !titlePrep.skip()) cache.put(titlePrep.plainText(), targetLang, rawTitle);
                    if (rawSubtitle != null && !subtitlePrep.skip()) cache.put(subtitlePrep.plainText(), targetLang, rawSubtitle);
                    for (int i = 0; i < descPreps.size(); i++) {
                        String raw = descFutures.get(i).join();
                        if (raw != null && !descPreps.get(i).skip()) cache.put(descPreps.get(i).plainText(), targetLang, raw);
                    }
                }

                isTranslated = true;
                isTranslating = false;
                FTBQuestTranslator.LOGGER.info("Translation complete!");
                if (onTranslationComplete != null) Minecraft.getInstance().execute(onTranslationComplete);
            } catch (Exception e) {
                FTBQuestTranslator.LOGGER.error("Translation failed: {}", e.getMessage());
                isTranslating = false;
                clearTranslation();
            }
        });
    }

    public void clearTranslation() {
        activeQuestId = 0;
        translatedTitle = null;
        translatedSubtitle = null;
        translatedDescription = null;
        isTranslating = false;
        isTranslated = false;
        onTranslationComplete = null;
    }

    public boolean isActiveFor(long questId) { return isTranslated && activeQuestId == questId; }
    public boolean isTranslating() { return isTranslating; }
    public boolean isTranslated() { return isTranslated; }
    public long getActiveQuestId() { return activeQuestId; }
    public String getTranslatedTitle() { return translatedTitle; }
    public String getTranslatedSubtitle() { return translatedSubtitle; }
    public List<String> getTranslatedDescription() { return translatedDescription; }

    public Component getStatusComponent() {
        if (isTranslating) return Component.translatable("ftbquesttransl.translating");
        else if (isTranslated) return Component.translatable("ftbquesttransl.translated");
        else return Component.translatable("ftbquesttransl.translate");
    }
}
