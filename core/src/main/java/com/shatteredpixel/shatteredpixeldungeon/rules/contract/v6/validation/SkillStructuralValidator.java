package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure structural validation; it never normalizes or completes an editable SkillSpec. */
public final class SkillStructuralValidator {
	public DependencyReport validate(SkillSpec skill) {
		List<DependencyDiagnostic> result = new ArrayList<>();
		if (!skill.typed()) {
			result.add(d(skill, "skill", DependencyState.UNSUPPORTED, "skill.legacy_envelope"));
			return new DependencyReport(result);
		}
		if (skill.activation() instanceof UnconfiguredTriggerSpec) result.add(d(skill, "activation", DependencyState.UNRESOLVED, "skill.trigger_required"));
		if (skill.condition() instanceof UnconfiguredConditionExpr) result.add(d(skill, "condition", DependencyState.UNRESOLVED, "skill.condition_required"));
		else if (!(skill.condition() instanceof AllOfCondition)) result.add(d(skill, "condition", DependencyState.UNSUPPORTED, "skill.condition_expr_unsupported"));
		else if (((AllOfCondition) skill.condition()).children().size() > 8) result.add(d(skill, "condition.children", DependencyState.HARD_CONFLICT, "skill.condition_too_deep"));
		EffectSpec primary = skill.effects().primary();
		if (primary instanceof UnconfiguredEffectSpec) result.add(d(skill, "effects.primary", DependencyState.UNRESOLVED,
				((UnconfiguredEffectSpec) primary).selectedFamily() == null ? "skill.effect_family_required" : "skill.effect_variant_required"));
		if (skill.effects().secondary() != null && skill.effects().secondary().effect() instanceof UnconfiguredEffectSpec) {
			result.add(d(skill, "effects.secondary.effect", DependencyState.UNRESOLVED, "skill.secondary_effect_variant_required"));
		}
		if (skill.delivery() instanceof UnconfiguredDeliverySpec) result.add(d(skill, "delivery", DependencyState.UNRESOLVED, "skill.delivery_required"));
		if (skill.targeting().selector() instanceof UnconfiguredSelectorSpec) result.add(d(skill, "targeting.selector", DependencyState.UNRESOLVED, "skill.selector_required"));
		if (skill.targeting().coverage() instanceof UnconfiguredCoverageSpec) result.add(d(skill, "targeting.coverage", DependencyState.UNRESOLVED, "skill.coverage_required"));
		if (skill.targeting().filter() instanceof UnconfiguredEntityFilterSpec) result.add(d(skill, "targeting.filter", DependencyState.UNRESOLVED, "skill.filter_required"));
		if (skill.modifier() instanceof UnconfiguredModifierSpec) result.add(d(skill, "modifier", DependencyState.UNRESOLVED, "skill.modifier_choice_required"));
		if (skill.cost() instanceof UnconfiguredCostSpec) result.add(d(skill, "cost", DependencyState.UNRESOLVED, "skill.cost_required"));
		if (skill.constraint() instanceof UnconfiguredSkillConstraintSpec) result.add(d(skill, "constraint", DependencyState.UNRESOLVED, "skill.constraint_choice_required"));
		Set<StableId> ids = new HashSet<>();
		unique(result, skill, ids, "id", skill.id());
		unique(result, skill, ids, "activation.node_id", skill.activation().nodeId());
		unique(result, skill, ids, "effects.chain_id", skill.effects().chainId());
		unique(result, skill, ids, "effects.primary.effect_id", primary.effectId());
		if(primary instanceof ResourceOperationEffectSpec)unique(result,skill,ids,"effects.primary.operation_id",((ResourceOperationEffectSpec)primary).operation().operationId());
		if (skill.effects().secondary() != null) unique(result, skill, ids, "effects.secondary.effect_id", skill.effects().secondary().effect().effectId());
		if(skill.effects().secondary()!=null&&skill.effects().secondary().effect() instanceof ResourceOperationEffectSpec)unique(result,skill,ids,"effects.secondary.operation_id",((ResourceOperationEffectSpec)skill.effects().secondary().effect()).operation().operationId());
		unique(result, skill, ids, "delivery.node_id", skill.delivery().nodeId());
		unique(result, skill, ids, "targeting.node_id", skill.targeting().nodeId());
		if (skill.modifier() != null) unique(result, skill, ids, "modifier.node_id", skill.modifier().nodeId());
		unique(result, skill, ids, "cost.node_id", skill.cost().nodeId());
		if (skill.constraint() != null) unique(result, skill, ids, "constraint.node_id", skill.constraint().constraintId());
		collectConditionIds(result,skill,ids,skill.condition(),"condition");
		return new DependencyReport(result);
	}
	private static void collectConditionIds(List<DependencyDiagnostic> out,SkillSpec skill,Set<StableId> seen,ConditionExpr value,String path){if(value instanceof AllOfCondition){int index=0;for(ConditionExpr child:((AllOfCondition)value).children())collectConditionIds(out,skill,seen,child,path+"["+(index++)+"]");}else if(value instanceof BuiltinStatCompareCondition)unique(out,skill,seen,path+".node_id",((BuiltinStatCompareCondition)value).nodeId());else if(value instanceof ResourceCompareCondition)unique(out,skill,seen,path+".node_id",((ResourceCompareCondition)value).nodeId());}
	private static void unique(List<DependencyDiagnostic> out, SkillSpec skill, Set<StableId> seen, String path, StableId id) {
		if (!seen.add(id)) out.add(new DependencyDiagnostic(skill.id(), path, DependencyState.HARD_CONFLICT, id, "skill.duplicate_nested_id"));
	}
	private static DependencyDiagnostic d(SkillSpec skill, String path, DependencyState state, String key) {
		return new DependencyDiagnostic(skill.id(), path, state, skill.id(), key);
	}
}
