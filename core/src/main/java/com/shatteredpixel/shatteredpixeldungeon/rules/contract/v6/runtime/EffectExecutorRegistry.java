package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Exact compiled-variant and compiled-type dispatch; no authoring or FormSchema dependency. */
public final class EffectExecutorRegistry {
	private final Map<CompiledSkill.EffectVariant,EffectExecutor<?>> executors;
	public EffectExecutorRegistry(Map<CompiledSkill.EffectVariant,EffectExecutor<?>> executors){
		if(executors==null||executors.containsKey(null)||executors.containsValue(null))throw new IllegalArgumentException("executor map is invalid");
		this.executors=Collections.unmodifiableMap(new EnumMap<>(executors));
		for(Map.Entry<CompiledSkill.EffectVariant,EffectExecutor<?>> entry:this.executors.entrySet()){
			if(entry.getKey()!=entry.getValue().variant())throw new IllegalArgumentException("executor key/variant mismatch");
			if(entry.getKey()==CompiledSkill.EffectVariant.DIRECT_DAMAGE&&entry.getValue().effectType()!=CompiledSkill.DirectDamageEffect.class)
				throw new IllegalArgumentException("DIRECT_DAMAGE executor type mismatch");
		}
	}
	public static EffectExecutorRegistry standard(){Map<CompiledSkill.EffectVariant,EffectExecutor<?>> result=new EnumMap<>(CompiledSkill.EffectVariant.class);result.put(CompiledSkill.EffectVariant.DIRECT_DAMAGE,new DirectDamageExecutor());return new EffectExecutorRegistry(result);}
	public static EffectExecutorRegistry empty(){return new EffectExecutorRegistry(new EnumMap<CompiledSkill.EffectVariant,EffectExecutor<?>>(CompiledSkill.EffectVariant.class));}
	public boolean has(CompiledSkill.EffectVariant key){return key!=null&&executors.containsKey(key);}
	public boolean supportsAll(Iterable<CompiledSkill.EffectVariant> variants){for(CompiledSkill.EffectVariant variant:variants)if(!has(variant))return false;return true;}
	public EffectPreflightResult preflight(CompiledSkill.Effect effect,Char target,RuntimeExecutionContext context){
		if(effect==null)return EffectPreflightResult.failed(EffectPreflightResult.Status.TYPE_MISMATCH,"executor.effect_absent");
		EffectExecutor<?> executor=executors.get(effect.variant());
		if(executor==null)return EffectPreflightResult.failed(EffectPreflightResult.Status.MISSING_EXECUTOR,"executor.missing:"+effect.variant());
		if(!executor.effectType().equals(effect.getClass()))return EffectPreflightResult.failed(EffectPreflightResult.Status.TYPE_MISMATCH,"executor.type_mismatch:"+effect.variant());
		return typedPreflight(executor,effect,target,context);
	}
	public EffectResult execute(CompiledSkill.Effect effect,Char target,RuntimeExecutionContext context,RuntimeTrace trace){
		EffectPreflightResult preflight=preflight(effect,target,context);
		if(!preflight.readyForExecute()){trace.record("effect","variant="+(effect==null?"ABSENT":effect.variant())+" status="+preflight.status());return failed(preflight);}
		return typedExecute(executors.get(effect.variant()),effect,target,context,trace);
	}
	@SuppressWarnings("unchecked") private static <E extends CompiledSkill.Effect> EffectPreflightResult typedPreflight(EffectExecutor<?> raw,CompiledSkill.Effect effect,Char target,RuntimeExecutionContext context){return ((EffectExecutor<E>)raw).preflight((E)effect,target,context);}
	@SuppressWarnings("unchecked") private static <E extends CompiledSkill.Effect> EffectResult typedExecute(EffectExecutor<?> raw,CompiledSkill.Effect effect,Char target,RuntimeExecutionContext context,RuntimeTrace trace){return ((EffectExecutor<E>)raw).execute((E)effect,target,context,trace);}
	static EffectResult failed(EffectPreflightResult value){return EffectResult.failed(EffectResult.Status.valueOf(value.status().name()),value.diagnostic());}
}
