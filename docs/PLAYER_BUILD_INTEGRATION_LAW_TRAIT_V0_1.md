# Player Build Integration + Law/Trait Vocabulary V0.1

## Budget authority audit

The former player builder inherited `ClassBuild.BASE_BUDGET = 16`, while archetype QA declared a
private `CLASS_BUDGET = 35`.  Pre-ClassBuild saves also carried `CustomClassConfig.BASE_CAPACITY`
(14 plus a restriction rebate).  These were three independent sources of truth.

`ClassBudgetPolicy` is now the only foundation authority. New player builds, `ClassBuild`, the
builder, formatter, integrity analyzer, reference builds, stress QA, fuzzer, DEV Rule Lab and save
migration all receive a foundation budget of **35**. Progression additions and explicit
restrictions are the only saved adjustments. A stored legacy 14/16-point base is treated as an old
schema limit and migrates to 35; the old value is never retained as the class's permanent ceiling.

## Player validation

`PlayerFacingValidationIssue` carries a stable code, severity, affected field, short message and
detail message. `PlayerFacingBuildValidator` collects field failures and budget failures together,
and the skill editor keeps the complete localized list visible next to Save. The class sheet also
collects every blocking issue instead of merely disabling Create.

The player-facing codes are missing selection, incompatible delivery, incompatible target,
incompatible effect/affix, missing effect parameter, missing resource, invalid cost, invalid
constraint, incompatible secondary effect, unsupported capability, skill/class budget overflow,
broken build integrity and incompatible Law/Trait dependency.

## Law and Trait boundary

Laws are broad class-wide reinterpretations. The formal Registry contains five:

| Law | Budget |
| --- | ---: |
| Healing Becomes Shield | 5 |
| Forced Movement Counts as Movement | 4 |
| Translocation Counts as Entering a Tile | 3 |
| Resource Overdraft Uses Health | 4 |
| Owned Actions Count as Yours | 5 |

Traits connect narrower pieces of a build. The formal Registry contains twenty:

| Trait | Budget | Binding |
| --- | ---: | --- |
| Accumulation | 2 | action event |
| Overflow | 3 | resource |
| Compensation | 2 | failable effect |
| Phase Shift | 3 | health threshold |
| Echo | 4 | active skill |
| Hunt Mark | 2 | repeated target |
| Propagation | 2 | Hunt Mark |
| Status Feedback | 3 | resource |
| Water Flow | 3 | resource |
| Kill Tempo | 2 | kill event |
| Kinetic Mark | 3 | forced movement |
| Mobile Charge | 3 | resource |
| Temporary-Life Payment | 4 | temporary life + health cost |
| Piercing Mark | 3 | projectile/beam |
| Owned Resource Feedback | 4 | resource + owned actor |
| Carrier Resource Feedback | 4 | resource + carrier/device |
| Hazard Feedback | 3 | resource + hazard + forced movement |
| Mode Guard | 3 | selected mode |
| Transfer Feedback | 3 | resource + transfer/copy |
| Status Chain | 3 | status + mark |

Resource- and Mode-dependent Traits use `TraitSpec`, not resource-specific enums. Their stable ID
includes the selected binding, and that binding is serialized, formatted, budgeted and checked by
the same Registry used by the builder.

The former Overflow-to-Shield, Status Absorption, Water Affinity and Kill Acceleration Laws migrate
to Overflow, Status Feedback, Water Flow and Kill Tempo Traits. Overdraw and the two event bridges
migrate to their corresponding Laws. Legacy enum values remain load adapters only; new builds do
not save them as authoritative content.

## Shared QA contract

`lawTraitVocabularyQa` checks Registry exposure, English and Simplified Chinese name/summary/detail,
non-zero budget, runtime mapping, formatter coverage, compatibility and ClassBuild roundtrip.
`playerBuildEquivalenceQa` rebuilds archetypes and Law/Trait references through the same public
registries and verifies their parameters, budget, and save roundtrip. A QA fixture which the player
builder cannot construct is a failing result.

The build overview is a deterministic description derived from real resources, deliveries,
effects, marks, terrain, ownership, modes and reactions. It is presentation only and never assigns
a hidden class domain.
