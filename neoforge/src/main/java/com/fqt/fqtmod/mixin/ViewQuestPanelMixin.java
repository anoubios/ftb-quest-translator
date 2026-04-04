package com.fqt.fqtmod.mixin;

import com.fqt.fqtmod.translation.QuestTranslationManager;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel;
import dev.ftb.mods.ftbquests.quest.Quest;
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

    protected ViewQuestPanelMixin(Panel panel) {
        super(panel);
    }

    @Shadow
    public abstract Quest getViewedQuest();

    /**
     * Inject at the end of addWidgets() to add our translate button.
     */
    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void ftbquesttransl$addTranslateButton(CallbackInfo ci) {
        if (quest == null) return;

        ViewQuestPanel self = (ViewQuestPanel) (Object) this;

        // Use custom icon from our mod resources
        Icon translateIcon = Icon.getIcon("ftbquesttransl:textures/gui/translate.png");

        // Create translate button
        Button translateButton = new SimpleButton(self,
                QuestTranslationManager.getInstance().getStatusComponent(),
                translateIcon,
                (widget, button) -> ftbquesttransl$onTranslateClicked()
        ) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                QuestTranslationManager mgr = QuestTranslationManager.getInstance();

                if (mgr.isTranslating()) {
                    // Pulsing yellow background while translating
                    float pulse = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.5f + 0.5f;
                    int alpha = (int) (pulse * 160 + 60);
                    Color4I.rgb(0xFFCC00).withAlpha(alpha).draw(graphics, x, y, w, h);
                } else if (mgr.isActiveFor(quest.id)) {
                    // Green background when translated
                    Color4I.rgb(0x00B850).withAlpha(100).draw(graphics, x, y, w, h);
                }

                // Draw the custom icon
                translateIcon.draw(graphics, x + 1, y + 1, w - 2, h - 2);
            }

            @Override
            public void addMouseOverText(TooltipList list) {
                QuestTranslationManager mgr = QuestTranslationManager.getInstance();
                if (mgr.isTranslating()) {
                    list.add(Component.translatable("ftbquesttransl.translating")
                            .withStyle(ChatFormatting.YELLOW));
                } else if (mgr.isActiveFor(quest.id)) {
                    list.add(Component.translatable("ftbquesttransl.translated")
                            .withStyle(ChatFormatting.GREEN));
                    list.add(Component.translatable("ftbquesttransl.click_to_restore")
                            .withStyle(ChatFormatting.GRAY));
                } else {
                    list.add(Component.translatable("ftbquesttransl.translate")
                            .withStyle(ChatFormatting.WHITE));
                    list.add(Component.literal("(" + mgr.getTargetLanguage() + ")")
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        };

        // Position: smaller button, vertically centered in title bar
        int iconSize = 12;
        translateButton.setPosAndSize(18, 2, iconSize, iconSize);
        self.add(translateButton);
    }

    /**
     * Clear translation state when panel closes.
     */
    @Inject(method = "onClosed", at = @At("HEAD"))
    private void ftbquesttransl$onClosed(CallbackInfo ci) {
        QuestTranslationManager.getInstance().clearTranslation();
    }

    @Unique
    private void ftbquesttransl$onTranslateClicked() {
        if (quest == null) return;

        ViewQuestPanel self = (ViewQuestPanel) (Object) this;
        QuestTranslationManager manager = QuestTranslationManager.getInstance();

        // Get raw text from quest
        String title = quest.getRawTitle();
        String subtitle = quest.getRawSubtitle();
        List<String> description = quest.getRawDescription();

        // Toggle translation
        manager.toggleTranslation(quest.id, title, subtitle, description, () -> {
            quest.clearCachedData();
            self.refreshWidgets();
        });

        // Refresh immediately to show "translating..." state
        quest.clearCachedData();
        self.refreshWidgets();
    }
}
