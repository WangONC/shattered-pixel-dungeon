package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Contract v0.2 descriptor inventory. Only evidence-complete through-P04 variants are exposed. */
public final class GameplayVariantCatalog {
	private static final Map<String, VariantDescriptor> BY_KEY = create();
	private GameplayVariantCatalog() {}

	public static List<VariantDescriptor> all() {
		return Collections.unmodifiableList(new ArrayList<>(BY_KEY.values()));
	}
	public static List<VariantDescriptor> playerExposed() {
		List<VariantDescriptor> result = new ArrayList<>();
		for (VariantDescriptor value : BY_KEY.values()) if (value.playerExposed()) result.add(value);
		return Collections.unmodifiableList(result);
	}
	public static VariantDescriptor require(String family, String variantKey) {
		VariantDescriptor result = BY_KEY.get(family + "." + variantKey);
		if (result == null) throw new IllegalArgumentException("unknown Contract variant " + family + "." + variantKey);
		return result;
	}

	private static Map<String, VariantDescriptor> create() {
		Map<String, VariantDescriptor> result = new LinkedHashMap<>();
		implemented(result, "TRIGGER", "ACTIVE", "skill.trigger.active");
		implemented(result,"TRIGGER","EVENT","component.trigger.event");
		deferred(result, "TRIGGER", "PERIODIC", "THRESHOLD_CROSSED", "ABILITY_OBSERVED");
		implemented(result, "CONDITION_EXPR", "ALL_OF", "skill.condition.all_of");
		unsupported(result, "CONDITION_EXPR", "ANY_OF", "NOT");
		implemented(result, "CONDITION", "ALWAYS", "skill.condition.always");
		implemented(result,"CONDITION","BUILTIN_STAT_COMPARE","skill.condition.builtin_stat_compare");
		implemented(result,"CONDITION","RESOURCE_COMPARE","skill.condition.resource_compare");
		deferred(result, "CONDITION", "TARGET_EXISTS",
				"MARK_COMPARE", "MODE_ACTIVE", "STATUS_PRESENT", "TERRAIN", "DISTANCE_COMPARE",
				"ADJACENT_ACTOR_COUNT", "ENTITY_COUNT", "RELATION_EXISTS", "STATIONARY_TURNS",
				"ABILITY_POOL_COUNT");
		implemented(result, "EFFECT", "DIRECT_DAMAGE", "skill.effect.direct_damage");
		implemented(result,"EFFECT","PERCENT_MAX_HP_DAMAGE","skill.effect.percent_max_hp_damage");
		implemented(result,"EFFECT","MISSING_HP_DAMAGE","skill.effect.missing_hp_damage");
		implemented(result,"EFFECT","EXECUTE","skill.effect.execute");
		implemented(result,"EFFECT","APPLY_STATUS","skill.effect.apply_status");
		implemented(result,"EFFECT","PUSH","skill.effect.push");implemented(result,"EFFECT","PULL","skill.effect.pull");implemented(result,"EFFECT","THROW","skill.effect.throw");implemented(result,"EFFECT","DASH","skill.effect.dash");implemented(result,"EFFECT","TELEPORT","skill.effect.teleport");implemented(result,"EFFECT","SWAP_POSITION","skill.effect.swap_position");
		implemented(result,"EFFECT","HEAL","skill.effect.heal");implemented(result,"EFFECT","BARRIER","skill.effect.barrier");implemented(result,"EFFECT","TEMPORARY_HP","skill.effect.temporary_hp");implemented(result,"EFFECT","MITIGATE","skill.effect.mitigate");implemented(result,"EFFECT","REDIRECT_DAMAGE","skill.effect.redirect_damage");implemented(result,"EFFECT","CLEANSE","skill.effect.cleanse");implemented(result,"EFFECT","RESOURCE_OPERATION","skill.effect.resource_operation");
		deferred(result, "EFFECT",
				"ADD_MARK", "SET_MARK", "CONSUME_MARK", "REMOVE_MARK", "CREATE_ENTITY", "CREATE_TERRAIN",
				"DESTROY_TERRAIN", "CREATE_HAZARD", "CLEAR_HAZARD", "ASSIGN_OWNERSHIP", "BREAK_OWNERSHIP",
				"CREATE_LINK", "BREAK_LINK", "COMMAND_ENTITY", "INHERIT_CAPABILITY", "TRANSFER_RESOURCE",
				"COPY_STATUS", "TRANSFER_MARK", "SWAP_BARRIER", "CAPTURE_SNAPSHOT", "RESTORE_SNAPSHOT",
				"CAPTURE_ABILITY", "GRANT_LEARNED_ABILITY", "EXTRACT_PROPERTY", "TRANSFER_PROPERTY",
				"MODE_SHIFT", "CAPABILITY_OVERRIDE", "BEHAVIOR_OVERRIDE", "DECOMPOSE", "SYNTHESIZE", "IMBUE");
		implemented(result, "EFFECT_CHAIN", "PRIMARY", "skill.chain.primary");
		implemented(result, "SECONDARY_ACTIVATION", "IMMEDIATE_ON_PRIMARY_SUCCESS", "skill.chain.immediate_secondary");
		unsupported(result, "SECONDARY_ACTIVATION", "DELAY_AFTER_PRIMARY_SUCCESS", "ON_NEXT_ACTION_AFTER_PRIMARY_SUCCESS");
		implemented(result, "DELIVERY", "DIRECT", "skill.delivery.direct");
		implemented(result,"DELIVERY","SELF","skill.delivery.self");implemented(result,"DELIVERY","CONTACT","skill.delivery.contact");implemented(result,"DELIVERY","PROJECTILE","skill.delivery.projectile");implemented(result,"DELIVERY","TRACE","skill.delivery.trace");implemented(result,"DELIVERY","GROUND","skill.delivery.ground");
		deferred(result, "DELIVERY",
				"PERSISTENT_CARRIER", "ACTION_ATTACHMENT");
		implemented(result, "SELECTOR", "SELECTED_ACTOR", "skill.target.selector.selected_actor");
		implemented(result,"SELECTOR","SELF","skill.target.selector.self");implemented(result,"SELECTOR","SELECTED_CELL","skill.target.selector.selected_cell");
		deferred(result, "SELECTOR", "EVENT_SOURCE", "EVENT_TARGET", "NEAREST_ACTOR",
				"RANDOM_ACTOR", "ALL_MATCHING_ACTORS", "OWNED_ENTITY");
		implemented(result, "COVERAGE", "SINGLE", "skill.target.coverage.single");
		implemented(result,"COVERAGE","ADJACENT","skill.target.coverage.adjacent");implemented(result,"COVERAGE","RADIUS","skill.target.coverage.radius");implemented(result,"COVERAGE","LINE","skill.target.coverage.line");
		deferred(result, "COVERAGE", "CONE", "RING", "CHAIN");
		implemented(result, "FILTER", "RELATION_ENEMY_EXCLUDE_SELF", "skill.target.filter.enemy");
		implemented(result,"FILTER","RELATION_ALLY_EXCLUDE_SELF","skill.target.filter.ally");implemented(result,"FILTER","RELATION_ALLY_INCLUDE_SELF","skill.target.filter.ally_or_self");implemented(result,"FILTER","ANY_ACTOR","skill.target.filter.any_actor");implemented(result,"FILTER","SELF","skill.target.filter.self");
		deferred(result, "FILTER", "OWNED_BY", "ENTITY_TYPE", "ENTITY_BLUEPRINT",
				"HAS_MARK", "HAS_STATUS", "HP_PERCENT", "HAS_CAPABILITY");
		unsupported(result, "FILTER_EXPR", "ANY_OF", "NOT");
		deferred(result, "FILTER_EXPR", "ALL_OF");
		implemented(result,"MODIFIER","REPEAT","skill.modifier.repeat");implemented(result,"MODIFIER","INTENSITY","skill.modifier.intensity");implemented(result,"MODIFIER","EXTEND_DURATION","skill.modifier.extend_duration");implemented(result,"MODIFIER","PIERCE","skill.modifier.pierce");implemented(result,"MODIFIER","BOUNCE","skill.modifier.bounce");deferred(result, "MODIFIER", "DELAY", "ECHO");
		implemented(result, "COST", "NO_COST", "skill.cost.none");
		implemented(result,"COST","RESOURCE","skill.cost.resource");implemented(result,"COST","HP","skill.cost.hp");implemented(result,"COST","ACTION_TIME","skill.cost.action_time");implemented(result,"COST","COOLDOWN","skill.cost.cooldown");implemented(result,"COST","ITEM","skill.cost.item");
		deferred(result, "COST", "MARK", "ENTITY",
				"ABILITY_CHARGE", "PROPERTY");
		for(String key:new String[]{"GAIN","DRAIN","SET","CLEAR","CONVERT","RESERVE","SUPPRESS"})implemented(result,"RESOURCE_OPERATION",key,"resource.operation."+key.toLowerCase());
		implemented(result,"CLASS_COMPONENT","BASIC_ATTACK","component.basic_attack");implemented(result,"CLASS_COMPONENT","RESOURCE_FLOW","component.resource_flow");implemented(result,"CLASS_COMPONENT","ACTIVE_RESOURCE_OPERATION","component.active_resource_operation");implemented(result,"CLASS_OPERATION","RESOURCE_OPERATION","class_operation.resource");
		deferred(result, "SKILL_CONSTRAINT", "LIMITED_USES_PER_FLOOR", "REQUIRES_STATIONARY_TURNS",
				"ONLY_AFTER_EVENT", "SKILL_COOLDOWN_FLOOR", "EXCLUSIVE_MODE");
		return Collections.unmodifiableMap(result);
	}
	private static void implemented(Map<String, VariantDescriptor> out, String family, String key, String priceKey) {
		add(out, new VariantDescriptor(family, key, ImplementationState.IMPLEMENTED, true, priceKey));
	}
	private static void deferred(Map<String, VariantDescriptor> out, String family, String... keys) {
		for (String key : keys) add(out, new VariantDescriptor(family, key, ImplementationState.DEFERRED, false, ""));
	}
	private static void unsupported(Map<String, VariantDescriptor> out, String family, String... keys) {
		for (String key : keys) add(out, new VariantDescriptor(family, key, ImplementationState.UNSUPPORTED, false, ""));
	}
	private static void add(Map<String, VariantDescriptor> out, VariantDescriptor value) {
		if (out.put(value.qualifiedKey(), value) != null) throw new IllegalStateException("duplicate variant " + value.qualifiedKey());
	}
}
