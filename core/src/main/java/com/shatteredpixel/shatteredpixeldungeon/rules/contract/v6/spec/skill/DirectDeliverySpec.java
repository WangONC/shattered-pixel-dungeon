package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class DirectDeliverySpec implements DeliverySpec {
	public static final String VARIANT = "DIRECT";
	private final StableId nodeId;
	private final boolean requiresLineOfSight;
	public DirectDeliverySpec(StableId nodeId, boolean requiresLineOfSight) {
		if (nodeId == null) throw new IllegalArgumentException("delivery node id is required");
		this.nodeId = nodeId;
		this.requiresLineOfSight = requiresLineOfSight;
	}
	@Override public StableId nodeId() { return nodeId; }
	@Override public String variantKey() { return VARIANT; }
	public boolean requiresLineOfSight() { return requiresLineOfSight; }
	public DirectDeliverySpec withRequiresLineOfSight(boolean value) {
		return new DirectDeliverySpec(nodeId, value);
	}
}
