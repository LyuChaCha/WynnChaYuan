# Glossaire français (fr_fr)

> This file says **how to translate**. For **what things are**, see [World and story](docs/world-and-story.md).
> It is the fr_fr glossary of its own. It is **not** a conversion of `GLOSSARY.md` (zh_tw) or of the Spanish files:
> the Spanish (es_es) corpus is our reviewed reference for *what* gets translated and *what stays English*,
> but the wording here is French.

Every recurring game term goes through this table. When a word is not sure, look it up here and **do not invent a second one**.
**To change a term, change it here first**, then change the translation files that use it, so everyone moves together.
Use Ctrl+F.

## General rules

- **The player is addressed with *vous*** (`Vous ne remplissez pas les conditions`, `de votre vie max.`, `Cliquez pour ouvrir`), as the term files already do and as Minecraft's own French does. Characters may *tutoyer* each other, or the player, when the scene calls for it (old friends, children, rough mercenaries), but stay consistent inside one character's lines. The F6 settings screen (`lang/fr_fr.json`) uses *vous* too.
- **Short.** Most strings sit in tooltips, item lists and a dialogue box of fixed width. Prefer the short natural word.
- **Never write a capital `À` or `Á`.** The game font uses those two glyphs as icons, so they show as a black screen or an image. A capitalised preposition is written **`A`** (`A vos ordres`, `A bientôt`). Lower-case `à` is fine.
- **Typography the game can draw**: write `oe`, not `œ` (`coeur`, `soeur`, `oeil`); no non-breaking spaces; no `« »` guillemets, use `"..."`; **no space before `:` `!` `?` `;`** (`Vie:`, `Attention!`). The corpus never has that space, keep it that way. Accented letters `é è ê ë ç à â ô î ï û ù` are fine.
- **Placeholders stay exactly as they are**: `{#}` `{~}` `{~1}` `{p}` `{u}`, same number of line breaks. `{cN}…{/}` colour markers are optional but welcome. If you reorder a sentence that has two `{~}` (or two `{#}`), number them (`{~1}`, `{~2}` / `{#1}`, `{#2}`) so the values do not land in the wrong slots. `{p}` and `{u}` have no numbers: they fill in order, so do not swap two of them.
- **Follow es_es on what stays English.** If the Spanish keeps a name in English (person, place, item, monster with a made-up name), keep it in English too. Place names are handled by `{p}` automatically.
- **Sentence case** for UI labels (`Dégâts d'attaque principale`, not `Dégâts D'Attaque Principale`). Element names keep a capital inside stat labels, as in Spanish (`Dégâts de Feu`).
- **Spell names**: in the stat lines of the ability tree (`ability-labels.json`, e.g. `Teleport Cost:`) the base spell name **stays English** (`Coût de Teleport:`), exactly like es_es. In item stat lines (`ui-labels.json`, e.g. `Teleport Cost`) it is **translated** (`Coût de Téléportation`). The two files are matched against different screens; do not unify them.
- Elision follows the word as written: `Coût d'Ice Snake:`, `Coût d'Uppercut`, `Dégâts d'Eau`.
- Avoid symbols the game never uses (full-width brackets, `⋯`, `～`). Use three periods `...` for an ellipsis (the dialogue font draws `…` as centred dots) and plain `-`/`—`.

## Classes

Class names are always "**main class/skin**", with a plain slash and no spaces.

| English | fr_fr | Notes |
|---|---|---|
| Mage/Dark Wizard | Mage/Sorcier noir | |
| Archer/Hunter | Archer/Chasseur | |
| Warrior/Knight | Guerrier/Chevalier | |
| Assassin/Ninja | Assassin/Ninja | |
| Shaman/Skyseer | Chaman/Voyant céleste | |
| Class | Classe | |
| Class Type | Type de classe | |

### Archetypes

Label form: `Archétype X`, `Archétype X:`, `Min. archétype X:`.

| English | fr_fr | Notes |
|---|---|---|
| Archetype | Archétype | |
| Light Bender / Lightbender | Plieur de lumière | Mage |
| Arcanist | Arcaniste | Mage |
| Riftwalker | Arpenteur de failles | Mage |
| Fallen | Déchu | Warrior |
| Battle Monk | Moine de combat | Warrior |
| Paladin | Paladin | Warrior |
| Sharpshooter | Tireur d'élite | Archer. "Élite" alone is also a Lootrun word; the full form is unambiguous |
| Trapper | Trappeur | Archer |
| Boltslinger | Lance-foudre | Archer |
| Shadestepper | Passe-ombre | Assassin |
| Trickster | Filou | Assassin |
| Acrobat | Acrobate | Assassin |
| Summoner | Invocateur | Shaman |
| Ritualist | Ritualiste | Shaman |
| Acolyte | Acolyte | Shaman |

### Base spells

| Order | Mage | Archer | Warrior | Assassin | Shaman |
|---|---|---|---|---|---|
| 1st | Soin (Heal) | Tempête de flèches (Arrow Storm) | Fracas (Bash) | Attaque tournoyante (Spin Attack) | Totem |
| 2nd | Téléportation (Teleport) | Fuite (Escape) | Charge (Charge) | Disparition (Vanish) | Traction (Haul) |
| 3rd | Météore (Meteor) | Flèche explosive (Arrow Bomb / Bomb Arrow) | Uppercut | Coups multiples (Multihit) | Aura |
| 4th | Serpent de glace (Ice Snake) | Bouclier de flèches (Arrow Shield) | Cri de guerre (War Scream) | Bombe fumigène (Smoke Bomb) | Déracinement (Uproot) |

- Ordinals: `1st/2nd/3rd/4th Spell` = `1er/2e/3e/4e sort` (`Coût du 1er sort`).
- `Charge` (warrior spell) and `Charges` (uses of an ability/item) are the same word in French. The spell keeps its capital (`Coût de Charge`); the uses are lower case inside a sentence (`3 charges`) and `Charges:` as a label.
- Remember the rule above: in `ability-labels.json` these names stay English.

## Weapons and gear

| English | fr_fr | Notes |
|---|---|---|
| Weapon | Arme | |
| Armour / Armor | Armure | |
| Accessory | Accessoire | |
| Gear / Equipment | Équipement | |
| Item | Objet | |
| Spear | Lance | Warrior |
| Dagger | Dague | Assassin |
| Bow | Arc | Archer |
| Wand | Baguette | Mage. Not "bâton" (that would be a staff) |
| Relik | Relik | Shaman. Invented word, stays as is (es too) |
| Helmet | Casque | |
| Chestplate | Plastron | Minecraft fr word |
| Leggings | Jambières | Minecraft fr word |
| Boots | Bottes | |
| Ring | Anneau | |
| Bracelet | Bracelet | |
| Necklace | Collier | |
| Unidentified X | X non identifié(e) | `Casque non identifié`, `Baguette non identifiée` |
| Untradable Item | Objet non échangeable | |
| Quest Item | Objet de quête | |
| Durability | Durabilité | |
| Powder | Poudre | `Poudre de Foudre II`. Powder Specials = Spéciaux de poudre |
| Powder Slots / Sockets | Emplacements de poudre | |
| Ingredient | Ingrédient | Crafting ingredient (mob drops etc.) |
| Material | Matériau | Gathered raw material. Keep the two apart |
| Ingredient Pouch | Sacoche d'ingrédients | |
| Tome / Mastery Tome | Tome / Tome de maîtrise | |
| Charm | Charme | |
| Aspect | Aspect | |
| Crafted Item | Objet fabriqué | `Crafted by` = Fabriqué par (but crafted item *names* stay as the player wrote them) |
| Mount | Monture | |
| Pet | Familier | |

### Item tiers (rarity)

Adjective after the noun, agreeing in gender: `Objet mythique`, `Baguette légendaire`.

| English | fr_fr | Notes |
|---|---|---|
| Normal | Normal | |
| Unique | Unique | |
| Rare | Rare | |
| Legendary | Légendaire | |
| Fabled | Fabuleux / Fabuleuse | as es "fabuloso" |
| Mythic | Mythique | Gear names follow es: where es translates a name (with a category word) French does too; where es leaves it blank (most Mythics) it stays English. The in-game item-name setting can show the original |
| Set | D'ensemble | `Objet d'ensemble`, `Bonus d'ensemble` |
| Crafted | Fabriqué | |
| Shiny | scintillant(e) | Item prefix, after the name as es does (`{name} reluciente`): `Masque scintillant`, `Lance … scintillante` |
| tier (as in `{~} tier`, Tier IV) | rang | `rang {~}`, `Poudre de rang IV`. Not "niveau", which is Level |

## Elements

| English | fr_fr | Notes |
|---|---|---|
| Earth | Terre | |
| Thunder | Foudre | |
| Water | Eau | |
| Fire | Feu | |
| Air | Air | |
| Neutral | Neutre | `Dégâts neutres` |
| Elemental | Élémentaire | `Dégâts élémentaires` |
| element / elements / all elements | élément / éléments / tous les éléments | |

## Skill points (attributes)

`Skill Point` and `Ability Point` are two different systems; keep them apart.

| English | fr_fr | Notes |
|---|---|---|
| Skill Point | Point d'attribut | The five stats below. es "punto de atributo" |
| Ability Point | Point de compétence | Spent in the ability tree |
| Ability Tree | Arbre de compétences | |
| Ability | Compétence | Not "capacité" |
| Strength | Force | |
| Dexterity | Dextérité | |
| Intelligence | Intelligence | |
| Defence / Defense | Défense | |
| Agility | Agilité | |
| Min. Strength (etc.) | Force min. | Requirement lines |
| Unassigned Skill Points | Points d'attribut non assignés | |
| Unused Ability Points | Points de compétence inutilisés | |

## Stat lines (identifications)

Damage stats are built from pieces. Pattern: **Dégâts + [d'attaque principale / des sorts] + [élémentaires / de Élément]**, then the modifier.

| Piece | fr_fr | Example |
|---|---|---|
| Damage | Dégâts (plural) | |
| Main Attack | Attaque principale | `Main Attack Damage` = Dégâts d'attaque principale |
| Spell | Sort | `Spell Damage` = Dégâts des sorts. Not "magie" |
| Element X | de X | `Fire Spell Damage` = Dégâts des sorts de Feu |
| Elemental (combined) | élémentaires (after the noun) | `Elemental Main Attack Damage` = Dégâts d'attaque principale élémentaires |
| `%` (item line) | ` %` | `Dégâts de Feu %` |
| `Raw` (item line) | ` (valeur fixe)` | `Dégâts de Feu (valeur fixe)` |
| `X:%` (ability tree line) | `Pourcentage de x:` | `Air Damage:%` = Pourcentage de dégâts d'Air: |
| Defence (elemental) | Défense de X / Défense élémentaire | `Défense d'Eau` |

| English | fr_fr | Notes |
|---|---|---|
| Health | Vie | Not "santé" |
| Health Regen | Régénération de vie | |
| Healing Efficiency | Efficacité des soins | |
| Overhealth | Surplus de vie | Extra health above the maximum |
| Mana | Mana | Masculine: `le mana`, `mana max` |
| Max Mana | Mana max | |
| Mana Regen | Régénération de mana | |
| Mana Cost | Coût en mana | |
| Life Steal | Vol de vie | |
| Mana Steal | Vol de mana | |
| Critical Damage | Dégâts critiques | |
| Attack Speed | Vitesse d'attaque | |
| DPS | Dégâts par seconde | Stat label. In running text the abbreviation stays: `(de vos DPS)` |
| Main Attack Range | Portée de l'attaque principale | |
| Walk Speed | Vitesse de déplacement | |
| Sprint / Sprint Regen | Sprint / Régénération de sprint | |
| Jump Height | Hauteur de saut | Mount stat short form: Saut |
| Knockback | Recul | Minecraft fr word |
| Thorns | Épines | Minecraft fr word |
| Reflection | Réflexion | |
| Poison | Poison | |
| Exploding | Explosion | |
| Stealing | Vol d'émeraudes | |
| Loot / Loot Bonus | Butin / Bonus de butin | `Lootrun` itself stays English |
| Loot Quality | Qualité du butin | |
| XP Bonus | Bonus d'XP | Players say XP |
| Combat / Gathering Experience | Expérience de combat / de récolte | |
| Gathering Speed | Vitesse de récolte | |
| Slow Enemy | Ralentir l'ennemi | |
| Weaken Enemy | Affaiblir l'ennemi | |
| Cost | Coût | |
| Cooldown | Recharge | |
| Range | Portée | |
| Area of Effect | Zone d'effet | |
| Duration | Durée | |
| Charges | Charges | Uses. See Charge (spell) |
| Radius | Rayon | |
| Per Hit | Par coup | |
| Total Damage / Total Heal | Dégâts totaux / Soins totaux | |
| Damage Multiplier | Multiplicateur de dégâts | |
| Perfect / Defective | Parfait / Défectueux | Item roll quality |
| Combat Level | Niveau de combat | |
| Crafting Level | Niveau d'artisanat | |
| Lv. / Lv. Min | Niv. / Niv. min. | `Niv. de combat min.:` |

### Scale (two meanings)

| English | fr_fr | Notes |
|---|---|---|
| Scale (stat weighting: Main Scale, Damage Scale, Raid Scale…) | Affinité | `Affinité principale`, `Affinité de dégâts`, `Affinité de mêlée`. es "afinidad". This is the weight that rates how good an item roll is |
| scale (size multiplier in a tool/setting) | échelle | Not the stat. Do not unify |

### Attack speed

Feminine, it agrees with *vitesse*.

| English | fr_fr |
|---|---|
| Super Fast | Super rapide |
| Very Fast | Très rapide |
| Fast | Rapide |
| Normal | Normale |
| Slow | Lente |
| Very Slow | Très lente |
| Super Slow | Super lente |

## Status effects and ability terms

| English | fr_fr | Notes |
|---|---|---|
| Slow / Slowness | Ralentissement / Lenteur | Slow = the action; Slowness = the effect |
| Blind / Blindness | Aveuglement | |
| Weaken | Affaiblissement | |
| Stun | Étourdissement | |
| Freeze | Gel | |
| Burn | Brûlure | |
| Bleed / Bleeding | Saignement | |
| Poison | Poison | |
| Buff | Renforcement | `Durée du renforcement`. Not "bonus" (that is Bonus) |
| Shield | Bouclier | |
| Heal / Healing | Soin(s) | |
| Effect | Effet | |
| Clone / Mirror / Mirage / Shadow Clone | Clone / Clone miroir / Clone mirage / Clone d'ombre | |
| Mark | Marque | |
| Trap | Piège | |
| Puppet | Marionnette | Shaman |
| Effigy | Effigie | |
| Mask (of the Lunatic / Fanatic / Heretic) | Masque (du Lunatique / du Fanatique / de l'Hérétique) | |
| Hound | Limier | |
| Crow | Corbeau | |
| Snake / Serpent | Serpent | |
| Orb | Orbe | |
| Holy Power | Pouvoir sacré | |
| Ultimate Meter | Jauge d'ultime | |
| Mana Bank | Banque de mana | |
| Blood Pool | Réserve de sang | |
| Summons | Invocations | |
| Riftspawn | Rejeton de faille | |
| Corrupted (status) | Corrompu | |
| Provoked / Confused / Discombobulated | Provoqué / Confus / Déboussolé | |
| Guardian Angel | Ange gardien | |
| Twilight Zone | Zone crépusculaire | |

## Professions

| English | fr_fr | Notes |
|---|---|---|
| Profession | Métier | The usual MMO word in French |
| Tailoring | Couture | |
| Jeweling | Joaillerie | |
| Weaponsmithing | Forge d'armes | |
| Armouring | Forge d'armures | |
| Woodworking | Menuiserie | |
| Cooking | Cuisine | |
| Alchemism | Alchimie | |
| Scribing | Calligraphie | Makes scrolls |
| Woodcutting | Bûcheronnage | `Niv. min. de bûcheronnage` |
| Mining | Minage | |
| Fishing | Pêche | |
| Farming | Agriculture | |
| Gathering | Récolte | |
| Crafting | Artisanat | `Station d'artisanat`, `Niveau d'artisanat` |
| Gathering Totem | Totem de récolte | See Totem below |

## Currency and trading

| English | fr_fr | Notes |
|---|---|---|
| Emerald | Émeraude | |
| Emerald Block | Bloc d'émeraude | Item name |
| Liquid Emerald | Émeraude liquide | |
| Emerald Pouch | Sacoche d'émeraudes | |
| emeralds (guild resource) | émeraudes | Guild/territory emeralds are a separate resource from your wallet. zh_tw names them differently ("gems"); es, pt and fr keep "émeraudes" because the English does. Make the context clear (`émeraudes de la guilde`) when a line could be read either way |
| Trade Market | Marché | |
| Bank | Banque | |
| Sell / Buy | Vendre / Acheter | |
| Price | Prix | |

## Content types

| English | fr_fr | Notes |
|---|---|---|
| Quest | Quête | |
| Mini-Quest | Mini-quête | |
| Storyline | Histoire principale | |
| Content Book | Livre de contenu | |
| Discovery (Territorial / World / Secret) | Découverte (territoriale / mondiale / secrète) | Lists use the plural adjective: `Territoriales:`, `Mondiales:`, `Secrètes:` |
| Area Discovered: | Zone découverte: | The area name after it stays English, unless the World Discovery name is translated (then use that name: `Zone découverte: Passage pourri`) |
| Cave | Grotte | |
| Dungeon | Donjon | Dungeon names are translated (as es); see the table below |
| Corrupted Dungeon | Donjon corrompu | `Égouts décrépits corrompus` |
| Raid | Raid | French players say raid. Raid names translated as es; see below. General "raid/raider" in dialogue = attaque / pillard |
| Lootrun | Lootrun | **Stays English** (es too). `Camp de Lootrun` |
| World Event | Événement mondial | |
| Boss | Boss | `Autel du boss`, `Barre du boss` |
| Mob | Mob / monstre | `mob` in UI text, `monstre` in prose |
| NPC | PNJ | |
| Party | Groupe | `Recherche de groupe` (Party Finder) |
| Daily Objective | Objectif quotidien | |
| Daily Reward | Récompense quotidienne | |
| Bomb Bell | Cloche à bombes | |
| Crates | Caisses | |
| Seaskipper | Seaskipper | Stays English (es too) |
| Part I/II | partie I/II | After a comma: `L'émissaire, partie I` |

### Dungeon and raid names

Sentence case, like any French proper name built from common words: the article stays lower case in running text (`dans les Égouts décrépits`).

| English | fr_fr |
|---|---|
| Decrepit Sewers | Égouts décrépits |
| Infested Pit | Fosse infestée |
| Underworld Crypt | Crypte des enfers |
| Lost Sanctuary | Sanctuaire perdu |
| Sand-Swept Tomb | Tombeau des sables |
| Ice Barrows | Tertres de glace |
| Undergrowth Ruins | Ruines des broussailles |
| Galleon's Graveyard | Cimetière des galions |
| Fallen Factory | Usine déchue |
| Eldritch Outlook | Belvédère surnaturel |
| Timelost Sanctum | Sanctum hors du temps |
| Nest of the Grootslangs | Nid des Grootslangs |
| Orphion's Nexus of Light | Nexus de lumière d'Orphion |
| The Canyon Colossus | Le Colosse du canyon |
| The Nameless Anomaly | L'Anomalie sans nom |

### Quest names

Translate them meaningfully, like es does, and keep the pun when you can (`Lost Soles` = *Semelles en peine*, `Cowfusion` = *Vachefusion*, `Celebrations in Smoke` = *La fête part en fumée*). The full list is in `quest-name.json`; **every other place that shows a quest name must reuse it exactly**.
Names the Spanish keeps English stay English inside the title: `Dwarves et Doguns, partie I`, `Ennuis à Tempo Town`.

## Lootrun

| English | fr_fr | Notes |
|---|---|---|
| Lootrun | Lootrun | |
| Challenge | Défi | |
| Beacon | Balise | `Balise violette`, `Balise aqua vibrante` |
| Boon | Bénédiction | |
| Curse (lootrun stat) | Renfort ennemi | Enemy buff counter, as es ("refuerzo enemigo"). The ability `Curse` (Shaman) is **Malédiction** |
| Mission | Mission | No clash: Quest = Quête |
| Trial | Épreuve | |
| Pulls | Tirages | `Tirages de récompense` |
| Reroll | Relance | `relance de balise`, `relance de la récompense finale` |
| End Reward | Récompense finale | |
| Sacrifice | Sacrifice | `sacrifice de récompense finale` |
| Elite | Élite | `Lootrun: Élite II` |
| Potency | Puissance | |
| Time Bonus | Bonus de temps | |

## Raid

| English | fr_fr | Notes |
|---|---|---|
| Raid | Raid | |
| Gambit | Gambit | `Gambits de raid`, `Emplacement de gambit`. A real French word (chess) |
| Raid Keeper | Raid Keeper | NPC name, stays English (es) |

## Guild

| English | fr_fr | Notes |
|---|---|---|
| Guild | Guilde | |
| Guild Bank | Banque de la guilde | |
| Guild Tower | Tour de la guilde | |
| War (guild system) | Guerre (de guildes) | General "war" in dialogue = guerre too |
| Territory | Territoire | |
| Territory Defences | Défenses du territoire | |
| HQ | QG | |
| Season Rating | Score de saison | |
| Owner | Propriétaire | Guild rank |
| Chief | Chef | Guild rank. No clash: Boss stays "Boss" |
| Strategist | Stratège | |
| Captain | Capitaine | |
| Recruiter | Recruteur | |
| Recruit | Recrue | Guild rank and Fruma army recruit alike (`La recrue du roi`) |
| Ore / Wood / Fish / Crops | Minerai / Bois / Poissons / Récoltes | Territory resources |

## Totem (two meanings)

| English | fr_fr | Notes |
|---|---|---|
| Totem (Shaman 1st spell) | Totem | Ability name |
| Mob Totem / Gathering Totem | Totem de monstres / Totem de récolte | The placeable items. Same word in fr (es also uses one word); zh_tw splits them, we don't need to |
| Totem Pole | Totem | Discovery name |

## World and lore

| English | fr_fr | Notes |
|---|---|---|
| Corruption | Corruption | The faction/force in the story. Capitalised when it is the force: `la Corruption` |
| Corrupted / the corrupted (mobs) | corrompu(s) | `Le village corrompu` |
| Decay (Gavel's calamity) | la Décadence | Capitalised, as es/pt. General "decay/decaying" = pourrissant, en décomposition |
| Sovereign | Souverain / Souveraine | `Souverain Zhiraok`, `Souveraine Soosu` |
| Her Majesty | Sa Majesté | |
| Cursed One (Fruma) | le Maudit / un Maudit / les Maudits | Always capital M as a title; quest *The Cursed One* = `Le Maudit`. Lowercase `maudit` only as a plain adjective |
| Inquisitor | Inquisiteur | |
| Gendarme | Gendarme | `Soldat gendarme`, `Commandant de la Gendarmerie` |
| Realm of Light / Light | Royaume de la Lumière / la Lumière | |
| Realm of Darkness / Dark | Royaume des Ténèbres / les Ténèbres | |
| Wanderer | Errant | `l'Errant` |
| Vagabond | Vagabond | Kept apart from Wanderer |
| Dwarf / Dwarves / Dwarven | Nain / Nains / nain | In dialogue and descriptions. The quest title keeps `Dwarves` (as es) |
| Corkian | Corkian | Stays English |
| The Order of the Grook | l'Ordre du Grook | |
| Leaves in the Wind | Feuilles au vent | Fruma resistance |
| Item Identifier | Identificateur d'objets | NPC job |
| Powder Master | Maître des poudres | NPC job |
| Blacksmith / Merchant | Forgeron / Marchand | |

## Common UI words

| English | fr_fr | Notes |
|---|---|---|
| Settings | Paramètres | |
| Left Click / Right Click | Clic gauche / Clic droit | `Maj + clic droit` |
| Click to Open | Cliquez pour ouvrir | |
| Confirm / Cancel | Confirmer / Annuler | |
| Back / Close | Retour / Fermer | |
| Next Page / Previous Page | Page suivante / Page précédente | |
| Search / Filter / Sort | Rechercher / Filtrer / Trier | |
| Unlock / Unlocked | Débloquer / Débloqué | |
| Locked | Verrouillé | |
| Equipped | Équipé | |
| Reward(s) | Récompense(s) | |
| Requirement(s) | Condition(s) | `Vous ne remplissez pas les conditions` |
| Level | Niveau | |
| Tracker (quest tracker) | Suivi de quête | |
| Nameplate | Plaque de nom | |
| Tooltip | Infobulle | |
| TIP: | ASTUCE: | |
| Upload / Download | Envoyer / Télécharger | |
| On / Off | Activé / Désactivé | |

## Always keep in English

- **Place names** (Ragni, Detlas, Troms…): handled by the `{p}` placeholder.
- **People's names** and made-up monster names the Spanish leaves alone.
- **Lootrun**, **Relik**, **Corkian**, **Seaskipper**, **Major ID**.
- Ingredients and materials inside square brackets in quest dialogue (`[{~} Carp Meat]`).
- What other players type in chat: never translated.

## Want to change a term?

Open a PR that changes this file and explains why, and change the translation files that use the term in the same PR, so the glossary and the corpus never disagree.
