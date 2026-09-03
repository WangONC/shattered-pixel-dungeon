package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class SecondaryEffectSpec {
	private final EffectSpec effect;
	private final SecondaryActivationSpec activation;
	public SecondaryEffectSpec(EffectSpec effect, SecondaryActivationSpec activation) {
		if (effect == null || activation == null) throw new IllegalArgumentException("secondary fields are required");
		this.effect = effect;
		this.activation = activation;
	}
	public EffectSpec effect() { return effect; }
	public SecondaryActivationSpec activation() { return activation; }
}
