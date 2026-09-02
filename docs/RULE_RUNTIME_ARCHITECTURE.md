# Rule Runtime Architecture

## Player-build authority (V0.1 integration pass)

`ClassBuild` remains the authoritative blueprint. Its foundation budget is supplied only by
`ClassBudgetPolicy` (35); builder screens, runtime presentation, analyzer, fuzzer, reference builds,
DEV Rule Lab and migrations do not define private maxima. Old fixed-slot 14/16-point data is input
to `ClassBuildMigrator` only and is normalized to the current foundation budget.

Formal build-wide content is exposed by `LawTraitRegistry`. Laws remain a small set of broad rule
reinterpretations. Parameterized `TraitSpec` values carry resource or Mode bindings for narrower
cross-system feedback. Runtime instances retain independent RuleDefinition state, delayed payloads,
marks, bridges and counters exactly as before.

Player validation is a separate presentation contract: `PlayerFacingBuildValidator` translates
the structural, compatibility, budget and integrity checks into field-specific localized issues.
It does not alter execution semantics.

## Scope

Rule Runtime V0.2 is a small Java composition layer. It does not embed a scripting language and does not replace SPD's Actor, Buff, Item, Level, or Talent systems. It describes when a rule runs, which saved conditions must pass, how it pays, which generic target selector it uses, which existing gameplay effect it invokes, and how a modifier changes execution.

The implementation lives in `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules`. It is not named for CREATE A CLASS: a future Hero, Item, Organ, Device, Trap, or synthesis result can own independent rule instances and dispatch an appropriate `RuleContext`.

## Player-facing semantic presentation

Gameplay modules and presentation are intentionally separate. `RuleSemanticFormatter` converts a saved `RuleDefinition` into a localized Technique name and natural description; it suppresses `ALWAYS` and never exposes enum identifiers, priority, or runtime order. `CustomClassSummaryFormatter` deterministically creates the short Hero Select identity and the grouped Class Overview/build sheet.

`RuleDefinition.debugDescription` remains available only to DEV RULE LAB. Normal UI uses `RuleDefinition.description`, which delegates to the semantic formatter. Resource tooltips describe only their Resource Engine; the complete player build is shown in `WndClassOverview`, while counters and cooldowns remain in DEV RULE LAB.

## Saved model

- `RuleEvent`: stable gameplay event vocabulary.
- `RuleContext`: transient owner, source, target, selected/current cell, Item, Buff, amount, and melee metadata. It is never saved.
- `RuleDefinition`: one installed rule instance. Its Trigger, ordered AND Conditions, Cost, Target, Effect, Modifier, priority, and creation order form the saved definition. Cooldown and trigger count are independent saved instance state.
- `RuleRuntime`: per-owner Resource Engine, Class Law, restrictions, rules, counters, capacity, and dispatch state.
- `ClassBuild`: the authoritative schema-versioned, slot-free player blueprint. It owns independent Resource specs, unified Skill specs, Laws, Traits, Restrictions, Starting Kit, progression envelope, and total budget.
- `SkillSpec`: the common active/reactive model which compiles through a compatibility adapter into the existing `RuleDefinition` runtime.
- `EffectSpec`: the saved family/operation payload. Full Skill Runtime V0.1 adds real Delivery/Targeting execution, typed/scaled damage, status stacking, entity/carrier/relation state, and explicit fail-closed capabilities; see `FULL_SKILL_VOCABULARY_RUNTIME_V0_1.md`.
- `CustomClassConfig`: a compatibility envelope and legacy projection. Old fixed-slot saves migrate explicitly to `ClassBuild`; it is no longer the authoritative builder model.

Two equal `RuleDefinition` objects do not share cooldown or counters. V0.2 intentionally avoids a large definition/state rewrite: independent factory construction plus saved per-instance state is sufficient for multiple identical rules today.

## V0.2 module vocabulary

Events: `ON_TURN_START`, `ON_MOVE`, `ON_ATTACK`, `ON_HIT`, `ON_DAMAGED`, `ON_KILL`, `ON_ITEM_USE`, `ON_ENTER_TILE`, `ON_STATUS_APPLIED`, `ON_LOW_HP`, `ON_WAIT`, and `ACTIVE`.

Resources:

- Mana: gains 1 every three Hero turns.
- Rage: gains from effective damage and melee hits; fades after leaving combat.
- Blood: no pool; HP pays technique costs and cannot be reduced to zero by the base cost.
- Momentum: consecutive moves gain increasing resource; stopping decays it and WAIT clears it.
- Focus: WAIT grants 2; consecutive safe turns grant more; damage clears Focus and its safe-turn chain.
- Affliction: any accepted SPD `Buff` whose type is `NEGATIVE` grants resource. Poison and Burning therefore work without effect-specific resource hardcode.

Conditions are ordered and use explicit AND semantics: `SELF_HP_BELOW`, `TARGET_HP_BELOW`, `TARGET_HAS_POISON`, `TARGET_IS_BURNING`, `SELF_IN_WATER`, `DISTANCE_AT_LEAST`, `ADJACENT_ENEMIES_AT_LEAST`, and `RESOURCE_AT_LEAST`. Parameters and ordering are Bundle-saved. `ALWAYS`, `TARGET_EXISTS`, and the V0.1 `HERO_BELOW_HALF` value remain available for compatibility.

Targets: `SELF`, `ATTACKER`, `HIT_TARGET`, `SELECTED_TARGET`, `SELECTED_CELL`, `NEAREST_ENEMY`, `ALL_ADJACENT_ENEMIES`, and `CURRENT_TILE`. V0.1 `ATTACK_TARGET` restores as the same event-target semantics.

Effects reuse SPD gameplay code: PUSH and PULL use `WandOfBlastWave`; POISON, BURNING/FIRE, BLEED, SLOW, HASTE, Barrier SHIELD, and status CLEANSE use existing Buffs; TELEPORT uses `ScrollOfTeleportation`; SWAP uses `Char.interact`; CREATE_WATER uses `Level.setCellToWater`; CREATE_GAS seeds `ToxicGas`; HEAL uses the class-law-aware recovery hook.

Modifiers:

- AREA expands resolved cells by a saved map radius before applying the Effect.
- REPEAT invokes a compatible Effect multiple times but pays the Cost once.
- EXTEND_DURATION multiplies duration for duration-bearing Effects.

Incompatible pairs are rejected by `RuleModifier.compatible`; the creator hides them and runtime execution rejects them again. REPEAT cannot silently repeat TELEPORT/SWAP, and EXTEND_DURATION is limited to duration effects.

Class Laws:

- Healing becomes Shield.
- Resource overflow becomes Shield.
- Status Absorption grants resource while leaving the negative status intact.
- Water Affinity increases positive resource gains while standing in water and reduces resource
  costs above one by one; it never makes a cost free.
- Predator Rhythm accelerates rule cooldowns and grants resource on kills.

Gameplay restrictions: no ordinary weapons, weak healing potions, frail body, no traditional healing, WAIT clears resources, and active techniques cost additional HP.

## Deterministic execution and recursion safety

`RuleRuntime` keeps an `ArrayList`, assigns every installed instance a monotonically increasing saved `runtimeOrder`, and executes a snapshot sorted by:

1. higher explicit priority first;
2. lower creation/runtime order first.

No `HashMap` or `HashSet` iteration defines execution order.

Nested events are permitted so one rule can feed another. The dispatcher tracks a maximum execution depth of four and a set of currently executing rule instance orders. A rule cannot re-enter itself while its Effect is active, but other rules can respond to the nested event. Both protections unwind in `finally` blocks.

## Expressiveness primitives

The expressiveness pass adds five small primitives to the same `RuleContext`, `RuleDefinition`, `RuleRuntime`, and `RuleTrace` path. They are runtime infrastructure, not new player-facing vocabulary.

- **Semantic tags:** `RuleEffect.tags()` derives a minimal queryable set from the existing Effect. PUSH/PULL provide `FORCED_MOVEMENT`; TELEPORT provides `MOVEMENT` and `TRANSLOCATION`; current status, recovery, protection, removal, terrain, and mark effects expose only the tags that are already needed. `RuleDefinition` delegates tag queries to its Effect.
- **Causal event chains:** every dispatched event receives saved-runtime-monotonic event, parent, and root IDs plus its original event and source rule. A real rule-applied Buff emits `ON_STATUS_APPLIED` only after SPD accepts the Buff. The nested context preserves source, target, cell, original event, and trace ancestry.
- **Event bridges:** a saved `RuleEventBridge` maps a semantic tag to an additional Rule event. The initial production bridge is `FORCED_MOVEMENT -> ON_MOVE`. The original event is retained, and a per-chain event set rejects A-to-B-to-A remaps before dispatch. The existing execution-depth and same-rule guards remain a second safety boundary.
- **Marks/tokens:** `RuleMark` is a real neutral SPD Buff. It saves type, stacks, source actor ID, owner actor ID, and Buff cooldown; it therefore supports apply, check, stack, consume, expiry, and removal using normal Actor scheduling. `RuleMarkCondition` and `RuleMarkEffect` are generic modules rather than a HUNTED-specific skill.
- **Delayed payloads:** `RuleDefinition.delayTurns` schedules a copied Effect/Modifier and resolved target identity in a `RuleDelayedPayload` Actor. Cost and conditions are evaluated once when scheduled; the Effect runs through the normal Rule effect path after N turns. Runtime save data stores every pending payload, remaining scheduler time, target/source identity, and causal provenance. Reload reattaches the Actor with its remaining delay.

Forced movement completion is emitted from `WandOfBlastWave` only after its real callback mutates the target cell. Rule-applied Poison, Burning, Bleeding, Slow, Haste, and Barrier similarly emit their status event after the real Buff exists. Headless tests therefore exercise production effects rather than synthetic event injection.

## Core Rule Vocabulary V0.1

The first player-content batch is build-wide vocabulary layered on the same dispatcher. In the current slot-free `ClassBuild`, Traits are an unrestricted list bounded by total Class Budget rather than two fixed slots. The creator filters entries whose required Effect, event, law, or companion entry is absent.

| Player entry | Existing runtime primitives and exact V0.1 semantics |
| --- | --- |
| Accumulation | The selected reaction's MOVE, HIT, DAMAGED, or WAIT event adds an `ACCUMULATION` token. Three stacks become one `CHARGED` token; the next compatible power-scalable technique consumes it for 1.5x Effect power. |
| Overdraw | `RuleCost.RESOURCE` delegates to the Runtime. Each missing point costs 2 HP and payment is rejected if it would reduce the Hero below 1 HP. Blood cannot select it. |
| Overflow | Positive resource gain is capped normally. Every two excess points become one real Barrier point; the saved fractional remainder is never converted back into resource. It cannot be combined with the pre-existing overflow Class Law. |
| Compensation | Only after conditions and payment succeed, if every resolved primary Effect reports failure, the Hero receives 2 Barrier and a five-turn lock token. A merely unmet condition does not compensate. |
| Phase Shift | Crossing to 30% HP or less applies a real `LOW_PHASE` token. While present, otherwise-unmodified compatible techniques use the existing AREA modifier; leaving the threshold removes the token. |
| Echo | One successful ACTIVE technique schedules a 0.5x copied Effect for three turns later using `RuleDelayedPayload.Kind.ECHO`. An Echo is a payload, not a new technique execution, so it cannot schedule another Echo. |
| Hunt Mark | Each real HIT adds a source/owner-aware HUNTED stack to that enemy. At three stacks, the next successful technique against it consumes the mark and applies real Slow. |
| Propagation | On a Hero-attributed KILL, up to two HUNTED stacks move to the nearest living enemy. `transferDepth=1` makes the transferred mark ineligible for another propagation. This entry requires Hunt Mark. |
| Inertial Link | Installs the saved semantic bridge `FORCED_MOVEMENT -> ON_MOVE`; real PUSH/PULL completion can therefore drive a movement reaction while retaining original-event provenance. |
| Arrival Link | Installs `TRANSLOCATION -> ON_ENTER_TILE`; real TELEPORT/SWAP completion can drive tile-entry rules without pretending every forced movement is a voluntary step. |
| Translocation Backlash | A real TRANSLOCATION tag applies the existing Slow Buff to the Hero for three turns. |
| Bulwark Backlash | A rule-generated PROTECTION tag applies the existing Vulnerable Buff to the Hero for three turns. |

The last four rows are the two player-selectable Event Remap instances and the two configurable Backlash instances required by the ten conceptual vocabulary families. No player-facing text exposes mark IDs, tags, payload kinds, bridge provenance, or enum identifiers.

Vocabulary event processing uses the existing causal context. Before-event handlers establish state needed by rules (Hunt Mark and phase state); rules then execute in their saved deterministic order; after-event handlers perform accumulation and propagation. Semantic-tag backlashes and bridges remain children of the Effect that emitted the tag, so the existing depth, source-rule, and bridge-cycle guards cover all of them.

## Legacy V0.2 capacity

Base Rule Capacity is 14. A Class Law, each Trigger, Target, Effect, and Modifier consume capacity; Conditions narrow applicability and cost zero. The one selected Restriction adds its saved bonus:

- no ordinary weapons: +3;
- weak healing: +1;
- frail body: +2;
- no traditional healing: +3;
- WAIT clears resource: +2;
- active techniques cost HP: +2.

Old fixed-slot saves retain these values during migration. New builds use the Class Budget described in `CLASS_SYSTEM_V0_1.md`; CREATE A CLASS refuses over-budget, structurally unsupported, or integrity-BROKEN builds. DEV RULE LAB keeps a saved debug allowance so isolated regression rules can be installed deliberately.

## SPD integration points

| Existing class | Integration |
| --- | --- |
| `Dungeon.init()` | Consumes pending custom config after standard Hero initialization. |
| `Hero.act()` / `Hero.rest()` | Dispatches turn start and real WAIT/rest turns. |
| `Hero.move()` | Dispatches MOVE and ENTER_TILE after a real position change. |
| `Hero.attack()` | Dispatches ATTACK and successful HIT with its target/melee flag. |
| `Hero.damage()` | Dispatches effective DAMAGED with actual source, then LOW_HP at 30% or less. |
| `Hero.add(Buff)` | Dispatches STATUS_APPLIED only after SPD accepts the Buff. |
| `Mob.die()` | Dispatches KILL for an enemy directly killed by the Hero. |
| `Item.execute()` | Dispatches ITEM_USE for non-drop item actions. |
| `EquipableItem.execute()` | Blocks ordinary weapon equip when restricted. |
| `PotionOfHealing.heal()` | Applies weak/no-healing restrictions or converts healing to Barrier. |

Every hook immediately no-ops when `Hero.ruleRuntime()` is absent. Vanilla classes therefore retain their original paths.

## Save/load compatibility

`Hero.storeInBundle()` writes `rule_runtime` only when installed. Runtime save data contains the authoritative `ClassBuild`, primary and additional resource states, engine counters, all Laws/Traits/restrictions, progression, next stable order, all rule instances/modules/state, event provenance counters, Bridges, Marks, and delayed payloads. Conditions are a saved collection.

V0.1 saves without Class Law or engine counters receive stable defaults. A V0.1 single `condition` child is migrated into the V0.2 `conditions` list, rules without a saved order receive one during restore, and fixed-slot custom configurations are migrated by `ClassBuildMigrator`. New blueprints store `class_build_schema_version=1`; unknown schemas fail explicitly. Vanilla saves without `rule_runtime` are unchanged. Save previews read only the custom name child.

## Temporary Warrior progression shell

V0.2 still uses Warrior class initialization and talent plumbing. Runtime installation now detaches and discards the Warrior-only Broken Seal and its shield Buff, preventing the most visible equipment law leak. Warrior starting weapon/identification and Warrior talent/subclass/mastery plumbing remain temporary compatibility behavior. Replacing that progression shell belongs to the later dedicated Technique/Talent/Specialization stage, not this pass.
