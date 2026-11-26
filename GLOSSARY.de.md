# Deutsches Glossar (de_de)

> This file says **how to translate**. For **what things are**, see [World and story](docs/world-and-story.md).
> It is the de_de glossary of its own. It is **not** a conversion of `GLOSSARY.md` (zh_tw) or of the Spanish files:
> the Spanish (es_es) corpus is our reviewed reference for *what* gets translated and *what stays English*,
> but the wording here is German.

Every recurring game term goes through this table. When a word is not sure, look it up here and **do not invent a second one**.
**To change a term, change it here first**, then change the translation files that use it, so everyone moves together.
Use Ctrl+F.

## General rules

- **The player is "du"**, lower case, informal, everywhere: tooltips, dialogue, chat, UI (`Du erfüllst die Voraussetzungen nicht`, `Hol sie dir im Mitgliedermenü ab.`), exactly as the F6 settings screen does. Never *Sie* towards the player. Between NPCs, peers and friends say *du*; a character speaking to a superior, officer, noble, royal or cleric (or to a stranger in a formal setting) uses the fantasy-register *Ihr/Euch* (`Eure Majestät`, `Eure Exzellenz`), never *Sie*. Keep each pair consistent inside a quest.
- **Short.** Most strings sit in tooltips, item lists and a dialogue box of fixed width, and German runs long. Prefer the short natural word and the usual abbreviations (`Max.`, `Min.`, `Sek.` only where space is really tight).
- **Placeholders stay exactly as they are**: `{#}` `{~}` `{~1}` `{p}` `{u}`, same number of line breaks. `{cN}…{/}` colour markers are optional but welcome. If you reorder a sentence that has two `{~}` or two `{#}`, number them (`{~1}`, `{~2}`, `{#1}`, `{#2}`) so the values do not land in the wrong slots. `{p}` and `{u}` have no numbers: they are filled in order, so keep two names in their English order.
- **Do not inflect a placeholder.** `{p}` and `{u}` are filled with English names, so never glue a genitive `-s` to them: write `Monstertotem von {u}`, `Die Kanalisation von {p}`, not `{u}s Monstertotem`.
- **Never write a capital "À" or "Á"** (the game font hijacks those two glyphs for screen effects). No non-breaking spaces. No `„ “` or `« »` quotes in game text: use plain `"..."` as the English does. Avoid symbols the game never uses (full-width brackets, `⋯`, `～`); use three periods `...` for an ellipsis (the dialogue font draws `…` as centred dots) and plain `-` / `—`. Umlauts and `ß` are fine.
- **Numbers and units stay as the source writes them**: keep `{~}%` without an added space, keep the source's decimal point (the number comes from the game). Write `{~} Sekunden`, `{~} Blöcke` in prose; the short timer forms are in `scoped/label.json`.
- **Capitalisation**: German capitalises nouns as usual; everything else follows normal German, not English title case (`Gegner verlangsamen`, `Nicht handelbarer Gegenstand`, `Ergebnisse sortieren`). The first word of a label is capitalised.
- **Compounds**: German words are joined (`Feuerschaden`, `Zauberschaden`, `Smaragdbeutel`). Join an English or made-up name with a hyphen: `Lootrun-Lager`, `Raid-Altar`, `Kampfmönch-Archetyp`, `Qira-Bienenstock`.
- **Follow es_es on what stays English.** If the Spanish keeps a name in English (person, place, item, monster with a made-up name), keep it in English too. Place names are handled by `{p}` automatically.
- **Spell names**: in the stat lines of the ability tree (`ability-labels.json`, e.g. `Ice Snake Cost:`) the base spell name **stays English** (`Kosten für Ice Snake:`, `Dauer von Heal:`), exactly like es_es. In item stat lines (`ui-labels.json`, e.g. `Ice Snake Cost`) it is **translated** (`Kosten für Eisschlange`). The two files are matched against different screens; do not unify them.
- **The F6 screen** (`lang/de_de.json`) uses the same terms as the game text (`Gegenstand`, `Inhaltsbuch`, `Foliant`).

## Classes

Class names are always "**main class/skin**", with a plain slash and no spaces.

| English | de_de | Notes |
|---|---|---|
| Mage/Dark Wizard | Magier/Dunkler Zauberer | |
| Archer/Hunter | Bogenschütze/Jäger | |
| Warrior/Knight | Krieger/Ritter | |
| Assassin/Ninja | Assassine/Ninja | |
| Shaman/Skyseer | Schamane/Himmelsseher | |
| Class | Klasse | |
| Class Type | Klassentyp | |

### Archetypes

Label form: `X-Archetyp`, `X-Archetyp:`, `Min. X-Archetyp:` (the name stays in its base form: `Gefallener-Archetyp`).

| English | de_de | Notes |
|---|---|---|
| Archetype | Archetyp | |
| Light Bender / Lightbender | Lichtbändiger | Mage |
| Arcanist | Arkanist | Mage |
| Riftwalker | Spaltwandler | Mage. "Spalt" (rift) is reserved for this family: Riftspawn = Spaltbrut |
| Fallen | Gefallener | Warrior |
| Battle Monk | Kampfmönch | Warrior |
| Paladin | Paladin | Warrior |
| Sharpshooter | Scharfschütze | Archer. Not "Elite": Elite is a Lootrun word |
| Trapper | Fallensteller | Archer |
| Boltslinger | Bolzenschleuderer | Archer |
| Shadestepper | Schattenschreiter | Assassin |
| Trickster | Trickser | Assassin |
| Acrobat | Akrobat | Assassin |
| Summoner | Beschwörer | Shaman |
| Ritualist | Ritualist | Shaman |
| Acolyte | Akolyth | Shaman |

### Base spells

| Order | Mage | Archer | Warrior | Assassin | Shaman |
|---|---|---|---|---|---|
| 1st | Heilung (Heal) | Pfeilsturm (Arrow Storm) | Hieb (Bash) | Wirbelangriff (Spin Attack) | Totem |
| 2nd | Teleport (Teleport) | Flucht (Escape) | Ansturm (Charge) | Verschwinden (Vanish) | Zerren (Haul) |
| 3rd | Meteor (Meteor) | Bombenpfeil (Arrow Bomb / Bomb Arrow) | Aufwärtshaken (Uppercut) | Mehrfachschlag (Multihit) | Aura |
| 4th | Eisschlange (Ice Snake) | Pfeilschild (Arrow Shield) | Kriegsschrei (War Scream) | Rauchbombe (Smoke Bomb) | Entwurzeln (Uproot) |

- Ordinals: `1st/2nd/3rd/4th Spell` = `1./2./3./4. Zauber` (`Kosten des 1. Zaubers`).
- `Charge` (warrior spell) is **Ansturm**, so it never collides with `Charges` = **Ladungen** (uses of an ability/item).
- `Spell` is always **Zauber** (der), never "Magie" or "Spruch". `Ability` is **Fähigkeit**.
- Remember the rule above: in `ability-labels.json` these names stay English.

## Weapons and gear

| English | de_de | Notes |
|---|---|---|
| Weapon | Waffe | |
| Armour / Armor | Rüstung | |
| Accessory | Schmuckstück (pl. Schmuck / Schmuckstücke) | Rings, bracelets, necklaces; made by Juwelierkunst |
| Gear / Equipment | Ausrüstung | |
| Item | Gegenstand (pl. Gegenstände) | As in `Nicht handelbarer Gegenstand`, `Gegenstände aufdecken`. |
| Spear | Speer | Warrior |
| Dagger | Dolch | Assassin |
| Bow | Bogen | Archer |
| Wand | Zauberstab | Mage. Not "Stab" alone |
| Relik | Relik (das, pl. Reliks) | Shaman. Invented word, stays as is (es too) |
| Helmet | Helm | |
| Chestplate | Brustpanzer | Minecraft German |
| Leggings | Hose | Minecraft German |
| Boots | Stiefel | |
| Ring | Ring | |
| Bracelet | Armband | |
| Necklace | Halskette | |
| Unidentified X | Nicht identifizierte(r/s) X | `Nicht identifizierter Helm`, `Nicht identifizierte Waffe` |
| Untradable Item | Nicht handelbarer Gegenstand | |
| Quest Item | Questgegenstand | |
| Durability | Haltbarkeit | |
| Powder | Pulver | `Donnerpulver II`. Powder Specials = Pulver-Spezialeffekte |
| Powder Slots / Sockets | Pulverplätze | |
| Ingredient | Zutat | Crafting ingredient (mob drops etc.). `Zutatenbeutel` |
| Material | Material | Gathered raw material. Keep the two apart |
| Ingredient Pouch | Zutatenbeutel | |
| Tome / Mastery Tome | Foliant / Meisterschaftsfoliant | |
| Charm | Talisman (pl. Talismane) | |
| Aspect | Aspekt | |
| Crafted Item | Hergestellter Gegenstand | `Crafted by` = Hergestellt von (but crafted item *names* stay as the player wrote them) |
| Mount | Reittier | `Reittiergehege` |
| Pet | Haustier | |
| Potion / Scroll | Trank / Schriftrolle | `Heiltrank`, `Teleportationsschriftrolle` |
| Key / Fragment | Schlüssel / Fragment | Dungeon keys and fragments |

### Item tiers (rarity)

Adjective before the noun, declined as usual: `Mythischer Gegenstand`, `Legendäre Waffe`, `Mythischer Aspekt`.

| English | de_de | Notes |
|---|---|---|
| Normal | Normal | |
| Unique | Einzigartig | |
| Rare | Selten | |
| Legendary | Legendär | |
| Fabled | Sagenhaft | |
| Mythic | Mythisch | Gear names follow es: where es translates a name (with a category word) German does too (`Halskette des Eintrachtsprotokolls`); where es leaves it blank (most Mythics) it stays English. The in-game item-name setting can show the original |
| Set | Set | `Set-Gegenstand`, `Setbonus` |
| Crafted | Hergestellt | |
| Shiny | Shiny | Item prefix and tracker system stay English, as es |
| tier (as in `{~} tier`, Tier IV) | Rang | `Rang {~}`, `Pulver Rang IV`. Not "Stufe", which is Level |

## Elements

| English | de_de | Notes |
|---|---|---|
| Earth | Erde | Compounds: `Erdschaden`, `Erdverteidigung`, but `Erde-Hauptangriffsschaden` |
| Thunder | Donner | `Donnerschaden` |
| Water | Wasser | `Wasserschaden` |
| Fire | Feuer | `Feuerschaden` |
| Air | Luft | `Luftschaden` |
| Neutral | Neutral | `Neutraler Schaden` |
| Elemental | Elementar- | `Elementarschaden`, `Elementar-Hauptangriffsschaden` |
| element / elements / all elements | Element / Elemente / alle Elemente | |

## Skill points (attributes)

`Skill Point` and `Ability Point` are two different systems; keep them apart.

| English | de_de | Notes |
|---|---|---|
| Skill Point | Attributpunkt | The five stats below |
| Ability Point | Fähigkeitspunkt | Spent in the ability tree |
| Ability Tree | Fähigkeitenbaum | One ui-labels line still says "Fähigkeitsbaum"; align it to this |
| Ability | Fähigkeit | |
| Strength | Stärke | |
| Dexterity | Geschick | |
| Intelligence | Intelligenz | |
| Defence / Defense | Verteidigung | |
| Agility | Agilität | |
| Min. Strength (etc.) | Min. Stärke | Requirement lines |
| Unassigned Skill Points | Nicht verteilte Attributpunkte | |
| Unused Ability Points | Ungenutzte Fähigkeitspunkte | |

## Stat lines (identifications)

Damage stats are one compound: **[Element] + [Hauptangriffs- / Zauber-] + schaden**. With "Main Attack" and an element, join with a hyphen.

| Piece | de_de | Example |
|---|---|---|
| Damage | Schaden | |
| Main Attack | Hauptangriff | `Main Attack Damage` = Hauptangriffsschaden |
| Spell | Zauber | `Spell Damage` = Zauberschaden |
| Element X | X- | `Fire Spell Damage` = Feuerzauberschaden; `Fire Main Attack Damage` = Feuer-Hauptangriffsschaden |
| Elemental (combined) | Elementar- | `Elemental Spell Damage` = Elementarzauberschaden |
| `%` (item line) | ` %` | `Feuerschaden %` |
| `Raw` (item line) | ` (fester Wert)` | `Feuerschaden (fester Wert)` |
| `X:%` (ability tree line) | `X (%):` | `Air Damage:%` = Luftschaden (%): |
| Defence (elemental) | X-verteidigung / Elementarverteidigung | `Feuerverteidigung` |

| English | de_de | Notes |
|---|---|---|
| Health | Leben (das) | Not "Gesundheit". `Max. Leben` |
| Health Regen | Lebensregeneration | |
| Healing Efficiency | Heilungseffizienz | |
| Overhealth | Zusatzleben | Extra health above the maximum |
| Mana | Mana (das) | |
| Max Mana | Max. Mana | |
| Mana Regen | Manaregeneration | |
| Mana Cost | Manakosten | |
| Life Steal | Lebensraub | |
| Mana Steal | Manaraub | |
| Critical Damage | Kritischer Schaden | |
| Attack Speed | Angriffsgeschwindigkeit | |
| DPS | Schaden pro Sekunde | Item line. The ability tree keeps "DPS" (`deines DPS`) |
| Main Attack Range | Hauptangriffsreichweite | |
| Walk Speed | Laufgeschwindigkeit | |
| Sprint / Sprint Regen | Sprint / Sprintregeneration | |
| Jump Height | Sprunghöhe | |
| Knockback | Rückstoß | Minecraft German |
| Thorns | Dornen | Minecraft German |
| Reflection | Reflexion | |
| Poison | Gift | |
| Exploding | Explosion | |
| Stealing | Smaragddiebstahl | |
| Loot / Loot Bonus | Beute / Beutebonus | `Lootrun` itself stays English |
| Loot Quality | Beutequalität | |
| XP / XP Bonus | EP / EP-Bonus | Erfahrungspunkte. Not "XP" in game text |
| Combat / Gathering Experience | Kampferfahrung / Sammelerfahrung | |
| Gathering Speed | Sammelgeschwindigkeit | |
| Slow Enemy | Gegner verlangsamen | |
| Weaken Enemy | Gegner schwächen | |
| Cost | Kosten (pl.) | `Kosten für Teleport` |
| Cooldown | Abklingzeit | |
| Range | Reichweite | |
| Area of Effect | Wirkungsbereich | |
| Duration | Dauer | |
| Charges | Ladungen | Uses. See Charge (spell) |
| Radius | Radius | |
| Per Hit | Pro Treffer | |
| Perfect / Defective | Perfekt / Fehlerhaft | Item roll quality |
| Combat Level | Kampfstufe | |
| Crafting Level | Herstellungsstufe | |
| Level / Lv. / Lv. Min | Stufe / Stufe / Min. Stufe | `Min. Holzfällen-Stufe` |

### Scale (two meanings)

| English | de_de | Notes |
|---|---|---|
| Scale (stat weighting: Main Scale, Damage Scale, Raid Scale…) | Affinität | `Hauptaffinität`, `Schadensaffinität`, `Nahkampfaffinität`, `Lootrun-Affinität`. This is the weight that rates how good an item roll is |
| scale (size multiplier in a tool/setting) | Skalierung | Not the stat. Do not unify |

### Attack speed

Used predicatively after `Angriffsgeschwindigkeit`, so no agreement endings.

| English | de_de |
|---|---|
| Super Fast | Superschnell |
| Very Fast | Sehr schnell |
| Fast | Schnell |
| Normal | Normal |
| Slow | Langsam |
| Very Slow | Sehr langsam |
| Super Slow | Superlangsam |

## Status effects and ability terms

| English | de_de | Notes |
|---|---|---|
| Slow / Slowness | Verlangsamung / Langsamkeit | Slow = the action; Slowness = the effect (Minecraft German) |
| Blind / Blindness | Blendung | |
| Weaken | Schwächung | |
| Stun | Betäubung | |
| Freeze | Frost / Einfrieren | Labels: `Gefrierdauer`, `Frostschaden` |
| Burn | Verbrennung | Labels: `Brenndauer`, `Brandschaden` |
| Bleed / Bleeding | Blutung | |
| Poison | Gift | |
| Buff | Stärkung | `Stärkungsdauer`. Not "Bonus" (that is Bonus) |
| Shield | Schild | |
| Heal / Healing | Heilung | |
| Effect | Effekt | |
| Clone / Mirror / Mirage / Shadow Clone | Klon / Spiegelklon / Trugbildklon / Schattenklon | |
| Mark | Markierung | |
| Trap | Falle | `Basaltfalle` |
| Puppet | Puppe | Shaman |
| Effigy | Bildnis | |
| Mask (of the Lunatic / Fanatic / Heretic) | Maske (des Wahnsinnigen / des Fanatikers / des Ketzers) | |
| Hound | Jagdhund | |
| Crow | Krähe | |
| Snake / Serpent | Schlange | `Donnerschlange`, `Feuerschlange` |
| Orb | Kugel | |
| Holy Power | Heilige Macht | |
| Ultimate Meter | Ultimate-Leiste | |
| Mana Bank | Manaspeicher | |
| Blood Pool | Blutreserve | |
| Summons | Beschwörungen | |
| Riftspawn | Spaltbrut | |
| Corrupted (status) | Verderbt | |
| Provoked / Confused / Discombobulated | Provoziert / Verwirrt / Desorientiert | |
| Guardian Angel | Schutzengel | |
| Twilight Zone | Zwielichtzone | |
| Momentum | Schwung | |
| Curse (Shaman ability) | Fluch | Not the Lootrun stat, see below |

## Professions

| English | de_de | Notes |
|---|---|---|
| Profession | Beruf | |
| Tailoring | Schneiderei | |
| Jeweling | Juwelierkunst | |
| Weaponsmithing | Waffenschmieden | |
| Armouring | Rüstungsschmieden | |
| Woodworking | Holzbearbeitung | |
| Cooking | Kochen | |
| Alchemism | Alchemie | |
| Scribing | Schreibkunst | Makes scrolls |
| Woodcutting | Holzfällen | `Min. Holzfällen-Stufe` |
| Mining | Bergbau | |
| Fishing | Angeln | |
| Farming | Landwirtschaft | |
| Gathering | Sammeln | `Sammelerfahrung`, `Sammelwerkzeug` |
| Crafting | Herstellung | `Herstellungsstufe` |
| Crafting Station | Handwerksstation | As the F6 screen |
| Gathering Totem | Sammeltotem | See Totem below |
| Pickaxe / Axe / Rod / Scythe | Spitzhacke / Axt / Angel / Sense | Gathering tools |

## Currency and trading

| English | de_de | Notes |
|---|---|---|
| Emerald | Smaragd | |
| Emerald Block | Smaragdblock | Item name |
| Liquid Emerald | Flüssiger Smaragd (pl. Flüssige Smaragde) | |
| Emerald Pouch | Smaragdbeutel | |
| emeralds (guild resource) | Smaragde | Guild/territory emeralds are a separate resource from your wallet. zh_tw names them differently ("gems"); es and de keep "emerald" because the English does. Make the context clear (`Gildensmaragde`) when a line could be read either way |
| Trade Market | Handelsmarkt | |
| Bank | Bank | |
| Sell / Buy | Verkaufen / Kaufen | |
| Price | Preis | |

## Content types

| English | de_de | Notes |
|---|---|---|
| Quest | Quest (die, pl. Quests) | German players say Quest. `Benötigte Quest:` |
| Mini-Quest | Mini-Quest | |
| Storyline | Hauptgeschichte | |
| Content Book | Inhaltsbuch | |
| Discovery (Territorial / World / Secret) | Gebietsentdeckung / Weltentdeckung / Geheime Entdeckung | Lists: `Territorial:`, `Welt:`, `Geheim:` |
| Area Discovered: | Gebiet entdeckt: | The area name after it stays English (`{p}`) |
| Cave | Höhle | |
| Dungeon | Dungeon (der, pl. Dungeons) | Dungeon names are translated (as es), see below |
| Corrupted Dungeon | Verderbter Dungeon | |
| Raid | Raid (der, pl. Raids) | Raid names translated as es, see below. General "raid/raider" in dialogue = Überfall / Plünderer |
| Lootrun | Lootrun (der, pl. Lootruns) | **Stays English** (es too). `Lootrun-Lager` |
| World Event | Weltereignis | |
| Boss | Boss (pl. Bosse) | `Boss-Altar`, `Bossleiste` |
| Mob | Mob / Monster | `Mob` in UI text, `Monster` in prose |
| Party | Gruppe | `Gruppensuche` (Party Finder) |
| Daily Objective | Tagesziel | `Gildenziel` for guild objectives |
| Daily Reward | Tägliche Belohnung | |
| Bomb Bell | Bombenglocke | |
| Bombs | Kampf-EP-Bombe, Berufs-EP-Bombe, Berufstempo-Bombe, Beutebombe, Dungeon-Bombe, Weltereignis-Bombe, Partybombe | Combat XP / Profession XP / Profession Speed / Loot / Dungeon / World Event / Party Bomb |
| Crates | Kisten | |
| Loot Chest / Flying Chest | Beutetruhe / Fliegende Truhe | |
| Part I/II | Teil I/II | After a comma: `Der Gesandte, Teil I` |

### Dungeon and raid names

Translate like es does; the `Corrupted` version is `Verderbte(r/s) …`. Use these forms everywhere (keys, fragments, shops, tracker).

| English | de_de |
|---|---|
| Decrepit Sewers | Verfallene Kanalisation |
| Infested Pit | Befallene Grube |
| Lost Sanctuary | Verlorenes Heiligtum |
| Underworld Crypt | Unterweltgruft |
| Sand-Swept Tomb | Sandverwehtes Grab |
| Ice Barrows | Eishügelgräber |
| Undergrowth Ruins | Ruinen im Unterholz |
| Galleon's Graveyard | Galeonenfriedhof |
| Fallen Factory | Gefallene Fabrik |
| Eldritch Outlook | Unheimlicher Ausguck |
| Timelost Sanctum | Zeitverlorenes Sanktum |
| Nest of the Grootslangs | Nest der Grootslangs |
| Orphion's Nexus of Light | Orphions Nexus des Lichts |
| The Canyon Colossus | Der Canyon-Koloss |
| The Nameless Anomaly | Die namenlose Anomalie |

Pattern: `Schlüssel der Verfallenen Kanalisation`, `Zerbrochener Schlüssel …`, `Fragment der Unterweltgruft`, `Dungeon-Laden: Unterweltgruft`.

### Quest names

Translate them meaningfully, like es does, and keep the pun when you can (`Lost Soles` = *Verlorene Sohlen*, `Cowfusion` = *Kuhddelmuddel*, `Celebrations in Smoke` = *Ein Fest in Rauch*). The full list is in `quest-name.json`; **every other place that shows a quest name must reuse it exactly**.
Names the Spanish keeps English stay English inside the title: `Dwarves und Doguns, Teil I`, `Ärger in Tempo Town`.

### Major IDs

`Major ID` stays English. Major ID **names** are translated (as es), the description follows this glossary; the F6 "ability + original" setting can show the English next to them.

## Lootrun

| English | de_de | Notes |
|---|---|---|
| Lootrun | Lootrun | |
| Challenge | Herausforderung | |
| Beacon | Leuchtfeuer (das) | `Lila Leuchtfeuer`, `Aqua-Leuchtfeuer`, `Regenbogen-Leuchtfeuer` |
| Vibrant / Obscured (beacon) | Intensiv / Verhüllt | `Intensives Aqua-Leuchtfeuer`, `Verhülltes Leuchtfeuer` |
| Boon | Segen (pl. Segen) | |
| Curse (lootrun stat) | Gegnerverstärkung | Enemy buff counter, as es ("refuerzo enemigo"). The ability `Curse` (Shaman) is **Fluch** |
| Mission | Auftrag | Kept away from Quest |
| Trial | Prüfung | |
| Pulls | Ziehungen | `Belohnungsziehungen` |
| Reroll | Neuwurf | `Leuchtfeuer-Neuwurf`, `Neuwurf der Endbelohnung` |
| End Reward | Endbelohnung | |
| Sacrifice | Opfer | `Opfer der Endbelohnung` |
| Elite | Elite | `Lootrun: Elite II` |
| Potency | Wirksamkeit | |
| Time Bonus | Zeitbonus | |

## Raid

| English | de_de | Notes |
|---|---|---|
| Raid | Raid | |
| Gambit | Gambit (das) | `Raid-Gambits`, `Gambit-Platz` |
| Raid Keeper | Raid Keeper | NPC name, stays English (es) |

## Guild

| English | de_de | Notes |
|---|---|---|
| Guild | Gilde | |
| Guild Bank | Gildenbank | |
| Guild Tower | Gildenturm | |
| War (guild system) | Krieg / Gildenkrieg | General "war" in dialogue = Krieg too |
| Territory | Gebiet | |
| Territory Defences | Gebietsverteidigung | |
| HQ | Hauptquartier (HQ) | |
| Season Rating | Saisonwertung | |
| Member Menu | Mitgliedermenü | |
| Owner | Besitzer | Guild rank |
| Chief | Anführer | Guild rank. **Not "Boss"** |
| Strategist | Stratege | |
| Captain | Hauptmann | |
| Recruiter | Anwerber | |
| Recruit | Rekrut | Guild rank and army recruit alike (`Der Rekrut des Königs`) |
| Ore / Wood / Fish / Crops | Erz / Holz / Fisch / Getreide | Territory resources |

## Totem (two meanings)

| English | de_de | Notes |
|---|---|---|
| Totem (Shaman 1st spell) | Totem | Ability name |
| Mob Totem / Gathering Totem | Monstertotem / Sammeltotem | The placeable items. `Monstertotem von {u}` |
| Totem Pole | Totempfahl | Discovery name |

## World and lore

| English | de_de | Notes |
|---|---|---|
| Corruption | Verderbnis | The faction/force in the story: `die Verderbnis` |
| Corrupted / the corrupted (mobs) | verderbt / die Verderbten | `Das verderbte Dorf`. Not "korrupt" (that means bribe-taking) |
| Decay (Gavel's calamity) | der Verfall | As a proper noun. General "decay/decaying" = Verfall / verfallend |
| Sovereign | Herrscher / Herrscherin | `Herrscher Zhiraok`, `Herrscherin Soosu` |
| Her Majesty | Ihre Majestät | Direct address: `Eure Majestät` |
| the Cursed (Fruma) | die Verfluchten | `Der Verfluchte` (quest *The Cursed One*) |
| Inquisitor | Inquisitor | |
| Gendarme | Gendarm | `Gendarmeriekommandant` |
| Realm of Light / Light | Reich des Lichts / das Licht | |
| Realm of Darkness / Dark | Reich der Dunkelheit / die Dunkelheit | |
| Void | die Leere | |
| Wanderer | der Wanderer | Kept apart from Spaltwandler (Riftwalker) |
| Vagabond | Vagabund | Kept apart from Wanderer |
| Dwarf / Dwarves / Dwarven | Zwerg / Zwerge / zwergisch | In dialogue and descriptions. The quest title keeps `Dwarves` (as es) |
| Corkian | Corkian | Stays English |
| The Order of the Grook | der Orden des Grook | |
| Leaves in the Wind | Blätter im Wind | Fruma resistance |
| Item Identifier | Gegenstandsidentifizierer | NPC job |
| Powder Master | Pulvermeister | NPC job |
| Blacksmith / Merchant | Schmied / Händler | `Smaragdhändler`, `Trankhändler`, `Schriftrollenhändler`, `Werkzeughändler` |
| Banker / Guard / Villager / Citizen | Bankier / Wache / Dorfbewohner / Bürger | |
| King / Queen / Elder | König / Königin / Ältester | |

### Common monsters

Generic creature words are translated; made-up species names stay English (`Gert`, `Grook`, `Dernic`, `Wybel`), as es.

| English | de_de |
|---|---|
| Zombie / Skeleton / Spider | Zombie / Skelett / Spinne |
| Wolf / Bear / Slime | Wolf / Bär / Schleim |
| Bandit / Pirate / Thief | Bandit / Pirat / Dieb |
| Ghost / Undead / Mummy | Geist / Untoter / Mumie |
| Witch / Golem / Scarab | Hexe / Golem / Skarabäus |

## Common UI words

| English | de_de | Notes |
|---|---|---|
| Settings | Einstellungen | |
| Left Click / Right Click | Linksklick / Rechtsklick | `Shift + Rechtsklick` |
| Click to Open | Klicken zum Öffnen | `Linksklick zum Anzeigen des Inhalts` |
| Confirm / Cancel | Bestätigen / Abbrechen | |
| Back / Close | Zurück / Schließen | |
| Next Page / Previous Page | Nächste Seite / Vorherige Seite | |
| Search / Filter / Sort | Suchen / Filtern / Sortieren | `Ergebnisse sortieren` |
| Unlock / Unlocked | Freischalten / Freigeschaltet | |
| Locked | Gesperrt | |
| Equipped | Ausgerüstet | |
| Reward(s) | Belohnung(en) | |
| Requirement(s) | Voraussetzung(en) | `Du erfüllst die Voraussetzungen nicht` |
| Level | Stufe | |
| Tracker (quest tracker) | Quest-Tracker | |
| Nameplate | Namensschild | |
| Tooltip | Tooltip | |
| TIP: | TIPP: | |
| Upload / Download | Hochladen / Herunterladen | |
| On / Off | An / Aus | |

## Always keep in English

- **Place names** (Ragni, Detlas, Troms…): handled by the `{p}` placeholder.
- **People's names** and made-up monster names the Spanish leaves alone.
- **Item names** that es leaves untranslated (most Mythics).
- **Lootrun**, **Relik**, **Shiny**, **Corkian**, **Major ID**, **Raid Keeper**.
- Ingredients and materials inside square brackets in quest dialogue (`[{~} Carp Meat]`).
- What other players type in chat: never translated.

## Want to change a term?

Open a PR that changes this file and explains why, and change the translation files that use the term in the same PR, so the glossary and the corpus never disagree.
