package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

/** Persistable draft sentinel for the condition choice. */
public final class UnconfiguredConditionExpr implements ConditionExpr {
	public static final String VARIANT = "UNCONFIGURED";
	@Override public String variantKey() { return VARIANT; }
}
