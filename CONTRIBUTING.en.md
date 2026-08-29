# Translating WynnChaYuan

繁體中文版請見 [CONTRIBUTING.md](CONTRIBUTING.md)。

You don't need to know Java, Git or Minecraft modding. Everything a translator
touches is JSON, and you can edit it in the GitHub web interface.

## The quickest way: edit on GitHub

1. Open [docs/PROGRESS.md](docs/PROGRESS.md) and find your language.
2. Pick a file that still has work left.
3. Open it, press the pencil icon, fill in the `dst` fields.
4. "Commit changes" → "Create a new branch" → open a pull request.

The checks run automatically on your pull request. If something is mechanically
wrong you get a comment saying exactly which line and why.

## Where the files are

```
src/main/resources/assets/wynnchayuan/translations/<language>/
```

Use your own language folder — `de_de/`, `es_es/`, `fr_fr/`, `ja_jp/`, `ko_kr/`,
`ru_ru/`, `zh_cn/`, `zh_tw/`. **Do not edit another language's folder**, and do not
put files directly under `translations/` — that was the layout before languages
were split, and the checks will tell you to move them.

## What you edit

Fill in `dst`. Never touch `src` — that is what the mod matches against the game,
and changing it makes the line stop working.

```json
"Dwelling Walls#003": {
  "src": "Finally, the journal. It's out here somewhere, I assume.",
  "dst": "Endlich, das Tagebuch. Es muss hier irgendwo sein, nehme ich an.",
  "role": "desc",
  "kind": "dialogue"
}
```

Some files are flat instead — the English **is** the key:

```json
"Combat Level": "Kampfstufe",
```

## Placeholders — keep them, move them freely

| Placeholder | Means | Rule |
|---|---|---|
| `{#}` | A resource-pack glyph (element icon, item symbol) | Same count as `src` |
| `{~}` | A number the game fills in | Same count as `src` |
| `{p}` | A place name | Exactly one, same as `src` |
| `{u}` | The player's name | Exactly one, same as `src` |

You may reorder them to suit your grammar. When a line has more than one `{~}`
and your word order differs from English, use the numbered form to say which is
which:

```
src: "- {~} Rows ({~}stx Total)"
dst: "- {~1} Reihen (insgesamt {~2} Stapel)"
```

**Why this matters more than it looks:** if the counts don't match, the mod gives
up on that line and shows the English. On screen that is indistinguishable from
"not translated yet", so the mistake can sit there for months. The automated
check exists for exactly this.

## When the colour comes out wrong: `{c1}` `{c2}` `{/}`

Colour is normally **guessed**: the mod takes each coloured run of the original and
looks for the same literal text in your translation. That works for terms you keep
in the original — ability names, place names — and fails for everything you actually
translate, so the run falls back to the base colour. A line with a single colour can
still be recovered by position; **a line mixing two or three colours cannot**.

When the guess can't work, you are the only one who knows the answer, so write it:

| Syntax | Means |
|---|---|
| `{c1}`–`{c9}` | The Nth style of the original, ordered by first appearance |
| `{c:#FF55FF}` | An explicit hex colour |
| `{c:gold}` | An explicit vanilla colour name (the 16 Minecraft ones) |
| `{/}` | End the span; back to whatever that part was |

```json
"src": "[Cave Completed]\nGrook's Nest\n- Rewards:",
"dst": "{c1}[Cave Completed]{/}\n{c2}Grook's Nest{/}\n{c3}- {c4}Rewards:{/}"
```

`{cN}` moves the whole **style**, not just the hue — bold, underline and Wynncraft's
non-vanilla colour codes come along, so the line follows the game if its palette
ever changes.

**To see which number is which colour**, read the "available colours" section of
`config/wynnchayuan/majorid-debug.txt` (written while diagnostics are on, F6):

```
=== available colours 1 ===
  dst: [Cave Completed]
    {c1}  #55FF55               src: "[Cave Completed]"
    {c2}  #FFFFFF bold          src: "Grook's Nest"
    {c3}  #FF55FF               src: "-"
```

Three things to watch:

- These go **in `dst` only**. A colour placeholder in `src` makes that key
  unmatchable forever, because the game never sends those characters.
- A span runs until `{/}`, the next `{cN}`, or **the end of that line**. Forgetting
  `{/}` affects that line only; it never bleeds into the rest of the block.
- An out-of-range index is treated as **not written** — the span falls back to
  guessing. A wrong colour is survivable; a line that refuses to render is not.

## Place names stay in the original

`Detlas`, `Ragni`, `Nivla Woods`, `Corkus` — players use these to tell each other
where to go, in every language. Translating them breaks that.

The checker warns when a place name in `src` is missing from `dst`.

## Item and gear names

Gear and weapon names are left in English by default (`translateItemNames` is off),
because they are how items are looked up on the wiki and traded. They are not
counted in the progress numbers. Translating them is optional.

## Three layers of checking

**1. The machine** — runs on every pull request. It only catches mechanical
errors: placeholder counts, resource-pack glyphs pasted into a translation,
invalid `§` codes, place names that vanished, a translation much longer than the
original. It says nothing about whether the translation is *good*.

You can run it yourself:

```bash
python tools/validate.py
```

**2. The game** — build the mod, drop it in, and look at the line in place.
Column alignment and colour are things only the screen can tell you.
`F8` saves a picture of the translation panel to `screenshots/wynnchayuan/`,
which is a good thing to paste into a pull request.

**3. A human** — a maintainer reads it. Machines cannot tell "correct" from
"natural".

## Conventions

- Match the register of the original. Wynncraft's NPCs have distinct voices —
  a pirate, an old soldier, a nervous shopkeeper — and flattening them all into
  neutral prose loses most of the writing.
- Keep it short. The game's boxes have fixed widths; a translation twice as long
  as the original will wrap badly or overflow.
- Don't invent content. If the source line is broken (a few are — some were
  scraped from the wiki with pieces missing), leave `dst` empty and say so in the
  pull request.

## Starting a new language

```bash
python tools/new-language.py ko_kr
```

Use Minecraft's own language codes: `ja_jp` not `jp`, `ru_ru` not `ru`.
The mod selects a language by matching the game's setting, so a wrong code will
simply never be used.

Ask a maintainer if you'd rather not run it yourself.

## Reporting missing text

If you find game text that isn't in the corpus at all, turn on collection in the
F6 settings, walk past it, and open an issue with the file it produced under
`config/wynnchayuan/`.
