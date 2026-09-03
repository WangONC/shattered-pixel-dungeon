package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.UnconfiguredEffectSpec;

import java.util.ArrayList;
import java.util.List;

/** Checks executable capability without changing the Spec or consulting FormSchema. */
public final class RuntimeCapabilityValidator {
	private final EffectExecutorRegistry executors;
	public RuntimeCapabilityValidator(EffectExecutorRegistry executors) {
		if (executors == null) throw new IllegalArgumentException("executor registry is required");
		this.executors = executors;
	}
	public DependencyReport validate(SkillSpec skill) {
		List<DependencyDiagnostic> result = new ArrayList<>();
		if (!skill.typed()) return new DependencyReport(result);
		check(result, skill, "effects.primary", skill.effects().primary());
		if (skill.effects().secondary() != null) check(result, skill, "effects.secondary.effect", skill.effects().secondary().effect());
		return new DependencyReport(result);
	}
	private void check(List<DependencyDiagnostic> out, SkillSpec skill, String path, EffectSpec effect) {
		CompiledSkill.EffectVariant compiledVariant = effect.variantKey() == com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.EffectVariantKey.DIRECT_DAMAGE
				? CompiledSkill.EffectVariant.DIRECT_DAMAGE : null;
		if (!(effect instanceof UnconfiguredEffectSpec) && (compiledVariant == null || !executors.has(compiledVariant))) {
			out.add(new DependencyDiagnostic(skill.id(), path, DependencyState.UNSUPPORTED, effect.effectId(), "skill.executor_missing"));
		}
	}
}
