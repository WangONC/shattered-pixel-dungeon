package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.EffectSpec;

public interface EffectExecutor {
	EffectResult execute(EffectSpec effect, Char target, GameplayEventContext context, RuntimeTrace trace);
}
