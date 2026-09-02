# Rule Build Integrity

## Scope

`RuleBuildAnalyzer` is Level A QA: a fast, conservative static integrity check over a detached `RuleBuild`. `RuleBuild.from(ClassBuild)` projects the slot-free authoritative schema without discarding resources, skills, laws, traits, restrictions, or Starting Kit. It does not initialize a map and does not claim that a build is balanced or fun. `HeadlessGameplayHarness` is Level B QA and validates real runtime behavior.

`RuleSemanticMetadata` and structured `EffectSpec` analysis describe only capabilities that currently execute. The analyzer now awards `ENVIRONMENT_KILL`, `SUMMON_DAMAGE`, and `DEVICE_DAMAGE` only to their implemented Fire/Gas, owned Actor, or carrier operations. `BYPASS` remains an unused extension point.

## Minimal semantic metadata

Current metadata can express:

- `RESOURCE_SOURCE` and `RESOURCE_SINK`;
- enemy resolution through `DIRECT_DAMAGE` and `DOT_DAMAGE`;
- `FORCED_MOVEMENT`, `ENVIRONMENT_CONTROL`, `MOBILITY`, and `CONTROL`;
- `SURVIVAL`, `HEALING`, and `SHIELDING`;
- `STATUS_APPLICATION` and `STATUS_REMOVAL`;
- produced/required states: Poison, Burning, Water, and Resource;
- removed capabilities, currently ordinary weapon damage and traditional healing;
- source reliability: reliable engine, external input, or the Hero HP pool.

Metadata is derived from Resource, Effect, Law, and Restriction enums. It is kept out of UI presentation and does not alter Rule Runtime execution or save data.

## Classification

- `VALID`: no basic integrity problem was found.
- `RISKY`: potentially playable, but dependent on external terrain/status, contains a duplicate/dead resource, or exposes a conservative free-loop warning. Creation need not be blocked.
- `BROKEN`: definite structural failure such as no enemy-resolution path, syntax/target/effect incompatibility, unreachable resource threshold, missing executable module, or a required resource with no reliable build source.

A result retains every finding; classification is the greatest severity observed.

## Current checks

### Victory path

Ordinary Heroes receive production weapon `DIRECT_DAMAGE`. `NO_ORDINARY_WEAPONS` removes that capability, after which an implemented damage Rule such as Fire/Poison/Bleed/Gas must provide a real enemy-resolution path. Shield + Heal + Cleanse + Water therefore reports `BROKEN / NO_VICTORY_PATH`; no `hasDamage` shortcut treats defensive capability as victory.

### Resource economy

- resource source with no meaningful cost reports `RISKY / DEAD_RESOURCE`;
- a Resource cost whose stable pool ID is not declared reports `BROKEN / COST_WITHOUT_SOURCE`;
- invalid Consumable/State references report `BROKEN / COST_SOURCE_INVALID`; finite consumable reliance is reported as `RISKY / EXTERNAL_COST_REQUIRED`;
- a charged-State cost without Accumulation reports `BROKEN / COST_WITHOUT_SOURCE`;
- a consuming Affliction build with no internal negative-status producer reports `BROKEN / RESOURCE_WITHOUT_SOURCE` because its only source is external input;
- `RESOURCE_AT_LEAST` above the engine maximum reports `BROKEN / UNREACHABLE_RULE`;
- Blood is treated as the Hero HP pool, not a regenerating numeric resource.

### State dependencies

A Poison, Burning, or Water condition without a matching build producer reports `RISKY / CONDITION_SOURCE_MISSING` or `EXTERNAL_STATE_REQUIRED`. It is not always BROKEN because monsters, items, and terrain may provide that state. A self-targeted status-producing Rule can establish an Affliction source.

### Duplicate, target, and no-op rules

The analyzer fingerprints Trigger, ordered Conditions and parameters, Cost, Target, Effect/power, and Modifier/magnitude. An exact duplicate reports `DUPLICATE_RULE`. Missing modules, invalid Trigger/Target context, incompatible Effect/Target, or incompatible Effect/Modifier report `NO_OP_RULE` or `INVALID_TARGET_CONTEXT`.

### Loop risk

Focus + repeatable ON_WAIT Heal/Shield with no cost or a cost funded by the same wait reports `FREE_REPEATABLE_POWER_LOOP`. This static warning is supplemented by the headless monotonic-state detector; the latter is what confirms the water shield exploit as `UNBOUNDED_POWER_LOOP`.

Structured runtime checks additionally report unsupported engine capabilities, entity production loops/unlimited production, permanent free carriers, ownership or command orphans, inheritance source absence, link/redirect cycle risk, Action Attachment recursion risk, invalid terrain operations, unreachable modes, protected transfer/copy requests, missing damage-scaling pools, and total budget overflow. Runtime caps and recursion guards remain active even when a build is deliberately installed by DEV QA.

## Fuzzer

`RuleBuildFuzzer` derives an independent 64-bit seed for every build index. It generates free `ClassBuild` lists, chooses only Effects that have at least one Target compatible with the chosen Activation, chooses a compatible orthogonal Targeting preset, and selects only Modifiers accepted by production compatibility. `ClassBuild.valid()` is the final syntax/budget gate. The grammar guarantees a real resolution source for unarmed samples and does not fabricate impossible resource thresholds; semantic risks remain in the corpus intentionally.

Class System analysis also rejects unsupported Effect Family/Delivery/Targeting requests, missing Starting Kit carrier dependencies, fake constraint rebates, duplicate cost/constraint representations, and total Class Budget overflow.

Each fuzz run:

1. generates a version-stable build fingerprint;
2. validates production compatibility;
3. runs static analysis;
4. runs a configurable sample of non-BROKEN builds through real Headless gameplay;
5. records Runtime failures or dynamic positive loops;
6. aggregates classification, findings, vocabulary usage, and compatibility pairs.

The default sample cycles through WAIT spam, Active spam, resource farming, and a two-cell movement loop.

## Metrics

The report includes:

- `TOTAL_GENERATED` / `syntacticValid`;
- static `VALID`, `RISKY`, and `BROKEN`;
- `HEADLESS_RUNNABLE`, runtime failures, and dynamic failures;
- per-vocabulary appearances within every classification;
- `POTENTIAL_DEAD_VOCABULARY` for entries whose observed builds are overwhelmingly or exclusively BROKEN;
- static-analysis time, total fuzz time, and Headless turns/second.

The old single “Basic Effective Combination Density” label is retained only as a legacy JSON field. Human reports now show the following separately:

```text
(syntactically valid builds that are not BROKEN and have no runtime/dynamic failure)
-------------------------------------------------------------------------------
                         all syntactically valid builds
```

- `VALID density`;
- `RISKY density`;
- `BROKEN density`;
- `NON-BROKEN density = VALID + RISKY`;
- sampled Runtime failure rate;
- sampled exploit/infinite-loop rate.

NON-BROKEN density measures absence of a definite structural break, not high-quality or fun builds.

## Compatibility matrix

`compatibility.json` records observed pair totals and outcomes for:

- Trigger x Target;
- Condition x Effect;
- Effect x Modifier;
- Resource x Cost;
- Law x Restriction.
- Delivery x structured Effect operation;
- Selector x Coverage;
- Coverage x Filter.

Counts are empirical within the deterministic corpus. They are QA evidence about vocabulary reach, not an automatic delete or balance decision.

## Reproduction workflow

Run the canonical corpus:

```text
gradlew.bat ruleFuzz --args="--count 1000 --seed 12345 --trace-on-failure"
```

Use `fuzz.json` to locate `id`, derived `seed`, serialized build fingerprint, scenario, turn, and trace. The same master seed regenerates the same build at the same index; the per-build seed can also be used in a focused Scenario JSON. Fixes should preserve the original failing fixture as a regression whenever possible.
