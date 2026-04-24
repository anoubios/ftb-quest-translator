package com.fqt.fqtmod.client.ui;

import com.fqt.fqtmod.translation.BatchTranslationCache;
import com.fqt.fqtmod.translation.BatchTranslationManager;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.PanelScrollBar;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class TranslatorBatchSelectionScreen extends BaseScreen {
    private final BaseScreen previousScreen;
    private final Set<String> selectedCaches;
    private final String targetLang;
    private static String lastFilterLang = null;
    private String currentFilterLang = null;
    
    private CustomScrollPanel scrollPanel;
    private PanelScrollBar scrollBar;
    private SimpleButton selectAllButton;
    private SimpleButton langFilterButton;
    private SimpleButton applyButton;
    private SimpleButton cancelButton;

    private final boolean isDeleteMode;

    public TranslatorBatchSelectionScreen(BaseScreen previousScreen, String targetLang) {
        this(previousScreen, targetLang, false);
    }
    
    public TranslatorBatchSelectionScreen(BaseScreen previousScreen, String targetLang, boolean isDeleteMode) {
        this.previousScreen = previousScreen;
        this.targetLang = targetLang;
        this.isDeleteMode = isDeleteMode;
        this.selectedCaches = new HashSet<>(isDeleteMode ? new java.util.ArrayList<>() : BatchTranslationManager.getInstance().getSelectedCachesForView());
    }

    @Override
    public boolean onInit() {
        if (currentFilterLang == null) {
            currentFilterLang = lastFilterLang != null ? lastFilterLang : "ALL";
        }
        return setFullscreen();
    }

    @Override
    public void addWidgets() {
        cancelButton = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            closeGui(false);
            if (previousScreen != null) {
                previousScreen.refreshWidgets();
                previousScreen.openGui();
            }
        }) {
            @Override
            public void draw(net.minecraft.client.gui.GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                Icons.LEFT.draw(graphics, x + (w - 16) / 2, y + (h - 16) / 2, 16, 16);
            }
            @Override
            public void addMouseOverText(dev.ftb.mods.ftblibrary.util.TooltipList list) { }
        };
        add(cancelButton);

        scrollPanel = new CustomScrollPanel(this);
        add(scrollPanel);
        
        scrollBar = new PanelScrollBar(this, scrollPanel);
        scrollBar.setCanAlwaysScroll(true);
        add(scrollBar);
        
        selectAllButton = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            boolean onlyCurrentFiltered = currentFilterLang != null && !"ALL".equals(currentFilterLang) && !"".equals(currentFilterLang);
            Map<Long, List<String>> cachesByChapter = new java.util.HashMap<>();
            for (String key : com.fqt.fqtmod.translation.BatchTranslationCache.getInstance().getAvailableCaches().keySet()) {
                if (onlyCurrentFiltered && !key.endsWith(":" + currentFilterLang)) continue;
                long chapterId = Long.parseLong(key.split(":")[0]);
                cachesByChapter.computeIfAbsent(chapterId, k -> new java.util.ArrayList<>()).add(key);
            }
            List<String> bestKeys = new java.util.ArrayList<>();
            if (isDeleteMode) {
                for (List<String> keys : cachesByChapter.values()) {
                    bestKeys.addAll(keys);
                }
            } else {
                for (List<String> keys : cachesByChapter.values()) {
                    String bestKey = keys.stream().filter(k -> k.endsWith(":" + targetLang)).findFirst().orElse(keys.get(0));
                    bestKeys.add(bestKey);
                }
            }
            if (onlyCurrentFiltered) {
                boolean allBestSelected = selectedCaches.containsAll(bestKeys);
                if (allBestSelected) {
                    selectedCaches.removeAll(bestKeys);
                } else {
                    if (!isDeleteMode) {
                        selectedCaches.removeIf(k -> {
                            String chapterPrefix = k.split(":")[0] + ":";
                            return bestKeys.stream().anyMatch(bk -> bk.startsWith(chapterPrefix));
                        });
                    }
                    selectedCaches.addAll(bestKeys);
                }
            } else {
                boolean allBestSelected = selectedCaches.containsAll(bestKeys);
                if (!isDeleteMode) selectedCaches.clear();
                if (!allBestSelected) {
                    selectedCaches.addAll(bestKeys);
                } else if (isDeleteMode) {
                    selectedCaches.removeAll(bestKeys);
                }
            }
            scrollPanel.refreshWidgets();
        }) {
            @Override
            public void draw(net.minecraft.client.gui.GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                theme.drawString(graphics, Component.translatable("ftbquesttransl.batch.select_all"), x + w / 2, y + h / 2 - 4, theme.getContentColor(WidgetType.mouseOver(isMouseOver())), Theme.CENTERED);
            }
            @Override
            public void addMouseOverText(dev.ftb.mods.ftblibrary.util.TooltipList list) { }
        };
        add(selectAllButton);


        langFilterButton = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            Set<String> uniqueLangs = new HashSet<>();
            for (String key : com.fqt.fqtmod.translation.BatchTranslationCache.getInstance().getAvailableCaches().keySet()) {
                String[] parts = key.split(":");
                if (parts.length > 1) uniqueLangs.add(parts[1]);
            }
            List<dev.ftb.mods.ftblibrary.ui.ContextMenuItem> items = new ArrayList<>();
            items.add(new dev.ftb.mods.ftblibrary.ui.ContextMenuItem(Component.translatable("ftbquesttransl.batch.filter_all"), Icons.GLOBE, b2 -> {
                currentFilterLang = "ALL";
                lastFilterLang = "ALL";
                scrollPanel.refreshWidgets();
            }));
            for (String l : uniqueLangs) {
                items.add(new dev.ftb.mods.ftblibrary.ui.ContextMenuItem(Component.literal(l), Icons.GLOBE, b2 -> {
                    currentFilterLang = l;
                    lastFilterLang = l;
                    scrollPanel.refreshWidgets();
                }));
            }
            getGui().openContextMenu(new dev.ftb.mods.ftblibrary.ui.ContextMenu(getGui(), items));
        }) {
            @Override
            public void draw(net.minecraft.client.gui.GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                theme.drawString(graphics, currentFilterLang, x + w / 2, y + h / 2 - 4, theme.getContentColor(WidgetType.mouseOver(isMouseOver())), Theme.CENTERED);
            }
            @Override
            public void addMouseOverText(dev.ftb.mods.ftblibrary.util.TooltipList list) { }
        };
        add(langFilterButton);
        
        applyButton = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            if (isDeleteMode) {
                for (String cacheKey : selectedCaches) {
                    com.fqt.fqtmod.translation.BatchTranslationCache.getInstance().removeChapter(cacheKey);
                }
                Set<String> viewCaches = new HashSet<>(BatchTranslationManager.getInstance().getSelectedCachesForView());
                viewCaches.removeAll(selectedCaches);
                BatchTranslationManager.getInstance().setSelectedCachesForView(viewCaches);
                
                if (net.minecraft.client.Minecraft.getInstance().player != null) {
                    net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(Component.translatable("ftbquesttransl.cache_cleared"), true);
                }
            } else {
                BatchTranslationManager.getInstance().setSelectedCachesForView(selectedCaches);
                BatchTranslationManager.getInstance().setBatchTranslationActive(true);
            }
            if (ClientQuestFile.INSTANCE != null) {
                ClientQuestFile.INSTANCE.clearCachedData();
            }
            closeGui(false);
            if (previousScreen != null) {
                previousScreen.refreshWidgets();
                previousScreen.openGui();
            }
        }) {
            @Override
            public void draw(net.minecraft.client.gui.GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                Component text = Component.translatable(isDeleteMode ? "ftbquesttransl.batch.delete_cache" : "ftbquesttransl.batch.apply");
                theme.drawString(graphics, text, x + w / 2, y + h / 2 - 4, theme.getContentColor(WidgetType.mouseOver(isMouseOver())), Theme.CENTERED);
            }
            @Override
            public void addMouseOverText(dev.ftb.mods.ftblibrary.util.TooltipList list) { }
        };
        add(applyButton);
    }

    @Override
    public void alignWidgets() {
        int panelWidth = Math.min(width - 40, 400);
        int panelHeight = Math.min(height - 40, 500);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        cancelButton.setPosAndSize(panelX, panelY + 10, 20, 20);
        selectAllButton.setPosAndSize(panelX + 30, panelY + 10, 100, 20);
        langFilterButton.setPosAndSize(panelX + 140, panelY + 10, 60, 20);
        applyButton.setPosAndSize(panelX + panelWidth - 110, panelY + 10, 100, 20);

        scrollPanel.setPosAndSize(panelX, panelY + 40, panelWidth - 15, panelHeight - 80);
        scrollBar.setPosAndSize(panelX + panelWidth - 15, panelY + 40, 15, panelHeight - 80);

        scrollPanel.alignWidgets();
    }

    @Override
    public void drawBackground(net.minecraft.client.gui.GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
        int panelWidth = Math.min(w - 40, 400);
        int panelHeight = Math.min(h - 40, 500);
        int panelX = (w - panelWidth) / 2;
        int panelY = (h - panelHeight) / 2;
        theme.drawPanelBackground(graphics, panelX - 5, panelY - 5, panelWidth + 10, panelHeight + 10);
    }

    private class CustomScrollPanel extends Panel {
        public CustomScrollPanel(Panel panel) {
            super(panel);
        }

        @Override
        public void addWidgets() {
            Map<String, String> caches = com.fqt.fqtmod.translation.BatchTranslationCache.getInstance().getAvailableCaches();
            for (Map.Entry<String, String> entry : caches.entrySet()) {
                if (currentFilterLang != null && !"ALL".equals(currentFilterLang) && !"".equals(currentFilterLang)) {
                    if (!entry.getKey().endsWith(":" + currentFilterLang)) continue;
                }
                add(new CacheRowPanel(this, entry.getKey(), entry.getValue()));
            }
        }

        @Override
        public void alignWidgets() {
            int y = 0;
            for (Widget w : widgets) {
                w.setPosAndSize(0, y, width, 24);
                if (w instanceof Panel) {
                    ((Panel) w).alignWidgets();
                }
                y += 24;
            }
        }
    }

            private class CacheRowPanel extends Panel {
        private final String cacheKey;
        private final String chapterName;
        private final String langCode;
        private final int questCount;
        private final int totalQuests;
        private final dev.ftb.mods.ftblibrary.icon.Icon chapterIcon;

        public CacheRowPanel(Panel panel, String cacheKey, String cacheName) {
            super(panel);
            this.cacheKey = cacheKey;
            
            String[] parts = cacheKey.split(":");
            long chapterId = Long.parseLong(parts[0]);
            this.langCode = parts.length > 1 ? parts[1] : "??";
            
            Chapter chapter = ClientQuestFile.INSTANCE.getChapter(chapterId);
            if (chapter != null) {
                this.chapterName = chapter.getTitle().getString();
                this.chapterIcon = chapter.getIcon();
                this.totalQuests = chapter.getQuests().size() + 1;
            } else {
                // Fallback to cacheName if chapter not found
                this.chapterName = cacheName.contains(" - ") ? cacheName.split(" - ")[0] : cacheName;
                this.chapterIcon = Icons.BOOK;
                this.totalQuests = -1;
            }
            
            Map<Long, com.fqt.fqtmod.translation.BatchTranslationCache.TranslatedQuest> chapterCache = com.fqt.fqtmod.translation.BatchTranslationCache.getInstance().getChapterCache(cacheKey);
            this.questCount = chapterCache != null ? chapterCache.size() : 0;
        }

        @Override
        public void addWidgets() {
        }

        @Override
        public void alignWidgets() {
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver()) {
                if (isDeleteMode) {
                    if (selectedCaches.contains(cacheKey)) {
                        selectedCaches.remove(cacheKey);
                    } else {
                        selectedCaches.add(cacheKey);
                    }
                } else {
                    boolean wasSelected = selectedCaches.contains(cacheKey);
                    String chapterPrefix = cacheKey.split(":")[0] + ":";
                    selectedCaches.removeIf(k -> k.startsWith(chapterPrefix));
                    if (!wasSelected) {
                        selectedCaches.add(cacheKey);
                    }
                }
                scrollPanel.refreshWidgets();
                return true;
            }
            return false;
        }

        @Override
        public void draw(net.minecraft.client.gui.GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            super.draw(graphics, theme, x, y, w, h);
            boolean isSelected = selectedCaches.contains(cacheKey);
            
            // 1. Selection status (checkbox/checkmark)
            if (isSelected) {
                Icons.CHECK.withColor(dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0x00FF00)).draw(graphics, x + 4, y + 4, 16, 16);
            }
            
            // 2. Chapter Icon
            chapterIcon.draw(graphics, x + 24, y + 4, 16, 16);
            
            // 3. Chapter Name
            int nameX = x + 44;
            theme.drawString(graphics, chapterName, nameX, y + (h - 8) / 2, dev.ftb.mods.ftblibrary.icon.Color4I.WHITE, 0);
            
            // 4. Language & Count (aligned to right)
            String countStr = totalQuests >= 0 ? questCount + "/" + totalQuests : String.valueOf(questCount);
            String info = "[" + langCode.toUpperCase() + "] (" + countStr + ")";
            int infoWidth = theme.getStringWidth(info);
            theme.drawString(graphics, info, x + w - infoWidth - 5, y + (h - 8) / 2, dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0xAAAAAA), 0);
        }

        @Override
        public void drawBackground(net.minecraft.client.gui.GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver()) {
                dev.ftb.mods.ftblibrary.icon.Color4I.WHITE.withAlpha(20).draw(graphics, x, y, w, h);
            }
        }
    }
}
