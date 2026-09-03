package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Contract v0.2 descriptor inventory. Only the evidence-complete P03 slice is exposed. */
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
		deferred(result, "TRIGGER", "EVENT", "PERIODIC", "THRESHOLD_CROSSED", "ABILITY_OBSERVED");
		implemented(result, "CONDITION_EXPR", "ALL_OF", "skill.condition.all_of");
		unsupported(result, "CONDITION_EXPR", "ANY_OF", "NOT");
		implemented(result, "CONDITION", "ALWAYS", "skill.condition.always");
		deferred(result, "CONDITION", "TARGET_EXISTS", "BUILTIN_STAT_COMPARE", "RESOURCE_COMPARE",
				"MARK_COMPARE", "MODE_ACTIVE", "STATUS_PRESENT", "TERRAIN", "DISTANCE_COMPARE",
				"ADJACENT_ACTOR_COUNT", "ENTITY_COUNT", "RELATION_EXISTS", "STATIONARY_TURNS",
				"ABILITY_POOL_COUNT");
		implemented(result, "EFFECT", "DIRECT_DAMAGE", "skill.effect.direct_damage");
		deferred(result, "EFFECT", "PERCENT_MAX_HP_DAMAGE", "MISSING_HP_DAMAGE", "EXECUTE", "APPLY_STATUS",
				"PUSH", "PULL", "THROW", "DASH", "TELEPORT", "SWAP_POSITION", "HEAL", "BARRIER",
				"TEMPORARY_HP", "MITIGATE", "REDIRECT_DAMAGE", "CLEANSE", "RESOURCE_OPERATION",
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
		deferred(result, "DELIVERY", "SELF", "CONTACT", "PROJECTILE", "TRACE", "GROUND",
				"PERSISTENT_CARRIER", "ACTION_ATTACHMENT");
		implemented(result, "SELECTOR", "SELECTED_ACTOR", "skill.target.selector.selected_actor");
		deferred(result, "SELECTOR", "SELF", "EVENT_SOURCE", "EVENT_TARGET", "SELECTED_CELL", "NEAREST_ACTOR",
				"RANDOM_ACTOR", "ALL_MATCHING_ACTORS", "OWNED_ENTITY");
		implemented(result, "COVERAGE", "SINGLE", "skill.target.coverage.single");
		deferred(result, "COVERAGE", "ADJACENT", "RADIUS", "LINE", "CONE", "RING", "CHAIN");
		implemented(result, "FILTER", "RELATION_ENEMY_EXCLUDE_SELF", "skill.target.filter.enemy");
		deferred(result, "FILTER", "ANY_ACTOR", "SELF", "OWNED_BY", "ENTITY_TYPE", "ENTITY_BLUEPRINT",
				"HAS_MARK", "HAS_STATUS", "HP_PERCENT", "HAS_CAPABILITY");
		unsupported(result, "FILTER_EXPR", "ANY_OF", "NOT");
		deferred(result, "FILTER_EXPR", "ALL_OF");
		deferred(result, "MODIFIER", "REPEAT", "INTENSITY", "EXTEND_DURATION", "PIERCE", "BOUNCE", "DELAY", "ECHO");
		implemented(result, "COST", "NO_COST", "skill.cost.none");
		deferred(result, "COST", "RESOURCE", "HP", "ACTION_TIME", "COOLDOWN", "ITEM", "MARK", "ENTITY",
				"ABILITY_CHARGE", "PROPERTY");
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
