package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.SkillCompiler;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/** Layer B: the full player command trace starts from an empty session and compiles after save/load. */
public class P03PlayerBuilderVerticalSliceTest {
	@Test public void blankBuilderSaveLoadFinalizeCompileAndReplayAreDeterministic() throws Exception {
		String seed="p03-player-vertical";PlayerBuildSession session=P03TestBuilds.directDamage(seed,7,false,true);
		String skillId=session.state().draft().skills().get(0).id().value();
		session.dispatch(new BuilderCommand.SaveDraft("p03"));
		session.dispatch(new BuilderCommand.SetTypedSkillField(skillId,"effects.primary","DIRECT_DAMAGE","amount","9"));
		session.dispatch(new BuilderCommand.LoadDraft("p03"));
		ClassBuildSpec finalized=session.finalizeOrThrow();
		ClassCompilePlan plan=new SkillCompiler(EffectExecutorRegistry.standard()).compile(finalized);
		assertTrue(plan.diagnostics().toString(),plan.ready());assertEquals(1,plan.skills().size());
		String trace=session.saveTrace();
		PlayerBuildSession replay=BuilderCommandTrace.deserialize(trace).replay(new DeterministicIdGenerator(seed));
		assertEquals(new CanonicalBuildCodec().serialize(session.state().draft()),new CanonicalBuildCodec().serialize(replay.state().draft()));
		assertTrue(trace.contains("SelectEffectVariant"));assertTrue(trace.contains("SetTargetingFilter"));assertTrue(trace.contains("FinalizeBuild"));
		writeArtifact("P03_BUILDER_COMMAND_TRACE.txt",trace);writeArtifact("P03_CANONICAL_BUILD.json",new CanonicalBuildCodec().serialize(finalized));
	}
	private static void writeArtifact(String name,String value)throws Exception{String root=System.getenv("P03_ARTIFACT_DIR");if(root==null||root.isEmpty())return;Path dir=Paths.get(root);Files.createDirectories(dir);Files.write(dir.resolve(name),value.getBytes(StandardCharsets.UTF_8));}
}
