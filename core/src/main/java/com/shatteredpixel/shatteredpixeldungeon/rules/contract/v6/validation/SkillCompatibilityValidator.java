package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;
import java.util.ArrayList;
import java.util.List;

/** Explicit P04 compatibility matrix. Unknown combinations fail closed. */
public final class SkillCompatibilityValidator {
	public DependencyReport validate(SkillSpec skill){
		List<DependencyDiagnostic> out=new ArrayList<>();
		if(!skill.typed())return new DependencyReport(out);
		if(!(skill.activation() instanceof ActiveTriggerSpec)&&!(skill.activation() instanceof UnconfiguredTriggerSpec))unsupported(out,skill,"activation","skill.trigger_not_p04");
		if(skill.condition() instanceof AllOfCondition){
			for(ConditionExpr child:((AllOfCondition)skill.condition()).children())
				if(!(child instanceof AlwaysCondition||child instanceof BuiltinStatCompareCondition||child instanceof ResourceCompareCondition))unsupported(out,skill,"condition","skill.condition_not_p04");
		}else if(!(skill.condition() instanceof UnconfiguredConditionExpr))unsupported(out,skill,"condition","skill.condition_not_p04");
		checkEffect(out,skill,"effects.primary",skill.effects().primary());
		if(skill.effects().secondary()!=null){
			if(!(skill.effects().secondary().activation() instanceof ImmediateOnPrimarySuccess))unsupported(out,skill,"effects.secondary.activation","skill.secondary_activation_not_p04");
			checkEffect(out,skill,"effects.secondary.effect",skill.effects().secondary().effect());
		}
		if(!delivery(skill.delivery()))unsupported(out,skill,"delivery","skill.delivery_not_p04");
		if(!selector(skill.targeting().selector()))unsupported(out,skill,"targeting.selector","skill.selector_not_p04");
		if(!coverage(skill.targeting().coverage()))unsupported(out,skill,"targeting.coverage","skill.coverage_not_p04");
		if(!filter(skill.targeting().filter()))unsupported(out,skill,"targeting.filter","skill.filter_not_p04");
		if(skill.targeting().coverage() instanceof SingleCoverageSpec&&skill.targeting().maximumTargets()!=1)conflict(out,skill,"targeting.maximum_targets","skill.single_requires_one_target");
		if(skill.modifier()!=null&&!modifier(skill.modifier()))unsupported(out,skill,"modifier","skill.modifier_not_p04");
		if(!cost(skill.cost()))unsupported(out,skill,"cost","skill.cost_not_p04");
		if(skill.constraint()!=null&&!(skill.constraint() instanceof UnconfiguredSkillConstraintSpec))unsupported(out,skill,"constraint","skill.constraint_not_p04");
		if(skill.modifier() instanceof PierceModifierSpec&&!(skill.delivery() instanceof ProjectileDeliverySpec||skill.delivery() instanceof TraceDeliverySpec))conflict(out,skill,"modifier","skill.pierce_requires_projectile_or_trace");
		if(skill.modifier() instanceof BounceModifierSpec&&!(skill.delivery() instanceof DirectDeliverySpec||skill.delivery() instanceof ProjectileDeliverySpec))conflict(out,skill,"modifier","skill.bounce_requires_actor_delivery");
		if(skill.modifier() instanceof ExtendDurationModifierSpec&&!durationEffect(skill.effects().primary()))conflict(out,skill,"modifier","skill.extend_requires_duration_effect");
		if(skill.delivery() instanceof SelfDeliverySpec&&!(skill.targeting().selector() instanceof SelfSelector))conflict(out,skill,"targeting.selector","skill.self_delivery_requires_self_selector");
		return new DependencyReport(out);
	}
	private static void checkEffect(List<DependencyDiagnostic> out,SkillSpec skill,String path,EffectSpec effect){
		if(effect instanceof UnconfiguredEffectSpec)return;
		if(effect instanceof DirectDamageEffectSpec&&((DirectDamageEffectSpec)effect).defensePolicy()!=DirectDamageEffectSpec.NativeDefensePolicy.SPD_NATIVE)unsupported(out,skill,path+".defense_policy","skill.ignore_armor_not_p04");
		else if(!(effect instanceof DirectDamageEffectSpec||effect instanceof PercentMaxHpDamageEffectSpec||effect instanceof MissingHpDamageEffectSpec||effect instanceof ExecuteEffectSpec||effect instanceof ApplyStatusEffectSpec||effect instanceof PushEffectSpec||effect instanceof PullEffectSpec||effect instanceof ThrowEffectSpec||effect instanceof DashEffectSpec||effect instanceof TeleportEffectSpec||effect instanceof SwapPositionEffectSpec||effect instanceof HealEffectSpec||effect instanceof BarrierEffectSpec||effect instanceof TemporaryHpEffectSpec||effect instanceof MitigateEffectSpec||effect instanceof RedirectDamageEffectSpec||effect instanceof CleanseEffectSpec||effect instanceof ResourceOperationEffectSpec))unsupported(out,skill,path,"skill.effect_not_p04");
	}
	private static boolean delivery(DeliverySpec x){return x instanceof UnconfiguredDeliverySpec||x instanceof DirectDeliverySpec||x instanceof SelfDeliverySpec||x instanceof ContactDeliverySpec||x instanceof ProjectileDeliverySpec||x instanceof TraceDeliverySpec||x instanceof GroundDeliverySpec;}
	private static boolean selector(SelectorSpec x){return x instanceof UnconfiguredSelectorSpec||x instanceof SelectedActorSelector||x instanceof SelfSelector||x instanceof SelectedCellSelector;}
	private static boolean coverage(CoverageSpec x){return x instanceof UnconfiguredCoverageSpec||x instanceof SingleCoverageSpec||x instanceof AdjacentCoverageSpec||x instanceof RadiusCoverageSpec||x instanceof LineCoverageSpec;}
	private static boolean filter(EntityFilterExpr x){return x instanceof UnconfiguredEntityFilterSpec||x instanceof RelationFilterSpec||x instanceof AnyActorFilterSpec||x instanceof SelfFilterSpec;}
	private static boolean modifier(ModifierSpec x){return x instanceof UnconfiguredModifierSpec||x instanceof RepeatModifierSpec||x instanceof IntensityModifierSpec||x instanceof ExtendDurationModifierSpec||x instanceof PierceModifierSpec||x instanceof BounceModifierSpec;}
	private static boolean cost(CostSpec x){return x instanceof UnconfiguredCostSpec||x instanceof NoCostSpec||x instanceof ResourceCostSpec||x instanceof HpCostSpec||x instanceof ActionTimeCostSpec||x instanceof CooldownCostSpec||x instanceof ItemCostSpec;}
	private static boolean durationEffect(EffectSpec x){return x instanceof ApplyStatusEffectSpec||x instanceof TemporaryHpEffectSpec||x instanceof MitigateEffectSpec||x instanceof RedirectDamageEffectSpec;}
	private static void unsupported(List<DependencyDiagnostic> out,SkillSpec s,String p,String k){out.add(new DependencyDiagnostic(s.id(),p,DependencyState.UNSUPPORTED,s.id(),k));}
	private static void conflict(List<DependencyDiagnostic> out,SkillSpec s,String p,String k){out.add(new DependencyDiagnostic(s.id(),p,DependencyState.HARD_CONFLICT,s.id(),k));}
}
