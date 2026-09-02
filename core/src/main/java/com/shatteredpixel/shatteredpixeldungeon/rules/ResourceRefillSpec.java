package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Class-level resource refill declaration. It produces a Reload ClassOperation, never a fake Skill. */
public class ResourceRefillSpec implements Bundlable {
	public enum Model { NONE, ACTIVE }
	public Model model = Model.NONE;
	/** Zero means refill to the pool's maximum. */
	public int amount;
	public float actionTime = 1f;
	public ResourceRefillSpec() {}
	public ResourceRefillSpec(Model model, int amount, float actionTime) {
		this.model = model == null ? Model.NONE : model;
		this.amount = Math.max(0, amount);
		this.actionTime = Math.max(1f, actionTime);
	}

	public boolean active() { return model == Model.ACTIVE; }
	public int refillAmount(int current, int maximum) {
		return Math.max(0, amount <= 0 ? maximum - current : Math.min(amount, maximum - current));
	}
	public int budgetCost() {
		if (!active()) return 0;
		// A dedicated sustainable refill is useful even though its committed action is a real
		// tradeoff. Fast/full refills pay more; slower or small refills remain inexpensive.
		return (actionTime <= 1f ? 1 : 0) + (amount <= 0 || amount >= 4 ? 1 : 0);
	}
	public ResourceRefillSpec copy() {
		ResourceRefillSpec result = new ResourceRefillSpec();
		result.model = model; result.amount = amount; result.actionTime = actionTime;
		return result;
	}
	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("model", model); bundle.put("amount", amount); bundle.put("action_time", actionTime);
	}
	@Override public void restoreFromBundle(Bundle bundle) {
		model = bundle.contains("model") ? bundle.getEnum("model", Model.class) : Model.NONE;
		amount = Math.max(0, bundle.getInt("amount"));
		actionTime = bundle.contains("action_time") ? Math.max(1f, bundle.getFloat("action_time")) : 1f;
	}
}
