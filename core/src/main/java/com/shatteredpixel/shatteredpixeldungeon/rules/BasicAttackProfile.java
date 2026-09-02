package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Player-facing class-level policy for the hero's ordinary click-to-attack action only. */
public enum BasicAttackProfile {
	WEAK,
	FULL;

	/** A weak attack is a safe fallback, not a second full victory path. */
	public float damageMultiplier() {
		return this == WEAK ? 0.55f : 1f;
	}

	public String displayName() {
		return Messages.get(BasicAttackProfile.class, name().toLowerCase() + "_name");
	}

	public String description() {
		return Messages.get(BasicAttackProfile.class, name().toLowerCase() + "_desc");
	}

	/**
	 * FULL is free when it is the build's only dependable enemy-resolution path.  Its cost rises
	 * when it becomes a complete, resource-free fallback beside ranged, area, or owned-entity
	 * offense.  This deliberately evaluates components, never an archetype name.
	 */
	public int budgetCost(ClassBuild build) {
		if (this == WEAK || build == null) return 0;
		boolean independentDamagePath = false;
		boolean rangedPath = false;
		boolean areaPath = false;
		boolean entityPath = false;
		boolean sustainable = false;
		for (SkillSpec skill : build.skills) {
			if (skill == null || skill.primary == null) continue;
			// Reactions and contact-attached techniques do not replace the ordinary attack: they
			// either require that attack or require the hero to be attacked first. Charging FULL
			// for those would make martial builds pay for their only dependable victory path.
			boolean activeIndependent = skill.activation == RuleEvent.ACTIVE
					&& skill.delivery != SkillDelivery.CONTACT_ATTACK;
			EffectSpec[] effects = {skill.primary, skill.secondary};
			for (EffectSpec effect : effects) {
				if (effect == null) continue;
				boolean resolvesEnemy = effect.family == EffectFamily.DAMAGE
						|| effect.operation == EffectSpec.Operation.STATUS_POISON
						|| effect.operation == EffectSpec.Operation.STATUS_BURNING
						|| effect.operation == EffectSpec.Operation.STATUS_BLEEDING;
				// Execute is a finisher, not a standalone attack loop. It only contributes when the
				// same build already contains another independent way to reach its threshold.
				if (activeIndependent && resolvesEnemy
						&& effect.operation != EffectSpec.Operation.DAMAGE_EXECUTE) independentDamagePath = true;
				if (effect.operation == EffectSpec.Operation.CREATE_ACTOR
						|| effect.operation == EffectSpec.Operation.CREATE_DEVICE) {
					entityPath = true;
					independentDamagePath = true;
				}
			}
			if (activeIndependent && (skill.delivery == SkillDelivery.PROJECTILE
					|| skill.delivery == SkillDelivery.TRACE_BEAM
					|| skill.delivery == SkillDelivery.DIRECT_TARGET)) rangedPath = true;
			if (activeIndependent && skill.targeting != null
					&& skill.targeting.coverage != TargetingSpec.Coverage.SINGLE) areaPath = true;
			if (activeIndependent && (skill.cost == null || skill.cost.type == RuleCost.Type.NONE
					|| skill.cost.type == RuleCost.Type.COOLDOWN || skill.cost.type == RuleCost.Type.ACTION)) sustainable = true;
			if (activeIndependent && skill.cost != null && skill.cost.type == RuleCost.Type.RESOURCE) {
				ResourceSpec pool = build.resource(skill.cost.resourceId);
				if (pool != null) for (ClassGameplayComponentSpec component : build.gameplayComponents) {
					if (component != null && skill.cost.resourceId.equals(component.resourceId)
							&& (component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL
							|| component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW
							&& component.resourceOperation == ResourceFlowSpec.Operation.GAIN)) sustainable = true;
				}
			}
		}
		if (!independentDamagePath && !entityPath) return 0;
		int result = 2;
		if (rangedPath) result++;
		if (areaPath) result++;
		if (entityPath) result += 2;
		if (sustainable) result++;
		return Math.min(7, result);
	}
}
