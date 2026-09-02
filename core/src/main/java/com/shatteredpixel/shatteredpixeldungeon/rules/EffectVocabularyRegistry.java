package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Authoritative player exposure for the frozen EffectFamily -> variant -> parameters flow. */
public final class EffectVocabularyRegistry {
	public enum Parameters {
		NONE, DAMAGE, STATUS, DISTANCE, POWER_DURATION, TEMPORARY_HP, RESOURCE,
		RESOURCE_CONVERSION, MARK, ENTITY, CARRIER, MODE, INHERIT
	}

	public static final class FamilyEntry {
		public final EffectFamily family;
		private FamilyEntry(EffectFamily family) { this.family = family; }
		public String name() { return family.displayName(); }
		public String summary() { return Messages.get(EffectVocabularyRegistry.class,
				"family_" + family.name().toLowerCase() + "_summary"); }
		public String detail() { return Messages.get(EffectVocabularyRegistry.class,
				"family_" + family.name().toLowerCase() + "_detail"); }
	}

	public static final class EffectEntry {
		public final EffectFamily family;
		public final EffectSpec.Operation operation;
		public final Parameters parameters;
		private EffectEntry(EffectFamily family, EffectSpec.Operation operation, Parameters parameters) {
			this.family = family;
			this.operation = operation;
			this.parameters = parameters;
		}
		public EffectSpec create() { return new EffectSpec(family, operation, 2); }
		public String name() { return create().displayName(); }
		public String summary() { return create().description(); }
		public String detail() { return Messages.get(EffectVocabularyRegistry.class, "effect_detail",
				summary(), create().powerCost()); }
		public boolean supported() { return create().implemented(); }
		public boolean hasParameters() { return parameters != Parameters.NONE; }
	}

	/** One independently editable effect setting. Re-opening the page edits another field. */
	public static final class ParameterOption {
		public final EffectSpec spec;
		public final String name;
		private ParameterOption(EffectSpec spec, String name) { this.spec = spec; this.name = name; }
		public String summary() { return spec.parameterSummary(); }
		public String detail() { return spec.description(); }
	}

	private static final EffectFamily[] FAMILY_ORDER = {
			EffectFamily.DAMAGE, EffectFamily.STATUS, EffectFamily.MOVEMENT,
			EffectFamily.RECOVERY_DEFENSE, EffectFamily.RESOURCE_OPERATION,
			EffectFamily.MARK_ACCUMULATION, EffectFamily.CREATE_ENTITY,
			EffectFamily.WORLD_TERRAIN, EffectFamily.RELATION_CONTROL,
			EffectFamily.TRANSFER_COPY, EffectFamily.TRANSFORM
	};

	private static final EffectEntry[] EFFECTS = {
		e(EffectSpec.Operation.DAMAGE_STANDARD, Parameters.DAMAGE),
		e(EffectSpec.Operation.DAMAGE_PERCENT, Parameters.DAMAGE),
		e(EffectSpec.Operation.DAMAGE_MISSING_HP, Parameters.DAMAGE),
		e(EffectSpec.Operation.DAMAGE_EXECUTE, Parameters.DAMAGE),
		e(EffectSpec.Operation.STATUS_POISON, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_BURNING, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_BLEEDING, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_SLOW, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_HASTE, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_PARALYSIS, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_ROOTS, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_AMOK, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_TERROR, Parameters.STATUS),
		e(EffectSpec.Operation.STATUS_VULNERABLE, Parameters.STATUS),
		e(EffectSpec.Operation.MOVE_PUSH, Parameters.DISTANCE),
		e(EffectSpec.Operation.MOVE_PULL, Parameters.DISTANCE),
		e(EffectSpec.Operation.MOVE_THROW, Parameters.DISTANCE),
		e(EffectSpec.Operation.MOVE_DASH, Parameters.DISTANCE),
		e(EffectSpec.Operation.MOVE_TELEPORT, Parameters.NONE),
		e(EffectSpec.Operation.MOVE_SWAP, Parameters.NONE),
		e(EffectSpec.Operation.RECOVER_HEAL, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.DEFENSE_BARRIER, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.DEFENSE_TEMP_HP, Parameters.TEMPORARY_HP),
		e(EffectSpec.Operation.DEFENSE_MITIGATE, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.DEFENSE_REDIRECT, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.DEFENSE_CLEANSE, Parameters.NONE),
		e(EffectSpec.Operation.RESOURCE_GAIN, Parameters.RESOURCE),
		e(EffectSpec.Operation.RESOURCE_DRAIN, Parameters.RESOURCE),
		e(EffectSpec.Operation.RESOURCE_CONVERT, Parameters.RESOURCE_CONVERSION),
		e(EffectSpec.Operation.RESOURCE_RESERVE, Parameters.RESOURCE),
		e(EffectSpec.Operation.RESOURCE_SUPPRESS, Parameters.RESOURCE),
		e(EffectSpec.Operation.MARK_APPLY, Parameters.MARK),
		e(EffectSpec.Operation.MARK_STACK, Parameters.MARK),
		e(EffectSpec.Operation.MARK_COUNTER, Parameters.MARK),
		e(EffectSpec.Operation.MARK_CONSUME, Parameters.MARK),
		e(EffectSpec.Operation.MARK_SPREAD, Parameters.MARK),
		e(EffectSpec.Operation.CREATE_ACTOR, Parameters.ENTITY),
		e(EffectSpec.Operation.CREATE_DEVICE, Parameters.CARRIER),
		e(EffectSpec.Operation.CREATE_TRAP, Parameters.CARRIER),
		e(EffectSpec.Operation.CREATE_FIELD, Parameters.CARRIER),
		e(EffectSpec.Operation.WORLD_WATER, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.WORLD_GRASS, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.WORLD_DESTROY, Parameters.NONE),
		e(EffectSpec.Operation.WORLD_TOXIC_GAS, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.WORLD_FIRE, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.WORLD_CLEAR_HAZARD, Parameters.NONE),
		e(EffectSpec.Operation.RELATION_OWNERSHIP, Parameters.NONE),
		e(EffectSpec.Operation.RELATION_LINK, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.RELATION_COMMAND_FOLLOW, Parameters.NONE),
		e(EffectSpec.Operation.RELATION_COMMAND_ATTACK, Parameters.NONE),
		e(EffectSpec.Operation.RELATION_COMMAND_GUARD, Parameters.NONE),
		e(EffectSpec.Operation.RELATION_INHERIT, Parameters.INHERIT),
		e(EffectSpec.Operation.RELATION_BREAK, Parameters.NONE),
		e(EffectSpec.Operation.COPY_STATUS, Parameters.POWER_DURATION),
		e(EffectSpec.Operation.TRANSFER_MARK, Parameters.MARK),
		e(EffectSpec.Operation.SWAP_BARRIER, Parameters.NONE),
		e(EffectSpec.Operation.TRANSFORM_MODE, Parameters.MODE)
	};

	private EffectVocabularyRegistry() {}

	private static EffectEntry e(EffectSpec.Operation operation, Parameters parameters) {
		return new EffectEntry(EffectSpec.familyFor(operation), operation, parameters);
	}

	public static List<FamilyEntry> families() {
		ArrayList<FamilyEntry> result = new ArrayList<>();
		for (EffectFamily family : FAMILY_ORDER) result.add(new FamilyEntry(family));
		return Collections.unmodifiableList(result);
	}

	public static List<EffectEntry> variants(EffectFamily family) {
		ArrayList<EffectEntry> result = new ArrayList<>();
		for (EffectEntry entry : EFFECTS) if (entry.family == family && entry.supported()) result.add(entry);
		return Collections.unmodifiableList(result);
	}

	public static List<EffectEntry> exposed() {
		ArrayList<EffectEntry> result = new ArrayList<>();
		for (EffectEntry entry : EFFECTS) if (entry.supported()) result.add(entry);
		return Collections.unmodifiableList(result);
	}

	public static EffectEntry find(EffectSpec effect) {
		if (effect == null) return null;
		for (EffectEntry entry : EFFECTS) if (entry.operation == effect.operation) return entry;
		return null;
	}

	public static boolean exposed(EffectSpec.Operation operation) {
		for (EffectEntry entry : EFFECTS) if (entry.operation == operation && entry.supported()) return true;
		return false;
	}

	/**
	 * Shared Builder/QA parameter authority. Each choice changes one orthogonal setting and keeps
	 * the rest, so every value used by a reference build can be reproduced without a giant
	 * Cartesian-product menu.
	 */
	public static List<ParameterOption> parameterOptions(EffectSpec current, ClassBuild build) {
		ArrayList<ParameterOption> result = new ArrayList<>();
		EffectEntry entry = find(current);
		if (current == null || entry == null) return result;
		if (entry.parameters == Parameters.NONE) {
			result.add(option(current.copy(), Messages.get(EffectVocabularyRegistry.class, "parameter_none")));
			return result;
		}
		if (entry.parameters == Parameters.DAMAGE) {
			for (int value = 1; value <= 10; value++) addPower(result, current, value);
			if (current.operation == EffectSpec.Operation.DAMAGE_MISSING_HP) {
				for (int value = 10; value <= 100; value += 10) {
					EffectSpec copy=current.copy(); copy.secondaryParameter=value;
					result.add(option(copy, Messages.get(EffectVocabularyRegistry.class,
							"parameter_missing_hp_scale", value)));
				}
			} else if (current.operation == EffectSpec.Operation.DAMAGE_EXECUTE) {
				for (int value = 10; value <= 40; value += 10) {
					EffectSpec copy=current.copy(); copy.secondaryParameter=value;
					result.add(option(copy, Messages.get(EffectVocabularyRegistry.class,
							"parameter_execute_threshold", value)));
				}
			}
			for (EffectSpec.DamageType value : EffectSpec.DamageType.values()) {
				EffectSpec copy=current.copy(); copy.damageType=value;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_damage_type", copy.damageTypeName())));
			}
			for (EffectSpec.ScalingSource value : EffectSpec.ScalingSource.values()) {
				if (value == EffectSpec.ScalingSource.CURRENT_RESOURCE) continue;
				EffectSpec copy=current.copy(); copy.scalingSource=value; copy.resourceId="";
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_scaling", copy.scalingSourceName())));
			}
			for (ResourceSpec resource : build.resources) {
				EffectSpec copy=current.copy(); copy.scalingSource=EffectSpec.ScalingSource.CURRENT_RESOURCE; copy.resourceId=resource.id;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_scaling_resource", resource.displayName())));
			}
		} else if (entry.parameters == Parameters.STATUS) {
			for (int value = 1; value <= 10; value++) addPower(result, current, value);
			for (int value = 1; value <= 12; value++) addDuration(result, current, value);
			for (EffectSpec.StatusStacking value : EffectSpec.StatusStacking.values()) {
				EffectSpec copy=current.copy(); copy.statusStacking=value;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_stacking", copy.statusStackingName())));
			}
		} else if (entry.parameters == Parameters.DISTANCE) {
			for (int value = 1; value <= 6; value++) addPower(result, current, value);
		} else if (entry.parameters == Parameters.POWER_DURATION || entry.parameters == Parameters.TEMPORARY_HP) {
			for (int value = 1; value <= 12; value++) addPower(result, current, value);
			for (int value = 1; value <= 12; value++) addDuration(result, current, value);
		} else if (entry.parameters == Parameters.RESOURCE_CONVERSION) {
			for (ResourceSpec source : build.resources) for (ResourceSpec target : build.resources) if (!source.id.equals(target.id)) {
				EffectSpec copy=current.copy(); copy.resourceId=source.id; copy.targetResourceId=target.id;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_conversion",
						source.displayName(), target.displayName())));
			}
			for (int value = 1; value <= 6; value++) addPower(result, current, value);
		} else if (entry.parameters == Parameters.RESOURCE) {
			for (ResourceSpec resource : build.resources) {
				EffectSpec copy=current.copy(); copy.resourceId=resource.id;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_resource", resource.displayName())));
			}
			for (int value = 1; value <= 10; value++) addPower(result, current, value);
		} else if (entry.parameters == Parameters.ENTITY) {
			for (int value = 1; value <= 10; value++) addPower(result, current, value);
			for (int value = 1; value <= 3; value++) { EffectSpec copy=current.copy(); copy.count=value;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_count", value))); }
			for (int value = 1; value <= 12; value++) { EffectSpec copy=current.copy(); copy.lifetime=value;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_lifetime", value))); }
		} else if (entry.parameters == Parameters.CARRIER) {
			for (int value = 1; value <= 10; value++) addPower(result, current, value);
			for (int value = 1; value <= 16; value++) { EffectSpec copy=current.copy(); copy.lifetime=value;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_lifetime", value))); }
			for (int value = 1; value <= 4; value++) { EffectSpec copy=current.copy(); copy.period=value;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_period", value))); }
			for (String payload : new String[]{"fire","poison","heal"}) { EffectSpec copy=current.copy(); copy.stateId=payload;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_payload_"+payload))); }
		} else if (entry.parameters == Parameters.MARK) {
			for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark.Type mark
					: com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark.Type.values()) {
				EffectSpec copy=current.copy(); copy.stateId=mark.name();
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_mark_"+mark.name().toLowerCase())));
			}
			for (int value = 1; value <= 6; value++) addPower(result, current, value);
			for (int value = 1; value <= 12; value++) addDuration(result, current, value);
		} else if (entry.parameters == Parameters.INHERIT) {
			for (String capability : new String[]{"mode","haste","mitigation"}) { EffectSpec copy=current.copy(); copy.stateId=capability;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_inherit_"+capability))); }
		} else if (entry.parameters == Parameters.MODE) {
			if (build == null || build.modes().isEmpty()) {
				EffectSpec copy=current.copy(); copy.stateId="";
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_mode_pending")));
			} else for (String mode : build.modes()) { EffectSpec copy=current.copy(); copy.stateId=mode;
				result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_mode_named", mode))); }
			for (int value = 1; value <= 12; value++) addDuration(result, current, value);
		}
		return Collections.unmodifiableList(result);
	}

	/** Exact inverse contract used by QA: every stored parameter must be selectable in the Builder. */
	public static boolean playerReachable(EffectSpec value, ClassBuild build) {
		EffectEntry entry = find(value);
		if (value == null || entry == null || !entry.supported() || !value.configurationValid()) return false;
		switch (entry.parameters) {
			case NONE: return true;
			case DAMAGE:
				if (value.power < 1 || value.power > 10) return false;
				if (value.scalingSource == EffectSpec.ScalingSource.CURRENT_RESOURCE
						&& !hasResource(build, value.resourceId)) return false;
				if (value.operation == EffectSpec.Operation.DAMAGE_MISSING_HP)
					return value.secondaryParameter >= 10 && value.secondaryParameter <= 100
							&& value.secondaryParameter % 10 == 0;
				if (value.operation == EffectSpec.Operation.DAMAGE_EXECUTE)
					return value.secondaryParameter >= 10 && value.secondaryParameter <= 40
							&& value.secondaryParameter % 10 == 0;
				return true;
			case STATUS: return in(value.power,1,10) && in(value.duration,1,12);
			case DISTANCE: return in(value.power,1,6);
			case POWER_DURATION:
			case TEMPORARY_HP: return in(value.power,1,12) && in(value.duration,1,12);
			case RESOURCE: return in(value.power,1,10) && hasResource(build,value.resourceId);
			case RESOURCE_CONVERSION: return in(value.power,1,6)
					&& hasResource(build,value.resourceId) && hasResource(build,value.targetResourceId)
					&& !value.resourceId.equals(value.targetResourceId);
			case MARK:
				try { com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark.Type.valueOf(value.stateId); }
				catch (Exception ignored) { return false; }
				return in(value.power,1,6) && in(value.duration,1,12);
			case ENTITY: return in(value.power,1,10) && in(value.count,1,3) && in(value.lifetime,1,12);
			case CARRIER: return in(value.power,1,10) && in(value.lifetime,1,16)
					&& in(value.period,1,4) && ("fire".equals(value.stateId)
					|| "poison".equals(value.stateId) || "heal".equals(value.stateId));
			case MODE: return build != null && build.producesMode(value.stateId) && in(value.duration,1,12);
			case INHERIT: return "mode".equals(value.stateId) || "haste".equals(value.stateId)
					|| "mitigation".equals(value.stateId);
			default: return false;
		}
	}

	private static boolean in(int value,int min,int max){return value>=min&&value<=max;}
	private static boolean hasResource(ClassBuild build,String id){return build!=null&&id!=null&&build.resource(id)!=null;}

	private static ParameterOption option(EffectSpec spec, String name) { return new ParameterOption(spec, name); }
	private static void addPower(ArrayList<ParameterOption> result, EffectSpec current, int value) {
		EffectSpec copy=current.copy(); copy.power=value;
		result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_power", value)));
	}
	private static void addDuration(ArrayList<ParameterOption> result, EffectSpec current, int value) {
		EffectSpec copy=current.copy(); copy.duration=value;
		result.add(option(copy, Messages.get(EffectVocabularyRegistry.class, "parameter_duration", value)));
	}
}
