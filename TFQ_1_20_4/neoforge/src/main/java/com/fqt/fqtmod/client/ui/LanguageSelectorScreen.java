package com.fqt.fqtmod.client.ui;

import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class LanguageSelectorScreen extends BaseScreen {
    private static final Map<String, String> LANGUAGES = new LinkedHashMap<>();
    static {
        LANGUAGES.put("en", "English");
        LANGUAGES.put("uk", "Ukrainian");
        LANGUAGES.put("ru", "Russian");
        LANGUAGES.put("es", "Spanish");
        LANGUAGES.put("fr", "French");
        LANGUAGES.put("de", "German");
        LANGUAGES.put("pl", "Polish");
        LANGUAGES.put("ja", "Japanese");
        LANGUAGES.put("zh", "Chinese (Simplified)");
        LANGUAGES.put("zh-TW", "Chinese (Traditional)");
        LANGUAGES.put("ko", "Korean");
        LANGUAGES.put("it", "Italian");
        LANGUAGES.put("pt", "Portuguese");
        LANGUAGES.put("nl", "Dutch");
        LANGUAGES.put("tr", "Turkish");
        LANGUAGES.put("af", "Afrikaans");
        LANGUAGES.put("sq", "Albanian");
        LANGUAGES.put("am", "Amharic");
        LANGUAGES.put("ar", "Arabic");
        LANGUAGES.put("hy", "Armenian");
        LANGUAGES.put("as", "Assamese");
        LANGUAGES.put("ay", "Aymara");
        LANGUAGES.put("az", "Azerbaijani");
        LANGUAGES.put("bm", "Bambara");
        LANGUAGES.put("eu", "Basque");
        LANGUAGES.put("be", "Belarusian");
        LANGUAGES.put("bn", "Bengali");
        LANGUAGES.put("bho", "Bhojpuri");
        LANGUAGES.put("bs", "Bosnian");
        LANGUAGES.put("bg", "Bulgarian");
        LANGUAGES.put("ca", "Catalan");
        LANGUAGES.put("ceb", "Cebuano");
        LANGUAGES.put("co", "Corsican");
        LANGUAGES.put("hr", "Croatian");
        LANGUAGES.put("cs", "Czech");
        LANGUAGES.put("da", "Danish");
        LANGUAGES.put("dv", "Dhivehi");
        LANGUAGES.put("eo", "Esperanto");
        LANGUAGES.put("et", "Estonian");
        LANGUAGES.put("ee", "Ewe");
        LANGUAGES.put("tl", "Filipino (Tagalog)");
        LANGUAGES.put("fi", "Finnish");
        LANGUAGES.put("fy", "Frisian");
        LANGUAGES.put("gl", "Galician");
        LANGUAGES.put("ka", "Georgian");
        LANGUAGES.put("el", "Greek");
        LANGUAGES.put("gn", "Guarani");
        LANGUAGES.put("gu", "Gujarati");
        LANGUAGES.put("ht", "Haitian Creole");
        LANGUAGES.put("ha", "Hausa");
        LANGUAGES.put("haw", "Hawaiian");
        LANGUAGES.put("hi", "Hindi");
        LANGUAGES.put("hmn", "Hmong");
        LANGUAGES.put("hu", "Hungarian");
        LANGUAGES.put("is", "Icelandic");
        LANGUAGES.put("ig", "Igbo");
        LANGUAGES.put("ilo", "Ilocano");
        LANGUAGES.put("id", "Indonesian");
        LANGUAGES.put("ga", "Irish");
        LANGUAGES.put("jv", "Javanese");
        LANGUAGES.put("kn", "Kannada");
        LANGUAGES.put("kk", "Kazakh");
        LANGUAGES.put("km", "Khmer");
        LANGUAGES.put("rw", "Kinyarwanda");
        LANGUAGES.put("kri", "Krio");
        LANGUAGES.put("ku", "Kurdish");
        LANGUAGES.put("ky", "Kyrgyz");
        LANGUAGES.put("lo", "Lao");
        LANGUAGES.put("la", "Latin");
        LANGUAGES.put("lv", "Latvian");
        LANGUAGES.put("ln", "Lingala");
        LANGUAGES.put("lt", "Lithuanian");
        LANGUAGES.put("lg", "Luganda");
        LANGUAGES.put("lb", "Luxembourgish");
        LANGUAGES.put("mk", "Macedonian");
        LANGUAGES.put("mg", "Malagasy");
        LANGUAGES.put("ms", "Malay");
        LANGUAGES.put("ml", "Malayalam");
        LANGUAGES.put("mt", "Maltese");
        LANGUAGES.put("mi", "Maori");
        LANGUAGES.put("mr", "Marathi");
        LANGUAGES.put("mn", "Mongolian");
        LANGUAGES.put("my", "Myanmar (Burmese)");
        LANGUAGES.put("ne", "Nepali");
        LANGUAGES.put("no", "Norwegian");
        LANGUAGES.put("ny", "Nyanja (Chichewa)");
        LANGUAGES.put("or", "Odia (Oriya)");
        LANGUAGES.put("om", "Oromo");
        LANGUAGES.put("ps", "Pashto");
        LANGUAGES.put("fa", "Persian");
        LANGUAGES.put("pa", "Punjabi");
        LANGUAGES.put("qu", "Quechua");
        LANGUAGES.put("ro", "Romanian");
        LANGUAGES.put("sm", "Samoan");
        LANGUAGES.put("sa", "Sanskrit");
        LANGUAGES.put("gd", "Scots Gaelic");
        LANGUAGES.put("nso", "Sepedi");
        LANGUAGES.put("sr", "Serbian");
        LANGUAGES.put("st", "Sesotho");
        LANGUAGES.put("sn", "Shona");
        LANGUAGES.put("sd", "Sindhi");
        LANGUAGES.put("si", "Sinhala");
        LANGUAGES.put("sk", "Slovak");
        LANGUAGES.put("sl", "Slovenian");
        LANGUAGES.put("so", "Somali");
        LANGUAGES.put("su", "Sundanese");
        LANGUAGES.put("sw", "Swahili");
        LANGUAGES.put("sv", "Swedish");
        LANGUAGES.put("tg", "Tajik");
        LANGUAGES.put("ta", "Tamil");
        LANGUAGES.put("tt", "Tatar");
        LANGUAGES.put("te", "Telugu");
        LANGUAGES.put("th", "Thai");
        LANGUAGES.put("ti", "Tigrinya");
        LANGUAGES.put("ts", "Tsonga");
        LANGUAGES.put("ak", "Twi (Akan)");
        LANGUAGES.put("ur", "Urdu");
        LANGUAGES.put("ug", "Uyghur");
        LANGUAGES.put("uz", "Uzbek");
        LANGUAGES.put("vi", "Vietnamese");
        LANGUAGES.put("cy", "Welsh");
        LANGUAGES.put("xh", "Xhosa");
        LANGUAGES.put("yi", "Yiddish");
        LANGUAGES.put("yo", "Yoruba");
        LANGUAGES.put("zu", "Zulu");
    }

    private final BaseScreen parent;
    private final Consumer<String> onSelect;
    private TextBox searchBox;
    private LangListPanel listPanel;
    private PanelScrollBar scrollBar;
    private String filterText = "";

    public LanguageSelectorScreen(BaseScreen parent, Consumer<String> onSelect) {
        this.parent = parent;
        this.onSelect = onSelect;
    }

    @Override
    public boolean onInit() {
        return setFullscreen();
    }

    @Override
    public void addWidgets() {
        int cx = width / 2;
        int cy = height / 2;

        SimpleButton closeBtn = new SimpleButton(this, Component.empty(), Icons.CANCEL, (b, mb) -> {
            parent.openGui();
        }) {
            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                Icons.CANCEL.draw(graphics, x, y, w, h);
            }
        };
        closeBtn.setPosAndSize(cx + 85, cy - 115, 16, 16);
        add(closeBtn);

        searchBox = new TextBox(this) {
            @Override
            public void onTextChanged() {
                filterText = getText().toLowerCase();
                listPanel.refreshWidgets();
                if (scrollBar != null) scrollBar.setValue(0);
            }
        };
        searchBox.ghostText = "Search language or code (e.g. es)...";
        searchBox.setPosAndSize(cx - 100, cy - 90, 200, 16);
        add(searchBox);

        listPanel = new LangListPanel(this);
        listPanel.setPosAndSize(cx - 100, cy - 70, 185, 150);
        add(listPanel);

        scrollBar = new PanelScrollBar(this, listPanel);
        scrollBar.setCanAlwaysScroll(true);
        scrollBar.setScrollStep(16);
        scrollBar.setPosAndSize(cx + 85, cy - 70, 15, 150);
        add(scrollBar);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        int cx = width / 2;
        int cy = height / 2;
        int pw = 220;
        int ph = 240;
        int px = cx - pw / 2;
        int py = cy - ph / 2;

        // Render solid dark background instead of semi-transparent
        graphics.fill(px, py, px + pw, py + ph, 0xFF202020);
        
        // Draw borders
        graphics.fill(px - 1, py - 1, px + pw + 1, py, 0xFF505050); // Top
        graphics.fill(px - 1, py + ph, px + pw + 1, py + ph + 1, 0xFF505050); // Bottom
        graphics.fill(px - 1, py, px, py + ph, 0xFF505050); // Left
        graphics.fill(px + pw, py, px + pw + 1, py + ph, 0xFF505050); // Right

        theme.drawString(graphics, "Select Target Language", px + pw / 2, py + 10, theme.getContentColor(WidgetType.NORMAL), Theme.CENTERED);
    }

    private class LangListPanel extends Panel {
        public LangListPanel(Panel panel) {
            super(panel);
        }

        @Override
        public void addWidgets() {
            String mcLang = com.fqt.fqtmod.translation.QuestTranslationManager.getInstance().getTargetLanguage();
            
            // Priority 1: ALL
            if (matchesFilter("ALL", "Global")) {
                add(new LangEntryBtn(this, "ALL", "Global", dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0x00FF00)));
            }

            // Priority 2: Current Minecraft Language
            String mcLangName = LANGUAGES.getOrDefault(mcLang, "Minecraft Language");
            if (matchesFilter(mcLang, mcLangName)) {
                add(new LangEntryBtn(this, mcLang, mcLangName + " (Current)", dev.ftb.mods.ftblibrary.icon.Color4I.rgb(0x00FFFF)));
            }

            // The Rest
            for (Map.Entry<String, String> entry : LANGUAGES.entrySet()) {
                if (entry.getKey().equals(mcLang)) continue; // Skip since it's at top
                if (matchesFilter(entry.getKey(), entry.getValue())) {
                    add(new LangEntryBtn(this, entry.getKey(), entry.getValue(), dev.ftb.mods.ftblibrary.icon.Color4I.WHITE));
                }
            }
        }

        @Override
        public void alignWidgets() {
            int y = 0;
            for (Widget w : widgets) {
                w.setPosAndSize(0, y, width, 16);
                y += 16;
            }
        }

        private boolean matchesFilter(String code, String name) {
            if (filterText.isEmpty()) return true;
            return code.toLowerCase().contains(filterText) || name.toLowerCase().contains(filterText);
        }
    }

    private class LangEntryBtn extends SimpleButton {
        private final String code;
        private final String name;
        private final dev.ftb.mods.ftblibrary.icon.Color4I color;

        public LangEntryBtn(Panel panel, String code, String name, dev.ftb.mods.ftblibrary.icon.Color4I color) {
            super(panel, Component.empty(), Icons.INFO, (b, mb) -> {
                onSelect.accept(code);
                LanguageSelectorScreen.this.parent.openGui();
            });
            this.code = code;
            this.name = name;
            this.color = color;
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver()) {
                graphics.fill(x, y, x + w, y + h, 0x44FFFFFF);
            }
            theme.drawString(graphics, "[" + code + "] " + name, x + 5, y + 4, color, 0);
        }
    }
}
