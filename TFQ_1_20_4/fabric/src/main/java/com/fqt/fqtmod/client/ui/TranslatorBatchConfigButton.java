package com.fqt.fqtmod.client.ui;

import com.fqt.fqtmod.translation.BatchTranslationCache;
import com.fqt.fqtmod.translation.BatchTranslationManager;
import com.fqt.fqtmod.translation.QuestTranslationManager;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.ContextMenu;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.gui.CustomToast;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.client.gui.quests.TabButton;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TranslatorBatchConfigButton extends TabButton {
    public TranslatorBatchConfigButton(Panel panel) {
        super(panel, Component.translatable("ftbquesttransl.batch.title"), Icons.SETTINGS);
    }

    private Chapter getSelectedChapter(QuestScreen qs) {
        try {
            for (Field f : qs.getClass().getDeclaredFields()) {
                if (Chapter.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (Chapter) f.get(qs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onClicked(MouseButton button) {
        playClickSound();
        BatchTranslationManager mgr = BatchTranslationManager.getInstance();
                if (mgr.isRunning()) {
            List<ContextMenuItem> items = new ArrayList<>();
            items.add(new ContextMenuItem(Component.translatable("ftbquesttransl.batch.stop"), Icons.CANCEL, b -> {
                mgr.stopBatch();
            }));
            items.add(new ContextMenuItem(Component.literal(mgr.getProgressPercentage() + "% (" + mgr.getCompletedQuests() + "/" + mgr.getTotalQuests() + ")"), Icons.INFO, b -> {}));
            getGui().openContextMenu(new ContextMenu(getGui(), items));
            return;
        }

        String targetLang = QuestTranslationManager.getInstance().getTargetLanguage();
        
        if (!mgr.isRunning() && mgr.getProgress() != null && !mgr.getProgress().pendingQuestIds.isEmpty()) {
            List<ContextMenuItem> items = new ArrayList<>();
            items.add(new ContextMenuItem(Component.translatable("ftbquesttransl.batch.continue_old", mgr.getCompletedQuests(), mgr.getTotalQuests()), Icons.RIGHT, b -> {
                mgr.resumeBatch();
            }));
            items.add(new ContextMenuItem(Component.translatable("ftbquesttransl.batch.start_new"), Icons.REFRESH, b -> {
                Minecraft.getInstance().setScreen(new ConfirmScreen(confirm -> {
                    if (confirm) {
                        mgr.cancelBatch();
                    }
                    if (getGui() instanceof QuestScreen qs) {
                        qs.getGui().openGui();
                        if (confirm) {
                            ftbquesttransl$openNewBatchMenu(mgr, targetLang);
                        }
                    } else if (confirm) {
                        ftbquesttransl$openNewBatchMenu(mgr, targetLang);
                    }
                }, Component.translatable("ftbquesttransl.batch.confirm_new_over_old"), Component.translatable("ftbquesttransl.batch.confirm_new_over_old_desc")));
            }));
            getGui().openContextMenu(new ContextMenu(getGui(), items));
            return;
        }

        ftbquesttransl$openNewBatchMenu(mgr, targetLang);
    }
    
    private void ftbquesttransl$openNewBatchMenu(BatchTranslationManager mgr, String targetLang) {
        List<ContextMenuItem> items = new ArrayList<>();
        
        items.add(new ContextMenuItem(Component.translatable("ftbquesttransl.batch.current_chapter"), Icons.NOTES, b -> {
            if (getGui() instanceof QuestScreen qs) {
                Chapter chapter = getSelectedChapter(qs);
                if (chapter != null) {
                    List<Long> questIds = new ArrayList<>();
                    chapter.getQuests().forEach(q -> questIds.add(q.id));
                    if (questIds.isEmpty()) {
                        Minecraft.getInstance().getToasts().addToast(new CustomToast(
                            Component.translatable("ftbquesttransl.batch.no_quests"),
                            dev.ftb.mods.ftblibrary.icon.ItemIcon.getItemIcon(net.minecraft.world.item.Items.BARRIER),
                            Component.empty()
                        ));
                        return;
                    }
                    
                    if (BatchTranslationCache.getInstance().hasChapter(chapter.id, targetLang)) {
                        Minecraft.getInstance().setScreen(new ConfirmScreen(confirm -> {
                            if (confirm) {
                                BatchTranslationCache.getInstance().removeChapter(BatchTranslationCache.getInstance().makeKey(chapter.id, targetLang));
                                mgr.startBatch(targetLang, questIds);
                            }
                            qs.getGui().openGui();
                        }, Component.translatable("ftbquesttransl.batch.confirm_restart"), Component.translatable("ftbquesttransl.batch.confirm_restart_desc")));
                    } else {
                        mgr.startBatch(targetLang, questIds);
                    }
                }
            }
        }));

        items.add(new ContextMenuItem(Component.translatable("ftbquesttransl.batch.all_chapters"), Icons.BOOK, b -> {
            List<Long> questIds = new ArrayList<>();
            boolean hasAnyTranslated = false;
            if (ClientQuestFile.INSTANCE != null) {
                for (dev.ftb.mods.ftbquests.quest.QuestObjectBase obj : ClientQuestFile.INSTANCE.getAllObjects()) {
                    if (obj instanceof Chapter c) {
                        if (BatchTranslationCache.getInstance().hasChapter(c.id, targetLang)) hasAnyTranslated = true;
                    }
                    if (obj instanceof Quest q) questIds.add(q.id);
                }
            }
            if (!questIds.isEmpty()) {
                if (hasAnyTranslated) {
                    Minecraft.getInstance().setScreen(new ConfirmScreen(confirm -> {
                        if (confirm) {
                            if (ClientQuestFile.INSTANCE != null) {
                                for (dev.ftb.mods.ftbquests.quest.QuestObjectBase obj : ClientQuestFile.INSTANCE.getAllObjects()) {
                                    if (obj instanceof Chapter c) {
                                        BatchTranslationCache.getInstance().removeChapter(BatchTranslationCache.getInstance().makeKey(c.id, targetLang));
                                    }
                                }
                            }
                            mgr.startBatch(targetLang, questIds);
                        }
                        if (getGui() instanceof QuestScreen qs) qs.getGui().openGui();
                    }, Component.translatable("ftbquesttransl.batch.confirm_restart_all"), Component.translatable("ftbquesttransl.batch.confirm_restart_desc")));
                } else {
                    mgr.startBatch(targetLang, questIds);
                }
            }
        }));

        items.add(new ContextMenuItem(Component.translatable("ftbquesttransl.batch.clear_cache"), Icons.BIN, b -> {
            Map<String, String> availableCaches = BatchTranslationCache.getInstance().getAvailableCaches();
            if (availableCaches.isEmpty()) {
                Minecraft.getInstance().getToasts().addToast(new CustomToast(Component.translatable("ftbquesttransl.batch.cache_empty"), Icons.INFO, Component.empty()));
                return;
            }
            
            new TranslatorBatchSelectionScreen(getGui(), targetLang, true).openGui();
        }));



        getGui().openContextMenu(new ContextMenu(getGui(), items));
    }

    @Override
    public void draw(net.minecraft.client.gui.GuiGraphics graphics, dev.ftb.mods.ftblibrary.ui.Theme theme, int x, int y, int w, int h) {
        super.draw(graphics, theme, x, y, w, h);
        Icons.GLOBE.draw(graphics, x + w - 8, y + h - 8, 8, 8);
    }
}
