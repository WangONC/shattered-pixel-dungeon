# Class System V0.1

## Purpose

`ClassBuild` is the versioned, slot-free blueprint for a custom class. It replaces the old assumption that every class must have exactly one resource, one law, one active rule, one reaction rule, two vocabulary entries, and one restriction. The existing Rule Runtime remains the execution engine: `SkillSpec` compiles to independent `RuleDefinition` instances, preserving deterministic order, cooldowns, marks, bridges, delayed payloads, and save compatibility.

## Saved data model

```text
ClassBuild
  schemaVersion / name / baseBudget
  resources[]
  skills[]
  laws[]
  traits[]
  restrictions[]
  startingKit
  progression

SkillSpec
  activation
  conditions[]                 (ordered AND)
  primary EffectSpec
  optional secondary EffectSpec
  delivery
  TargetingSpec(selector, coverage, filter)
  modifier
  cost
  constraint
  priority / requiredCarrier
```

Zero resources, multiple resources, zero active skills, multiple active skills, reaction-only builds, and zero or multiple Laws/Traits are legal structures. Integrity and total budget, rather than slot count, decide whether the build can be created.

Each `ResourceSpec` has a stable pool ID. The first pool is retained as the legacy/visual primary pool and additional pools use independent saved `RuleResourceState` objects. Every Skill cost names the pool it spends, so equal Resource Engines cannot accidentally share runtime state.

## Effects, delivery, and targeting

`EffectFamily` defines the eleven design families: Damage, Status, Movement, Recovery/Defense, Resource Operation, Mark/Accumulation, Create Entity, World/Terrain, Relation/Control, Transfer/Copy, and Transform. `EffectSpec` stores the family, current production `RuleEffect` variant, strength, duration, and scalar parameter.

Only families and variants backed by current SPD gameplay are executable. V0.1 compiles the existing Damage-over-time/status, movement, recovery/defense, and terrain operations. Reserved Create Entity, Relation, Transfer/Copy, and Transform requests are rejected as `UNSUPPORTED_ENGINE_CAPABILITY`; the model does not pretend those systems exist.

`SkillDelivery` and `TargetingSpec` are separate. Self, Contact/Attack, Direct Target, Ground Placement, and Action Attachment compile today. Selector, Coverage, and Filter remain separately saved even where the current adapter accepts only a subset. Unsupported Projectile, Trace/Beam, Persistent Carrier, Random/All Matching, Line/Cone/Ring/Chain, Ally/Owned filters are rejected rather than silently approximated.

World effects call `WorldCapabilityValidator`. Protected entrance/exit cells, chasms, solid boundary structures, and illegal Blob cells are rejected before `Level` mutation. Current capabilities are deliberately limited to what SPD supports: replaceable floor, water placement, and Blob seeding.

## Costs and constraints

One `RuleCost` supports Resource, HP, Action time, Cooldown, finite Consumable, and saved State payment. Resource and HP use existing runtime paths; Action changes real Actor time after a successful active use; Cooldown compiles into saved rule cooldown; Consumable detaches a real matching Item from Belongings; State consumes a real `RuleMark` stack.

Costs use one conservative budget-rebate table. Skill constraints are Target, Self State, Position, Timing, Commitment, or Frequency constraints. Effective constraint rebate is evaluated against the complete build: Hunt Mark discounts a marked-target constraint and Create Water discounts a water-only constraint. Duplicate cooldown cost/constraint pairs and an HP commitment without HP cost are structurally rejected.

## Class budget and integrity

Class budget is:

```text
starting kit + resources + skills + laws + traits
- effective skill constraint rebates
<= base budget + progression budget + restriction rebates
```

Skill power includes the primary core effect, optional secondary effect, target/coverage adapter, modifier, trigger automation, delivery, and conservative Cost rebate. Secondary effects execute inside the owning rule's existing recursion guard and cannot create a second hidden Skill runtime.

`RuleBuildAnalyzer` checks engine support, budget overflow, victory path, declared resource sources/sinks, invalid cost sources, target/effect compatibility, duplicate/no-op rules, dead conditions, automatically satisfied constraints, and required starting carriers. `VALID`, `RISKY`, and `BROKEN` are QA classifications, not balance or fun ratings.

## Builder and in-game sheet

`WndCreateClass` is an editable, scrollable Build Sheet rather than a class-wide wizard. The player can add, edit, or remove any Resource, Skill, Law, Trait, or Restriction; configure the Starting Kit; inspect live used/maximum/remaining budget and integrity status; then open a compact preview or detailed player-facing overview. Each Skill has a small local editor for Activation, Condition, primary/secondary Effect, Delivery, Targeting, Modifier, Cost, and Constraint.

`WndClassOverview` uses `ClassBuildFormatter`. It displays localized player language and current progression additions, never runtime order, internal IDs, bridge provenance, payload kinds, or enum names. The current editor authors one primary condition per Skill, while the saved model, migration, runtime, formatter, and tests preserve multiple ordered AND conditions.

## Save and migration

New blueprints store `class_build_schema_version=1` and a Bundled `class_build`. An unknown schema throws explicitly instead of restoring partial data. `CustomClassConfig` remains a compatibility envelope for old menus and pending-new-game plumbing.

`ClassBuildMigrator.fromLegacy` maps the old resource/law/two rules/two vocabulary entries/restriction into free lists. No-weapon builds migrate to the Unarmed Starting Kit. `toLegacyProjection` keeps old save-preview and DEV plumbing operational but never replaces the authoritative `ClassBuild`.

## Progression and Starting Kit limits

`ClassProgression` saves the official cadence envelope (Tier 1, Tier 2, Tier 3/specialization, high-level, Tier 4), added budget, and localized addition records. Actual talent-tier editing UI is intentionally not implemented in V0.1.

Starting Kit currently exposes Standard Dungeon Kit and Unarmed. It removes the Warrior Broken Seal and moves the initial weapon to the backpack for Unarmed/no-weapon builds. Custom carrier grants and priced special equipment remain unsupported; a Skill declaring a carrier that the kit does not provide is rejected. Warrior talent/subclass plumbing remains a temporary compatibility chassis.
