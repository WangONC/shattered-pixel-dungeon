package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;

import java.util.ArrayList;
import java.util.List;

/** Explicit P03 compatibility matrix. Unknown combinations never degrade to standard damage. */
public final class SkillCompatibilityValidator {
	public DependencyReport validate(SkillSpec skill) {
		List<DependencyDiagnostic> result = new ArrayList<>();
		if (!skill.typed()) return new DependencyReport(result);
		if (!(skill.activation() instanceof ActiveTriggerSpec) && !(skill.activation() instanceof UnconfiguredTriggerSpec)) unsupported(result, skill, "activation", "skill.trigger_not_p03");
		if (skill.condition() instanceof AllOfCondition) {
			for (ConditionExpr child : ((AllOfCondition) skill.condition()).children()) if (!(child instanceof AlwaysCondition)) unsupported(result, skill, "condition", "skill.condition_not_p03");
		} else if (!(skill.condition() instanceof UnconfiguredConditionExpr)) unsupported(result, skill, "condition", "skill.condition_not_p03");
		checkEffect(result, skill, "effects.primary", skill.effects().primary());
		if (skill.effects().secondary() != null) {
			if (!(skill.effects().secondary().activation() instanceof ImmediateOnPrimarySuccess)) unsupported(result, skill, "effects.secondary.activation", "skill.secondary_activation_not_p03");
			checkEffect(result, skill, "effects.secondary.effect", skill.effects().secondary().effect());
		}
		if (!(skill.delivery() instanceof DirectDeliverySpec) && !(skill.delivery() instanceof UnconfiguredDeliverySpec)) unsupported(result, skill, "delivery", "skill.delivery_not_p03");
		if (!(skill.targeting().selector() instanceof SelectedActorSelector) && !(skill.targeting().selector() instanceof UnconfiguredSelectorSpec)) unsupported(result, skill, "targeting.selector", "skill.selector_not_p03");
		if (!(skill.targeting().coverage() instanceof SingleCoverageSpec) && !(skill.targeting().coverage() instanceof UnconfiguredCoverageSpec)) unsupported(result, skill, "targeting.coverage", "skill.coverage_not_p03");
		if (skill.targeting().filter() instanceof RelationFilterSpec) {
			RelationFilterSpec filter = (RelationFilterSpec) skill.targeting().filter();
			if (filter.relationToClassOwner() != RelationFilterSpec.RelationAlignment.ENEMY || filter.includeSelf()) unsupported(result, skill, "targeting.filter", "skill.filter_not_p03");
		} else if (!(skill.targeting().filter() instanceof UnconfiguredEntityFilterSpec)) unsupported(result, skill, "targeting.filter", "skill.filter_not_p03");
		if (skill.targeting().coverage() instanceof SingleCoverageSpec && skill.targeting().maximumTargets() != 1) conflict(result, skill, "targeting.maximum_targets", "skill.single_requires_one_target");
		if (skill.modifier() != null && !(skill.modifier() instanceof UnconfiguredModifierSpec)) unsupported(result, skill, "modifier", "skill.modifier_not_p03");
		if (!(skill.cost() instanceof NoCostSpec) && !(skill.cost() instanceof UnconfiguredCostSpec)) unsupported(result, skill, "cost", "skill.cost_not_p03");
		if (skill.constraint() != null && !(skill.constraint() instanceof UnconfiguredSkillConstraintSpec)) unsupported(result, skill, "constraint", "skill.constraint_not_p03");
		return new DependencyReport(result);
	}
	private static void checkEffect(List<DependencyDiagnostic> out, SkillSpec skill, String path, EffectSpec effect) {
		if (effect instanceof UnconfiguredEffectSpec) return;
		if (!(effect instanceof DirectDamageEffectSpec)) { unsupported(out, skill, path, "skill.effect_not_p03"); return; }
		DirectDamageEffectSpec damage = (DirectDamageEffectSpec) effect;
		if (!(damage.amount() instanceof FixedValueSpec)) unsupported(out, skill, path + ".amount", "skill.value_not_p03");
		if (damage.damageType() != DirectDamageEffectSpec.DamageType.UNTYPED) unsupported(out, skill, path + ".damage_type", "skill.damage_type_not_p03");
		if (damage.defensePolicy() != DirectDamageEffectSpec.NativeDefensePolicy.SPD_NATIVE) unsupported(out, skill, path + ".defense_policy", "skill.defense_policy_not_p03");
	}
	private static void unsupported(List<DependencyDiagnostic> out, SkillSpec skill, String path, String key) {
		out.add(new DependencyDiagnostic(skill.id(), path, DependencyState.UNSUPPORTED, skill.id(), key));
	}
	private static void conflict(List<DependencyDiagnostic> out, SkillSpec skill, String path, String key) {
		out.add(new DependencyDiagnostic(skill.id(), path, DependencyState.HARD_CONFLICT, skill.id(), key));
	}
}
