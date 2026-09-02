package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Reproducible acceptance builds exposed by DEV RULE LAB and tests. */
public final class RuleTestBuilds {
	public enum Preset {
		A_MOMENTUM_SKIRMISHER,
		B_FOCUS_CONTROLLER,
		C_AFFLICTION_ALCHEMIST,
		D_BLOOD_CASTER,
		E_STATUS_COMBO,
		F_WATER_SHAPER
	}

	private RuleTestBuilds() {}

	public static String displayName(Preset preset) {
		return Messages.get(RuleTestBuilds.class, preset.name().toLowerCase());
	}

	public static CustomClassConfig config(Preset preset) {
		CustomClassConfig c = new CustomClassConfig();
		// Keep blueprint construction headless-testable; the DEV UI applies the localized display name.
		c.name = preset.name();
		switch (preset) {
			case A_MOMENTUM_SKIRMISHER:
				c.resource = ResourceEngine.MOMENTUM;
				c.law = ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD;
				c.activeTarget = RuleTarget.Type.SELECTED_CELL;
				c.activeEffect = RuleEffect.Type.TELEPORT;
				c.reactionTrigger = RuleEvent.ON_HIT;
				c.reactionCondition = RuleCondition.Type.TARGET_HAS_POISON;
				c.reactionTarget = RuleTarget.Type.HIT_TARGET;
				c.reactionEffect = RuleEffect.Type.PUSH;
				c.restriction = Restriction.WAIT_CLEARS_RESOURCE;
				break;
			case B_FOCUS_CONTROLLER:
				c.resource = ResourceEngine.FOCUS;
				c.law = ClassLaw.KILL_ACCELERATES_RULES;
				c.activeTarget = RuleTarget.Type.SELECTED_TARGET;
				c.activeEffect = RuleEffect.Type.PULL;
				c.activeModifier = RuleModifier.Type.AREA;
				c.reactionTrigger = RuleEvent.ON_WAIT;
				c.reactionCondition = RuleCondition.Type.ALWAYS;
				c.reactionTarget = RuleTarget.Type.SELF;
				c.reactionEffect = RuleEffect.Type.SHIELD;
				c.restriction = Restriction.ACTIVE_COSTS_HP;
				break;
			case C_AFFLICTION_ALCHEMIST:
				c.resource = ResourceEngine.AFFLICTION;
				c.law = ClassLaw.STATUS_ABSORPTION;
				c.activeTarget = RuleTarget.Type.SELF;
				c.activeEffect = RuleEffect.Type.CLEANSE;
				c.reactionTrigger = RuleEvent.ON_STATUS_APPLIED;
				c.reactionCondition = RuleCondition.Type.ALWAYS;
				c.reactionTarget = RuleTarget.Type.SELF;
				c.reactionEffect = RuleEffect.Type.HASTE;
				c.restriction = Restriction.WEAK_HEALING;
				break;
			case D_BLOOD_CASTER:
				c.resource = ResourceEngine.BLOOD;
				c.law = ClassLaw.HEALING_TO_SHIELD;
				c.activeTarget = RuleTarget.Type.SELECTED_CELL;
				c.activeEffect = RuleEffect.Type.FIRE;
				c.reactionTrigger = RuleEvent.ON_KILL;
				c.reactionCondition = RuleCondition.Type.ALWAYS;
				c.reactionTarget = RuleTarget.Type.SELF;
				c.reactionEffect = RuleEffect.Type.HEAL;
				c.restriction = Restriction.NO_ORDINARY_WEAPONS;
				break;
			case E_STATUS_COMBO:
				c.resource = ResourceEngine.MANA;
				c.law = ClassLaw.KILL_ACCELERATES_RULES;
				c.activeTarget = RuleTarget.Type.SELECTED_TARGET;
				c.activeEffect = RuleEffect.Type.POISON;
				c.reactionTrigger = RuleEvent.ON_HIT;
				c.reactionCondition = RuleCondition.Type.TARGET_HAS_POISON;
				c.reactionTarget = RuleTarget.Type.HIT_TARGET;
				c.reactionEffect = RuleEffect.Type.PULL;
				c.restriction = Restriction.WEAK_HEALING;
				break;
			case F_WATER_SHAPER:
				c.resource = ResourceEngine.FOCUS;
				c.law = ClassLaw.WATER_AFFINITY;
				c.activeTarget = RuleTarget.Type.SELECTED_CELL;
				c.activeEffect = RuleEffect.Type.CREATE_WATER;
				c.activeModifier = RuleModifier.Type.AREA;
				c.reactionTrigger = RuleEvent.ON_ENTER_TILE;
				c.reactionCondition = RuleCondition.Type.SELF_IN_WATER;
				c.reactionTarget = RuleTarget.Type.SELF;
				c.reactionEffect = RuleEffect.Type.SHIELD;
				c.restriction = Restriction.NO_ORDINARY_WEAPONS;
				break;
		}
		return c;
	}
}
