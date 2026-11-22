# Bảng thuật ngữ tiếng Việt (vi_vn)

> This file says **how to translate**. For **what things are**, see [World and story](docs/world-and-story.md).
> It is the vi_vn glossary of its own. It is **not** a conversion of `GLOSSARY.md` (zh_tw) or of the Spanish files:
> the Spanish (es_es) corpus is our reviewed reference for *what* gets translated and *what stays English*,
> but the wording here is Vietnamese.

Every recurring game term goes through this table. When a word is not sure, look it up here and **do not invent a second one**.
**To change a term, change it here first**, then change the translation files that use it, so everyone moves together.
Use Ctrl+F.

## General rules

- **Standard modern Vietnamese, full diacritics, precomposed (NFC) characters only** (`ệ`, `ở`, never a base letter plus a combining mark). Check with a script before you submit if your keyboard (Telex/VNI) produces combining marks.
- **Address the player as *bạn*.** In dialogue, pick pronouns that fit who speaks to whom: *tôi/bạn* by default, *ta/ngươi* for villains, gods and ancient beings, *anh/chị/em* only when the relationship is clear. Keep the choice consistent per character inside a quest.
- **Short.** Most strings sit in tooltips, item lists and a dialogue box of fixed width. Prefer the short natural word, the way Vietnamese players talk (*máu*, *mana*, *hồi chiêu*, *sát thương*).
- **Placeholders stay exactly as they are**: `{#}` `{~}` `{~1}` `{p}` `{u}`, same number of line breaks. `{cN}…{/}` colour markers are optional but welcome. Vietnamese often reorders sentences: if a line has two `{~}` and you swap their order, number them (`{~1}`, `{~2}`) so the numbers do not land in the wrong slots.
- **Follow es_es on what stays English.** If the Spanish keeps a name in English (person, place, item, monster with a made-up name), keep it in English too. Place names are handled by `{p}` automatically.
- **Sentence case** for UI labels (`Sát thương đánh thường`, not `Sát Thương Đánh Thường`). Exceptions: element names inside stat labels keep a capital, as in Spanish (`Sát thương Lửa`, `Phòng thủ Gió`); spell names in item lines keep a capital on the first word (`Tiêu hao Bão tên`); archetype names are written in Title Case because they are titles (`Thánh Kỵ Sĩ`).
- **Spell names**: in the stat lines of the ability tree (`ability-labels.json`, e.g. `Teleport Cost:`) the base spell name **stays English** (`Tiêu hao Teleport:`), exactly like es_es. In item stat lines (`ui-labels.json`, e.g. `Teleport Cost`) it is **translated** (`Tiêu hao Dịch chuyển`). The two files are matched against different screens; do not unify them.
- **Percent and raw lines.** Ability tree `X:%` = `X (%):` (`Air Damage:%` = `Sát thương Gió (%):`). Item line `X%` = `X %` (`Sát thương Lửa %`), `X Raw` = `X (cố định)`.
- **No space before a colon**, as in normal Vietnamese typography (`Máu:`).
- Avoid symbols the game never uses (full-width brackets, `⋯`, `～`). Use `…` for an ellipsis and plain `-`/`—`.

## Classes

Class names are always "**main class/skin**", with a plain slash and no spaces.

| English | vi_vn | Notes |
|---|---|---|
| Mage/Dark Wizard | Pháp sư/Phù thủy bóng tối | |
| Archer/Hunter | Cung thủ/Thợ săn | |
| Warrior/Knight | Chiến binh/Hiệp sĩ | |
| Assassin/Ninja | Sát thủ/Ninja | |
| Shaman/Skyseer | Thầy pháp/Chiêm tinh sư | *Thầy pháp* (folk magic, spirits) is kept apart from *Pháp sư* (Mage). Never write "Pháp sư" for the Shaman |
| Class | Lớp nhân vật | |
| Class Type | Lớp nhân vật | Item requirement line |

### Archetypes

Label form: `Trường phái X`, `Trường phái X:`, `Trường phái X tối thiểu:` (es `Mín. de arquetipo X:`). The pattern "noun + *tối thiểu*" is the same for every minimum: `Điểm kỹ năng tối thiểu:`, `Sức mạnh tối thiểu`.

| English | vi_vn | Notes |
|---|---|---|
| Archetype | Trường phái | Not "nguyên mẫu" (sounds like a prototype) |
| Light Bender / Lightbender | Ngự Quang Sư | Mage. "One who commands light", like es "Doblaluz" |
| Arcanist | Bí Thuật Sư | Mage |
| Riftwalker | Hành Giả Khe Nứt | Mage. *Khe nứt* = rift, see Riftspawn |
| Fallen | Kẻ Sa Ngã | Warrior |
| Battle Monk | Võ Tăng | Warrior |
| Paladin | Thánh Kỵ Sĩ | Warrior |
| Sharpshooter | Thần Xạ Thủ | Archer. Not "Tinh anh": Elite is a Lootrun word |
| Trapper | Thợ Bẫy | Archer |
| Boltslinger | Lôi Xạ Thủ | Archer. es "Lanzarrayos" |
| Shadestepper | Kẻ Bước Bóng | Assassin |
| Trickster | Kẻ Bịp Bợm | Assassin |
| Acrobat | Kẻ Nhào Lộn | Assassin |
| Summoner | Triệu Hồi Sư | Shaman |
| Ritualist | Tế Lễ Sư | Shaman |
| Acolyte | Môn Đồ | Shaman |

### Base spells

| Order | Mage | Archer | Warrior | Assassin | Shaman |
|---|---|---|---|---|---|
| 1st | Chữa lành (Heal) | Bão tên (Arrow Storm) | Đập mạnh (Bash) | Đòn xoay (Spin Attack) | Totem |
| 2nd | Dịch chuyển (Teleport) | Thoát thân (Escape) | Xung phong (Charge) | Ẩn thân (Vanish) | Kéo lê (Haul) |
| 3rd | Thiên thạch (Meteor) | Tên bom (Arrow Bomb / Bomb Arrow) | Hất tung (Uppercut) | Liên kích (Multihit) | Hào quang (Aura) |
| 4th | Rắn băng (Ice Snake) | Khiên tên (Arrow Shield) | Chiến hống (War Scream) | Bom khói (Smoke Bomb) | Nhổ rễ (Uproot) |

- Ordinals: `1st/2nd/3rd/4th Spell` = `phép 1/2/3/4` (`Tiêu hao phép 1`). Short on purpose; "phép thứ nhất" is too long for a stat line.
- `Charge` (warrior spell) is **Xung phong**, so it never collides with `Charges` = **Số lượt** (uses of an ability/item).
- `Bash` as a counting word in a description (`per bash`) = *mỗi cú đập*.
- Remember the rule above: in `ability-labels.json` these names stay English.

## Weapons and gear

| English | vi_vn | Notes |
|---|---|---|
| Weapon | Vũ khí | |
| Armour / Armor | Giáp | |
| Accessory | Phụ kiện | |
| Gear / Equipment | Trang bị | |
| Item | Vật phẩm | |
| Spear | Giáo | Warrior |
| Dagger | Dao găm | Assassin |
| Bow | Cung | Archer |
| Wand | Đũa phép | Mage. Not "trượng" (that would be a staff) |
| Relik | Relik | Shaman. Invented word, stays as is (es too) |
| Helmet | Mũ giáp | |
| Chestplate | Áo giáp | |
| Leggings | Quần giáp | |
| Boots | Giày | |
| Ring | Nhẫn | |
| Bracelet | Vòng tay | |
| Necklace | Dây chuyền | |
| Unidentified X | X chưa giám định | `Mũ giáp chưa giám định` |
| Untradable Item | Vật phẩm không thể giao dịch | |
| Quest Item | Vật phẩm nhiệm vụ | |
| Durability | Độ bền | |
| Powder | Bột | `Bột Sấm II`. Powder Specials = Đặc kỹ bột |
| Powder Slots / Sockets | Ô bột | |
| Ingredient | Nguyên liệu | Crafting ingredient (mob drops etc.) |
| Material | Vật liệu | Gathered raw material. Keep the two apart |
| Ingredient Pouch | Túi nguyên liệu | |
| Tome / Mastery Tome | Bí kíp / Bí kíp tinh thông | *Bí kíp* is the word Vietnamese players already use for a book that grants power |
| Charm | Bùa | |
| Aspect | Tinh hoa | Raid reward that changes an ability. Not "khía cạnh" (sounds abstract) |
| Crafted Item | Vật phẩm chế tạo | `Crafted by` = Chế tạo bởi (but crafted item *names* stay as the player wrote them) |
| Mount | Thú cưỡi | |
| Pet | Thú cưng | |

### Item tiers (rarity)

Adjective after the noun: `Vật phẩm Thần thoại`, `Tinh hoa Huyền thoại`.

| English | vi_vn | Notes |
|---|---|---|
| Normal | Thường | |
| Unique | Độc nhất | |
| Rare | Hiếm | |
| Legendary | Huyền thoại | |
| Fabled | Truyền kỳ | |
| Mythic | Thần thoại | Item names (Mythic included) are translated like the other languages do, with a category word (Giáo, Dao găm, Cung, Đũa phép, Relik…); the in-game item-name setting can show or hide the original |
| Set | Bộ | `Vật phẩm bộ`, `Thưởng bộ` |
| Crafted | Chế tạo | |
| Shiny | Shiny | Item prefix and tracker system stay English, as es |
| tier (as in `{~} tier`, Tier IV) | bậc | `bậc {~}`, `Bột bậc IV`. Not "cấp", which is Level |

## Elements

Capitalised inside stat labels, as in Spanish: `Sát thương Lửa`, `Phòng thủ Gió`.

| English | vi_vn | Notes |
|---|---|---|
| Earth | Đất | |
| Thunder | Sấm | |
| Water | Nước | |
| Fire | Lửa | |
| Air | Gió | Not "Không khí": Vietnamese games call the air element *Gió* |
| Neutral | Trung tính | `Sát thương trung tính` |
| Elemental | Nguyên tố | `Sát thương nguyên tố` |
| element / elements / all elements | nguyên tố / nguyên tố / mọi nguyên tố | |

## Skill points (attributes)

`Skill Point` and `Ability Point` are two different systems; keep them apart.

| English | vi_vn | Notes |
|---|---|---|
| Skill Point | Điểm thuộc tính | The five stats below |
| Ability Point | Điểm kỹ năng | Spent in the ability tree |
| Ability Tree | Cây kỹ năng | |
| Ability | Kỹ năng | |
| Strength | Sức mạnh | |
| Dexterity | Khéo léo | Precision, crits. **Not** "Nhanh nhẹn" |
| Intelligence | Trí tuệ | |
| Defence / Defense | Phòng thủ | |
| Agility | Nhanh nhẹn | Evasion, dodging. **Not** "Khéo léo". The two are easy to swap: check before you write |
| Min. Strength (etc.) | Sức mạnh tối thiểu | Requirement lines |
| Unassigned Skill Points | Điểm thuộc tính chưa phân bổ | |
| Unused Ability Points | Điểm kỹ năng chưa dùng | |

## Stat lines (identifications)

Damage stats are built from pieces. Pattern: **Sát thương + [đánh thường / phép] + [nguyên tố / Element]**, then the modifier.

| Piece | vi_vn | Example |
|---|---|---|
| Damage | Sát thương | |
| Main Attack | Đánh thường | `Main Attack Damage` = Sát thương đánh thường. es "ataque principal"; Vietnamese players say *đánh thường* |
| Spell | Phép | `Spell Damage` = Sát thương phép |
| Element X | X | `Fire Spell Damage` = Sát thương phép Lửa |
| Elemental (combined) | nguyên tố (at the end) | `Elemental Main Attack Damage` = Sát thương đánh thường nguyên tố |
| `%` (item line) | ` %` | `Sát thương Lửa %` |
| `Raw` (item line) | ` (cố định)` | `Sát thương Lửa (cố định)` |
| `X:%` (ability tree line) | `X (%):` | `Air Damage:%` = Sát thương Gió (%): |
| Defence (elemental) | Phòng thủ X / Phòng thủ nguyên tố | |

| English | vi_vn | Notes |
|---|---|---|
| Health | Máu | Not "sức khỏe". HP in everyday speech |
| Health Regen | Hồi máu | |
| Healing | Lượng hồi máu | The amount healed, in ability lines. `Heals:` = Số lần hồi máu, `Total Heal` = Tổng hồi máu |
| Healing Efficiency | Hiệu quả hồi máu | |
| Overhealth | Máu vượt mức | Extra health above the maximum |
| Mana | Mana | Not "ma lực": players say mana |
| Max Mana | Mana tối đa | |
| Mana Regen | Hồi mana | |
| Mana Cost | Tiêu hao mana | `Cost` alone = Tiêu hao |
| Life Steal | Hút máu | |
| Mana Steal | Hút mana | |
| Critical Damage | Sát thương chí mạng | |
| Attack Speed | Tốc độ đánh | |
| DPS | Sát thương mỗi giây | as es; the demo line keeps "DPS" |
| Main Attack Range | Tầm đánh thường | |
| Walk Speed | Tốc độ di chuyển | |
| Sprint / Sprint Regen | Chạy nước rút / Hồi chạy nước rút | Minecraft's Vietnamese word for sprinting |
| Jump Height | Độ cao nhảy | Mount stat short form: Sức nhảy |
| Knockback | Đẩy lùi | Minecraft vi word |
| Thorns | Gai góc | Minecraft vi word for the Thorns enchantment |
| Reflection | Phản xạ | |
| Poison | Độc | |
| Exploding | Nổ tung | |
| Stealing | Trộm ngọc lục bảo | |
| Loot / Loot Bonus | Chiến lợi phẩm / Thưởng chiến lợi phẩm | `Lootrun` itself stays English |
| Loot Quality | Chất lượng chiến lợi phẩm | |
| XP Bonus | Thưởng XP | Players say XP |
| Combat / Gathering Experience | Kinh nghiệm chiến đấu / Kinh nghiệm thu thập | |
| Gathering Speed | Tốc độ thu thập | |
| Slow Enemy | Làm chậm địch | |
| Weaken Enemy | Làm yếu địch | |
| Cost | Tiêu hao | |
| Cooldown | Hồi chiêu | |
| Range | Tầm xa | |
| Area of Effect | Vùng ảnh hưởng | |
| Duration | Thời lượng | |
| Charges | Số lượt | Uses. See Charge (spell) |
| Radius | Bán kính | |
| Per Hit | Mỗi đòn | |
| Damage Multiplier | Hệ số sát thương | |
| Perfect / Defective | Hoàn hảo / Khiếm khuyết | Item roll quality |
| Combat Level | Cấp chiến đấu | |
| Crafting Level | Cấp chế tạo | |
| Level / Lv. / Lv. Min | Cấp / Cấp / Cấp tối thiểu | No abbreviation: "Cấp" is already short |
| Block (distance) | khối | `{~} khối` |
| second(s) | giây | |
| to Allies / to Enemies | cho đồng minh / lên kẻ địch | |

### Scale (two meanings)

| English | vi_vn | Notes |
|---|---|---|
| Scale (stat weighting: Main Scale, Damage Scale, Raid Scale…) | Độ phù hợp | `Độ phù hợp chính`, `Độ phù hợp sát thương`, `Độ phù hợp cận chiến`, `Độ phù hợp Bí Thuật Sư`. es "afinidad". This is the weight that rates how good an item roll is for a build |
| scale (size multiplier in a tool/setting) | tỉ lệ | Not the stat. Do not unify |

### Attack speed

| English | vi_vn |
|---|---|
| Super Fast | Siêu nhanh |
| Very Fast | Rất nhanh |
| Fast | Nhanh |
| Normal | Bình thường |
| Slow | Chậm |
| Very Slow | Rất chậm |
| Super Slow | Siêu chậm |

## Status effects and ability terms

| English | vi_vn | Notes |
|---|---|---|
| Slow (action) / Slowness (effect) | Làm chậm / Chậm chạp | *Chậm chạp* is Minecraft's Vietnamese name for the effect |
| Blind / Blindness | Mù | |
| Weaken | Suy yếu | |
| Stun | Choáng | |
| Freeze | Đóng băng | |
| Burn | Thiêu đốt | |
| Bleed / Bleeding | Chảy máu | |
| Poison | Độc | |
| Buff | Cường hóa | `Thời lượng cường hóa`. Not "thưởng" (that is Bonus) |
| Shield | Khiên | |
| Heal (generic) | Hồi máu | The Mage spell is *Chữa lành* (see Base spells) |
| Effect | Hiệu ứng | |
| Clone / Mirror / Mirage / Shadow Clone | Phân thân / Phân thân gương / Phân thân ảo ảnh / Phân thân bóng tối | Vietnamese has no plural; the same word serves singular and plural |
| Tricks | Mánh khóe | |
| Mark | Dấu ấn | |
| Trap | Bẫy | `Bẫy bazan` (Basaltic Traps) |
| Puppet | Con rối | Shaman |
| Effigy | Hình nhân | |
| Mask (of the Lunatic / Fanatic / Heretic) | Mặt nạ (Kẻ Điên / Kẻ Cuồng Tín / Kẻ Dị Giáo) | |
| Zealot | Cuồng Nhiệt | `Zealot's Bolt` = Mũi tên Cuồng Nhiệt. Kept apart from Fanatic = Cuồng Tín |
| Hound | Chó săn | |
| Crow | Quạ | |
| Snake / Serpent | Rắn | `Rắn Sấm`, `Rắn Lửa` |
| Hummingbird | Chim ruồi | |
| Orb | Quả cầu | |
| Knife / Knives | Dao | |
| Flint Arrow | Tên đá lửa | |
| Holy Power | Thánh lực | |
| Ultimate Meter | Thanh tuyệt kỹ | Ultimate = tuyệt kỹ |
| Mana Bank | Kho mana | |
| Blood Pool | Bể máu | |
| Summons | Vật triệu hồi | |
| Riftspawn | Sinh vật khe nứt | |
| Mantle | Áo choàng | `Áo choàng của Bovemist`. Same word as the cosmetic cape |
| Unstable / Crystallized / Shining | Bất ổn / Kết tinh / Rực sáng | |
| Weathering / Corroded / Static / Surge | Phong hóa / Bị ăn mòn / Tĩnh điện / Bộc phát | |
| Contaminate(d) | Làm ô nhiễm / Bị ô nhiễm | Not "tha hóa", which is Corruption |
| Enkindle(d) | Châm lửa / Bị châm lửa | |
| Drained / Noxious / Whipped / Transfused | Kiệt sức / Độc hại / Bị quất / Được truyền máu | |
| Corrupted (status) | Bị tha hóa | |
| Provoked / Confused / Discombobulated | Bị khiêu khích / Hỗn loạn / Choáng váng | |
| Guardian Angel | Thiên thần hộ mệnh | |
| Twilight Zone | Vùng chạng vạng | |
| Abomination | Quái dị | |
| Possessive fragments (`Mask's`, `Totem's`) | của mặt nạ / của Totem | They sit inside a sentence built around them; Vietnamese puts "của X" after the noun |

## Professions

| English | vi_vn | Notes |
|---|---|---|
| Profession | Nghề | |
| Tailoring | May mặc | |
| Jeweling | Kim hoàn | |
| Weaponsmithing | Rèn vũ khí | |
| Armouring | Rèn giáp | |
| Woodworking | Nghề mộc | |
| Cooking | Nấu ăn | |
| Alchemism | Giả kim | |
| Scribing | Chép bùa | Makes scrolls. es "Escribanía"; "Chép bùa" says what the profession produces |
| Woodcutting | Đốn gỗ | `Cấp đốn gỗ tối thiểu` |
| Mining | Khai khoáng | |
| Fishing | Câu cá | |
| Farming | Trồng trọt | |
| Gathering | Thu thập | |
| Crafting | Chế tạo | `Trạm chế tạo` |
| Gathering Totem | Totem thu thập | See Totem below |

## Currency and trading

| English | vi_vn | Notes |
|---|---|---|
| Emerald | Ngọc lục bảo | Minecraft's Vietnamese name |
| Emerald Block | Khối ngọc lục bảo | Item name |
| Liquid Emerald | Ngọc lục bảo lỏng | Abbreviations players type (EB, LE) stay as they are |
| Emerald Pouch | Túi ngọc lục bảo | |
| emeralds (guild resource) | ngọc lục bảo | Guild/territory emeralds are a separate resource from your wallet. zh_tw names them differently ("gems"); es, pt and vi keep "emerald" because the English does. Make the context clear (`ngọc lục bảo của bang hội`) when a line could be read either way |
| Trade Market | Chợ giao dịch | |
| Bank | Ngân hàng | |
| Sell / Buy | Bán / Mua | |
| Price | Giá | |

## Content types

| English | vi_vn | Notes |
|---|---|---|
| Quest | Nhiệm vụ | |
| Mini-Quest | Nhiệm vụ nhỏ | |
| Storyline | Cốt truyện chính | |
| Content Book | Sách nội dung | |
| Discovery (Territorial / World / Secret) | Khám phá (Lãnh thổ / Thế giới / Bí mật) | List headers: `Lãnh thổ:`, `Thế giới:`, `Bí mật:` |
| Area Discovered: | Đã khám phá khu vực: | |
| Cave | Hang động | |
| Dungeon | Hầm ngục | Dungeon names are translated (as es) |
| Corrupted Dungeon | Hầm ngục tha hóa | |
| Raid | Đột kích | Raid names translated as es. General "raid/raider" in dialogue = cuộc đột kích / kẻ cướp phá |
| Lootrun | Lootrun | **Stays English** (es too). `Trại Lootrun` |
| World Event | Sự kiện thế giới | |
| Boss | Trùm | `Tế đàn trùm` (Boss Altar), `Thanh máu trùm` (boss bar) |
| Mob | Quái | |
| Party | Tổ đội | `Tìm tổ đội` |
| Daily Objective | Mục tiêu hằng ngày | |
| Daily Reward | Phần thưởng hằng ngày | |
| Bomb Bell | Chuông bom | |
| Crates | Rương | |
| Part I/II | phần I/II | After a comma: `Sứ giả, phần I` |

### Quest names

Translate them meaningfully, like es does, and keep the pun when you can (`Cowfusion` = *Bò rối* from *bối rối*, `The Worm Holes` = *Lỗ sâu*, `Celebrations in Smoke` = *Lễ hội tan thành mây khói*, `A Grave Mistake` = *Sai lầm chết người*, `Stable Story` = *Chuyện chuồng ngựa*). The full list is in `quest-name.json`; **every other place that shows a quest name must reuse it exactly**.
Names the Spanish keeps English stay English inside the title: `Dwarves và Doguns, phần I`, `Rắc rối ở Tempo Town`, `Cơn đói của Gerts, phần I`.

## Lootrun

| English | vi_vn | Notes |
|---|---|---|
| Lootrun | Lootrun | |
| Challenge | Thử thách | |
| Beacon | Đèn hiệu | `Đèn hiệu tím` |
| Boon | Ân huệ | |
| Curse (lootrun stat) | Cường hóa địch | Enemy buff counter, as es ("refuerzo enemigo"). The ability `Curse` (Shaman) is **Lời nguyền** |
| Mission | Sứ mệnh | Kept away from Quest = Nhiệm vụ |
| Trial | Thử luyện | Kept away from Challenge = Thử thách |
| Pulls | Lượt quay | `Lượt quay phần thưởng` |
| Reroll | Quay lại | `quay lại đèn hiệu`, `quay lại phần thưởng cuối` |
| End Reward | Phần thưởng cuối | |
| Sacrifice | Hiến tế | |
| Elite | Tinh anh | `Lootrun: Tinh anh II` |
| Potency | Hiệu lực | |
| Time Bonus | Thưởng thời gian | |

## Raid

| English | vi_vn | Notes |
|---|---|---|
| Raid | Đột kích | |
| Gambit | Canh bạc | `Canh bạc đột kích` |
| Raid Keeper | Raid Keeper | NPC name, stays English (es) |

## Guild

| English | vi_vn | Notes |
|---|---|---|
| Guild | Bang hội | Short form *bang* is fine in tight UI: `Ngân hàng bang` |
| Guild Bank | Ngân hàng bang hội | |
| Guild Tower | Tháp bang hội | |
| War (guild system) | Chiến tranh bang hội | General "war" in dialogue = chiến tranh |
| Territory | Lãnh thổ | |
| Territory Defences | Phòng thủ lãnh thổ | |
| HQ | Tổng hành dinh | |
| Season Rating | Điểm mùa giải | |
| Owner | Chủ bang | Guild rank |
| Chief | Thủ lĩnh | Guild rank. **Not "Trùm"**, which is Boss |
| Strategist | Chiến lược gia | |
| Captain | Đội trưởng | |
| Recruiter | Người tuyển mộ | |
| Recruit | Tân binh | Guild rank and Fruma army recruit alike (`Tân binh của nữ hoàng`) |
| Ore / Wood / Fish / Crops | Quặng / Gỗ / Cá / Nông sản | Territory resources |

## Totem (two meanings)

| English | vi_vn | Notes |
|---|---|---|
| Totem (Shaman 1st spell) | Totem | Ability name. Vietnamese players already say totem; "vật tổ" reads as anthropology |
| Mob Totem / Gathering Totem | Totem quái / Totem thu thập | The placeable items. Same word (es also uses one word); zh_tw splits them, we don't need to |
| Totem Pole | Cột totem | Discovery name |

## World and lore

| English | vi_vn | Notes |
|---|---|---|
| Corruption | Sự Tha Hóa / Tha Hóa | The faction/force in the story. Capitalised when it is the force. Ability term `Corruption` = Tha Hóa |
| Corrupted / the corrupted (mobs) | bị tha hóa / kẻ bị tha hóa | `Ngôi làng bị tha hóa`. Not "ô nhiễm" (that is Contaminated) |
| Decay (Gavel's calamity) | Sự Mục Rữa | Capitalised, as es. General "decay/decaying" = mục nát, thối rữa |
| Sovereign | Quân Chủ | `Quân Chủ Zhiraok` |
| Her Majesty | Nữ hoàng Bệ hạ | Speaker name in full; in a sentence *Bệ hạ* |
| the Cursed (Fruma) | những Kẻ Bị Nguyền | `Kẻ bị nguyền` (quest *The Cursed One*) |
| Inquisitor | Quan thẩm tra | |
| Gendarme | Hiến binh | `Lính hiến binh`, `Chỉ huy hiến binh` |
| Realm of Light / Light | Cõi Ánh Sáng / Ánh Sáng | |
| Realm of Darkness / Dark | Cõi Bóng Tối / Bóng Tối | |
| Wanderer | Kẻ Lang Thang | `Kẻ Lang Thang` |
| Vagabond | Kẻ Phiêu Bạt | Kept apart from Wanderer |
| Dwarf / Dwarves / Dwarven | Người lùn / Người lùn / của người lùn | In dialogue and descriptions. The quest title keeps `Dwarves` (as es) |
| Corkian | Corkian | Stays English |
| The Order of the Grook | Hội Grook | Quest title and dialogue alike |
| Leaves in the Wind | Lá Trong Gió | Fruma resistance |
| Rune | Phù văn | `Phù văn Olmic` |
| Item Identifier | Người giám định | NPC job |
| Powder Master | Bậc thầy bột | NPC job |
| Blacksmith / Merchant | Thợ rèn / Thương nhân | |

## Common UI words

| English | vi_vn | Notes |
|---|---|---|
| Settings | Cài đặt | |
| Left Click / Right Click | Nhấp chuột trái / Nhấp chuột phải | `Shift + nhấp chuột phải` |
| Click to Open | Nhấp để mở | |
| Confirm / Cancel | Xác nhận / Hủy | |
| Back / Close / Done | Quay lại / Đóng / Xong | |
| Next Page / Previous Page | Trang sau / Trang trước | |
| Search / Filter / Sort | Tìm kiếm / Lọc / Sắp xếp | |
| Unlock / Unlocked | Mở khóa / Đã mở khóa | |
| Locked | Bị khóa | |
| Equipped | Đã trang bị | |
| Reward(s) | Phần thưởng | |
| Requirement(s) | Yêu cầu | `Bạn chưa đủ điều kiện` (You do not meet the requirements) |
| Level | Cấp | |
| Tracker (quest tracker) | Theo dõi nhiệm vụ | |
| Nameplate | Bảng tên | |
| Tooltip | Khung thông tin | |
| Translation panel | Bảng dịch | Mod UI |
| TIP: | MẸO: | |
| Upload / Download | Tải lên / Tải xuống | |
| On / Off | Bật / Tắt | |

## Always keep in English

- **Place names** (Ragni, Detlas, Troms…): handled by the `{p}` placeholder.
- **People's names** and made-up monster names the Spanish leaves alone.
- **Lootrun**, **Relik**, **Shiny**, **Corkian**, **Major ID**, **Totem** (the spell).
- Base spell names in ability-tree stat lines (`Tiêu hao Teleport:`), see the rule at the top.
- Ingredients and materials inside square brackets in quest dialogue (`[{~} Carp Meat]`).
- What other players type in chat: never translated.

## Want to change a term?

Open a PR that changes this file and explains why, and change the translation files that use the term in the same PR, so the glossary and the corpus never disagree.
