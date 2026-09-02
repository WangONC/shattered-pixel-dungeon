# Full Skill Vocabulary Runtime V0.1

## Runtime boundary

`ClassBuild` and `SkillSpec` remain authoritative. A `SkillSpec` compiles into the existing saved `RuleDefinition`; `RuleRuntime` still owns deterministic ordering, causal event chains, recursion guards, costs, cooldowns, marks, bridges, and delayed payloads. The new execution adapters are `SkillTargetResolver`, `SkillEffectRuntime`, `WorldCapabilityValidator`, `RuleOwnedEntity`, and `RuleCarrierTrap`.

No parallel simulator or scripting engine was added. Effects call production `Char.damage`, Buffs, `Ballistica`, `Level.set`, Blob seeding, Trap placement, `DirectableAlly`, and Actor scheduling. Unsupported operations fail structural validation and are disabled in CREATE A CLASS.

## Effect family coverage

| Family | Status | V0.1 execution |
| --- | --- | --- |
| Damage | FULL (V0.1 scope) | Fixed, Hero-level, and current-resource scaling; standard, percent, missing-HP, and capped execute variants; untyped, Burning, Poison, and Bleeding source classes use SPD immunity/resistance. Boss/miniboss execute is capped and cannot directly finish the target. |
| Status | FULL (V0.1 scope) | Existing Poison, Burning, Bleeding, Slow, Haste, Paralysis, Roots, Amok, Terror, and Vulnerable Buffs; strength/duration plus native refresh, extend, or replace stacking. |
| Movement | FULL (V0.1 scope) | Existing Push, Pull, Teleport, and Swap plus structured Dash and Throw; destination/collision validation remains in SPD movement code. |
| Recovery / Defense | PARTIAL | Heal, Barrier, independent expiring Temporary HP, Mitigation, Cleanse, and guarded one-way Redirect. Revive is deliberately unavailable because safe death/game-over restoration is not a local Buff operation. |
| Resource Operation | FULL for owner pools | Gain, Drain, Convert, Reserve/return, and timed Suppress use stable pool IDs. Spending remains a Cost. Cross-owner Resource transfer is engine-limited because SPD has one authoritative player resource owner. |
| Mark / Accumulation | FULL | Apply, stack, counter, consume, expiry, remove, and bounded spread use the saved source/owner-aware `RuleMark` Buff. |
| Create Entity | FULL (minimal templates) | Real Actor ally, stationary device, field carrier, and saved trap carrier. Actor count is capped at six; count/lifetime/period/payload affect budget. QA actors use a reused Rat sprite and real `DirectableAlly` scheduling. |
| World / Terrain | PARTIAL | Water, grass, internal wall destruction, Fire/ToxicGas Blob creation, hazard clear, and trap placement. Map-array growth, boundary destruction, and protected entrance/exit/locked structure changes are rejected. |
| Relation / Control | PARTIAL | Saved Ownership and Link, Follow/Attack/Guard commands, Break/Release, and whitelist inheritance of current Mode, Haste, or Mitigation. No RTS command UI or arbitrary RuleRuntime inheritance. |
| Transfer / Copy | PARTIAL | Whitelist status copy, Mark transfer, and Barrier swap. Unknown/Boss/progression/class-schema state is rejected; arbitrary Buff/RuleRuntime and cross-owner Resource copying are not implemented. |
| Transform | ENGINE-LIMITED / PARTIAL | Saved timed Mode Shift and `MODE_IS` conditions are real. Full polymorph, Actor-class replacement, arbitrary capability override, and behavior override are disabled because they cannot preserve Actor identity, equipment, AI, and saves safely in the current adapter. |

## Delivery coverage

| Delivery | Status | Semantics |
| --- | --- | --- |
| Self | FULL | Resolves the owner only. |
| Contact / Attack | FULL | Requires adjacency and delivers through the common effect path; it does not fabricate a second weapon attack. |
| Direct Target | FULL | Applies orthogonal selector/coverage/filter resolution. |
| Projectile | FULL | Real `Ballistica.PROJECTILE` path, range, terrain stop, actor collision, Pierce, Repeat, Echo, Delay, and primary/secondary on each hit. |
| Trace / Beam | FULL | Real line path through `Ballistica`, first/all-hit behavior, piercing, and cell/actor effects. |
| Ground Placement | FULL | Valid selected/current cell is the actual terrain/entity/carrier placement point. |
| Persistent Carrier | FULL (minimal) | Saved Actor-backed field/device carrier with owner, filter, period, lifetime, primary payload, and optional secondary payload. |
| Action Attachment | FULL | Saved generic Buff attaches primary plus optional secondary to the next N selected gameplay events, consumes before executing, and preserves recursion provenance. |

## Targeting coverage

Selectors `SELF`, `SELECTED_ACTOR`, `SELECTED_CELL`, `NEAREST`, deterministic seeded `RANDOM`, and `ALL_MATCHING` execute independently from coverage and filter. Coverage supports `SINGLE`, `ADJACENT`, `RADIUS`, collision-limited `LINE`, deterministic grid `CONE`, `RING`, and nearest-valid no-duplicate `CHAIN`. Filters support Enemy, Ally, Self, Owned Entity, Marked/minimum stacks, Has Negative Status, HP threshold, and a conservative compatible-entity check.

Dash requires an unoccupied selected cell; entity/world operations require cell-capable targeting; owner Resource/Mode operations require Self. These constraints are checked by `EffectSpec.compatibleTargeting`, `SkillSpec.structurallyValid`, Builder option availability, and Build Integrity.

## Modifiers, cost, and budget

AREA, REPEAT, EXTEND_DURATION, INTENSITY, PIERCE, BOUNCE, and DELAY change delivery/effect execution rather than adding a hidden second effect. Echo and Propagation remain the existing saved Trait/vocabulary implementations. A Secondary Effect is separately budgeted, limited to one, applied to every successful primary target, shares Delay, and is saved with Attachment/Carrier payloads; it cannot own another Secondary.

Resource, HP, Action, Cooldown, Consumable, and State costs remain centralized in `RuleCost`. Active Action cost uses `Hero.spendAndNext` / the real headless Actor scheduler. Effect strength/type/scaling/stacking, target coverage/range/count, modifiers, entity count/lifetime/period, delivery persistence, Cost rebates, and Constraints all contribute to Class Budget.

## Persistence and compatibility

No `class_build_schema_version` bump was needed. The model already saved `EffectSpec`, Delivery, Targeting, and Secondary objects. V0.1 adds optional `EffectSpec` fields for damage type, scaling source, and status stacking; absent fields restore to untyped, fixed, and native-refresh behavior. Owned entities, ownership/link relations, carrier payloads, attachment primary/secondary payloads, temporary HP, redirect/mitigation, mode, reserve/suppress state, and delayed payloads all implement Bundle persistence.

Old fixed-slot builds still migrate through `ClassBuildMigrator`. Existing Rule Runtime state and old LEGACY Effect adapters are unchanged.

## Reference builds

The Headless corpus contains seventeen non-player QA builds: ammo projectile, piercing beam, cone forced movement, chain status, temporary-HP bruiser, saved owned summon with real Actor turns, summon/Mark, saved persistent field, terrain control, mode shift, status copy, reaction-only, saved next-attack attachment with Secondary, trap carrier, entity expiry, Blob creation, and owned command plus whitelist inheritance. They are regression fixtures and are not exposed as preset professions.

## Explicit engine limits

- No Level array expansion or digging outside the map.
- No full polymorph/Actor-class replacement.
- No arbitrary capability/AI override.
- No arbitrary Buff, Boss/story state, progression, save identity, or RuleRuntime transfer/copy.
- No cross-Hero resource transfer in a single-Hero SPD run.
- No Revive until death/game-over and save restoration can be made safe without replacing core engine flow.
- Device/Object V0.1 uses a minimal Actor/Trap carrier abstraction; it is not a general Level object framework or Engineer domain.

