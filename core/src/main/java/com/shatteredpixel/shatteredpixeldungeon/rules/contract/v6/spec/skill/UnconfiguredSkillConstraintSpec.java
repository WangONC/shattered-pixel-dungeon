package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class UnconfiguredSkillConstraintSpec implements SkillConstraintSpec {
	public static final String VARIANT = "UNCONFIGURED";
	private final StableId constraintId;
	public UnconfiguredSkillConstraintSpec(StableId constraintId) {
		if (constraintId == null) throw new IllegalArgumentException("constraint id is required");
		this.constraintId = constraintId;
	}
	@Override public StableId constraintId() { return constraintId; }
	@Override public String variantKey() { return VARIANT; }
}
