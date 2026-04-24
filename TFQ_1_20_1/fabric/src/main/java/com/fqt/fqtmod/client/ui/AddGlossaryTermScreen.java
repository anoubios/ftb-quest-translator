package com.fqt.fqtmod.client.ui;

import com.fqt.fqtmod.translation.GlossaryManager;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class AddGlossaryTermScreen extends BaseScreen {
    private final BaseScreen previousScreen;
    private final GlossaryManager.GlossaryTerm termToEdit;
    private final boolean isEditing;
    
    private String originalValue;
    private String translatedValue;
    private String targetLangValue;

    private TextBox originalBox;
    private TextBox translatedBox;
    private SimpleButton langToggleBtn;
    
    private SimpleButton saveBtn;
    private SimpleButton cancelBtn;
    private SimpleButton infoBtn;

    public AddGlossaryTermScreen(BaseScreen previousScreen, GlossaryManager.GlossaryTerm termToEdit) {
        this.previousScreen = previousScreen;
        this.termToEdit = termToEdit;
        this.isEditing = termToEdit != null;
        
        if (isEditing) {
            this.originalValue = termToEdit.original;
            this.translatedValue = termToEdit.translated;
            this.targetLangValue = termToEdit.targetLang;
        } else {
            this.originalValue = "";
            this.translatedValue = "";
            this.targetLangValue = "ALL";
        }
    }

    @Override
    public boolean onInit() {
        return setFullscreen();
    }

    @Override
    public void addWidgets() {
        originalBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                originalValue = getText();
            }
        };
        originalBox.setText(originalValue);
        originalBox.ghostText = Component.translatable("ftbquesttransl.glossary.original_word").getString();
        add(originalBox);

        translatedBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                translatedValue = getText();
            }
        };
        translatedBox.setText(translatedValue);
        translatedBox.ghostText = Component.translatable("ftbquesttransl.glossary.translated_word").getString();
        add(translatedBox);

        langToggleBtn = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            new LanguageSelectorScreen(this, (lang) -> {
                targetLangValue = lang;
            }).openGui();
        }) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                String text = "ALL".equalsIgnoreCase(targetLangValue) ? "Lang: ANY" : "Lang: " + targetLangValue;
                dev.ftb.mods.ftblibrary.icon.Color4I color = "ALL".equalsIgnoreCase(targetLangValue) ? dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0x00FF00) : dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0xFFCC00);
                theme.drawString(graphics, text, x + w / 2, y + h / 2 - 4, color, Theme.CENTERED);
            }
        };
        add(langToggleBtn);

        saveBtn = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            if (!originalValue.isEmpty() && !translatedValue.isEmpty()) {
                if (isEditing && !originalValue.equals(termToEdit.original)) {
                    GlossaryManager.getInstance().getGlossary().remove(termToEdit);
                }
                if (isEditing) {
                    termToEdit.original = originalValue;
                    termToEdit.translated = translatedValue;
                    termToEdit.targetLang = targetLangValue;
                } else {
                    GlossaryManager.getInstance().getGlossary().add(0, new GlossaryManager.GlossaryTerm(originalValue, translatedValue, targetLangValue));
                }
                GlossaryManager.getInstance().save();
            }
            closeGui(false);
            if (previousScreen != null) {
                previousScreen.refreshWidgets();
                previousScreen.openGui();
            }
        }) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                theme.drawString(graphics, Component.translatable("ftbquesttransl.glossary.accept"), x + w / 2, y + h / 2 - 4, dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0x00FF00), Theme.CENTERED);
            }
        };
        add(saveBtn);
        
        cancelBtn = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {
            closeGui(false);
            if (previousScreen != null) {
                previousScreen.refreshWidgets();
                previousScreen.openGui();
            }
        }) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, WidgetType.mouseOver(isMouseOver()));
                theme.drawString(graphics, Component.translatable("ftbquesttransl.glossary.cancel"), x + w / 2, y + h / 2 - 4, dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0xFF0000), Theme.CENTERED);
            }
        };
        add(cancelBtn);

        infoBtn = new SimpleButton(this, Component.empty(), dev.ftb.mods.ftblibrary.icon.Color4I.empty(), (b, mb) -> {}) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawString(graphics, "(?)", x + w / 2, y + h / 2 - 4, dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0xAAAAAA), Theme.CENTERED);
            }
            @Override
            public void addMouseOverText(dev.ftb.mods.ftblibrary.util.TooltipList list) {
                list.add(Component.translatable("ftbquesttransl.glossary.info.title").withStyle(net.minecraft.ChatFormatting.YELLOW));
                list.add(Component.translatable("ftbquesttransl.glossary.info.desc").withStyle(net.minecraft.ChatFormatting.WHITE));
                list.add(Component.translatable("ftbquesttransl.glossary.info.original").withStyle(net.minecraft.ChatFormatting.GRAY));
                list.add(Component.translatable("ftbquesttransl.glossary.info.translated").withStyle(net.minecraft.ChatFormatting.GRAY));
                list.add(Component.translatable("ftbquesttransl.glossary.info.lang").withStyle(net.minecraft.ChatFormatting.GRAY));
                list.add(Component.translatable("ftbquesttransl.glossary.info.all_mode").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        };
        add(infoBtn);
    }

    @Override
    public void alignWidgets() {
        int panelWidth = 260;
        int panelHeight = 200;
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;

        originalBox.setPosAndSize(panelX + 20, panelY + 30, panelWidth - 40, 24);
        langToggleBtn.setPosAndSize(panelX + 20, panelY + 65, 75, 24);
        translatedBox.setPosAndSize(panelX + 100, panelY + 65, panelWidth - 120, 24);
        
        saveBtn.setPosAndSize(panelX + 20, panelY + 145, 100, 24);
        cancelBtn.setPosAndSize(panelX + 140, panelY + 145, 100, 24);

        Component title = Component.translatable(isEditing ? "ftbquesttransl.glossary.edit_term" : "ftbquesttransl.glossary.add_term");
        int titleWidth = getTheme().getStringWidth(title);
        infoBtn.setPosAndSize(panelX + panelWidth/2 + titleWidth/2 + 4, panelY + 8, 12, 12);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
        int panelWidth = 260;
        int panelHeight = 200;
        int panelX = (w - panelWidth) / 2;
        int panelY = (h - panelHeight) / 2;
        theme.drawPanelBackground(graphics, panelX, panelY, panelWidth, panelHeight);
        Component title = Component.translatable(isEditing ? "ftbquesttransl.glossary.edit_term" : "ftbquesttransl.glossary.add_term");
        theme.drawString(graphics, title, panelX + panelWidth/2, panelY + 10, dev.ftb.mods.ftblibrary.icon.Color4I.WHITE, Theme.CENTERED);
    }
}
