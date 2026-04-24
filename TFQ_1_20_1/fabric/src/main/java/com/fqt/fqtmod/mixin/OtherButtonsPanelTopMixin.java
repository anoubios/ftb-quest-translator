package com.fqt.fqtmod.mixin;

import com.fqt.fqtmod.client.ui.TranslatorClearCacheButton;
import com.fqt.fqtmod.client.ui.TranslatorConfigButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftbquests.client.gui.quests.OtherButtonsPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.OtherButtonsPanelTop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OtherButtonsPanelTop.class, remap = false)
public abstract class OtherButtonsPanelTopMixin extends OtherButtonsPanel {

    public OtherButtonsPanelTopMixin(Panel panel) {
        super(panel);
    }

    @Inject(method = "addWidgets", at = @At("TAIL"))
    private void ftbquesttransl$addConfigButton(CallbackInfo ci) {
        this.add(new TranslatorClearCacheButton(this));
        this.add(new TranslatorConfigButton(this));
        this.add(new com.fqt.fqtmod.client.ui.TranslatorBatchConfigButton(this));
        this.add(new com.fqt.fqtmod.client.ui.TranslatorBatchToggleButton(this));
        this.add(new dev.ftb.mods.ftbquests.client.gui.quests.TabButton(this, net.minecraft.network.chat.Component.translatable("ftbquesttransl.glossary"), dev.ftb.mods.ftblibrary.icon.Icons.NOTES) {
            @Override
            public void onClicked(dev.ftb.mods.ftblibrary.ui.input.MouseButton button) {
                playClickSound();
                new com.fqt.fqtmod.client.ui.GlossaryScreen(getGui()).openGui();
            }
        });
    }
}
