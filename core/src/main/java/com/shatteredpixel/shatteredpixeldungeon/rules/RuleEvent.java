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

/** Stable gameplay events exposed by the rule integration layer. */
public enum RuleEvent {
	ON_TURN_START,
	ON_MOVE,
	ON_ATTACK,
	ON_HIT,
	ON_DAMAGED,
	ON_KILL,
	ON_ITEM_USE,
	ON_ENTER_TILE,
	ON_STATUS_APPLIED,
	ON_LOW_HP,
	ON_WAIT,
	ACTIVE
}
