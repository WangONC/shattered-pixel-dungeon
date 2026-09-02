package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/**
 * One saved, player-editable source or loss rule for a resource pool. Active refill is declared by
 * ResourceRefillSpec and generates a ClassOperation; this class describes event/passive economy.
 */
public class ResourceFlowSpec implements Bundlable {

	public enum Trigger {
		TURN, HIT, DAMAGED, MOVE, WAIT, KILL, NEGATIVE_STATUS, OWNED_ENTITY_REMOVED,
		OUT_OF_COMBAT, STOPPED_MOVING
	}
	public enum Operation { GAIN, LOSE, CLEAR, CONVERT }

	public Trigger trigger = Trigger.TURN;
	public Operation operation = Operation.GAIN;
	public int amount = 1;
	/** Apply once per this many matching events/turns. */
	public int interval = 1;
	/** OUT_OF_COMBAT/STOPPED_MOVING grace period. */
	public int delay;
	public boolean meleeOnly;

	public ResourceFlowSpec() {}

	public ResourceFlowSpec(Trigger trigger, Operation operation, int amount, int interval, int delay) {
		this.trigger = trigger;
		this.operation = operation;
		this.amount = amount;
		this.interval = interval;
		this.delay = delay;
	}

	public ResourceFlowSpec copy() {
		ResourceFlowSpec result = new ResourceFlowSpec(trigger, operation, amount, interval, delay);
		result.meleeOnly = meleeOnly;
		return result;
	}

	public boolean valid() {
		return trigger != null && operation != null && amount >= 0 && interval >= 1 && delay >= 0;
	}

	public boolean gain() { return operation == Operation.GAIN; }
	public boolean loss() { return operation == Operation.LOSE || operation == Operation.CLEAR || operation == Operation.CONVERT; }

	/** Automatic gain occupies budget; genuine decay offsets it conservatively. */
	public int nominalPower() {
		if (!gain()) return 0;
		int result = trigger == Trigger.TURN ? 2 : 1;
		if (amount >= 3 || interval == 1 && trigger == Trigger.TURN) result++;
		return result;
	}

	public int rebate() {
		if (!loss()) return 0;
		return operation == Operation.CLEAR || amount >= 2 ? 1 : 0;
	}

	public String displayName() {
		return Messages.get(ResourceFlowSpec.class, "name",
				Messages.get(ResourceFlowSpec.class, "trigger_" + trigger.name().toLowerCase()),
				Messages.get(ResourceFlowSpec.class, "operation_" + operation.name().toLowerCase()));
	}

	public String description() {
		return Messages.get(ResourceFlowSpec.class, "description", displayName(), amount, interval, delay);
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("trigger", trigger);
		bundle.put("operation", operation);
		bundle.put("amount", amount);
		bundle.put("interval", interval);
		bundle.put("delay", delay);
		bundle.put("melee_only", meleeOnly);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		trigger = bundle.getEnum("trigger", Trigger.class);
		operation = bundle.getEnum("operation", Operation.class);
		amount = Math.max(0, bundle.getInt("amount"));
		interval = Math.max(1, bundle.getInt("interval"));
		delay = Math.max(0, bundle.getInt("delay"));
		meleeOnly = bundle.getBoolean("melee_only");
	}
}
