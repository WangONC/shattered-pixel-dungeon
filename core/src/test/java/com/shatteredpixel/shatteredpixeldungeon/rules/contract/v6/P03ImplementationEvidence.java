package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit, test-only evidence registry. Production compiler/runtime never sees this data. */
final class P03ImplementationEvidence {
	static final class Row {
		final String variantKey;final ImplementationState state;
		final String schemaTestId,builderPathTestId,dependencyTestId,formatterTestId,budgetTestId,
				saveLoadTestId,compilerTestId,executorTestId,runtimeBehaviorTestId,adversarialTestId;
		Row(String variantKey,String schemaTestId,String builderPathTestId,String dependencyTestId,
				String formatterTestId,String budgetTestId,String saveLoadTestId,String compilerTestId,
				String executorTestId,String runtimeBehaviorTestId,String adversarialTestId){
			this.variantKey=required(variantKey);this.state=ImplementationState.IMPLEMENTED;
			this.schemaTestId=required(schemaTestId);this.builderPathTestId=required(builderPathTestId);
			this.dependencyTestId=required(dependencyTestId);this.formatterTestId=required(formatterTestId);
			this.budgetTestId=required(budgetTestId);this.saveLoadTestId=required(saveLoadTestId);
			this.compilerTestId=required(compilerTestId);this.executorTestId=required(executorTestId);
			this.runtimeBehaviorTestId=required(runtimeBehaviorTestId);this.adversarialTestId=required(adversarialTestId);
		}
		List<String> testIds(){List<String> ids=new ArrayList<>();Collections.addAll(ids,schemaTestId,builderPathTestId,dependencyTestId,formatterTestId,budgetTestId,saveLoadTestId,compilerTestId,executorTestId,runtimeBehaviorTestId,adversarialTestId);return Collections.unmodifiableList(ids);}
		private static String required(String value){if(value==null||value.isEmpty())throw new IllegalArgumentException("evidence field required");return value;}
	}

	private static final String SCHEMA = "P03SkillFormSchemaTest#onlyImplementedVariantsArePlayerExposed";
	private static final String BUILDER = "P03PlayerBuilderVerticalSliceTest#blankBuilderSaveLoadFinalizeCompileAndReplayAreDeterministic";
	private static final String DEPENDENCY = "P03SkillValidationAndCatalogTest#completeSliceResolvesWhileIncompleteAndUnsupportedRemainDistinct";
	private static final String FORMATTER = "P03SkillValidationAndCatalogTest#bilingualFormatterAndPriceLedgerDescribeTheExactTypedFields";
	private static final String BUDGET = "P03SkillValidationAndCatalogTest#bilingualFormatterAndPriceLedgerDescribeTheExactTypedFields";
	private static final String SAVE_LOAD = "P03TypedSkillCanonicalRoundTripTest#typedPrimaryAndImmediateSecondaryRoundTripByteCanonically";
	private static final String COMPILER = "P03R2CompileAdmissionTest#directDamageOnlyBuildIsExecutableAndEmptyDeferredSectionsDoNotBlock";
	private static final String EXECUTOR = "P03R1ExecutorPreflightTest#preflightDistinguishesFailuresWithoutMutation";
	private static final String RUNTIME = "P03DirectDamageRuntimeBehaviorTest#installedBuildExecutesThroughRuleHooksAndPreservesKillCausality";
	private static final String ADVERSARIAL = "P03R1CompilePlanTest#previewPartialAndMissingExecutorPlansCannotExecute";

	static Map<String, Row> registry() {
		Map<String, Row> out = new LinkedHashMap<>();
		register(out, row("TRIGGER.ACTIVE"));
		register(out, row("CONDITION_EXPR.ALL_OF"));
		register(out, row("CONDITION.ALWAYS"));
		register(out, row("EFFECT.DIRECT_DAMAGE"));
		register(out, row("EFFECT_CHAIN.PRIMARY"));
		register(out, row("SECONDARY_ACTIVATION.IMMEDIATE_ON_PRIMARY_SUCCESS"));
		register(out, row("DELIVERY.DIRECT"));
		register(out, row("SELECTOR.SELECTED_ACTOR"));
		register(out, row("COVERAGE.SINGLE"));
		register(out, row("FILTER.RELATION_ENEMY_EXCLUDE_SELF"));
		register(out, row("COST.NO_COST"));
		return Collections.unmodifiableMap(out);
	}
	static java.util.Set<String> requiredVariants(){return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(registry().keySet()));}

	private static Row row(String qualifiedVariantKey) {
		return new Row(qualifiedVariantKey, SCHEMA, BUILDER, DEPENDENCY, FORMATTER, BUDGET,
				SAVE_LOAD, COMPILER, EXECUTOR, RUNTIME, ADVERSARIAL);
	}
	private static void register(Map<String, Row> out, Row row) {
		if (out.put(row.variantKey, row) != null) throw new IllegalStateException("duplicate evidence row " + row.variantKey);
	}
	private P03ImplementationEvidence(){}
}
