package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;

/** Typed SPD-native direct damage. All mutation is confined to execute. */
public final class DirectDamageExecutor implements EffectExecutor<CompiledSkill.DirectDamageEffect> {
	@Override public CompiledSkill.EffectVariant variant(){return CompiledSkill.EffectVariant.DIRECT_DAMAGE;}
	@Override public Class<CompiledSkill.DirectDamageEffect> effectType(){return CompiledSkill.DirectDamageEffect.class;}
	@Override public EffectPreflightResult preflight(CompiledSkill.DirectDamageEffect effect,Char target,RuntimeExecutionContext context){
		if(effect==null)return EffectPreflightResult.failed(EffectPreflightResult.Status.TYPE_MISMATCH,"direct_damage.type_mismatch");
		if(context==null||context.resolve(effect.value())<=0||effect.damageType()!=CompiledSkill.DamageType.UNTYPED||effect.defensePolicy()!=CompiledSkill.DefensePolicy.SPD_NATIVE)
			return EffectPreflightResult.failed(EffectPreflightResult.Status.UNSUPPORTED_PARAMETERS,"direct_damage.parameters_unsupported");
		if(target==null||!target.isAlive())return EffectPreflightResult.failed(EffectPreflightResult.Status.TARGET_UNAVAILABLE,"direct_damage.target_unavailable");
		Char source=context==null?null:context.sourceActor();
		if(source==null||!source.isAlive())return EffectPreflightResult.failed(EffectPreflightResult.Status.TARGET_UNAVAILABLE,"direct_damage.source_unavailable");
		if(target.isInvulnerable(source.getClass()))return EffectPreflightResult.failed(EffectPreflightResult.Status.IMMUNE,"direct_damage.target_immune");
		return EffectPreflightResult.ready();
	}
	@Override public EffectResult execute(CompiledSkill.DirectDamageEffect effect,Char target,RuntimeExecutionContext context,RuntimeTrace trace){
		EffectPreflightResult check=preflight(effect,target,context);if(!check.readyForExecute())return EffectExecutorRegistry.failed(check);
		int requested=context.resolve(effect.value());CompiledSkill.DirectDamageEffect resolved=new CompiledSkill.DirectDamageEffect(effect.effectId(),requested,effect.damageType(),effect.defensePolicy());
		RuntimeExecutionContext.DamageOutcome outcome=context.applyDamage(resolved,target);
		trace.record("effect","variant=DIRECT_DAMAGE effect="+effect.effectId().value()+" source_actor="+context.event().sourceActorId()
				+" target_actor="+target.id()+" target_cell="+target.pos+" requested="+requested+" hp_before="+outcome.hpBefore()
				+" hp_after="+outcome.hpAfter()+" applied="+outcome.appliedAmount()+" defense=SPD_NATIVE event_id="+context.event().eventId()
				+" cause_event_id="+context.event().causeEventId()+" originating_skill="+context.event().originatingSkillId().value());
		return EffectResult.applied(outcome.appliedAmount());
	}
}
