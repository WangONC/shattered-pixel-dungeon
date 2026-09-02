package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Saved state for a non-primary resource pool in a multi-resource ClassBuild. */
public class RuleResourceState implements Bundlable {
	public String id = "";
	public ResourceEngine engine = ResourceEngine.MANA;
	public ResourceSpec spec;
	public int value;
	public int max;
	public int turnCounter;
	public int turnsSinceCombat;
	public int turnsSinceMove;
	public int consecutiveMoves;
	public int focusSafeTurns;
	public int overflowRemainder;
	/** Per-flow matching-event counters for intervals that are not derived from turn clocks. */
	public int[] flowCounters = new int[0];

	public RuleResourceState() {}
	public RuleResourceState(ResourceSpec spec) {
		id = spec.id;
		engine = spec.engine;
		this.spec = spec.copy();
		value = spec.initialValue;
		max = spec.capacity;
		flowCounters = new int[0];
	}

	public RuleResourceState copy() {
		RuleResourceState result = new RuleResourceState();
		result.id = id;
		result.engine = engine;
		result.spec = spec == null ? null : spec.copy();
		result.value = value;
		result.max = max;
		result.turnCounter = turnCounter;
		result.turnsSinceCombat = turnsSinceCombat;
		result.turnsSinceMove = turnsSinceMove;
		result.consecutiveMoves = consecutiveMoves;
		result.focusSafeTurns = focusSafeTurns;
		result.overflowRemainder = overflowRemainder;
		result.flowCounters = flowCounters == null ? new int[0] : flowCounters.clone();
		return result;
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("id", id);
		bundle.put("engine", engine);
		if (spec != null) bundle.put("spec", spec);
		bundle.put("value", value);
		bundle.put("max", max);
		bundle.put("turn", turnCounter);
		bundle.put("combat", turnsSinceCombat);
		bundle.put("move", turnsSinceMove);
		bundle.put("moves", consecutiveMoves);
		bundle.put("focus", focusSafeTurns);
		bundle.put("overflow_remainder", overflowRemainder);
		bundle.put("flow_counters", flowCounters == null ? new int[0] : flowCounters);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		engine = bundle.getEnum("engine", ResourceEngine.class);
		spec = bundle.contains("spec") ? (ResourceSpec)bundle.get("spec") : null;
		value = bundle.getInt("value");
		max = bundle.getInt("max");
		turnCounter = bundle.getInt("turn");
		turnsSinceCombat = bundle.getInt("combat");
		turnsSinceMove = bundle.getInt("move");
		consecutiveMoves = bundle.getInt("moves");
		focusSafeTurns = bundle.getInt("focus");
		overflowRemainder = bundle.getInt("overflow_remainder");
		flowCounters = bundle.contains("flow_counters") ? bundle.getIntArray("flow_counters") : new int[0];
	}
}
