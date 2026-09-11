<p align="center">
  <img src="docs/icon.png" width="160" alt="WynnChaYuan">
</p>

# WynnChaYuan

A translation mod for Wynncraft. **It does not replace the original text** —
translations appear in a separate panel next to it, and the game's own screen is
left untouched.

繁體中文說明請見 [README.md](README.md)。

> [!IMPORTANT]
> **This is a beta (0.1.0).** Features and translations are still being worked on. If you
> run into anything, find **LyuChaCha** on Discord — all reports are welcome.
>
> **The translations are mostly AI-generated; only some have been proofread by a
> human.** Expect mistranslations, unnatural phrasing, and inconsistent proper
> nouns. If that bothers you, please hold off on using it for now.


## Why not just swap the text

Wynncraft is a multiplayer game. If your screen shows only your own language, you
can no longer follow a conversation about "go see the Blacksmith" — everyone else
knows the English names. On top of that, Wynncraft's layout leans heavily on
resource-pack glyphs and invisible alignment characters, so a naive replacement
breaks the display.

So the **default** is: keep the original, show the translation beside it.

If you don't need the English, **F6 → Item translation** can be switched to
**replace in place**, writing the translation into the tooltip itself.
Both modes use the same per-segment replacement, so glyphs, colours and column
alignment stay intact either way.

**Quest dialogue** has its own three-way setting: a separate box (default),
**replace in place** — which writes the translation into Wynncraft's own dialogue
box, keeping its frame, nameplate and portrait — or off.

## What it does

| Content | How |
|---|---|
| Item tooltips | A panel beside the tooltip, or written into the tooltip itself |
| NPC dialogue | Translated **inside Wynncraft's own dialogue box** — frame, nameplate and portrait kept |
| Dialogue choices | The same three modes as the dialogue, set separately |
| NPC nameplates, floating text | Off / a box while you look at one / replaced in place (switchable). Crafting stations and the "Right-Click with an Empty Hand" prompts count too |
| Quest tracker | A translated box on the left |
| Ability tree | Every node, description and archetype, all five classes |
| Major IDs | Name and full description |
| Dungeons and raids | Aspects, gambits, the loot panel |
| Lootruns | Missions, boons, beacons, the end-of-run panel |
| Discoveries and secret discoveries | Names and descriptions |
| System messages | Quest completions, reward lists, area transitions — the chat ones |
| Title text | The big text in the middle of the screen |

### Beyond translating

| Feature | What it does |
|---|---|
| **Market search in your language** | Type the translated name in the trade market and it is turned back into the English one before the search is sent |
| **Translations update themselves** | Translations are not baked into the jar — once a change is merged everyone gets it on their next launch, **no new download** |
| **Corpus sharing** | Lines the mod could not translate are sent back to the translation team so they can be translated for everyone (can be turned off) |
| **Copy chat** | Lists recent chat messages; click one to copy (key unbound by default) |
| **Screenshots** | **F9** captures the translation panel — clipboard or file (rebindable) |
| **Contributor tags** | People who have translated get an extra line above their nameplate, visible only to others running this mod (can be turned off) |

### Settings (F6)

Five tabs; hover an entry for an explanation.

| Tab | What's in it |
|---|---|
| **Items** | Tooltip mode, whether to translate gear names, market search, screenshots |
| **Panel** | Follow the mouse or pin it, which side, gap, border colour |
| **Dialogue** | Quest dialogue, dialogue choices, hold time, the dialogue/tracker boxes |
| **World & chat** | Nameplates and floating text (with range and aim angle), chat, title text, copy chat |
| **Data** | Translation source, reload, collect untranslated strings, collect GUI text, share with the translation team, debug dumps |

All four boxes can be **dragged into position**, and are shown together while you
arrange them so you can tell whether they overlap.

**Client-side only.** The server does not need it and cannot tell you are using it.

## Install

Requires [Wynntils](https://modrinth.com/mod/wynntils) 4.2+ and Fabric API.

Drop the jar into `mods/` and press **F6** in game for the settings.
On first launch the translation working files are created under
`config/wynnchayuan/translations/`.

## Languages

Seven languages have a folder. Only Traditional Chinese has content so far —
the rest are empty skeletons with the source strings in place, waiting for a
translator.

A language with no translations at all is **not shipped in the jar** and does not
appear in the game's language list; it joins automatically once it has its first
line.

See [docs/PROGRESS.md](docs/PROGRESS.md) for **which files each language still
needs**.

## Progress

### How much of the collected corpus is translated

<!-- 進度:開始 -->
更新於 2026-09-11。

| 語言 | 進度 | 已翻 / 總數 |
|---|---|---:|
| `zh_tw` 繁體中文 | ██████████ 96.7% | 33,402 / 34,546 |
| `zh_cn` 简体中文 | █░░░░░░░░░ 8.0% | 2,240 / 28,162 |
| `de_de` Deutsch | ░░░░░░░░░░ 0.0% | 0 / 28,050 |
| `es_es` Español | ░░░░░░░░░░ 0.0% | 0 / 28,050 |
| `fr_fr` Français | ░░░░░░░░░░ 0.0% | 0 / 28,050 |
| `ja_jp` 日本語 | ░░░░░░░░░░ 0.0% | 0 / 28,050 |
| `ko_kr` 한국어 | ░░░░░░░░░░ 0.0% | 0 / 28,050 |
| `ru_ru` Русский | ░░░░░░░░░░ 0.0% | 0 / 28,050 |

每一種語言**還缺哪些檔案**見 [docs/PROGRESS.md](docs/PROGRESS.md)。<br>Per-language breakdown: [docs/PROGRESS.md](docs/PROGRESS.md).
<!-- 進度:結束 -->

### How much of the whole game has been collected

The denominator above is **the lines we have**. A line nobody has ever run into
sits in neither the numerator nor the denominator — so that percentage is not
"this much left to go".

This table answers the other half:

<!-- 涵蓋率:開始 -->
| Category | Collected | Count | Where the denominator comes from |
|---|---|---:|---|
| Quest dialogue | ██████████ | 157 / 157 quests | Official quest list (wiki) |
| Secret discovery stories | ░░░░░░░░░░ | 1 / 121 discoveries | Official secret discovery list (wiki) |
| Gear lore | ██████████ | all | Official CDN, downloaded wholesale |
| Ingredients, materials, tomes, aspects | ██████████ | all | Official CDN, downloaded wholesale |
| Ability trees | ██████████ | all | Official CDN, downloaded wholesale |
| NPC nameplates, menus, system messages | — | 1,881 + 1,037 collected | **No official list** - only what players run into |

Quest dialogue, NPC nameplates and menu text **have no public data source** - not in the Wynncraft API, not on Wynntils' CDN. They only arrive when a player actually runs into them in game, so the last row is an honest blank:

> We know how much we **have**. We do not know how much there **is**.
<!-- 涵蓋率:結束 -->

## Installing the mod helps finish it

Quest dialogue and NPC names **have no official data source**. There is no file to
scrape — somebody has to walk up to that NPC in game.

So the mod records the lines it could not translate and, **by default**, sends
them back to the translation team (F6 → *Share with the translation team* turns
this off). A hundred people each playing their own way add up to the whole game —
**you do not have to translate anything; just playing with it on already helps.**

What is sent:

| | |
|---|---|
| **Sent** | The game's own English text: quest dialogue, menus, item lore, NPC nameplates, server announcements |
| **Never sent** | Your account, UUID, coordinates, which world you are on; guild, party, shout and private chat — **anything other people typed** |

There are three independent personal-data filters — one in the mod before sending,
one in the collector, one before anything reaches the repository — each with its
own tests. The mod explains this in chat once, on your first launch, and never
again.

If you would rather not share, you can turn sharing off and leave only **Collect
untranslated strings** on: the lines go to `config/wynnchayuan/captured.json` for
you to read through and attach to a
[GitHub issue](https://github.com/LyuChaCha/WynnChaYuan/issues) yourself.

## Helping translate

Everything lives in JSON under
`src/main/resources/assets/wynnchayuan/translations/<language>/`.
Edit the `dst` fields; leave `src` alone.

```json
"Dwelling Walls#003": {
  "src": "Finally, the journal. It's out here somewhere, I assume.",
  "dst": "",
  "role": "desc",
  "kind": "dialogue"
}
```

### Placeholders

| Placeholder | Means | Rule |
|---|---|---|
| `{#}` | A resource-pack glyph (element icon, item symbol) | Keep the same count |
| `{~}` | A number the game fills in | Keep the same count; `{~1}`–`{~9}` pick a specific one when your word order differs |
| `{p}` `{u}` | Place name / player name | Keep exactly one of each |

**Getting the count wrong makes the whole line silently fall back to English.**
It looks identical to "not translated yet", so nobody notices. `tools/validate.py`
catches this and runs on every pull request.

### Colour placeholders

Colour is normally recovered by matching the translated text against the coloured
run in the original. That works while a term stays in English and breaks the moment
it doesn't — the run falls back to the base colour. These go **in `dst` only**;
putting one in `src` makes the line unmatchable forever.

| Syntax | Means |
|---|---|
| `{c1}`–`{c9}` | The Nth style of the original, ordered by first appearance |
| `{c:#FF55FF}` | An explicit hex colour |
| `{c:gold}` | An explicit vanilla colour name (the 16 Minecraft ones) |
| `{/}` | End the span; back to whatever that part was |

```json
"src": "[Cave Completed]\nGrook's Nest",
"dst": "{c1}[Cave Completed]{/}\n{c2}Grook's Nest{/}"
```

A span runs until `{/}` or the next `{cN}`, and **may cross line breaks** — a wrapped
sentence keeps its colour onto the next line. Omitting `{/}` colours through to the end
of that entry and never reaches another one. An out-of-range index is treated as not
written, so the line falls back to matching rather than breaking.

### Place names stay in English

`Detlas`, `Ragni`, `Nivla Woods` — players coordinate with each other using these.
The validator warns when a place name disappears from a translation.

### Starting a new language

```bash
python tools/new-language.py ko_kr
```

Use Minecraft's own language codes (`ja_jp`, not `jp`) — the mod picks a language
by matching the game's setting, so a wrong code is never selected.

The tool copies the structure, blanks every `dst`, and updates the language list.
It deliberately does **not** machine-translate from another language: a converted
file reads as "already done" in the progress table while nobody has actually read
a word of it.

## Building

```bash
./gradlew build
```

Wynntils has no Maven coordinates, so download the Fabric jar from
[Modrinth](https://modrinth.com/mod/wynntils/versions) into `libs/` first.
The build tells you if it is missing.

```bash
./gradlew check          # 19 test suites
python tools/validate.py # corpus checks
```

## Sponsoring

This project is free and will stay free. If you would like to buy us a tea:

**<https://ko-fi.com/lyuchacha>**

| | |
|---|---|
| **3 USD/month** or more | Listed in the sponsor credits |
| **10 USD** one-off or more | Listed in the sponsor credits |

The list appears in the [README](README.md), in game under **F6 → About /
Contributors**, and as an extra line above your nameplate.

Sponsoring does not influence what gets translated, and there are no paid
features.

## Versioning

Beta versions start at `0.1.0`:

| | |
|---|---|
| Major or notable update | `0.1.0` → `0.1.1` |
| Bug fix, small change, test build | `0.1.0` → `0.1.0_1` |

**Translation-only updates are not released** — translations sync themselves from
GitHub, so there is nothing to download.

## Licence

See [LICENSE](LICENSE). Wynncraft content belongs to the Wynncraft team;
this mod only ships translations of it.

CJK glyphs inside the dialogue box use [Fusion Pixel 10px](https://github.com/TakWolf/fusion-pixel-font)
(proportional), licensed under SIL OFL 1.1 — full text in
`assets/wynnchayuan/font/OFL-fusion.txt`. Its cap height is exactly 7px, matching
the Latin caps in Wynncraft's own dialogue font.
Fonts are shipped **per language**, since the same codepoint is drawn differently
across regions; only Traditional Chinese (`zh_tw`) is bundled today. No pixel font
covers every ideograph, so a line whose translation contains a glyph the font
lacks is left in English rather than drawn as boxes.
