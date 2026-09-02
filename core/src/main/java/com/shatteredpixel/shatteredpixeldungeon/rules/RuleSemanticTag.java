package com.shatteredpixel.shatteredpixeldungeon.rules;

/**
 * Small, query-only semantic vocabulary for implemented rule behavior. Tags are derived from
 * modules and are not saved player choices.
 */
public enum RuleSemanticTag {
	FORCED_MOVEMENT,
	MOVEMENT,
	TRANSLOCATION,
	STATUS_APPLICATION,
	DAMAGE_OVER_TIME,
	RECOVERY,
	PROTECTION,
	STATUS_REMOVAL,
	TERRAIN_CHANGE,
	MARK
}
