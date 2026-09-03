package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.ClassBuildValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.RuntimeStateValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.SkillExecutionResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.V6RuleRuntimeBridge;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ActiveTriggerSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ResourceState;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class P04RuntimeRestoreAndIdentityTest extends P04RuntimeTestBase {
	@Test public void realHeroRestoreRecompilesAndPreservesCompatibleRuntimeState(){PlayerBuildSession session=P04TestBuilds.classConvert("p04-restore");ClassBuildSpec build=session.finalizeOrThrow();Talent.initClassTalents(hero);V6RuleRuntimeBridge installed=V6RuleRuntimeBridge.install(hero,build);StableId operation=installed.plan().operations().get(0).id();SkillExecutionResult changed=installed.triggerOperation(hero,operation.value(),700,0,damageGateway());assertEquals(SkillExecutionResult.Status.APPLIED,changed.status());assertPair(installed,3,5);
		Bundle bundle=new Bundle();hero.storeInBundle(bundle);Hero restored=new Hero();restored.restoreFromBundle(bundle);assertNotNull(restored.gameplayComponentsV6RuntimeDiagnostic(),restored.gameplayComponentsV6RuntimeBridge());assertEquals("",restored.gameplayComponentsV6RuntimeDiagnostic());assertPair(restored.gameplayComponentsV6RuntimeBridge(),3,5);assertTrue(restored.gameplayComponentsV6RuntimeBridge().plan().executable());}

	@Test public void sameBuildReinstallKeepsStateAndDifferentStatefulBuildIsExplicitlyRejected(){ClassBuildSpec first=P04TestBuilds.classConvert("p04-preserve-a").finalizeOrThrow();V6RuleRuntimeBridge installed=V6RuleRuntimeBridge.install(hero,first);installed.triggerOperation(hero,installed.plan().operations().get(0).id().value(),701,0,damageGateway());V6RuleRuntimeBridge rebuilt=V6RuleRuntimeBridge.install(hero,first);assertPair(rebuilt,3,5);
		ClassBuildSpec different=P04TestBuilds.classConvert("p04-preserve-b").finalizeOrThrow();try{V6RuleRuntimeBridge.install(hero,different);fail("stateful build mismatch was silently reset");}catch(IllegalArgumentException expected){assertTrue(expected.getMessage(),expected.getMessage().contains("runtime.build_id_mismatch"));}assertPair(hero.gameplayComponentsV6RuntimeBridge(),3,5);}

	@Test public void invalidRestoreFailsClosedWithDiagnosticAndWithoutFallback(){Hero source=new Hero();Talent.initClassTalents(source);source.setGameplayComponentsV6Payloads("{not-json","{not-runtime");Bundle bundle=new Bundle();source.storeInBundle(bundle);Hero restored=new Hero();restored.restoreFromBundle(bundle);assertNull(restored.gameplayComponentsV6RuntimeBridge());assertFalse(restored.gameplayComponentsV6RuntimeDiagnostic().isEmpty());}

	@Test public void buildWideNestedIdsAreUniqueAndRuntimeCountersOnlyNameExecutableNodes(){ClassBuildSpec original=P04TestBuilds.resourceSkillConvert("p04-global-id").finalizeOrThrow();SkillSpec skill=original.skills().get(0);SkillSpec duplicate=skill.withActivation(new ActiveTriggerSpec(skill.delivery().nodeId()));ClassBuildSpec invalid=original.toBuilder().skills(Collections.singletonList(duplicate)).build();DependencyReport duplicateReport=new ClassBuildValidator().validate(invalid);assertTrue(duplicateReport.diagnostics().toString(),duplicateReport.has(DependencyState.HARD_CONFLICT));assertTrue(duplicateReport.diagnostics().toString(),duplicateReport.diagnostics().stream().anyMatch(d->"dependency.duplicate_id".equals(d.messageKey())));
		ClassRuntimeState valid=com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.ResourceRuntimeStateFactory.initialize(original);Map<StableId,Integer> wrong=new LinkedHashMap<>();wrong.put(original.resources().get(0).id(),2);ClassRuntimeState invalidCounter=valid.toBuilder().cooldowns(wrong).usesThisFloor(wrong).build();DependencyReport counterReport=new RuntimeStateValidator().validate(original,invalidCounter);assertEquals(DependencyState.HARD_CONFLICT,counterReport.aggregateState());assertTrue(counterReport.diagnostics().toString(),counterReport.diagnostics().stream().anyMatch(d->"runtime.counter.wrong_node_type".equals(d.messageKey())));}

	private static void assertPair(V6RuleRuntimeBridge bridge,int rage,int focus){assertNotNull(bridge);assertEquals(rage,current(bridge,0));assertEquals(focus,current(bridge,1));}
	private static int current(V6RuleRuntimeBridge bridge,int index){StableId id=bridge.plan().resources().get(index).id();ResourceState state=bridge.state().resources().get(new ResourceRef(id,""));assertNotNull(state);return state.current();}
}
