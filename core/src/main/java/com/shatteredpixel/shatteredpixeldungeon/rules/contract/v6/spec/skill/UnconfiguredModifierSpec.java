package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class UnconfiguredModifierSpec implements ModifierSpec {
	public static final String VARIANT = "UNCONFIGURED";
	private final StableId nodeId;
	public UnconfiguredModifierSpec(StableId nodeId) {
		if (nodeId == null) throw new IllegalArgumentException("modifier node id is required");
		this.nodeId = nodeId;
	}
	@Override public StableId nodeId() { return nodeId; }
	@Override public String variantKey() { return VARIANT; }
}
