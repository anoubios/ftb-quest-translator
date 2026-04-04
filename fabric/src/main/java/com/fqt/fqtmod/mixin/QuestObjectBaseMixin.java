package com.fqt.fqtmod.mixin;

import com.fqt.fqtmod.translation.QuestTranslationManager;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.util.TextUtils;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = QuestObjectBase.class, remap = false)
public abstract class QuestObjectBaseMixin {

    @Inject(method = "getTitle", at = @At("HEAD"), cancellable = true)
    private void ftbquesttransl$getTitle(CallbackInfoReturnable<Component> cir) {
        QuestObjectBase self = (QuestObjectBase) (Object) this;
        if (self instanceof Quest quest) {
            QuestTranslationManager manager = QuestTranslationManager.getInstance();
            if (manager.isActiveFor(quest.id) && manager.getTranslatedTitle() != null) {
                String translatedTitle = manager.getTranslatedTitle();
                if (!translatedTitle.isEmpty()) {
                    cir.setReturnValue(TextUtils.parseRawText(translatedTitle, quest.holderLookup()));
                }
            }
        }
    }
}
