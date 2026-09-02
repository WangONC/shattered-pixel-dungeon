# DEV RULE LAB

DEV RULE LAB is available only when `DeviceCompat.isDebug()` is true. In a debug game, open the normal in-game menu and select the localized **DEV RULE LAB** entry. Release players never receive it.

The A–F menu is explicitly labeled **DEV / QA PRESETS**. These regression builds remain separate from Hero Select and the player-facing Class Overview. The lab intentionally retains the structural Rule and Runtime dump (including the otherwise-hidden unconditional condition) for debugging.

## Operations

- HP: set to 1, half, or full.
- Resource: switch among Mana, Rage, Blood, Momentum, Focus, and Affliction; fill numeric resources; add or subtract resource.
- Rule state: add the V0.1 required compositions and ON_HIT Poison, remove the last rule, and inspect saved counters/cooldowns/order.
- Status: apply real Poison or Burning, clear negative statuses, or clear optional Buffs while retaining Hunger, Regeneration, and the resource indicator.
- Mob: spawn Rat, Snake, Slime, or Thief at a valid respawn cell.
- Item: spawn healing/liquid-flame potions, a random weapon, or a random Item.
- Travel/save: save, save and reload through `InterlevelScene.Mode.CONTINUE`, next depth, or depths 1/5/10/15/20/25.
- Helpers: kill enemies, reveal map, heal full, show class config, and show runtime state.
- V0.2 presets: replace the current debug runtime with acceptance build A–F.

## Meaningful Diversity presets

| Preset | Gameplay loop |
| --- | --- |
| A · Momentum Skirmisher | Movement builds Momentum; an active TELEPORT spends it; hitting an already poisoned target PUSHes it; WAIT clears the loop. |
| B · Focus Controller | WAIT/safe turns build Focus; damage breaks it; AREA PULL creates space and ON_WAIT grants Shield; active use also costs HP. |
| C · Affliction Weaver | Negative Buffs create Affliction and Status Absorption adds more; the active technique CLEANSEs; status application triggers HASTE. |
| D · Blood Caster | FIRE spends HP; kills feed rule HEAL which the Class Law converts to Shield; ordinary weapons are forbidden. |
| E · Status Combo | Active POISON establishes state; ON_HIT only PULLs a poisoned target, proving conditional rule synergy. |
| F · Water Shaper | AREA CREATE_WATER changes the map; entering water satisfies SELF_IN_WATER and creates Shield; Water Affinity increases resource gain there. |

The preset constructor is shared with `RuleRuntimeTest`, so DEV setup and automated acceptance blueprints cannot drift silently.

## Save/load verification

1. Load a preset and change its resource to a non-default value.
2. Trigger Active and Reaction rules so counters/cooldowns change.
3. Choose **Travel / Save / Reload → Save and reload from disk**.
4. Reopen DEV RULE LAB and inspect **current Runtime**.
5. Confirm name, Resource Engine/value, Class Law, two rules, ordered conditions/parameters, restrictions, Capacity, trigger counts, and cooldowns.

## Notes

- Opening rule/resource tools on a Vanilla Hero installs a localized debug Runtime without replaying character creation.
- Loading a preset replaces only the debug Runtime. Legacy A-F fixtures pass through `ClassBuildMigrator` into the same slot-free production runtime; they remain QA presets rather than formal player classes. A prior Frail max-HP adjustment is undone before installing the new configuration.
- Full runtime inspection displays the authoritative ClassBuild plus independent resources, Skills, Laws/Traits, restrictions, cooldowns, counters, Marks, Bridges, and delayed payloads. Player-facing Build Sheet windows continue to hide those debug identifiers.
- Debug addition may exceed normal Capacity. CREATE A CLASS always enforces its budget and Modifier compatibility.
- Depth travel uses SPD's normal interlevel path, not direct map mutation.
