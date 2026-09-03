package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;

/** Independent support metadata. Form exposure and executor registration are checked separately. */
public final class VariantDescriptor {
	private final String family;
	private final String variantKey;
	private final ImplementationState state;
	private final boolean playerExposed;
	private final String priceKey;
	public VariantDescriptor(String family, String variantKey, ImplementationState state,
			boolean playerExposed, String priceKey) {
		if (family == null || family.isEmpty() || variantKey == null || variantKey.isEmpty() || state == null) {
			throw new IllegalArgumentException("variant descriptor fields are required");
		}
		if (playerExposed && state != ImplementationState.IMPLEMENTED) {
			throw new IllegalArgumentException("only implemented variants may be player exposed");
		}
		if (playerExposed && (priceKey == null || priceKey.isEmpty())) {
			throw new IllegalArgumentException("player-exposed variants require a price key");
		}
		this.family = family;
		this.variantKey = variantKey;
		this.state = state;
		this.playerExposed = playerExposed;
		this.priceKey = priceKey == null ? "" : priceKey;
	}
	public String family() { return family; }
	public String variantKey() { return variantKey; }
	public String qualifiedKey() { return family + "." + variantKey; }
	public ImplementationState state() { return state; }
	public boolean playerExposed() { return playerExposed; }
	public String priceKey() { return priceKey; }
}
