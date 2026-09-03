package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.GameplayVariantCatalog;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.P03CompletionMatrix;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.VariantDescriptor;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation.SkillValidation;

import java.util.ArrayList;
import java.util.List;

/** Fail-closed build-to-runtime compiler. It never repairs a draft. */
public final class SkillCompiler {
	private final EffectExecutorRegistry executors;
	public SkillCompiler(EffectExecutorRegistry executors) {
		if (executors == null) throw new IllegalArgumentException("executor registry is required");
		this.executors = executors;
	}
	public ClassCompilePlan compile(ClassBuildSpec build) {
		if (build == null) throw new IllegalArgumentException("build is required");
		List<CompiledSkill> compiled = new ArrayList<>();
		List<CompileDiagnostic> diagnostics = new ArrayList<>();
		for (SkillSpec skill : build.skills()) {
			DependencyReport report = SkillValidation.validate(skill, executors);
			for (DependencyDiagnostic diagnostic : report.diagnostics()) diagnostics.add(new CompileDiagnostic(
					skill.id(), diagnostic.fieldPath(), diagnostic.state(), diagnostic.messageKey()));
			if (!report.finalizationAllowed()) continue;
			if (skill.implementationState() != ImplementationState.IMPLEMENTED) {
				diagnostics.add(new CompileDiagnostic(skill.id(), "implementation_state",
						com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState.UNSUPPORTED,
						"compile.skill_not_implemented"));
				continue;
			}
			for (VariantDescriptor descriptor : GameplayVariantCatalog.playerExposed()) P03CompletionMatrix.require(descriptor.qualifiedKey());
			compiled.add(new CompiledSkill(skill));
		}
		if (build.skills().isEmpty()) diagnostics.add(new CompileDiagnostic(build.buildId(), "skills",
				com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState.UNRESOLVED,
				"compile.no_skill"));
		return new ClassCompilePlan(compiled, diagnostics);
	}
}
