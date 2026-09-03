package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

public final class NumberFieldSchema extends AbstractFormFieldSchema {
	private final int minimum;
	private final int maximum;
	private final int step;
	public NumberFieldSchema(String fieldKey, String labelKey, boolean required, int minimum, int maximum, int step) {
		super(fieldKey, labelKey, required);
		if (maximum < minimum || step < 1) throw new IllegalArgumentException("invalid number field bounds");
		this.minimum = minimum;
		this.maximum = maximum;
		this.step = step;
	}
	@Override public Kind kind() { return Kind.NUMBER; }
	public int minimum() { return minimum; }
	public int maximum() { return maximum; }
	public int step() { return step; }
	public int clamp(int value) { return Math.max(minimum, Math.min(maximum, value)); }
}
