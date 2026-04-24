package com.fqt.fqtmod.client.ui;

import com.fqt.fqtmod.translation.BatchTranslationManager;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.gui.quests.TabButton;
import net.minecraft.network.chat.Component;

public class TranslatorBatchToggleButton extends TabButton {
    public TranslatorBatchToggleButton(Panel panel) {
        super(panel, Component.translatable("ftbquesttransl.batch.toggle"), Icons.GLOBE);
    }

    @Override
    public void onClicked(MouseButton button) {
        playClickSound();
        if (BatchTranslationManager.getInstance().isBatchTranslationActive()) {
            BatchTranslationManager.getInstance().setBatchTranslationActive(false);
            if (dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE != null) {
                dev.ftb.mods.ftbquests.client.ClientQuestFile.INSTANCE.clearCachedData();
            }
            if (getGui() != null) {
                getGui().refreshWidgets();
            }
        } else {
            new TranslatorBatchSelectionScreen(getGui(), com.fqt.fqtmod.translation.QuestTranslationManager.getInstance().getTargetLanguage()).openGui();
        }
    }
    @Override
    public void draw(net.minecraft.client.gui.GuiGraphics graphics, dev.ftb.mods.ftblibrary.ui.Theme theme, int x, int y, int w, int h) {
        super.draw(graphics, theme, x, y, w, h);
        if (BatchTranslationManager.getInstance().isBatchTranslationActive()) {
            Icons.CHECK.draw(graphics, x + w - 8, y + h - 8, 8, 8);
        }
    }
}
