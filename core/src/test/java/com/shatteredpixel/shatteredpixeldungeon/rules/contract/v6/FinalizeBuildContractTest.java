package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class FinalizeBuildContractTest {
	@Test public void unexposedDeferredSectionsDoNotRejectOtherwiseFinalizableBuild() {
		DeterministicIdGenerator ids=new DeterministicIdGenerator("p02-final-ok");
		ContractNodeSpec action=new ContractNodeSpec(ids.nextId("component"),DisplayName.of("Test Action"),ContractNodeSpec.NodeKind.COMPONENT,"TEST_ONLY_ACTION",ImplementationState.IMPLEMENTED);
		ClassBuildSpec build=ClassBuildSpec.builder(ids.nextId("build"),DisplayName.of("Finalizable")).addClassComponent(action).budgetMetadata(new BudgetMetadata("test",1)).build();
		PlayerBuildSession session=PlayerBuildSession.fromDraft(ids,build);session.dispatch(new BuilderCommand.FinalizeBuild());
		assertTrue(session.state().finalization().diagnostics().toString(),session.state().finalization().allowed());
		assertEquals(ImplementationState.DEFERRED,session.state().draft().startingKit().state());
		assertEquals(ImplementationState.DEFERRED,session.state().draft().progression().state());
	}

	@Test public void unresolvedHardConflictOverBudgetAndPlayerExposedUnsupportedAllReject() {
		DeterministicIdGenerator unresolvedIds=new DeterministicIdGenerator("p02-final-unresolved");
		ContractNodeSpec action=action(unresolvedIds,ImplementationState.IMPLEMENTED,"ACTION");
		ModeSpec mode=new ModeSpec(unresolvedIds.nextId("mode"),DisplayName.of("Lost Mode"),new ModeGroupRef(unresolvedIds.nextId("modegrp"),"Missing"),false,ModeSpec.ModeDurationPolicy.PERSISTENT,0);
		assertRejected(ClassBuildSpec.builder(unresolvedIds.nextId("build"),DisplayName.of("Unresolved")).addClassComponent(action).addMode(mode).build(),unresolvedIds,"dependency:");

		DeterministicIdGenerator conflictIds=new DeterministicIdGenerator("p02-final-conflict");
		ContractNodeSpec first=action(conflictIds,ImplementationState.IMPLEMENTED,"A");
		ContractNodeSpec duplicate=new ContractNodeSpec(first.id(),DisplayName.of("Duplicate"),ContractNodeSpec.NodeKind.COMPONENT,"B",ImplementationState.IMPLEMENTED);
		assertRejected(ClassBuildSpec.builder(conflictIds.nextId("build"),DisplayName.of("Conflict")).addClassComponent(first).addClassComponent(duplicate).build(),conflictIds,"dependency:");

		DeterministicIdGenerator budgetIds=new DeterministicIdGenerator("p02-final-budget");
		ClassBuildSpec expensive=ClassBuildSpec.builder(budgetIds.nextId("build"),DisplayName.of("Expensive")).addClassComponent(action(budgetIds,ImplementationState.IMPLEMENTED,"ACTION")).budgetMetadata(new BudgetMetadata("test",1)).build();
		BuilderBudgetPolicy over=new BuilderBudgetPolicy(){@Override public BuilderBudgetLedger evaluate(ClassBuildSpec draft){return new BuilderBudgetLedger("test",2,1);}};
		PlayerBuildSession budget=PlayerBuildSession.fromDraft(budgetIds,expensive,over);budget.dispatch(new BuilderCommand.FinalizeBuild());assertTrue(budget.state().finalization().diagnostics().stream().anyMatch(v->v.startsWith("budget.over_limit")));

		DeterministicIdGenerator exposedIds=new DeterministicIdGenerator("p02-final-exposed");
		ClassBuildSpec exposed=ClassBuildSpec.builder(exposedIds.nextId("build"),DisplayName.of("Exposed")).addClassComponent(action(exposedIds,ImplementationState.PLAYER_EXPOSED,"NO_EXECUTOR")).build();
		assertRejected(exposed,exposedIds,"runtime.unsupported_player_exposed");
	}

	@Test public void p02CommandsNeverPromoteDeferredNodesToImplemented() {
		PlayerBuildSession session=PlayerBuildSession.empty(new DeterministicIdGenerator("p02-no-promotion"));
		session.dispatch(new BuilderCommand.CreateClassComponent("Core","P02_DEFERRED"));session.dispatch(new BuilderCommand.CreateSkill("Skill","P02_DEFERRED"));
		assertEquals(ImplementationState.DEFERRED,session.state().draft().classComponents().get(0).implementationState());
		assertEquals(ImplementationState.DEFERRED,session.state().draft().skills().get(0).implementationState());
		session.dispatch(new BuilderCommand.FinalizeBuild());assertFalse(session.state().finalization().allowed());assertTrue(session.state().finalization().diagnostics().contains("finalization.no_gameplay_action"));
	}

	private static ContractNodeSpec action(DeterministicIdGenerator ids,ImplementationState state,String variant){return new ContractNodeSpec(ids.nextId("component"),DisplayName.of("Action"),ContractNodeSpec.NodeKind.COMPONENT,variant,state);}
	private static void assertRejected(ClassBuildSpec build,DeterministicIdGenerator ids,String prefix){PlayerBuildSession session=PlayerBuildSession.fromDraft(ids,build);session.dispatch(new BuilderCommand.FinalizeBuild());assertFalse(session.state().finalization().allowed());assertTrue(session.state().finalization().diagnostics().toString(),session.state().finalization().diagnostics().stream().anyMatch(v->v.startsWith(prefix)));}
}
