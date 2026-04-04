package com.fqt.fqtmod.mixin;

import com.fqt.fqtmod.translation.QuestTranslationManager;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.util.TextUtils;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = Quest.class, remap = false)
public abstract class QuestMixin {

    @Inject(method = "getSubtitle", at = @At("HEAD"), cancellable = true)
    private void ftbquesttransl$getSubtitle(CallbackInfoReturnable<Component> cir) {
        Quest self = (Quest) (Object) this;
        QuestTranslationManager manager = QuestTranslationManager.getInstance();
        if (manager.isActiveFor(self.id) && manager.getTranslatedSubtitle() != null) {
            String translatedSubtitle = manager.getTranslatedSubtitle();
            if (!translatedSubtitle.isEmpty()) {
                cir.setReturnValue(TextUtils.parseRawText(translatedSubtitle, self.holderLookup()));
            }
        }
    }

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void ftbquesttransl$getDescription(CallbackInfoReturnable<List<Component>> cir) {
        Quest self = (Quest) (Object) this;
        QuestTranslationManager manager = QuestTranslationManager.getInstance();
        if (manager.isActiveFor(self.id) && manager.getTranslatedDescription() != null) {
            List<Component> translated = manager.getTranslatedDescription().stream()
                    .map(str -> TextUtils.parseRawText(str, self.holderLookup()))
                    .toList();
            cir.setReturnValue(translated);
        }
    }
}
