package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

/** Fail-closed envelope for a blueprint facet owned by a later phase. */
public final class DeferredEntityFacetSpec implements EntityBodySpec, SpawnPolicySpec,
		OwnershipSpec, RelationSpec, PersistenceSpec {
	private final EntityFacetKind facetKind;
	private final String variantKey;
	private final ImplementationState implementationState;

	public DeferredEntityFacetSpec(EntityFacetKind facetKind, String variantKey,
			ImplementationState implementationState) {
		if (facetKind == null || variantKey == null || variantKey.isEmpty()
				|| (implementationState != ImplementationState.DEFERRED
				&& implementationState != ImplementationState.UNSUPPORTED)) {
			throw new IllegalArgumentException("entity facet must be explicitly deferred or unsupported");
		}
		this.facetKind = facetKind;
		this.variantKey = variantKey;
		this.implementationState = implementationState;
	}

	@Override public EntityFacetKind facetKind() { return facetKind; }
	@Override public String variantKey() { return variantKey; }
	@Override public ImplementationState implementationState() { return implementationState; }
}
