package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * Player-selectable, build-wide uses of existing Rule Runtime primitives.  These entries do not
 * introduce a second dispatcher: RuleRuntime translates them into Marks, Bridges, Delay payloads,
 * ordinary Buffs, or small changes to the existing cost/effect path.
 */
public enum CoreRuleVocabulary {
	NONE(0),
	ACCUMULATION(2),
	OVERDRAW(2), // MIGRATION_ONLY: promoted to RESOURCE_OVERDRAFT_USES_HP law.
	OVERFLOW(3, true, false),
	COMPENSATION(2),
	PHASE_SHIFT(3),
	ECHO(4),
	HUNT_MARK(2),
	PROPAGATION(2),
	INERTIA_BRIDGE(1), // MIGRATION_ONLY: promoted to FORCED_MOVEMENT_COUNTS_AS_MOVE law.
	TRANSLOCATION_BRIDGE(1), // MIGRATION_ONLY: promoted to TRANSLOCATION_COUNTS_AS_ENTER_TILE law.
	TRANSLOCATION_BACKLASH(1),
	SHIELD_BACKLASH(1),

	STATUS_FEEDBACK(3, true, false),
	WATER_FLOW(3, true, false),
	// Reclassification of the former 2-point KILL_ACCELERATES_RULES law.  The scope and runtime
	// semantics did not change, so moving it to Trait must not silently increase class cost.
	KILL_TEMPO(2),
	KINETIC_MARK(3),
	MOBILE_CHARGE(3, true, false),
	TEMP_HP_PAYMENT(4),
	PIERCING_MARK(3),
	OWNED_RESOURCE_FEEDBACK(4, true, false),
	CARRIER_RESOURCE_FEEDBACK(4, true, false),
	HAZARD_FEEDBACK(3, true, false),
	MODE_GUARD(3, false, true),
	TRANSFER_FEEDBACK(3, true, false),
	STATUS_CHAIN(3);

	public final int capacityCost;
	private final boolean requiresResource;
	private final boolean requiresMode;

	CoreRuleVocabulary(int capacityCost) {
		this(capacityCost, false, false);
	}

	CoreRuleVocabulary(int capacityCost, boolean requiresResource, boolean requiresMode) {
		this.capacityCost = capacityCost;
		this.requiresResource = requiresResource;
		this.requiresMode = requiresMode;
	}

	public String displayName() {
		return Messages.get(CoreRuleVocabulary.class, name().toLowerCase());
	}

	public String description() {
		return Messages.get(CoreRuleVocabulary.class, name().toLowerCase() + "_desc");
	}

	public String summary(String binding) {
		return Messages.get(CoreRuleVocabulary.class, name().toLowerCase() + "_summary", binding == null ? "" : binding);
	}

	public String detail(String binding) {
		return Messages.get(CoreRuleVocabulary.class, name().toLowerCase() + "_detail", binding == null ? "" : binding);
	}

	public boolean requiresResource() { return requiresResource; }
	public boolean requiresMode() { return requiresMode; }

	public static boolean supportsAccumulation(RuleEvent event) {
		return event == RuleEvent.ON_MOVE || event == RuleEvent.ON_HIT
				|| event == RuleEvent.ON_DAMAGED || event == RuleEvent.ON_WAIT;
	}

	public static boolean validSelection(CustomClassConfig config) {
		if (config == null || config.vocabulary1 == null || config.vocabulary2 == null) return false;
		if (config.vocabulary1 != NONE && config.vocabulary1 == config.vocabulary2) return false;
		return available(config.vocabulary1, config, config.vocabulary2)
				&& available(config.vocabulary2, config, config.vocabulary1);
	}

	/** Used by the Builder to hide known dead choices after both Techniques are known. */
	public static boolean available(CoreRuleVocabulary value, CustomClassConfig config,
			CoreRuleVocabulary companion) {
		if (value == null || value == NONE) return true;
		if (value == PROPAGATION) return companion == HUNT_MARK;
		if (value == ACCUMULATION) return supportsAccumulation(config.reactionTrigger);
		if (value == OVERDRAW) return config.resource != ResourceEngine.BLOOD;
		if (value == OVERFLOW) return config.law != ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD;
		if (value == INERTIA_BRIDGE) return hasEffect(config, RuleEffect.Type.PUSH, RuleEffect.Type.PULL);
		if (value == TRANSLOCATION_BRIDGE || value == TRANSLOCATION_BACKLASH) {
			return hasEffect(config, RuleEffect.Type.TELEPORT, RuleEffect.Type.SWAP_POSITION);
		}
		if (value == SHIELD_BACKLASH) {
			return hasEffect(config, RuleEffect.Type.SHIELD)
					|| config.law == ClassLaw.HEALING_TO_SHIELD
					|| config.law == ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD
					|| companion == OVERFLOW;
		}
		return true;
	}

	private static boolean hasEffect(CustomClassConfig config, RuleEffect.Type... types) {
		if (config == null) return false;
		for (RuleEffect.Type type : types) {
			if (config.activeEffect == type || config.reactionEffect == type) return true;
		}
		return false;
	}
}
