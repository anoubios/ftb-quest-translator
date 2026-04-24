package com.fqt.fqtmod.client.ui;

import com.fqt.fqtmod.FabricConfig;
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

        group.addString("targetLanguage", FabricConfig.getTargetLanguage(), FabricConfig::setTargetLanguage, "auto")
             .setNameKey("ftbquesttransl.configuration.targetLanguage");

        NameMap<FabricConfig.TranslationProvider> providerNameMap = NameMap.of(FabricConfig.TranslationProvider.AUTO, FabricConfig.TranslationProvider.values())
             .nameKey(v -> v.name())
             .create();

        group.addEnum("translationProvider", FabricConfig.getTranslationProvider(), FabricConfig::setTranslationProvider, providerNameMap, FabricConfig.TranslationProvider.AUTO)
             .setNameKey("ftbquesttransl.configuration.translationProvider");

        group.addString("deepLApiKey", com.fqt.fqtmod.SecretsConfig.getDeepLApiKey(), com.fqt.fqtmod.SecretsConfig::setDeepLApiKey, "")
             .setNameKey("ftbquesttransl.configuration.deepLApiKey");

        group.addBool("enableLargeTextWarning", FabricConfig.isEnableLargeTextWarning(), FabricConfig::setEnableLargeTextWarning, true)
             .setNameKey("ftbquesttransl.configuration.enableLargeTextWarning");
        
        group.addString("largeTextThreshold", String.valueOf(FabricConfig.getLargeTextThreshold()), v -> {
            try {
                if (!v.isEmpty()) {
                    int val = Integer.parseInt(v.replaceAll("[^0-9]", ""));
                    FabricConfig.setLargeTextThreshold(val);
                }
            } catch (Exception ignored) {}
        }, "2000", java.util.regex.Pattern.compile("^[0-9]*$"))
             .setNameKey("ftbquesttransl.configuration.largeTextThreshold");

        group.addBool("enableTranslatingAnimation", FabricConfig.isEnableTranslatingAnimation(), FabricConfig::setEnableTranslatingAnimation, true)
             .setNameKey("ftbquesttransl.configuration.enableTranslatingAnimation");

        group.addBool("enableTranslatedAnimation", FabricConfig.isEnableTranslatedAnimation(), FabricConfig::setEnableTranslatedAnimation, true)
             .setNameKey("ftbquesttransl.configuration.enableTranslatedAnimation");

        group.addBool("enableTtsLoadingAnimation", FabricConfig.isEnableTtsLoadingAnimation(), FabricConfig::setEnableTtsLoadingAnimation, true)
             .setNameKey("ftbquesttransl.configuration.enableTtsLoadingAnimation");

        group.addBool("enableTtsPlayingAnimation", FabricConfig.isEnableTtsPlayingAnimation(), FabricConfig::setEnableTtsPlayingAnimation, true)
             .setNameKey("ftbquesttransl.configuration.enableTtsPlayingAnimation");

        group.addBool("enableTranslateButton", FabricConfig.isEnableTranslateButton(), FabricConfig::setEnableTranslateButton, true)
             .setNameKey("ftbquesttransl.configuration.enableTranslateButton");

        group.addBool("enableTtsButton", FabricConfig.isEnableTtsButton(), FabricConfig::setEnableTtsButton, true)
             .setNameKey("ftbquesttransl.configuration.enableTtsButton");

        group.addString("translationAnimationDuration", String.valueOf(FabricConfig.getTranslationAnimationDuration()), v -> {
            try { int val = Integer.parseInt(v.replaceAll("[^0-9-]", "")); if(val > 30000 || val < 0) throw new IllegalArgumentException(); FabricConfig.setTranslationAnimationDuration(Math.max(500, val)); } catch (Exception ignored) {}
        }, "5000", java.util.regex.Pattern.compile("^[0-9]*$")).setNameKey("ftbquesttransl.configuration.translationAnimationDuration");

        group.addString("ttsPitch", String.valueOf(FabricConfig.getTtsPitch()), v -> {
            try { double val = Double.parseDouble(v); if(val > 5.0 || val < 0.1) throw new IllegalArgumentException(); FabricConfig.setTtsPitch(val); } catch (Exception ignored) {}
        }, "1.0", java.util.regex.Pattern.compile("^[0-9]*\\.?[0-9]*$")).setNameKey("ftbquesttransl.configuration.ttsPitch");

        group.addString("ttsRate", String.valueOf(FabricConfig.getTtsRate()), v -> {
            try { double val = Double.parseDouble(v); if(val > 5.0 || val < 0.1) throw new IllegalArgumentException(); FabricConfig.setTtsRate(val); } catch (Exception ignored) {}
        }, "1.0", java.util.regex.Pattern.compile("^[0-9]*\\.?[0-9]*$")).setNameKey("ftbquesttransl.configuration.ttsRate");

        new EditConfigScreen(group).setAutoclose(true).openGui();
    }

    @Override
    public void draw(net.minecraft.client.gui.GuiGraphics graphics, dev.ftb.mods.ftblibrary.ui.Theme theme, int x, int y, int w, int h) {
        super.draw(graphics, theme, x, y, w, h);
        dev.ftb.mods.ftblibrary.icon.Icon.getIcon("ftbquesttransl:textures/gui/translate.png").draw(graphics, x + w - 8, y + h - 8, 8, 8);
    }
}
