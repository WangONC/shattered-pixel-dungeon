package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.SkillSpec;

import java.util.ArrayList;
import java.util.List;

public final class SkillValidation {
	private SkillValidation() {}
	public static DependencyReport validate(SkillSpec skill, EffectExecutorRegistry executors) {
		List<DependencyDiagnostic> diagnostics = new ArrayList<>();
		diagnostics.addAll(new SkillStructuralValidator().validate(skill).diagnostics());
		diagnostics.addAll(new SkillCompatibilityValidator().validate(skill).diagnostics());
		diagnostics.addAll(new RuntimeCapabilityValidator(executors).validate(skill).diagnostics());
		return new DependencyReport(diagnostics);
	}
}
