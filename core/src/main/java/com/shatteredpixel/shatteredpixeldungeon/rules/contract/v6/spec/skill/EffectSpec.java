package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Discriminated effect union. Concrete variants never share a generic parameter bag. */
public interface EffectSpec {
	StableId effectId();
	EffectFamily family();
	EffectVariantKey variantKey();
}
