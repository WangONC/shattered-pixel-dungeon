package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

/**
 * P01 save boundary for the typed parts of an entity blueprint. Concrete
 * gameplay variants are introduced by their owning later phase.
 */
public interface EntityFacetSpec {
	EntityFacetKind facetKind();
	String variantKey();
	ImplementationState implementationState();
}
