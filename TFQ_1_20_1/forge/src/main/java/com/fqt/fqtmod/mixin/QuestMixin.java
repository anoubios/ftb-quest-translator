package com.fqt.fqtmod.mixin;

import com.fqt.fqtmod.translation.BatchTranslationCache;
import com.fqt.fqtmod.translation.BatchTranslationManager;
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
        if (BatchTranslationManager.getInstance().isBatchTranslationActive()) {
            String selectedLang = BatchTranslationManager.getInstance().getSelectedLangForChapter(self.getQuestChapter().id);
            if (selectedLang != null) {
                BatchTranslationCache.TranslatedQuest tq = BatchTranslationCache.getInstance().getQuest(self.getQuestChapter().id, selectedLang, self.id);
                if (tq != null && tq.subtitle != null && !tq.subtitle.isEmpty()) {
                    cir.setReturnValue(TextUtils.parseRawText(tq.subtitle));
                    return;
                }
            }
        }
        if (manager.isActiveFor(self.id) && manager.getTranslatedSubtitle() != null) {
            String translatedSubtitle = manager.getTranslatedSubtitle();
            if (!translatedSubtitle.isEmpty()) {
                cir.setReturnValue(TextUtils.parseRawText(translatedSubtitle));
            }
        }
    }

    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void ftbquesttransl$getDescription(CallbackInfoReturnable<List<Component>> cir) {
        Quest self = (Quest) (Object) this;
        QuestTranslationManager manager = QuestTranslationManager.getInstance();
        if (BatchTranslationManager.getInstance().isBatchTranslationActive()) {
            String selectedLang = BatchTranslationManager.getInstance().getSelectedLangForChapter(self.getQuestChapter().id);
            if (selectedLang != null) {
                BatchTranslationCache.TranslatedQuest tq = BatchTranslationCache.getInstance().getQuest(self.getQuestChapter().id, selectedLang, self.id);
                if (tq != null && tq.description != null) {
                    List<Component> translated = tq.description.stream()
                            .map(str -> (net.minecraft.network.chat.Component) TextUtils.parseRawText(str))
                            .toList();
                    cir.setReturnValue(translated);
                    return;
                }
            }
        }
        if (manager.isActiveFor(self.id) && manager.getTranslatedDescription() != null) {
            List<Component> translated = manager.getTranslatedDescription().stream()
                    .map(str -> (net.minecraft.network.chat.Component) TextUtils.parseRawText(str))
                    .toList();
            cir.setReturnValue(translated);
        }
    }
}
