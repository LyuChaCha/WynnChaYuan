# อภิธานศัพท์ภาษาไทย (th_th)

> This file says **how to translate**. For **what things are**, see [World and story](docs/world-and-story.md).
> It is the th_th glossary of its own. It is **not** a conversion of `GLOSSARY.md` (zh_tw) or of the Spanish files:
> the Spanish (es_es) corpus is our reviewed reference for *what* gets translated and *what stays English*,
> but the wording here is natural modern Thai as used in Thai game localisation.

Every recurring game term goes through this table. When a word is not sure, look it up here and **do not invent a second one**.
**To change a term, change it here first**, then change the translation files that use it, so everyone moves together.
Use Ctrl+F.

## General rules

- **Thai script, Arabic digits.** Never Thai digits (๑๒๓). `เวทที่ 1`, `Lv. ขั้นต่ำ: 80`.
- **Spaces the Thai way.** No spaces between words. Put a space between phrases and sentences, and around Latin words and numbers: `ความลับของ Aldorei ตอนที่ I`, `ค่ามานา Teleport:`, `{~} บล็อก`.
- **Short.** Most strings sit in tooltips, item lists and a dialogue box of fixed width. Prefer the short word players actually use (`คูลดาวน์`, `บัฟ`, `สตัน`, `ไอเทม`, `เควส`).
- **Placeholders stay exactly as they are**: `{#}` `{~}` `{~1}` `{p}` `{u}`, same number of line breaks. `{cN}…{/}` colour markers are optional but welcome. If you reorder a sentence that has two `{~}`, number them (`{~1}`, `{~2}`) so the numbers do not land in the wrong slots. Thai word order often puts the number after the noun (`{~} บล็อก` stays, but `Kill {~} mobs` = `กำจัดมอนสเตอร์ {~} ตัว`).
- **Follow es_es on what stays English.** If the Spanish keeps a name in English (person, place, item, monster with a made-up name), keep it in **Latin letters** too. Do not transliterate it into Thai script. Place names are handled by `{p}` automatically.
- **Classifiers.** Thai counts with classifiers: `ตัว` for mobs and animals, `ชิ้น` for items, `ครั้ง` for times, `คน` for people, `บล็อก` for blocks, `วินาที` for seconds.
- **Address the player as `คุณ`.** In dialogue give each character a voice: `ครับ` for male speakers, `ค่ะ/คะ` for female speakers, `นะ` to soften; `ข้า/เจ้า` for villains, gods, ancient beings and old-fashioned speakers. Keep one voice per character inside a quest.
- **Punctuation.** Thai normally has no full stop or comma; end a sentence with a space or the line end. Keep `:` in labels (half-width), `…` for an ellipsis, plain `-`. Avoid symbols the game never uses (full-width brackets, `⋯`, `～`). Avoid `ฯ` and `ฯลฯ`; write `และอีกมากมาย` instead.
- **Spell names**: in the stat lines of the ability tree (`ability-labels.json`, e.g. `Teleport Cost:`) the base spell name **stays English** (`ค่ามานา Teleport:`), exactly like es_es. In item stat lines (`ui-labels.json`, e.g. `Teleport Cost`) it is **translated** with `เวท` in front (`ค่ามานาเวทเทเลพอร์ต`). The two files are matched against different screens; do not unify them.
- **Ability-tree `%` labels** (`Health:%`) are `X %:` (`พลังชีวิต %:`), short on purpose: the ability tree column is narrow.

## Classes

Class names are always "**main class/skin**", with a plain slash and no spaces.

| English | th_th | Notes |
|---|---|---|
| Mage/Dark Wizard | จอมเวท/พ่อมดมืด | |
| Archer/Hunter | นักธนู/นายพราน | |
| Warrior/Knight | นักรบ/อัศวิน | |
| Assassin/Ninja | นักฆ่า/นินจา | |
| Shaman/Skyseer | ชาแมน/ผู้หยั่งฟ้า | `ชาแมน`, not `หมอผี` (that reads as a witch doctor) |
| Class | คลาส | **Not `อาชีพ`**: that word is reserved for Profession |
| Class Type | ประเภทคลาส | |

### Archetypes

Archetype = **สาย** (the way Thai players say "a build line": สายเวท, สายแทงก์). Label forms: `สายX`, `สายX:`, `สายXขั้นต่ำ:` (as es `Mín. de arquetipo X:`). The bare label `Archetype:` is `สาย:`.

| English | th_th | Notes |
|---|---|---|
| Archetype | สาย | |
| Light Bender / Lightbender | ผู้บิดแสง | Mage. es "Doblaluz" |
| Arcanist | จอมอาคม | Mage |
| Riftwalker | ผู้เดินรอยแยก | Mage. Rift = รอยแยก everywhere (Riftspawn = สิ่งกำเนิดรอยแยก) |
| Fallen | ผู้ร่วงหล่น | Warrior |
| Battle Monk | นักพรตนักรบ | Warrior |
| Paladin | พาลาดิน | Warrior |
| Sharpshooter | มือแม่น | Archer |
| Trapper | นักวางกับดัก | Archer |
| Boltslinger | นักรัวศร | Archer. It fires volleys of arrows, so "ศร" (arrow), not lightning |
| Shadestepper | ผู้ย่างเงา | Assassin |
| Trickster | จอมเจ้าเล่ห์ | Assassin |
| Acrobat | นักกายกรรม | Assassin |
| Summoner | นักอัญเชิญ | Shaman |
| Ritualist | นักพิธีกรรม | Shaman |
| Acolyte | สาวก | Shaman |

### Base spells

Translated names (item stat lines, descriptions). Ability-tree stat labels keep the English name.

| Order | Mage | Archer | Warrior | Assassin | Shaman |
|---|---|---|---|---|---|
| 1st | ฮีล (Heal) | พายุลูกธนู (Arrow Storm) | ทุบ (Bash) | โจมตีหมุน (Spin Attack) | โทเท็ม (Totem) |
| 2nd | เทเลพอร์ต (Teleport) | หลบหนี (Escape) | พุ่งชน (Charge) | หายตัว (Vanish) | ลากดึง (Haul) |
| 3rd | อุกกาบาต (Meteor) | ธนูระเบิด (Arrow Bomb / Bomb Arrow) | อัปเปอร์คัต (Uppercut) | รัวโจมตี (Multihit) | ออร่า (Aura) |
| 4th | งูน้ำแข็ง (Ice Snake) | โล่ลูกธนู (Arrow Shield) | คำรามศึก (War Scream) | ระเบิดควัน (Smoke Bomb) | ถอนราก (Uproot) |

- Ordinals: `1st/2nd/3rd/4th Spell` = `เวทที่ 1/2/3/4` (`ค่ามานาเวทที่ 1`).
- Spell cost on items: `ค่ามานาเวท` + name (`ค่ามานาเวทพุ่งชน`), so a one-word name never runs into "ค่ามานา".
- The Heal **spell** is `ฮีล` (what players call it); healing in general is `การรักษา` (`การรักษารวม`, `ประสิทธิภาพการรักษา`).
- `Charge` (warrior spell) is **พุ่งชน**, so it never collides with `Charges` = **จำนวนครั้ง** (uses of an ability/item).

## Weapons and gear

| English | th_th | Notes |
|---|---|---|
| Weapon | อาวุธ | |
| Armour / Armor | เกราะ | |
| Accessory | เครื่องประดับ | Also the product of Jeweling |
| Gear / Equipment | อุปกรณ์สวมใส่ | Short form in lists: อุปกรณ์ |
| Item | ไอเทม | What Thai players say; not `สิ่งของ` |
| Spear | หอก | Warrior |
| Dagger | มีดสั้น | Assassin. `Knife/Knives` (ability) = มีด |
| Bow | ธนู | Archer. An arrow is `ลูกธนู`; `ศร` only in poetic names |
| Wand | คทา | Mage |
| Relik | Relik | Shaman. Invented word, stays in Latin letters (es too) |
| Helmet | หมวกเกราะ | |
| Chestplate | เกราะอก | |
| Leggings | กางเกงเกราะ | |
| Boots | รองเท้า | |
| Ring | แหวน | |
| Bracelet | กำไล | |
| Necklace | สร้อยคอ | |
| Unidentified X | X ที่ยังไม่ได้ตรวจสอบ | `หมวกเกราะที่ยังไม่ได้ตรวจสอบ` |
| Untradable Item | ไอเทมแลกเปลี่ยนไม่ได้ | |
| Quest Item | ไอเทมเควส | |
| Durability | ความทนทาน | |
| Powder | ผงธาตุ | `ผงธาตุสายฟ้า II`. Powder Specials = พลังพิเศษผงธาตุ |
| Powder Slots / Sockets | ช่องผงธาตุ | |
| Ingredient | วัตถุดิบ | Crafting ingredient (mob drops etc.) |
| Material | วัสดุ | Gathered raw material. Keep the two apart |
| Ingredient Pouch | ถุงวัตถุดิบ | |
| Tome / Mastery Tome | ตำรา / ตำราความชำนาญ | |
| Charm | เครื่องราง | |
| Aspect | แอสเพกต์ | Raid reward; no Thai word fits, keep it short |
| Crafted Item | ไอเทมประดิษฐ์ | `Crafted by` = ประดิษฐ์โดย (but crafted item *names* stay as the player wrote them) |
| Mount | พาหนะ | `Right-Click to Summon` = คลิกขวาเพื่อเรียก |
| Pet | สัตว์เลี้ยง | |

### Item tiers (rarity)

Tier word after the noun: `ไอเทมตำนาน`, `แอสเพกต์เทพปกรณัม`.

| English | th_th | Notes |
|---|---|---|
| Normal | ธรรมดา | |
| Unique | พิเศษ | |
| Rare | หายาก | |
| Legendary | ตำนาน | |
| Fabled | ปรัมปรา | Kept apart from Legendary and Mythic |
| Mythic | เทพปกรณัม | Item names (Mythic included) are translated like the other languages do, with a category word (หอก, มีดสั้น, ธนู, คทา, Relik…); the in-game item-name setting can show or hide the original |
| Set | เซ็ต | `ไอเทมเซ็ต`, `โบนัสเซ็ต` |
| Crafted | ประดิษฐ์ | |
| Shiny | Shiny | Item prefix and tracker system stay English, as es |
| tier (as in `{~} tier`, Tier IV) | ระดับ | `ระดับ {~}`, `ผงธาตุระดับ IV`. Not `เลเวล`, which is Level |

### Mount stats

| English | th_th |
|---|---|
| Potential | ศักยภาพ |
| Speed | ความเร็ว |
| Acceleration | อัตราเร่ง |
| Altitude | ระดับความสูง |
| Energy | พลังงาน |
| Handling | การควบคุม |
| Toughness | ความอึด (not ความแข็งแกร่ง, see Strength) |
| Boost | แรงเร่ง |
| Training | การฝึก |
| Jump Height (mount) | กระโดด |

## Elements

Standalone (`Fire:` in the ability tree, element lists) the bare word; inside stat names with `ธาตุ` in front: `ความเสียหายธาตุไฟ`.

| English | th_th | Notes |
|---|---|---|
| Earth | ดิน | `ธาตุดิน` |
| Thunder | สายฟ้า | `ธาตุสายฟ้า`. Not ฟ้าร้อง (that is only the sound) |
| Water | น้ำ | `ธาตุน้ำ` |
| Fire | ไฟ | `ธาตุไฟ`. `เพลิง` only in poetic names |
| Air | ลม | `ธาตุลม` |
| Neutral | ไร้ธาตุ | `ความเสียหายไร้ธาตุ` |
| Elemental | ธาตุ | `ความเสียหายธาตุ`, `พลังป้องกันธาตุ` |
| element / elements / all elements | ธาตุ / ธาตุ / ทุกธาตุ | |

## Skill points (attributes)

`Skill Point` and `Ability Point` are two different systems; keep them apart.

| English | th_th | Notes |
|---|---|---|
| Skill Point | แต้มค่าสถานะ | The five stats below. es "punto de atributo" |
| Ability Point | แต้มความสามารถ | Spent in the ability tree |
| Ability Tree | ต้นไม้ความสามารถ | |
| Ability | ความสามารถ | Not สกิล (players use it loosely for both; we need the two apart) |
| Strength | พละกำลัง | |
| Dexterity | ความแม่นยำ | Precision (crit chance). **Not** ความคล่องแคล่ว, which reads like Agility |
| Intelligence | สติปัญญา | |
| Defence / Defense | พลังป้องกัน | Stat and elemental defences alike: `พลังป้องกันธาตุไฟ` |
| Agility | ความว่องไว | Evasion. Kept apart from Dexterity |
| Min. Strength (etc.) | พละกำลังขั้นต่ำ | Requirement lines |
| Unassigned Skill Points | แต้มค่าสถานะคงเหลือ | |
| Unused Ability Points | แต้มความสามารถคงเหลือ | |

## Stat lines (identifications)

Damage stats are built from pieces. Pattern: **ความเสียหาย + [โจมตีหลัก / เวท] + [ธาตุ / ธาตุX]**, then the modifier.

| Piece | th_th | Example |
|---|---|---|
| Damage | ความเสียหาย | Standard Thai localisation word. Not ดาเมจ |
| Main Attack | โจมตีหลัก (standalone: การโจมตีหลัก) | `Main Attack Damage` = ความเสียหายโจมตีหลัก |
| Spell | เวท | `Spell Damage` = ความเสียหายเวท. Used for every class, warriors included |
| Element X | ธาตุX | `Fire Spell Damage` = ความเสียหายเวทธาตุไฟ |
| Elemental (combined) | ธาตุ | `Elemental Main Attack Damage` = ความเสียหายโจมตีหลักธาตุ |
| `%` (item line) | ` %` | `ความเสียหายธาตุไฟ %` |
| `Raw` (item line) | ` (ค่าคงที่)` | `ความเสียหายธาตุไฟ (ค่าคงที่)` (es "valor fijo") |
| `X:%` (ability tree line) | `X %:` | `Air Damage:%` = ความเสียหายธาตุลม %: |

| English | th_th | Notes |
|---|---|---|
| Health | พลังชีวิต | |
| Health Regen | ฟื้นฟูพลังชีวิต | |
| Healing Efficiency | ประสิทธิภาพการรักษา | |
| Overhealth | พลังชีวิตส่วนเกิน | Extra health above the maximum |
| Mana | มานา | |
| Max Mana | มานาสูงสุด | |
| Mana Regen | ฟื้นฟูมานา | |
| Mana Cost / Spell Cost | ค่ามานา | `ค่ามานาต่อวินาที` |
| Life Steal | ดูดเลือด | The usual Thai gamer word; health itself stays พลังชีวิต |
| Mana Steal | ดูดมานา | |
| Critical Damage | ความเสียหายคริติคอล | |
| Attack Speed | ความเร็วโจมตี | |
| DPS | ความเสียหายต่อวินาที | as es; the demo line keeps "DPS" |
| Main Attack Range | ระยะโจมตีหลัก | |
| Walk Speed | ความเร็วเดิน | |
| Sprint / Sprint Regen | พลังวิ่ง / ฟื้นฟูพลังวิ่ง | The sprint stamina bar |
| Jump Height | ความสูงกระโดด | Mount stat short form: กระโดด |
| Knockback | ผลักกลับ | Minecraft Thai word |
| Thorns | หนาม | Minecraft Thai word |
| Reflection | สะท้อนกลับ | |
| Poison | พิษ | |
| Exploding | ระเบิด | |
| Stealing | ขโมยมรกต | |
| Loot / Loot Bonus | ของดรอป / โบนัสของดรอป | `Lootrun` itself stays English |
| Loot Quality | คุณภาพของดรอป | |
| XP Bonus | โบนัส XP | Players say XP |
| Combat / Gathering Experience | ค่าประสบการณ์ต่อสู้ / ค่าประสบการณ์เก็บรวบรวม | |
| Gathering Speed | ความเร็วเก็บรวบรวม | |
| Slow Enemy | ชะลอศัตรู | |
| Weaken Enemy | บั่นทอนศัตรู | Weaken = บั่นทอน everywhere |
| Cost | ค่าใช้ | Mana cost is ค่ามานา |
| Cooldown | คูลดาวน์ | |
| Range | ระยะ | |
| Area of Effect | พื้นที่ส่งผล | |
| Duration | ระยะเวลา | |
| Charges | จำนวนครั้ง | Uses. See Charge (spell) |
| Radius | รัศมี | |
| Per Hit | ต่อฮิต | |
| Damage Multiplier | ตัวคูณความเสียหาย | |
| Perfect / Defective | สมบูรณ์แบบ / บกพร่อง | Item roll quality |
| Combat Level | เลเวลต่อสู้ | |
| Crafting Level | เลเวลประดิษฐ์ | |
| Level / Lv. / Lv. Min | เลเวล / Lv. / Lv. ขั้นต่ำ | `Lv. ตัดไม้ขั้นต่ำ:` |

### Scale (two meanings)

| English | th_th | Notes |
|---|---|---|
| Scale (stat weighting: Main Scale, Damage Scale, Raid Scale…) | ความเหมาะสม | `ความเหมาะสมหลัก`, `ความเหมาะสมด้านความเสียหาย`, `ความเหมาะสมระยะประชิด`, `ความเหมาะสมสายจอมอาคม`. es "afinidad". This is the weight that rates how good an item roll is |
| scale (size multiplier in a tool/setting) | ขนาด / อัตราส่วน | Not the stat. Do not unify |

### Attack speed

| English | th_th |
|---|---|
| Super Fast | เร็วสุดขีด |
| Very Fast | เร็วมาก |
| Fast | เร็ว |
| Normal | ปกติ |
| Slow | ช้า |
| Very Slow | ช้ามาก |
| Super Slow | ช้าสุดขีด |

## Status effects and ability terms

| English | th_th | Notes |
|---|---|---|
| Slow / Slowness | ชะลอ / เชื่องช้า | Slow = the action; Slowness = the effect |
| Blind / Blindness | ตาบอด | |
| Weaken | บั่นทอน | |
| Stun | สตัน | Players' word. Kept apart from Discombobulated (งุนงง) |
| Freeze | แช่แข็ง | |
| Burn | เผาไหม้ | |
| Bleed / Bleeding | เลือดไหล | |
| Poison | พิษ | |
| Buff | บัฟ | `ระยะเวลาบัฟ`. Not "โบนัส" (that is Bonus) |
| Shield | โล่ | |
| Heal / Healing | การรักษา | The spell is ฮีล |
| Effect | เอฟเฟกต์ | |
| Clone / Mirror / Mirage / Shadow Clone | ร่างแยก / ร่างแยกกระจก / ร่างแยกภาพลวง / ร่างแยกเงา | Thai has no plural: Clones = ร่างแยก too |
| Mark | เครื่องหมาย | |
| Trap | กับดัก | |
| Puppet | หุ่นเชิด | Shaman |
| Effigy | หุ่นบูชา | |
| Mask (of the Lunatic / Fanatic / Heretic) | หน้ากาก (ผู้วิปลาส / ผู้คลั่งลัทธิ / ผู้นอกรีต) | Zealot = ผู้คลั่งศรัทธา, kept apart from Fanatic |
| Hound | สุนัขล่าเนื้อ | |
| Crow | อีกา | |
| Snake / Serpent | งู / อสรพิษ | Ice Snake = งูน้ำแข็ง; Thunder Serpent = อสรพิษสายฟ้า |
| Orb | ออร์บ | |
| Holy Power | พลังศักดิ์สิทธิ์ | |
| Ultimate Meter | เกจอัลติเมต | |
| Mana Bank | คลังมานา | |
| Blood Pool | แอ่งเลือด | |
| Summons | สิ่งอัญเชิญ | |
| Riftspawn | สิ่งกำเนิดรอยแยก | |
| Corrupted (status) | แปดเปื้อน | Same word as the Corruption, see World |
| Provoked / Confused / Discombobulated | ถูกยั่วยุ / สับสน / งุนงง | |
| Enkindle / Enkindled | จุดเพลิง / ติดเพลิง | |
| Guardian Angel | เทวดาผู้พิทักษ์ | |
| Twilight Zone | แดนสนธยา | |

## Professions

Profession = **อาชีพ** (Class is คลาส, so the two never collide).

| English | th_th | Notes |
|---|---|---|
| Profession | อาชีพ | |
| Tailoring | ตัดเย็บ | |
| Jeweling | ทำเครื่องประดับ | It makes rings, bracelets and necklaces |
| Weaponsmithing | ตีอาวุธ | |
| Armouring | ตีเกราะ | |
| Woodworking | งานไม้ | Bows, wands, reliks |
| Cooking | ทำอาหาร | |
| Alchemism | เล่นแร่แปรธาตุ | |
| Scribing | เขียนคัมภีร์ | Makes scrolls |
| Woodcutting | ตัดไม้ | `Lv. ตัดไม้ขั้นต่ำ` |
| Mining | ขุดแร่ | |
| Fishing | ตกปลา | |
| Farming | เพาะปลูก | |
| Gathering | เก็บรวบรวม | Covers all four gathering professions (not เก็บเกี่ยว, which is only crops) |
| Crafting | ประดิษฐ์ | `แท่นประดิษฐ์` (crafting station) |
| Gathering Totem | โทเท็มเก็บรวบรวม | See Totem below |

## Currency and trading

| English | th_th | Notes |
|---|---|---|
| Emerald | มรกต | |
| Emerald Block | บล็อกมรกต | Item name |
| Liquid Emerald | มรกตเหลว | |
| Emerald Pouch | ถุงมรกต | |
| emeralds (guild resource) | มรกต | Guild/territory emeralds are a separate resource from your wallet. zh_tw names them differently ("gems"); es and th keep "emeralds" because the English does. Make the context clear (`มรกตของกิลด์`) when a line could be read either way |
| Trade Market | ตลาด | |
| Bank | ธนาคาร | |
| Sell / Buy | ขาย / ซื้อ | |
| Price | ราคา | |

## Content types

| English | th_th | Notes |
|---|---|---|
| Quest | เควส | What players say. Kept apart from Lootrun Mission = ภารกิจ |
| Mini-Quest | มินิเควส | |
| Storyline | เนื้อเรื่องหลัก | |
| Content Book | สมุดเนื้อหา | |
| Discovery (Territorial / World / Secret) | การค้นพบ (ดินแดน / โลก / ลับ) | List labels: `ดินแดน:`, `โลก:`, `ลับ:` |
| Area Discovered: | ค้นพบพื้นที่: | |
| Cave | ถ้ำ | |
| Dungeon | ดันเจียน | Dungeon names are translated (as es) |
| Corrupted Dungeon | ดันเจียนแปดเปื้อน | |
| Raid | เรด | Raid names translated as es. General "raid/raider" in dialogue = การบุกโจมตี / ผู้บุกโจมตี |
| Lootrun | Lootrun | **Stays English** (es too). `แคมป์ Lootrun` |
| World Event | อีเวนต์โลก | |
| Boss | บอส | `แท่นบูชาบอส`, `แถบบอส` |
| Mob | มอนสเตอร์ | |
| Party | ปาร์ตี้ | |
| Daily Objective | ภารกิจรายวัน | |
| Daily Reward | รางวัลรายวัน | |
| Bomb Bell | ระฆังระเบิด | |
| Crates | กล่องสุ่ม | |
| Part I/II | ตอนที่ I/II | After a space: `ทูต ตอนที่ I`. Roman numerals stay |

### Quest names

Translate them meaningfully, like es does, and keep the pun when you can (`Lost Soles` = *พื้นรองเท้าพเนจร*, echoing วิญญาณพเนจร; `Cowfusion` = *วัวงงงวย*; `Lost in the Jungle` = *หลงป่า*; `The Hidden City` = *นครลับแล*; `Overture to Despair` = *โหมโรงสู่ความสิ้นหวัง*). The full list is in `quest-name.json`; **every other place that shows a quest name must reuse it exactly**.
Names the Spanish keeps English stay English inside the title: `Dwarves และ Doguns ตอนที่ I`, `วุ่นวายใน Tempo Town`.

## Lootrun

| English | th_th | Notes |
|---|---|---|
| Lootrun | Lootrun | |
| Challenge | ความท้าทาย | |
| Beacon | บีคอน | Minecraft Thai word. `บีคอนสีม่วง` |
| Boon | พร | |
| Curse (lootrun stat) | มอนสเตอร์เสริมพลัง | Enemy buff counter, as es ("refuerzo enemigo"). The ability `Curse` (Shaman) is **คำสาป** |
| Mission | ภารกิจ | Kept away from Quest = เควส |
| Trial | บททดสอบ | |
| Pulls | สิทธิ์สุ่มรางวัล | |
| Reroll | สุ่มใหม่ | `สุ่มบีคอนใหม่`, `สุ่มรางวัลสุดท้ายใหม่` |
| End Reward | รางวัลสุดท้าย | |
| Sacrifice | สังเวย | |
| Elite | อีลีท | `Lootrun: อีลีท II` |
| Potency | ความแรง | |
| Time Bonus | โบนัสเวลา | |

## Raid

| English | th_th | Notes |
|---|---|---|
| Raid | เรด | |
| Gambit | เดิมพัน | `เดิมพันเรด`, `ช่องเดิมพัน` |
| Raid Keeper | Raid Keeper | NPC name, stays English (es) |

## Guild

| English | th_th | Notes |
|---|---|---|
| Guild | กิลด์ | |
| Guild Bank | ธนาคารกิลด์ | |
| Guild Tower | หอคอยกิลด์ | |
| War (guild system) | สงครามกิลด์ | General "war" in dialogue = สงคราม. War Scream (spell) = คำรามศึก, unrelated |
| Territory | ดินแดน | |
| Territory Defences | การป้องกันดินแดน | |
| HQ | กองบัญชาการ | |
| Season Rating | คะแนนประจำซีซัน | |
| Owner | เจ้าของ | Guild rank |
| Chief | หัวหน้า | Guild rank. **Not บอส**, which is Boss |
| Strategist | นักวางแผน | |
| Captain | กัปตัน | |
| Recruiter | ผู้สรรหา | |
| Recruit | สมาชิกใหม่ | Guild rank. A Fruma army recruit is ทหารใหม่ (`ทหารใหม่ของราชินี`) |
| Ore / Wood / Fish / Crops | แร่ / ไม้ / ปลา / พืชผล | Territory resources |

## Totem (two meanings)

| English | th_th | Notes |
|---|---|---|
| Totem (Shaman 1st spell) | โทเท็ม | Ability name (`Totem` in ability-tree labels) |
| Mob Totem / Gathering Totem | โทเท็มมอนสเตอร์ / โทเท็มเก็บรวบรวม | The placeable items. One word in Thai is fine (es also uses one); zh_tw splits them, we don't need to |
| Totem Pole | เสาโทเท็ม | Discovery name |

## World and lore

| English | th_th | Notes |
|---|---|---|
| Corruption | ความแปดเปื้อน | The faction/force in the story: a taint that spreads. `Corrupted` (mobs, places) = แปดเปื้อน (`หมู่บ้านแปดเปื้อน`). Not ทุจริต (that is bribery) |
| Decay (Gavel's calamity) | ความเสื่อมสลาย | Gavel's calamity. Keep it apart from Corruption. General "decay/decaying" = เน่าเปื่อย, ผุพัง. `Corroded` = ถูกกัดกร่อน |
| Sovereign | องค์อธิราช | `องค์อธิราช Zhiraok`. Used for male and female sovereigns alike |
| Her Majesty | สมเด็จพระราชินี | In speech `ฝ่าบาท` |
| the Cursed (Fruma) | ผู้ต้องสาป | `ผู้ต้องสาป` (quest *The Cursed One*) |
| Inquisitor | ผู้ไต่สวน | |
| Gendarme | ตำรวจทหาร | `Gendarme Soldier` = ตำรวจทหาร, `Gendarme Commander` = ผู้บัญชาการตำรวจทหาร |
| Realm of Light / Light | อาณาจักรแห่งแสง / แสง | |
| Realm of Darkness / Dark | อาณาจักรแห่งความมืด / ความมืด | |
| Wanderer | ผู้พเนจร | Kept apart from Vagabond |
| Vagabond | คนจรจัด | |
| Dwarf / Dwarves / Dwarven | คนแคระ | In dialogue and descriptions. The quest title keeps `Dwarves` (as es) |
| Corkian | Corkian | Stays English |
| The Order of the Grook | ภาคีแห่ง Grook | As es "la orden" (zh_tw chose "academy"; we follow es) |
| Leaves in the Wind | ใบไม้ในสายลม | Fruma resistance |
| Item Identifier | ผู้ตรวจสอบไอเทม | NPC job |
| Powder Master | ปรมาจารย์ผงธาตุ | NPC job |
| Blacksmith / Merchant | ช่างตีเหล็ก / พ่อค้า | |

## Common UI words

| English | th_th | Notes |
|---|---|---|
| Settings | การตั้งค่า | |
| Left Click / Right Click | คลิกซ้าย / คลิกขวา | `Shift + คลิกขวา` |
| Click to Open | คลิกเพื่อเปิด | |
| Confirm / Cancel | ยืนยัน / ยกเลิก | |
| Back / Close | ย้อนกลับ / ปิด | |
| Next Page / Previous Page | หน้าถัดไป / หน้าก่อนหน้า | |
| Search / Filter / Sort | ค้นหา / กรอง / เรียง | |
| Unlock / Unlocked | ปลดล็อก / ปลดล็อกแล้ว | |
| Locked | ล็อกอยู่ | |
| Blocked (ability) | ถูกบล็อก | |
| Equipped | สวมใส่อยู่ | |
| Reward(s) | รางวัล | |
| Requirement(s) | เงื่อนไข | `คุณยังไม่ผ่านเงื่อนไข` |
| Level | เลเวล | |
| Tracker (quest tracker) | ตัวติดตามเควส | |
| Nameplate | ป้ายชื่อ | |
| Tooltip | คำอธิบาย | |
| TIP: | เคล็ดลับ: | |
| Upload / Download | อัปโหลด / ดาวน์โหลด | |
| On / Off | เปิด / ปิด | |
| Translation / Original | คำแปล / ต้นฉบับ | |

## Always keep in English

- **Place names** (Ragni, Detlas, Troms…): handled by the `{p}` placeholder.
- **People's names** and made-up monster names the Spanish leaves alone, in Latin letters (no Thai transliteration).
- **Lootrun**, **Relik**, **Shiny**, **Corkian**, **Major ID**, **XP**, **Lv.**
- Ingredients and materials inside square brackets in quest dialogue (`[{~} Carp Meat]`).
- What other players type in chat: never translated.

## Want to change a term?

Open a PR that changes this file and explains why, and change the translation files that use the term in the same PR, so the glossary and the corpus never disagree.
