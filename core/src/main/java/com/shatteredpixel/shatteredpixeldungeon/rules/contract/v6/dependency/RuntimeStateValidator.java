package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ContractNodeSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only validation of every P01 runtime reference and authoritative runtime key. */
public final class RuntimeStateValidator {
	/** List form exists so duplicate entries can be diagnosed before a Map would collapse them. */
	public static final class NodeCounterValue {
		private final StableId nodeId;
		private final Integer value;
		public NodeCounterValue(StableId nodeId, Integer value) { this.nodeId = nodeId; this.value = value; }
		public StableId nodeId() { return nodeId; }
		public Integer value() { return value; }
	}

	public DependencyReport validate(ClassBuildSpec build, ClassRuntimeState runtime) {
		if (build == null || runtime == null) throw new IllegalArgumentException("build and runtime are required");
		List<DependencyDiagnostic> result = new ArrayList<>();
		if (!build.buildId().equals(runtime.buildId())) {
			result.add(diagnostic(build.buildId(), "runtime.build_id", DependencyState.HARD_CONFLICT,
					runtime.buildId(), "runtime.build_id_mismatch"));
		}
		List<DependencyRequest> references = new ArrayList<>();
		int resourceIndex = 0;
		for (ResourceState value : runtime.resources().values()) {
			String path = "runtime.resources[" + resourceIndex++ + "]";
			references.add(new DependencyRequest(build.buildId(), path + ".resource", value.resource()));
			duplicateLongIds(result, build.buildId(), path + ".reservations", value.resource().targetId(),
					value.reservations().stream().map(ResourceState.Reservation::reservationId).toArray(Long[]::new));
			duplicateLongIds(result, build.buildId(), path + ".suppressions", value.resource().targetId(),
					value.suppressions().stream().map(ResourceState.Suppression::suppressionId).toArray(Long[]::new));
		}
		Set<String> modes = new HashSet<>();
		for (int i = 0; i < runtime.modes().size(); i++) {
			ModeState value = runtime.modes().get(i);
			String path = "runtime.modes[" + i + "]";
			references.add(new DependencyRequest(build.buildId(), path + ".mode", value.mode()));
			if (!modes.add(value.mode().targetId().value() + "@" + value.subjectActorId()))
				result.add(diagnostic(build.buildId(), path, DependencyState.HARD_CONFLICT,
						value.mode().targetId(), "runtime.duplicate_mode_state"));
		}
		Set<String> marks = new HashSet<>();
		for (int i = 0; i < runtime.marks().size(); i++) {
			MarkState value = runtime.marks().get(i);
			String path = "runtime.marks[" + i + "]";
			references.add(new DependencyRequest(build.buildId(), path + ".mark", value.mark()));
			if (!marks.add(value.mark().targetId().value() + "@" + value.subjectActorId()))
				result.add(diagnostic(build.buildId(), path, DependencyState.HARD_CONFLICT,
						value.mark().targetId(), "runtime.duplicate_mark_state"));
		}
		Set<Long> entities = new HashSet<>();
		for (int i = 0; i < runtime.entities().size(); i++) {
			EntityInstanceState value = runtime.entities().get(i);
			String path = "runtime.entities[" + i + "]";
			references.add(new DependencyRequest(build.buildId(), path + ".blueprint", value.blueprint()));
			if (!entities.add(value.instanceId())) result.add(diagnostic(build.buildId(), path + ".instance_id",
					DependencyState.HARD_CONFLICT, value.blueprint().targetId(), "runtime.duplicate_entity_instance"));
			duplicateOpaque(result, build.buildId(), path + ".capability_states", value.capabilityStates());
		}
		Set<StableId> properties = new HashSet<>();
		for (int i = 0; i < runtime.properties().size(); i++) {
			PropertyInventoryState value = runtime.properties().get(i);
			String path = "runtime.properties[" + i + "]";
			references.add(new DependencyRequest(build.buildId(), path + ".property", value.property()));
			if (!properties.add(value.property().targetId())) result.add(diagnostic(build.buildId(), path,
					DependencyState.HARD_CONFLICT, value.property().targetId(), "runtime.duplicate_property_state"));
		}
		duplicateOpaque(result, build.buildId(), "runtime.scheduled_payloads", runtime.scheduledPayloads());
		duplicateOpaque(result, build.buildId(), "runtime.attachments", runtime.attachments());
		duplicateOpaque(result, build.buildId(), "runtime.snapshots", runtime.snapshots());
		duplicateOpaque(result, build.buildId(), "runtime.learned_abilities", runtime.learnedAbilities());
		result.addAll(validateNodeCounters(build, "runtime.cooldowns", counterValues(runtime.cooldowns())).diagnostics());
		result.addAll(validateNodeCounters(build, "runtime.uses_this_floor", counterValues(runtime.usesThisFloor())).diagnostics());
		result.addAll(new DependencyResolver().resolveReferences(build, references).diagnostics());
		return new DependencyReport(result);
	}

	public DependencyReport validateNodeCounters(ClassBuildSpec build, String fieldPath,
			List<NodeCounterValue> values) {
		if (build == null || fieldPath == null || fieldPath.isEmpty() || values == null) {
			throw new IllegalArgumentException("build, counter path, and values are required");
		}
		List<DependencyDiagnostic> result = new ArrayList<>();
		Set<StableId> seen = new HashSet<>();
		for (int i = 0; i < values.size(); i++) {
			NodeCounterValue value = values.get(i);
			String path = fieldPath + "[" + i + "]";
			if (value == null || value.nodeId() == null) {
				result.add(diagnostic(build.buildId(), path + ".node_id", DependencyState.HARD_CONFLICT,
						build.buildId(), "runtime.counter.invalid_node_id_type"));
				continue;
			}
			if (!seen.add(value.nodeId())) result.add(diagnostic(build.buildId(), path + ".node_id",
					DependencyState.HARD_CONFLICT, value.nodeId(), "runtime.counter.duplicate_node_id"));
			if (value.value() == null || value.value() < 0) result.add(diagnostic(build.buildId(), path + ".value",
					DependencyState.HARD_CONFLICT, value.nodeId(), "runtime.counter.invalid_value_type_or_range"));
			StableTarget target = find(build, value.nodeId());
			if (target == null) result.add(diagnostic(build.buildId(), path + ".node_id", DependencyState.UNRESOLVED,
					value.nodeId(), "runtime.counter.node_missing"));
			else if (!(target instanceof ContractNodeSpec)
					|| (((ContractNodeSpec) target).nodeKind() != ContractNodeSpec.NodeKind.SKILL
					&& ((ContractNodeSpec) target).nodeKind() != ContractNodeSpec.NodeKind.OPERATION)) {
				result.add(diagnostic(build.buildId(), path + ".node_id", DependencyState.HARD_CONFLICT,
						value.nodeId(), "runtime.counter.wrong_node_type"));
			}
		}
		return new DependencyReport(result);
	}

	private static List<NodeCounterValue> counterValues(Map<StableId, Integer> values) {
		List<NodeCounterValue> result = new ArrayList<>();
		for (Map.Entry<StableId, Integer> value : values.entrySet()) {
			result.add(new NodeCounterValue(value.getKey(), value.getValue()));
		}
		return result;
	}

	private static StableTarget find(ClassBuildSpec build, StableId id) {
		for (StableTarget target : build.allTargets()) if (target.id().equals(id)) return target;
		return null;
	}

	private static void duplicateLongIds(List<DependencyDiagnostic> result, StableId owner, String path,
			StableId target, Long[] values) {
		Set<Long> seen = new HashSet<>();
		for (int i = 0; i < values.length; i++) if (!seen.add(values[i]))
			result.add(diagnostic(owner, path + "[" + i + "]", DependencyState.HARD_CONFLICT,
					target, "runtime.duplicate_nested_state_id"));
	}

	private static void duplicateOpaque(List<DependencyDiagnostic> result, StableId owner, String path,
			List<OpaqueRuntimeState> values) {
		Set<StableId> seen = new HashSet<>();
		for (int i = 0; i < values.size(); i++) {
			OpaqueRuntimeState value = values.get(i);
			if (!seen.add(value.stateId())) result.add(diagnostic(owner, path + "[" + i + "].state_id",
					DependencyState.HARD_CONFLICT, value.stateId(), "runtime.duplicate_opaque_state"));
			if (value.implementationState() == com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState.DEFERRED
					|| value.implementationState() == com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState.UNSUPPORTED)
				result.add(diagnostic(owner, path + "[" + i + "].implementation_state", DependencyState.UNSUPPORTED,
						value.stateId(), "runtime.node_unsupported"));
		}
	}

	private static DependencyDiagnostic diagnostic(StableId owner, String path, DependencyState state,
			StableId target, String key) {
		return new DependencyDiagnostic(owner, path, state, target, key);
	}
}
