package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Persistable draft sentinel. It is never exposed as a runnable variant. */
public final class UnconfiguredTriggerSpec implements TriggerSpec {
	public static final String VARIANT = "UNCONFIGURED";
	private final StableId nodeId;
	public UnconfiguredTriggerSpec(StableId nodeId) {
		if (nodeId == null) throw new IllegalArgumentException("trigger node id is required");
		this.nodeId = nodeId;
	}
	@Override public StableId nodeId() { return nodeId; }
	@Override public String variantKey() { return VARIANT; }
}
