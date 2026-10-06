# 简体中文专有名词对照表

> 这里讲**怎么翻**。这份表是 zh_cn 自己的术语表，**不是** `GLOSSARY.md`（zh_tw）的繁简转换——
> 同一个英文词在两边常常是不同的中文（Corruption：zh_tw「腐敗」／zh_cn「腐化」），
> 拿 zh_tw 的表套 zh_cn 会把整套正确的译法判成错的。

为了译名的标准话语一致性，专有名词翻译时请遵循下列表格，碰到不确定的词就查这里，**不要自己另外想一个**。
**要改某个词的译法，先改这里**，让所有人一起跟着改。另外，请善用 Ctrl+F 查询译名。

与 zh_tw 译法**刻意不同**的地方在备注里标了「与 zh_tw 不同」——这些是简中社区自己的惯例，不是漏转。

## 制作与采集

| 原文 | 译文 | 备注 |
|---|---|---|
| Tailoring | 裁缝 | |
| Jeweling | 珠宝 | |
| Weaponsmithing | 武器锻造 | 与 zh_tw 不同（鍛兵） |
| Armouring | 盔甲锻造 | 与 zh_tw 不同（鑄甲） |
| Woodworking | 木工 | |
| Cooking | 烹饪 | |
| Alchemism | 炼金 | |
| Scribing | 抄写 | |
| Fishing | 钓鱼 | |
| Woodcutting | 伐木 | |
| Mining | 采矿 | |
| Farming | 农耕 | |

## 职业内容相关

职业名一律「**主职/造型**」，中间用半形斜线。

| 原文 | 译文 | 备注 |
|---|---|---|
| Mage/Dark Wizard | 法师/黑暗巫师 | 与 zh_tw 不同（闇導士） |
| Archer/Hunter | 弓箭手/猎人 | 与 zh_tw 不同（弓手） |
| Warrior/Knight | 战士/骑士 | |
| Assassin/Ninja | 刺客/忍者 | |
| Shaman/Skyseer | 萨满/观星者 | |

职业基础技能：zh_cn 技能树这二十个**全部都翻**，照下表。

注意别跟装备提示那一列搞混：`ability-labels.json` 里的「X Cost:」是刻意保留英文名的
（`Bash 消耗:`），zh_tw／ja／ko／es 都一样——那一列换成中文名反而跟技能树以外的
画面对不起来。要翻的是技能树（`ability/*.json`）与词条名（`ui-labels.json`）这两处，
而且两处要一致。

| 原文 | 译文 | 备注 |
|---|---|---|
| Teleport | 传送 | |
| Heal | 治疗 | |
| Meteor | 陨石 | |
| Ice Snake | 冰蛇 | 技能树前置提示已有此译 |
| Arrow Storm | 箭矢风暴 | 不用「箭雨」——其余五个语言都是「箭矢＋风暴」 |
| Arrow Bomb | 箭矢炸弹 | |
| Arrow Shield | 箭盾 | |
| Escape | 脱身 | |
| War Scream | 战吼 | 战士技能，与公会战 War 无关 |
| Spin Attack | 旋转攻击 | |
| Smoke Bomb | 烟雾弹 | |
| Haul | 牵引 | |
| Uproot | 连根拔起 | |
| Bash | 重击 | |
| Charge | 冲锋 | |
| Uppercut | 上挑 | 不用「上勾拳」——那是拳击的勾拳，这招是武器上撩 |
| Multihit | 连击 | |
| Vanish | 隐身 | 不用「消失」——语料里当普通动词的「消失」有四十几处 |
| Totem | 图腾 | |
| Aura | 光环 | |

## 攻击速度

| 原文 | 译文 | 备注 |
|---|---|---|
| Super Fast | 极快 | 与 zh_tw 不同（超快） |
| Very Fast | 很快 | |
| Fast | 快速 | |
| Normal | 普通 | |
| Slow | 缓慢 | |
| Very Slow | 很慢 | |
| Super Slow | 极慢 | 与 zh_tw 不同（超慢） |

## 游戏系统

| 原文 | 译文 | 备注 |
|---|---|---|
| Crafting Level | 制作等级 | |
| Combat Level | 战斗等级 | |
| Class Type | 职业类型 | |
| Material | 原料 | 制作用的原料，和 Ingredient 一词相对 |
| Ingredient | 素材 | 制作用的素材，和 Material 一词相对 |
| Ingredient Pouch | 素材袋 | |
| Emerald Pouch | 绿宝石袋 | |
| Mastery Tome | 精通书卷 | Tome 为书卷 |
| Liquid Emeralds | 液态绿宝石 | |
| Item Identifier | 物品鉴定师 | NPC 职业，不是「鉴定道具」 |
| Lootrun | Lootrun | 保留原文 |
| Dungeon | 地牢 | 与 zh_tw 不同（地城） |
| Raid | 副本 | 与 zh_tw 不同（討伐戰）。指后期需要 4 人加入的副本；一般语意的 raid／assault 仍用「突袭」 |
| Quest | 任务 | |
| Mini-Quest | 迷你任务 | |
| Discovery | 发现 | |
| Secret Discovery | 秘密发现 | |
| World Event | 世界事件 | |
| Cave | 洞窟 | |
| Boss Altar | 首领祭坛 | |

## 词条相关

### Skill Point

| 原文 | 译文 | 备注 |
|---|---|---|
| Skill Point | 属性点 | Ability Point 为技能点，两者务必分清楚 |
| Strength | 力量 | |
| Dexterity | 灵巧 | |
| Intelligence | 智力 | 与 zh_tw 不同（智慧） |
| Defense | 防御 | |
| Agility | 敏捷 | |

### 攻击、防御属性相关

与 zh_tw 同一套组合式规则：属性 + 作用方式 + Damage + 作用乘区（Raw/%），
前两块可为空，Damage 一律译为伤害。例如 Elemental Spell Damage % 译作「元素法术伤害百分比」。

属性（词条中作「X属性」）：

| 原文 | 译文 | 备注 |
|---|---|---|
| Neutral | 中性 | 与 zh_tw 不同（無屬性）。技能树里的 Neutral Damage 同样译「中性伤害」 |
| Elemental | 元素 | |
| Earth | 地属性 | |
| Thunder | 雷属性 | |
| Water | 水属性 | |
| Fire | 火属性 | |
| Air | 气属性 | 与 zh_tw 不同（風） |

作用方式：

| 原文 | 译文 | 备注 |
|---|---|---|
| Spell | 法术 | |
| Main Attack | 普攻 | |

作用乘区：

| 原文 | 译文 | 备注 |
|---|---|---|
| % | 百分比 | |
| Raw | 固定值 | 与 zh_tw 不同（值）。如 Spell Cost Raw → 法术消耗固定值 |

不属于该分类方法的完整词条：

| 原文 | 译文 | 备注 |
|---|---|---|
| Attack Speed | 攻击速度 | |
| Main Attack Range | 普攻距离 | |
| Knockback | 击退 | 与 zh_tw 不同（擊退效果） |
| Critical Damage Bonus | 暴击伤害百分比 | 此词条没有 Raw，故置于此 |

### 生命与法力

| 原文 | 译文 | 备注 |
|---|---|---|
| Health | 生命 | |
| Health Regen | 生命恢复 | |
| Life Steal | 生命偷取 | 与 zh_tw 不同（生命竊取） |
| Healing Efficiency | 治疗效率 | |
| Mana Regen | 法力恢复 | Mana 一律译「法力」，与 zh_tw 不同（魔力） |
| Mana Steal | 法力偷取 | |
| Max Mana | 最大法力 | |
| Spell Cost | 法术消耗 | 序数形式译作「第X法术」（X 为小写中文数字） |

### 被动词条

| 原文 | 译文 | 备注 |
|---|---|---|
| Exploding | 爆炸 | |
| Poison | 中毒 | |
| Thorns | 近战反伤 | |
| Reflection | 远程反伤 | |

### 移动相关

| 原文 | 译文 | 备注 |
|---|---|---|
| Walk Speed | 移动速度 | |
| Sprint | 体力 | 与 zh_tw 不同（耐力） |
| Sprint Regen | 体力恢复 | |
| Jump Height | 跳跃高度 | |

### 经验与采集

| 原文 | 译文 | 备注 |
|---|---|---|
| Loot Bonus | 掉落加成 | Loot 一律译「掉落」，与 zh_tw 不同（寶物） |
| Loot Quality | 掉落品质 | |
| Stealing | 偷取绿宝石 | |
| XP Bonus | 经验加成 | |
| Gather XP Bonus | 采集经验 | |
| Gather Speed | 采集速度 | |

## 世界观相关

| 原文 | 译文 | 备注 |
|---|---|---|
| Corruption | 腐化 | **定译**。Corrupt／Corrupted／Corruption 整族都是「腐化」：腐化之石、腐化孢子、腐化投石手、腐化之泉、腐化宝箱。与 zh_tw 不同（腐敗），两边译法各自成立，不要互相搬 |
| Decay | 黯蚀 | **定译**。指 Gavel 那场灾厄：黯蚀之心（The Heart of the Decay）、黯蚀之山（Decaying Mountains）。一般语意的腐朽／枯萎与机制上的层数衰减不适用。**与 Corruption（腐化）是两个词，撞译会变成两个原文同一个中文** |
| Umbral | 本影 | **定译**。本影弩手（Umbral Arbalist）、本影精华（Umbral Essence）。「暗影」留给 Shadow |
| Shadow | 暗影 | **定译**。暗影吊坠（Shadow Pendant）。与 Umbral（本影）分开 |
| Ghastly Ghoul | 骇人食尸鬼 | **定译**。label.json 已有此译，任务行里不要留英文 |
| War（公会战系统） | 公会战 | 公会之间争夺领地的系统。一般语意的 war 仍译「战争」；War Scream 是战士技能「战吼」，与此无关 |
| Wars Won | 公会战胜场 | 个人资料上的统计行 |
| Territory | 领地 | 公会战争夺的单位 |

## 一律保留原文

翻了会跟其他玩家对不上话，或社群本来就讲英文。

- **地名**（Ragni、Detlas、Troms…）——用 `{p}` 占位符自动处理
- **装备与物品名称**——专有名词，翻了对不上 wiki 与交易市场
- **Lootrun**、**Guild** 等社群通用词（Raid 不在此列，见上面的对照表）
- **职业技能名**——目前大半保留英文，见「职业内容相关」一节的说明

## 想改某个词？

送 PR 改这个文件，说明为什么。改完之后**同时把相关译文文件一起改掉**，
否则表与实际译文会不一致。
