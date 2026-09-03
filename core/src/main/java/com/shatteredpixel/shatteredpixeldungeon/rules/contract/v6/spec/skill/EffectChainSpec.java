package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class EffectChainSpec {
	private final StableId chainId;
	private final EffectSpec primary;
	private final SecondaryEffectSpec secondary;
	public EffectChainSpec(StableId chainId, EffectSpec primary, SecondaryEffectSpec secondary) {
		if (chainId == null || primary == null) throw new IllegalArgumentException("chain id and primary are required");
		this.chainId = chainId;
		this.primary = primary;
		this.secondary = secondary;
	}
	public StableId chainId() { return chainId; }
	public EffectSpec primary() { return primary; }
	public SecondaryEffectSpec secondary() { return secondary; }
	public EffectChainSpec withPrimary(EffectSpec value) { return new EffectChainSpec(chainId, value, secondary); }
	public EffectChainSpec withSecondary(SecondaryEffectSpec value) { return new EffectChainSpec(chainId, primary, value); }
}
