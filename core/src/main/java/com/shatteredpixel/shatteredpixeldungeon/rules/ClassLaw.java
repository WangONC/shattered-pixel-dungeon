/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** A persistent class-wide rule, deliberately separate from event techniques. */
public enum ClassLaw {
	HEALING_TO_SHIELD(5),
	RESOURCE_OVERFLOW_TO_SHIELD(2), // MIGRATION_ONLY: now the parameterized Overflow Trait.
	STATUS_ABSORPTION(2), // MIGRATION_ONLY: now the parameterized Status Feedback Trait.
	WATER_AFFINITY(2), // MIGRATION_ONLY: now the parameterized Water Flow Trait.
	KILL_ACCELERATES_RULES(2), // MIGRATION_ONLY: now the Kill Tempo Trait.
	FORCED_MOVEMENT_COUNTS_AS_MOVE(4),
	TRANSLOCATION_COUNTS_AS_ENTER_TILE(3),
	RESOURCE_OVERDRAFT_USES_HP(4),
	OWNED_ACTIONS_COUNT_AS_YOURS(5);

	public final int capacityCost;

	ClassLaw(int capacityCost) {
		this.capacityCost = capacityCost;
	}

	public String displayName() {
		return Messages.get(ClassLaw.class, name().toLowerCase() + "_name");
	}

	public String description() {
		return Messages.get(ClassLaw.class, name().toLowerCase() + "_desc");
	}

	public String summary() {
		return Messages.get(ClassLaw.class, name().toLowerCase() + "_summary");
	}

	public String detail() {
		return Messages.get(ClassLaw.class, name().toLowerCase() + "_detail");
	}
}
