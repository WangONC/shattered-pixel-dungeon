package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Layer A-E evidence IDs for every P03 implementation claim. */
public final class P03CompletionMatrix {
	private static final List<ComponentCompletionRow> ROWS = create();
	private P03CompletionMatrix() {}
	public static List<ComponentCompletionRow> rows() { return ROWS; }
	public static ComponentCompletionRow require(String qualifiedKey) {
		for (ComponentCompletionRow row : ROWS) if (row.variantKey().equals(qualifiedKey)) return row;
		throw new IllegalArgumentException("completion row missing for " + qualifiedKey);
	}
	private static List<ComponentCompletionRow> create() {
		List<ComponentCompletionRow> result = new ArrayList<>();
		for (VariantDescriptor descriptor : GameplayVariantCatalog.playerExposed()) {
			result.add(new ComponentCompletionRow(descriptor.qualifiedKey(), ImplementationState.IMPLEMENTED,
					"P03SkillFormSchemaTest#onlyEvidenceCompleteVariantsArePlayerExposedAndHaveCompletionRows",
					"P03PlayerBuilderVerticalSliceTest#blankBuilderSaveLoadFinalizeCompileAndReplayAreDeterministic",
					"P03SkillValidationAndCatalogTest#completeSliceResolvesWhileIncompleteAndUnsupportedRemainDistinct",
					"P03SkillValidationAndCatalogTest#bilingualFormatterAndPriceLedgerDescribeTheExactTypedFields",
					"P03SkillValidationAndCatalogTest#bilingualFormatterAndPriceLedgerDescribeTheExactTypedFields",
					"P03TypedSkillCanonicalRoundTripTest#typedPrimaryAndImmediateSecondaryRoundTripByteCanonically",
					"P03DirectDamageRuntimeBehaviorTest#compiledPrimaryAndImmediateSecondaryDealRealDamageAndTraceEveryStage",
					"P03DirectDamageRuntimeBehaviorTest#noTargetBlockedMissingExecutorAndUnsupportedAreDifferentStatuses"));
		}
		return Collections.unmodifiableList(result);
	}
}
