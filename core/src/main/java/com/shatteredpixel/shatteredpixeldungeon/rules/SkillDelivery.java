package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** How a Skill reaches its target. Area shape deliberately belongs to TargetingSpec. */
public enum SkillDelivery {
	SELF,
	CONTACT_ATTACK,
	DIRECT_TARGET,
	PROJECTILE,
	TRACE_BEAM,
	GROUND_PLACEMENT,
	PERSISTENT_CARRIER,
	ACTION_ATTACHMENT;

	public String displayName() {
		return Messages.get(SkillDelivery.class, name().toLowerCase() + "_name");
	}

	public String description() {
		return Messages.get(SkillDelivery.class, name().toLowerCase() + "_desc");
	}

	/** Every frozen delivery now has a concrete adapter in SkillTargetResolver/RuleRuntime. */
	public boolean implemented() {
		return true;
	}

	public int powerCost() {
		return this == PERSISTENT_CARRIER ? 2 : this == PROJECTILE || this == TRACE_BEAM ? 2 : 0;
	}
}
