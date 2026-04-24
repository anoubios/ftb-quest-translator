# Changelog

All notable changes to this project will be documented in this file.

## [1.4.0] - 2026-04-19

### Added
- **Glossary (Translation Memory)**: You can now configure words to remain untranslated using `config/tfq_glossary.json`. Perfect for keeping mod names like "Redstone", "Create", or "Mekanism" in their original form. The system securely masks these words as `{{gX}}` tokens before sending them to the API, ensuring they don't break the grammatical context of the translated sentence.
- **Batch Translation**: Added two new buttons to the FTB Quests sidebar. You can now translate the entire questbook in the background. The mod uses a smart rate-limiting queue (1.5s delay) to avoid IP bans from free APIs. Progress is saved to `config/tfq_batch_progress.json`, allowing you to safely close the game and resume translation later.
- **Text-to-Speech (TTS) Voiceover**: Added a speaker icon next to the quest translation button. Clicking it reads the quest text aloud using Google TTS. It dynamically reads either the original English or the translated text, depending on your current view state. (Note: The icon will flash red if the language isn't supported for voiceover).
- **Regional Language Mapping**: The mod now correctly maps Minecraft's regional variants (like `es_mx` or `es_ar`) to their root API languages (`es`). For Chinese, it correctly splits between Simplified (`zh-CN`) and Traditional (`zh-TW`). The `[T]` tooltip in the language menu now informs users if they are using a mapped regional variant.

### Fixed
- **AUTO Provider Fallback Bug**: Fixed a critical issue where falling back from Google to MyMemory could cache garbage (`???`) if the target language wasn't actually supported by MyMemory (e.g., Kyrgyz `ky`). Added strict `supportsLanguage()` checks before fallback.
- **Config UI Title**: Fixed the raw translation key `ftbquesttransl.settings` showing up as the title in the FTB Quests configuration menu. It now properly displays "FTB Quests Translator Settings" (or localized equivalents).

## [1.3.3] - 2026-04-18

### Added
- **Localization Key Resolution**: Modpacks that use FTB Quests' built-in localization system (`{ftbquests.chapter.name.quest1.description1}` keys in SNBT) are now fully supported. The mod automatically resolves these keys via Minecraft's `I18n` language system before translation, while preserving FTB special tags (`{image:...}`, `{@pagebreak}`, `{open_url:...}`).
- **Title Fallback**: Quests without an explicit `title:` field in SNBT (which use the task item name as title) now have their titles translated. The mod falls back to `quest.getTitle().getString()` when `getRawTitle()` is empty.

### Fixed
- **Quests Not Translating**: Fixed a critical bug where the `FORMAT_CODE` regex (`\{[A-Za-z0-9_@:-]+[^}]*\}`) incorrectly matched FTB Quests localization keys as format tags. After stripping, the entire text became empty, causing "No translatable text found" for all quests using localization keys.

## [1.3.2] - 2026-04-18

### Added
- **DoH (DNS-over-HTTPS) Bypass Rewrite**: Completely rewrote the DoH bypass using raw SSL sockets with `InetAddress.getByAddress()`. Java automatically sets correct TLS SNI from the hostname while connecting to the DoH-resolved IP, and a manually constructed HTTP/1.1 request ensures the proper `Host` header. This enables reliable Google Translate access for users in regions with DNS-level censorship (e.g., Russia).
- **Batch Translation**: All text segments (title, subtitle, description lines) are now joined with a unique separator and sent in a **single HTTP request** instead of 7+ parallel requests. Dramatically reduces API rate limiting risk and improves translation speed.
- **HTML Entity Decoding**: Added `decodeHtmlEntities()` to convert API-returned HTML entities (`&quot;` → `"`, `&amp;` → `&`, `&lt;` → `<`, etc.) back to plain text before rendering.
- **Format Code Sanitizer**: Added `sanitizeFormatCodes()` that strips `&` before any character that isn't a valid Minecraft formatting code, preventing "Invalid formatting!" errors in quest UI.

### Fixed
- **DoH 404 Error**: The old `HttpsURLConnection`-based DoH approach returned HTTP 404 because Google's edge servers couldn't route requests from raw IP + Host header. The new raw SSL socket approach resolves this completely.
- **`&quot;` Artifacts in Quest Text**: MyMemory API returns HTML-encoded responses. After the format sanitizer stripped `&` from `&quot;`, the text `quot;` remained visible. Fixed by decoding HTML entities before format sanitization.
- **AUTO Provider Selecting Wrong Provider**: When switching providers in config, stale probe results incorrectly marked Google as unavailable. Now `providerAvailability` is cleared on provider change, forcing a fresh availability check.
- **Google Translate JSON Parsing**: Raw SSL socket responses include chunked encoding remnants after the JSON body, causing `MalformedJsonException`. Added JSON boundary detection and lenient parsing to handle this.

### Removed
- **Lingva Translate Provider**: Removed entirely due to unreliable availability and frequent HTTP 429 rate limits. The auto chain is now simplified to **Google → MyMemory**.

## [1.3.1] - 2026-04-12

### Fixed
- **API Formatting Errors**: Fixed issue where manually entering target language variants (like `uk_ua` or `en_us`) crashed the DeepL integration by converting underscores `_` to proper hyphens `-`.
- **MyMemory Language**: Enforced uniform `Autodetect` for MyMemory translation endpoint structure.
- **Cache Efficiency**: Unchanged words (e.g., untranslatable nouns) are now cleanly cached locally. The mod will no longer relentlessly query translation APIs for words that remain identically translated.
- **Text Skipping**: Removed faulty regex behavior that incorrectly skipped formatting boundaries around `{image}` macro insertions.
- **DeepL Config Security**: Re-engineered the translation settings. The DeepL API token is uncoupled from shared files and strictly stored within the `local/ftbquesttranslator-secrets.json` dictionary.

## [1.2.1-1.20.1] - 2026-04-11

### Added
- Initial backport to Minecraft 1.20.1 (Forge & Fabric).
- Reverted Data Components (1.21+) back to classic NBT handling for item manipulation flexibility.
- Switched NeoForge loader to standard LexForge to match 1.20.1 modpack ecosystems.

## [1.2.2] - 2026-04-11

### Fixed
- **API Rate Limiting**: The background connection check no longer forcefully pings APIs every time the UI is resized or rebuilt. Added a 2-minute cooldown cache to protect against temporary rate-limit bans from Google/DeepL.
- **Micro-stutters**: The translation engine now delegates HTTP requests to an isolated thread pool with background priorities. Previous versions accidentally shared Minecraft's internal `commonPool`, which could momentarily starve rendering and chunk loading loops.
- **Cache Data Loss**: Rebuilt the Translation Cache with disk persistence (`ftbqt-cache.json`). The mod now asynchronously saves translations back to disk 5 seconds after translating, preventing wasted DeepL quotas on game restarts.
- **Memory Leaks**: Refactored Translate Providers into strict Singleton patterns, eliminating thousands of pointless object instantiations when traversing quests.

## [1.2.1] - 2026-04-10

### Added
- **DeepL API Provider**: You can now select `DEEPL` as your translation provider and use a DeepL API key in configs. This provides drastically better, context-aware translations.
- **Contextual Formatted Translation**: Entirely rewrote the formatted text translation logic (`TextFormatUtils`). Instead of breaking down sentences into chunked format codes (which destroyed the grammatical context), the mod now securely masks Minecraft codes and FTB tags into XML tags (`<mX/>`) before sending them.
  - This solves formatting-induced grammar errors (e.g., incorrect case endings like "Доля цього світ" instead of "світу").
- **Universal Tag Protection**: FTB Quests macros such as `{@pagebreak}` and `{image: ...}` handles are now perfectly shielded and treated as regular colors during translation, preventing them from being mistakenly translated and broken mid-sentence.

## [1.2.0] - 2026-04-10

### Added
- **Language Support Indicator (`[T]`)**: A new green `[T]` icon now appears in the vanilla Minecraft Language Selection screen next to the languages that are supported by the translation API.
- **Visual Connection Diagnostics**: The mod now seamlessly performs asynchronous ping tests to translation APIs in the background. If you have no internet or the server is down, the translation button in the quest panel will display **pulsing red rings**.
- **Error Tooltips**: Clear tooltip explanations when hovering over the button during connection failures or unsupported language selections.

### Changed
- **Strict Language Whitelist**: The mod now accurately verifies the Minecraft language code against a strict whitelist of 133 real-world ISO-639-1 languages. Previously, non-standard local dialects (e.g., Галицька / Halych, Pirate Speak) would silently fail to translate while pretending to be supported. 
- **Graceful Failure UI**: Selecting an unsupported dialect or losing internet connection now prevents the translation attempt, displays a red error animation on the button, and updates the tooltip immediately so you know exactly why it's not working.

## [1.1.2] - 2026-04-10

### Changed
- **NeoForge**: Completely removed strict version requirements for FTB Quests and FTB Library (`versionRange` is now set to `*`), fixing crashes and loading errors in modpacks running older FTB dependencies.

## [1.1.1] - 2026-04-10

### Changed
- Downgraded target mod loader versions to match FTB Quests for maximum modpack compatibility
  - **NeoForge**: Downgraded recommended version to `21.1.213`
  - **Fabric**: Downgraded loader version to `0.15.11` and Fabric API to `0.102.1+1.21.1`

## [1.1.0] - 2026-04-04

### Changed
- **Formatting Preservation**: Translation now preserves all text formatting codes
  - Bold (`&l`), italic (`&o`), underline (`&n`), strikethrough (`&m`), obfuscated (`&k`)
  - Minecraft color codes (`&0`-`&f`, `&6` for gold, etc.)
  - Hex color codes (`&#FF0923`, `&#9FFF2A`, etc.)
  - Reset code (`&r`)
  - Multiple consecutive codes (`&l&o` for bold italic)
- Segment-based translation: each formatted span is translated independently
- Structural elements ({@pagebreak}, {image:...}, JSON) still preserved as-is

### Fixed
- Formatting codes no longer "shift" to wrong words after translation
- Colors now correctly apply to the translated words they were meant for
- **MyMemory provider**: Fixed URL encoding bug (`|` not encoded as `%7C`) that caused all MyMemory translations to fail
- **Translate button**: Moved from top-left (overlapping title) to bottom-left corner
- **Translation cache**: Failed translations are no longer cached as successful ones

## [1.0.0] - 2026-04-04

### Added
- Initial release
- Translate button in quest view panel
- Google Translate provider (free, no API key)
- MyMemory provider (free, 5000 chars/day)
- In-memory translation caching
- Auto language detection from Minecraft settings
- Toggle translation on/off
- Auto-clear on quest panel close
- Ukrainian (uk_ua) and English (en_us) localization
- Formatting code stripping for clean translated text
- NeoForge 1.21.1 support
- Fabric 1.21.1 support
