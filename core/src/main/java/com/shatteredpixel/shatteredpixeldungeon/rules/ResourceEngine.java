package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public enum ResourceEngine {
	MANA(10, 5),
	RAGE(10, 0),
	BLOOD(0, 0),
	MOMENTUM(12, 0),
	FOCUS(10, 0),
	AFFLICTION(10, 0),
	/** Generic finite pool with no automatic gain; skills must explicitly refill it. */
	MANUAL(6, 0);

	public final int max;
	public final int initial;

	ResourceEngine(int max, int initial) {
		this.max = max;
		this.initial = initial;
	}

	public String displayName() {
		return Messages.get(ResourceEngine.class, name().toLowerCase());
	}

	public String description() {
		return Messages.get(ResourceEngine.class, name().toLowerCase() + "_desc");
	}
}
