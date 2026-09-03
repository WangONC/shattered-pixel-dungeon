package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable aggregate consumed by both the window adapter and headless player session. */
public final class BuilderState {
	private final ClassBuildSpec draft;
	private final DependencyReport dependencies;
	private final BuilderValidationReport validation;
	private final BuilderBudgetLedger budget;
	private final BuilderNavigationState navigation;
	private final UndoRedoState history;
	private final Map<String, String> savedDrafts;
	private final List<String> commandDiagnostics;
	private final FinalizationReport finalization;

	BuilderState(ClassBuildSpec draft, DependencyReport dependencies, BuilderValidationReport validation,
			BuilderBudgetLedger budget, BuilderNavigationState navigation, UndoRedoState history,
			Map<String, String> savedDrafts, List<String> commandDiagnostics, FinalizationReport finalization) {
		this.draft = draft; this.dependencies = dependencies; this.validation = validation; this.budget = budget;
		this.navigation = navigation; this.history = history;
		this.savedDrafts = Collections.unmodifiableMap(new LinkedHashMap<>(savedDrafts));
		this.commandDiagnostics = Collections.unmodifiableList(new ArrayList<>(commandDiagnostics));
		this.finalization = finalization;
	}
	public ClassBuildSpec draft() { return draft; }
	public DependencyReport dependencies() { return dependencies; }
	public BuilderValidationReport validation() { return validation; }
	public BuilderBudgetLedger budget() { return budget; }
	public BuilderNavigationState navigation() { return navigation; }
	public UndoRedoState history() { return history; }
	public Map<String, String> savedDrafts() { return savedDrafts; }
	public List<String> commandDiagnostics() { return commandDiagnostics; }
	public FinalizationReport finalization() { return finalization; }
}
