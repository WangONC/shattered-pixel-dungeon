package com.shatteredpixel.shatteredpixeldungeon.headless;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.BuilderFormController;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import org.junit.Test;

import static org.junit.Assert.*;

/** UI controller and headless adapter must feed the identical command/reducer path. */
public class P03HeadlessTypedSkillParityTest {
	@Test public void uiAndHeadlessTypedSkillTransitionsAreCanonicalEquals() {
		String seed="p03-headless-parity";PlayerBuildSession ui=PlayerBuildSession.empty(new DeterministicIdGenerator(seed));HeadlessPlayerBuildAdapter headless=HeadlessPlayerBuildAdapter.empty(new DeterministicIdGenerator(seed));
		BuilderCommand create=new BuilderCommand.CreateSkill("Parity Strike");ui.dispatch(create);headless.dispatch(create);String id=ui.state().draft().skills().get(0).id().value();assertEquals(id,headless.state().draft().skills().get(0).id().value());
		BuilderFormController form=new BuilderFormController(ui);apply(form,headless,id,"activation_variant","ACTIVE");apply(form,headless,id,"condition_variant","ALWAYS");apply(form,headless,id,"effect_primary_family","DAMAGE");apply(form,headless,id,"effect_primary_variant","DIRECT_DAMAGE");apply(form,headless,id,"effect_primary_amount","2");apply(form,headless,id,"delivery_variant","DIRECT");apply(form,headless,id,"targeting_selector","SELECTED_ACTOR");apply(form,headless,id,"targeting_coverage","SINGLE");apply(form,headless,id,"targeting_filter","RELATION_ENEMY_EXCLUDE_SELF");apply(form,headless,id,"targeting_range","2");apply(form,headless,id,"modifier_variant","NONE");apply(form,headless,id,"cost_variant","NO_COST");apply(form,headless,id,"constraint_variant","NONE");
		CanonicalBuildCodec codec=new CanonicalBuildCodec();assertEquals(codec.serialize(ui.state().draft()),codec.serialize(headless.state().draft()));assertEquals(com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState.IMPLEMENTED,headless.state().draft().skills().get(0).implementationState());
		String replay=codec.serialize(BuilderCommandTrace.deserialize(headless.saveTrace()).replay(new DeterministicIdGenerator(seed)).state().draft());assertEquals(codec.serialize(headless.state().draft()),replay);
	}
	private static void apply(BuilderFormController form,HeadlessPlayerBuildAdapter headless,String id,String field,String value){BuilderCommand command=form.commandForValue(id,field,value);form.dispatch(command);headless.dispatch(command);}
}
