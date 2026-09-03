package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class FixedValueSpec implements ValueSpec {
	public static final String VARIANT = "FIXED";
	private final int value;
	public FixedValueSpec(int value) {
		if (value < -999999 || value > 999999) throw new IllegalArgumentException("fixed value outside supported range");
		this.value = value;
	}
	public int value() { return value; }
	@Override public String variantKey() { return VARIANT; }
}
