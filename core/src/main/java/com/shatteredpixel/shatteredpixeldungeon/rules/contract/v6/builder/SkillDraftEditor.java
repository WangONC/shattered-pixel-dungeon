package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.GameplayVariantCatalog;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.TypedRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;

import java.util.ArrayList;
import java.util.List;

/** Exact typed edit functions for SkillSpec v0.2. There is no reflective or generic payload mutation. */
final class SkillDraftEditor {
	static final String PRIMARY = "PRIMARY";
	static final String IMMEDIATE_SECONDARY = "IMMEDIATE_SECONDARY";

	ClassBuildSpec selectTrigger(ClassBuildSpec build,String skillId,String variant) {
		exposed("TRIGGER",variant);if(!ActiveTriggerSpec.VARIANT.equals(variant))throw unsupported("trigger",variant);
		SkillSpec skill=require(build,skillId);return replace(build,skill.withActivation(new ActiveTriggerSpec(skill.activation().nodeId())));
	}
	ClassBuildSpec selectCondition(ClassBuildSpec build,String skillId,String variant) {
		exposed("CONDITION",variant);if(!AlwaysCondition.VARIANT.equals(variant))throw unsupported("condition",variant);
		return replace(build,require(build,skillId).withCondition(AllOfCondition.always()));
	}
	ClassBuildSpec selectEffectFamily(ClassBuildSpec build,String skillId,String slot,String family,StableId newEffectId) {
		if(IMMEDIATE_SECONDARY.equals(slot)&&"NONE".equals(family)){SkillSpec skill=require(build,skillId);return replace(build,skill.withEffects(skill.effects().withSecondary(null)));}
		if(!EffectFamily.DAMAGE.name().equals(family))throw unsupported("effect family",family);
		SkillSpec skill=require(build,skillId);EffectFamily selected=EffectFamily.valueOf(family);
		if(PRIMARY.equals(slot))return replace(build,skill.withEffects(skill.effects().withPrimary(new UnconfiguredEffectSpec(skill.effects().primary().effectId(),selected))));
		if(IMMEDIATE_SECONDARY.equals(slot)){
			StableId id=skill.effects().secondary()==null?newEffectId:skill.effects().secondary().effect().effectId();
			if(id==null)throw new IllegalArgumentException("secondary effect id is required");
			return replace(build,skill.withEffects(skill.effects().withSecondary(new SecondaryEffectSpec(new UnconfiguredEffectSpec(id,selected),new ImmediateOnPrimarySuccess()))));
		}
		throw new IllegalArgumentException("unknown effect slot "+slot);
	}
	ClassBuildSpec selectEffectVariant(ClassBuildSpec build,String skillId,String slot,String variant) {
		exposed("EFFECT",variant);if(!EffectVariantKey.DIRECT_DAMAGE.name().equals(variant))throw unsupported("effect",variant);
		SkillSpec skill=require(build,skillId);EffectSpec old=effect(skill,slot);
		if(!(old instanceof UnconfiguredEffectSpec)||old.family()!=EffectFamily.DAMAGE)throw new IllegalArgumentException("select DAMAGE family before DIRECT_DAMAGE");
		DirectDamageEffectSpec next=new DirectDamageEffectSpec(old.effectId(),new FixedValueSpec(1),DirectDamageEffectSpec.DamageType.UNTYPED,DirectDamageEffectSpec.NativeDefensePolicy.SPD_NATIVE);
		return replace(build,skill.withEffects(withEffect(skill.effects(),slot,next)));
	}
	ClassBuildSpec setTargetingSelector(ClassBuildSpec build,String skillId,String variant) {
		exposed("SELECTOR",variant);if(!SelectedActorSelector.VARIANT.equals(variant))throw unsupported("selector",variant);
		SkillSpec skill=require(build,skillId);return replace(build,skill.withTargeting(skill.targeting().withSelector(new SelectedActorSelector())));
	}
	ClassBuildSpec setTargetingCoverage(ClassBuildSpec build,String skillId,String variant) {
		exposed("COVERAGE",variant);if(!SingleCoverageSpec.VARIANT.equals(variant))throw unsupported("coverage",variant);
		SkillSpec skill=require(build,skillId);return replace(build,skill.withTargeting(skill.targeting().withCoverage(new SingleCoverageSpec()).withMaximumTargets(1)));
	}
	ClassBuildSpec setTargetingFilter(ClassBuildSpec build,String skillId,String variant) {
		exposed("FILTER",variant);if(!"RELATION_ENEMY_EXCLUDE_SELF".equals(variant))throw unsupported("filter",variant);
		SkillSpec skill=require(build,skillId);return replace(build,skill.withTargeting(skill.targeting().withFilter(new RelationFilterSpec(RelationFilterSpec.RelationAlignment.ENEMY,false))));
	}
	ClassBuildSpec setDelivery(ClassBuildSpec build,String skillId,String variant) {
		exposed("DELIVERY",variant);if(!DirectDeliverySpec.VARIANT.equals(variant))throw unsupported("delivery",variant);
		SkillSpec skill=require(build,skillId);return replace(build,skill.withDelivery(new DirectDeliverySpec(skill.delivery().nodeId(),false)));
	}
	ClassBuildSpec setModifier(ClassBuildSpec build,String skillId,String variant) {
		if(!"NONE".equals(variant))throw unsupported("modifier",variant);return replace(build,require(build,skillId).withModifier(null));
	}
	ClassBuildSpec setCost(ClassBuildSpec build,String skillId,String variant) {
		exposed("COST",variant);if(!NoCostSpec.VARIANT.equals(variant))throw unsupported("cost",variant);
		SkillSpec skill=require(build,skillId);return replace(build,skill.withCost(new NoCostSpec(skill.cost().nodeId())));
	}
	ClassBuildSpec setConstraint(ClassBuildSpec build,String skillId,String variant) {
		if(!"NONE".equals(variant))throw unsupported("skill constraint",variant);return replace(build,require(build,skillId).withConstraint(null));
	}
	ClassBuildSpec setField(ClassBuildSpec build,String skillId,String ownerPath,String variant,String field,String raw) {
		SkillSpec skill=require(build,skillId);
		if("effects.primary".equals(ownerPath)||"effects.secondary".equals(ownerPath)){
			if(!EffectVariantKey.DIRECT_DAMAGE.name().equals(variant))throw new IllegalArgumentException("effect variant mismatch");
			String slot="effects.primary".equals(ownerPath)?PRIMARY:IMMEDIATE_SECONDARY;EffectSpec rawEffect=effect(skill,slot);
			if(!(rawEffect instanceof DirectDamageEffectSpec))throw new IllegalArgumentException("DIRECT_DAMAGE effect required");
			DirectDamageEffectSpec damage=(DirectDamageEffectSpec)rawEffect;
			if("amount".equals(field))damage=damage.withAmount(new FixedValueSpec(integer(raw)));
			else if("damage_type".equals(field)){DirectDamageEffectSpec.DamageType type=en(DirectDamageEffectSpec.DamageType.class,raw);if(type!=DirectDamageEffectSpec.DamageType.UNTYPED)throw unsupported("damage type",raw);damage=damage.withDamageType(type);}
			else if("defense_policy".equals(field)){DirectDamageEffectSpec.NativeDefensePolicy policy=en(DirectDamageEffectSpec.NativeDefensePolicy.class,raw);if(policy!=DirectDamageEffectSpec.NativeDefensePolicy.SPD_NATIVE)throw unsupported("defense policy",raw);damage=damage.withDefensePolicy(policy);}
			else throw new IllegalArgumentException("unknown DirectDamage field "+field);
			return replace(build,skill.withEffects(withEffect(skill.effects(),slot,damage)));
		}
		if("delivery".equals(ownerPath)){
			if(!DirectDeliverySpec.VARIANT.equals(variant)||!(skill.delivery() instanceof DirectDeliverySpec)||!"requires_line_of_sight".equals(field))throw new IllegalArgumentException("unknown DirectDelivery field");
			return replace(build,skill.withDelivery(((DirectDeliverySpec)skill.delivery()).withRequiresLineOfSight(bool(raw))));
		}
		if("targeting".equals(ownerPath)){
			if(!"TARGETING".equals(variant))throw new IllegalArgumentException("targeting variant mismatch");
			TargetingSpec targeting=skill.targeting();if("range".equals(field))targeting=targeting.withRange(integer(raw));
			else if("maximum_targets".equals(field)){int count=integer(raw);if(count!=1)throw new IllegalArgumentException("SINGLE coverage requires maximum_targets=1");targeting=targeting.withMaximumTargets(count);}
			else throw new IllegalArgumentException("unknown Targeting field "+field);
			return replace(build,skill.withTargeting(targeting));
		}
		throw new IllegalArgumentException("unknown typed owner path "+ownerPath);
	}
	ClassBuildSpec setReference(ClassBuildSpec build,String skillId,String ownerPath,String variant,String field,TypedRef reference) {
		require(build,skillId);throw new IllegalArgumentException("no P03 player-exposed skill reference field: "+ownerPath+"."+field);
	}
	private static EffectSpec effect(SkillSpec skill,String slot){if(PRIMARY.equals(slot))return skill.effects().primary();if(IMMEDIATE_SECONDARY.equals(slot)&&skill.effects().secondary()!=null)return skill.effects().secondary().effect();throw new IllegalArgumentException("effect slot is not configured: "+slot);}
	private static EffectChainSpec withEffect(EffectChainSpec chain,String slot,EffectSpec effect){if(PRIMARY.equals(slot))return chain.withPrimary(effect);if(IMMEDIATE_SECONDARY.equals(slot)&&chain.secondary()!=null)return chain.withSecondary(new SecondaryEffectSpec(effect,chain.secondary().activation()));throw new IllegalArgumentException("effect slot is not configured: "+slot);}
	private static SkillSpec require(ClassBuildSpec build,String id){StableId wanted=StableId.fromStored(id);for(SkillSpec skill:build.skills())if(skill.id().equals(wanted)){if(!skill.typed())throw new IllegalArgumentException("legacy skill envelope is not editable as SkillSpec v0.2");return skill;}throw new IllegalArgumentException("skill not found: "+id);}
	private static ClassBuildSpec replace(ClassBuildSpec build,SkillSpec replacement){List<SkillSpec> values=new ArrayList<>();boolean found=false;for(SkillSpec skill:build.skills()){if(skill.id().equals(replacement.id())){values.add(replacement);found=true;}else values.add(skill);}if(!found)throw new IllegalArgumentException("skill not found");return build.toBuilder().skills(values).build();}
	private static void exposed(String family,String variant){if(!GameplayVariantCatalog.require(family,variant).playerExposed())throw unsupported(family,variant);}
	private static IllegalArgumentException unsupported(String kind,String variant){return new IllegalArgumentException("unsupported P03 "+kind+" variant "+variant);}
	private static int integer(String value){try{return Integer.parseInt(value);}catch(NumberFormatException error){throw new IllegalArgumentException("integer required: "+value,error);}}
	private static boolean bool(String value){if("true".equals(value))return true;if("false".equals(value))return false;throw new IllegalArgumentException("boolean required: "+value);}
	private static<E extends Enum<E>>E en(Class<E> type,String value){try{return Enum.valueOf(type,value);}catch(IllegalArgumentException error){throw new IllegalArgumentException("unknown "+type.getSimpleName()+" "+value,error);}}
}
