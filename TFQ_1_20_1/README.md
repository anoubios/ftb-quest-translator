# FTB Quest Translator

A client-side addon for [FTB Quests](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge) that adds an in-game translate button to the quest view panel. Translates quest titles, subtitles, and descriptions on-the-fly using free translation APIs.

## ✨ Features

- 🌐 **One-click translation** — Toggle button in the quest panel
- 🔄 **Auto language detection** — Uses your Minecraft language settings
- 📦 **In-memory caching** — No repeated API calls for the same text
- 🎯 **Client-side only** — No server changes needed
- 🔧 **Multiple providers** — Google Translate (free) and MyMemory API
- ⚡ **Async translation** — No UI freezing during translation

## 📥 Installation

1. Install [FTB Quests](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge) and [FTB Library](https://www.curseforge.com/minecraft/mc-mods/ftb-library-forge)
2. Download the latest release for your version and mod loader
3. Drop the JAR into your `mods/` folder
4. Launch the game!

## 🎮 Usage

1. Open any quest in FTB Quests
2. Click the 🌐 translate button (top-left of the quest panel)
3. Wait for translation to complete
4. Click again to restore original text
5. Translation clears automatically when closing the quest

## ⚙️ Configuration

Configuration file location:
- **NeoForge**: `.minecraft/config/ftbquesttransl-client.toml`
- **Fabric**: `.minecraft/config/ftbquesttransl.json`

| Setting | Default | Description |
|---------|---------|-------------|
| `targetLanguage` | `auto` | Target language (ISO 639-1). `auto` = Minecraft's language |
| `enableCaching` | `true` | Cache translations in memory |
| `translationProvider` | `GOOGLE` | API provider: `GOOGLE` or `MYMEMORY` |

## 🔧 Supported Versions

| Minecraft | NeoForge | Fabric |
|-----------|----------|--------|
| 1.21.1    | ✅       | ✅     |

## 📋 Dependencies

- **FTB Quests** — Required
- **FTB Library** — Required (dependency of FTB Quests)

## 📄 License

This project is licensed under the [MIT License](LICENSE).

## 🤝 Contributing

Contributions are welcome! Feel free to open issues and pull requests.

## Credits

- **FTB Team** — For FTB Quests and FTB Library
- **Google Translate** / **MyMemory** — Translation APIs
