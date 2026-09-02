package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.Restriction;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEffect;

import java.util.EnumSet;

/** Metadata for only the V0.2 vocabulary which actually exists. */
public final class RuleSemanticMetadata {
	public enum SourceReliability { RELIABLE, EXTERNAL_INPUT, HP_POOL }

	private RuleSemanticMetadata() {}

	public static SourceReliability sourceReliability(ResourceEngine engine) {
		if (engine == ResourceEngine.AFFLICTION) return SourceReliability.EXTERNAL_INPUT;
		if (engine == ResourceEngine.BLOOD) return SourceReliability.HP_POOL;
		return SourceReliability.RELIABLE;
	}

	public static EnumSet<RuleQaCapability> capabilities(ResourceEngine engine) {
		EnumSet<RuleQaCapability> result = EnumSet.of(RuleQaCapability.RESOURCE_SOURCE);
		if (engine == ResourceEngine.BLOOD) result.add(RuleQaCapability.SURVIVAL);
		return result;
	}

	public static EnumSet<RuleQaCapability> capabilities(RuleEffect.Type effect) {
		EnumSet<RuleQaCapability> result = EnumSet.noneOf(RuleQaCapability.class);
		switch (effect) {
			case FIRE:
			case POISON:
			case BLEED:
			case CREATE_GAS:
				result.add(RuleQaCapability.DOT_DAMAGE);
				result.add(RuleQaCapability.STATUS_APPLICATION);
				break;
			case PUSH:
			case PULL:
			case SWAP_POSITION:
				result.add(RuleQaCapability.FORCED_MOVEMENT);
				result.add(RuleQaCapability.CONTROL);
				break;
			case TELEPORT:
			case HASTE:
				result.add(RuleQaCapability.MOBILITY);
				break;
			case SLOW:
				result.add(RuleQaCapability.CONTROL);
				result.add(RuleQaCapability.STATUS_APPLICATION);
				break;
			case HEAL:
				result.add(RuleQaCapability.HEALING);
				result.add(RuleQaCapability.SURVIVAL);
				break;
			case SHIELD:
				result.add(RuleQaCapability.SHIELDING);
				result.add(RuleQaCapability.SURVIVAL);
				break;
			case CLEANSE:
				result.add(RuleQaCapability.STATUS_REMOVAL);
				result.add(RuleQaCapability.SURVIVAL);
				break;
			case CREATE_WATER:
				result.add(RuleQaCapability.ENVIRONMENT_CONTROL);
				break;
		}
		return result;
	}

	public static String producedState(RuleEffect.Type effect) {
		switch (effect) {
			case POISON:
			case CREATE_GAS: return "POISON";
			case FIRE: return "BURNING";
			case CREATE_WATER: return "WATER";
			default: return null;
		}
	}

	public static String requiredState(RuleCondition.Type condition) {
		switch (condition) {
			case TARGET_HAS_POISON: return "POISON";
			case TARGET_IS_BURNING: return "BURNING";
			case SELF_IN_WATER: return "WATER";
			case RESOURCE_AT_LEAST: return "RESOURCE";
			default: return null;
		}
	}

	public static EnumSet<RuleQaCapability> capabilities(ClassLaw law) {
		EnumSet<RuleQaCapability> result = EnumSet.noneOf(RuleQaCapability.class);
		if (law == ClassLaw.HEALING_TO_SHIELD || law == ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD) {
			result.add(RuleQaCapability.SHIELDING);
			result.add(RuleQaCapability.SURVIVAL);
		}
		if (law == ClassLaw.STATUS_ABSORPTION) result.add(RuleQaCapability.RESOURCE_SOURCE);
		return result;
	}

	public static EnumSet<RuleQaCapability> capabilities(CoreRuleVocabulary vocabulary) {
		EnumSet<RuleQaCapability> result = EnumSet.noneOf(RuleQaCapability.class);
		if (vocabulary == CoreRuleVocabulary.OVERFLOW
				|| vocabulary == CoreRuleVocabulary.COMPENSATION) {
			result.add(RuleQaCapability.SHIELDING);
			result.add(RuleQaCapability.SURVIVAL);
		}
		if (vocabulary == CoreRuleVocabulary.INERTIA_BRIDGE
				|| vocabulary == CoreRuleVocabulary.TRANSLOCATION_BRIDGE) {
			result.add(RuleQaCapability.MOBILITY);
		}
		if (vocabulary == CoreRuleVocabulary.HUNT_MARK) result.add(RuleQaCapability.CONTROL);
		return result;
	}

	public static EnumSet<RuleQaCapability> removedCapabilities(Restriction restriction) {
		EnumSet<RuleQaCapability> result = EnumSet.noneOf(RuleQaCapability.class);
		if (restriction == Restriction.NO_ORDINARY_WEAPONS) result.add(RuleQaCapability.DIRECT_DAMAGE);
		if (restriction == Restriction.NO_TRADITIONAL_HEALING) result.add(RuleQaCapability.HEALING);
		return result;
	}
}
