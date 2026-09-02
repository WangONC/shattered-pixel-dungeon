package com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5;

/**
 * P00 boundary metadata for the pre-contract Gameplay Components implementation.
 *
 * <p>The existing ClassBuild/EffectSpec/RuleMark/RuleMode/registry stack remains runnable for
 * regression and migration checks, but its reports cannot establish Gameplay Components v6
 * completion. This class deliberately contains no gameplay behavior.</p>
 */
public final class LegacyGameplayBoundary {
	public static final String VERSION = "legacy-v5";
	public static final String EVIDENCE_CLASSIFICATION = "LEGACY_EVIDENCE_NOT_PLAYER_PATH";
	public static final boolean V6_COMPLETION_ELIGIBLE = false;

	private LegacyGameplayBoundary() {}
}
