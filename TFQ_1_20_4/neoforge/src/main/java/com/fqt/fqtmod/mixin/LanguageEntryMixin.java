package com.fqt.fqtmod.mixin;

import com.fqt.fqtmod.translation.QuestTranslationManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.LanguageSelectScreen$LanguageSelectionList$Entry")
public abstract class LanguageEntryMixin {

    @Unique private String ftbquesttransl$cachedLangCode;
    @Unique private boolean ftbquesttransl$cachedLangCodeResolved;

    @Unique
    private String ftbquesttransl$resolveLanguageCode() {
        if (ftbquesttransl$cachedLangCodeResolved) {
            return ftbquesttransl$cachedLangCode;
        }
        ftbquesttransl$cachedLangCodeResolved = true;

        Class<?> c = this.getClass();
        while (c != null && c != Object.class) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (f.getType() != String.class) continue;
                String name = f.getName();
                if (name.startsWith("ftbquesttransl$")) continue;
                try {
                    f.setAccessible(true);
                    Object v = f.get(this);
                    if (!(v instanceof String s)) continue;
                    String lower = s.toLowerCase(java.util.Locale.ROOT);
                    if (!s.equals(lower)) continue;
                    if (!lower.matches("^[a-z]{2,4}(_[a-z]{2,4})?$")) continue;
                    ftbquesttransl$cachedLangCode = lower;
                    return ftbquesttransl$cachedLangCode;
                } catch (Throwable ignored) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ftbquesttransl$renderSupportedIcon(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick, CallbackInfo ci) {
        String langCode = ftbquesttransl$resolveLanguageCode();

        // Blacklist Minecraft's novelty/joke languages that reuse real ISO codes
        java.util.Set<String> JOKE_LANGUAGES = java.util.Set.of(
            "en_pt", "en_ud", "en_ws", "en_an", "enp",  // Pirate, Upside-down, Shakespearean, Anglish
            "lol_us", "tlh_aa", "qya_aa", "jbo_en"       // LOLCAT, Klingon, Quenya, Lojban
        );
        if (langCode != null && JOKE_LANGUAGES.contains(langCode.toLowerCase())) {
            // Don't show [T] for joke languages
        } else if (langCode != null && QuestTranslationManager.getInstance().getActiveProvider().supportsLanguage(com.fqt.fqtmod.translation.LanguageMapper.getApiLanguageCode(langCode))) {
            Component icon = Component.literal("[T]").withStyle(ChatFormatting.GREEN);
            // Draw at the right side of the entry
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, icon, left + width - 20, top + 1, 0xFFFFFF, false);
            
            if (isMouseOver) {
                if (mouseX >= left + width - 25 && mouseX <= left + width) {
                    Component tooltip;
                    if (com.fqt.fqtmod.translation.LanguageMapper.isRegionalVariant(langCode)) {
                        String baseCode = com.fqt.fqtmod.translation.LanguageMapper.getApiLanguageCode(langCode);
                        tooltip = Component.translatable("ftbquesttransl.supported_language_regional", baseCode);
                    } else {
                        String baseCode = com.fqt.fqtmod.translation.LanguageMapper.getApiLanguageCode(langCode);
                        tooltip = Component.translatable("ftbquesttransl.supported_language", baseCode);
                    }
                    graphics.renderTooltip(net.minecraft.client.Minecraft.getInstance().font, tooltip, mouseX, mouseY);
                }
            }
        }
    }
}
