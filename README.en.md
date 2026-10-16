<p align="center">
  <img src="logo/2026/wynnchayuan-banner-animated-680x240.gif" width="680" alt="WynnChaYuan">
</p>

# WynnChaYuan

繁體中文：**[README.md](README.md)**

A multi-language translation mod for Wynncraft (Fabric 1.21.11). **The original text is kept**; the
translation is shown beside it, or written in its place.

[Download](https://github.com/LyuChaCha/WynnChaYuan/releases/latest) ·
[Changelog](CHANGELOG.md) ·
[Report a problem](https://github.com/LyuChaCha/WynnChaYuan/issues) ·
[Ko-fi](https://ko-fi.com/lyuchacha)

> [!IMPORTANT]
> **About the translations.** Some of the translations are written by hand and then rendered by AI,
> a smaller part is translated by AI directly, and most of the core content has been
> proofread by a person. The other languages are currently AI-translated from the
> Traditional Chinese, so expect mistranslations and inconsistent terms.
> Report problems to **LyuChaCha** on Discord or on
> [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues).

## Features

### What it translates

| Content | What is translated, and how it is shown |
|---|---|
| Item tooltips | Gear lore, stats, Major IDs; a panel beside the tooltip, or written into the tooltip itself |
| Ability trees | Nodes, descriptions and archetypes for all five classes |
| Quest dialogue, dialogue choices | Written into **Wynncraft's own dialogue box** (frame, nameplate and portrait kept), typed out in step with the original; or a separate box |
| Quest tracker | Its own box; the heading says what is tracked (quest, world event, cave, raid…) |
| NPC nameplates, floating text | A box while you look at one, or replaced in place; crafting stations and interaction prompts included |
| Chat messages | Server messages (quest completions, rewards, area transitions); column layouts such as the Lootrun summary and beacons stay aligned. Player chat is never translated |
| Menus and interfaces | Trade market, guild, store and other screens |
| Lootruns, raids, dungeons | Missions, boons, beacons, aspects, gambits, end-of-run and loot panels, dungeon names and keys |
| Discoveries, secret discoveries | Names, descriptions and stories |
| Title text | The big title and subtitle in the middle of the screen |

Gear names stay in English by default: they are proper nouns, and the trade market and the wiki use
the English ones. You can turn them on in the settings.

### More

- **Translations update themselves**: translations are downloaded from GitHub, so new ones arrive on your next launch, **no mod update needed**. Offline, the last cache or the copy in the jar is used
- **Market search**: search the trade market in your language; the English name is sent. Matches are listed as you type: ↑↓ to pick, Tab to fill. With [WynnMarketSearch](https://modrinth.com/mod/wynnmarketsearch) installed, its search panel takes translated names too
- **Copy chat**: lists recent chat messages; click one to copy it for a report (key unbound by default)
- **Panel screenshot**: copies the translation panel to the clipboard or saves it to a file (key unbound by default)
- **Adjustable panels**: boxes can be dragged; dialogue, choices and the tracker can be resized from the corner
- **Update notice**: says so once in chat when a new version is out, linking to GitHub Releases
- **Leaf badges**: developers, sponsors and contributors get a leaf before their name (gold, cyan, green), visible only to others running the mod; see [Roles and badges](#roles-and-badges)

**Client-side only.** The server does not need it.

### Settings

Three ways to open them:

- [Mod Menu](https://modrinth.com/mod/modmenu)'s mod list → WynnChaYuan → configure
- type `/wcy` (or `/wynnchayuan`) in chat
- bind a key under Controls → WynnChaYuan (**nothing is bound by default**, so the mod takes none of your keys)

| Tab | What's in it |
|---|---|
| Items | Item translation (separate panel / replace in place / off), translate item names, market search, panel screenshot |
| Panel | Follow the mouse or pin it, which side, gap, arrange the boxes; the theme colour (of the settings screen) and the border colour (of the boxes in game) are set separately |
| Dialogue | Quest dialogue, dialogue choices (separate box / replace in place / off), hold time, dialogue/tracker boxes |
| World & chat | Nameplates and floating text (with range and aim angle), chat messages, title text, copy chat |
| Data | Translation language, fallback language, interface language, translation source, reload, collect untranslated strings, collect GUI text, export untranslated strings, how to submit, debug dumps |

Hover a setting for an explanation.

## Capes, the grey leaf, and what the mod sends

- **Capes**: wear your own WynnChaYuan cape, visible to other players who have this mod. Upload and browse capes at <https://wynnchayuan.com> (F6 > Badge & cape > Cape gallery opens it signed in). Anyone can upload a PNG (up to 1 MB); GIFs and the 20 MB limit are for developers, sponsors and contributors. Draw it on a template from [`capes/templates`](https://github.com/LyuChaCha/WynnChaYuan/tree/main/capes/templates).
- **Leaves**: every player who has this mod gets a leaf before their name (grey if you have no role), visible to each other. A developer's rainbow mode and the roles you chose to show look the same to others as they do to you.
- **These two features talk to our server (wynnchayuan.com).** The mod checks in once when you join Wynncraft and sends: your Minecraft UUID, your Mojang-signed public profile (name and skin), a public key and signature that prove the account is yours, whether to show your cape, and your badge settings (rainbow leaf, which roles to show). Uploading a cape also sends the image you chose. **Your private key and login credentials never leave the game; the mod does not send your world, chat or any game content, and the server does not store IP addresses.** The public list holds hashed codes only, not UUIDs or names.
- **There is no switch for this**: the mod checks in whenever it is installed and you join Wynncraft, so every user has a leaf. If you do not want others to see your cape, turn off "Wear my cape" under F6 > Badge & cape.
- **Site account** (optional): register at wynnchayuan.com with Discord, then type `/wcy link <code>` in game to link your game ID. We only read your Discord account ID and display name.
- **Update button**: the release notes screen has an Update button. Only when you press it does the mod download the new version from GitHub Releases, and it is swapped in when you close the game.

## Languages and progress

| Language | Coverage |
|---|---|
| `zh_tw` Traditional Chinese | Main language, everything |
| `zh_cn` Simplified Chinese | Everything, quest dialogue included |
| `ja_jp` Japanese | Everything, quest dialogue included |
| `ru_ru` Russian | Everything, quest dialogue included |
| `ko_kr` Korean | Everything, quest dialogue included |
| `es_es` Spanish | Everything, quest dialogue included |
| `tr_tr` Turkish | Everything, quest dialogue included |

Switch under **Settings → Data**, without changing the game's language or restarting:

- **Translation language**: which translations to show
- **Fallback language**: what to show where that language has nothing yet (another language, or the original)
- **Interface language**: the language of the settings screens themselves (English included)

<!-- 進度:開始 -->
更新於 2026-10-12 / Updated 2026-10-12

**翻了哪些**：任務對話與任務書、物品（名稱、詞條、敘述、Major ID）、技能樹、介面（設定畫面、背包、交易市場、公會、地圖、追蹤欄）、NPC 與地區名稱、看板與聊天公告。

**大概翻到哪**：7 種語言目前都在 **95% 以上**，繁體中文最完整。剩下的多半是零星的名稱與半句話，而且遊戲還在更新——**一定還有漏的**。看到沒翻、翻錯或版面跑掉的，[開個 issue](https://github.com/LyuChaCha/WynnChaYuan/issues) 告訴我們就好。

**What's covered**: quest dialogue and the quest book, items (names, stats, lore, Major IDs), the ability tree, the interface (settings, inventory, trade market, guild, map, tracker), NPC and place names, signs and chat announcements.

**Roughly how far**: every language is past **95%**, Traditional Chinese being the most complete. What is left is mostly stray names and half-sentences, and the game keeps changing — **there will be gaps**. Found something untranslated, wrong, or laid out badly? [Open an issue](https://github.com/LyuChaCha/WynnChaYuan/issues).

每一種語言還缺哪些檔案 / Per-language breakdown: [PROGRESS.md](docs/PROGRESS.md)
<!-- 進度:結束 -->

## Install

| Requirement | Version |
|---|---|
| Minecraft | 1.21.11 |
| Loader | Fabric |
| Dependencies | [Wynntils](https://modrinth.com/mod/wynntils) 4.2+, [Fabric API](https://modrinth.com/mod/fabric-api) |

1. Download the jar: [GitHub Releases](https://github.com/LyuChaCha/WynnChaYuan/releases/latest) ·
   [Modrinth](https://modrinth.com/mod/wynnchayuan) ·
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/wynnchayuan)
2. Put it in `mods/`. In game, open the settings from Mod Menu or by typing `/wcy` in chat.

### Mod compatibility

| Mod | Status | Notes |
|---|---|---|
| [Wynntils](https://modrinth.com/mod/wynntils) 4.2+ | **Required** | Item, quest and chat data all come through it; its own screens (content book, map, quest and cave names) are translated too |
| [Fabric API](https://modrinth.com/mod/fabric-api) | **Required** | |
| [Mod Menu](https://modrinth.com/mod/modmenu) | Optional, supported | Adds a configure button to the mod list |
| [WynnMarketSearch](https://modrinth.com/mod/wynnmarketsearch) | Optional, supported | Its search panel finds items by translated name and shows the translation next to the English name; what gets sent is still the English name |
| [Wynnventory](https://modrinth.com/mod/wynnventory) | Optional, supported | Its price box, reward screen and settings screen are translated too (item names follow the "Translate item names" setting); hold Shift to see the original. The switch only appears in the settings when it is installed |
| [WynnMod](https://modrinth.com/mod/wynnmod) | Compatible | The lines it reads to decide a raid's outcome stay in English while it is installed, so streaks are counted correctly (without it you see the translation as usual) |
| Mods that add sections to tooltips (Nori, Wynnpool…) | Compatible | Those sections are kept as they are. If they make the translation panel longer than the original, list their labels in `config/wynnchayuan/third-party-sections.json` to filter them |

Mods not listed here usually work alongside as well: this mod only changes the text drawn on your client. If something clashes, [open an issue](https://github.com/LyuChaCha/WynnChaYuan/issues) with your mod list.

## A line switches back to English halfway?

**That is normal.** It means the sentence is not in the corpus yet, so the mod shows the original.

Export it and send it to us as described in [How to help](#how-to-help). Once it is added, everyone
gets it on their next launch; no mod update needed.

## How to help

Quest dialogue and NPC nameplates have no official data source; they only arrive when players run
into them. **You do not have to translate anything**, just hand us the gaps you found:

1. **Settings → Data → Export untranslated strings** opens `config/wynnchayuan/export`, which holds `captured.json`.
2. Look through the file and delete anything personal (other players' names, guild names, private chat).
3. **Settings → Data → How to submit** opens the [issue form](https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml); drag the file in. Or send it to **LyuChaCha** on Discord.

The mod **never sends game text by itself**. (What the mod does send for capes and leaves is listed under "Capes, the grey leaf, and what the mod sends" above.) The export already leaves out player names and guild,
party, shout and private chat, but the filter is heuristic, so check it before you send it.

For a wrong translation or term, open a [GitHub issue](https://github.com/LyuChaCha/WynnChaYuan/issues);
include the English text if you can. Chat lines can be copied with Copy chat.

## Helping translate

No programming needed: translations are JSON, and you fill in `dst` on the GitHub website.

- [CONTRIBUTING.en.md](CONTRIBUTING.en.md): workflow, placeholders, colour tags
- [GLOSSARY.md](GLOSSARY.md): term glossary (Traditional Chinese)
- [For translators](docs/for-translators.md): recent conventions and changes (Traditional Chinese)

To try your own translations in game: set Settings → Translation source to **Local**, then press Reload after editing.

## Building from source

```bash
gradle build
```

Requires JDK 21. Wynntils has no Maven coordinates: download the **Fabric** jar from
[Modrinth](https://modrinth.com/mod/wynntils/versions) into `libs/` first (the build stops and tells
you if it is missing). Corpus checks: `python tools/validate.py`.

## Sponsoring

**The translations are free, and will stay free.** Every translation and every translation feature is provided by the team at no cost. Nothing is locked behind payment.

Sponsoring is entirely voluntary:

- **Not sponsoring never lowers translation quality.** Everyone gets the same translations, updates and fixes - no fewer and no later.
- **Sponsors only get fun extras**: the cyan leaf before your name, a place on the list, and cosmetic features such as the upcoming animated capes.
- **Sponsors' feedback is given priority**: we look at and act on their reports and suggestions first.

To buy us a tea: **<https://ko-fi.com/lyuchacha>**

A one-off **20 USD** or **2 USD/month** or more puts you on the sponsor list: below, under
Settings → About / Credits, and as a cyan leaf before your name.

## Roles and badges

The colour of the leaf before your name depends on your role on the credits list. That list is the
"Translation team" section below; a maintainer adds you once it is confirmed, and needs your Minecraft ID.

| Role | Leaf | How to get it |
|---|---|---|
| Developer | Gold | Helps develop and maintain the code |
| Sponsor | Cyan | A one-off **20 USD** or **2 USD/month** or more ([Ko-fi](https://ko-fi.com/lyuchacha)) |
| Contributor | Green | Any one of the items below |
| Everyone else running the mod | Grey | — (comes with the mod; other players with the mod see it) |

**What counts for contributors** — whether it was used, not how much:

- **Translations for your own language**: filling in missing lines, or a `captured.json` you sent that was used to fill gaps, merged into main.
- **Correct fixes**: pointing out a mistranslation, a missing line or inconsistent wording that is confirmed and fixed.
- **Real help with the code**: a merged fix or feature, or a report that reproduces a problem and helps find the cause.

People on the list can change their leaf colour under Settings → Badge; with several roles you can pick
which leaves to show, at least one.

## Translation team

The same list is in game under **Settings → About / Credits**. To add someone, edit
[`credits.json`](src/main/resources/assets/wynnchayuan/credits.json).

<!-- credits:begin -->

<!-- Generated by tools/sync-credits.py from credits.json - do not edit by hand. -->

### Developers

| Name | Minecraft ID |
|---|---|
| LyuChaCha | `Green_teaTW` |

### Sponsors

| Name | Minecraft ID |
|---|---|
| ㄉ綠 | `MlyuL` |
| 蛇蛇 | `snakiesnake` |

### Contributors

| Name | Minecraft ID |
|---|---|
| suSCP | `SCP_Night_sky` |
| 隨意 | `brine7459` |
| Chicken_sky | `Chicken_sky` |
| Pure | `21_Pure` |
| 泥巴先生 | `MrMud8033112` |
| 幻影Joker | `NOT_Joker` |
| Pootato | `Pootato__` |
| N02sAyLa | `eric18960` |
| 鳥鳥 | `Smellybird_` |
| 98 | `Jackandmina98` |
| Jimmy | `0110jimmy` |
| 雪花 | `ThEsnowF` |
| Roy | `aaroye` |
| Chq | `Chqrish` |
| 邊緣安德 | `Enderchen2580` |
| AdoreFuSei | `AdoreFuSei` |
| anilken | `Pandik` |
| yool | `yool141` |
| 早安白月 | `ZLGX` |

### Data sources

| Name | Minecraft ID |
|---|---|
| Wynntils (item / ability CDN) | — |
| Wynncraft | — |

Translate one line and you are on this list. See [CONTRIBUTING.en.md](CONTRIBUTING.en.md).

<!-- credits:end -->

## Sources and licences

- Item and ability data: the public CDN used by [Wynntils](https://github.com/Wynntils/Wynntils)
- Quest and secret discovery lists: [Wynncraft Wiki](https://wynncraft.wiki.gg/) (CC BY-SA)
- CJK and Cyrillic glyphs in the dialogue box: [Fusion Pixel 10px](https://github.com/TakWolf/fusion-pixel-font) (SIL OFL 1.1,
  full text in `src/main/resources/assets/wynnchayuan/font/ofl-fusion.txt`). A line containing a glyph the font
  lacks stays in English instead of drawing boxes
- Resource-pack glyphs and layout belong to Wynncraft; this mod only displays them
- Code: [GNU AGPLv3 or later](LICENSE)
- Translations and data (the corpus under `src/main/resources/assets/wynnchayuan/translations/`, `GLOSSARY.md`, etc.): [CC BY-NC-SA 4.0](LICENSE-DATA).
  Content taken from the Wynncraft Wiki stays under its original CC BY-SA

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.
