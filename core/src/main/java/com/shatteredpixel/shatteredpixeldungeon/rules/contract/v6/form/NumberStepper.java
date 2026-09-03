package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;

/** Three-control numeric editor model: decrement, current value, increment. */
public final class NumberStepper {
	private final NumberFieldSchema schema;
	private final int current;
	public NumberStepper(NumberFieldSchema schema, int current) {
		if (schema == null || current < schema.minimum() || current > schema.maximum()) {
			throw new IllegalArgumentException("stepper current value is outside schema bounds");
		}
		this.schema = schema;
		this.current = current;
	}
	public NumberFieldSchema schema() { return schema; }
	public int current() { return current; }
	public int decrement() { return move(-1); }
	public int increment() { return move(1); }
	public int accelerated(int direction, int repeatCount) {
		if (repeatCount < 1) throw new IllegalArgumentException("repeat count must be positive");
		return move(direction * Math.max(1, repeatCount));
	}
	public BuilderCommand.SetFieldValue command(String ownerId, String variantKey, int direction, int repeatCount) {
		return new BuilderCommand.SetFieldValue(ownerId, variantKey, schema.fieldKey(),
				Integer.toString(accelerated(direction, repeatCount)));
	}
	private int move(int steps) {
		long candidate = (long) current + (long) schema.step() * steps;
		return schema.clamp(candidate > Integer.MAX_VALUE ? Integer.MAX_VALUE
				: candidate < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) candidate);
	}
}
