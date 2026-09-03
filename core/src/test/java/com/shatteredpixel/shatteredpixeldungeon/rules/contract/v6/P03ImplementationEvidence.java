package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import java.util.ArrayList;import java.util.Collections;import java.util.List;

/** Test-only QA evidence model; production compiler/runtime never sees this data. */
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
	static List<Row> rows(Iterable<String> variantKeys){List<Row> out=new ArrayList<>();for(String key:variantKeys)out.add(new Row(key,
			"P03SkillFormSchemaTest#onlyImplementedVariantsArePlayerExposed",
			"P03PlayerBuilderVerticalSliceTest#blankBuilderSaveLoadFinalizeCompileAndReplayAreDeterministic",
			"P03SkillValidationAndCatalogTest#completeSliceResolvesWhileIncompleteAndUnsupportedRemainDistinct",
			"P03SkillValidationAndCatalogTest#bilingualFormatterAndPriceLedgerDescribeTheExactTypedFields",
			"P03SkillValidationAndCatalogTest#bilingualFormatterAndPriceLedgerDescribeTheExactTypedFields",
			"P03TypedSkillCanonicalRoundTripTest#typedPrimaryAndImmediateSecondaryRoundTripByteCanonically",
			"P03R1CompilePlanTest#compiledSkillIsIndependentImmutableGraph",
			"P03R1ExecutorPreflightTest#preflightDistinguishesFailuresWithoutMutation",
			"P03DirectDamageRuntimeBehaviorTest#installedBuildExecutesThroughRuleHooksAndPreservesKillCausality",
			"P03R1CompilePlanTest#previewPartialAndMissingExecutorPlansCannotExecute"));return Collections.unmodifiableList(out);}
	private P03ImplementationEvidence(){}
}
