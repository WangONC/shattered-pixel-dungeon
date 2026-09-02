# Player-facing Class Core + Combat UX V0.1

## Scope

This pass changes the player-facing abstraction without replacing `ClassBuild`, `SkillSpec`, or
the Rule/Skill runtimes. Skills still describe authored combat effects. Class-level mechanisms now
live in `ClassGameplaySpec` and generate saved `ClassOperationSpec` actions.

## Class-level gameplay components

- `BasicAttackProfile`: `WEAK` is a 55% ordinary click-attack fallback; `FULL` preserves ordinary
  weapon damage and is dynamically priced from independent ranged, area, sustainable, and
  owned-entity offense already present in the build.
- `ResourceSpec` + `ResourceRefillSpec`: an active refill declaration generates `RELOAD`; the
  player does not author a resource-gain Skill to simulate it.
- ownership, entity/device capacity, command, recycle, and mode declarations are held by
  `ClassGameplaySpec`.
- `ClassBuild.syncClassOperations()` deterministically grants `RELOAD`, `COMMAND`, `MODE_SWITCH`,
  and `RECYCLE`. Operations have their own stable identity and save payload.

The Builder presents these under **Gameplay Components / Core Mechanics**. Skills remain under a
separate Skills section, while laws, traits, and restrictions are grouped as Rules & Synergies.

## Runtime and combat presentation

- `ClassOperationRuntime` reuses real resource state, ownership, `DirectableAlly`, `RuleMode`, and
  actor removal paths.
- `Hero.performBasicAttack` is the only hook that applies the Basic Attack multiplier. Thrown
  weapons, weapon abilities, Skill damage, Projectile Skills, and vanilla heroes do not call this
  hook.
- `ClassActionBar` exposes ACTIVE Skills and ClassOperations as independent combat buttons. Up to
  four actions are direct; larger action sets use three direct actions plus an overflow list.
- `ClassActionIcon` maps effects, deliveries, and operations to different existing SPD item frames.
  The old `RuleAbility` book is an inert `MIGRATION_ONLY` serialization shell and has no actions.
- `ClassActionExecutor` renders Projectile deliveries with `MagicMissile` along the real
  `Ballistica` destination before resolving the existing Rule Runtime effect.
- `ClassResourceHUD` displays every class resource and pulses when a value changes.
- reload plays feedback, updates the HUD, and consumes its configured action time.

## Text input and mobile interaction

`WndTextInput` uses libGDX's platform text-input dialog on Android. This leaves composition to the
active Android IME and returns committed text for class, resource, and Skill names. Builder option
buttons and info buttons pass drag gestures to the surrounding `ScrollPane`; a drag cancels the
row click and remembered scroll position is restored when returning from details.

## Player-path QA

`playerClassCoreQa` creates an Ammo build only through `PlayerBuildAssembler`, the auditable
equivalent of ordinary Builder selections:

1. select `WEAK` ordinary attack;
2. add a six-point resource named `弹药`;
3. configure active, full, one-turn refill;
4. add `穿甲射击` as Damage + Projectile + selected enemy + range 6 + Pierce + one Ammo cost;
5. compile the real `RuleRuntime`, execute reload, and save/load the result.

The report asserts independent Skill/Operation HUD IDs, a visible Projectile presentation route,
distinct icons, Ammo HUD output, dynamic `FULL` attack cost, no actionable legacy book, and formal
Command/Mode/Recycle operations without fake Skills. It is written to
`build/reports/ruleqa/player_class_core.json`.

This task proves code and headless behavior. Android touch/IME/render behavior must be reported as
`NOT PERFORMED` whenever no emulator or device is attached; an APK build alone is not runtime
validation.
