package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Single player/runtime/budget registry for class-level gameplay component templates. */
public final class GameplayComponentRegistry {
	public enum Category { BASIC_COMBAT, RESOURCE_ECONOMY, ENTITY_RELATION, PERSISTENCE_CAPACITY, MODE_STATE, TRADEOFF }
	public enum Template {
		BASIC_ATTACK(Category.BASIC_COMBAT, ClassGameplayComponentSpec.Type.BASIC_ATTACK),
		RESOURCE_POOL(Category.RESOURCE_ECONOMY, null),
		RESOURCE_GAIN(Category.RESOURCE_ECONOMY, ClassGameplayComponentSpec.Type.RESOURCE_FLOW),
		RESOURCE_LOSS(Category.RESOURCE_ECONOMY, ClassGameplayComponentSpec.Type.RESOURCE_FLOW),
		RESOURCE_CONVERT(Category.RESOURCE_ECONOMY, ClassGameplayComponentSpec.Type.RESOURCE_FLOW),
		ACTIVE_REFILL(Category.RESOURCE_ECONOMY, ClassGameplayComponentSpec.Type.ACTIVE_REFILL),
		OWNERSHIP(Category.ENTITY_RELATION, ClassGameplayComponentSpec.Type.OWNERSHIP),
		COMMAND(Category.ENTITY_RELATION, ClassGameplayComponentSpec.Type.COMMAND),
		RECYCLE(Category.ENTITY_RELATION, ClassGameplayComponentSpec.Type.RECYCLE),
		ENTITY_CAPACITY(Category.PERSISTENCE_CAPACITY, ClassGameplayComponentSpec.Type.ENTITY_CAPACITY),
		PERSISTENCE(Category.PERSISTENCE_CAPACITY, ClassGameplayComponentSpec.Type.PERSISTENCE),
		MODE_ENGINE(Category.MODE_STATE, ClassGameplayComponentSpec.Type.MODE_ENGINE),
		GLOBAL_CONSTRAINT(Category.TRADEOFF, ClassGameplayComponentSpec.Type.GLOBAL_CONSTRAINT);

		public final Category category;
		public final ClassGameplayComponentSpec.Type type;
		Template(Category category, ClassGameplayComponentSpec.Type type) { this.category = category; this.type = type; }
		public String displayName() { return Messages.get(GameplayComponentRegistry.class, name().toLowerCase() + "_name"); }
		public String description() { return Messages.get(GameplayComponentRegistry.class, name().toLowerCase() + "_desc"); }
	}

	private static final List<Template> TEMPLATES = Collections.unmodifiableList(Arrays.asList(Template.values()));
	private GameplayComponentRegistry() {}

	public static List<Template> templates() { return TEMPLATES; }
	public static ArrayList<Template> templates(Category category) {
		ArrayList<Template> result = new ArrayList<>();
		for (Template value : TEMPLATES) if (value.category == category) result.add(value);
		return result;
	}

	public static ClassGameplayComponentSpec create(Template template, String id) {
		if (template == Template.RESOURCE_POOL) return null;
		ClassGameplayComponentSpec result = new ClassGameplayComponentSpec(template.type, id);
		switch (template) {
			case BASIC_ATTACK: result.basicAttack = BasicAttackProfile.WEAK; break;
			case RESOURCE_GAIN: result.resourceOperation = ResourceFlowSpec.Operation.GAIN; break;
			case RESOURCE_LOSS: result.resourceOperation = ResourceFlowSpec.Operation.LOSE; break;
			case RESOURCE_CONVERT: result.resourceOperation = ResourceFlowSpec.Operation.CONVERT; break;
			case ENTITY_CAPACITY: result.entityFilter = ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR; result.capacity = 1; break;
			case PERSISTENCE: result.entityFilter = ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR; result.lifetime = 5; break;
			case MODE_ENGINE: result.modes.add("offense"); result.modes.add("defense"); break;
			case GLOBAL_CONSTRAINT: result.restriction = Restriction.WAIT_CLEARS_RESOURCE; break;
			default: break;
		}
		return result;
	}

	public static boolean playerExposed(ClassGameplayComponentSpec value) {
		if (value == null || !value.structurallyValid()) return false;
		for (Template template : TEMPLATES) if (template.type == value.type) {
			if (value.type != ClassGameplayComponentSpec.Type.RESOURCE_FLOW) return true;
			boolean matches = template == Template.RESOURCE_GAIN && value.resourceOperation == ResourceFlowSpec.Operation.GAIN
					|| template == Template.RESOURCE_LOSS && (value.resourceOperation == ResourceFlowSpec.Operation.LOSE
					|| value.resourceOperation == ResourceFlowSpec.Operation.CLEAR)
					|| template == Template.RESOURCE_CONVERT && value.resourceOperation == ResourceFlowSpec.Operation.CONVERT;
			if (matches) return true;
		}
		return false;
	}
}
