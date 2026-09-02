package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Saved player-facing identity for an operation naturally granted by class-level components. */
public class ClassOperationSpec implements Bundlable {
	public enum Type { RELOAD, COMMAND, MODE_SWITCH, RECYCLE }
	public String id = "operation";
	public String name = "";
	public Type type = Type.RELOAD;
	/** Stable class-level component which grants this operation. */
	public String sourceComponentId = "";
	public String resourceId = "";
	public ClassGameplayComponentSpec.EntityFilter entityFilter = ClassGameplayComponentSpec.EntityFilter.OWNED_ENTITY;
	public int amount;
	public float actionTime = 1f;

	public ClassOperationSpec() {}
	public ClassOperationSpec(Type type, String id) { this.type = type; this.id = id; }
	public ClassOperationSpec copy() {
		ClassOperationSpec result = new ClassOperationSpec(type, id);
		result.name = name; result.sourceComponentId = sourceComponentId; result.resourceId = resourceId;
		result.entityFilter = entityFilter; result.amount = amount; result.actionTime = actionTime;
		return result;
	}
	public String displayName(ClassBuild build) {
		if (name != null && !name.trim().isEmpty()) return name.trim();
		if (type == Type.RELOAD && build != null && build.resource(resourceId) != null) {
			return Messages.get(ClassOperationSpec.class, "reload_named", build.resource(resourceId).displayName());
		}
		return Messages.get(ClassOperationSpec.class, type.name().toLowerCase() + "_name");
	}
	public String description(ClassBuild build) {
		if (type == Type.RELOAD && build != null && build.resource(resourceId) != null) {
			ResourceSpec pool = build.resource(resourceId);
			return amount <= 0 ? Messages.get(ClassOperationSpec.class, "reload_full_desc", pool.displayName())
					: Messages.get(ClassOperationSpec.class, "reload_amount_desc", amount, pool.displayName());
		}
		return Messages.get(ClassOperationSpec.class, type.name().toLowerCase() + "_desc");
	}
	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("id", id); bundle.put("name", name); bundle.put("type", type);
		bundle.put("source_component_id", sourceComponentId);
		bundle.put("resource_id", resourceId); bundle.put("entity_filter", entityFilter); bundle.put("amount", amount); bundle.put("action_time", actionTime);
	}
	@Override public void restoreFromBundle(Bundle bundle) {
		id = bundle.getString("id"); name = bundle.getString("name"); type = bundle.getEnum("type", Type.class);
		sourceComponentId = bundle.getString("source_component_id"); if (sourceComponentId == null) sourceComponentId = "";
		resourceId = bundle.getString("resource_id"); amount = Math.max(0, bundle.getInt("amount"));
		entityFilter = bundle.contains("entity_filter") ? bundle.getEnum("entity_filter", ClassGameplayComponentSpec.EntityFilter.class)
				: ClassGameplayComponentSpec.EntityFilter.OWNED_ENTITY;
		actionTime = bundle.contains("action_time") ? Math.max(1f, bundle.getFloat("action_time")) : 1f;
	}
}
