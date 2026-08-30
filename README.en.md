<p align="center">
  <img src="docs/icon.png" width="160" alt="WynnChaYuan">
</p>

# WynnChaYuan

A translation mod for Wynncraft. **It does not replace the original text** —
translations appear in a separate panel next to it, and the game's own screen is
left untouched.

繁體中文說明請見 [README.md](README.md)。

## Why not just swap the text

Wynncraft is a multiplayer game. If your screen shows only your own language, you
can no longer follow a conversation about "go see the Blacksmith" — everyone else
knows the English names. On top of that, Wynncraft's layout leans heavily on
resource-pack glyphs and invisible alignment characters, so a naive replacement
breaks the display.

So the **default** is: keep the original, show the translation beside it.

| Content | How it is shown |
|---|---|
| Item tooltips | A separate translation panel next to the tooltip |
| NPC nameplates, floating text | A small box under the crosshair while you look at them |
| Quest dialogue | A small box at the bottom of the screen |
| Quest tracker | A small box on the left |

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
| Ability tree | Every node, description and archetype |
| System messages | Quest completions, reward lists, area transitions — the chat ones |
| Title text | The big text in the middle of the screen |
| Copy chat | Lists recent chat messages; click one to copy (key unbound by default) |
| Screenshots | **F9** captures the translation panel — clipboard or file (rebindable) |

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

See the [README](README.md) for the current progress table, and
[docs/PROGRESS.md](docs/PROGRESS.md) for **which files each language still
needs**.

## Installing the mod helps finish it

Quest dialogue and NPC names **have no official data source**. There is no file to
scrape — somebody has to walk up to that NPC in game.

So the mod can collect the lines it could not translate. Turn on **Collect
untranslated strings** in F6, play normally, and it writes them to
`config/wynnchayuan/captured.json`. Attach that file to a
[GitHub issue](https://github.com/LyuChaCha/WynnChaYuan/issues) and those lines
become translatable for everyone.

> **Please glance at the file before attaching it.** The mod filters out player
> names, friend lists and coordinates, but Wynncraft has a lot of notification
> formats and something may slip through. Delete any line with somebody's name in
> it — and tell us, so the filter can be fixed.

You do not have to translate anything to help.

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
