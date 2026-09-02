package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;

/**
 * One independently editable class-level gameplay mechanism.
 *
 * <p>This is deliberately not an archetype tag and not a Skill.  Each instance owns only the
 * parameters needed by one class-wide mechanism and may be added, removed, saved and rebound
 * independently.</p>
 */
public class ClassGameplayComponentSpec implements Bundlable {

	public enum Type {
		BASIC_ATTACK,
		RESOURCE_FLOW,
		ACTIVE_REFILL,
		OWNERSHIP,
		ENTITY_CAPACITY,
		PERSISTENCE,
		COMMAND,
		RECYCLE,
		MODE_ENGINE,
		GLOBAL_CONSTRAINT
	}

	public enum EntityFilter { OWNED_ACTOR, OWNED_DEVICE, OWNED_CARRIER, OWNED_ENTITY }

	public String id = "component";
	public String name = "";
	public Type type = Type.BASIC_ATTACK;

	public BasicAttackProfile basicAttack = BasicAttackProfile.WEAK;
	public String resourceId = "";
	public String targetResourceId = "";
	public ResourceFlowSpec.Trigger trigger = ResourceFlowSpec.Trigger.TURN;
	public ResourceFlowSpec.Operation resourceOperation = ResourceFlowSpec.Operation.GAIN;
	public int amount = 1;
	/** Amount received by the destination of a conversion. */
	public int targetAmount = 1;
	public int interval = 1;
	public int delay;
	public boolean meleeOnly;
	public float actionTime = 1f;
	public EntityFilter entityFilter = EntityFilter.OWNED_ENTITY;
	public int capacity = 1;
	public int lifetime = 5;
	public final ArrayList<String> modes = new ArrayList<>();
	public Restriction restriction = Restriction.NONE;

	public ClassGameplayComponentSpec() {}

	public ClassGameplayComponentSpec(Type type, String id) {
		this.type = type;
		this.id = id;
	}

	public static ClassGameplayComponentSpec basicAttack(BasicAttackProfile profile) {
		ClassGameplayComponentSpec result = new ClassGameplayComponentSpec(Type.BASIC_ATTACK, "basic_attack");
		result.basicAttack = profile == null ? BasicAttackProfile.WEAK : profile;
		return result;
	}

	public static ClassGameplayComponentSpec resourceFlow(String id, String resourceId,
			ResourceFlowSpec.Trigger trigger, ResourceFlowSpec.Operation operation, int amount,
			int interval, int delay) {
		ClassGameplayComponentSpec result = new ClassGameplayComponentSpec(Type.RESOURCE_FLOW, id);
		result.resourceId = resourceId == null ? "" : resourceId;
		result.trigger = trigger;
		result.resourceOperation = operation;
		result.amount = Math.max(0, amount);
		result.interval = Math.max(1, interval);
		result.delay = Math.max(0, delay);
		return result;
	}

	public static ClassGameplayComponentSpec activeRefill(String id, String resourceId,
			int amount, float actionTime) {
		ClassGameplayComponentSpec result = new ClassGameplayComponentSpec(Type.ACTIVE_REFILL, id);
		result.resourceId = resourceId == null ? "" : resourceId;
		result.amount = Math.max(0, amount);
		result.actionTime = Math.max(1f, actionTime);
		return result;
	}

	public ClassGameplayComponentSpec copy() {
		ClassGameplayComponentSpec result = new ClassGameplayComponentSpec(type, id);
		result.name = name;
		result.basicAttack = basicAttack;
		result.resourceId = resourceId;
		result.targetResourceId = targetResourceId;
		result.trigger = trigger;
		result.resourceOperation = resourceOperation;
		result.amount = amount;
		result.targetAmount = targetAmount;
		result.interval = interval;
		result.delay = delay;
		result.meleeOnly = meleeOnly;
		result.actionTime = actionTime;
		result.entityFilter = entityFilter;
		result.capacity = capacity;
		result.lifetime = lifetime;
		result.modes.addAll(modes);
		result.restriction = restriction;
		return result;
	}

	/** Structural validity intentionally permits missing cross-component bindings. */
	public boolean structurallyValid() {
		if (id == null || id.isEmpty() || type == null || amount < 0 || targetAmount < 1
				|| interval < 1 || delay < 0 || actionTime < 1f || capacity < 1 || lifetime < 1) return false;
		switch (type) {
			case BASIC_ATTACK: return basicAttack != null;
			case RESOURCE_FLOW:
				return trigger != null && resourceOperation != null;
			case ENTITY_CAPACITY:
			case PERSISTENCE: return entityFilter != null;
			case MODE_ENGINE:
				if (modes.size() < 2) return false;
				for (String mode : modes) if (mode == null || mode.trim().isEmpty()) return false;
				return true;
			case GLOBAL_CONSTRAINT: return restriction != null && restriction != Restriction.NONE;
			default: return true;
		}
	}

	public ComponentDependency dependency(ClassBuild build) {
		if (!structurallyValid()) return ComponentDependency.hard("invalid_component_parameters");
		switch (type) {
			case RESOURCE_FLOW:
			case ACTIVE_REFILL:
				if (build == null || build.resource(resourceId) == null) return ComponentDependency.unresolved("needs_resource");
				if (type == Type.RESOURCE_FLOW && resourceOperation == ResourceFlowSpec.Operation.CONVERT
						&& (targetResourceId == null || build.resource(targetResourceId) == null)) {
					return ComponentDependency.unresolved("needs_target_resource");
				}
				if (type == Type.RESOURCE_FLOW && resourceOperation == ResourceFlowSpec.Operation.CONVERT
						&& resourceId.equals(targetResourceId)) return ComponentDependency.hard("same_resource_conversion");
				return ComponentDependency.resolved();
			case ENTITY_CAPACITY:
			case PERSISTENCE:
			case COMMAND:
			case RECYCLE:
				if (build == null || !build.hasGameplayComponent(Type.OWNERSHIP)) {
					return ComponentDependency.unresolved("needs_ownership");
				}
				if (!hasCompatibleEntitySource(build)) {
					return ComponentDependency.unresolved("needs_entity_source");
				}
				if (type == Type.RECYCLE && build.resource(resourceId) == null) {
					return ComponentDependency.unresolved("needs_resource");
				}
				return ComponentDependency.resolved();
			default: return ComponentDependency.resolved();
		}
	}

	private boolean hasCompatibleEntitySource(ClassBuild build) {
		if (build == null) return false;
		for (SkillSpec skill : build.skills) {
			if (skill != null && ((skill.delivery == SkillDelivery.PERSISTENT_CARRIER
					&& (entityFilter == EntityFilter.OWNED_ENTITY || entityFilter == EntityFilter.OWNED_CARRIER))
					|| matchesCreatedEntity(skill.primary) || matchesCreatedEntity(skill.secondary))) return true;
		}
		return false;
	}

	private boolean matchesCreatedEntity(EffectSpec effect) {
		if (effect == null) return false;
		if (entityFilter == EntityFilter.OWNED_ENTITY) return effect.operation == EffectSpec.Operation.CREATE_ACTOR
				|| effect.operation == EffectSpec.Operation.CREATE_DEVICE
				|| effect.operation == EffectSpec.Operation.CREATE_FIELD;
		if (entityFilter == EntityFilter.OWNED_ACTOR) return effect.operation == EffectSpec.Operation.CREATE_ACTOR;
		if (entityFilter == EntityFilter.OWNED_DEVICE) return effect.operation == EffectSpec.Operation.CREATE_DEVICE;
		return effect.operation == EffectSpec.Operation.CREATE_FIELD;
	}

	public int budgetCost(ClassBuild build) {
		return ClassBudgetPolicy.gameplayComponentCost(this, build);
	}

	public ResourceFlowSpec asResourceFlow() {
		ResourceFlowSpec result = new ResourceFlowSpec(trigger, resourceOperation, amount, interval, delay);
		result.meleeOnly = meleeOnly;
		return result;
	}

	public String displayName(ClassBuild build) {
		if (name != null && !name.trim().isEmpty()) return name.trim();
		return Messages.get(ClassGameplayComponentSpec.class, type.name().toLowerCase() + "_name");
	}

	public String description(ClassBuild build) {
		String resource = resourceName(build, resourceId);
		switch (type) {
			case BASIC_ATTACK: return basicAttack == null ? "" : basicAttack.description();
			case RESOURCE_FLOW:
				if (resourceOperation == ResourceFlowSpec.Operation.CONVERT) {
					return Messages.get(ClassGameplayComponentSpec.class, "resource_convert_desc", amount,
							resource, targetAmount, resourceName(build, targetResourceId));
				}
				return Messages.get(ClassGameplayComponentSpec.class, "resource_flow_desc", resource,
						asResourceFlow().description());
			case ACTIVE_REFILL:
				return amount <= 0
						? Messages.get(ClassGameplayComponentSpec.class, "active_refill_full_desc", actionTime, resource)
						: Messages.get(ClassGameplayComponentSpec.class, "active_refill_amount_desc", actionTime, amount, resource);
			case ENTITY_CAPACITY:
				return Messages.get(ClassGameplayComponentSpec.class, "entity_capacity_desc",
						entityFilterName(), capacity);
			case PERSISTENCE:
				return Messages.get(ClassGameplayComponentSpec.class, "persistence_desc",
						entityFilterName(), lifetime);
			case RECYCLE:
				return Messages.get(ClassGameplayComponentSpec.class, "recycle_desc", entityFilterName(), amount, resource);
			case MODE_ENGINE:
				return Messages.get(ClassGameplayComponentSpec.class, "mode_engine_desc", joinModes());
			case GLOBAL_CONSTRAINT:
				return restriction == null ? "" : restriction.description();
			default:
				return Messages.get(ClassGameplayComponentSpec.class, type.name().toLowerCase() + "_desc");
		}
	}

	private String resourceName(ClassBuild build, String id) {
		ResourceSpec value = build == null ? null : build.resource(id);
		return value == null ? Messages.get(ClassGameplayComponentSpec.class, "unbound_resource") : value.displayName();
	}

	public String entityFilterName() {
		return Messages.get(ClassGameplayComponentSpec.class, "entity_filter_" + entityFilter.name().toLowerCase());
	}

	private String joinModes() {
		StringBuilder out = new StringBuilder();
		for (String mode : modes) {
			if (out.length() > 0) out.append(Messages.get(ClassGameplayComponentSpec.class, "separator"));
			out.append(mode);
		}
		return out.toString();
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("id", id); bundle.put("name", name); bundle.put("type", type);
		bundle.put("basic_attack", basicAttack); bundle.put("resource_id", resourceId);
		bundle.put("target_resource_id", targetResourceId); bundle.put("trigger", trigger);
		bundle.put("resource_operation", resourceOperation); bundle.put("amount", amount);
		bundle.put("target_amount", targetAmount); bundle.put("interval", interval);
		bundle.put("delay", delay); bundle.put("melee_only", meleeOnly);
		bundle.put("action_time", actionTime); bundle.put("entity_filter", entityFilter);
		bundle.put("capacity", capacity); bundle.put("lifetime", lifetime);
		bundle.put("modes", modes.toArray(new String[0])); bundle.put("restriction", restriction);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		id = bundle.getString("id"); name = bundle.getString("name"); type = bundle.getEnum("type", Type.class);
		basicAttack = bundle.contains("basic_attack") ? bundle.getEnum("basic_attack", BasicAttackProfile.class) : BasicAttackProfile.WEAK;
		resourceId = bundle.getString("resource_id"); targetResourceId = bundle.getString("target_resource_id");
		trigger = bundle.contains("trigger") ? bundle.getEnum("trigger", ResourceFlowSpec.Trigger.class) : ResourceFlowSpec.Trigger.TURN;
		resourceOperation = bundle.contains("resource_operation") ? bundle.getEnum("resource_operation", ResourceFlowSpec.Operation.class) : ResourceFlowSpec.Operation.GAIN;
		amount = Math.max(0, bundle.getInt("amount"));
		targetAmount = bundle.contains("target_amount") ? Math.max(1, bundle.getInt("target_amount")) : Math.max(1, amount);
		interval = bundle.contains("interval") ? Math.max(1, bundle.getInt("interval")) : 1;
		delay = Math.max(0, bundle.getInt("delay")); meleeOnly = bundle.getBoolean("melee_only");
		actionTime = bundle.contains("action_time") ? Math.max(1f, bundle.getFloat("action_time")) : 1f;
		entityFilter = bundle.contains("entity_filter") ? bundle.getEnum("entity_filter", EntityFilter.class) : EntityFilter.OWNED_ENTITY;
		capacity = bundle.contains("capacity") ? Math.max(1, bundle.getInt("capacity")) : 1;
		lifetime = bundle.contains("lifetime") ? Math.max(1, bundle.getInt("lifetime")) : 5;
		modes.clear(); String[] storedModes = bundle.getStringArray("modes");
		if (storedModes != null) for (String mode : storedModes) if (mode != null && !mode.trim().isEmpty()) modes.add(mode);
		restriction = bundle.contains("restriction") ? bundle.getEnum("restriction", Restriction.class) : Restriction.NONE;
		if (resourceId == null) resourceId = ""; if (targetResourceId == null) targetResourceId = "";
	}
}
