package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.SkillCompiler;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;

/** Narrow production bridge for an installed finalized v6 build. */
public final class V6RuleRuntimeBridge {
	private final ClassCompilePlan plan;private final EffectExecutorRegistry executors;private final V6RuleRuntime runtime;
	private SkillExecutionResult lastExecution;
	private V6RuleRuntimeBridge(ClassCompilePlan plan,EffectExecutorRegistry executors){this.plan=plan;this.executors=executors;this.runtime=new V6RuleRuntime(plan,executors);}
	public static V6RuleRuntimeBridge install(Hero hero,ClassBuildSpec finalizedBuild){
		if(hero==null||finalizedBuild==null)throw new IllegalArgumentException("hero and finalized build required");
		EffectExecutorRegistry executors=EffectExecutorRegistry.standard();ClassCompilePlan plan=new SkillCompiler(executors).compile(finalizedBuild);
		if(!plan.executable())throw new IllegalArgumentException("v6 build is not executable: "+plan.diagnostics());
		V6RuleRuntimeBridge bridge=new V6RuleRuntimeBridge(plan,executors);hero.storeGameplayComponentsV6(finalizedBuild,ClassRuntimeState.builder(finalizedBuild.buildId()).build());hero.setGameplayComponentsV6RuntimeBridge(bridge);return bridge;
	}
	public SkillExecutionResult triggerActive(Hero hero,int selectedCell,String requestedSkillId,long eventId,long causeEventId,RuntimeExecutionContext.DamageGateway damageGateway){
		if(hero==null||damageGateway==null)throw new IllegalArgumentException("hero and damage gateway required");
		CompiledSkill skill=select(requestedSkillId);if(skill==null)throw new IllegalArgumentException("active skill is absent or ambiguous");
		Char selected=Actor.findChar(selectedCell);int selectedId=selected==null?0:selected.id();
		GameplayEventContext event=new GameplayEventContext(eventId,causeEventId,GameplayEventContext.RuleEventType.ACTIVE,
				hero.id(),hero.id(),selectedId,hero.pos,hero.pos,selectedCell,skill.skillId());
		lastExecution=runtime.execute(skill.skillId(),new RuntimeExecutionContext(event,damageGateway));return lastExecution;
	}
	private CompiledSkill select(String requestedSkillId){
		if(requestedSkillId!=null){try{return plan.find(StableId.fromStored(requestedSkillId));}catch(IllegalArgumentException ignored){return null;}}
		return plan.skills().size()==1?plan.skills().get(0):null;
	}
	public ClassCompilePlan plan(){return plan;}public EffectExecutorRegistry executors(){return executors;}public SkillExecutionResult lastExecution(){return lastExecution;}
}
