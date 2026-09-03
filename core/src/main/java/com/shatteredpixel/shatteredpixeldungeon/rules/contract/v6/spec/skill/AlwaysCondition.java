package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

/** Explicit Always leaf. It has no payload and therefore cannot hide a fallback. */
public final class AlwaysCondition implements ConditionExpr {
	public static final String VARIANT = "ALWAYS";
	@Override public String variantKey() { return VARIANT; }
}
