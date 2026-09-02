package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;

/**
 * A saved resource pool declaration. It stores only pool identity and bounds; gain, loss,
 * conversion and refill live in independent {@link ClassGameplayComponentSpec} instances.
 */
public class ResourceSpec implements Bundlable {
	public String id = "resource";
	public String name = "";
	public int minimum;
	public int capacity = 10;
	public int initialValue;
	/** Runtime value in a live ClassBuild copy; new build declarations initialize it from initialValue. */
	public int current;

	/** MIGRATION_ONLY identity read from schema-4 and older saves. New pools are generic. */
	@Deprecated public ResourceEngine engine = ResourceEngine.MANUAL;
	/** MIGRATION_ONLY data extracted by ClassBuild and never written by schema 5. */
	final ArrayList<ResourceFlowSpec> legacyFlows = new ArrayList<>();
	ResourceRefillSpec legacyRefill = new ResourceRefillSpec();

	public ResourceSpec() {}

	/** MIGRATION/fixture convenience. Player presets use ResourceRegistry recipes. */
	public ResourceSpec(ResourceEngine engine) {
		applyLegacyPreset(engine == null ? ResourceEngine.MANUAL : engine);
	}

	private void applyLegacyPreset(ResourceEngine engine) {
		this.engine = engine;
		id = engine.name().toLowerCase();
		name = engine.displayName();
		minimum = 0;
		capacity = Math.max(0, engine.max);
		initialValue = Math.max(minimum, Math.min(capacity, engine.initial));
		current = initialValue;
	}

	public ResourceSpec copy() {
		ResourceSpec result = new ResourceSpec();
		result.id = id; result.name = name; result.minimum = minimum;
		result.capacity = capacity; result.initialValue = initialValue; result.current = current;
		result.engine = engine;
		for (ResourceFlowSpec flow : legacyFlows) result.legacyFlows.add(flow.copy());
		result.legacyRefill = legacyRefill == null ? new ResourceRefillSpec() : legacyRefill.copy();
		return result;
	}

	public String displayName() {
		return name == null || name.trim().isEmpty() ? id : name.trim();
	}

	public boolean valid() {
		return id != null && !id.isEmpty() && name != null && minimum >= 0
				&& capacity >= minimum && initialValue >= minimum && initialValue <= capacity
				&& current >= minimum && current <= capacity;
	}

	/** Pool storage itself is neutral; extra pools pay for the adaptation in ClassBuild. */
	public int componentPowerCost() { return Math.max(0, capacity - 10) / 5; }

	boolean hasLegacyEconomy() {
		return engine != ResourceEngine.MANUAL || !legacyFlows.isEmpty()
				|| legacyRefill != null && legacyRefill.active();
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("id", id); bundle.put("name", name); bundle.put("minimum", minimum);
		bundle.put("capacity", capacity); bundle.put("initial_value", initialValue); bundle.put("current", current);
		// Explicit generic marker protects new saves from reviving a preset-specific runtime branch.
		bundle.put("engine", ResourceEngine.MANUAL);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		engine = bundle.contains("engine") ? bundle.getEnum("engine", ResourceEngine.class) : ResourceEngine.MANUAL;
		if (id == null || id.isEmpty()) id = engine.name().toLowerCase();
		if (bundle.contains("capacity")) {
			name = bundle.getString("name");
			minimum = bundle.contains("minimum") ? Math.max(0, bundle.getInt("minimum")) : 0;
			capacity = Math.max(minimum, bundle.getInt("capacity"));
			initialValue = Math.max(minimum, Math.min(capacity, bundle.getInt("initial_value")));
			current = bundle.contains("current") ? Math.max(minimum, Math.min(capacity, bundle.getInt("current"))) : initialValue;
			legacyFlows.clear();
			if (bundle.contains("flows")) for (Bundlable value : bundle.getCollection("flows")) {
				if (value instanceof ResourceFlowSpec) legacyFlows.add((ResourceFlowSpec)value);
			}
			legacyRefill = bundle.contains("refill") ? (ResourceRefillSpec)bundle.get("refill") : new ResourceRefillSpec();
		} else {
			applyLegacyPreset(engine);
		}
		if (name == null || name.trim().isEmpty()) name = engine.displayName();
	}
}
