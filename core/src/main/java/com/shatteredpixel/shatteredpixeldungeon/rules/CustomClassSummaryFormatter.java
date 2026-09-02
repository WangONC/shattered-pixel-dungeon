package com.shatteredpixel.shatteredpixeldungeon.rules;

/** Deterministic, localized player summaries for one authored class blueprint. */
public final class CustomClassSummaryFormatter {

	private CustomClassSummaryFormatter() {}

	public static String shortSummary(CustomClassConfig config) {
		return PlayerFacingClassBuildFormatter.shortSummary(config == null ? null : config.toClassBuild());
	}

	public static String buildSheet(CustomClassConfig config) {
		return PlayerFacingClassBuildFormatter.buildSheet(config == null ? null : config.toClassBuild());
	}

	/** Fixed-height, names-only status used while the player moves between builder steps. */
	public static String builderStatus(CustomClassConfig config) {
		return PlayerFacingClassBuildFormatter.builderStatus(config == null ? null : config.toClassBuild());
	}

	/** Short final preview. Full natural-language rules remain available through buildSheet(). */
	public static String compactPreview(CustomClassConfig config) {
		return PlayerFacingClassBuildFormatter.compactPreview(config == null ? null : config.toClassBuild());
	}
}
