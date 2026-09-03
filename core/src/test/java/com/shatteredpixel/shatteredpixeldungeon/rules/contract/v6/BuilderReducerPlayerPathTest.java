package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.CapacityRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class BuilderReducerPlayerPathTest {
	@Test public void commandsCreateAndEditEveryP02DeclarationWithoutFinalObjects() {
		PlayerBuildSession session=PlayerBuildSession.empty(new DeterministicIdGenerator("p02-all-declarations"));
		session.dispatch(new BuilderCommand.CreateResource("Rage"));
		session.dispatch(new BuilderCommand.CreateMark("灼痕"));
		session.dispatch(new BuilderCommand.CreateModeGroup("Stance"));
		ModeGroupSpec group=session.state().draft().modeGroups().get(0);
		session.dispatch(new BuilderCommand.CreateMode("Guard",new ModeGroupRef(group.id(),group.displayName().text())));
		session.dispatch(new BuilderCommand.CreateEntityCapacity("Devices","DEVICE,TRAP",2,"REJECT_NEW"));
		EntityCapacitySpec capacity=session.state().draft().capacities().get(0);
		session.dispatch(new BuilderCommand.CreateEntity("Turret","DEVICE",new CapacityRef(capacity.id(),capacity.displayName().text())));
		session.dispatch(new BuilderCommand.CreateAbilityPool("Observed"));
		session.dispatch(new BuilderCommand.CreateProperty("Ember"));
		session.dispatch(new BuilderCommand.CreateRecipe("Forge Ember","P02_DEFERRED_OUTPUT"));
		session.dispatch(new BuilderCommand.CreateClassComponent("Core","P02_DEFERRED_COMPONENT"));
		session.dispatch(new BuilderCommand.CreateClassConstraint("Vow","P02_DEFERRED_CONSTRAINT"));
		session.dispatch(new BuilderCommand.CreateClassOperation("Switch","P02_DEFERRED_OPERATION"));
		session.dispatch(new BuilderCommand.CreateSkill("Strike","P02_DEFERRED_SKILL"));
		ClassBuildSpec build=session.state().draft();
		assertEquals(13,build.allTargets().size());

		session.dispatch(new BuilderCommand.EditResourceField(build.resources().get(0).id().value(),"maximum","25"));
		session.dispatch(new BuilderCommand.EditMarkField(build.marks().get(0).id().value(),"kind","STACK"));
		session.dispatch(new BuilderCommand.SetFieldValue(group.id().value(),"MODE_GROUP","policy","INDEPENDENT"));
		session.dispatch(new BuilderCommand.SetFieldValue(build.modes().get(0).id().value(),"MODE","initial","true"));
		session.dispatch(new BuilderCommand.SetFieldValue(capacity.id().value(),"ENTITY_CAPACITY","maximum","4"));
		session.dispatch(new BuilderCommand.SetFieldValue(build.entities().get(0).id().value(),"ENTITY","entity_type","TRAP"));
		session.dispatch(new BuilderCommand.SetFieldValue(build.abilityPools().get(0).id().value(),"ABILITY_POOL","capacity","3"));
		session.dispatch(new BuilderCommand.SetFieldValue(build.properties().get(0).id().value(),"PROPERTY","maximum_stack","12"));
		session.dispatch(new BuilderCommand.SetFieldValue(build.recipes().get(0).id().value(),"RECIPE","output_variant","P02_OTHER_DEFERRED_OUTPUT"));
		session.dispatch(new BuilderCommand.SetFieldValue(build.classComponents().get(0).id().value(),"CONTRACT_NODE","variant_key","P02_RENAMED_DEFERRED_COMPONENT"));
		assertTrue(session.state().commandDiagnostics().toString(),session.state().commandDiagnostics().isEmpty());
		assertEquals(25,session.state().draft().resources().get(0).maximum());
		assertEquals(MarkSpec.MarkKind.STACK,session.state().draft().marks().get(0).kind());
		assertEquals(EntityType.TRAP,session.state().draft().entities().get(0).type());
		assertEquals(ImplementationState.DEFERRED,session.state().draft().skills().get(0).implementationState());
	}

	@Test public void deleteSameNameAndExplicitRebindRemainStrictlyIdBased() {
		PlayerBuildSession session=PlayerBuildSession.empty(new DeterministicIdGenerator("p02-rebind"));
		session.dispatch(new BuilderCommand.CreateModeGroup("姿态"));
		ModeGroupSpec original=session.state().draft().modeGroups().get(0);
		session.dispatch(new BuilderCommand.CreateMode("防守",new ModeGroupRef(original.id(),original.displayName().text())));
		String modeId=session.state().draft().modes().get(0).id().value();
		session.dispatch(new BuilderCommand.DeleteDeclaration(original.id().value()));
		assertTrue(session.state().dependencies().diagnostics().stream().anyMatch(d->d.state()==DependencyState.UNRESOLVED));
		assertEquals(original.id(),session.state().draft().modes().get(0).group().targetId());
		session.dispatch(new BuilderCommand.CreateModeGroup("姿态"));
		ModeGroupSpec replacement=session.state().draft().modeGroups().get(0);
		assertNotEquals(original.id(),replacement.id());
		assertEquals(original.id(),session.state().draft().modes().get(0).group().targetId());
		session.dispatch(new BuilderCommand.RebindReference(modeId,"group",new ModeGroupRef(replacement.id(),replacement.displayName().text())));
		assertEquals(replacement.id(),session.state().draft().modes().get(0).group().targetId());
		assertFalse(session.state().dependencies().diagnostics().stream().anyMatch(d->d.state()==DependencyState.UNRESOLVED));
	}

	@Test public void invalidFieldOrVariantDoesNotMutateDraft() {
		PlayerBuildSession session=PlayerBuildSession.empty(new DeterministicIdGenerator("p02-invalid"));
		session.dispatch(new BuilderCommand.CreateResource("Rage"));
		String before=new com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec().serialize(session.state().draft());
		String id=session.state().draft().resources().get(0).id().value();
		session.dispatch(new BuilderCommand.SetFieldValue(id,"RESOURCE","not_a_field","9"));
		assertFalse(session.state().commandDiagnostics().isEmpty());
		assertEquals(before,new com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec().serialize(session.state().draft()));
	}
}
