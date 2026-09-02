# Vocabulary Foundation V0.1 — Frozen

## Decision

The base Class vocabulary is frozen at V0.1. The class-archetype stress matrix can express the
ten primary reference playstyles with comparable budgets, distinct behavior signatures, scenario
specialties, no detected comparable-budget dominance, and zero runtime failures. Future work moves
to progression, Tier 1–4 content, specialization, and authored class growth.

This is not a claim that every random build is fun or balanced. A new base gameplay family is only
permitted when authored content exposes a reproducible, cross-build primitive gap which cannot be
expressed by the components below. A weak reference build is not sufficient evidence.

## Frozen vocabulary surface

Effect families:

1. Damage
2. Status
3. Movement
4. Recovery / Defense
5. Resource Operation
6. Mark / Accumulation
7. Create Entity
8. World / Terrain
9. Relation / Control
10. Transfer / Copy
11. Transform

Delivery remains orthogonal to effects: Self, Contact / Attack, Direct Target, Projectile, Trace /
Beam, Ground Placement, Persistent Carrier, and Action Attachment.

Targeting remains three-dimensional:

- Selector: Self, Selected Actor, Selected Cell, Nearest, Random, All Matching.
- Coverage: Single, Adjacent, Radius, Line, Cone, Ring, Chain.
- Filter: Enemy, Ally, Self, Owned Entity, Marked, Has Status, HP Threshold, and compatible entity.

Modifiers remain execution-shape changes, not hidden secondary effects: Area, Intensity, Pierce,
Bounce, Duration, Repeat, Echo, Delay, and Propagation. A second gameplay effect continues to use
the single optional Secondary Effect slot and cannot recursively create another secondary.

Costs remain Resource, HP, Action, Cooldown, Consumable, and State. Constraints remain Target,
Self State, Position, Timing, Commitment, and Frequency. The saved limited-use constraint and
manual finite resource pool are the two general primitives added by the archetype stress phase.

Resources are still data composed from pool, gain/loss/decay, overflow, and persistence behavior.
Current concrete engines are Mana, Rage, Blood-as-HP-cost, Momentum, Focus, Affliction, and Manual.
Manual is the non-regenerating base for ammunition, fuel, and charges; reload/refuel is an ordinary
resource-gain Skill and pays real action time.

## Runtime scopes

Create Entity uses real Actor-backed owned creatures and stationary Device/Field carriers, plus
saved trap payloads. Ownership, lifetime, period, count, placement, commands, and payloads remain
budgeted. It is not an Engineer or Summoner domain.

Terrain mutations remain behind `WorldCapabilityValidator`: internal replaceable/destructible
terrain, water, grass, supported Blobs, traps, plants, and hazard clearing are allowed only where
the Level operation is legal. Boundaries, entrances, exits, locked/protected structures, and Level
array expansion remain rejected.

Relation/Control remains saved ownership, typed link/bind, small follow/attack/guard commands,
whitelisted inheritance, and break/release. Transform remains temporary Mode, Capability, and
Behavior overrides. Full actor-class polymorph is outside the frozen base.

Transfer/Copy remains fail-closed and capability-whitelisted for resource, transferable status,
mark/stack, barrier, and temporary HP cases. Arbitrary RuleRuntime, progression, boss state, story
state, or identity copying is intentionally unsupported.

## Budget principles frozen by the gap pass

- Projectile and beam delivery include their ranged safety value.
- Range beyond six cells, expanded coverage magnitude, and multi-target capacity have explicit
  cost instead of being free parameters.
- Pierce and Repeat scale with magnitude; long-range Pierce pays an additional synergy cost.
- Persistent Carrier pays for delivery plus lifetime/period persistence.
- One-shot traps are cheaper than an autonomous Actor/Device lifetime, but never free.
- World operations are priced by operation: water/grass/clearing are cheaper than damaging gas or
  fire; protected/destructive terrain remains capability-gated.
- Nominal cooldown rebate is discounted against the effect's natural replacement cadence. A
  cooldown shorter than an entity lifetime or normal defensive replacement interval is not a real
  restriction.
- Mark and water constraints continue to lose rebate when the same build can cheaply create the
  required mark/water itself.
- Starting Kit, entity count/lifetime, automation, range, coverage, persistence, secondary effects,
  Laws, and Traits all remain inside total Class Budget.

Water Affinity now has a decision-visible general rule: while standing in water, positive resource
gain increases and resource costs above one are reduced by one. It never makes a cost free.

## Freeze evidence

The final deterministic run used 15 ordinary `ClassBuild` fixtures, 12 shared scenarios, and 10
fixed seeds per pair: 1,800 real headless combat runs in 2.41 seconds. The ten primary budgets were
33–35 of 35 (6.06% spread), runtime failures were zero, and the conservative dominance matrix was
empty.

Terrain Controller changed from an 18.3% baseline to 60.0%; with equal enemy pressure it won 60%
in Terrain Rich and 0% in Terrain Poor, while remaining weak in ranged, sustained, and mixed-threat
scenarios. Device Engineer changed from 25.0% to 47.5% and retained carrier-led damage, deployment
downtime, and narrow/ranged specialties. Ammo Gunner retained an 86.7% ranged identity but fell to
40% in Swarm and 0% in Mixed Threat. Mark Assassin executed its Mark → setup → finisher loop.
Mode Shifter separated assault damage from guard temporary-HP absorption.

The Water + Focus + WAIT unbounded-shield scenario remains an intentionally preserved QA exploit.
It is not evidence for a missing vocabulary family and was not hidden or weakened by this pass.

## Intentionally unsupported

- full Polymorph/Form replacement with arbitrary Actor class swapping;
- Revive as a general user-authored primitive;
- arbitrary RuleRuntime, class schema, boss/protected state, or story-state copying;
- Level array expansion or digging outside generated map bounds;
- unrestricted permanent carriers or unbounded actor production;
- full RTS command UI and domain-specific automation factories.

Unless future authored content supplies reproducible cross-build evidence, these exclusions do not
reopen the foundation. Progression work should compose and upgrade the frozen vocabulary first.
