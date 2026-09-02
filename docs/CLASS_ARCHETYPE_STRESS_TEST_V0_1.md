# Class Archetype Stress Test V0.1

This is gameplay QA infrastructure, not a catalogue of official classes. Reference builds are
ordinary `ClassBuild` values assembled from the same `SkillSpec`, resource, law, trait, cost,
constraint, starting-kit, delivery, targeting, and effect components available to CREATE A CLASS.
No production runtime branches on an archetype ID.

## Reference builds

The ten primary fixtures are Martial Defender, Blood Berserker, Ammo Gunner, Area Caster, Mark
Finisher, Owned Summoner, Device Automator, Terrain Controller, Status Controller, and Mode
Shifter. Five additional structural probes cover reaction-only, resource-less cooldown,
dual-resource, transfer/copy support, and low-damage pure control builds. Primary builds target
the shared 35-point QA budget; starting kits are explicitly `UNARMED` and still use the normal
starting-kit budget calculation.

The DEV RULE LAB exposes these under **Reference Builds — DEV / QA Only**. Loading one installs its
normal compiled `ClassBuild` into `RuleRuntime`; the list is never added to Hero selection.

## Scenario matrix and policy

Every build runs against the same twelve scenarios: normal single target, high-HP target, swarm,
ranged pressure, narrow corridor, open room, resource starvation, sustained encounter, low-HP
start, terrain-poor, terrain-rich, and mixed threat. The default task uses five deterministic
seeds per build/scenario pair.

`ArchetypeStressHarness` uses real `Hero`, `Mob`, `Actor`, `Buff`, `Level`, and `RuleRuntime`
objects. Its one shared policy ranks legal skills by survival need, target count, resource state,
mode, persistence, and effect family; otherwise it attacks, moves toward a target, or waits. It
does not contain turn scripts or branches for individual archetype IDs. Enemy and Actor tie
ordering is stable by saved Actor ID, so a reported seed is reproducible.

## Metrics and reports

`gradlew.bat classArchetypeStressQa` writes machine-readable files under
`build/reports/ruleqa/`:

- `archetype_summary.json`: budgets, aggregate outcomes, behavior signatures, and A/B/C/D finding;
- `archetype_scenarios.json`: every raw build/scenario/seed run;
- `archetype_scenario_aggregates.json`: mean, median, min/max, and failure counts per pair;
- `archetype_behavior_profiles.json`: damage attribution, defense, actions, resources, marks,
  entities, terrain, modes, attachments, and failed attempts;
- `archetype_dominance.json`: conservative dominance/dominated leads at comparable budgets;
- `archetype_component_usage.json`: used skills, source/sink traffic, real cost payments, and
  low-signal components;
- `archetype_constraint_effectiveness.json`: nominal versus effective rebates and observed blocks;
- `archetype_specialty.json`: best/worst scenarios, relative strengths/weaknesses, and behavior signals;
- `archetype_component_value.json`: approximate damage, mitigation, control, resource, entity, and
  terrain value attribution by component/provenance;
- `archetype_missing_capabilities.json`: evidence for general primitives and remaining telemetry
  limits.

Dominance is a QA lead, not an automatic balance verdict. It compares scenario victory coverage,
single-target and crowd outcomes, time to victory, incoming damage, control, resource
sustainability, and action efficiency. A build is not classified as broken merely for being weak.

## General primitive evidence

Two small general gaps were exposed. `ResourceEngine.MANUAL` is a finite, non-regenerating pool
used through ordinary resource-gain skills; it supports ammunition, device fuel, and limited
charges. `SkillConstraint.LIMITED_USE` compiles to a saved per-rule use limit; it supports limited
deployment, emergency defenses, and high-impact control. Both participate in Class Budget,
Builder/runtime serialization, integrity analysis, and headless behavior tests.

No gunner, summoner, engineer, defender, or berserker domain/runtime was added. Precise legacy trap
damage attribution remains a reporting limitation: ownership and carrier use are measured, but an
untyped trap source can be grouped with player direct rule damage. Gameplay behavior is not
affected.

## Regression contract

The stress task is additive. The existing 48 headless scenarios remain in `headlessQa`, including
the intentionally failing-as-an-exploit Water + Focus + WAIT unbounded-shield finding. Old build
migration, save round trips, Vanilla no-rule behavior, build integrity, and rule fuzz remain
separate required gates.

## Vocabulary and balance gap pass

The follow-up gap pass did not add a gameplay primitive or archetype-specific runtime. It corrected
general pricing for projectile/beam safety, long range, Pierce/Repeat magnitude, persistent carrier
lifetime, and one-shot traps. Cooldown rebates now compare the stated cooldown with the owning
effect's natural reuse/replacement cadence. The shared policy now filters legal targets (including
marks/status), saves resources for unaffordable high-value skills, rotates repeated deployments,
and values real hazards without checking an archetype ID.

Terrain Poor and Terrain Rich now use the same enemy pressure. Rich supplies real SPD water,
internal geometry, a real trap, and a real Fire Blob. The Terrain fixture uses ordinary
Action/Cooldown/Focus costs to establish water, create a control window, seed a hazard, and defend
itself while in water. Device payload/lifetime and deployment diversity were calibrated without
changing Device Runtime identity. Pinning Round was redundant and was folded into the ordinary
secondary-status payload on Piercing Shot.

The final 10-seed matrix executed 1,800 runs with zero runtime failures. Primary budgets were
33–35/35, Terrain Rich outperformed Terrain Poor 60% to 0%, Device Engineer had narrow/ranged
specialties, Execute was observed as a real finisher, and Mode damage/mitigation separated between
assault/guard. No conservative dominance pair remained. The result is conclusion **D** and the
foundation contract is recorded in `VOCABULARY_FOUNDATION_V0_1_FROZEN.md`.
