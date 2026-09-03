package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledClassComponent;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.SkillCompiler;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.HeroClassBundleCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;

/** Narrow production bridge for an installed, finalized and admitted v6 build. */
public final class V6RuleRuntimeBridge {
	private final ClassBuildSpec build;private final ClassCompilePlan plan;private final EffectExecutorRegistry executors;private final V6RuleRuntime runtime;
	private SkillExecutionResult lastExecution;
	private V6RuleRuntimeBridge(ClassBuildSpec build,ClassCompilePlan plan,EffectExecutorRegistry executors,ClassRuntimeState state){this.build=build;this.plan=plan;this.executors=executors;this.runtime=new V6RuleRuntime(plan,executors,state);}
	public static V6RuleRuntimeBridge install(Hero hero,ClassBuildSpec finalizedBuild){
		if(hero==null||finalizedBuild==null)throw new IllegalArgumentException("hero and finalized build required");
		ClassRuntimeState state=stateForInstall(hero,finalizedBuild);return installExact(hero,finalizedBuild,state);
	}
	private static V6RuleRuntimeBridge installExact(Hero hero,ClassBuildSpec build,ClassRuntimeState state){
		EffectExecutorRegistry executors=EffectExecutorRegistry.standard();ClassCompilePlan plan=new SkillCompiler(executors).compile(build);
		if(!plan.executable())throw new IllegalArgumentException("v6 build is not executable: "+plan.diagnostics());
		String incompatible=ResourceRuntimeStateFactory.incompatibility(plan,state);if(!incompatible.isEmpty())throw new IllegalArgumentException(incompatible);
		V6RuleRuntimeBridge bridge=new V6RuleRuntimeBridge(build,plan,executors,state);hero.storeGameplayComponentsV6(build,state);hero.setGameplayComponentsV6RuntimeBridge(bridge);hero.setGameplayComponentsV6RuntimeDiagnostic("");return bridge;
	}
	private static ClassRuntimeState stateForInstall(Hero hero,ClassBuildSpec build){
		if(hero.gameplayComponentsV6BuildPayload()==null&&hero.gameplayComponentsV6RuntimePayload()==null)return ResourceRuntimeStateFactory.initialize(build);
		HeroClassBundleCodec.LoadPair loaded=hero.loadGameplayComponentsV6();if(loaded.build()==null||loaded.runtime()==null)throw new IllegalArgumentException("runtime.existing_payload_invalid:"+loaded.diagnostic());
		if(!loaded.build().buildId().equals(build.buildId())){if(pristine(loaded.runtime()))return ResourceRuntimeStateFactory.initialize(build);throw new IllegalArgumentException("runtime.build_id_mismatch");}
		String incompatible=ResourceRuntimeStateFactory.incompatibility(build,loaded.runtime());if(!incompatible.isEmpty())throw new IllegalArgumentException(incompatible);return loaded.runtime();
	}
	private static boolean pristine(ClassRuntimeState s){return s.resources().isEmpty()&&s.cooldowns().isEmpty()&&s.usesThisFloor().isEmpty()&&s.modes().isEmpty()&&s.marks().isEmpty()&&s.entities().isEmpty()&&s.scheduledPayloads().isEmpty()&&s.attachments().isEmpty()&&s.snapshots().isEmpty()&&s.learnedAbilities().isEmpty()&&s.properties().isEmpty()&&s.nextRuntimeEntityId()==0&&s.nextPayloadInstanceId()==0&&s.nextEventId()==0;}
	/** Restore is fail-closed and records diagnostics; it never repairs or falls back. */
	public static boolean restore(Hero hero){
		if(hero==null)throw new IllegalArgumentException("hero is required");if(hero.gameplayComponentsV6BuildPayload()==null&&hero.gameplayComponentsV6RuntimePayload()==null){hero.setGameplayComponentsV6RuntimeBridge(null);hero.setGameplayComponentsV6RuntimeDiagnostic("");return false;}
		try{HeroClassBundleCodec.LoadPair loaded=hero.loadGameplayComponentsV6();if(loaded.build()==null||loaded.runtime()==null||loaded.state()==DependencyState.HARD_CONFLICT||loaded.state()==DependencyState.UNRESOLVED)throw new IllegalArgumentException("runtime.restore_invalid state="+loaded.state()+" diagnostic="+loaded.diagnostic()+" report="+loaded.report().diagnostics());installExact(hero,loaded.build(),loaded.runtime());return true;}catch(RuntimeException rejected){hero.setGameplayComponentsV6RuntimeBridge(null);hero.setGameplayComponentsV6RuntimeDiagnostic(rejected.getMessage()==null?rejected.getClass().getSimpleName():rejected.getMessage());return false;}
	}
	public SkillExecutionResult triggerActive(Hero hero,int selectedCell,String requestedSkillId,long eventId,long causeEventId,RuntimeExecutionContext.DamageGateway damageGateway){
		if(hero==null||damageGateway==null)throw new IllegalArgumentException("hero and damage gateway required");CompiledSkill skill=select(requestedSkillId);if(skill==null)throw new IllegalArgumentException("active skill is absent or ambiguous");Char selected=Actor.findChar(selectedCell);int selectedId=selected==null?0:selected.id();GameplayEventContext event=new GameplayEventContext(eventId,causeEventId,GameplayEventContext.RuleEventType.ACTIVE,hero.id(),hero.id(),selectedId,hero.pos,hero.pos,selectedCell,skill.skillId());lastExecution=runtime.execute(skill.skillId(),new RuntimeExecutionContext(event,damageGateway,host(hero)));hero.storeGameplayComponentsV6(build,runtime.state());return lastExecution;
	}
	public SkillExecutionResult triggerOperation(Hero hero,String operationId,long eventId,long causeEventId,RuntimeExecutionContext.DamageGateway damageGateway){if(hero==null||damageGateway==null)throw new IllegalArgumentException("hero and damage gateway required");StableId id=StableId.fromStored(operationId);GameplayEventContext event=new GameplayEventContext(eventId,causeEventId,GameplayEventContext.RuleEventType.ACTIVE,hero.id(),hero.id(),hero.id(),hero.pos,hero.pos,hero.pos,id);lastExecution=runtime.executeOperation(id,new RuntimeExecutionContext(event,damageGateway,host(hero)));hero.storeGameplayComponentsV6(build,runtime.state());return lastExecution;}
	public SkillExecutionResult triggerResourceFlows(Hero hero,GameplayEventContext.RuleEventType eventType,long eventId,long causeEventId,RuntimeExecutionContext.DamageGateway damageGateway){if(hero==null||eventType==null||damageGateway==null)throw new IllegalArgumentException("hero, event type and damage gateway required");GameplayEventContext event=new GameplayEventContext(eventId,causeEventId,eventType,hero.id(),hero.id(),hero.id(),hero.pos,hero.pos,hero.pos,plan.buildId());lastExecution=runtime.executeResourceFlows(new RuntimeExecutionContext(event,damageGateway,host(hero)));hero.storeGameplayComponentsV6(build,runtime.state());return lastExecution;}
	private static RuntimeExecutionContext.HostGateway host(final Hero hero){return new RuntimeExecutionContext.HostGateway(){public int classOwnerLevel(){return hero.lvl;}public boolean hasItemCost(CompiledSkill.ItemCategory category,int count){return hero.hasGameplayComponentsV6ItemCost(category.name(),count);}public boolean commitItemCost(CompiledSkill.ItemCategory category,int count){return hero.commitGameplayComponentsV6ItemCost(category.name(),count);}public void spendActionTime(int turns){hero.spend(turns);}};}
	private CompiledSkill select(String requestedSkillId){if(requestedSkillId!=null){try{return plan.find(StableId.fromStored(requestedSkillId));}catch(IllegalArgumentException ignored){return null;}}return plan.skills().size()==1?plan.skills().get(0):null;}
	public boolean basicAttackAllowed(){CompiledClassComponent component=basicAttack();return component==null||component.attackAvailability()!=CompiledClassComponent.BasicAttackAvailability.NONE;}
	public int basicAttackDamage(int nativeDamage){CompiledClassComponent component=basicAttack();if(component==null)return nativeDamage;if(component.attackAvailability()==CompiledClassComponent.BasicAttackAvailability.NONE)return 0;return (int)Math.max(0,Math.min(Integer.MAX_VALUE,(long)nativeDamage*component.first()/component.second()));}
	public float basicAttackDelay(float nativeDelay){CompiledClassComponent component=basicAttack();return component==null?nativeDelay:Math.max(0.01f,nativeDelay*component.actionTurns());}
	private CompiledClassComponent basicAttack(){for(CompiledClassComponent component:plan.components())if(component.variant()==CompiledClassComponent.Variant.BASIC_ATTACK)return component;return null;}
	public ClassCompilePlan plan(){return plan;}public EffectExecutorRegistry executors(){return executors;}public ClassRuntimeState state(){return runtime.state();}public SkillExecutionResult lastExecution(){return lastExecution;}
}
