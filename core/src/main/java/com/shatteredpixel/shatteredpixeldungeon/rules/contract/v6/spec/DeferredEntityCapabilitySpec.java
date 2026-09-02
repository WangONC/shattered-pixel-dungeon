package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

/** Fail-closed envelope for non-P01 entity capabilities. */
public final class DeferredEntityCapabilitySpec implements EntityCapabilitySpec {
	private final String variantKey;
	private final ImplementationState implementationState;

	public DeferredEntityCapabilitySpec(String variantKey, ImplementationState implementationState) {
		if (variantKey == null || variantKey.isEmpty()
				|| (implementationState != ImplementationState.DEFERRED
				&& implementationState != ImplementationState.UNSUPPORTED)) {
			throw new IllegalArgumentException("entity capability must be explicitly deferred or unsupported");
		}
		this.variantKey = variantKey;
		this.implementationState = implementationState;
	}

	@Override public String variantKey() { return variantKey; }
	@Override public ImplementationState implementationState() { return implementationState; }
}
