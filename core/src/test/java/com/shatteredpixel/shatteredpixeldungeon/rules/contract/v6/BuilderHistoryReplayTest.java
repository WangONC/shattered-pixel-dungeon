package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalDeepEquivalence;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeGroupSpec;
import org.junit.Test;

import static org.junit.Assert.*;

public class BuilderHistoryReplayTest {
	@Test public void undoRedoSaveLoadAndSerializedReplayAreCanonical() {
		String seed="p02-replay";
		PlayerBuildSession session=PlayerBuildSession.empty(new DeterministicIdGenerator(seed));
		session.dispatch(new BuilderCommand.CreateResource("Rage"));
		session.dispatch(new BuilderCommand.CreateResource("Focus"));
		session.dispatch(new BuilderCommand.CreateMark("灼痕"));
		session.dispatch(new BuilderCommand.CreateModeGroup("姿态"));
		ModeGroupSpec group=session.state().draft().modeGroups().get(0);
		session.dispatch(new BuilderCommand.CreateMode("防守",new ModeGroupRef(group.id(),group.displayName().text())));
		session.dispatch(new BuilderCommand.CreateMode("进攻",new ModeGroupRef(group.id(),group.displayName().text())));
		session.dispatch(new BuilderCommand.SaveDraft("checkpoint"));
		String resourceId=session.state().draft().resources().get(0).id().value();
		session.dispatch(new BuilderCommand.RenameDeclaration(resourceId,"怒气"));
		assertEquals("怒气",session.state().draft().resources().get(0).displayName().text());
		session.dispatch(new BuilderCommand.Undo());
		assertEquals("Rage",session.state().draft().resources().get(0).displayName().text());
		session.dispatch(new BuilderCommand.Redo());
		assertEquals("怒气",session.state().draft().resources().get(0).displayName().text());
		session.dispatch(new BuilderCommand.LoadDraft("checkpoint"));
		assertEquals("Rage",session.state().draft().resources().get(0).displayName().text());
		String trace=session.saveTrace();
		PlayerBuildSession replay=BuilderCommandTrace.deserialize(trace).replay(new DeterministicIdGenerator(seed));
		assertEquals(new CanonicalBuildCodec().serialize(session.state().draft()),new CanonicalBuildCodec().serialize(replay.state().draft()));
		assertTrue(new CanonicalDeepEquivalence().equivalent(session.state().draft(),replay.state().draft()));
	}
}
