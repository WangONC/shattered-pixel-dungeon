package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class NoCostSpec implements CostSpec {
	public static final String VARIANT = "NO_COST";
	private final StableId nodeId;
	public NoCostSpec(StableId nodeId) {
		if (nodeId == null) throw new IllegalArgumentException("cost node id is required");
		this.nodeId = nodeId;
	}
	@Override public StableId nodeId() { return nodeId; }
	@Override public String variantKey() { return VARIANT; }
}
