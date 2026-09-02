package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

/** Declaration-only capability boundary; no executor is supplied by P01. */
public interface EntityCapabilitySpec {
	String variantKey();
	ImplementationState implementationState();
}
