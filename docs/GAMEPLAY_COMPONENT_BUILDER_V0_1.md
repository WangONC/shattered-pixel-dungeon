# Gameplay Component Builder V0.1

This pass completes the player path from an empty `ClassBuild` to the same component builds used
by headless QA. It does not add a core slot or an archetype/domain flag. Names such as Gunner and
Summoner exist only on DEV/QA fixtures.

## Authoritative player model

- Class-level economy: `ResourceSpec` plus zero or more saved `ResourceFlowSpec` entries.
- Skills: the frozen `SkillSpec` model (activation, conditions, primary/secondary effect,
  delivery, targeting, affix, cost, and constraint).
- Persistent play: create-entity variants, persistent delivery, ownership, commands, links,
  lifetime, count, and period.
- Memory and modes: marks, stacks, counters, consumption, and mode state/conditions.
- Connections: optional Laws and Traits. These connect or reinterpret components; they are not
  the class core and missing prerequisites may be added later.

`PlayerBuildAssembler` is the audited, non-UI equivalent of the official builder path. Reference
build QA must pass through it before a build may be called player-constructible.

## Resource economy

A resource pool exposes a player name, capacity, initial value, gain rules, and loss/decay rules.
Presets are editable starting patterns, not separate hard-coded resource engines. Supported gain
events are time, hit, damaged, move, wait, kill, negative status, and owned-entity removal. The
last event allows an expired or destroyed creation to be recycled without pretending that it was
a hero kill. Out-of-combat, stopped-moving, timed loss, and wait clearing are available loss
patterns. Event intervals and their current counters survive save/load.

Active reload/refill is built as an ordinary active resource-gain skill with an action cost. A
resource conversion is an ordinary Resource Operation effect. Overflow remains a parameterized
Trait connection.

## Dependency state machine

- `RESOLVED`: all required components are present.
- `UNRESOLVED`: selectable and saveable as a draft; the builder names the missing resource, mode,
  mark/state, owned-entity path, or link. Adding the dependency automatically rebinds the draft.
- `HARD_CONFLICT`: permanently incompatible with the current selection and cannot be added.
- `UNSUPPORTED`: not implemented by the runtime and not offered as a normal player option.

Only unresolved dependencies that remain when creating the final class are blocking. A missing
dependency no longer turns most of the Law/Trait list into unexplained disabled rows.

The `RESOURCE_OVERDRAFT_USES_HP` dependency is defined once and shared by copy and validation: it
requires both a non-Blood resource pool and a skill that actually spends that resource.

## Reference archetype decomposition

| QA fixture only | Ordinary player components |
| --- | --- |
| Martial Defender | Contact attacks, barrier/mitigation/temporary HP, damaged/hit reactions, throw/push |
| Blood Berserker | HP cost, low-HP condition/constraint, temporary HP, missing-HP damage, execute |
| Ammo Gunner | Finite manual resource, active action-cost refill, projectile, range, pierce/repeat |
| Area Caster | Recovering resource, projectile/beam/ground delivery, line/radius, damage/status |
| Mark Assassin | Mark/stack, dash, marked targeting, missing-HP/execute finisher, mark consumption |
| Owned Summoner | Create Actor, lifetime/count, ownership, owned filter, command, inheritance |
| Device Engineer | Manual fuel/refill, Create Device, persistent field/trap, period, ownership/link |
| Terrain Controller | Ground placement, water/gas/trap, forced movement, water position condition |
| Defense Support | Rule healing, temporary HP, cleanse, owned ally, redirect/link; ordinary weapons and ordinary healing are surrendered for the required budget |
| Mode Shifter | Mode effects, `MODE_IS` conditions, separate attack and defense/mobility skills |

No production code branches on these fixture names.

## Builder structure

The top-level **Core Gameplay** section is an editable component sheet and a generated summary. It
shows resource pools and their loops, active/refill and reactive skills, delivery advantages,
marks, owned entities, carriers, terrain use, forced movement, modes, defense, and HP tradeoffs.
It deliberately does not assign a class label.

The resource editor supports adding/removing multiple pools, changing player-facing names,
capacity and initial value, and adding/removing gain or loss flows. The skill editor exposes
persistent-carrier lifetime/period and action-attachment event/charges in addition to the frozen
skill vocabulary.

## Automated contracts

- `gradlew.bat gameplayComponentCoverageQa` writes
  `build/reports/ruleqa/gameplay_component_coverage.json`. Every discussed row must be implemented,
  player exposed, saveable, budgeted, and runtime verified. The report currently contains 205
  rows, including every activation, condition, all 11 effect families, all 57 exposed effect
  variants, delivery, targeting, affix, economy, cost/constraint, entity, carrier, and memory item.
- `gradlew.bat playerArchetypeReconstructionQa` writes
  `build/reports/ruleqa/player_archetype_reconstruction.json`. The ten primary fixtures are rebuilt
  through `PlayerBuildAssembler`, round-tripped, and run in real headless combat.

The required zero counters are:

- `discussed_component_not_player_exposed`
- `qa_build_not_player_constructible`
- `dependency_missing_but_disabled`
- `dependency_copy_validation_mismatch`
- `runtime_failure`

## Android verification boundary

The Android package is built normally. Core pointer/scroll regression tests remain available, but
physical-device interaction was not run in this pass because the device was no longer available.
No physical-device result should be inferred from a successful APK build.
