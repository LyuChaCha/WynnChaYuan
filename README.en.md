<p align="center">
  <img src="docs/icon.png" width="160" alt="WynnChaYuan">
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
> **This is a beta.** The translations are mostly AI-generated and only partly proofread, so expect
> mistranslations and inconsistent terms. Report problems to **LyuChaCha** on Discord or on
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
the English ones. F6 can turn them on.

### More

- **Translations update themselves**: translations are downloaded from GitHub, so new ones arrive on your next launch, **no mod update needed**. Offline, the last cache or the copy in the jar is used
- **Market search**: search the trade market in your language; the English name is sent. Matches are listed as you type: ↑↓ to pick, Tab to fill
- **Copy chat**: lists recent chat messages; click one to copy it for a report (key unbound by default)
- **Panel screenshot**: **F9** copies the translation panel to the clipboard or saves it to a file (rebindable)
- **Adjustable panels**: boxes can be dragged; dialogue, choices and the tracker can be resized from the corner
- **Update notice**: says so once in chat when a new version is out, linking to GitHub Releases
- **Contributor tags**: people on the credits list get an extra line above their nameplate, visible only to others running the mod (can be turned off)

**Client-side only.** The server does not need it.

### Settings (F6)

| Tab | What's in it |
|---|---|
| Items | Item translation (separate panel / replace in place / off), translate item names, market search, panel screenshot |
| Panel | Follow the mouse or pin it, which side, gap, border colour, arrange the boxes |
| Dialogue | Quest dialogue, dialogue choices (separate box / replace in place / off), hold time, dialogue/tracker boxes |
| World & chat | Nameplates and floating text (with range and aim angle), chat messages, title text, copy chat |
| Data | Translation language, fallback language, interface language, translation source, reload, collect untranslated strings, collect GUI text, export untranslated strings, how to submit, debug dumps |

Hover a setting for an explanation.

## Languages and progress

| Language | Coverage |
|---|---|
| `zh_tw` Traditional Chinese | Main language, everything |
| `zh_cn` Simplified Chinese | Everything |
| `ja_jp` Japanese | Everything, quest dialogue included |
| `ru_ru` Russian | Everything, quest dialogue included |
| `ko_kr` Korean | Item tooltip labels and Major IDs |

Switch under **F6 → Data**, without changing the game's language or restarting:

- **Translation language**: which translations to show
- **Fallback language**: what to show where that language has nothing yet (another language, or the original)
- **Interface language**: the language of the F6 screens themselves (English included)

<!-- 進度:開始 -->
更新於 2026-09-16。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | ██████████ 95.4% | 41,958 / 44,003 |
| `zh_cn` 简体中文 | █████████░ 94.0% | 40,095 / 42,647 |
| `ja_jp` 日本語 | █████████░ 93.8% | 40,012 / 42,647 |
| `ru_ru` Русский | █████████░ 93.5% | 39,865 / 42,647 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 1.8% | 778 / 42,639 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
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
2. Put it in `mods/` and press **F6** in game.

## A line switches back to English halfway?

**That is normal.** It means the sentence is not in the corpus yet, so the mod shows the original.

Export it and send it to us as described in [How to help](#how-to-help). Once it is added, everyone
gets it on their next launch; no mod update needed.

## How to help

Quest dialogue and NPC nameplates have no official data source; they only arrive when players run
into them. **You do not have to translate anything**, just hand us the gaps you found:

1. **F6 → Data → Export untranslated strings** opens `config/wynnchayuan/export`, which holds `captured.json`.
2. Look through the file and delete anything personal (other players' names, guild names, private chat).
3. **F6 → Data → How to submit** opens the [issue form](https://github.com/LyuChaCha/WynnChaYuan/issues/new?template=corpus.yml); drag the file in. Or send it to **LyuChaCha** on Discord.

The mod **never sends anything by itself**. The export already leaves out player names and guild,
party, shout and private chat, but the filter is heuristic, so check it before you send it.

For a wrong translation or term, open a [GitHub issue](https://github.com/LyuChaCha/WynnChaYuan/issues);
include the English text if you can. Chat lines can be copied with Copy chat.

## Helping translate

No programming needed: translations are JSON, and you fill in `dst` on the GitHub website.

- [CONTRIBUTING.en.md](CONTRIBUTING.en.md): workflow, placeholders, colour tags
- [GLOSSARY.md](GLOSSARY.md): term glossary (Traditional Chinese)
- [For translators](docs/for-translators.md): recent conventions and changes (Traditional Chinese)

To try your own translations in game: set F6 → Translation source to **Local**, then press Reload after editing.

## Building from source

```bash
gradle build
```

Requires JDK 21. Wynntils has no Maven coordinates: download the **Fabric** jar from
[Modrinth](https://modrinth.com/mod/wynntils/versions) into `libs/` first (the build stops and tells
you if it is missing). Corpus checks: `python tools/validate.py`.

## Sponsoring

Free, and it will stay free. To buy us a tea: **<https://ko-fi.com/lyuchacha>**

**3 USD/month** or a one-off **10 USD** or more puts you on the sponsor list: below, under
F6 → About / Credits, and as an extra line above your nameplate. Sponsoring does not influence
translations, and there are no paid features.

## Translation team

The same list is in game under **F6 → About / Credits**. To add someone, edit
[`credits.json`](src/main/resources/assets/wynnchayuan/credits.json).

<!-- credits:begin -->

<!-- Generated by tools/sync-credits.py from credits.json - do not edit by hand. -->

### Developers

| Name | Minecraft ID |
|---|---|
| LyuChaCha | `Green_teaTW` |
| 芋圓YuYuan | `s103064` |

### Sponsors

| Name | Minecraft ID |
|---|---|
| LyuChaCha | `Green_teaTW` |
| ㄉ綠 | `MlyuL` |

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
- Code: [MIT](LICENSE)

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.
