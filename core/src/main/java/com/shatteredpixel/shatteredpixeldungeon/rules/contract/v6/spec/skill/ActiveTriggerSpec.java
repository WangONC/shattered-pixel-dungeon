package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class ActiveTriggerSpec implements TriggerSpec {
	public static final String VARIANT = "ACTIVE";
	private final StableId nodeId;
	public ActiveTriggerSpec(StableId nodeId) {
		if (nodeId == null) throw new IllegalArgumentException("trigger node id is required");
		this.nodeId = nodeId;
	}
	@Override public StableId nodeId() { return nodeId; }
	@Override public String variantKey() { return VARIANT; }
}
