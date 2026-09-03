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
			if(expectedType(entry.getKey())!=entry.getValue().effectType())throw new IllegalArgumentException(entry.getKey()+" executor type mismatch");
		}
	}
	public static EffectExecutorRegistry standard(){Map<CompiledSkill.EffectVariant,EffectExecutor<?>> result=new EnumMap<>(CompiledSkill.EffectVariant.class);
		result.put(CompiledSkill.EffectVariant.DIRECT_DAMAGE,new DirectDamageExecutor());
		result.put(CompiledSkill.EffectVariant.PERCENT_MAX_HP_DAMAGE,new P04EffectExecutors.PercentMaxHp());result.put(CompiledSkill.EffectVariant.MISSING_HP_DAMAGE,new P04EffectExecutors.MissingHp());result.put(CompiledSkill.EffectVariant.EXECUTE,new P04EffectExecutors.Execute());result.put(CompiledSkill.EffectVariant.APPLY_STATUS,new P04EffectExecutors.ApplyStatus());
		result.put(CompiledSkill.EffectVariant.PUSH,new P04EffectExecutors.Push());result.put(CompiledSkill.EffectVariant.PULL,new P04EffectExecutors.Pull());result.put(CompiledSkill.EffectVariant.THROW,new P04EffectExecutors.ThrowValue());result.put(CompiledSkill.EffectVariant.DASH,new P04EffectExecutors.Dash());result.put(CompiledSkill.EffectVariant.TELEPORT,new P04EffectExecutors.Teleport());result.put(CompiledSkill.EffectVariant.SWAP_POSITION,new P04EffectExecutors.Swap());
		result.put(CompiledSkill.EffectVariant.HEAL,new P04EffectExecutors.Heal());result.put(CompiledSkill.EffectVariant.BARRIER,new P04EffectExecutors.BarrierValue());result.put(CompiledSkill.EffectVariant.TEMPORARY_HP,new P04EffectExecutors.TemporaryHp());result.put(CompiledSkill.EffectVariant.MITIGATE,new P04EffectExecutors.Mitigate());result.put(CompiledSkill.EffectVariant.REDIRECT_DAMAGE,new P04EffectExecutors.Redirect());result.put(CompiledSkill.EffectVariant.CLEANSE,new P04EffectExecutors.Cleanse());result.put(CompiledSkill.EffectVariant.RESOURCE_OPERATION,new P04EffectExecutors.ResourceOperationValue());return new EffectExecutorRegistry(result);}
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
	private static Class<?> expectedType(CompiledSkill.EffectVariant variant){switch(variant){case DIRECT_DAMAGE:return CompiledSkill.DirectDamageEffect.class;case PERCENT_MAX_HP_DAMAGE:return CompiledSkill.PercentMaxHpDamageEffect.class;case MISSING_HP_DAMAGE:return CompiledSkill.MissingHpDamageEffect.class;case EXECUTE:return CompiledSkill.ExecuteEffect.class;case APPLY_STATUS:return CompiledSkill.ApplyStatusEffect.class;case PUSH:return CompiledSkill.PushEffect.class;case PULL:return CompiledSkill.PullEffect.class;case THROW:return CompiledSkill.ThrowEffect.class;case DASH:return CompiledSkill.DashEffect.class;case TELEPORT:return CompiledSkill.TeleportEffect.class;case SWAP_POSITION:return CompiledSkill.SwapPositionEffect.class;case HEAL:return CompiledSkill.HealEffect.class;case BARRIER:return CompiledSkill.BarrierEffect.class;case TEMPORARY_HP:return CompiledSkill.TemporaryHpEffect.class;case MITIGATE:return CompiledSkill.MitigateEffect.class;case REDIRECT_DAMAGE:return CompiledSkill.RedirectDamageEffect.class;case CLEANSE:return CompiledSkill.CleanseEffect.class;case RESOURCE_OPERATION:return CompiledSkill.ResourceOperationEffect.class;default:throw new AssertionError(variant);}}
}
