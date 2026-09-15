<p align="center">
  <img src="docs/icon.png" width="160" alt="WynnChaYuan">
</p>

# WynnChaYuan

繁體中文：**[README.md](README.md)**

A multi-language translation mod for Wynncraft. **The original text is kept**; the
translation is shown beside it, or written in its place.

> [!IMPORTANT]
> **This is a beta.** The translations are mostly AI-generated and only partly proofread, so expect
> mistranslations and inconsistent terms. Report problems to **LyuChaCha** on Discord or on
> [GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues).

## What it translates

| Content | How it is shown |
|---|---|
| Item tooltips | A panel beside the tooltip, or written into the tooltip itself |
| Quest dialogue, dialogue choices | Written into **Wynncraft's own dialogue box** (frame, nameplate and portrait kept), or a separate box; set separately |
| NPC nameplates, floating text | A box while you look at one, or replaced in place; crafting stations and "Right-Click" prompts included |
| Quest tracker | Its own box; the heading says what is tracked (quest, world event, cave, raid…) |
| Ability trees | Nodes, descriptions and archetypes for all five classes |
| Major IDs | Name and full description |
| Lootruns, raids, dungeons | Missions, boons, beacons, aspects, gambits, end-of-run and loot panels, dungeon names |
| Discoveries, secret discoveries | Names, descriptions and stories |
| Chat messages | Server messages (quest completions, rewards, area transitions); replaced or original + translation. Player chat is never translated |
| Title text | The big title and subtitle in the middle of the screen |

Gear names stay in English by default: they are proper nouns, and the trade market and the wiki use
the English ones. F6 can turn them on.

## Other features

| Feature | What it does |
|---|---|
| Market search | Search the trade market in your language; the English name is sent. Matches are listed as you type: ↑↓ to pick, Tab to fill |
| Translations sync themselves | Translations come from GitHub, so fixes arrive on your next launch, **no new download**. Offline, the last cache or the copy in the jar is used |
| Corpus sharing | Lines without a translation are sent to the translation team; on by default (see [Installing it helps](#installing-it-helps)) |
| Copy chat | Lists recent chat messages; click one to copy it for a report (key unbound by default) |
| Panel screenshot | **F9** copies the translation panel to the clipboard or saves it to a file (rebindable) |
| Adjustable panels | Every box can be dragged; dialogue, choices and the tracker can be resized from the corner; the panel can follow the mouse or be pinned, with a custom border colour |
| Update notice | Says so once in chat when a new version is out; F6 → Changelog shows what changed |
| Contributor tags | People on the credits list get an extra line above their nameplate, visible only to others running the mod (can be turned off) |

**Client-side only.** The server does not need it.

## Settings (F6)

| Tab | What's in it |
|---|---|
| Items | Item translation (separate panel / replace in place / off), translate item names, market search, panel screenshot |
| Panel | Follow the mouse or pin it, which side, gap, border colour, arrange the boxes |
| Dialogue | Quest dialogue, dialogue choices (separate box / replace in place / off), hold time, dialogue/tracker boxes |
| World & chat | Nameplates and floating text (with range and aim angle), chat messages, title text, copy chat |
| Data | Translation language, fallback language, interface language, translation source, reload, collect untranslated strings, collect GUI text, share with the translation team, debug dumps |

Hover a setting for an explanation.

## Languages

| Language | Status |
|---|---|
| `zh_tw` Traditional Chinese | Main language, everything |
| `zh_cn` Simplified Chinese | Everything, on par with Traditional Chinese |
| `ja_jp` Japanese | Everything, on par with Traditional Chinese |
| `ru_ru` Russian | Interface, ability trees, nameplates, item lore; quest dialogue in progress |
| `ko_kr` Korean | Item tooltip labels and Major IDs only |

Switch under **F6 → Data**, without changing the game's language or restarting:

- **Translation language**: which translations to show
- **Fallback language**: what to show where that language has nothing yet (another language, or the original)
- **Interface language**: the language of the F6 screens themselves (English included)

## Translation progress

<!-- 進度:開始 -->
更新於 2026-09-15。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_cn` 简体中文 | █████████░ 93.7% | 39,854 / 42,537 |
| `zh_tw` 繁體中文 | █████████░ 93.7% | 40,893 / 43,650 |
| `ja_jp` 日本語 | █████████░ 93.5% | 39,771 / 42,537 |
| `ru_ru` Русский | ████░░░░░░ 40.8% | 17,348 / 42,537 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 1.5% | 621 / 42,529 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
<!-- 進度:結束 -->

## A line switches back to English halfway?

**That is normal.** It means the sentence is not in the corpus yet, so the mod shows the original.

Report it and it will be filled: **LyuChaCha** on Discord, or
[GitHub Issues](https://github.com/LyuChaCha/WynnChaYuan/issues). Once it is added, everyone gets it on
their next launch; no mod update needed. Include the English text if you can; chat lines can be
copied with Copy chat.

## Installing it helps

**F6 → Data → Share with the translation team is on by default.** Lines you run into that have no
translation are sent to the translation team, and once translated they show up for everyone. Quest
dialogue and NPC nameplates have no official data source; they only arrive when players run into
them. **You do not have to translate anything: playing with the mod installed already helps.**

| | |
|---|---|
| **Sent** | The game's own English text: quest dialogue, menus, item lore, NPC nameplates, server announcements |
| **Never sent** | Your account, UUID, coordinates, which world you are on; guild, party, shout and private chat (anything other people typed) |

Player names are removed by three independent filters (in the mod, in the collector, before anything
reaches the repository). The mod explains this in chat once, on your first launch.

To not share, turn it off and leave only **Collect untranslated strings** on: lines go to
`config/wynnchayuan/captured.json` for you to review and attach to an issue yourself.

## Install

| Requirement | Version |
|---|---|
| Minecraft | 1.21.11 |
| Loader | Fabric |
| Dependencies | [Wynntils](https://modrinth.com/mod/wynntils) 4.2+, Fabric API |

Put the jar in `mods/` and press **F6** in game.

Download: [GitHub Releases](https://github.com/LyuChaCha/WynnChaYuan/releases/latest) ·
[Modrinth](https://modrinth.com/mod/wynnchayuan) ·
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/wynnchayuan)

## Helping translate

No programming needed: translations are JSON, and you fill in `dst` on the GitHub website.

- [CONTRIBUTING.en.md](CONTRIBUTING.en.md): workflow, placeholders, colour tags
- [GLOSSARY.md](GLOSSARY.md): term glossary (Traditional Chinese)
- [For translators](docs/for-translators.md): recent conventions and changes (Traditional Chinese)

To try your own translations in game: set F6 → Translation source to **Local**, then press Reload after editing.

## Building

```bash
gradle build
```

Wynntils has no Maven coordinates: download the **Fabric** jar from
[Modrinth](https://modrinth.com/mod/wynntils/versions) into `libs/` first (the build stops and tells
you if it is missing). Corpus checks: `python tools/validate.py`.

## Sponsoring

Free, and it will stay free. To buy us a tea: **<https://ko-fi.com/lyuchacha>**

| | |
|---|---|
| **3 USD/month** or more | Listed as a sponsor |
| **10 USD** one-off or more | Listed as a sponsor |

The list appears below, under F6 → About / Credits, and as an extra line above your nameplate.
Sponsoring does not influence translations, and there are no paid features.

## Team

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

## Data sources and licences

- Item and ability data: the public CDN used by [Wynntils](https://github.com/Wynntils/Wynntils)
- Quest and secret discovery lists: [Wynncraft Wiki](https://wynncraft.wiki.gg/) (CC BY-SA)
- CJK and Cyrillic glyphs in the dialogue box: [Fusion Pixel 10px](https://github.com/TakWolf/fusion-pixel-font) (SIL OFL 1.1,
  full text in `src/main/resources/assets/wynnchayuan/font/ofl-fusion.txt`). A line containing a glyph the font
  lacks stays in English instead of drawing boxes
- Resource-pack glyphs and layout belong to Wynncraft; this mod only displays them
- Code: [MIT](LICENSE)

**Not affiliated with Wynncraft or the Wynntils team.** A community translation project.
