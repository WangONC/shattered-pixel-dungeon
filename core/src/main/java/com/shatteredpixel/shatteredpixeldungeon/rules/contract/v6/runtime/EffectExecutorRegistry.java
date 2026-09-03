package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.EffectVariantKey;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Exact effect-key dispatch. This registry has no dependency on builder FormSchema. */
public final class EffectExecutorRegistry {
	private final Map<EffectVariantKey, EffectExecutor> executors;
	public EffectExecutorRegistry(Map<EffectVariantKey, EffectExecutor> executors) {
		if (executors == null || executors.containsKey(null) || executors.containsValue(null)) throw new IllegalArgumentException("executor map is invalid");
		this.executors = Collections.unmodifiableMap(new EnumMap<>(executors));
	}
	public static EffectExecutorRegistry standard() {
		Map<EffectVariantKey, EffectExecutor> result = new EnumMap<>(EffectVariantKey.class);
		result.put(EffectVariantKey.DIRECT_DAMAGE, new DirectDamageExecutor());
		return new EffectExecutorRegistry(result);
	}
	public static EffectExecutorRegistry empty() { return new EffectExecutorRegistry(new EnumMap<EffectVariantKey, EffectExecutor>(EffectVariantKey.class)); }
	public boolean has(EffectVariantKey key) { return executors.containsKey(key); }
	public EffectResult execute(EffectSpec effect, Char target, GameplayEventContext context, RuntimeTrace trace) {
		EffectExecutor executor = executors.get(effect.variantKey());
		if (executor == null) {
			trace.record("effect", "variant=" + effect.variantKey() + " status=MISSING_EXECUTOR");
			return EffectResult.failed(EffectResult.Status.MISSING_EXECUTOR, "executor.missing:" + effect.variantKey());
		}
		return executor.execute(effect, target, context, trace);
	}
}
