# Glossário do português brasileiro (pt_br)

> This file says **how to translate**. For **what things are**, see [World and story](docs/world-and-story.md).
> It is the pt_br glossary of its own. It is **not** a conversion of `GLOSSARY.md` (zh_tw) or of the Spanish files:
> the Spanish (es_es) corpus is our reviewed reference for *what* gets translated and *what stays English*,
> but the wording here is Brazilian Portuguese.

Every recurring game term goes through this table. When a word is not sure, look it up here and **do not invent a second one**.
**To change a term, change it here first**, then change the translation files that use it, so everyone moves together.
Use Ctrl+F.

## General rules

- **Brazilian, not European, Portuguese**: *você*, *mouse*, *tela*, *baixar*, *arquivo*, *salvar*, the way Brazilian players talk. Never *ecrã*, *rato*, *ficheiro*, *guardar* (in the sense of save), *tu* conjugations.
- **Short.** Most strings sit in tooltips, item lists and a dialogue box of fixed width. Prefer the short natural word.
- **Placeholders stay exactly as they are**: `{#}` `{~}` `{~1}` `{p}` `{u}`, same number of line breaks. `{cN}…{/}` colour markers are optional but welcome. If you reorder a sentence that has two `{~}`, number them (`{~1}`, `{~2}`) so the numbers do not land in the wrong slots.
- **Follow es_es on what stays English.** If the Spanish keeps a name in English (person, place, item, monster with a made-up name), keep it in English too. Place names are handled by `{p}` automatically.
- **Sentence case** for UI labels (`Dano de ataque principal`, not `Dano De Ataque Principal`). Element names keep a capital inside stat labels, as in Spanish (`Dano de Fogo`).
- **Spell names**: in the stat lines of the ability tree (`ability-labels.json`, e.g. `Teleport Cost:`) the base spell name **stays English** (`Custo de Teleport:`), exactly like es_es. In item stat lines (`ui-labels.json`, e.g. `Teleport Cost`) it is **translated** (`Custo de Teletransporte`). The two files are matched against different screens; do not unify them.
- Avoid symbols the game never uses (full-width brackets, `⋯`, `～`). Use `…` for an ellipsis and plain `-`/`—`.

## Classes

Class names are always "**main class/skin**", with a plain slash and no spaces.

| English | pt_br | Notes |
|---|---|---|
| Mage/Dark Wizard | Mago/Feiticeiro sombrio | |
| Archer/Hunter | Arqueiro/Caçador | |
| Warrior/Knight | Guerreiro/Cavaleiro | |
| Assassin/Ninja | Assassino/Ninja | |
| Shaman/Skyseer | Xamã/Vidente celeste | |
| Class | Classe | |
| Class Type | Tipo de classe | |

### Archetypes

Label form: `Arquétipo X`, `Arquétipo X:`, `Mín. de arquétipo X:` (as es_es `Mín. de arquetipo X:`).

| English | pt_br | Notes |
|---|---|---|
| Archetype | Arquétipo | |
| Light Bender / Lightbender | Dobraluz | Mage. Like es "Doblaluz" |
| Arcanist | Arcanista | Mage |
| Riftwalker | Andarilho da Fenda | Mage. "Andarilho" is reserved for this; see Wanderer |
| Fallen | Caído | Warrior |
| Battle Monk | Monge de batalha | Warrior |
| Paladin | Paladino | Warrior |
| Sharpshooter | Atirador certeiro | Archer. Not "de elite": Elite is a Lootrun word |
| Trapper | Armadilheiro | Archer |
| Boltslinger | Lança-raios | Archer |
| Shadestepper | Passa-sombras | Assassin |
| Trickster | Trapaceiro | Assassin |
| Acrobat | Acrobata | Assassin |
| Summoner | Invocador | Shaman |
| Ritualist | Ritualista | Shaman |
| Acolyte | Acólito | Shaman |

### Base spells

| Order | Mage | Archer | Warrior | Assassin | Shaman |
|---|---|---|---|---|---|
| 1st | Cura (Heal) | Tempestade de flechas (Arrow Storm) | Pancada (Bash) | Ataque giratório (Spin Attack) | Totem |
| 2nd | Teletransporte (Teleport) | Fuga (Escape) | Investida (Charge) | Desvanecer (Vanish) | Arrasto (Haul) |
| 3rd | Meteoro (Meteor) | Flecha-bomba (Arrow Bomb / Bomb Arrow) | Gancho (Uppercut) | Golpe múltiplo (Multihit) | Aura |
| 4th | Serpente de gelo (Ice Snake) | Escudo de flechas (Arrow Shield) | Grito de guerra (War Scream) | Bomba de fumaça (Smoke Bomb) | Desenraizar (Uproot) |

- Ordinals: `1st/2nd/3rd/4th Spell` = `1º/2º/3º/4º feitiço` (`Custo do 1º feitiço`).
- `Charge` (warrior spell) is **Investida**, so it never collides with `Charges` = **Cargas** (uses of an ability/item).
- Remember the rule above: in `ability-labels.json` these names stay English.

## Weapons and gear

| English | pt_br | Notes |
|---|---|---|
| Weapon | Arma | |
| Armour / Armor | Armadura | |
| Accessory | Acessório | |
| Gear / Equipment | Equipamento | |
| Item | Item (pl. itens) | es uses "objeto"; Brazilian players say "item" |
| Spear | Lança | Warrior |
| Dagger | Adaga | Assassin |
| Bow | Arco | Archer |
| Wand | Varinha | Mage. Not "cajado" (that would be a staff) |
| Relik | Relik | Shaman. Invented word, stays as is (es too) |
| Helmet | Capacete | |
| Chestplate | Peitoral | |
| Leggings | Calças | |
| Boots | Botas | |
| Ring | Anel | |
| Bracelet | Bracelete | |
| Necklace | Colar | |
| Unidentified X | X não identificado(a) | `Capacete não identificado`, `Varinha não identificada` |
| Untradable Item | Item não negociável | |
| Quest Item | Item de missão | |
| Durability | Durabilidade | |
| Powder | Pó | `Pó de Trovão II`. Powder Specials = Especiais de pó |
| Powder Slots / Sockets | Encaixes de pó | |
| Ingredient | Ingrediente | Crafting ingredient (mob drops etc.) |
| Material | Material | Gathered raw material. Keep the two apart |
| Ingredient Pouch | Bolsa de ingredientes | |
| Tome / Mastery Tome | Tomo / Tomo de maestria | |
| Charm | Amuleto | |
| Aspect | Aspecto | |
| Crafted Item | Item fabricado | `Crafted by` = Fabricado por (but crafted item *names* stay as the player wrote them) |
| Mount | Montaria | |
| Pet | Mascote | |

### Item tiers (rarity)

Adjective after the noun, agreeing in gender: `Item mítico`, `Aspecto mítico`.

| English | pt_br | Notes |
|---|---|---|
| Normal | Normal | |
| Unique | Único | |
| Rare | Raro | |
| Legendary | Lendário | |
| Fabled | Fabuloso | as es |
| Mythic | Mítico | Item names (Mythic included) are translated like the other languages do, with a category word (Lança, Adaga, Arco, Varinha, Relik…); the in-game item-name setting can show or hide the original |
| Set | De conjunto | `Item de conjunto`, `Bônus de conjunto` |
| Crafted | Fabricado | |
| Shiny | Shiny | Item prefix and tracker system stay English, as es |
| tier (as in `{~} tier`, Tier IV) | grau | `grau {~}`, `Pó de grau IV`. Not "nível", which is Level |

## Elements

| English | pt_br | Notes |
|---|---|---|
| Earth | Terra | |
| Thunder | Trovão | |
| Water | Água | |
| Fire | Fogo | |
| Air | Ar | |
| Neutral | Neutro | `Dano neutro` |
| Elemental | Elemental | |
| element / elements / all elements | elemento / elementos / todos os elementos | |

## Skill points (attributes)

`Skill Point` and `Ability Point` are two different systems; keep them apart.

| English | pt_br | Notes |
|---|---|---|
| Skill Point | Ponto de atributo | The five stats below. es "punto de atributo" |
| Ability Point | Ponto de habilidade | Spent in the ability tree |
| Ability Tree | Árvore de habilidades | |
| Ability | Habilidade | |
| Strength | Força | |
| Dexterity | Destreza | |
| Intelligence | Inteligência | |
| Defence / Defense | Defesa | |
| Agility | Agilidade | |
| Min. Strength (etc.) | Força mín. | Requirement lines |
| Unassigned Skill Points | Pontos de atributo livres | |
| Unused Ability Points | Pontos de habilidade livres | |

## Stat lines (identifications)

Damage stats are built from pieces. Pattern: **Dano + [de ataque principal / de feitiço] + [elemental / de Elemento]**, then the modifier.

| Piece | pt_br | Example |
|---|---|---|
| Damage | Dano | |
| Main Attack | Ataque principal | `Main Attack Damage` = Dano de ataque principal |
| Spell | Feitiço | `Spell Damage` = Dano de feitiço. Not "magia" |
| Element X | de X | `Fire Spell Damage` = Dano de feitiço de Fogo |
| Elemental (combined) | elemental (after the noun) | `Elemental Main Attack Damage` = Dano de ataque principal elemental |
| `%` (item line) | ` %` | `Dano de Fogo %` |
| `Raw` (item line) | ` (valor fixo)` | `Dano de Fogo (valor fixo)` |
| `X:%` (ability tree line) | `Porcentagem de x:` | `Air Damage:%` = Porcentagem de dano de Ar: |
| Defence (elemental) | Defesa de X / Defesa elemental | |

| English | pt_br | Notes |
|---|---|---|
| Health | Vida | Not "saúde" |
| Health Regen | Regeneração de vida | |
| Healing Efficiency | Eficiência de cura | |
| Overhealth | Sobrevida | Extra health above the maximum |
| Mana | Mana | Feminine, no accent (es "maná") |
| Max Mana | Mana máxima | |
| Mana Regen | Regeneração de mana | |
| Mana Cost | Custo de mana | |
| Life Steal | Roubo de vida | |
| Mana Steal | Roubo de mana | |
| Critical Damage | Dano crítico | |
| Attack Speed | Velocidade de ataque | |
| DPS | Dano por segundo | as es; the demo line keeps "DPS" |
| Main Attack Range | Alcance do ataque principal | |
| Walk Speed | Velocidade de movimento | |
| Sprint / Sprint Regen | Corrida / Regeneração de corrida | |
| Jump Height | Altura de salto | Mount stat short form: Salto |
| Knockback | Repulsão | Minecraft pt-BR word |
| Thorns | Espinhos | Minecraft pt-BR word |
| Reflection | Reflexo | |
| Poison | Veneno | |
| Exploding | Explosão | |
| Stealing | Roubo de esmeraldas | |
| Loot / Loot Bonus | Saque / Bônus de saque | `Lootrun` itself stays English |
| Loot Quality | Qualidade do saque | |
| XP Bonus | Bônus de XP | Players say XP |
| Combat / Gathering Experience | Experiência de combate / de coleta | |
| Gathering Speed | Velocidade de coleta | |
| Slow Enemy | Desacelerar inimigo | |
| Weaken Enemy | Enfraquecer inimigo | |
| Cost | Custo | |
| Cooldown | Recarga | |
| Range | Alcance | |
| Area of Effect | Área de efeito | |
| Duration | Duração | |
| Charges | Cargas | Uses. See Charge (spell) |
| Radius | Raio | |
| Per Hit | Por acerto | |
| Perfect / Defective | Perfeito / Defeituoso | Item roll quality |
| Combat Level | Nível de combate | |
| Crafting Level | Nível de fabricação | |
| Lv. / Lv. Min | Nv. / Nv. mín. | |

### Scale (two meanings)

| English | pt_br | Notes |
|---|---|---|
| Scale (stat weighting: Main Scale, Damage Scale, Raid Scale…) | Afinidade | `Afinidade principal`, `Afinidade de dano`, `Afinidade corpo a corpo`. es "afinidad". This is the weight that rates how good an item roll is |
| scale (size multiplier in a tool/setting) | escala | Not the stat. Do not unify |

### Attack speed

Feminine, it agrees with *velocidade*.

| English | pt_br |
|---|---|
| Super Fast | Super-rápida |
| Very Fast | Muito rápida |
| Fast | Rápida |
| Normal | Normal |
| Slow | Lenta |
| Very Slow | Muito lenta |
| Super Slow | Superlenta |

(`super-` takes a hyphen before *r*, not before *l*: current spelling rules.)

## Status effects and ability terms

| English | pt_br | Notes |
|---|---|---|
| Slow / Slowness | Desaceleração / Lentidão | Slow = the action; Slowness = the effect |
| Blind / Blindness | Cegueira | |
| Weaken | Enfraquecimento | |
| Stun | Atordoamento | |
| Freeze | Congelamento | |
| Burn | Queimadura | |
| Bleed / Bleeding | Sangramento | |
| Poison | Veneno | |
| Buff | Fortalecimento | `Duração do fortalecimento`. Not "bônus" (that is Bonus) |
| Shield | Escudo | |
| Heal / Healing | Cura | |
| Effect | Efeito | |
| Clone / Mirror / Mirage / Shadow Clone | Clone / Clone espelhado / Clone-miragem / Clone sombrio | |
| Mark | Marca | |
| Trap | Armadilha | |
| Puppet | Marionete | Shaman |
| Effigy | Efígie | |
| Mask (of the Lunatic / Fanatic / Heretic) | Máscara (do Lunático / do Fanático / do Herege) | |
| Hound | Sabujo | |
| Crow | Corvo | |
| Snake / Serpent | Serpente | |
| Orb | Orbe | |
| Holy Power | Poder sagrado | |
| Ultimate Meter | Medidor de suprema | "Suprema" as BR games call an ultimate |
| Mana Bank | Banco de mana | |
| Blood Pool | Reserva de sangue | |
| Summons | Invocações | |
| Riftspawn | Cria da fenda | |
| Corrupted (status) | Corrompido | |
| Provoked / Confused / Discombobulated | Provocado / Confuso / Desnorteado | |
| Guardian Angel | Anjo da guarda | |
| Twilight Zone | Zona crepuscular | |

## Professions

| English | pt_br | Notes |
|---|---|---|
| Profession | Profissão | |
| Tailoring | Alfaiataria | |
| Jeweling | Joalheria | |
| Weaponsmithing | Forja de armas | |
| Armouring | Forja de armaduras | |
| Woodworking | Marcenaria | |
| Cooking | Culinária | |
| Alchemism | Alquimia | |
| Scribing | Caligrafia | Makes scrolls. es "Escribanía" has no good pt equivalent |
| Woodcutting | Corte de madeira | `Nv. mín. de corte de madeira` |
| Mining | Mineração | |
| Fishing | Pesca | |
| Farming | Agricultura | |
| Gathering | Coleta | |
| Crafting | Fabricação | `Estação de fabricação` |
| Gathering Totem | Totem de coleta | See Totem below |

## Currency and trading

| English | pt_br | Notes |
|---|---|---|
| Emerald | Esmeralda | |
| Emerald Block | Bloco de esmeralda | Item name |
| Liquid Emerald | Esmeralda líquida | |
| Emerald Pouch | Bolsa de esmeraldas | |
| emeralds (guild resource) | esmeraldas | Guild/territory emeralds are a separate resource from your wallet. zh_tw names them differently ("gems"); es and pt keep "esmeraldas" because the English does. Make the context clear (`esmeraldas da guilda`) when a line could be read either way |
| Trade Market | Mercado | |
| Bank | Banco | |
| Sell / Buy | Vender / Comprar | |
| Price | Preço | |

## Content types

| English | pt_br | Notes |
|---|---|---|
| Quest | Missão | |
| Mini-Quest | Minimissão | |
| Storyline | História principal | |
| Content Book | Livro de conteúdo | |
| Discovery (Territorial / World / Secret) | Descoberta (territorial / do mundo / secreta) | Lists use the plural adjective: `Territoriais:`, `Mundiais:`, `Secretas:` |
| Area Discovered: | Área descoberta: | |
| Cave | Caverna | |
| Dungeon | Masmorra | Dungeon names are translated (as es): `Esgotos Decrépitos` |
| Corrupted Dungeon | Masmorra corrompida | |
| Raid | Incursão | Raid names translated as es: `Ninho dos Grootslang`. General "raid/raider" in dialogue = ataque / saqueador |
| Lootrun | Lootrun | **Stays English** (es too). `Acampamento de Lootrun` |
| World Event | Evento do mundo | |
| Boss | Chefe | `Altar do chefe`, `Barra do chefe` |
| Mob | Mob / monstro | `mob` in UI text, `monstro` in prose |
| Party | Grupo | `Buscador de grupos` |
| Daily Objective | Objetivo diário | |
| Daily Reward | Recompensa diária | |
| Bomb Bell | Sino de bombas | |
| Crates | Caixas | |
| Part I/II | parte I/II | After a comma: `O enviado, parte I` |

### Quest names

Translate them meaningfully, like es does, and keep the pun when you can (`Lost Soles` = *Solas penadas*, `Cowfusion` = *Vacafusão*, `Celebrations in Smoke` = *Festa que virou fumaça*). The full list is in `quest-name.json`; **every other place that shows a quest name must reuse it exactly**.
Names the Spanish keeps English stay English inside the title: `Dwarves e Doguns, parte I`, `Problemas em Tempo Town`.

## Lootrun

| English | pt_br | Notes |
|---|---|---|
| Lootrun | Lootrun | |
| Challenge | Desafio | |
| Beacon | Sinalizador | `Sinalizador roxo`, `Sinalizador aqua vibrante`. es "baliza" sounds like parking in Brazil |
| Boon | Bênção | |
| Curse (lootrun stat) | Reforço inimigo | Enemy buff counter, as es ("refuerzo enemigo"). The ability `Curse` (Shaman) is **Maldição** |
| Mission | Tarefa | Kept away from Quest = Missão |
| Trial | Provação | |
| Pulls | Giros | `Giros de recompensa` |
| Reroll | Novo giro | `novo giro de sinalizador`, `novo giro da recompensa final` |
| End Reward | Recompensa final | |
| Sacrifice | Sacrifício | `sacrifício de recompensa final` |
| Elite | Elite | `Lootrun: Elite II` |
| Potency | Potência | |
| Time Bonus | Bônus de tempo | |

## Raid

| English | pt_br | Notes |
|---|---|---|
| Raid | Incursão | |
| Gambit | Aposta | `Apostas de incursão`, `Encaixe de aposta` |
| Raid Keeper | Raid Keeper | NPC name, stays English (es) |

## Guild

| English | pt_br | Notes |
|---|---|---|
| Guild | Guilda | Not "grêmio" (es "gremio"): Brazilian players say guilda |
| Guild Bank | Banco da guilda | |
| Guild Tower | Torre da guilda | |
| War (guild system) | Guerra (de guildas) | General "war" in dialogue = guerra too |
| Territory | Território | |
| Territory Defences | Defesas do território | |
| HQ | Sede | |
| Season Rating | Pontuação da temporada | |
| Owner | Dono | Guild rank |
| Chief | Líder | Guild rank. **Not "Chefe"**, which is Boss |
| Strategist | Estrategista | |
| Captain | Capitão | |
| Recruiter | Recrutador | |
| Recruit | Recruta | Guild rank and Fruma army recruit alike |
| Ore / Wood / Fish / Crops | Minério / Madeira / Peixes / Colheitas | Territory resources |

## Totem (two meanings)

| English | pt_br | Notes |
|---|---|---|
| Totem (Shaman 1st spell) | Totem | Ability name |
| Mob Totem / Gathering Totem | Totem de monstros / Totem de coleta | The placeable items. Same word in pt (es also uses one word); zh_tw splits them, we don't need to |
| Totem Pole | Totem | Discovery name |

## World and lore

| English | pt_br | Notes |
|---|---|---|
| Corruption | Corrupção | The faction/force in the story. Capitalised when it is the force: `a Corrupção` |
| Corrupted / the corrupted (mobs) | corrompido(s) | `Aldeia corrompida`. Not "corrupto" (that means bribe-taking) |
| Decay (Gavel's calamity) | a Decadência | Capitalised, as es. General "decay/decaying" = decadente, em decomposição |
| Sovereign | Soberano / Soberana | `Soberano Zhiraok`, `Soberana Soosu` |
| Her Majesty | Sua Majestade | |
| the Cursed (Fruma) | os Amaldiçoados | `O amaldiçoado` (quest *The Cursed One*) |
| Inquisitor | Inquisidor | |
| Gendarme | Gendarme | `Soldado gendarme`, `Comandante da Gendarmaria` |
| Realm of Light / Light | Reino da Luz / a Luz | |
| Realm of Darkness / Dark | Reino da Escuridão / a Escuridão | |
| Wanderer | Errante | `o Errante`. Not "Andarilho" (reserved for Riftwalker) |
| Vagabond | Vagabundo | Kept apart from Wanderer |
| Dwarf / Dwarves / Dwarven | Anão / Anões / anão | In dialogue and descriptions. The quest title keeps `Dwarves` (as es) |
| Corkian | Corkian | Stays English |
| The Order of the Grook | a Ordem do Grook | |
| Leaves in the Wind | Folhas ao Vento | Fruma resistance |
| Item Identifier | Identificador de itens | NPC job |
| Powder Master | Mestre do pó | NPC job |
| Blacksmith / Merchant | Ferreiro / Mercador | |

## Common UI words

| English | pt_br | Notes |
|---|---|---|
| Settings | Configurações | |
| Left Click / Right Click | Clique esquerdo / Clique direito | `Shift + clique direito` |
| Click to Open | Clique para abrir | |
| Confirm / Cancel | Confirmar / Cancelar | |
| Back / Close | Voltar / Fechar | |
| Next Page / Previous Page | Próxima página / Página anterior | |
| Search / Filter / Sort | Buscar / Filtrar / Ordenar | |
| Unlock / Unlocked | Desbloquear / Desbloqueado | |
| Locked | Bloqueado | |
| Equipped | Equipado | |
| Reward(s) | Recompensa(s) | |
| Requirement(s) | Requisito(s) | `Você não cumpre os requisitos` |
| Level | Nível | |
| Tracker (quest tracker) | Rastreador de missões | |
| Nameplate | Placa de nome | |
| Tooltip | Descrição | |
| TIP: | DICA: | |
| Upload / Download | Enviar / Baixar | |
| On / Off | Ligado / Desligado | |

## Always keep in English

- **Place names** (Ragni, Detlas, Troms…): handled by the `{p}` placeholder.
- **People's names** and made-up monster names the Spanish leaves alone.
- **Lootrun**, **Relik**, **Shiny**, **Corkian**, **Major ID**.
- Ingredients and materials inside square brackets in quest dialogue (`[{~} Carp Meat]`).
- What other players type in chat: never translated.

## Want to change a term?

Open a PR that changes this file and explains why, and change the translation files that use the term in the same PR, so the glossary and the corpus never disagree.
