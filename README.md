# Forbidden Enchants (Paper 1.21.x)

![AI LOGO LOL](https://i.imgur.com/w8nRrWX.png)

Forbidden Enchants is a set of 53 original custom enchants for Paper (with more planned), designed to feel truly forbidden: many are borderline cheat-powerful, while others are deliberately cursed and punishing. Because this is a Paper plugin (not a datapack), it has access to stronger runtime hooks and mechanics than datapacks normally allow.

The core selling point is the admin GUI workflow: a creative-style browser for books/items, a structure injector editor that lets you add, remove, and tune loot injection on the fly for any structure (including datapack structures), and a librarian trade editor for custom forbidden-book offers at exact costs.

## Features

- Fancy `/fe` admin command with help/list output and colored feedback.
- `/fe gui` paged creative style inventory menu sorted by enchant type.
- `/fe injector` command suite for structure/vault injection setup (`help`, `status|list`, `enable`, `disable`, `add`, `set|chance`, `remove`, `clear`, `defaultchance|default`, `vault`, `gui|menu`).
- `/fe injector gui` visual editor for injector structure lists and per-structure chance tuning.
- `/fe librarian` command suite for librarian trade blend setup (`help`, `status|list`, `enable`, `disable`, `add`, `remove`, `clear`, `gui|menu`).
- `/fe librarian gui` visual editor with all-book + configured views, per-book chance, and cost tuning.
- Librarian blend mode keeps vanilla/datapack trade generation and appends configured forbidden-book offers by per-trade chance.
- `/fe toggles` GUI for per-enchant enable/disable controls (usage and chest/vault spawning), with aliases `settings` and `enchanttoggles`.
- Generate both enchant books and pre-enchanted gear items directly.
- Anvil application flow for helmets, chestplates, and boots.
- 53 custom enchants with level support where configured.
- Loot-table compatible book detection via `custom_model_data`.
- Loot-table compatible gear detection via `custom_model_data` fallback.
- Helmet-only balancing rules for the vision enchants.
- Mystery book/item generation with obfuscated text (`/fe mysterybook`, `/fe mysteryitem`).
- Mystery gear reveals its real enchant when equipped/used.
- Optional structure loot injector (config + command + GUI editor) for random forbidden books in selected structures.

## Loot Integration Paths

You can add Forbidden Enchants to world loot in two different ways:

1. Datapack/resourcepack-style loot table editing  
Use your own loot table JSONs (or the provided example) to explicitly place specific forbidden books/items in specific tables with your own fixed rules.

2. In-game injector system (`/fe injector` or `/fe injector gui`)  
Use plugin-side runtime injection to add random forbidden books into selected structures and trial vault rewards with configurable chances, without manually editing loot table files.

Use loot table editing when you want exact, deterministic entries.  
Use injector when you want fast setup and configurable randomization from in-game commands/GUI.

## Requirements

- Paper `1.21.x` (built for modern 1.21 API; target use-case is `1.21.11`).
- Java `21+` runtime for the server.
- Gradle `8.10+` (wrapper included; only needed to build from source).

## Build

```bash
./gradlew build -x test
```

Output jar:

- `build/libs/forbidden-enchants-1.0.0.jar`

## Install

1. Build or use the packaged jar from `build/libs/`.
2. Drop jar into your server `plugins/` folder.
3. Restart server (or use a full plugin reload strategy you trust).

## Commands

```text
/fe help
/fe list
/fe gui [player]
/fe menu [player]
/fe give <enchant> <level> [player]
/fe givebook <enchant> <level> [player]
/fe giveitem <enchant> <level> <material> [player]
/fe mysterybook <slot> [player]
/fe mysteryitem <material> [player]
/fe injector <...>
/fe structureinjector <...>
/fe librarian <...>
/fe librariantrades <...>
/fe libtrades <...>
/fe toggles [player]
/fe settings [player]
/fe enchanttoggles [player]
```

Permission: `forbiddenenchants.admin` (default: op).

#### Injector subcommands:

```text
/fe injector help
/fe injector status
/fe injector list
/fe injector enable
/fe injector disable
/fe injector gui [player]
/fe injector menu [player]
/fe injector add <structure[,structure2...]> [chance]
/fe injector set <structure> <chance>
/fe injector chance <structure> <chance>
/fe injector mode <structure> <book_only|uncursed_book_only|cursed_book_only|mystery_book_only|cursed_item_only|item_only|uncursed_item_only|mystery_item_only|cursed_only|uncursed_only|mystery_only|all|cycle|cycletype|cyclecurse|cyclemystery|status>
/fe injector mode <structure> <books|items|all> <all|cursed|uncursed> [all|mystery|non_mystery]
/fe injector mystery <structure> <on|off|toggle|status>
/fe injector notify <on|off|toggle|status>
/fe injector remove <structure[,structure2...]>
/fe injector clear
/fe injector defaultchance <chance>
/fe injector default <chance>
/fe injector vault <status|enable|disable|normal|ominous|both> [chance]
```

#### Librarian subcommands:

```text
/fe librarian help
/fe librarian status
/fe librarian list
/fe librarian enable
/fe librarian disable
/fe librarian gui [player]
/fe librarian menu [player]
/fe librarian add <enchant> <level> <chance> <emeralds> [books]
/fe librarian remove <index>
/fe librarian clear
```

#### Examples:

```text
/fe list
/fe gui
/fe gui SomePlayer
/fe give divine_vision 3
/fe givebook loot_sense 2 SomePlayer
/fe giveitem divine_vision 3 diamond_helmet SomePlayer
/fe giveitem void_grasp 1 netherite_chestplate
/fe giveitem incite_fear 2 netherite_sword SomePlayer
/fe give miners_intuition 2 SomePlayer
/fe give loot_sense 1
/fe give extended_grasp 1
/fe give void_grasp 1
/fe give masquerade 1
/fe give ascension 1
/fe give incite_fear 2
/fe mysterybook chestplate
/fe mysteryitem trident SomePlayer
/fe injector status
/fe injector enable
/fe injector add trial_chambers,ancient_city 15
/fe injector set minecraft:ancient_city 22.5
/fe injector mode minecraft:ancient_city cursed_book_only
/fe injector mystery minecraft:ancient_city toggle
/fe injector notify toggle
/fe injector remove trial_chambers
/fe injector clear
/fe injector defaultchance 7.5
/fe injector vault status
/fe injector vault enable
/fe injector vault disable
/fe injector vault normal 10
/fe injector vault ominous 20
/fe injector vault both 15
/fe injector gui
/fe librarian status
/fe librarian enable
/fe librarian add divine_vision 1 25 24 1
/fe librarian add void_grasp 1 15 32 2
/fe librarian gui
/fe librarian remove 2
/fe toggles
```

## 3 Main GUI Menus

This plugin has three main admin GUIs, each for a different workflow:

1. `/fe gui` (Creative-style admin picker)
   - Purpose: manually give forbidden books/items to players for admin distribution, balancing checks, and testing.
   - Layout: one enchant type per page (book first, then compatible pre-enchanted items).
   - Controls: click item to claim; left/right page arrows (left-click = 1 page, right-click = 5 pages).

   ![FEGUI](https://i.imgur.com/gSMVJu6.png)

2. `/fe injector gui` (Structure/vault loot injector editor)
   - Purpose: configure runtime loot injection so forbidden books/items appear in structure chests and trial vault rewards.
   - Supports all structures and configured-only view modes.
   - Includes an `Enchantment Rarity Editor` button to tune weighted rarity for each enchantment level.
   - Rarity editor includes a back button and an `Apply Weights To Enchanted Items` toggle (default: ON).
   - Per-entry controls:
     - Left/Right clicks: increase/decrease chance
     - `Q`: cycle loot type (`books -> items -> all`)
     - `Ctrl+Q`: cycle curse state (`all -> cursed -> uncursed`)
     - `F`: cycle mystery state (`all -> mystery_only -> non_mystery_only`)
   - Includes global enable toggle, notify toggle, clear-all, and normal/ominous vault entries.

   ![FEINJECTOR](https://i.imgur.com/nmPbD8G.png)

3. `/fe librarian gui` (Librarian trade blend editor)
   - Purpose: configure forbidden books to be added into librarian trades while keeping vanilla/datapack trade generation.
   - Supports all-books and configured-only view modes.
   - Per-book controls:
     - Left/Right clicks: increase/decrease trade chance
     - `Q`/`Ctrl+Q`: increase/decrease emerald cost
     - `F`: increase required book cost
     - Middle click: disable/remove that configured trade

    ![LIBRARY](https://i.imgur.com/WSySbwU.png)


#### Config + injector notes:

- Supports comma-separated structure lists for `add` and `remove`.
- Structure keys can be short (`trial_chambers`) or full namespaced (`minecraft:trial_chambers`).
- Config persistence:
  - `structure_injector.*` stores injector enable/chance/structure settings.
  - `structure_injector.book_rarity.*` stores per-book-level rarity weights for injector book rolls.
  - `structure_injector.rarity_apply_to_items` toggles whether rarity weights apply to books only (`false`) or books + enchanted items (`true`).
  - `enchant_controls.*` stores per-enchant `use_enabled` + `spawn_enabled` toggles.
  - `librarian_trades.*` stores librarian trade enable state + configured offers (chance + costs).
- Defaults:
  - Structure injector default chance: `5.0%`.
  - Trial vault default chance fallback: `7.5%` for normal and ominous when config values are missing.
  - Injector rarity toggle default: `true` (weights apply to both books and enchanted items).

## Enchants

| Enchant | Item Slot | Levels | Effect |
|---|---|---|---|
| Divine Vision | Helmet | I-III | Applies glow to nearby players/mobs through walls. Range: 10/20/30 blocks. Travel wear: every 100 blocks traveled deals 10% durability damage. |
| Miners Intuition | Helmet | I-III | Tracks nearest matching ore by helmet material (see mapping below). Range: 10/20/30. Travel wear: every 100 blocks traveled deals 10% durability damage. |
| Loot Sense | Helmet | I-III | Tracks nearest chest, barrel, or shulker. Range: 20/30/50. Travel wear: every 100 blocks traveled deals 10% durability damage. |
| Extended Grasp | Chestplate | I | Sets block/entity interaction reach to 6 blocks. |
| Void Grasp | Chestplate | I | 6 block reach plus through-wall interactions (containers/buttons/etc.) and through-wall attack tracing. |
| Masquerade | Boots | I | Sneak to disguise as nearest mob; cannot attack while disguised. Travel wear: every 100 blocks traveled deals 10% durability damage. |
| Ascension | Boots | I | Airborne upward boost while jumping; intentionally heavy durability drain. |
| Incite Fear | Sword | I-II | On hit, nearby mobs have a 25%/50% chance to flee from you. |
| Blindness | Sword | I | On player hit, 33% chance to blind for a random 1.0-5.0 seconds. |
| Miasma | Bow/Crossbow | I | On arrow impact: smoke burst, close-range invisibility, nearby blindness, explosion sound pulse. |
| Charm | Bow/Crossbow | I-IV | Hit mobs become temporary allies that protect/follow you. If shot at a player and no mobs are nearby, hostile allies are spawned to attack for you. Duration/ally cap: 15s/1, 30s/2, 60s/3, permanent/4. Charmed mobs emit hearts while active, no longer target each other, and each successful charm proc costs 10% weapon durability. Warden charm clears nearby Darkness. |
| Dragons Breath | Crossbow | I-II | Arrow impact spawns a dragon-breath cloud that damages all players and mobs in the core zone for 5s (I) or 15s (II), including the shooter. |
| Explosive Reaction | Crossbow | I | Arrow impact causes smoke with a reduced TNT-style explosion. |
| Miasma Form | Chestplate | I | Sneak to enter smoke form: hides gear visuals, phases through 1-block walls (costs 2 damage), cannot attack/interact, immune to non-fire damage, ignored by mobs except blazes. |
| The Unyielding | Chestplate | I | Prevents pushback from damage, explosions, wind bursts, and flowing-water push. Compatible with other chest enchants. |
| Aquatic Sacrifice | Helmet (Binding Curse) | I | Underwater breathing/speed/mining buffs and +2 damage underwater; outside water you slowly take drowning-style damage. |
| The Hated One | Helmet (Binding Curse) | I-II | I: reduced aggro/loot pressure. II: full aggro pressure, extra hostile/raider spawns, and much higher loot/equipment drops. |
| Withering Strike | Trident | I | Thrown-trident hits apply withering that ticks every 3 seconds until cured. |
| Healing Touch | Hoe | I | Hits heal instead of harm, cure wither, and grant golden-apple-like regen/absorption; user takes 1 heart self-damage. Undead take double damage, wither skeletons convert to normal skeletons, and zombie villagers can be cured. Starts at full durability but degrades quickly; no Mending/Unbreaking. |
| Full Pockets | Leggings | I-IV | On first open of a world container, adds bonus rare loot with a `10% * level` chance (max 40%). Also pulls XP orbs within 30 blocks toward you. |
| Forbidden Agility | Boots | I-IV | Increases movement speed by `0.006 * level`; compatible with other boot enchants. |
| Pocket Dimension | Leggings | I | At 5% health or lower, teleports you ~50 blocks to a safe location and then breaks the leggings. |
| Petty Thief | Hoe | I | 1/10 chance on PvP hit to steal a random inventory item. On mob hit, 1/10 chance to grant a themed drop item. |
| Lumberjack | Axe | I | Breaking the bottom log fells the connected tree (logs + leaves) and applies durability equal to 50% of the full block break cost. |
| Sonic Panic | Sword | I | At 50% health or lower, triggers a warden-strength radial sonic blast and shatters the sword. |
| No Fall | Boots | I | Negates all fall damage. If the fall would have killed you, consume 25% boot durability instead. Incompatible with Feather Falling and Mending. |
| Creepers Influence | Helmet | I | Nearby creepers prioritize exploding other mobs; if no other mobs are nearby they self-detonate. |
| Staff Of The Evoker | Spear | I | Spear jabs and left-click casts send a longer forward line of evoker fangs, even out of melee range, and the fangs damage mobs. |
| Vexatious | Helmet | I-III | Maintains 1/2/3 bound vex allies. Extra vexes require pristine helmet durability. |
| Wololo | Helmet (Binding Curse) | I | Sheep within 3 blocks recolor once; each conversion costs you 1 heart. |
| Locked Out | Boots (Binding Curse) | I | Blocks interaction with doors/buttons/switches/portals; Nether portal use breaks the portal block, End portal use is denied. |
| Evokers Revenge | Chestplate (Binding Curse) | I | Random evokers spawn near you every few minutes; each evoker you kill increases future spawn count. |
| The Seeker | Helmet | I | Shows only the player you are most lined up with (about a 20 degree FOV): `PlayerName 125m`. If no one is in view alignment, nothing is shown. Helmet loses 10% durability per 100 blocks traveled. Mending/Unbreaking are blocked/stripped. |
| Disarm | Axe | I | 1/20 chance to force main-hand drop from players/mobs; unarmed mobs are feared for 3 seconds. |
| Marked | Bow/Crossbow | I | Hit players glow for 30 seconds; your subsequent damage to that marked player is increased by 25%. |
| Greed | Leggings (Binding Curse) | I | Vacuums nearby item entities and XP (30 blocks), but prevents dropping items. |
| Wing Clipper | Sword | I | On player hit, blocks Elytra equip for 10 seconds; if Elytra is worn it is instantly broken to 1 durability. |
| Launch | Elytra | I | Grounded double jump gives a rocket-like launch boost and consumes 5% Elytra durability. |
| Full Force | Mace | I | Grounded mace hits deal 10 damage and launch targets about 8 blocks with a smash impact (sound + particles). |
| Temporal Sickness | Any Armor (Binding Curse) | I-III | I: every 30s teleports to the next available dimension using safe-surface placement rules. II: every 5-30s teleports to border-safe random positions within 10,000 blocks per destination dimension. III: every 10-30s does level-II style warp and deals 1 heart damage. Curse breaks by killing 10/20/30 mobs at levels I/II/III, which breaks the enchanted armor piece. |
| Grave Robber | Compass | I-IV | Compass points to the nearest recorded player death location within 1000/2500/5000/10000 blocks for levels I/II/III/IV. Action bar shows obfuscated coordinate text that reveals more digits as you get closer. |
| Pocket Seeker | Compass | I-IV | Compass points to the nearest living player within 1000/2500/5000/10000 blocks for levels I/II/III/IV. If none are in range it random-spins, and action bar uses obfuscated coordinate text that reveals more digits as you get closer. |
| Charmed Pet | Name Tag | I | Name-tagged mobs become passive pets that follow the tagger; right-click toggles sit/stay. |
| Applied Curse | Name Tag | I-III | Name-tagging another player renames them for chat + display: 30m (I), 1h (II), until death (III). |
| Get Over Here! | Lead | I | Allows enchanted leads to leash villagers (must sneak/right-click villager). |
| Mujahideen | Totem of Undying | I | On totem pop, detonates roughly 2x TNT force at your death spot with a miasma-like smoke blast, then teleports you ~50 blocks to a safe location. |
| Knockback | Shield | I | Blocking a melee hit knocks the attacker back with strong force (roughly Knockback II feel). |
| Ricochet | Shield | I | While blocking: 55% chance to reflect arrows, 100% reflect vs ghast and shulker projectiles. |
| Shockwave | Totem of Undying | I | On totem pop, emits a shockwave that pushes nearby mobs/players back and deals 3 hearts. |
| Proud Warrior | Chestplate (Binding Curse) | I | On first hit from mob/player, detonates roughly 2x TNT force, leaves wearer at 1 HP, and breaks the chestplate. |
| Sisko's Solution | Chestplate (Binding Curse) | I | Near 3+ villagers, black smoke spreads villager-to-villager, applies wither damage in the cloud, and summons wither skeletons that ignore the wearer until all linked villagers die. |
| Borg Technology | Chestplate | I | Adapts on repeated hits: after 3 matching hits, gains a barrier against that attack profile until 337 blocks of movement are traveled. |
| Warp 9.5 | Boots | I | On bedrock, grants extreme speed, degrades over travel distance, and breaks after about 3333 blocks of warp travel. |


## What Can / Can't Be Applied

| Item | Can apply | Can't apply / behavior notes |
|---|---|---|
| Helmets | Vision set: Divine Vision **or** Miners Intuition **or** Loot Sense **or** Aquatic Sacrifice **or** The Hated One; plus Creepers Influence / Vexatious / Wololo / The Seeker | Vision-set enchants are mutually exclusive with each other. Wololo applies Binding Curse automatically. The Seeker blocks Mending/Unbreaking. |
| Chestplates | Extended Grasp **or** Void Grasp **or** Miasma Form, plus The Unyielding and/or Evokers Revenge; also Proud Warrior, Sisko's Solution, Borg Technology | Grasp/Miasma Form are mutually exclusive. Evokers Revenge, Proud Warrior, and Sisko's Solution apply Binding Curse automatically. Proud Warrior / Sisko's Solution / Borg Technology must be solo enchants on the item. |
| Leggings | Full Pockets, Pocket Dimension | Can coexist on the same leggings. |
| Boots | Masquerade **or** Ascension, plus Forbidden Agility and/or Locked Out; also Warp 9.5 and No Fall | Masquerade/Ascension are mutually exclusive. Locked Out applies Binding Curse automatically. Warp 9.5 must be a solo enchant on the item. No Fall is incompatible with Feather Falling and Mending. |
| Swords | Incite Fear, Blindness, Sonic Panic | Can coexist on the same sword. |
| Bow/Crossbow | Miasma **or** Charm **or** Dragons Breath **or** Explosive Reaction | Mutually exclusive with each other. Miasma is additionally incompatible with Power, Punch, Flame, Infinity, Multishot, Quick Charge, Piercing. Dragons Breath and Explosive Reaction are now incompatible with any other enchant (vanilla or custom). |
| Tridents/Spears | Withering Strike, Staff Of The Evoker | Withering Strike applies to tridents only and triggers on thrown-trident hits. Staff Of The Evoker expects spear-type materials if available; trident fallback is used on non-1.21.11/no-spear-material setups and applies on melee and left-click cast. |
| Elytra | Launch | Enables double-jump launch while grounded and drains 5% durability per launch. |
| Maces | Full Force | Grounded hits deal 10 damage and launch targets about 8 blocks with smash impact effects. |
| Hoes | Healing Touch **or** Petty Thief | Healing Touch and Petty Thief are mutually exclusive. Healing Touch blocks/strips Mending and Unbreaking. |
| Axes | Lumberjack | Triggers when the bottom log is broken. |
| Compasses | Grave Robber **or** Pocket Seeker | Grave Robber and Pocket Seeker are mutually exclusive. |
| Name Tags | Charmed Pet **or** Applied Curse | Charmed Pet and Applied Curse are mutually exclusive. |
| Leads | Get Over Here! | Allows enchanted leads to leash villagers (must sneak-right-click). |
| Shields | Knockback, Ricochet | Can coexist on one shield. |
| Totem Of Undying | Shockwave, Mujahideen | Triggers when the enchanted totem is consumed. |
| Wrong slot items | None | Anvil/book and `giveitem` validation prevent wrong-slot application. |

## Per-Enchant Compatibility Matrix

| Enchant | Can combine with | Cannot combine with |
|---|---|---|
| Divine Vision | None (helmet stays single-forbidden-enchant) | Miners Intuition, Loot Sense, Aquatic Sacrifice, The Hated One |
| Miners Intuition | None (helmet stays single-forbidden-enchant) | Divine Vision, Loot Sense, Aquatic Sacrifice, The Hated One |
| Loot Sense | None (helmet stays single-forbidden-enchant) | Divine Vision, Miners Intuition, Aquatic Sacrifice, The Hated One |
| Aquatic Sacrifice | None (helmet stays single-forbidden-enchant) | Divine Vision, Miners Intuition, Loot Sense, The Hated One |
| The Hated One | None (helmet stays single-forbidden-enchant) | Divine Vision, Miners Intuition, Loot Sense, Aquatic Sacrifice |
| Extended Grasp | The Unyielding | Void Grasp, Miasma Form |
| Void Grasp | The Unyielding | Extended Grasp, Miasma Form |
| Miasma Form | The Unyielding | Extended Grasp, Void Grasp |
| The Unyielding | Extended Grasp or Void Grasp or Miasma Form | None (custom-enchant conflict-wise) |
| Masquerade | Forbidden Agility | Ascension |
| Ascension | Forbidden Agility | Masquerade |
| Forbidden Agility | Masquerade or Ascension | None |
| Incite Fear | Blindness | None |
| Blindness | Incite Fear | None |
| Miasma | None of other ranged forbidden enchants | Charm, Dragons Breath, Explosive Reaction; plus vanilla Power, Punch, Flame, Infinity, Multishot, Quick Charge, Piercing |
| Charm | None of other ranged forbidden enchants | Miasma, Dragons Breath, Explosive Reaction |
| Dragons Breath | None (must be sole enchant on the crossbow) | Miasma, Charm, Explosive Reaction, and any other vanilla/custom enchant |
| Explosive Reaction | None (must be sole enchant on the crossbow) | Miasma, Charm, Dragons Breath, and any other vanilla/custom enchant |
| Withering Strike | None (single trident custom enchant in current set) | None (custom-enchant conflict-wise) |
| Healing Touch | None (single hoe custom enchant in current set) | Mending, Unbreaking |
| Full Pockets | None (single leggings custom enchant in current set) | None (custom-enchant conflict-wise) |
| Pocket Dimension | Full Pockets | None (custom-enchant conflict-wise) |
| Petty Thief | None (single hoe custom enchant in current set) | Healing Touch |
| Lumberjack | None (single axe custom enchant in current set) | None (custom-enchant conflict-wise) |
| Sonic Panic | Incite Fear, Blindness | None (custom-enchant conflict-wise) |
| Creepers Influence | Most other helmet enchants except helmet-exclusives below | None (custom-enchant conflict-wise) |
| Staff Of The Evoker | Withering Strike | None (custom-enchant conflict-wise) |
| Vexatious | Most other helmet enchants except helmet-exclusives below | None (custom-enchant conflict-wise) |
| Wololo | Most other helmet enchants except helmet-exclusives below | None (custom-enchant conflict-wise) |
| Locked Out | Masquerade or Ascension, Forbidden Agility | None (custom-enchant conflict-wise) |
| Evokers Revenge | The Unyielding, and one of Extended Grasp/Void Grasp/Miasma Form | None (custom-enchant conflict-wise) |
| The Seeker | Most other helmet enchants except helmet-exclusives below | Mending, Unbreaking |
| Disarm | Lumberjack | None (custom-enchant conflict-wise) |
| Marked | Miasma or Charm (unless blocked by other ranged exclusivity) | Dragons Breath, Explosive Reaction (by ranged exclusivity) |
| Greed | Full Pockets, Pocket Dimension | None (custom-enchant conflict-wise) |
| Wing Clipper | Incite Fear, Blindness, Sonic Panic | None (custom-enchant conflict-wise) |
| Launch | None (single Elytra custom enchant in current set) | None (custom-enchant conflict-wise) |
| Full Force | None (single mace custom enchant in current set) | None (custom-enchant conflict-wise) |
| Temporal Sickness | None (single armor custom enchant in current set) | None (custom-enchant conflict-wise) |
| Grave Robber | None (single compass custom enchant in current set) | Pocket Seeker |
| Pocket Seeker | None (single compass custom enchant in current set) | Grave Robber |
| Charmed Pet | None (single name-tag custom enchant in current set) | Applied Curse |
| Applied Curse | None (single name-tag custom enchant in current set) | Charmed Pet |
| Get Over Here! | None (single lead custom enchant in current set) | None (custom-enchant conflict-wise) |
| Mujahideen | Shockwave | None (custom-enchant conflict-wise) |
| Knockback | Ricochet | None (custom-enchant conflict-wise) |
| Ricochet | Knockback | None (custom-enchant conflict-wise) |
| Shockwave | Mujahideen | None (custom-enchant conflict-wise) |
| Proud Warrior | None (must be sole enchant on the chestplate) | Any other vanilla/custom enchant |
| Sisko's Solution | None (must be sole enchant on the chestplate) | Any other vanilla/custom enchant |
| No Fall | Masquerade or Ascension, Forbidden Agility, Locked Out, Warp 9.5 | Feather Falling, Mending |
| Borg Technology | None (must be sole enchant on the chestplate) | Any other vanilla/custom enchant |
| Warp 9.5 | None (must be sole enchant on the boots) | Any other vanilla/custom enchant |



## Miners Intuition Material Mapping

Miners Intuition always points to the **nearest single matching ore** in range.

| Helmet material/theme | Ore it locates |
|---|---|
| Turtle Shell | Spawner |
| Diamond | Diamond Ore, Deepslate Diamond Ore |
| Netherite | Ancient Debris |
| Iron | Iron Ore, Deepslate Iron Ore |
| Golden/Gold | Gold Ore, Deepslate Gold Ore, Nether Gold Ore |
| Chainmail | Emerald Ore, Deepslate Emerald Ore |
| Leather | Coal Ore, Deepslate Coal Ore |
| Copper-themed custom helmet | Copper Ore, Deepslate Copper Ore |
| Unknown/default fallback | Diamond Ore, Deepslate Diamond Ore |

## Full Pockets Loot Pool

When Full Pockets procs, it injects exactly one bonus stack into that container (or drops overflow nearby). While worn, it also attracts XP orbs in a 30 block radius:

- `40%`: Diamond x1-2
- `32%`: Emerald x2-5
- `14%`: Netherite Scrap x1
- `10%`: Experience Bottle x8-16
- `4%`: Enchanted Golden Apple x1

## Helmet Restrictions (Divine Vision / Miners Intuition / Loot Sense)

If a helmet has any of these custom enchants:

- Vanilla enchants are stripped from that helmet.
- Enchanting table application is blocked.
- Helmet custom enchants are mutually exclusive (cannot combine Divine Vision / Miners Intuition / Loot Sense).
- Durability is heavily nerfed:
  - Vanilla damage taken is doubled.
  - Effective remaining durability is capped to leather helmet durability.

## Loot Table Integration

Example files are included:

- `examples/loot_table_with_vision_books.json`
- `examples/loot_table_all_mystery_items.json`
- `examples/loot_table_mystery_items_armor.json`
- `examples/loot_table_mystery_items_utility.json`
- `examples/loot_table_mystery_items_weapons_tools.json`
- `examples/loot_table_mystery_items_weighted.json`

These examples cover:

- Forbidden enchanted books
- Mystery books and mystery pre-enchanted items
- Category-specific mystery pools (armor / utility / weapons-tools)
- Weighted mystery selection patterns

This plugin recognizes books by `custom_model_data` (and also by display name/PDC fallback).
It also recognizes pre-enchanted gear items by PDC and by the same `custom_model_data` formula.

Model data formula used by the plugin:

```text
model_data = 930000 + ((typeIndex + 1) * 10) + level
```

Type index order:

1. `divine_vision`
2. `miners_intuition`
3. `loot_sense`
4. `extended_grasp`
5. `void_grasp`
6. `masquerade`
7. `ascension`
8. `incite_fear`
9. `blindness`
10. `miasma`
11. `charm`
12. `miasma_form`
13. `aquatic_sacrifice`
14. `the_hated_one`
15. `withering_strike`
16. `healing_touch`
17. `full_pockets`
18. `dragons_breath`
19. `explosive_reaction`
20. `the_unyielding`
21. `forbidden_agility`
22. `pocket_dimension`
23. `petty_thief`
24. `lumberjack`
25. `sonic_panic`
26. `creepers_influence`
27. `staff_of_the_evoker`
28. `vexatious`
29. `wololo`
30. `locked_out`
31. `evokers_revenge`
32. `the_seeker`
33. `disarm`
34. `marked`
35. `greed`
36. `wing_clipper`
37. `launch`
38. `full_force`
39. `temporal_sickness`
40. `grave_robber`
41. `pocket_seeker`
42. `charmed_pet`
43. `applied_curse`
44. `get_over_here`
45. `mujahideen`
46. `shield_knockback`
47. `ricochet`
48. `shockwave`
49. `proud_warrior`
50. `siskos_solution`
51. `no_fall`
52. `borg_technology`
53. `warp_9_5`

So the generated IDs are:

- Divine Vision I/II/III: `930011`, `930012`, `930013`
- Miners Intuition I/II/III: `930021`, `930022`, `930023`
- Loot Sense I/II/III: `930031`, `930032`, `930033`
- Extended Grasp I: `930041`
- Void Grasp I: `930051`
- Masquerade I: `930061`
- Ascension I: `930071`
- Incite Fear I/II: `930081`, `930082`
- Blindness I: `930091`
- Miasma I: `930101`
- Charm I/II/III/IV: `930111`, `930112`, `930113`, `930114`
- Miasma Form I: `930121`
- Aquatic Sacrifice I: `930131`
- The Hated One I/II: `930141`, `930142`
- Withering Strike I: `930151`
- Healing Touch I: `930161`
- Full Pockets I/II/III/IV: `930171`, `930172`, `930173`, `930174`
- Dragons Breath I/II: `930181`, `930182`
- Explosive Reaction I: `930191`
- The Unyielding I: `930201`
- Forbidden Agility I/II/III/IV: `930211`, `930212`, `930213`, `930214`
- Pocket Dimension I: `930221`
- Petty Thief I: `930231`
- Lumberjack I: `930241`
- Sonic Panic I: `930251`
- Creepers Influence I: `930261`
- Staff Of The Evoker I: `930271`
- Vexatious I/II/III: `930281`, `930282`, `930283`
- Wololo I: `930291`
- Locked Out I: `930301`
- Evokers Revenge I: `930311`
- The Seeker I: `930321`
- Disarm I: `930331`
- Marked I: `930341`
- Greed I: `930351`
- Wing Clipper I: `930361`
- Launch I: `930371`
- Full Force I: `930381`
- Temporal Sickness I/II/III: `930391`, `930392`, `930393`
- Grave Robber I/II/III/IV: `930401`, `930402`, `930403`, `930404`
- Pocket Seeker I/II/III/IV: `930411`, `930412`, `930413`, `930414`
- Charmed Pet I: `930421`
- Applied Curse I/II/III: `930431`, `930432`, `930433`
- Get Over Here! I: `930441`
- Mujahideen I: `930451`
- Knockback I: `930461`
- Ricochet I: `930471`
- Shockwave I: `930481`
- Proud Warrior I: `930491`
- Sisko's Solution I: `930501`
- No Fall I: `930511`
- Borg Technology I: `930521`
- Warp 9.5 I: `930531`

## Notes / Caveats

- Vision and sense effects are particle/status based and run on scheduled ticks.
- Through-wall interactions for `Void Grasp` are intentionally permissive by design.
- `Masquerade` uses an invisible player + silent mob disguise entity.

## Architecture

The plugin is split into focused layers:

- **Composition root**: `ForbiddenEnchantsPlugin` wires services, owns runtime maps/PDC keys, schedules tick maintenance, and exposes command/menu entrypoints.
- **Event bridge**: `ForbiddenEnchantsListener` keeps Bukkit handlers thin and routes events into services/runtime dispatch.
- **Enchant domain model**:
  - `EnchantType` is the registry of all enchants (arg, model index, color, slot, max level, compatibility flags/groups).
  - `enchants/ForbiddenEnchantDefinition` defines metadata contract.
  - `enchants/ForbiddenEnchantRuntime` defines optional runtime hooks.
  - `enchants/EnchantList` builds the runtime dispatcher from `EnchantType.values()`.
- **Rules and item pipeline**: `EnchantStateService`, `EnchantRuleCoreService`, `EnchantEventRuleService`, `EnchantBookFactoryService`, `MysteryItemService`.
- **Feature services**: combat/mob/effect systems (`GraspCombatService`, `WitheringStrikeService`, `VexatiousService`, `FullPocketsService`, `TemporalSicknessService`, etc.).
- **Injector subsystem**: `StructureInjectorRuntimeService`, `InjectorCommandHandler`, `InjectorMenuService`, `InjectorLootMode`, `InjectorMysteryState`.
- **Librarian trade subsystem**: `LibrarianTradeService`, `LibrarianTradeCommandHandler`, `LibrarianTradeMenuService`.
- **Presentation/admin UX**: `FeCommandHandler`, `FePresentationService`, `FeMenuService`, `FeCatalogService`, `EnchantToggleMenuService`, `InjectorMessagingUtil`.
- **Persistence**: `ConfigPersistenceService` for `structure_injector.*`, `enchant_controls.*`, and `librarian_trades.*`.

## Project Structure

- `src/main/java/dev/cevapi/forbiddenenchants/`
  - `ForbiddenEnchantsPlugin.java`: composition root and shared runtime state.
  - `ForbiddenEnchantsListener.java`: Bukkit event routing.
  - `EnchantType.java`: canonical enchant registry + compatibility metadata.
  - `StructureInjectorRuntimeService.java`: structure/vault loot injection runtime.
  - `InjectorCommandHandler.java`, `InjectorMenuService.java`: injector CLI + GUI.
  - `LibrarianTradeService.java`, `LibrarianTradeCommandHandler.java`, `LibrarianTradeMenuService.java`: librarian trade runtime + CLI + GUI.
  - `ConfigPersistenceService.java`: load/save of injector, per-enchant toggle, and librarian trade config.
  - `PluginModels.java`: inventory holders/records for GUI/runtime model objects.
  - `*Service.java`: focused subsystems (combat, effects, lifecycle, utility, etc.).
- `src/main/java/dev/cevapi/forbiddenenchants/enchants/`
  - `BaseForbiddenEnchant.java`, `ForbiddenEnchantDefinition.java`, `ForbiddenEnchantRuntime.java`
  - `EnchantList.java` runtime dispatcher
  - one class per enchant (`*Enchant.java`)
- `src/main/resources/`
  - `plugin.yml`: command + plugin metadata
  - `config.yml`: persisted defaults/state (`structure_injector`, `enchant_controls`, `librarian_trades`)
- `examples/`
  - all datapack-style loot table examples (books + mystery books/items + weighted/category pools)

## Example Loot Tables

- `examples/loot_table_with_vision_books.json`: direct forbidden-book entries.
- `examples/loot_table_all_mystery_items.json`: broad mystery-item pool.
- `examples/loot_table_mystery_items_armor.json`: armor-only mystery pool.
- `examples/loot_table_mystery_items_utility.json`: utility-only mystery pool.
- `examples/loot_table_mystery_items_weapons_tools.json`: weapons/tools mystery pool.
- `examples/loot_table_mystery_items_weighted.json`: weighted mystery mix.

Minimal direct-book entry shape (from `loot_table_with_vision_books.json`):

```json
{
  "type": "minecraft:item",
  "name": "minecraft:enchanted_book",
  "functions": [
    {
      "function": "minecraft:set_components",
      "components": {
        "minecraft:custom_name": {
          "text": "Forbidden Divine Vision I",
          "color": "aqua",
          "italic": false
        },
        "minecraft:custom_model_data": {
          "floats": [930011.0]
        }
      }
    }
  ]
}
```

For mystery entries, include `minecraft:custom_data` with:

- `"forbiddenenchants:mystery_item": 1`

## Adding New Enchants

1. Add a new enchant class in `src/main/java/dev/cevapi/forbiddenenchants/enchants/`, typically extending `BaseForbiddenEnchant`.
2. In that class, define:
   - `arg`, `pdcKey`, `displayName`, `slot`, `maxLevel`, `color`, aliases
   - `effectDescription(level)` (shown in lore/UI)
   - runtime hooks only if needed (`onDamageByEntity`, `onInteract`, `onPlayerTick`, etc.)
3. Register the enchant in `EnchantType`:
   - add import
   - add enum constant with a unique `modelTypeIndex` and `new YourEnchantClass()`
   - keep `modelTypeIndex` unique forever (do not reuse old numbers)
4. Update compatibility/flags in `EnchantType` static init when needed:
   - `setExclusiveGroup(...)`
   - `addMutualIncompatibility(...)`
   - sets like `APPLIES_BINDING_CURSE`, `STRIPS_VANILLA_ENCHANTS`, `STRIPS_MENDING_UNBREAKING`, `REQUIRES_NO_OTHER_ENCHANTS`, `REQUIRES_SOLO_ON_TRIDENT`
5. Validate slot/material behavior:
   - existing slot: confirm `EnchantMaterialCatalog` + `ItemClassificationService` rules match your intent
   - new slot category: update `ArmorSlot`, `SlotParsingUtil`, `EnchantMaterialCatalog`, and slot checks in `ItemClassificationService`
6. Understand what is automatic:
   - command arg resolution and tab-complete come from `EnchantType` metadata
   - runtime dispatch is automatic through `EnchantList` once the enchant is in `EnchantType`
   - book/item lore wrapping uses `slotDescription()` + `effectDescription(level)` via `EnchantBookFactoryService` and `LoreWrapUtil`
7. If your enchant needs extra runtime state/services, wire it in `ForbiddenEnchantsPlugin` and route relevant events through existing services/listener flow.
8. Build and validate:
   - `./gradlew build`
   - test `/fe give`, `/fe givebook`, `/fe giveitem`, `/fe mysterybook`, `/fe mysteryitem`, `/fe gui`, and injector/toggle interactions.

Note: this repository currently has no scaffold script for new enchants; add them manually with the steps above.

## Enchantment Requests

![Kawaii](https://i.imgur.com/8BsLBc6.png)

I am open to requests for new enchants! Just send a message via GitHub!
