package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Typed skill activation. Every concrete trigger owns only its own parameters. */
public interface TriggerSpec {
	StableId nodeId();
	String variantKey();
}
