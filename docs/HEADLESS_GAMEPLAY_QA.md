# Headless Gameplay QA

The Class Archetype Stress Test is documented in
`docs/CLASS_ARCHETYPE_STRESS_TEST_V0_1.md`. It adds `classArchetypeStressQa`, a shared build-aware
policy, a twelve-scenario matrix, multi-seed behavior profiles, component/constraint utilization,
and conservative dominance reports without adding formal player classes.

## Purpose and boundary

The Headless Gameplay QA Harness is a shell/CI entry point for deterministic Rule Runtime integration tests. It uses libGDX's `HeadlessApplication`; it never creates GLFW, a window, an OpenGL context, audio playback, screenshots, mouse input, or Android services.

The harness is not a parallel combat simulator. Scenarios instantiate the production `Hero`, `Mob`, `Level`, `Actor`, `Buff`, `Item`, `RuleRuntime`, Poison, Burning, knockback, teleport, and Bundle code. `QaFixedLevel` only replaces procedural level generation with a small deterministic map. `NoOpCharSprite` and `RulePresentation` suppress visual feedback while preserving the gameplay callback that moves or damages actors.

## Commands

From the repository root on Windows:

```text
gradlew.bat headlessQa
gradlew.bat buildIntegrityQa --args="--count 1000 --seed 12345"
gradlew.bat ruleFuzz --args="--count 1000 --seed 12345"
```

Useful options:

- `--scenario <path>` runs one versioned JSON scenario instead of the built-in regression corpus.
- `--seed <long>` overrides the scenario/fuzzer seed.
- `--count <n>` controls static/fuzz build count.
- `--headless-sample <n>` controls how many non-broken fuzz builds run through real gameplay.
- `--trace` writes all gameplay traces.
- `--trace-on-failure` writes traces for failed or BROKEN scenarios.
- `--report-dir <path>` changes the report directory relative to the repository root.

The command-line bootstrap is in the `headless` Gradle module. Assets are made available as the working directory, but renderer assets are not loaded.

## Presentation isolation

`RulePresentation` is the narrow Rule-effect boundary for particles, status popups, blast animation, and sound. It delegates to the normal SPD presentation when a real sprite tree exists and becomes a no-op in headless tests. Teleport and knockback retain their production position mutation but execute the animation callback synchronously when no scene exists. The only general Hero change is an `instanceof HeroSprite` guard around the sprint animation; speed calculation remains unchanged.

This approach avoids a global `HeadlessMode` flag and prevents no-op behavior from leaking into Desktop or Android gameplay.

## Scenario schema

The schema identifier is `ruleqa-scenario-1`. A scenario contains an ID, fixed seed, complete build or DEV preset, fixed-level setup, ordered actions, and expected findings. The executable example is `headless/src/main/resources/scenarios/water_focus_wait_loop.json`.

```json
{
  "schema": "ruleqa-scenario-1",
  "id": "example",
  "seed": 10086,
  "hero": {
    "preset": "F_WATER_SHAPER",
    "hp": 100,
    "maxHp": 100
  },
  "level": {
    "type": "fixed_test_level",
    "width": 7,
    "height": 7,
    "heroCell": 24,
    "waterCells": [24]
  },
  "actions": [
    {"type": "WAIT", "repeat": 10}
  ],
  "expectedFindings": [],
  "staticOnly": false
}
```

A complete legacy `CustomClassConfig` or authoritative `ClassBuild` may be supplied instead of `hero.preset`. Bundle round-trip, explicit schema rejection, migration, and deterministic build fingerprints are covered by tests.

## Action API

Gameplay actions use production paths:

- `WAIT`: `RuleHooks.onWait`, Hero time spend, then real Actor scheduling.
- `MOVE`: validates the map, calls `Hero.move`, spends a turn, then schedules actors.
- `ATTACK`: calls `Hero.attack` and the production attack delay.
- `USE_ACTIVE_RULE`: calls `RuleHooks.triggerActive` with the selected cell.

Deterministic setup/control actions are `SET_HP`, `SET_RESOURCE`, `APPLY_STATUS`, `CLEAR_STATUS`, `SPAWN_MOB`, `SPAWN_ITEM`, `SET_TILE`, and `SAVE_RELOAD`. Setup actions may set test preconditions directly; status, mob, item, save, and reload behavior still uses production classes.

Supported V0.1 fixtures are Rat/Snake, healing or toxic-gas potion/Bomb, Poison/Burning, and EMPTY/WATER/WALL/CHASM tiles. Expanding fixtures is intentionally separate from expanding gameplay vocabulary.

## Actor scheduling

`Actor.processUntilTurn(hero, limit)` uses the same production actor list, time, priority, and `act()` methods as the normal scheduler. It stops immediately before the Hero's next turn. It fails loudly if gameplay requests an asynchronous animation callback or exceeds the actor safety limit; it does not silently skip the actor.

## Snapshot schema

`QaSnapshot` is a compact semantic view, not a second save format:

- world: turn, depth, total actor count;
- Hero: ID, cell, HP/max HP, shield, custom class name, law, resource type/value/max, Buff names, equipped Item names, restrictions;
- Rule instances: ID, event, deterministic runtime order, cooldown, trigger count;
- pending delayed payloads: payload ID, remaining turns, and Effect;
- mobs: production class, actor ID, cell, HP/max HP, Buff names;
- level: terrain values for relevant Hero and Mob cells.

`SAVE_RELOAD` Bundle-saves the production Hero, destroys the active actor/runtime objects, restores a new Hero, and compares resource, build, restrictions, rule order, cooldown, and counters.

## Event trace

`RuleTrace` is opt-in and has one null check when disabled. A trace records causal gameplay only:

```text
T=15 ACTION HERO ATTACK MOB#3 hit=true hp=8->0
T=15 EVENT ON_KILL owner=Hero target=Rat
T=15 RULE reaction_technique order=1
T=15 CONDITION ALWAYS=true
T=15 COST RESOURCE 2 paid=true
T=15 EFFECT SHIELD target=Hero
```

Runtime failures append a Java stack to the same trace. Reports store absolute trace paths, making a failing seed directly actionable for an agent.

## Built-in regressions

| ID | Coverage |
| --- | --- |
| `water_focus_wait_loop` | Real Focus/Water/ON_WAIT Shield loop, WAIT x500, dynamic unbounded-power detection. |
| `no_weapon_no_victory` | Static enemy-resolution failure under no ordinary weapons. |
| `poison_condition_without_source` | Missing state producer dependency. |
| `resource_without_source` | Affliction consumers with no internal status source. |
| `momentum_loop` | MOVE, MOVE, MOVE, WAIT through the real Momentum engine. |
| `affliction` | Real Poison and Burning feed the Affliction engine. |
| `real_combat` | Real Hero attack, Rat death, ON_KILL dispatch, and resource state. |
| `save_load` | Bundle round-trip of build and independent Rule instance state. |
| `event_chain` | Real HIT applies Poison; the accepted Buff emits STATUS_APPLIED and feeds Affliction. |
| `bridge_move` | A real BlastWave PUSH emits `FORCED_MOVEMENT`, bridges to ON_MOVE, and triggers another rule. |
| `bridge_recursion_guard` | A cyclic semantic bridge is stopped by causal-chain membership. |
| `mark_chain` | First rule applies a saved HUNTED Buff; the second rule checks and stacks it. |
| `mark_expire` | HUNTED expires through the real Actor/Buff scheduler. |
| `delay_three_turns` | A delayed Effect fires on the third following Hero turn, not at scheduling time. |
| `delay_save_load` | A pending delayed payload retains its remaining delay and behavior across Bundle reload. |
| `accumulation_echo` | Three real MOVE events charge Accumulation; the next active Effect is empowered, schedules one weakened Echo, and the saved payload fires once after reload. |
| `overflow_overdraw` | Mana overflow converts at 2:1 into Barrier with a saved remainder; a later empty-pool active technique pays its deficit in HP. |
| `compensation` | A full-health HEAL truly fails, produces its bounded Barrier alternative, saves its lock token, and does not compensate again for free. |
| `phase_blood` | Crossing below 30% HP applies the low-health mode and gives an unmodified Blood technique real AREA behavior across save/load. |
| `hunt_mark` | Repeated real attacks build HUNTED; the next successful technique consumes three stacks and applies real Slow. |
| `hunt_propagation` | Killing a marked Rat transfers at most two stacks to the nearest Snake with transfer depth capped at one. |
| `inertia_momentum` | A real PUSH emits forced movement, the formal Inertial Link bridges it to MOVE, and the movement reaction/resource loop runs. |
| `translocation_bridge` | A real TELEPORT emits translocation, bridges to tile entry, and the reaction changes the arrived tile to water. |
| `translocation_backlash` | A successful translocation applies the real Slow Buff to the Hero as its behavior-linked price. |
| `shield_backlash` | A rule-produced Barrier emits protection and applies the real Vulnerable Buff as its behavior-linked price. |
| `class_zero_resource` | A no-pool ClassBuild installs and runs without fabricating a resource engine. |
| `class_multiple_resource` | Independent primary/additional pools survive runtime use and save/load. |
| `class_multiple_active` | Multiple active Skills remain separately selectable; one action never dispatches all of them. |
| `class_reaction_only` | A ClassBuild with no active Skill remains valid and runs its real reaction rule. |
| `class_multiple_laws_traits` | Multiple Laws and Traits install without fixed slots. |
| `class_movement` | Ground-placement TELEPORT compiles through the unified Skill adapter and real SPD movement path. |

Full Skill Runtime adds seventeen `ref_*` fixtures covering projectile wall/range/collision, piercing beam, cone/ring/chain/filter/seeded targeting, typed damage and status stacking tests, Temporary HP and redirect guards, real scheduled owned Actors, ownership/Mark/command/inheritance, persistent fields/traps, terrain/Blob capability validation, Mode Shift, protected transfer rejection, saved Action Attachment with Secondary, entity lifetime, and reaction-only execution. The complete list is maintained by `FullSkillReferenceBuilds`; these fixtures are never player profession presets.

Expressiveness traces include event IDs, parent/root IDs, the original gameplay event, source rule, semantic tag emission, bridge decisions/guards, and delayed schedule/fire/completion records. This keeps parent-child causality visible without logging renderer noise.

The known water loop remains an expected BROKEN result. The task passes only when the harness detects `UNBOUNDED_POWER_LOOP`; changing the assertion to conceal the exploit would fail the regression's purpose.

The ten Core Vocabulary scenarios are fixed production-runtime regressions, not simulation fixtures. Together they cover the requested compound loops: Accumulation + Echo, Hunt Mark + Propagation, Overflow + Overdraw, Event Remap + Momentum, and Phase Shift + Blood. Their traces and snapshots include vocabulary slots, marks, transfer depth, overflow remainder, and delayed payload kind so save/load regressions remain machine-visible.

## Dynamic exploit detector and policies

`PositiveLoopDetector` is deliberately conservative. Without hostile interaction or finite setup consumption it samples state trends and reports monotonic, material growth in Shield, resource beyond maximum, actor count, or permanent max HP. It does not claim to prove that all positive loops are bounded.

Fuzz gameplay uses four deterministic adversarial scripts, not an AI: `WAIT_SPAMMER`, `ACTIVE_SPAMMER`, `RESOURCE_FARMER`, and `MOVE_LOOP`.

## Reports

The default output directory is `build/reports/ruleqa/`:

- `summary.json`: current mode, totals, performance, and top findings;
- `scenarios.json`: per-scenario seed, build, snapshots, findings, failure, turn, and trace path;
- `fuzz.json`: classifications, density, vocabulary usage, failures, and performance;
- `compatibility.json`: observed module-pair statistics;
- `traces/*.log`: optional causal traces.

All random failures include scenario ID, exact derived build seed, build fingerprint/config, and turn. Re-running with the same master seed and count regenerates the same build at the same index.
