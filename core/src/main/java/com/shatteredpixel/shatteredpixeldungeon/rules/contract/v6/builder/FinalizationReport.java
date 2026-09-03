package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FinalizationReport {
	private final ClassBuildSpec build;
	private final List<String> diagnostics;
	FinalizationReport(ClassBuildSpec build, List<String> diagnostics) {
		this.build = build; this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
	}
	public boolean allowed() { return build != null && diagnostics.isEmpty(); }
	public ClassBuildSpec build() { return build; }
	public List<String> diagnostics() { return diagnostics; }
}
