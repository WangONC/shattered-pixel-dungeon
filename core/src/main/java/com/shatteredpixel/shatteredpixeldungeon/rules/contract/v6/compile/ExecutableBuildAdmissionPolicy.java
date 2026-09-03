package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.ClassBuildValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.component.*;

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
		// ResourceSpec deliberately persists as DECLARED. P04 gives every resource
		// an immutable CompiledResource plus ResourceTransaction executor, so the
		// typed declaration is admitted without changing its canonical taxonomy.
		rejectRuntimeMissing(result, "marks", build.marks());
		rejectRuntimeMissing(result, "mode_groups", build.modeGroups());
		rejectRuntimeMissing(result, "modes", build.modes());
		rejectRuntimeMissing(result, "capacities", build.capacities());
		rejectRuntimeMissing(result, "entities", build.entities());
		rejectRuntimeMissing(result, "ability_pools", build.abilityPools());
		rejectRuntimeMissing(result, "properties", build.properties());
		rejectRuntimeMissing(result, "recipes", build.recipes());
		for(int i=0;i<build.classComponents().size();i++){StableTarget node=build.classComponents().get(i);if(!(node instanceof BasicAttackComponentSpec||node instanceof ResourceFlowComponentSpec||node instanceof ActiveResourceOperationComponentSpec)||node.implementationState()!=ImplementationState.IMPLEMENTED)reject(result,"class_components",i,node);}
		rejectRuntimeMissing(result, "class_constraints", build.classConstraints());
		for(int i=0;i<build.classOperations().size();i++){StableTarget node=build.classOperations().get(i);if(!(node instanceof ResourceClassOperationSpec)||node.implementationState()!=ImplementationState.IMPLEMENTED)reject(result,"class_operations",i,node);}
		return Collections.unmodifiableList(result);
	}
	private static void reject(List<CompileDiagnostic> out,String collection,int index,StableTarget node){rejectRuntimeMissing(out,collection,Collections.singletonList(node));}

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
