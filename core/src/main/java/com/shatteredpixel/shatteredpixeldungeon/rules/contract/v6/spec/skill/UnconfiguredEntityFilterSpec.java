package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class UnconfiguredEntityFilterSpec implements EntityFilterExpr {
	public static final String VARIANT = "UNCONFIGURED";
	@Override public String variantKey() { return VARIANT; }
}
