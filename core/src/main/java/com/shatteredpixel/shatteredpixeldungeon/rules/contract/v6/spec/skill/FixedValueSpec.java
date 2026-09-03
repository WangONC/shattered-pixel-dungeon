package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class FixedValueSpec implements ValueSpec {
	public static final String VARIANT = "FIXED";
	private final int value;
	public FixedValueSpec(int value) {
		if (value < 1 || value > 999) throw new IllegalArgumentException("fixed value must be 1..999");
		this.value = value;
	}
	public int value() { return value; }
	@Override public String variantKey() { return VARIANT; }
}
