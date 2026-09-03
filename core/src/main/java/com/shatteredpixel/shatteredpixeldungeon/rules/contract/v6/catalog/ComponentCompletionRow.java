package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;

public final class ComponentCompletionRow {
	private final String variantKey;
	private final ImplementationState state;
	private final String schemaTestId, builderPathTestId, dependencyTestId, formatterTestId,
			budgetTestId, saveLoadTestId, runtimeBehaviorTestId, adversarialTestId;
	public ComponentCompletionRow(String variantKey, ImplementationState state, String schemaTestId,
			String builderPathTestId, String dependencyTestId, String formatterTestId,
			String budgetTestId, String saveLoadTestId, String runtimeBehaviorTestId,
			String adversarialTestId) {
		this.variantKey = required(variantKey); this.state = state;
		this.schemaTestId = required(schemaTestId); this.builderPathTestId = required(builderPathTestId);
		this.dependencyTestId = required(dependencyTestId); this.formatterTestId = required(formatterTestId);
		this.budgetTestId = required(budgetTestId); this.saveLoadTestId = required(saveLoadTestId);
		this.runtimeBehaviorTestId = required(runtimeBehaviorTestId); this.adversarialTestId = required(adversarialTestId);
		if (state != ImplementationState.IMPLEMENTED) throw new IllegalArgumentException("completion rows are only for implemented variants");
	}
	private static String required(String value) { if (value == null || value.isEmpty()) throw new IllegalArgumentException("evidence id is required"); return value; }
	public String variantKey() { return variantKey; }
	public ImplementationState state() { return state; }
	public String schemaTestId() { return schemaTestId; }
	public String builderPathTestId() { return builderPathTestId; }
	public String dependencyTestId() { return dependencyTestId; }
	public String formatterTestId() { return formatterTestId; }
	public String budgetTestId() { return budgetTestId; }
	public String saveLoadTestId() { return saveLoadTestId; }
	public String runtimeBehaviorTestId() { return runtimeBehaviorTestId; }
	public String adversarialTestId() { return adversarialTestId; }
}
