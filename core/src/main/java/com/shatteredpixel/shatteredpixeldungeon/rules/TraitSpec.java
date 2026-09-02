package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Saved, parameterized player Trait. The enum is the behavior id; bindings are per-build data. */
public class TraitSpec implements Bundlable {
	public CoreRuleVocabulary type = CoreRuleVocabulary.ACCUMULATION;
	/** Optional stable resource pool id used by resource-connecting Traits. */
	public String resourceId = "";
	/** Optional mode/mark/status id used by state-connecting Traits. */
	public String stateId = "";

	public TraitSpec() {}
	public TraitSpec(CoreRuleVocabulary type) { this.type = type; }

	public static TraitSpec of(CoreRuleVocabulary type) { return new TraitSpec(type); }
	public static TraitSpec resource(CoreRuleVocabulary type, String resourceId) {
		TraitSpec result = new TraitSpec(type);
		result.resourceId = resourceId == null ? "" : resourceId;
		return result;
	}
	public static TraitSpec state(CoreRuleVocabulary type, String stateId) {
		TraitSpec result = new TraitSpec(type);
		result.stateId = stateId == null ? "" : stateId;
		return result;
	}

	public TraitSpec copy() {
		TraitSpec result = new TraitSpec(type);
		result.resourceId = resourceId;
		result.stateId = stateId;
		return result;
	}

	public String stableId() {
		return type.name().toLowerCase() + (resourceId.isEmpty() ? "" : ":resource=" + resourceId)
				+ (stateId.isEmpty() ? "" : ":state=" + stateId);
	}

	public int budgetCost() { return type.capacityCost; }
	public String displayName(ClassBuild build) {
		String binding = bindingName(build);
		return binding.isEmpty() ? type.displayName()
				: Messages.get(TraitSpec.class, "bound_name", type.displayName(), binding);
	}
	public String shortSummary(ClassBuild build) {
		return type.summary(bindingName(build));
	}
	public String detail(ClassBuild build) {
		return type.detail(bindingName(build));
	}

	public String bindingName(ClassBuild build) {
		if (!resourceId.isEmpty() && build != null) {
			ResourceSpec resource = build.resource(resourceId);
			if (resource != null) return resource.displayName();
		}
		if (!stateId.isEmpty()) return Messages.get(TraitSpec.class, "mode_" + stateId.toLowerCase());
		return "";
	}

	public boolean bindingValid(ClassBuild build) {
		if (type.requiresResource()) return build != null && !resourceId.isEmpty() && build.resource(resourceId) != null;
		if (type.requiresMode()) return build != null && !stateId.isEmpty() && build.producesMode(stateId);
		return true;
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("type", type);
		bundle.put("resource_id", resourceId);
		bundle.put("state_id", stateId);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		type = bundle.getEnum("type", CoreRuleVocabulary.class);
		resourceId = bundle.contains("resource_id") ? bundle.getString("resource_id") : "";
		stateId = bundle.contains("state_id") ? bundle.getString("state_id") : "";
		if (resourceId == null) resourceId = "";
		if (stateId == null) stateId = "";
	}
}
