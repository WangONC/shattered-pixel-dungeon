package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Explicit skill limitation. Its nominal rebate is discounted against the whole build. */
public class SkillConstraint implements Bundlable {
	public enum Type { NONE, TARGET, SELF_STATE, POSITION, TIMING, COMMITMENT, FREQUENCY }
	public enum Variant { NONE, TARGET_MARKED, SELF_LOW_HP, SELF_IN_WATER, COOLDOWN, HP_COMMITMENT, LIMITED_USE }

	public Type type = Type.NONE;
	public Variant variant = Variant.NONE;
	public int parameter;

	public SkillConstraint() {}
	public SkillConstraint(Variant variant, int parameter) {
		this.variant = variant;
		this.parameter = parameter;
		switch (variant) {
			case TARGET_MARKED: type = Type.TARGET; break;
			case SELF_LOW_HP: type = Type.SELF_STATE; break;
			case SELF_IN_WATER: type = Type.POSITION; break;
			case COOLDOWN: type = Type.TIMING; break;
			case HP_COMMITMENT: type = Type.COMMITMENT; break;
			case LIMITED_USE: type = Type.FREQUENCY; break;
			default: type = Type.NONE;
		}
	}

	public SkillConstraint copy() { return new SkillConstraint(variant, parameter); }

	public boolean implemented() { return true; }

	public int nominalRebate() {
		switch (variant) {
			case TARGET_MARKED:
			case SELF_IN_WATER:
			case COOLDOWN: return 2;
			case SELF_LOW_HP:
			case HP_COMMITMENT: return 1;
			case LIMITED_USE: return 3;
			default: return 0;
		}
	}

	public int effectiveRebate(ClassBuild build) {
		return effectiveRebate(build, null);
	}

	/**
	 * Effective rebate is build-aware and, for cooldowns, cadence-aware. A cooldown shorter than
	 * the effect's natural replacement/need interval is presentation text, not a real restriction.
	 */
	public int effectiveRebate(ClassBuild build, SkillSpec skill) {
		int result = nominalRebate();
		if (variant == Variant.TARGET_MARKED && (LawTraitRegistry.hasTrait(build, CoreRuleVocabulary.HUNT_MARK)
				|| produces(build, EffectSpec.Operation.MARK_APPLY, EffectSpec.Operation.MARK_STACK))) result /= 2;
		if (variant == Variant.SELF_IN_WATER && (build.producesEffect(RuleEffect.Type.CREATE_WATER)
				|| produces(build, EffectSpec.Operation.WORLD_WATER))) result /= 2;
		if (variant == Variant.COOLDOWN && skill != null) {
			int cooldown = Math.max(1, parameter);
			int naturalInterval = naturalUseInterval(skill);
			if (cooldown <= naturalInterval) result = 0;
			else if (cooldown <= naturalInterval + 2) result = Math.min(result, 1);
		}
		return result;
	}

	private static int naturalUseInterval(SkillSpec skill) {
		if (skill == null || skill.primary == null) return 1;
		if (skill.primary.family == EffectFamily.CREATE_ENTITY) {
			return Math.max(3, skill.primary.lifetime);
		}
		if (skill.primary.family == EffectFamily.RECOVERY_DEFENSE) return 4;
		if (skill.primary.family == EffectFamily.TRANSFORM) return 4;
		if (skill.primary.family == EffectFamily.WORLD_TERRAIN) return 2;
		return 1;
	}

	private static boolean produces(ClassBuild build, EffectSpec.Operation... operations) {
		for (SkillSpec skill : build.skills) {
			for (EffectSpec.Operation operation : operations) {
				if (skill.primary != null && skill.primary.operation == operation
						|| skill.secondary != null && skill.secondary.operation == operation) return true;
			}
		}
		return false;
	}

	public boolean automaticallySatisfied(ClassBuild build) {
		return nominalRebate() > 0 && effectiveRebate(build) < nominalRebate();
	}

	public boolean automaticallySatisfied(ClassBuild build, SkillSpec skill) {
		return nominalRebate() > 0 && effectiveRebate(build, skill) < nominalRebate();
	}

	public void applyTo(RuleDefinition rule) {
		switch (variant) {
			case TARGET_MARKED:
				rule.conditions.add(new RuleMarkCondition(RuleMark.Type.HUNTED, Math.max(1, parameter)));
				break;
			case SELF_LOW_HP:
				rule.conditions.add(new RuleCondition(RuleCondition.Type.SELF_HP_BELOW,
						parameter <= 0 ? 30 : parameter));
				break;
			case SELF_IN_WATER:
				rule.conditions.add(new RuleCondition(RuleCondition.Type.SELF_IN_WATER));
				break;
			case COOLDOWN:
				rule.modifier.cooldown = Math.max(1, parameter);
				break;
			case LIMITED_USE:
				rule.setUseLimit(Math.max(1, parameter));
				break;
			default:
		}
	}

	public String displayName() { return Messages.get(SkillConstraint.class, variant.name().toLowerCase() + "_name"); }
	public String description() { return Messages.get(SkillConstraint.class, variant.name().toLowerCase() + "_desc", Math.max(1, parameter)); }

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("type", type);
		bundle.put("variant", variant);
		bundle.put("parameter", parameter);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		type = bundle.getEnum("type", Type.class);
		variant = bundle.getEnum("variant", Variant.class);
		parameter = bundle.getInt("parameter");
	}
}
