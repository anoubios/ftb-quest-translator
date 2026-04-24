package com.fqt.fqtmod.mixin;

import com.fqt.fqtmod.translation.BatchTranslationCache;
import com.fqt.fqtmod.translation.BatchTranslationManager;
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
        QuestTranslationManager manager = QuestTranslationManager.getInstance();
        if (self instanceof Quest quest) {
            if (BatchTranslationManager.getInstance().isBatchTranslationActive()) {
                String selectedLang = BatchTranslationManager.getInstance().getSelectedLangForChapter(quest.getQuestChapter().id);
                if (selectedLang != null) {
                    BatchTranslationCache.TranslatedQuest tq = BatchTranslationCache.getInstance().getQuest(quest.getQuestChapter().id, selectedLang, quest.id);
                    if (tq != null && tq.title != null && !tq.title.isEmpty()) {
                        cir.setReturnValue(TextUtils.parseRawText(tq.title));
                        return;
                    }
                }
            }
            if (manager.isActiveFor(quest.id) && manager.getTranslatedTitle() != null) {
                String translatedTitle = manager.getTranslatedTitle();
                com.fqt.fqtmod.FTBQuestTranslator.LOGGER.info("QuestObjectBaseMixin intercepted getTitle for quest {} -> '{}'", Long.toHexString(quest.id), translatedTitle);
                if (!translatedTitle.isEmpty()) {
                    cir.setReturnValue(TextUtils.parseRawText(translatedTitle));
                }
            }
        } else if (self instanceof dev.ftb.mods.ftbquests.quest.Chapter chapter) {
            if (BatchTranslationManager.getInstance().isBatchTranslationActive()) {
                String selectedLang = BatchTranslationManager.getInstance().getSelectedLangForChapter(chapter.id);
                if (selectedLang != null) {
                    BatchTranslationCache.TranslatedQuest tq = BatchTranslationCache.getInstance().getQuest(chapter.id, selectedLang, chapter.id);
                    if (tq != null && tq.title != null && !tq.title.isEmpty()) {
                        cir.setReturnValue(TextUtils.parseRawText(tq.title));
                        return;
                    }
                }
            }
        }
    }
}
