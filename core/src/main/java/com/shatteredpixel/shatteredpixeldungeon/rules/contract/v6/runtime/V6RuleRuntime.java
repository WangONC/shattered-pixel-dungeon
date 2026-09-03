package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.SkillCompiler;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Executes only immutable compiled nodes; authoring models never cross this boundary. */
public final class V6RuleRuntime {
	private final ClassCompilePlan plan;private final EffectExecutorRegistry executors;
	public V6RuleRuntime(ClassCompilePlan plan,EffectExecutorRegistry executors){
		if(plan==null||executors==null)throw new IllegalArgumentException("compile plan and executors are required");
		if(!plan.executable())throw new IllegalArgumentException("preview or partial compile plan is not executable");
		if(!SkillCompiler.RUNTIME_VERSION.equals(plan.runtimeVersion()))throw new IllegalArgumentException("runtime version mismatch");
		if(!executors.supportsAll(plan.requiredEffectVariants()))throw new IllegalArgumentException("compile plan executors are unavailable");
		this.plan=plan;this.executors=executors;
	}
	public SkillExecutionResult execute(StableId skillId,RuntimeExecutionContext context){
		if(skillId==null||context==null)throw new IllegalArgumentException("skill and runtime context required");RuntimeTrace trace=new RuntimeTrace();GameplayEventContext event=context.event();
		trace.record("event","event_id="+event.eventId()+" cause_event_id="+event.causeEventId()+" type="+event.eventType()+" owner_actor="+event.classOwnerActorId()+" owner_cell="+event.classOwnerCell()+" source_actor="+event.sourceActorId()+" source_cell="+event.sourceCell()+" selected_actor="+event.selectedActorId()+" selected_cell="+event.selectedCell()+" originating_skill="+event.originatingSkillId().value());
		CompiledSkill skill=plan.find(skillId);if(skill==null)return result(SkillExecutionResult.Status.UNSUPPORTED,0,trace,"runtime.skill_not_compiled");
		trace.record("skill","skill="+skill.skillId().value()+" trigger="+skill.trigger().variant()+" delivery="+skill.delivery().variant());
		if(!skill.skillId().equals(event.originatingSkillId()))return result(SkillExecutionResult.Status.BLOCKED,0,trace,"runtime.originating_skill_mismatch");
		if(skill.trigger().variant()!=CompiledSkill.TriggerVariant.ACTIVE||event.eventType()!=GameplayEventContext.RuleEventType.ACTIVE)return result(SkillExecutionResult.Status.BLOCKED,0,trace,"runtime.trigger_not_satisfied");
		if(skill.condition().variant()!=CompiledSkill.ConditionVariant.ALWAYS)return result(SkillExecutionResult.Status.UNSUPPORTED,0,trace,"runtime.condition_unsupported");
		PreflightResult targetCheck=new SkillTargetPreflight().resolve(skill,context,trace);if(targetCheck.status()!=PreflightResult.Status.READY)return result(map(targetCheck.status()),0,trace,targetCheck.diagnostic());
		EffectPreflightResult primaryCheck=executors.preflight(skill.effectChain().primary(),targetCheck.target(),context);trace.record("effect_preflight","slot=primary variant="+skill.effectChain().primary().variant()+" status="+primaryCheck.status());
		if(!primaryCheck.readyForExecute())return result(map(primaryCheck.status()),0,trace,primaryCheck.diagnostic());
		if(skill.effectChain().secondary()!=null){EffectPreflightResult secondaryCheck=executors.preflight(skill.effectChain().secondary().effect(),targetCheck.target(),context);trace.record("effect_preflight","slot=secondary variant="+skill.effectChain().secondary().effect().variant()+" status="+secondaryCheck.status());if(!secondaryCheck.readyForExecute())return result(map(secondaryCheck.status()),0,trace,secondaryCheck.diagnostic());}
		if(skill.cost().variant()!=CompiledSkill.CostVariant.NO_COST)return result(SkillExecutionResult.Status.UNSUPPORTED,0,trace,"runtime.cost_unsupported");
		trace.record("cost","variant=NO_COST status=COMMITTED after_effect_preflight=true");
		EffectResult primary=executors.execute(skill.effectChain().primary(),targetCheck.target(),context,trace);if(primary.status()!=EffectResult.Status.APPLIED)return result(map(primary.status()),0,trace,primary.diagnostic());
		int applied=primary.appliedAmount();trace.record("chain","chain="+skill.effectChain().chainId().value()+" primary=APPLIED");
		if(skill.effectChain().secondary()!=null){EffectResult secondary=executors.execute(skill.effectChain().secondary().effect(),targetCheck.target(),context,trace);if(secondary.status()!=EffectResult.Status.APPLIED)return result(map(secondary.status()),applied,trace,secondary.diagnostic());applied+=secondary.appliedAmount();trace.record("chain","chain="+skill.effectChain().chainId().value()+" secondary=APPLIED activation="+skill.effectChain().secondary().activation());}
		return result(SkillExecutionResult.Status.APPLIED,applied,trace,"runtime.applied");
	}
	private static SkillExecutionResult result(SkillExecutionResult.Status status,int amount,RuntimeTrace trace,String diagnostic){trace.record("result","status="+status+" applied="+amount+" diagnostic="+diagnostic);return new SkillExecutionResult(status,amount,trace,diagnostic);}
	private static SkillExecutionResult.Status map(PreflightResult.Status status){switch(status){case NO_TARGET:return SkillExecutionResult.Status.NO_TARGET;case BLOCKED:return SkillExecutionResult.Status.BLOCKED;case UNSUPPORTED:return SkillExecutionResult.Status.UNSUPPORTED;default:throw new IllegalArgumentException("READY cannot map to failure");}}
	private static SkillExecutionResult.Status map(EffectPreflightResult.Status status){return SkillExecutionResult.Status.valueOf(status.name());}
	private static SkillExecutionResult.Status map(EffectResult.Status status){return SkillExecutionResult.Status.valueOf(status.name());}
}
