package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import java.util.*;

/** Explicit P04 completion evidence. Runtime evidence is assigned per concrete behavior. */
final class P04ImplementationEvidence {
	static final class Row {
		final String variantKey;final ImplementationState state=ImplementationState.IMPLEMENTED;
		final String schemaTestId,builderPathTestId,dependencyTestId,formatterTestId,budgetTestId,saveLoadTestId,compilerTestId,executorTestId,runtimeBehaviorTestId,adversarialTestId;
		Row(String key,String behavior){variantKey=req(key);schemaTestId=SCHEMA;builderPathTestId=req(behavior);dependencyTestId=DEPENDENCY;formatterTestId=FORMATTER;budgetTestId=BUDGET;saveLoadTestId=SAVE_LOAD;compilerTestId=COMPILER;executorTestId=behavior;runtimeBehaviorTestId=behavior;adversarialTestId=ADVERSARIAL;}
		List<String> testIds(){return Arrays.asList(schemaTestId,builderPathTestId,dependencyTestId,formatterTestId,budgetTestId,saveLoadTestId,compilerTestId,executorTestId,runtimeBehaviorTestId,adversarialTestId);}
		private static String req(String s){if(s==null||s.isEmpty())throw new IllegalArgumentException("evidence required");return s;}
	}
	private static final String SCHEMA="P04LayerEvidenceTest#schemaExposesVariantSpecificTypedFields";
	private static final String DEPENDENCY="P04LayerEvidenceTest#dependenciesAndGlobalIdentityFailClosed";
	private static final String FORMATTER="P04LayerEvidenceTest#formatterCoversEveryP04EffectWithBilingualParameterText";
	private static final String BUDGET="P04LayerEvidenceTest#everyExposedVariantHasAPriceProducedByItsTypedBuild";
	private static final String SAVE_LOAD="P04LayerEvidenceTest#canonicalRoundTripPreservesP04Nodes";
	private static final String COMPILER="P04LayerEvidenceTest#compilerBuildsIndependentImmutableP04NodesAndAdmissionKeepsFutureClosed";
	private static final String ADVERSARIAL="P04LayerEvidenceTest#deferredFamiliesRemainClosed";

	static Map<String,Row> registry(){Map<String,Row> out=new LinkedHashMap<>();
		r(out,"TRIGGER.ACTIVE","P04ComponentsAndRecipesTest#bloodBerserkerRecipeUsesHpCostLowHpAndMissingHpScaling");
		r(out,"TRIGGER.EVENT","P04ResourceTransactionTest#skillClassOperationAndResourceFlowShareExactConvertSemantics");
		r(out,"CONDITION_EXPR.ALL_OF","P04CoreEffectRuntimeTest#hpCostAndLowHpConditionUseNativeHpAndNeverConsumeShields");
		r(out,"CONDITION.ALWAYS","P04CoreEffectRuntimeTest#directDamageUsesTypedAmount");
		r(out,"CONDITION.BUILTIN_STAT_COMPARE","P04CoreEffectRuntimeTest#hpCostAndLowHpConditionUseNativeHpAndNeverConsumeShields");
		r(out,"CONDITION.RESOURCE_COMPARE","P04ResourceTransactionTest#resourceCompareConditionReadsTheDeclaredRuntimePool");
		r(out,"EFFECT.DIRECT_DAMAGE","P04CoreEffectRuntimeTest#directDamageUsesTypedAmount");
		r(out,"EFFECT.PERCENT_MAX_HP_DAMAGE","P04CoreEffectRuntimeTest#percentMaxHpDamageUsesPercentAndCap");
		r(out,"EFFECT.MISSING_HP_DAMAGE","P04CoreEffectRuntimeTest#missingHpDamageUsesNativeHpGapScaling");
		r(out,"EFFECT.EXECUTE","P04CoreEffectRuntimeTest#executeKillsOnlyAtConfiguredThreshold");
		r(out,"EFFECT.APPLY_STATUS","P04CoreEffectRuntimeTest#applyStatusUsesWhitelistKeyNotJavaClassName");
		r(out,"EFFECT.PUSH","P04CoreEffectRuntimeTest#pushMovesAwayAlongLegalCells");
		r(out,"EFFECT.PULL","P04CoreEffectRuntimeTest#pullMovesTowardOwnerAlongLegalCells");
		r(out,"EFFECT.THROW","P04CoreEffectRuntimeTest#throwUsesFrozenNativeCollisionPath");
		r(out,"EFFECT.DASH","P04CoreEffectRuntimeTest#dashMovesOwnerToExactSelectedCell");
		r(out,"EFFECT.TELEPORT","P04CoreEffectRuntimeTest#teleportMovesOwnerToExactSelectedCell");
		r(out,"EFFECT.SWAP_POSITION","P04CoreEffectRuntimeTest#swapPositionExchangesBothLegalCells");
		r(out,"EFFECT.HEAL","P04CoreEffectRuntimeTest#healChangesOnlyNativeHp");
		r(out,"EFFECT.BARRIER","P04CoreEffectRuntimeTest#barrierUsesIndependentShieldState");
		r(out,"EFFECT.TEMPORARY_HP","P04CoreEffectRuntimeTest#temporaryHpUsesIndependentExpiringState");
		r(out,"EFFECT.MITIGATE","P04CoreEffectRuntimeTest#mitigateInstallsTypedPercentAndDuration");
		r(out,"EFFECT.REDIRECT_DAMAGE","P04CoreEffectRuntimeTest#redirectUsesExplicitRecipientSubject");
		r(out,"EFFECT.CLEANSE","P04CoreEffectRuntimeTest#cleanseRemovesOnlyTheBoundedNegativeSet");
		r(out,"EFFECT.RESOURCE_OPERATION","P04ResourceTransactionTest#skillClassOperationAndResourceFlowShareExactConvertSemantics");
		r(out,"EFFECT_CHAIN.PRIMARY","P04TargetingAndTransactionBoundaryTest#postPaymentPrimaryFailureIsVisibleNotRefundedAndSkipsSecondary");
		r(out,"SECONDARY_ACTIVATION.IMMEDIATE_ON_PRIMARY_SUCCESS","P04TargetingAndTransactionBoundaryTest#chainAndRepeatPayResourceCostOnceAndTraceEveryActivation");
		r(out,"DELIVERY.DIRECT","P04DeliveryModifierCostTest#allSixDeliveryVariantsCompileTypedAndEnforceTheirPreflight");
		r(out,"DELIVERY.SELF","P04DeliveryModifierCostTest#allSixDeliveryVariantsCompileTypedAndEnforceTheirPreflight");
		r(out,"DELIVERY.CONTACT","P04DeliveryModifierCostTest#contactProjectileAndTraceBlockBeforeEffectWhenTheirPathFails");
		r(out,"DELIVERY.PROJECTILE","P04DeliveryModifierCostTest#contactProjectileAndTraceBlockBeforeEffectWhenTheirPathFails");
		r(out,"DELIVERY.TRACE","P04DeliveryModifierCostTest#contactProjectileAndTraceBlockBeforeEffectWhenTheirPathFails");
		r(out,"DELIVERY.GROUND","P04DeliveryModifierCostTest#allSixDeliveryVariantsCompileTypedAndEnforceTheirPreflight");
		r(out,"SELECTOR.SELECTED_ACTOR","P04TargetingAndTransactionBoundaryTest#enemyRelationAcceptsEnemyAndRejectsAllySelfAndNeutral");
		r(out,"SELECTOR.SELF","P04DeliveryModifierCostTest#allSixDeliveryVariantsCompileTypedAndEnforceTheirPreflight");
		r(out,"SELECTOR.SELECTED_CELL","P04DeliveryModifierCostTest#allSixDeliveryVariantsCompileTypedAndEnforceTheirPreflight");
		r(out,"COVERAGE.SINGLE","P04TargetingAndTransactionBoundaryTest#enemyRelationAcceptsEnemyAndRejectsAllySelfAndNeutral");
		r(out,"COVERAGE.ADJACENT","P04DeliveryModifierCostTest#adjacentRadiusAndLineCoverageCarryTheirOwnParametersAndRuntimeTrace");
		r(out,"COVERAGE.RADIUS","P04DeliveryModifierCostTest#adjacentRadiusAndLineCoverageCarryTheirOwnParametersAndRuntimeTrace");
		r(out,"COVERAGE.LINE","P04DeliveryModifierCostTest#adjacentRadiusAndLineCoverageCarryTheirOwnParametersAndRuntimeTrace");
		r(out,"FILTER.RELATION_ENEMY_EXCLUDE_SELF","P04TargetingAndTransactionBoundaryTest#enemyRelationAcceptsEnemyAndRejectsAllySelfAndNeutral");
		r(out,"FILTER.RELATION_ALLY_EXCLUDE_SELF","P04DeliveryModifierCostTest#allyAnyAndSelfFiltersAreDistinctAtRuntime");
		r(out,"FILTER.RELATION_ALLY_INCLUDE_SELF","P04DeliveryModifierCostTest#allyAnyAndSelfFiltersAreDistinctAtRuntime");
		r(out,"FILTER.ANY_ACTOR","P04DeliveryModifierCostTest#allyAnyAndSelfFiltersAreDistinctAtRuntime");
		r(out,"FILTER.SELF","P04DeliveryModifierCostTest#allyAnyAndSelfFiltersAreDistinctAtRuntime");
		r(out,"MODIFIER.REPEAT","P04TargetingAndTransactionBoundaryTest#chainAndRepeatPayResourceCostOnceAndTraceEveryActivation");
		r(out,"MODIFIER.INTENSITY","P04DeliveryModifierCostTest#intensityExtendPierceAndBounceHaveParameterSpecificRuntimeEffects");
		r(out,"MODIFIER.EXTEND_DURATION","P04DeliveryModifierCostTest#intensityExtendPierceAndBounceHaveParameterSpecificRuntimeEffects");
		r(out,"MODIFIER.PIERCE","P04DeliveryModifierCostTest#intensityExtendPierceAndBounceHaveParameterSpecificRuntimeEffects");
		r(out,"MODIFIER.BOUNCE","P04DeliveryModifierCostTest#intensityExtendPierceAndBounceHaveParameterSpecificRuntimeEffects");
		r(out,"COST.NO_COST","P04CoreEffectRuntimeTest#directDamageUsesTypedAmount");
		r(out,"COST.RESOURCE","P04TargetingAndTransactionBoundaryTest#chainAndRepeatPayResourceCostOnceAndTraceEveryActivation");
		r(out,"COST.HP","P04CoreEffectRuntimeTest#hpCostAndLowHpConditionUseNativeHpAndNeverConsumeShields");
		r(out,"COST.ACTION_TIME","P04DeliveryModifierCostTest#actionCooldownAndItemCostsCommitExactlyOnce");
		r(out,"COST.COOLDOWN","P04DeliveryModifierCostTest#actionCooldownAndItemCostsCommitExactlyOnce");
		r(out,"COST.ITEM","P04DeliveryModifierCostTest#actionCooldownAndItemCostsCommitExactlyOnce");
		r(out,"RESOURCE_OPERATION.GAIN","P04ResourceTransactionTest#allSevenResourceOperationVariantsHaveDistinctStateBehavior");
		r(out,"RESOURCE_OPERATION.DRAIN","P04ResourceTransactionTest#allSevenResourceOperationVariantsHaveDistinctStateBehavior");
		r(out,"RESOURCE_OPERATION.SET","P04ResourceTransactionTest#allSevenResourceOperationVariantsHaveDistinctStateBehavior");
		r(out,"RESOURCE_OPERATION.CLEAR","P04ResourceTransactionTest#allSevenResourceOperationVariantsHaveDistinctStateBehavior");
		r(out,"RESOURCE_OPERATION.CONVERT","P04ResourceTransactionTest#rageToFocusClassOperationIsExactAtomicAndTraced");
		r(out,"RESOURCE_OPERATION.RESERVE","P04ResourceTransactionTest#allSevenResourceOperationVariantsHaveDistinctStateBehavior");
		r(out,"RESOURCE_OPERATION.SUPPRESS","P04ResourceTransactionTest#allSevenResourceOperationVariantsHaveDistinctStateBehavior");
		r(out,"CLASS_COMPONENT.BASIC_ATTACK","P04ComponentsAndRecipesTest#basicAttackComponentChangesOnlyCompiledBasicAttackContract");
		r(out,"CLASS_COMPONENT.RESOURCE_FLOW","P04ResourceTransactionTest#skillClassOperationAndResourceFlowShareExactConvertSemantics");
		r(out,"CLASS_COMPONENT.ACTIVE_RESOURCE_OPERATION","P04ComponentsAndRecipesTest#activeResourceOperationComponentCompilesWithItsLinkedOperationAndRuns");
		r(out,"CLASS_OPERATION.RESOURCE_OPERATION","P04ResourceTransactionTest#rageToFocusClassOperationIsExactAtomicAndTraced");
		return Collections.unmodifiableMap(out);}
	private static void r(Map<String,Row> out,String key,String behavior){if(out.put(key,new Row(key,behavior))!=null)throw new IllegalStateException("duplicate evidence "+key);}
	private P04ImplementationEvidence(){}
}
