package com.fqt.fqtmod.client.ui;

import com.fqt.fqtmod.translation.TranslationCache;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.gui.quests.TabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TranslatorClearCacheButton extends TabButton {
    public TranslatorClearCacheButton(Panel panel) {
        super(panel, Component.translatable("ftbquesttransl.clear_cache"), Icons.BIN);
    }

    @Override
    public void onClicked(MouseButton button) {
        playClickSound();
        TranslationCache.getInstance().clear();
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("ftbquesttransl.cache_cleared"), true);
        }
    }
}
