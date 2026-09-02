package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;

/** Contract 6.3 declaration for an entity-owned resource slot. */
public final class EntityResourceSlotSpec {
	private final ResourceRef resource;
	private final int initialValue;
	private final int maximumOverride;

	public EntityResourceSlotSpec(ResourceRef resource, int initialValue, int maximumOverride) {
		if (resource == null || maximumOverride < 0) {
			throw new IllegalArgumentException("resource slot fields are invalid");
		}
		this.resource = resource;
		this.initialValue = initialValue;
		this.maximumOverride = maximumOverride;
	}

	public ResourceRef resource() { return resource; }
	public int initialValue() { return initialValue; }
	public int maximumOverride() { return maximumOverride; }
	public EntityResourceSlotSpec withResource(ResourceRef value) {
		return new EntityResourceSlotSpec(value, initialValue, maximumOverride);
	}
}
