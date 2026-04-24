package com.fqt.fqtmod.mixin;

import com.fqt.fqtmod.Config;
import com.fqt.fqtmod.translation.QuestTranslationManager;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ViewQuestPanel.class, remap = false)
public abstract class ViewQuestPanelMixin extends ModalPanel {

    @Shadow
    private Quest quest;

    @Shadow
    private Icon icon;

    protected ViewQuestPanelMixin(Panel panel) {
        super(panel);
    }

    @Shadow
    public abstract Quest getViewedQuest();

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void ftbquesttransl$clearIcon(CallbackInfo ci) {
        if (quest == null) return;
        if (Config.ENABLE_TRANSLATE_BUTTON.get() || Config.ENABLE_TTS_BUTTON.get()) {
            ViewQuestPanel self = (ViewQuestPanel) (Object) this;
            this.icon = Color4I.empty();
        }
    }

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void ftbquesttransl$addTTSButton(CallbackInfo ci) {
        if (!Config.ENABLE_TTS_BUTTON.get()) return;
        if (quest == null) return;
        ViewQuestPanel self = (ViewQuestPanel) (Object) this;
        // 3-frame speaker animation textures
        dev.ftb.mods.ftblibrary.icon.Icon ttsFrame1 = Icon.getIcon("ftbquesttransl:textures/gui/tts_1.png");
        dev.ftb.mods.ftblibrary.icon.Icon ttsFrame2 = Icon.getIcon("ftbquesttransl:textures/gui/tts_2.png");
        dev.ftb.mods.ftblibrary.icon.Icon ttsFrame3 = Icon.getIcon("ftbquesttransl:textures/gui/tts_3.png");
        dev.ftb.mods.ftblibrary.icon.Icon[] ttsFrames = {ttsFrame1, ttsFrame2, ttsFrame3};
        
        dev.ftb.mods.ftblibrary.ui.Button ttsButton = new dev.ftb.mods.ftblibrary.ui.SimpleButton(self, 
                Component.translatable("ftbquesttransl.tts.read"),
                ttsFrame3,
                (widget, button) -> {
                    com.fqt.fqtmod.translation.QuestTranslationManager mgr = com.fqt.fqtmod.translation.QuestTranslationManager.getInstance();
                    com.fqt.fqtmod.translation.BatchTranslationManager batchMgr = com.fqt.fqtmod.translation.BatchTranslationManager.getInstance();
                    boolean isBatch = batchMgr.isBatchTranslationActive();
                    boolean isSingle = mgr.isActiveFor(quest.id);
                    String langCode = isSingle ? mgr.getTargetLanguage() : (isBatch ? batchMgr.getSelectedLangForChapter(quest.getQuestChapter().id) : "en");
                    
                    String titleT = null; String subtitleT = null; java.util.List<String> descT = null;
                    if (isSingle) { titleT = mgr.getTranslatedTitle(); subtitleT = mgr.getTranslatedSubtitle(); descT = mgr.getTranslatedDescription(); }
                    else if (isBatch && langCode != null) {
                        com.fqt.fqtmod.translation.BatchTranslationCache.TranslatedQuest tq = com.fqt.fqtmod.translation.BatchTranslationCache.getInstance().getQuest(quest.getQuestChapter().id, langCode, quest.id);
                        if (tq != null) { titleT = tq.title; subtitleT = tq.subtitle; descT = tq.description; }
                    }
                    
                    if (titleT == null || titleT.isEmpty()) {
                        titleT = quest.getRawTitle();
                        if (titleT.isEmpty()) titleT = quest.getTitle().getString();
                        titleT = ftbquesttransl$resolveText(titleT);
                    }
                    if (subtitleT == null || subtitleT.isEmpty()) {
                        subtitleT = quest.getRawSubtitle();
                        if (subtitleT.isEmpty() && quest.getSubtitle() != null) subtitleT = quest.getSubtitle().getString();
                        subtitleT = ftbquesttransl$resolveText(subtitleT);
                    }
                    if (descT == null || descT.isEmpty()) {
                        descT = new java.util.ArrayList<>();
                        for (String line : quest.getRawDescription()) descT.add(ftbquesttransl$resolveText(line));
                    }
                    
                    String textToRead = titleT != null ? com.fqt.fqtmod.translation.TextFormatUtils.stripAllFormatting(titleT).trim() : "";
                    if (subtitleT != null && !subtitleT.trim().isEmpty()) {
                        String subStr = com.fqt.fqtmod.translation.TextFormatUtils.stripAllFormatting(subtitleT).trim();
                        if (!subStr.isEmpty()) textToRead += (textToRead.isEmpty() ? "" : (textToRead.matches(".*[.!?]$") ? " " : ". ")) + subStr;
                    }
                    if (descT != null && !descT.isEmpty()) {
                        String dStr = com.fqt.fqtmod.translation.TextFormatUtils.stripAllFormatting(String.join(" ", descT)).trim();
                        if (!dStr.isEmpty()) textToRead += (textToRead.isEmpty() ? "" : (textToRead.matches(".*[.!?]$") ? " " : ". ")) + dStr;
                    }

                    if (com.fqt.fqtmod.translation.TTSManager.getInstance().isPlaying() || com.fqt.fqtmod.translation.TTSManager.getInstance().isLoading()) {
                        com.fqt.fqtmod.translation.TTSManager.getInstance().stop();
                        return;
                    }
                    
                    com.fqt.fqtmod.translation.TTSManager.getInstance().play(textToRead, langCode);
                }) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                com.fqt.fqtmod.translation.QuestTranslationManager mgr = com.fqt.fqtmod.translation.QuestTranslationManager.getInstance();
                boolean isBatch = com.fqt.fqtmod.translation.BatchTranslationManager.getInstance().isBatchTranslationActive();
                String currentLang = mgr.isActiveFor(quest.id) ? mgr.getTargetLanguage() : (isBatch ? com.fqt.fqtmod.translation.BatchTranslationManager.getInstance().getSelectedLangForChapter(quest.getQuestChapter().id) : "en");
                
                if (!com.fqt.fqtmod.translation.TTSManager.getInstance().supportsLanguage(currentLang) || com.fqt.fqtmod.translation.TTSManager.getInstance().hasError()) {
                    if (Config.ENABLE_TTS_PLAYING_ANIMATION.get()) {
                        float time = (System.currentTimeMillis() % 1500) / 1500f;
                        for (int ring = 0; ring < 3; ring++) {
                            float progress = (time + ring / 3f) % 1f;
                            int radius = 4 + (int) (progress * 8);
                            int alpha = (int) ((1f - progress) * 120);
                            ftbquesttransl$drawRing(graphics, x + w/2, y + h/2, radius, 2, 0xCC0000, alpha);
                        }
                    }
                    ttsFrame3.draw(graphics, x + 1, y + 1, w - 2, h - 2);
                } else if (com.fqt.fqtmod.translation.TTSManager.getInstance().isLoading()) {
                    if (Config.ENABLE_TTS_LOADING_ANIMATION.get()) {
                        float time = (System.currentTimeMillis() % 1500) / 1500f;
                        for (int ring = 0; ring < 3; ring++) {
                            float progress = (time + ring / 3f) % 1f;
                            int radius = 4 + (int) (progress * 8);
                            int alpha = (int) ((1f - progress) * 120);
                            ftbquesttransl$drawRing(graphics, x + w/2, y + h/2, radius, 2, 0xFFCC00, alpha);
                        }
                    }
                    ttsFrame3.draw(graphics, x + 1, y + 1, w - 2, h - 2);
                } else if (com.fqt.fqtmod.translation.TTSManager.getInstance().isPlaying()) {
                    if (Config.ENABLE_TTS_PLAYING_ANIMATION.get()) {
                        int frameIndex = (int) ((System.currentTimeMillis() / 400) % 3);
                        ttsFrames[frameIndex].draw(graphics, x + 1, y + 1, w - 2, h - 2);
                    } else {
                        ttsFrame3.draw(graphics, x + 1, y + 1, w - 2, h - 2);
                    }
                } else {
                    ttsFrame3.draw(graphics, x + 1, y + 1, w - 2, h - 2);
                }
            }
            
            @Override
            public void addMouseOverText(TooltipList list) {
                com.fqt.fqtmod.translation.QuestTranslationManager mgr = com.fqt.fqtmod.translation.QuestTranslationManager.getInstance();
                boolean isBatch = com.fqt.fqtmod.translation.BatchTranslationManager.getInstance().isBatchTranslationActive();
                String currentLang = mgr.isActiveFor(quest.id) ? mgr.getTargetLanguage() : (isBatch ? com.fqt.fqtmod.translation.BatchTranslationManager.getInstance().getSelectedLangForChapter(quest.getQuestChapter().id) : "en");
                
                if (!com.fqt.fqtmod.translation.TTSManager.getInstance().supportsLanguage(currentLang)) {
                    list.add(Component.translatable("ftbquesttransl.tts.unsupported").withStyle(ChatFormatting.RED));
                } else if (com.fqt.fqtmod.translation.TTSManager.getInstance().hasError()) {
                    String err = com.fqt.fqtmod.translation.TTSManager.getInstance().getLastError();
                    list.add(Component.literal(err != null ? err : "TTS Error").withStyle(ChatFormatting.RED));
                } else {
                    list.add(Component.translatable("ftbquesttransl.tts.read").withStyle(ChatFormatting.WHITE));
                }
            }
        };
        
        int iconSize = Math.min(16,
                self.getWidgets().stream().filter(w -> w.getClass().getSimpleName().equals("QuestDescriptionField"))
                        .mapToInt(w -> w.height).findFirst().orElse(12) + 2);
                        
        int ttsX = Config.ENABLE_TRANSLATE_BUTTON.get() ? 4 + iconSize + 4 : 4;
        ttsButton.setPosAndSize(ttsX, 4, iconSize, iconSize);
        self.add(ttsButton);
    }
    
    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void ftbquesttransl$addTranslateButton(CallbackInfo ci) {
        if (!Config.ENABLE_TRANSLATE_BUTTON.get()) return;
        if (quest == null)
            return;

        ViewQuestPanel self = (ViewQuestPanel) (Object) this;

        QuestTranslationManager.getInstance().checkConnectionInBackground();
        Icon translateIcon = Icon.getIcon("ftbquesttransl:textures/gui/translate.png");

        Button translateButton = new SimpleButton(self,
                QuestTranslationManager.getInstance().getStatusComponent(),
                translateIcon,
                (widget, button) -> ftbquesttransl$onTranslateClicked()) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                QuestTranslationManager mgr = QuestTranslationManager.getInstance();
                int cx = x + w / 2;
                int cy = y + h / 2;

                boolean isSupported = mgr.getActiveProvider().supportsLanguage(mgr.getTargetLanguage());

                boolean isMissingDeepLApi = Config.TRANSLATION_PROVIDER.get() == Config.TranslationProvider.DEEPL
                        && (com.fqt.fqtmod.SecretsConfig.getDeepLApiKey() == null
                                || com.fqt.fqtmod.SecretsConfig.getDeepLApiKey().trim().isEmpty());

                if (isMissingDeepLApi || mgr.hasConnectionError() || !isSupported) {
                    float time = (System.currentTimeMillis() % 1500) / 1500f;
                    for (int ring = 0; ring < 3; ring++) {
                        float progress = (time + ring / 3f) % 1f;
                        int radius = 4 + (int) (progress * 8);
                        int alpha = (int) ((1f - progress) * 120);
                        ftbquesttransl$drawRing(graphics, cx, cy, radius, 2, 0xCC0000, alpha);
                    }
                } else if (mgr.isTranslating()) {
                    if (Config.ENABLE_TRANSLATING_ANIMATION.get()) {
                        float time = (System.currentTimeMillis() % 1500) / 1500f;
                        for (int ring = 0; ring < 3; ring++) {
                            float progress = (time + ring / 3f) % 1f;
                            int radius = 4 + (int) (progress * 8);
                            int alpha = (int) ((1f - progress) * 120);
                            ftbquesttransl$drawRing(graphics, cx, cy, radius, 2, 0xFFCC00, alpha);
                        }
                    }
                } else if (mgr.isActiveFor(quest.id) && (System.currentTimeMillis() - mgr.getTranslationTime() < Config.TRANSLATION_ANIMATION_DURATION.get())) {
                    if (Config.ENABLE_TRANSLATED_ANIMATION.get()) {
                        float time = (System.currentTimeMillis() % 1500) / 1500f;
                        for (int ring = 0; ring < 3; ring++) {
                            float progress = (time + ring / 3f) % 1f;
                            int radius = 4 + (int) (progress * 8);
                            int alpha = (int) ((1f - progress) * 120);
                            ftbquesttransl$drawRing(graphics, cx, cy, radius, 2, 0x00B850, alpha);
                        }
                    }
                }

                translateIcon.draw(graphics, x + 1, y + 1, w - 2, h - 2);
            }

            @Override
            public void addMouseOverText(TooltipList list) {
                QuestTranslationManager mgr = QuestTranslationManager.getInstance();
                boolean isSupported = mgr.getActiveProvider().supportsLanguage(mgr.getTargetLanguage());

                boolean isMissingDeepLApi = Config.TRANSLATION_PROVIDER.get() == Config.TranslationProvider.DEEPL
                        && (com.fqt.fqtmod.SecretsConfig.getDeepLApiKey() == null
                                || com.fqt.fqtmod.SecretsConfig.getDeepLApiKey().trim().isEmpty());

                if (isMissingDeepLApi) {
                    list.add(Component.translatable("ftbquesttransl.error.missing_api").withStyle(ChatFormatting.RED));
                    list.add(Component.translatable("ftbquesttransl.error.missing_api_desc")
                            .withStyle(ChatFormatting.GRAY));
                } else if (mgr.hasConnectionError()) {
                    list.add(Component.translatable("ftbquesttransl.error.connection").withStyle(ChatFormatting.RED));
                    list.add(Component.translatable("ftbquesttransl.error.connection_desc")
                            .withStyle(ChatFormatting.GRAY));
                } else if (!isSupported) {
                    list.add(Component.translatable("ftbquesttransl.error.unsupported").withStyle(ChatFormatting.RED));
                    list.add(Component.translatable("ftbquesttransl.error.unsupported_desc")
                            .withStyle(ChatFormatting.GRAY));
                } else if (mgr.isTranslating()) {
                    list.add(Component.translatable("ftbquesttransl.translating").withStyle(ChatFormatting.YELLOW));
                } else if (mgr.isActiveFor(quest.id)) {
                    list.add(Component.translatable("ftbquesttransl.translated").withStyle(ChatFormatting.GREEN));
                    list.add(Component.translatable("ftbquesttransl.click_to_restore").withStyle(ChatFormatting.GRAY));
                } else {
                    list.add(Component.translatable("ftbquesttransl.translate").withStyle(ChatFormatting.WHITE));
                    list.add(Component.literal("(" + mgr.getTargetLanguage() + ")").withStyle(ChatFormatting.GRAY));
                }
            }
        };

        int iconSize = Math.min(16,
                self.getWidgets().stream().filter(w -> w.getClass().getSimpleName().equals("QuestDescriptionField"))
                        .mapToInt(w -> w.height).findFirst().orElse(12) + 2);
        translateButton.setPosAndSize(4, 4, iconSize, iconSize);
        self.add(translateButton);
    }



    @Inject(method = "onClosed", at = @At("HEAD"))
    private void ftbquesttransl$onClosed(CallbackInfo ci) {
        QuestTranslationManager.getInstance().clearTranslation();
        com.fqt.fqtmod.translation.TTSManager.getInstance().stop();
    }

    private void ftbquesttransl$onTranslateClicked() {
        if (quest == null)
            return;
        QuestTranslationManager manager = QuestTranslationManager.getInstance();
        if (Config.TRANSLATION_PROVIDER.get() == Config.TranslationProvider.DEEPL
                && (com.fqt.fqtmod.SecretsConfig.getDeepLApiKey() == null
                        || com.fqt.fqtmod.SecretsConfig.getDeepLApiKey().trim().isEmpty())) {
            return;
        }
        if (!manager.getActiveProvider().supportsLanguage(manager.getTargetLanguage())) {
            return;
        }

        ViewQuestPanel self = (ViewQuestPanel) (Object) this;
        String rawT = quest.getRawTitle();
        if (rawT.isEmpty()) rawT = quest.getTitle().getString();
        String title = ftbquesttransl$resolveText(rawT);
        
        String rawS = quest.getRawSubtitle();
        if (rawS.isEmpty() && quest.getSubtitle() != null) rawS = quest.getSubtitle().getString();
        String subtitle = ftbquesttransl$resolveText(rawS);
        
        List<String> rawDescription = quest.getRawDescription();
        List<String> description = new java.util.ArrayList<>();
        for (String line : rawDescription)
            description.add(ftbquesttransl$resolveText(line));

        long totalChars = 0;
        if (title != null) totalChars += title.length();
        if (subtitle != null) totalChars += subtitle.length();
        for (String s : description) if (s != null) totalChars += s.length();

        if (!manager.isActiveFor(quest.id) && Config.ENABLE_LARGE_TEXT_WARNING.get()
                && totalChars >= Config.LARGE_TEXT_THRESHOLD.get()) {
            List<ContextMenuItem> items = new java.util.ArrayList<>();
            items.add(ContextMenuItem.title(Component.translatable("ftbquesttransl.warning.large_quest", totalChars))
                    .setCloseMenu(false));
            items.add(new ContextMenuItem(Component.translatable("gui.yes"), Icons.ACCEPT, b -> {
                manager.toggleTranslation(quest.id, title, subtitle, description, () -> {
                    quest.clearCachedData();
                    self.refreshWidgets();
                });
                b.getGui().closeContextMenu();
                quest.clearCachedData();
                self.refreshWidgets();
            }));
            items.add(new ContextMenuItem(Component.translatable("gui.no"), Icons.CANCEL, b -> {
                b.getGui().closeContextMenu();
            }));

            self.getGui().openContextMenu(new ContextMenu(self.getGui(), items));
            return;
        }

        manager.toggleTranslation(quest.id, title, subtitle, description, () -> {
            quest.clearCachedData();
            self.refreshWidgets();
        });

        quest.clearCachedData();
        self.refreshWidgets();
    }

    @Unique
    private static String ftbquesttransl$resolveText(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.length() > 2) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            if (inner.startsWith("@") || inner.contains(":")) return raw;
            if (inner.startsWith("\"") || inner.startsWith("[")) return raw;
            String resolved = net.minecraft.client.resources.language.I18n.get(inner);
            if (!resolved.equals(inner)) {
                return resolved;
            }
        }
        return raw;
    }

    @Unique
    private static void ftbquesttransl$drawRing(GuiGraphics graphics, int cx, int cy, int radius, int thickness,
            int color, int alpha) {
        if (alpha <= 0 || radius <= 0) return;

        int argb = (Math.min(alpha, 255) << 24) | (color & 0xFFFFFF);
        int outer = radius + thickness / 2;
        int inner = Math.max(0, radius - (thickness + 1) / 2);

        for (int dy = -outer; dy <= outer; dy++) {
            if (dy * dy > outer * outer) continue;
            int outerDx = (int) Math.sqrt((double) outer * outer - (double) dy * dy);

            if (inner > 0 && Math.abs(dy) < inner) {
                int innerDx = (int) Math.sqrt((double) inner * inner - (double) dy * dy);
                graphics.fill(cx - outerDx, cy + dy, cx - innerDx, cy + dy + 1, argb);
                graphics.fill(cx + innerDx + 1, cy + dy, cx + outerDx + 1, cy + dy + 1, argb);
            } else {
                graphics.fill(cx - outerDx, cy + dy, cx + outerDx + 1, cy + dy + 1, argb);
            }
        }
    }
}
