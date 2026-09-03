package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class RelationFilterSpec implements EntityFilterExpr {
	public enum RelationAlignment { ENEMY, ALLY }
	public static final String VARIANT = "RELATION";
	private final RelationAlignment relationToClassOwner;
	private final boolean includeSelf;
	public RelationFilterSpec(RelationAlignment relationToClassOwner, boolean includeSelf) {
		if (relationToClassOwner == null) throw new IllegalArgumentException("relation is required");
		this.relationToClassOwner = relationToClassOwner;
		this.includeSelf = includeSelf;
	}
	@Override public String variantKey() { return VARIANT; }
	public RelationAlignment relationToClassOwner() { return relationToClassOwner; }
	public boolean includeSelf() { return includeSelf; }
}
