package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompileDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ExecutableBuildAdmissionPolicy;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.SkillCompiler;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ContractNodeSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntitySpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntityType;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeSpec;
import org.junit.Test;

import static org.junit.Assert.*;

public class P03R2CompileAdmissionTest {
	private final SkillCompiler compiler = new SkillCompiler(EffectExecutorRegistry.standard());

	@Test public void directDamageOnlyBuildIsExecutableAndEmptyDeferredSectionsDoNotBlock() {
		ClassBuildSpec direct = P03TestBuilds.directDamage("p03-r2-direct", 7, false, false).finalizeOrThrow();
		ClassCompilePlan executable = compiler.compile(direct);
		assertEquals(ClassCompilePlan.Kind.EXECUTABLE, executable.kind());
		assertTrue(executable.diagnostics().isEmpty());

		ClassBuildSpec placeholdersOnly = PlayerBuildSession.empty(
				new DeterministicIdGenerator("p03-r2-placeholders")).state().draft();
		assertTrue(new ExecutableBuildAdmissionPolicy().evaluate(placeholdersOnly).isEmpty());
		ClassCompilePlan noSkill = compiler.compile(placeholdersOnly);
		assertEquals(ClassCompilePlan.Kind.PARTIAL, noSkill.kind());
		assertTrue(has(noSkill, "skills", DependencyState.UNRESOLVED, "compile.no_skill"));
		assertFalse(noSkill.diagnostics().stream().anyMatch(value ->
				"starting_kit".equals(value.fieldPath()) || "progression".equals(value.fieldPath())));
	}

	@Test public void actualUnsupportedClassComponentIsDiagnosedAndRejected() {
		ClassBuildSpec base = P03TestBuilds.directDamage("p03-r2-component", 7, false, false).finalizeOrThrow();
		DeterministicIdGenerator ids = new DeterministicIdGenerator("p03-r2-component-node");
		ContractNodeSpec component = new ContractNodeSpec(ids.nextId("component"), DisplayName.of("Deferred component"),
				ContractNodeSpec.NodeKind.COMPONENT, "P04_COMPONENT", ImplementationState.UNSUPPORTED);
		ClassCompilePlan plan = compiler.compile(base.toBuilder().addClassComponent(component).build());
		assertRejected(plan, component.id().value(), "class_components[0]", "compile.runtime_node_unsupported");
	}

	@Test public void actualDeferredEntityIsDiagnosedAndRejected() {
		ClassBuildSpec base = P03TestBuilds.directDamage("p03-r2-entity", 7, false, false).finalizeOrThrow();
		DeterministicIdGenerator ids = new DeterministicIdGenerator("p03-r2-entity-node");
		EntitySpec entity = new EntitySpec(ids.nextId("entity"), DisplayName.of("Deferred entity"),
				EntityType.ACTOR, null, ImplementationState.DEFERRED);
		ClassCompilePlan plan = compiler.compile(base.toBuilder().addEntity(entity).build());
		assertRejected(plan, entity.id().value(), "entities[0]", "compile.runtime_node_deferred");
	}

	@Test public void unresolvedReferenceAndOwningRuntimeNodeAreBothDiagnosed() {
		ClassBuildSpec base = P03TestBuilds.directDamage("p03-r2-unresolved", 7, false, false).finalizeOrThrow();
		DeterministicIdGenerator ids = new DeterministicIdGenerator("p03-r2-unresolved-node");
		ModeSpec mode = new ModeSpec(ids.nextId("mode"), DisplayName.of("Orphan mode"),
				new ModeGroupRef(ids.nextId("modegrp"), "Missing group"), false,
				ModeSpec.ModeDurationPolicy.PERSISTENT, 0);
		ClassCompilePlan plan = compiler.compile(base.toBuilder().addMode(mode).build());
		assertFalse(plan.executable());
		assertTrue(has(plan, "modes[0]", DependencyState.UNSUPPORTED, "compile.runtime_node_not_implemented"));
		assertTrue(has(plan, "group", DependencyState.UNRESOLVED, "dependency.target_missing"));
	}

	private static void assertRejected(ClassCompilePlan plan, String ownerId, String path, String messageKey) {
		assertEquals(ClassCompilePlan.Kind.PARTIAL, plan.kind());
		assertFalse(plan.executable());
		assertTrue(plan.diagnostics().toString(), plan.diagnostics().stream().anyMatch(value ->
				ownerId.equals(value.skillId().value()) && path.equals(value.fieldPath())
						&& value.state() == DependencyState.UNSUPPORTED && messageKey.equals(value.messageKey())));
	}

	private static boolean has(ClassCompilePlan plan, String path, DependencyState state, String messageKey) {
		for (CompileDiagnostic value : plan.diagnostics()) {
			if (path.equals(value.fieldPath()) && state == value.state() && messageKey.equals(value.messageKey())) return true;
		}
		return false;
	}
}
