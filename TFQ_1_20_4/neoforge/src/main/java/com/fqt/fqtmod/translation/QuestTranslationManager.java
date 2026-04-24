package com.fqt.fqtmod.translation;

import com.fqt.fqtmod.SecretsConfig;
import com.fqt.fqtmod.FTBQuestTranslator;
import com.fqt.fqtmod.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central manager for quest translation state.
 * Uses segment-based translation to preserve formatting codes.
 * Supports AUTO mode with automatic provider fallback chain.
 */
public class QuestTranslationManager {
    private static final QuestTranslationManager INSTANCE = new QuestTranslationManager();

    public static final ExecutorService HTTP_EXECUTOR = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()), new ThreadFactory() {
        private int count = 1;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "FTB-Translator-HTTP-" + (count++));
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    });

    private long activeQuestId = 0;
    private String translatedTitle = null;
    private String translatedSubtitle = null;
    private List<String> translatedDescription = null;
    private boolean isTranslating = false;
    private boolean isTranslated = false;
    private long translationTime = 0;
    private boolean connectionError = false;
    private Runnable onTranslationComplete = null;

    public long getTranslationTime() { return translationTime; }

    public static QuestTranslationManager getInstance() { return INSTANCE; }

    private final AtomicBoolean isCheckingConnection = new AtomicBoolean(false);
    private long lastConnectionCheckTime = 0;

    public void checkConnectionInBackground() {
        long now = System.currentTimeMillis();
        if (now - lastConnectionCheckTime < 120_000) return;

        if (isCheckingConnection.compareAndSet(false, true)) {
            if (Config.TRANSLATION_PROVIDER.get() == Config.TranslationProvider.AUTO) {
                List<CompletableFuture<Boolean>> checks = new ArrayList<>();
                for (ProviderEntry entry : autoChain) {
                    checks.add(entry.provider.checkConnectionAsync().exceptionally(e -> false));
                }
                CompletableFuture.allOf(checks.toArray(new CompletableFuture[0])).thenRun(() -> {
                    boolean anyAvailable = false;
                    for (CompletableFuture<Boolean> check : checks) {
                        try { if (check.join()) { anyAvailable = true; break; } } catch (Exception e) { /* skip */ }
                    }
                    this.connectionError = !anyAvailable;
                    this.lastConnectionCheckTime = System.currentTimeMillis();
                    this.isCheckingConnection.set(false);
                });
            } else {
                getProvider().checkConnectionAsync().thenAccept(success -> {
                    this.connectionError = !success;
                    this.lastConnectionCheckTime = System.currentTimeMillis();
                    this.isCheckingConnection.set(false);
                }).exceptionally(e -> {
                    this.connectionError = true;
                    this.lastConnectionCheckTime = System.currentTimeMillis();
                    this.isCheckingConnection.set(false);
                    return null;
                });
            }
        }
    }

    public String getTargetLanguage() {
        String lang = Config.TARGET_LANGUAGE.get();
        if ("auto".equalsIgnoreCase(lang)) {
            String mcLang = Minecraft.getInstance().options.languageCode;
            return LanguageMapper.getApiLanguageCode(mcLang);
        }
        return LanguageMapper.getApiLanguageCode(lang);
    }

    private final TranslationProvider myMemoryProvider = new MyMemoryTranslateProvider();
    private final TranslationProvider deepLProvider = new DeepLTranslateProvider();
    private final TranslationProvider googleProvider = new GoogleTranslateProvider();

    private final List<ProviderEntry> autoChain = List.of(
            new ProviderEntry("Google", googleProvider),
            new ProviderEntry("MyMemory", myMemoryProvider)
    );

    private final Map<String, Boolean> providerAvailability = new ConcurrentHashMap<>();
    private long lastProbeTime = 0;

    private Config.TranslationProvider lastProviderEnum = null;
    private String lastDeepLApiKey = null;

    private TranslationProvider getProvider() {
        Config.TranslationProvider provider = Config.TRANSLATION_PROVIDER.get();
        String currentKey = SecretsConfig.getDeepLApiKey();
        if (provider != lastProviderEnum || !java.util.Objects.equals(currentKey, lastDeepLApiKey)) {
            lastProviderEnum = provider; lastDeepLApiKey = currentKey; lastConnectionCheckTime = 0; connectionError = false;
            providerAvailability.clear(); lastProbeTime = 0; // Force re-probe on provider change
        }
        if (provider == Config.TranslationProvider.AUTO) return getAutoProvider();
        if (provider == Config.TranslationProvider.MYMEMORY) return myMemoryProvider;
        if (provider == Config.TranslationProvider.DEEPL) return deepLProvider;
        return googleProvider;
    }

    private TranslationProvider getAutoProvider() {
        if (System.currentTimeMillis() - lastProbeTime > 600_000) probeProvidersAsync();
        String targetLang = getTargetLanguage();
        for (ProviderEntry entry : autoChain) { Boolean available = providerAvailability.get(entry.name); if ((available == null || available) && entry.provider.supportsLanguage(targetLang)) return entry.provider; }
        return googleProvider;
    }

    public String getActiveAutoProviderName() {
        if (Config.TRANSLATION_PROVIDER.get() != Config.TranslationProvider.AUTO) return getProvider().getName();
        for (ProviderEntry entry : autoChain) { Boolean available = providerAvailability.get(entry.name); if (available == null || available) return entry.provider.getName(); }
        return googleProvider.getName();
    }

    private void probeProvidersAsync() {
        lastProbeTime = System.currentTimeMillis();
        for (ProviderEntry entry : autoChain) {
            entry.provider.checkConnectionAsync().thenAccept(ok -> { providerAvailability.put(entry.name, ok); FTBQuestTranslator.LOGGER.debug("Provider {} availability: {}", entry.name, ok); }).exceptionally(e -> { providerAvailability.put(entry.name, false); return null; });
        }
    }

    private TranslationProvider getNextAutoProvider(TranslationProvider failedProvider) {
        boolean foundFailed = false;
        String targetLang = getTargetLanguage();
        for (ProviderEntry entry : autoChain) {
            if (entry.provider == failedProvider) { providerAvailability.put(entry.name, false); foundFailed = true; continue; }
            if (foundFailed) { Boolean available = providerAvailability.get(entry.name); if ((available == null || available) && entry.provider.supportsLanguage(targetLang)) return entry.provider; }
        }
        return null;
    }

    public void toggleTranslation(long questId, String title, String subtitle, List<String> description, Runnable onComplete) {
        if (isTranslated && activeQuestId == questId) { clearTranslation(); if (onComplete != null) onComplete.run(); return; }
        if (isTranslating) return;
        
        title = TextFormatUtils.resolveTranslationKeys(title);
        subtitle = TextFormatUtils.resolveTranslationKeys(subtitle);
        if (description != null) {
            List<String> resDesc = new ArrayList<>();
            for (String str : description) resDesc.add(TextFormatUtils.resolveTranslationKeys(str));
            description = resDesc;
        }

        activeQuestId = questId; isTranslating = true; isTranslated = false; onTranslationComplete = onComplete;
        String targetLang = getTargetLanguage(); TranslationProvider provider = getProvider(); TranslationCache cache = TranslationCache.getInstance(); boolean useCache = Config.ENABLE_CACHING.get();
        FTBQuestTranslator.LOGGER.info("Starting translation to '{}' using {} for quest {}", targetLang, provider.getName(), Long.toHexString(questId));
        // Diagnostic: log raw quest data
        FTBQuestTranslator.LOGGER.info("Quest data - title: '{}', subtitle: '{}', description lines: {}", title, subtitle, description != null ? description.size() : 0);
        if (description != null) { for (int i = 0; i < Math.min(description.size(), 3); i++) FTBQuestTranslator.LOGGER.debug("  desc[{}]: '{}'", i, description.get(i)); }
        TextFormatUtils.TranslatableText titlePrep = TextFormatUtils.prepareForTranslation(title, targetLang); TextFormatUtils.TranslatableText subtitlePrep = TextFormatUtils.prepareForTranslation(subtitle, targetLang); List<TextFormatUtils.TranslatableText> descPreps = TextFormatUtils.prepareDescriptionForTranslation(description, targetLang);
        List<String> allTexts = new ArrayList<>(); List<int[]> textMapping = new ArrayList<>();
        collectTexts(titlePrep, 0, allTexts, textMapping); collectTexts(subtitlePrep, 1, allTexts, textMapping);
        for (int d = 0; d < descPreps.size(); d++) collectTexts(descPreps.get(d), d + 2, allTexts, textMapping);
        // Nothing to translate (all formatting codes or empty text)
        if (allTexts.isEmpty()) { translatedTitle = title; translatedSubtitle = subtitle; translatedDescription = new ArrayList<>(description); isTranslating = false; isTranslated = true; translationTime = System.currentTimeMillis(); FTBQuestTranslator.LOGGER.warn("No translatable text found for quest {} (title skip={}, subtitle skip={})", Long.toHexString(questId), titlePrep.skip(), subtitlePrep.skip()); if (onTranslationComplete != null) Minecraft.getInstance().execute(onTranslationComplete); return; }
        FTBQuestTranslator.LOGGER.info("Prepared {} text segments for translation", allTexts.size());
        if (useCache) { boolean allCached = true; List<String> cachedTranslations = new ArrayList<>(); for (String text : allTexts) { String cached = cache.get(text, targetLang); if (cached != null) cachedTranslations.add(cached); else { allCached = false; break; } } if (allCached) { applyTranslations(cachedTranslations, textMapping, titlePrep, subtitlePrep, descPreps, title, subtitle); isTranslating = false; isTranslated = true; translationTime = System.currentTimeMillis(); FTBQuestTranslator.LOGGER.info("Translation loaded from cache"); if (onTranslationComplete != null) Minecraft.getInstance().execute(onTranslationComplete); return; } }
        executeTranslation(provider, allTexts, textMapping, titlePrep, subtitlePrep, descPreps, title, subtitle, targetLang, useCache, cache);
    }

    private void executeTranslation(TranslationProvider provider, List<String> allTexts, List<int[]> textMapping, TextFormatUtils.TranslatableText titlePrep, TextFormatUtils.TranslatableText subtitlePrep, List<TextFormatUtils.TranslatableText> descPreps, String title, String subtitle, String targetLang, boolean useCache, TranslationCache cache) {
        provider.translateAll(allTexts, targetLang)
                .thenAccept(translatedTexts -> {
                    try {
                        boolean anyTranslated = false; for (int i = 0; i < allTexts.size(); i++) { if (!allTexts.get(i).equals(translatedTexts.get(i))) { anyTranslated = true; break; } }
                        if (!anyTranslated && Config.TRANSLATION_PROVIDER.get() == Config.TranslationProvider.AUTO) { TranslationProvider next = getNextAutoProvider(provider); if (next != null) { FTBQuestTranslator.LOGGER.info("Provider {} returned no translations, retrying with {}", provider.getName(), next.getName()); executeTranslation(next, allTexts, textMapping, titlePrep, subtitlePrep, descPreps, title, subtitle, targetLang, useCache, cache); return; } }
                        if (useCache) { for (int i = 0; i < allTexts.size(); i++) { String original = allTexts.get(i); String translated = translatedTexts.get(i); if (translated != null && !translated.isEmpty() && !original.equals(translated) && !translated.replace("?", "").trim().isEmpty()) cache.put(original, targetLang, translated); } }
                        applyTranslations(translatedTexts, textMapping, titlePrep, subtitlePrep, descPreps, title, subtitle);
                        isTranslated = true; isTranslating = false; connectionError = false; translationTime = System.currentTimeMillis();
                        FTBQuestTranslator.LOGGER.info("Translation complete via {}!", provider.getName());
                        FTBQuestTranslator.LOGGER.info("Translated title: '{}' | subtitle: '{}' | desc lines: {} | activeQuestId: {} | isTranslated: {}", translatedTitle, translatedSubtitle, translatedDescription != null ? translatedDescription.size() : 0, Long.toHexString(activeQuestId), isTranslated);
                        if (onTranslationComplete != null) { FTBQuestTranslator.LOGGER.info("Running onTranslationComplete callback on render thread"); Minecraft.getInstance().execute(onTranslationComplete); } else { FTBQuestTranslator.LOGGER.warn("onTranslationComplete callback is NULL!"); }
                    } catch (Exception e) { FTBQuestTranslator.LOGGER.error("Translation failed: {}", e.getMessage()); isTranslating = false; clearTranslation(); }
                })
                .exceptionally(ex -> {
                    if (Config.TRANSLATION_PROVIDER.get() == Config.TranslationProvider.AUTO) { TranslationProvider next = getNextAutoProvider(provider); if (next != null) { FTBQuestTranslator.LOGGER.info("Retrying with {} after {} failed: {}", next.getName(), provider.getName(), ex.getMessage()); executeTranslation(next, allTexts, textMapping, titlePrep, subtitlePrep, descPreps, title, subtitle, targetLang, useCache, cache); return null; } }
                    FTBQuestTranslator.LOGGER.error("Translation pipeline failed: {}", ex.getMessage()); isTranslating = false; clearTranslation(); return null;
                });
    }

    private void collectTexts(TextFormatUtils.TranslatableText prep, int sourceIndex, List<String> allTexts, List<int[]> textMapping) { List<String> texts = prep.getTextsForTranslation(); if (!texts.isEmpty()) { textMapping.add(new int[]{sourceIndex, allTexts.size(), texts.size()}); allTexts.addAll(texts); } }

    private void applyTranslations(List<String> translatedTexts, List<int[]> textMapping, TextFormatUtils.TranslatableText titlePrep, TextFormatUtils.TranslatableText subtitlePrep, List<TextFormatUtils.TranslatableText> descPreps, String originalTitle, String originalSubtitle) {
        translatedTitle = originalTitle; translatedSubtitle = originalSubtitle; translatedDescription = new ArrayList<>();
        for (int[] mapping : textMapping) { int sourceIndex = mapping[0]; int start = mapping[1]; int count = mapping[2]; List<String> segmentTranslations = translatedTexts.subList(start, start + count); if (sourceIndex == 0) translatedTitle = titlePrep.reconstruct(segmentTranslations); else if (sourceIndex == 1) translatedSubtitle = subtitlePrep.reconstruct(segmentTranslations); }
        for (int d = 0; d < descPreps.size(); d++) { TextFormatUtils.TranslatableText prep = descPreps.get(d); if (prep.skip()) { translatedDescription.add(prep.original()); } else { int sourceIndex = d + 2; boolean found = false; for (int[] mapping : textMapping) { if (mapping[0] == sourceIndex) { int start = mapping[1]; int count = mapping[2]; List<String> segmentTranslations = translatedTexts.subList(start, start + count); translatedDescription.add(prep.reconstruct(segmentTranslations)); found = true; break; } } if (!found) translatedDescription.add(prep.original()); } }
        if (titlePrep.skip()) translatedTitle = titlePrep.original(); if (subtitlePrep.skip()) translatedSubtitle = subtitlePrep.original() != null ? subtitlePrep.original() : originalSubtitle;
    }

    public void clearTranslation() { activeQuestId = 0; translatedTitle = null; translatedSubtitle = null; translatedDescription = null; isTranslating = false; isTranslated = false; translationTime = 0; onTranslationComplete = null; }
    public boolean isActiveFor(long questId) { return isTranslated && activeQuestId == questId; }
    public boolean isTranslating() { return isTranslating; }
    public boolean isTranslated() { return isTranslated; }
    public boolean hasConnectionError() { return connectionError; }
    public TranslationProvider getActiveProvider() { return getProvider(); }
    public long getActiveQuestId() { return activeQuestId; }
    public String getTranslatedTitle() { return translatedTitle; }
    public String getTranslatedSubtitle() { return translatedSubtitle; }
    public List<String> getTranslatedDescription() { return translatedDescription; }
    public Component getStatusComponent() { if (isTranslating) return Component.translatable("ftbquesttransl.translating"); else if (isTranslated) return Component.translatable("ftbquesttransl.translated"); else return Component.translatable("ftbquesttransl.translate"); }
    private static class ProviderEntry { final String name; final TranslationProvider provider; ProviderEntry(String name, TranslationProvider provider) { this.name = name; this.provider = provider; } }
}
