package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;

/** P02 has no price catalog; P09 supplies a real policy. The default ledger therefore spends zero. */
public interface BuilderBudgetPolicy {
	BuilderBudgetLedger evaluate(ClassBuildSpec draft);

	final class DeclarationOnly implements BuilderBudgetPolicy {
		@Override public BuilderBudgetLedger evaluate(ClassBuildSpec draft) {
			return new BuilderBudgetLedger(draft.budgetMetadata().priceVersion(), 0, draft.budgetMetadata().baseBudget());
		}
	}
}
