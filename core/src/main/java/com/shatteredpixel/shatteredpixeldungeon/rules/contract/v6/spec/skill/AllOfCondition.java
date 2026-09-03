package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AllOfCondition implements ConditionExpr {
	public static final String VARIANT = "ALL_OF";
	private final List<ConditionExpr> children;
	public AllOfCondition(List<ConditionExpr> children) {
		if (children == null || children.size() > 8 || children.contains(null)) {
			throw new IllegalArgumentException("AllOf requires 0..8 non-null children");
		}
		this.children = Collections.unmodifiableList(new ArrayList<>(children));
	}
	public static AllOfCondition always() { return new AllOfCondition(Collections.<ConditionExpr>emptyList()); }
	public List<ConditionExpr> children() { return children; }
	@Override public String variantKey() { return VARIANT; }
}
