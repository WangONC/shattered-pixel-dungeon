package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public enum Restriction {
	NONE(0),
	NO_ORDINARY_WEAPONS(3),
	WEAK_HEALING(1),
	FRAIL(2),
	NO_TRADITIONAL_HEALING(3),
	WAIT_CLEARS_RESOURCE(2),
	ACTIVE_COSTS_HP(2);

	public final int capacityBonus;

	Restriction(int capacityBonus) {
		this.capacityBonus = capacityBonus;
	}

	public String displayName() {
		return Messages.get(Restriction.class, name().toLowerCase() + "_name");
	}

	public String description() {
		return Messages.get(Restriction.class, name().toLowerCase() + "_desc");
	}
}
