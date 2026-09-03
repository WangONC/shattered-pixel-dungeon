package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.ClassBuildValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * P03 executable admission for the complete authored build.
 *
 * StartingKit and Progression are deliberately absent: their current types are empty,
 * unexposed section markers, not authored gameplay nodes. Every populated gameplay
 * collection other than skills is rejected until its runtime node is implemented.
 */
public final class ExecutableBuildAdmissionPolicy {
	public List<CompileDiagnostic> evaluate(ClassBuildSpec build) {
		if (build == null) throw new IllegalArgumentException("build is required");
		List<CompileDiagnostic> result = new ArrayList<>();
		for (DependencyDiagnostic diagnostic : new ClassBuildValidator().validate(build).diagnostics()) {
			// Explicit collection admission below reports actual unsupported nodes. This also
			// excludes the empty StartingKit/Progression placeholder diagnostics.
			if (!"node.unsupported".equals(diagnostic.messageKey())) result.add(toCompile(diagnostic));
		}
		rejectRuntimeMissing(result, "resources", build.resources());
		rejectRuntimeMissing(result, "marks", build.marks());
		rejectRuntimeMissing(result, "mode_groups", build.modeGroups());
		rejectRuntimeMissing(result, "modes", build.modes());
		rejectRuntimeMissing(result, "capacities", build.capacities());
		rejectRuntimeMissing(result, "entities", build.entities());
		rejectRuntimeMissing(result, "ability_pools", build.abilityPools());
		rejectRuntimeMissing(result, "properties", build.properties());
		rejectRuntimeMissing(result, "recipes", build.recipes());
		rejectRuntimeMissing(result, "class_components", build.classComponents());
		rejectRuntimeMissing(result, "class_constraints", build.classConstraints());
		rejectRuntimeMissing(result, "class_operations", build.classOperations());
		return Collections.unmodifiableList(result);
	}

	private static void rejectRuntimeMissing(List<CompileDiagnostic> out, String collection,
			List<? extends StableTarget> nodes) {
		for (int index = 0; index < nodes.size(); index++) {
			StableTarget node = nodes.get(index);
			String messageKey;
			if (node.implementationState() == ImplementationState.DEFERRED) {
				messageKey = "compile.runtime_node_deferred";
			} else if (node.implementationState() == ImplementationState.UNSUPPORTED) {
				messageKey = "compile.runtime_node_unsupported";
			} else {
				messageKey = "compile.runtime_node_not_implemented";
			}
			out.add(new CompileDiagnostic(node.id(), collection + "[" + index + "]",
					DependencyState.UNSUPPORTED, messageKey));
		}
	}

	private static CompileDiagnostic toCompile(DependencyDiagnostic diagnostic) {
		return new CompileDiagnostic(diagnostic.ownerNodeId(), diagnostic.fieldPath(),
				diagnostic.state(), diagnostic.messageKey());
	}
}
