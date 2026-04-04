package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.Config;
import com.fqt.fqtmod.FTBQuestTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Central manager for quest translation state.
 * Tracks which quest is being translated and stores translated texts.
 */
public class QuestTranslationManager {
    private static final QuestTranslationManager INSTANCE = new QuestTranslationManager();

    private long activeQuestId = 0;
    private String translatedTitle = null;
    private String translatedSubtitle = null;
    private List<String> translatedDescription = null;
    private boolean isTranslating = false;
    private boolean isTranslated = false;
    private Runnable onTranslationComplete = null;

    public static QuestTranslationManager getInstance() {
        return INSTANCE;
    }

    /**
     * Get the resolved target language code.
     * If config is "auto", uses Minecraft's language setting.
     */
    public String getTargetLanguage() {
        String lang = Config.TARGET_LANGUAGE.get();
        if ("auto".equalsIgnoreCase(lang)) {
            String mcLang = Minecraft.getInstance().options.languageCode;
            if (mcLang != null && mcLang.contains("_")) {
                return mcLang.split("_")[0];
            }
            return mcLang != null ? mcLang : "en";
        }
        return lang;
    }

    private TranslationProvider getProvider() {
        return switch (Config.TRANSLATION_PROVIDER.get()) {
            case MYMEMORY -> new MyMemoryTranslateProvider();
            case GOOGLE -> new GoogleTranslateProvider();
        };
    }

    /**
     * Toggle translation for a quest.
     */
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
        boolean useCache = Config.ENABLE_CACHING.get();

        FTBQuestTranslator.LOGGER.info("Starting translation to '{}' using {} for quest {}",
                targetLang, provider.getName(), Long.toHexString(questId));

        // Prepare texts - strip formatting codes
        TextFormatUtils.TranslatableText titlePrep = TextFormatUtils.prepareForTranslation(title);
        TextFormatUtils.TranslatableText subtitlePrep = TextFormatUtils.prepareForTranslation(subtitle);
        List<TextFormatUtils.TranslatableText> descPreps = TextFormatUtils.prepareDescriptionForTranslation(description);

        // Check cache for all texts
        if (useCache) {
            String cachedTitle = titlePrep.skip() ? titlePrep.original() : cache.get(titlePrep.plainText(), targetLang);
            String cachedSubtitle = subtitlePrep.skip() ? subtitlePrep.original() : cache.get(subtitlePrep.plainText(), targetLang);

            boolean allCached = (cachedTitle != null) && (cachedSubtitle != null || subtitlePrep.skip());
            List<String> cachedDescLines = new ArrayList<>();

            if (allCached) {
                for (TextFormatUtils.TranslatableText prep : descPreps) {
                    if (prep.skip()) {
                        cachedDescLines.add(prep.original());
                    } else {
                        String cached = cache.get(prep.plainText(), targetLang);
                        if (cached != null) {
                            cachedDescLines.add(prep.reconstruct(cached));
                        } else {
                            allCached = false;
                            break;
                        }
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
                if (onTranslationComplete != null) {
                    Minecraft.getInstance().execute(onTranslationComplete);
                }
                return;
            }
        }

        // Translate asynchronously - only translate non-skipped, plain text parts
        CompletableFuture<String> titleFuture = titlePrep.skip()
                ? CompletableFuture.completedFuture(null)
                : provider.translate(titlePrep.plainText(), targetLang);

        CompletableFuture<String> subtitleFuture = subtitlePrep.skip()
                ? CompletableFuture.completedFuture(null)
                : provider.translate(subtitlePrep.plainText(), targetLang);

        List<CompletableFuture<String>> descFutures = new ArrayList<>();
        for (TextFormatUtils.TranslatableText prep : descPreps) {
            if (prep.skip()) {
                descFutures.add(CompletableFuture.completedFuture(null));
            } else {
                descFutures.add(provider.translate(prep.plainText(), targetLang));
            }
        }

        CompletableFuture<Void> allDescriptions = CompletableFuture.allOf(
                descFutures.toArray(new CompletableFuture[0])
        );

        CompletableFuture.allOf(titleFuture, subtitleFuture, allDescriptions)
                .thenRun(() -> {
                    try {
                        // Reconstruct title with formatting
                        String rawTranslatedTitle = titleFuture.join();
                        translatedTitle = rawTranslatedTitle != null
                                ? titlePrep.reconstruct(rawTranslatedTitle)
                                : title;

                        // Reconstruct subtitle with formatting
                        String rawTranslatedSubtitle = subtitleFuture.join();
                        translatedSubtitle = rawTranslatedSubtitle != null
                                ? subtitlePrep.reconstruct(rawTranslatedSubtitle)
                                : subtitle;

                        // Reconstruct description lines with formatting
                        translatedDescription = new ArrayList<>();
                        for (int i = 0; i < descFutures.size(); i++) {
                            String rawTranslated = descFutures.get(i).join();
                            TextFormatUtils.TranslatableText prep = descPreps.get(i);
                            if (rawTranslated != null) {
                                translatedDescription.add(prep.reconstruct(rawTranslated));
                            } else {
                                translatedDescription.add(prep.original());
                            }
                        }

                        // Cache the plain text translations
                        if (useCache) {
                            if (rawTranslatedTitle != null && !titlePrep.skip()) {
                                cache.put(titlePrep.plainText(), targetLang, rawTranslatedTitle);
                            }
                            if (rawTranslatedSubtitle != null && !subtitlePrep.skip()) {
                                cache.put(subtitlePrep.plainText(), targetLang, rawTranslatedSubtitle);
                            }
                            for (int i = 0; i < descPreps.size(); i++) {
                                TextFormatUtils.TranslatableText prep = descPreps.get(i);
                                String rawTranslated = descFutures.get(i).join();
                                if (rawTranslated != null && !prep.skip()) {
                                    cache.put(prep.plainText(), targetLang, rawTranslated);
                                }
                            }
                        }

                        isTranslated = true;
                        isTranslating = false;

                        FTBQuestTranslator.LOGGER.info("Translation complete!");

                        if (onTranslationComplete != null) {
                            Minecraft.getInstance().execute(onTranslationComplete);
                        }
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

    public boolean isActiveFor(long questId) {
        return isTranslated && activeQuestId == questId;
    }

    public boolean isTranslating() {
        return isTranslating;
    }

    public boolean isTranslated() {
        return isTranslated;
    }

    public long getActiveQuestId() {
        return activeQuestId;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public String getTranslatedSubtitle() {
        return translatedSubtitle;
    }

    public List<String> getTranslatedDescription() {
        return translatedDescription;
    }

    public Component getStatusComponent() {
        if (isTranslating) {
            return Component.translatable("ftbquesttransl.translating");
        } else if (isTranslated) {
            return Component.translatable("ftbquesttransl.translated");
        } else {
            return Component.translatable("ftbquesttransl.translate");
        }
    }
}
