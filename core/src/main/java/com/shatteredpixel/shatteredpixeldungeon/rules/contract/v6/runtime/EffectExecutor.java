package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;

public interface EffectExecutor<E extends CompiledSkill.Effect> {
	CompiledSkill.EffectVariant variant();
	Class<E> effectType();
	EffectPreflightResult preflight(E effect,Char target,RuntimeExecutionContext context);
	EffectResult execute(E effect,Char target,RuntimeExecutionContext context,RuntimeTrace trace);
}
