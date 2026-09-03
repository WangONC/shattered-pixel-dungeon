package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.SkillCompiler;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.GameplayEventContext;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.ResourceRuntimeStateFactory;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.RuntimeExecutionContext;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.SkillExecutionResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.V6RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalRuntimeCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import com.shatteredpixel.shatteredpixeldungeon.ui.ClassResourceHUD;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Emits P04 evidence only when the gate runner supplies P04_ARTIFACT_DIR. */
public class P04DeliveryArtifactTest extends P04RuntimeTestBase {
	@Test public void blankBuilderConvertEmitsCanonicalRuntimeAndTraceArtifacts() throws Exception {
		PlayerBuildSession session=P04TestBuilds.classConvert("p04-delivery-artifacts");
		ClassBuildSpec build=session.finalizeOrThrow();
		ClassCompilePlan plan=new SkillCompiler(EffectExecutorRegistry.standard()).compile(build);
		assertTrue(plan.diagnostics().toString(),plan.executable());
		ClassRuntimeState before=ResourceRuntimeStateFactory.initialize(plan);
		StableId operation=plan.operations().get(0).id();
		GameplayEventContext event=new GameplayEventContext(404,0,GameplayEventContext.RuleEventType.ACTIVE,
				hero.id(),hero.id(),hero.id(),hero.pos,hero.pos,hero.pos,operation);
		V6RuleRuntime runtime=new V6RuleRuntime(plan,EffectExecutorRegistry.standard(),before);
		SkillExecutionResult result=runtime.executeOperation(operation,new RuntimeExecutionContext(event,damageGateway()));
		assertEquals(SkillExecutionResult.Status.APPLIED,result.status());
		String hud=ClassResourceHUD.readout(plan,runtime.state());
		assertTrue(hud,hud.contains("Rage 3/10"));
		assertTrue(hud,hud.contains("Focus 5/5"));

		writeArtifact("P04_RAGE_FOCUS_BUILDER_COMMAND_TRACE.txt",session.saveTrace());
		writeArtifact("P04_RAGE_FOCUS_CANONICAL_BUILD.json",new CanonicalBuildCodec().serialize(build));
		writeArtifact("P04_RAGE_FOCUS_RUNTIME_BEFORE.json",new CanonicalRuntimeCodec().serialize(before));
		writeArtifact("P04_RAGE_FOCUS_RUNTIME_AFTER.json",new CanonicalRuntimeCodec().serialize(runtime.state()));
		writeArtifact("P04_RAGE_FOCUS_RUNTIME_TRACE.txt",result.trace().serialize());
		writeArtifact("P04_RESOURCE_HUD.txt",hud);
	}

	private static void writeArtifact(String name,String value) throws Exception {
		String root=System.getenv("P04_ARTIFACT_DIR");
		if(root==null||root.isEmpty())return;
		Path dir=Paths.get(root);Files.createDirectories(dir);
		Files.write(dir.resolve(name),value.getBytes(StandardCharsets.UTF_8));
	}
}
