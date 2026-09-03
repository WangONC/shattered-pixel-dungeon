package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;

import java.util.List;

public final class BuilderValidationReport {
	private final DependencyReport report;
	BuilderValidationReport(DependencyReport report) { this.report = report; }
	public List<DependencyDiagnostic> diagnostics() { return report.diagnostics(); }
	public DependencyReport asDependencyReport() { return report; }
}
