package com.shatteredpixel.shatteredpixeldungeon.headless;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommandTrace;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import org.junit.Test;

import static org.junit.Assert.*;

public class HeadlessPlayerBuildAdapterTest {
	@Test public void adapterUsesTheSameCommandSessionAndProducesReplayableTrace(){String seed="p02-headless";HeadlessPlayerBuildAdapter adapter=HeadlessPlayerBuildAdapter.empty(new DeterministicIdGenerator(seed));adapter.dispatch(new BuilderCommand.CreateResource("Rage"));adapter.dispatch(new BuilderCommand.EditResourceField(adapter.state().draft().resources().get(0).id().value(),"maximum","20"));String trace=adapter.saveTrace();assertTrue(trace.startsWith(BuilderCommandTrace.FORMAT));String expected=new CanonicalBuildCodec().serialize(adapter.state().draft());String replayed=new CanonicalBuildCodec().serialize(BuilderCommandTrace.deserialize(trace).replay(new DeterministicIdGenerator(seed)).state().draft());assertEquals(expected,replayed);}
}
