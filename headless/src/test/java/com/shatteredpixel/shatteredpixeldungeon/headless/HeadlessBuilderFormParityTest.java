package com.shatteredpixel.shatteredpixeldungeon.headless;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.BuilderFormController;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Behavioral proof that the form UI and headless adapter execute the same reducer command. */
public class HeadlessBuilderFormParityTest {
	@Test public void formAndHeadlessPathsHaveIdenticalReducerTransitions() {
		String seed = "p02-r1-ui-headless";
		PlayerBuildSession uiSession = PlayerBuildSession.empty(new DeterministicIdGenerator(seed));
		HeadlessPlayerBuildAdapter headless = HeadlessPlayerBuildAdapter.empty(new DeterministicIdGenerator(seed));
		BuilderCommand create = new BuilderCommand.CreateResource("Rage");
		uiSession.dispatch(create);
		headless.dispatch(create);

		BuilderFormController form = new BuilderFormController(uiSession);
		String resourceId = uiSession.state().draft().resources().get(0).id().value();
		String nextMaximum = form.form(resourceId).requireField("maximum").suggestedValue();
		BuilderCommand edit = form.commandForValue(resourceId, "maximum", nextMaximum);
		form.dispatchValue(resourceId, "maximum", nextMaximum);
		headless.dispatch(edit);

		CanonicalBuildCodec codec = new CanonicalBuildCodec();
		assertEquals(codec.serialize(uiSession.state().draft()), codec.serialize(headless.state().draft()));
		form.dispatch(new BuilderCommand.Undo());
		headless.dispatch(new BuilderCommand.Undo());
		assertEquals(codec.serialize(uiSession.state().draft()), codec.serialize(headless.state().draft()));
		form.dispatch(new BuilderCommand.Redo());
		headless.dispatch(new BuilderCommand.Redo());
		assertEquals(codec.serialize(uiSession.state().draft()), codec.serialize(headless.state().draft()));
	}
}
