package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Persistable typed draft state; selectedFamily may be null until the family command runs. */
public final class UnconfiguredEffectSpec implements EffectSpec {
	private final StableId effectId;
	private final EffectFamily selectedFamily;
	public UnconfiguredEffectSpec(StableId effectId, EffectFamily selectedFamily) {
		if (effectId == null) throw new IllegalArgumentException("effect id is required");
		this.effectId = effectId;
		this.selectedFamily = selectedFamily;
	}
	@Override public StableId effectId() { return effectId; }
	@Override public EffectFamily family() { return selectedFamily; }
	@Override public EffectVariantKey variantKey() { return EffectVariantKey.UNCONFIGURED; }
	public EffectFamily selectedFamily() { return selectedFamily; }
}
