package com.shatteredpixel.shatteredpixeldungeon.qa;

/** Small semantic vocabulary for build integrity checks; it is not a gameplay API. */
public enum RuleQaCapability {
	RESOURCE_SOURCE,
	RESOURCE_SINK,
	DIRECT_DAMAGE,
	DOT_DAMAGE,
	FORCED_MOVEMENT,
	ENVIRONMENT_CONTROL,
	SURVIVAL,
	HEALING,
	SHIELDING,
	MOBILITY,
	CONTROL,
	STATUS_APPLICATION,
	STATUS_REMOVAL,
	// Reserved resolution extension points. V0.1 never claims these are implemented.
	ENVIRONMENT_KILL,
	SUMMON_DAMAGE,
	DEVICE_DAMAGE,
	BYPASS
}
