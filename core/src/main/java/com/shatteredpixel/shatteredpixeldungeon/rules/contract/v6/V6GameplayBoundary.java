package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

/**
 * Non-gameplay namespace marker established by P00 for Contract v0.2 FINAL.
 *
 * <p>No public v6 gameplay path is enabled by this phase. Later phases must implement contract
 * symbols here (or behind an equivalent one-way boundary) without importing the frozen legacy-v5
 * namespace.</p>
 */
public final class V6GameplayBoundary {
	public static final String CONTRACT = "0.2-final";
	public static final int TARGET_SCHEMA = 6;
	public static final boolean PUBLIC_GAMEPLAY_ENABLED = false;
	/** P02 real UI exists behind this transition flag; runtime gameplay remains disabled. */
	public static final boolean PLAYER_BUILDER_ENABLED = false;

	private V6GameplayBoundary() {}
}
