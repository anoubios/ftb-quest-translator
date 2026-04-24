package com.fqt.fqtmod.client.ui;

import com.fqt.fqtmod.translation.GlossaryManager;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public class GlossaryScreen extends BaseScreen {
    private final BaseScreen previousScreen;
    private CustomScrollPanel scrollPanel;
    private PanelScrollBar scrollBar;
    private SimpleButton addButton;
    private SimpleButton cancelButton;

    public GlossaryScreen(BaseScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public boolean onInit() {
        return setFullscreen();
    }

    @Override
    public void addWidgets() {
        cancelButton = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            GlossaryManager.getInstance().save();
            closeGui(false);
            if (previousScreen != null) {
                previousScreen.refreshWidgets();
                previousScreen.openGui();
            }
        }) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                Icons.LEFT.draw(graphics, x + (w - 16) / 2, y + (h - 16) / 2, 16, 16);
            }
        };
        add(cancelButton);

        scrollPanel = new CustomScrollPanel(this);
        add(scrollPanel);

        scrollBar = new PanelScrollBar(this, scrollPanel);
        scrollBar.setCanAlwaysScroll(true);
        add(scrollBar);

        addButton = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            new AddGlossaryTermScreen(getGui(), null).openGui();
        }) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                theme.drawString(graphics, Component.translatable("ftbquesttransl.glossary.add_term"), x + w / 2, y + h / 2 - 4, theme.getContentColor(WidgetType.mouseOver(isMouseOver())), Theme.CENTERED);
            }
        };
        add(addButton);
    }

    @Override
    public void alignWidgets() {
        int panelWidth = Math.min(width - 40, 450);
        int panelHeight = Math.min(height - 80, 500);
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        cancelButton.setPosAndSize(panelX, panelY + 10, 20, 20);
        addButton.setPosAndSize(width / 2 - 50, panelY + 10, 100, 20);

        scrollPanel.setPosAndSize(panelX, panelY + 40, panelWidth - 15, panelHeight - 80);
        scrollBar.setPosAndSize(panelX + panelWidth - 15, panelY + 40, 15, panelHeight - 80);

        scrollPanel.alignWidgets();
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
        int panelWidth = Math.min(w - 40, 450);
        int panelHeight = Math.min(h - 80, 500);
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
            List<GlossaryManager.GlossaryTerm> list = GlossaryManager.getInstance().getGlossary();
            for (GlossaryManager.GlossaryTerm term : list) {
                add(new TermRowPanel(this, term));
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

    private class TermRowPanel extends Panel {
        private final GlossaryManager.GlossaryTerm term;
        private SimpleButton removeButton;
        private SimpleButton editButton;

        public TermRowPanel(Panel panel, GlossaryManager.GlossaryTerm term) {
            super(panel);
            this.term = term;
        }

        @Override
        public void addWidgets() {
            removeButton = new SimpleButton(this, Component.translatable("ftbquesttransl.glossary.remove"), Icons.BIN, (b, mb) -> {
                GlossaryManager.getInstance().getGlossary().remove(term);
                GlossaryManager.getInstance().save();
                TermRowPanel.this.getGui().refreshWidgets();
            }) {
                @Override
                public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                    Icons.BIN.draw(graphics, x, y, w, h);
                }
            };
            add(removeButton);

            editButton = new SimpleButton(this, Component.translatable("ftbquesttransl.glossary.edit"), Icons.SETTINGS, (b, mb) -> {
                new AddGlossaryTermScreen(TermRowPanel.this.getGui(), term).openGui();
            }) {
                @Override
                public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                    Icons.NOTES.draw(graphics, x, y, w, h);
                }
            };
            add(editButton);
        }

        @Override
        public void alignWidgets() {
            editButton.setPosAndSize(width - 40, 4, 16, 16);
            removeButton.setPosAndSize(width - 20, 4, 16, 16);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            super.draw(graphics, theme, x, y, w, h);
            String lang = "ALL".equalsIgnoreCase(term.targetLang) ? "" : " [" + term.targetLang + "]";
            theme.drawString(graphics, term.original + " -> " + term.translated + lang, x + 5, y + (h - 8) / 2, dev.ftb.mods.ftblibrary.icon.Color4I.WHITE, 0);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver()) {
                dev.ftb.mods.ftblibrary.icon.Color4I.WHITE.withAlpha(20).draw(graphics, x, y, w, h);
            }
        }
    }
}
