package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save;

/**
 * Narrow, direction-safe boundary implemented by legacy player storage hosts.
 * The v6 codec depends on this port, never on the legacy Hero model.
 */
public interface V6PayloadHost {
	void setGameplayComponentsV6Payloads(String buildPayload, String runtimePayload);
	String gameplayComponentsV6BuildPayload();
	String gameplayComponentsV6RuntimePayload();
}
