package com.fqt.fqtmod.client.ui;

import com.fqt.fqtmod.Config;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.gui.quests.TabButton;
import net.minecraft.network.chat.Component;

public class TranslatorConfigButton extends TabButton {
    public TranslatorConfigButton(Panel panel) {
        super(panel, Component.translatable("ftbquesttransl.configuration"), Icons.SETTINGS);
    }

    @Override
    public void onClicked(MouseButton button) {
        playClickSound();

        ConfigGroup group = new ConfigGroup("ftbquesttransl.settings");
        group.setNameKey("ftbquesttransl.settings");

        group.addString("targetLanguage", Config.TARGET_LANGUAGE.get(), Config.TARGET_LANGUAGE::set, "auto")
             .setNameKey("ftbquesttransl.configuration.targetLanguage");

        NameMap<Config.TranslationProvider> providerNameMap = NameMap.of(Config.TranslationProvider.AUTO, Config.TranslationProvider.values())
             .nameKey(v -> v.name())
             .create();

        group.addEnum("translationProvider", Config.TRANSLATION_PROVIDER.get(), Config.TRANSLATION_PROVIDER::set, providerNameMap, Config.TranslationProvider.AUTO)
             .setNameKey("ftbquesttransl.configuration.translationProvider");

        group.addString("deepLApiKey", com.fqt.fqtmod.SecretsConfig.getDeepLApiKey(), com.fqt.fqtmod.SecretsConfig::setDeepLApiKey, "")
             .setNameKey("ftbquesttransl.configuration.deepLApiKey");

        group.addBool("enableLargeTextWarning", Config.ENABLE_LARGE_TEXT_WARNING.get(), Config.ENABLE_LARGE_TEXT_WARNING::set, true)
             .setNameKey("ftbquesttransl.configuration.enableLargeTextWarning");
        
        group.addString("largeTextThreshold", String.valueOf(Config.LARGE_TEXT_THRESHOLD.get()), v -> {
            try {
                if (!v.isEmpty()) {
                    int val = Integer.parseInt(v.replaceAll("[^0-9]", ""));
                    Config.LARGE_TEXT_THRESHOLD.set(val);
                }
            } catch (Exception ignored) {}
        }, "2000", java.util.regex.Pattern.compile("^[0-9]*$"))
             .setNameKey("ftbquesttransl.configuration.largeTextThreshold");

        group.addBool("enableTranslatingAnimation", Config.ENABLE_TRANSLATING_ANIMATION.get(), Config.ENABLE_TRANSLATING_ANIMATION::set, true)
             .setNameKey("ftbquesttransl.configuration.enableTranslatingAnimation");

        group.addBool("enableTranslatedAnimation", Config.ENABLE_TRANSLATED_ANIMATION.get(), Config.ENABLE_TRANSLATED_ANIMATION::set, true)
             .setNameKey("ftbquesttransl.configuration.enableTranslatedAnimation");

        group.addBool("enableTtsLoadingAnimation", Config.ENABLE_TTS_LOADING_ANIMATION.get(), Config.ENABLE_TTS_LOADING_ANIMATION::set, true)
             .setNameKey("ftbquesttransl.configuration.enableTtsLoadingAnimation");

        group.addBool("enableTtsPlayingAnimation", Config.ENABLE_TTS_PLAYING_ANIMATION.get(), Config.ENABLE_TTS_PLAYING_ANIMATION::set, true)
             .setNameKey("ftbquesttransl.configuration.enableTtsPlayingAnimation");

        group.addBool("enableTranslateButton", Config.ENABLE_TRANSLATE_BUTTON.get(), Config.ENABLE_TRANSLATE_BUTTON::set, true)
             .setNameKey("ftbquesttransl.configuration.enableTranslateButton");

        group.addBool("enableTtsButton", Config.ENABLE_TTS_BUTTON.get(), Config.ENABLE_TTS_BUTTON::set, true)
             .setNameKey("ftbquesttransl.configuration.enableTtsButton");

        group.addString("translationAnimationDuration", String.valueOf(Config.TRANSLATION_ANIMATION_DURATION.get()), v -> {
            try { int val = Integer.parseInt(v.replaceAll("[^0-9-]", "")); if(val > 30000 || val < 0) throw new IllegalArgumentException(); Config.TRANSLATION_ANIMATION_DURATION.set(Math.max(500, val)); } catch (Exception ignored) {}
        }, "5000", java.util.regex.Pattern.compile("^[0-9]*$")).setNameKey("ftbquesttransl.configuration.translationAnimationDuration");

        group.addString("ttsPitch", String.valueOf(Config.TTS_PITCH.get()), v -> {
            try { double val = Double.parseDouble(v); if(val > 5.0 || val < 0.1) throw new IllegalArgumentException(); Config.TTS_PITCH.set(val); } catch (Exception ignored) {}
        }, "1.0", java.util.regex.Pattern.compile("^[0-9]*\\.?[0-9]*$")).setNameKey("ftbquesttransl.configuration.ttsPitch");

        group.addString("ttsRate", String.valueOf(Config.TTS_RATE.get()), v -> {
            try { double val = Double.parseDouble(v); if(val > 5.0 || val < 0.1) throw new IllegalArgumentException(); Config.TTS_RATE.set(val); } catch (Exception ignored) {}
        }, "1.0", java.util.regex.Pattern.compile("^[0-9]*\\.?[0-9]*$")).setNameKey("ftbquesttransl.configuration.ttsRate");

        new EditConfigScreen(group).setAutoclose(true).openGui();
    }

    @Override
    public void draw(net.minecraft.client.gui.GuiGraphics graphics, dev.ftb.mods.ftblibrary.ui.Theme theme, int x, int y, int w, int h) {
        super.draw(graphics, theme, x, y, w, h);
        dev.ftb.mods.ftblibrary.icon.Icon.getIcon("ftbquesttransl:textures/gui/translate.png").draw(graphics, x + w - 8, y + h - 8, 8, 8);
    }
}
