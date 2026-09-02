# PLAYER_FACING_LEGACY_AUDIT

Scope: production call paths reachable from `HeroSelectScene` → `WndCreateClass`, the
in-game class overview, and the rule ability item.

## Legacy exposure found before the cutover

- `WndCreateClass` built one flat primary/secondary menu by merging
  `RuleEffect.Type.values()` with `EffectSpec.Operation.values()`.
- Target editing offered bundled presets instead of the orthogonal
  `TargetingSpec` selector, area, and eligibility dimensions.
- Delivery, modifier, cost, constraint, resource, law, trait, and restriction
  choices were assembled locally in the window, so runtime support and player
  exposure could drift apart.
- `ClassBuildFormatter` still contained an older fallback through
  `RuleSemanticFormatter` for legacy effects.
- Buttons inside `ScrollPane` blocked the pointer-down event, so an Android drag
  beginning on an option row could not become a pane drag.
- New saves still projected `ClassBuild` back into old fixed-slot scalar fields.

## Removed player paths

- Flat `RuleEffect` primary and secondary menus.
- Bundled legacy target presets.
- Local enum-driven delivery, target, modifier, cost, constraint, resource,
  law/trait, and restriction lists.
- Fixed active/reactive/resource/law/two-vocabulary/restriction workflow.
- The old `ClassBuildFormatter` implementation. The class name remains only as
  a translation-key namespace; it contains no formatting or gameplay logic.
- Projection of newly authored builds into the old save schema.

The production Builder now reads only the formal registries and writes only
`ClassBuild`/`SkillSpec`/`EffectSpec` data.

## MIGRATION_ONLY compatibility

- `ClassBuildMigrator.fromLegacy(CustomClassConfig)` converts a pre-ClassBuild
  fixed-slot save once.
- `CustomClassConfig.restoreFromBundle` reads scalar fields only when the saved
  bundle has no `class_build`, then immediately creates an authoritative
  `ClassBuild`.
- `EffectSpec.fromLegacy` maps saved `RuleEffect` verbs to formal operations.
  `LEGACY` remains only as a lossless internal adapter for verbs that cannot be
  upgraded during old-save loading.
- `RuleEffect`, `RuleTarget`, `RuleModifier`, and `RuleDefinition` remain internal
  runtime adapters and old-save vocabulary. They are not Builder registries.

## Cutover verification

- Production UI references to `RuleEffect.values()`: **0**.
- Production UI references to `RuleTarget.values()`: **0**.
- Production UI references to `RuleModifier.values()`: **0**.
- Supported effect operations exposed by Builder: checked by
  `builderVocabularyExposureQa`.
- Builder options unsupported by runtime: checked by the inverse audit in the
  same task.

Expected final state: **PLAYER-FACING LEGACY = 0**. Migration-only compatibility
is intentionally retained so an existing run is not silently corrupted.
