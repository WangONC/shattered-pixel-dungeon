package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

public final class ResourceSpec implements DeclarationSpec {
	public enum ResourceOverflowPolicy { FAIL, CLAMP, DISCARD_EXCESS }
	public static final class ResourceHudSpec {
		private final boolean visible;
		private final int order;
		private final String presentationKey;
		public ResourceHudSpec(boolean visible, int order, String presentationKey) {
			if (order < 0) throw new IllegalArgumentException("HUD order must be non-negative");
			this.visible = visible; this.order = order;
			this.presentationKey = presentationKey == null ? "" : presentationKey;
		}
		public boolean visible() { return visible; }
		public int order() { return order; }
		public String presentationKey() { return presentationKey; }
	}

	private final StableId id;
	private final DisplayName displayName;
	private final int minimum;
	private final int maximum;
	private final int initialValue;
	private final ResourceOverflowPolicy defaultOverflowPolicy;
	private final ResourceHudSpec hud;

	public ResourceSpec(StableId id, DisplayName displayName, int minimum, int maximum, int initialValue,
			ResourceOverflowPolicy defaultOverflowPolicy, ResourceHudSpec hud) {
		if (id == null || displayName == null || defaultOverflowPolicy == null || hud == null) {
			throw new IllegalArgumentException("resource identity, policy, and HUD are required");
		}
		if (maximum <= minimum || initialValue < minimum || initialValue > maximum) {
			throw new IllegalArgumentException("invalid resource bounds");
		}
		this.id = id; this.displayName = displayName; this.minimum = minimum; this.maximum = maximum;
		this.initialValue = initialValue; this.defaultOverflowPolicy = defaultOverflowPolicy; this.hud = hud;
	}

	@Override public StableId id() { return id; }
	@Override public DisplayName displayName() { return displayName; }
	@Override public RefKind refKind() { return RefKind.RESOURCE; }
	@Override public ImplementationState implementationState() { return ImplementationState.DECLARED; }
	public int minimum() { return minimum; }
	public int maximum() { return maximum; }
	public int initialValue() { return initialValue; }
	public ResourceOverflowPolicy defaultOverflowPolicy() { return defaultOverflowPolicy; }
	public ResourceHudSpec hud() { return hud; }
	public ResourceSpec withIdentity(StableId value) { return new ResourceSpec(value, displayName, minimum, maximum, initialValue, defaultOverflowPolicy, hud); }
	public ResourceSpec withDisplayName(DisplayName value) { return new ResourceSpec(id, value, minimum, maximum, initialValue, defaultOverflowPolicy, hud); }
}
