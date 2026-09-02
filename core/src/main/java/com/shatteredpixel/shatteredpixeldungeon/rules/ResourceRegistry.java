package com.shatteredpixel.shatteredpixeldungeon.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Quick recipes which expand into one generic pool and ordinary editable gameplay components. */
public final class ResourceRegistry {

	public static final class Recipe {
		public final ResourceSpec pool;
		public final ArrayList<ClassGameplayComponentSpec> components = new ArrayList<>();
		private Recipe(ResourceSpec pool) { this.pool = pool; }
		public void addTo(ClassBuild build) {
			build.resources.add(pool.copy());
			for (ClassGameplayComponentSpec component : components) build.gameplayComponents.add(component.copy());
		}
	}

	public enum Preset {
		MANA(ResourceEngine.MANA), RAGE(ResourceEngine.RAGE), MOMENTUM(ResourceEngine.MOMENTUM),
		FOCUS(ResourceEngine.FOCUS), AFFLICTION(ResourceEngine.AFFLICTION), CUSTOM(ResourceEngine.MANUAL);

		private final ResourceEngine legacyPresentation;
		Preset(ResourceEngine source) { legacyPresentation = source; }
		public String displayName() { return legacyPresentation.displayName(); }
		public String description() { return legacyPresentation.description(); }
		public String idBase() { return name().toLowerCase(); }

		public Recipe createRecipe(String id) {
			ResourceSpec pool = new ResourceSpec();
			pool.id = id; pool.name = displayName(); pool.minimum = 0;
			pool.capacity = this == MOMENTUM ? 12 : 10;
			pool.initialValue = this == MANA ? 5 : 0;
			pool.current = pool.initialValue;
			Recipe result = new Recipe(pool);
			switch (this) {
				case MANA:
					result.components.add(flow(id, "mana_regen_" + id, ResourceFlowSpec.Trigger.TURN,
							ResourceFlowSpec.Operation.GAIN, 1, 3, 0));
					break;
				case RAGE:
					ClassGameplayComponentSpec hit = flow(id, "rage_hit_" + id, ResourceFlowSpec.Trigger.HIT,
							ResourceFlowSpec.Operation.GAIN, 1, 1, 0);
					hit.meleeOnly = true; result.components.add(hit);
					result.components.add(flow(id, "rage_hurt_" + id, ResourceFlowSpec.Trigger.DAMAGED,
							ResourceFlowSpec.Operation.GAIN, 1, 1, 0));
					result.components.add(flow(id, "rage_decay_" + id, ResourceFlowSpec.Trigger.OUT_OF_COMBAT,
							ResourceFlowSpec.Operation.LOSE, 1, 2, 5));
					break;
				case MOMENTUM:
					result.components.add(flow(id, "momentum_move_" + id, ResourceFlowSpec.Trigger.MOVE,
							ResourceFlowSpec.Operation.GAIN, 1, 1, 0));
					result.components.add(flow(id, "momentum_stop_" + id, ResourceFlowSpec.Trigger.STOPPED_MOVING,
							ResourceFlowSpec.Operation.LOSE, 2, 1, 1));
					result.components.add(flow(id, "momentum_wait_" + id, ResourceFlowSpec.Trigger.WAIT,
							ResourceFlowSpec.Operation.CLEAR, 0, 1, 0));
					break;
				case FOCUS:
					result.components.add(flow(id, "focus_wait_" + id, ResourceFlowSpec.Trigger.WAIT,
							ResourceFlowSpec.Operation.GAIN, 2, 1, 0));
					result.components.add(flow(id, "focus_safe_" + id, ResourceFlowSpec.Trigger.OUT_OF_COMBAT,
							ResourceFlowSpec.Operation.GAIN, 1, 2, 0));
					result.components.add(flow(id, "focus_interrupt_" + id, ResourceFlowSpec.Trigger.DAMAGED,
							ResourceFlowSpec.Operation.CLEAR, 0, 1, 0));
					break;
				case AFFLICTION:
					result.components.add(flow(id, "affliction_status_" + id, ResourceFlowSpec.Trigger.NEGATIVE_STATUS,
							ResourceFlowSpec.Operation.GAIN, 2, 1, 0));
					break;
				case CUSTOM:
				default: break;
			}
			return result;
		}

		/** Pool-only helper retained for migration tests; Builder always applies createRecipe(). */
		public ResourceSpec create(String id) { return createRecipe(id).pool; }
	}

	private static ClassGameplayComponentSpec flow(String resourceId, String id,
			ResourceFlowSpec.Trigger trigger, ResourceFlowSpec.Operation operation,
			int amount, int interval, int delay) {
		return ClassGameplayComponentSpec.resourceFlow(id, resourceId, trigger, operation, amount, interval, delay);
	}

	private static final List<Preset> PRESETS = Collections.unmodifiableList(Arrays.asList(Preset.values()));
	private static final List<ResourceEngine> LEGACY_VALUES = Collections.unmodifiableList(Arrays.asList(ResourceEngine.values()));
	private ResourceRegistry() {}
	public static List<Preset> presets() { return PRESETS; }
	/** MIGRATION_ONLY. New player resources do not carry a ResourceEngine identity. */
	public static List<ResourceEngine> exposed() { return LEGACY_VALUES; }

	static Recipe migrateLegacy(ResourceSpec pool) {
		ResourceEngine source = pool.engine == null ? ResourceEngine.MANUAL : pool.engine;
		Preset preset;
		switch (source) {
			case MANA: preset = Preset.MANA; break;
			case RAGE: preset = Preset.RAGE; break;
			case MOMENTUM: preset = Preset.MOMENTUM; break;
			case FOCUS: preset = Preset.FOCUS; break;
			case AFFLICTION: preset = Preset.AFFLICTION; break;
			default: preset = Preset.CUSTOM; break;
		}
		Recipe result = preset.createRecipe(pool.id);
		result.pool.name = pool.name; result.pool.minimum = pool.minimum;
		result.pool.capacity = pool.capacity; result.pool.initialValue = pool.initialValue;
		result.pool.current = pool.current;
		if (!pool.legacyFlows.isEmpty()) {
			result.components.clear();
			int i = 0;
			for (ResourceFlowSpec flow : pool.legacyFlows) {
				ClassGameplayComponentSpec value = ClassGameplayComponentSpec.resourceFlow(
						"migrated_flow_" + pool.id + "_" + i++, pool.id, flow.trigger,
						flow.operation, flow.amount, flow.interval, flow.delay);
				value.meleeOnly = flow.meleeOnly; result.components.add(value);
			}
		}
		if (pool.legacyRefill != null && pool.legacyRefill.active()) {
			result.components.add(ClassGameplayComponentSpec.activeRefill("refill_" + pool.id,
					pool.id, pool.legacyRefill.amount, pool.legacyRefill.actionTime));
		}
		return result;
	}
}
