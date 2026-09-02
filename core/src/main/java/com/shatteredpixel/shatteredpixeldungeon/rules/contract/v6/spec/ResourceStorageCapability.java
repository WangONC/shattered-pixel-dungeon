package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Contract 6.3 data boundary. Runtime storage behavior remains deferred. */
public final class ResourceStorageCapability implements EntityCapabilitySpec {
	public static final String VARIANT = "RESOURCE_STORAGE";
	private final List<EntityResourceSlotSpec> slots;

	public ResourceStorageCapability(List<EntityResourceSlotSpec> slots) {
		if (slots == null) throw new IllegalArgumentException("resource slots are required");
		this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
	}

	public List<EntityResourceSlotSpec> slots() { return slots; }
	@Override public String variantKey() { return VARIANT; }
	@Override public ImplementationState implementationState() { return ImplementationState.DEFERRED; }
	public ResourceStorageCapability withSlots(List<EntityResourceSlotSpec> value) {
		return new ResourceStorageCapability(value);
	}
}
