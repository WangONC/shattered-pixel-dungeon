package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Saved family/variant payload; LEGACY is the lossless V0.2 RuleEffect adapter. */
public class EffectSpec implements Bundlable {
	public enum DamageType { UNTYPED, FIRE, POISON, BLEEDING }
	public enum ScalingSource { FIXED, HERO_LEVEL, CURRENT_RESOURCE }
	public enum StatusStacking { NATIVE, EXTEND, REPLACE }
	public enum Operation {
		LEGACY,
		DAMAGE_STANDARD, DAMAGE_PERCENT, DAMAGE_MISSING_HP, DAMAGE_EXECUTE,
		STATUS_POISON, STATUS_BURNING, STATUS_BLEEDING, STATUS_SLOW, STATUS_HASTE,
		STATUS_PARALYSIS, STATUS_ROOTS, STATUS_AMOK, STATUS_TERROR, STATUS_VULNERABLE,
		MOVE_PUSH, MOVE_PULL, MOVE_THROW, MOVE_DASH, MOVE_TELEPORT, MOVE_SWAP,
		RECOVER_HEAL, DEFENSE_BARRIER, DEFENSE_TEMP_HP, DEFENSE_MITIGATE,
		DEFENSE_REDIRECT, DEFENSE_CLEANSE,
		RESOURCE_GAIN, RESOURCE_DRAIN, RESOURCE_CONVERT, RESOURCE_RESERVE, RESOURCE_SUPPRESS,
		MARK_APPLY, MARK_STACK, MARK_COUNTER, MARK_CONSUME, MARK_SPREAD,
		CREATE_ACTOR, CREATE_DEVICE, CREATE_TRAP, CREATE_FIELD,
		WORLD_WATER, WORLD_GRASS, WORLD_DESTROY, WORLD_TOXIC_GAS, WORLD_FIRE, WORLD_CLEAR_HAZARD,
		RELATION_OWNERSHIP, RELATION_LINK, RELATION_COMMAND_FOLLOW, RELATION_COMMAND_ATTACK,
		RELATION_COMMAND_GUARD, RELATION_INHERIT, RELATION_BREAK,
		TRANSFER_RESOURCE, COPY_STATUS, TRANSFER_MARK, SWAP_BARRIER,
		TRANSFORM_MODE, TRANSFORM_CAPABILITY, TRANSFORM_BEHAVIOR
	}

	public EffectFamily family = EffectFamily.STATUS;
	public RuleEffect.Type variant = RuleEffect.Type.POISON;
	public Operation operation = Operation.LEGACY;
	public int power = 2;
	public int duration = 5;
	public int count = 1;
	public int period = 1;
	public int lifetime = 5;
	public int secondaryParameter;
	public String resourceId = "";
	public String targetResourceId = "";
	public String templateId = "rat";
	public String stateId = "mode";
	public DamageType damageType = DamageType.UNTYPED;
	public ScalingSource scalingSource = ScalingSource.FIXED;
	public StatusStacking statusStacking = StatusStacking.NATIVE;

	public EffectSpec() {}

	public EffectSpec(RuleEffect.Type variant, int power) {
		EffectSpec mapped = fromLegacy(variant, power, null);
		this.family = mapped.family;
		this.variant = mapped.variant;
		this.operation = mapped.operation;
		this.power = mapped.power;
		this.duration = mapped.duration;
		this.secondaryParameter = mapped.secondaryParameter;
	}

	public EffectSpec(EffectFamily family, Operation operation, int power) {
		this.family = family;
		this.operation = operation;
		this.power = power;
		this.variant = adapterVariant(family, operation);
		if (operation == Operation.DAMAGE_EXECUTE) secondaryParameter = 20;
		else if (operation == Operation.DAMAGE_MISSING_HP) secondaryParameter = 50;
		else if (operation == Operation.CREATE_ACTOR) { templateId = "rat"; stateId = "actor"; }
		else if (operation == Operation.CREATE_DEVICE) { templateId = "device"; stateId = "fire"; }
		else if (operation == Operation.CREATE_TRAP) { templateId = "trap"; stateId = "poison"; }
		else if (operation == Operation.CREATE_FIELD) { templateId = "field"; stateId = "poison"; }
		else if (operation == Operation.MARK_APPLY || operation == Operation.MARK_STACK
				|| operation == Operation.MARK_COUNTER || operation == Operation.MARK_CONSUME
				|| operation == Operation.MARK_SPREAD) stateId = "HUNTED";
	}

	public EffectSpec copy() {
		EffectSpec result = new EffectSpec();
		result.family=family; result.variant=variant; result.operation=operation; result.power=power;
		result.duration=duration; result.count=count; result.period=period; result.lifetime=lifetime;
		result.secondaryParameter=secondaryParameter; result.resourceId=resourceId;
		result.targetResourceId=targetResourceId; result.templateId=templateId; result.stateId=stateId;
		result.damageType=damageType; result.scalingSource=scalingSource; result.statusStacking=statusStacking;
		return result;
	}

	public boolean implemented() {
		if (family == null || operation == null) return false;
		// SPD has one authoritative Hero resource owner, so cross-Hero resource transfer is not a
		// player-usable capability. Full capability/behaviour overrides likewise need entity-class
		// replacement semantics that this adapter deliberately does not fake; Mode Shift is supported.
		if (operation == Operation.TRANSFER_RESOURCE
				|| operation == Operation.TRANSFORM_CAPABILITY
				|| operation == Operation.TRANSFORM_BEHAVIOR) return false;
		return operation == Operation.LEGACY
				? variant != null && family == EffectFamily.forEffect(variant)
				: familyFor(operation) == family;
	}

	/**
	 * MIGRATION_ONLY mapping from the pre-ClassBuild effect vocabulary. New builders must select
	 * an Operation through EffectVocabularyRegistry and never call this as their vocabulary source.
	 */
	public static EffectSpec fromLegacy(RuleEffect.Type legacy, int power, RuleTarget.Type target) {
		if (legacy == null) legacy = RuleEffect.Type.PUSH;
		Operation operation;
		switch (legacy) {
			case PUSH: operation = Operation.MOVE_PUSH; break;
			case PULL: operation = Operation.MOVE_PULL; break;
			case TELEPORT: operation = Operation.MOVE_TELEPORT; break;
			case SWAP_POSITION: operation = Operation.MOVE_SWAP; break;
			case POISON: operation = Operation.STATUS_POISON; break;
			case FIRE: operation = target == RuleTarget.Type.SELECTED_CELL
					? Operation.WORLD_FIRE : Operation.STATUS_BURNING; break;
			case BLEED: operation = Operation.STATUS_BLEEDING; break;
			case SLOW: operation = Operation.STATUS_SLOW; break;
			case HASTE: operation = Operation.STATUS_HASTE; break;
			case HEAL: operation = Operation.RECOVER_HEAL; break;
			case SHIELD: operation = Operation.DEFENSE_BARRIER; break;
			case CLEANSE: operation = Operation.DEFENSE_CLEANSE; break;
			case CREATE_WATER: operation = Operation.WORLD_WATER; break;
			case CREATE_GAS: operation = Operation.WORLD_TOXIC_GAS; break;
			default: operation = Operation.DAMAGE_STANDARD; break;
		}
		EffectFamily family = familyFor(operation);
		EffectSpec result = new EffectSpec(family, operation, power);
		result.variant = legacy;
		return result;
	}

	/** Parameters which can be validated without knowing the owning ClassBuild. */
	public boolean configurationValid() {
		if (!implemented() || power < 0 || duration < 1 || count < 1 || period < 1 || lifetime < 1) return false;
		if (operation == Operation.RESOURCE_CONVERT) {
			return targetResourceId != null && !targetResourceId.isEmpty()
					&& (resourceId == null || !targetResourceId.equals(resourceId));
		}
		if (family == EffectFamily.DAMAGE && scalingSource == ScalingSource.CURRENT_RESOURCE) {
			return resourceId != null && !resourceId.isEmpty();
		}
		return true;
	}

	/** Runtime-level target contract. This prevents schemas which compile but can only no-op. */
	public boolean compatibleTargeting(TargetingSpec targeting) {
		if (targeting == null || !implemented()) return false;
		if (operation == Operation.LEGACY) return RuleEffect.compatibleTarget(variant,
				targeting.compileType(RuleEvent.ACTIVE));
		switch (operation) {
			case MOVE_PUSH:
			case MOVE_PULL:
			case MOVE_THROW:
			case MOVE_SWAP:
				return targeting.selector != TargetingSpec.Selector.SELF
						&& targeting.selector != TargetingSpec.Selector.SELECTED_CELL
						&& targeting.filter != TargetingSpec.Filter.SELF;
			case MOVE_DASH:
				return targeting.selector == TargetingSpec.Selector.SELECTED_CELL;
			case CREATE_ACTOR:
			case CREATE_DEVICE:
			case CREATE_TRAP:
			case CREATE_FIELD:
			case WORLD_WATER:
			case WORLD_GRASS:
			case WORLD_DESTROY:
			case WORLD_TOXIC_GAS:
			case WORLD_FIRE:
			case WORLD_CLEAR_HAZARD:
				return targeting.selector == TargetingSpec.Selector.SELECTED_CELL
						|| targeting.selector == TargetingSpec.Selector.SELF;
			case RESOURCE_GAIN:
			case RESOURCE_DRAIN:
			case RESOURCE_CONVERT:
			case RESOURCE_RESERVE:
			case RESOURCE_SUPPRESS:
			case TRANSFORM_MODE:
				return targeting.selector == TargetingSpec.Selector.SELF;
			default:
				return true;
		}
	}

	public RuleEffect compile() { return new RuleEffect(operation == Operation.LEGACY ? variant : adapterVariant(family, operation), power); }

	public int powerCost() {
		if (!implemented()) return 999;
		if (operation == Operation.LEGACY) return compile().capacityCost();
		int result;
		switch (family) {
			case DAMAGE: result=operation==Operation.DAMAGE_EXECUTE?7:operation==Operation.DAMAGE_PERCENT?5:3; break;
			case STATUS: result=operation==Operation.STATUS_PARALYSIS?5:3; break;
			case MOVEMENT: result=3; break;
			case RECOVERY_DEFENSE: result=operation==Operation.DEFENSE_REDIRECT?5:3; break;
			case RESOURCE_OPERATION:
			case MARK_ACCUMULATION: result=2; break;
			case CREATE_ENTITY:
				// A one-shot trap has no autonomous lifetime/action stream. Actors, fields and
				// devices are priced for count and persistence separately.
				result=operation==Operation.CREATE_TRAP?3:4+Math.max(0,count-1)+Math.max(0,lifetime/5);
				break;
			case WORLD_TERRAIN:
				switch (operation) {
					case WORLD_WATER:
					case WORLD_GRASS:
					case WORLD_CLEAR_HAZARD: result=2; break;
					case WORLD_DESTROY: result=3; break;
					case WORLD_TOXIC_GAS:
					case WORLD_FIRE:
					default: result=4; break;
				}
				break;
			default: result=4;
		}
		if (family == EffectFamily.DAMAGE && (damageType != DamageType.UNTYPED
				|| scalingSource != ScalingSource.FIXED)) result++;
		if (family == EffectFamily.STATUS && statusStacking != StatusStacking.NATIVE) result++;
		return result + Math.max(0,power-4)/2 + Math.max(0,duration-5)/5;
	}

	public String displayName(){
		return operation==Operation.LEGACY ? compile().description()
				: Messages.get(EffectSpec.class,operation.name().toLowerCase()+"_name");
	}
	public String description(){
		return operation==Operation.LEGACY ? compile().description()
				: Messages.get(EffectSpec.class,operation.name().toLowerCase()+"_desc");
	}
	public String parameterSummary(){
		if(operation==Operation.LEGACY)return Messages.get(EffectSpec.class,"parameters_legacy",power);
		if(family==EffectFamily.MOVEMENT)return Messages.get(EffectSpec.class,"parameters_distance",power);
		switch(family){
			case DAMAGE:return Messages.get(EffectSpec.class,"parameters_damage",power,secondaryParameter,damageTypeName(),scalingSourceName());
			case STATUS:return Messages.get(EffectSpec.class,"parameters_status",power,duration,statusStackingName());
			case CREATE_ENTITY:return Messages.get(EffectSpec.class,"parameters_entity",power,count,lifetime,period,stateId);
			case RESOURCE_OPERATION:
				return operation==Operation.RESOURCE_CONVERT
						?Messages.get(EffectSpec.class,"parameters_convert",power,resourceId,targetResourceId)
						:Messages.get(EffectSpec.class,"parameters_resource",power,resourceId);
			case MARK_ACCUMULATION:return Messages.get(EffectSpec.class,"parameters_mark",power,duration,stateId);
			case TRANSFORM:return Messages.get(EffectSpec.class,"parameters_mode",stateId,duration);
			default:return Messages.get(EffectSpec.class,"parameters_general",power,duration);
		}
	}
	public String damageTypeName(){return Messages.get(EffectSpec.class,"damage_type_"+damageType.name().toLowerCase());}
	public String scalingSourceName(){return Messages.get(EffectSpec.class,"scaling_"+scalingSource.name().toLowerCase());}
	public String statusStackingName(){return Messages.get(EffectSpec.class,"stacking_"+statusStacking.name().toLowerCase());}

	public static EffectFamily familyFor(Operation op) {
		String n=op.name();
		if(n.startsWith("DAMAGE"))return EffectFamily.DAMAGE;
		if(n.startsWith("STATUS"))return EffectFamily.STATUS;
		if(n.startsWith("MOVE"))return EffectFamily.MOVEMENT;
		if(n.startsWith("RECOVER")||n.startsWith("DEFENSE"))return EffectFamily.RECOVERY_DEFENSE;
		if(n.startsWith("RESOURCE"))return EffectFamily.RESOURCE_OPERATION;
		if(n.startsWith("MARK"))return EffectFamily.MARK_ACCUMULATION;
		if(n.startsWith("CREATE"))return EffectFamily.CREATE_ENTITY;
		if(n.startsWith("WORLD"))return EffectFamily.WORLD_TERRAIN;
		if(n.startsWith("RELATION"))return EffectFamily.RELATION_CONTROL;
		if(n.startsWith("TRANSFER")||n.startsWith("COPY")||n.startsWith("SWAP"))return EffectFamily.TRANSFER_COPY;
		if(n.startsWith("TRANSFORM"))return EffectFamily.TRANSFORM;
		return null;
	}

	private static RuleEffect.Type adapterVariant(EffectFamily family, Operation operation) {
		if(operation==Operation.MOVE_PUSH||operation==Operation.MOVE_THROW)return RuleEffect.Type.PUSH;
		if(operation==Operation.MOVE_PULL)return RuleEffect.Type.PULL;
		if(operation==Operation.MOVE_DASH||operation==Operation.MOVE_TELEPORT)return RuleEffect.Type.TELEPORT;
		if(operation==Operation.MOVE_SWAP)return RuleEffect.Type.SWAP_POSITION;
		if(operation==Operation.STATUS_BURNING||operation==Operation.WORLD_FIRE)return RuleEffect.Type.FIRE;
		if(operation==Operation.STATUS_POISON)return RuleEffect.Type.POISON;
		if(operation==Operation.STATUS_BLEEDING)return RuleEffect.Type.BLEED;
		if(operation==Operation.STATUS_SLOW)return RuleEffect.Type.SLOW;
		if(operation==Operation.STATUS_HASTE)return RuleEffect.Type.HASTE;
		if(operation==Operation.RECOVER_HEAL)return RuleEffect.Type.HEAL;
		if(operation==Operation.DEFENSE_CLEANSE)return RuleEffect.Type.CLEANSE;
		if(operation==Operation.MOVE_DASH)return RuleEffect.Type.TELEPORT;
		if(operation==Operation.MOVE_THROW)return RuleEffect.Type.PUSH;
		if(operation==Operation.WORLD_WATER)return RuleEffect.Type.CREATE_WATER;
		if(operation==Operation.WORLD_TOXIC_GAS)return RuleEffect.Type.CREATE_GAS;
		if(family==EffectFamily.MOVEMENT)return RuleEffect.Type.PUSH;
		if(family==EffectFamily.RECOVERY_DEFENSE)return RuleEffect.Type.SHIELD;
		if(family==EffectFamily.WORLD_TERRAIN)return RuleEffect.Type.CREATE_WATER;
		return RuleEffect.Type.FIRE;
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("family", family);
		bundle.put("variant", variant);
		bundle.put("operation", operation);
		bundle.put("power", power);
		bundle.put("duration",duration); bundle.put("count",count); bundle.put("period",period);
		bundle.put("lifetime",lifetime); bundle.put("secondary_parameter",secondaryParameter);
		bundle.put("resource_id",resourceId); bundle.put("target_resource_id",targetResourceId);
		bundle.put("template_id",templateId); bundle.put("state_id",stateId);
		bundle.put("damage_type",damageType); bundle.put("scaling_source",scalingSource); bundle.put("status_stacking",statusStacking);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		family = bundle.getEnum("family", EffectFamily.class);
		variant = bundle.getEnum("variant", RuleEffect.Type.class);
		operation = bundle.contains("operation") ? bundle.getEnum("operation", Operation.class) : Operation.LEGACY;
		power = bundle.getInt("power");
		duration=bundle.contains("duration")?bundle.getInt("duration"):5;
		count=bundle.contains("count")?Math.max(1,bundle.getInt("count")):1;
		period=bundle.contains("period")?Math.max(1,bundle.getInt("period")):1;
		lifetime=bundle.contains("lifetime")?Math.max(1,bundle.getInt("lifetime")):5;
		secondaryParameter=bundle.getInt("secondary_parameter"); resourceId=bundle.getString("resource_id");
		targetResourceId=bundle.getString("target_resource_id"); templateId=bundle.getString("template_id"); stateId=bundle.getString("state_id");
		if(resourceId==null)resourceId=""; if(targetResourceId==null)targetResourceId="";
		if(templateId==null||templateId.isEmpty())templateId="rat"; if(stateId==null||stateId.isEmpty())stateId="mode";
		damageType=bundle.contains("damage_type")?bundle.getEnum("damage_type",DamageType.class):DamageType.UNTYPED;
		scalingSource=bundle.contains("scaling_source")?bundle.getEnum("scaling_source",ScalingSource.class):ScalingSource.FIXED;
		statusStacking=bundle.contains("status_stacking")?bundle.getEnum("status_stacking",StatusStacking.class):StatusStacking.NATIVE;
	}
}
