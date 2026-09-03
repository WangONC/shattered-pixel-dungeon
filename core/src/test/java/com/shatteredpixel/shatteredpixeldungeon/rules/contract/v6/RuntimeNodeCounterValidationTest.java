package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.RuntimeStateValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalLoadResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalRuntimeCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class RuntimeNodeCounterValidationTest {
	@Test public void missingDuplicateInvalidAndWrongNodeIdsProduceExplicitDiagnostics() {
		P01Fixtures f = new P01Fixtures();
		RuntimeStateValidator validator = new RuntimeStateValidator();
		StableId missing = StableId.fromStored("skill:missing-runtime-node");
		DependencyReport report = validator.validateNodeCounters(f.build(), "runtime.cooldowns", Arrays.asList(
				new RuntimeStateValidator.NodeCounterValue(missing, 1),
				new RuntimeStateValidator.NodeCounterValue(f.skill.id(), 2),
				new RuntimeStateValidator.NodeCounterValue(f.skill.id(), 3),
				new RuntimeStateValidator.NodeCounterValue(null, 1),
				new RuntimeStateValidator.NodeCounterValue(f.resource.id(), 1),
				new RuntimeStateValidator.NodeCounterValue(f.operation.id(), -1)));
		assertDiagnostic(report, "runtime.counter.node_missing", DependencyState.UNRESOLVED);
		assertDiagnostic(report, "runtime.counter.duplicate_node_id", DependencyState.HARD_CONFLICT);
		assertDiagnostic(report, "runtime.counter.invalid_node_id_type", DependencyState.HARD_CONFLICT);
		assertDiagnostic(report, "runtime.counter.wrong_node_type", DependencyState.HARD_CONFLICT);
		assertDiagnostic(report, "runtime.counter.invalid_value_type_or_range", DependencyState.HARD_CONFLICT);
	}

	@Test public void bothRuntimeMapsAcceptOnlySkillOrOperationNodes() {
		P01Fixtures f = new P01Fixtures();
		DependencyReport report = new RuntimeStateValidator().validate(f.build(), f.runtime());
		assertFalse(report.diagnostics().stream().anyMatch(d -> d.messageKey().startsWith("runtime.counter.")));
	}

	@Test public void canonicalReaderRejectsDuplicateAndInvalidCounterEntriesInsteadOfCollapsingThem() {
		P01Fixtures f = new P01Fixtures();
		CanonicalRuntimeCodec codec = new CanonicalRuntimeCodec();
		String baseline = codec.serialize(f.runtime());
		String entry = "{\"node_id\":\"" + f.skill.id().value() + "\",\"value\":4}";
		String duplicate = baseline.replace("\"cooldowns\":[" + entry + "]",
				"\"cooldowns\":[" + entry + "," + entry + "]");
		CanonicalLoadResult<ClassRuntimeState> duplicateLoad = codec.deserialize(duplicate);
		assertEquals(DependencyState.HARD_CONFLICT, duplicateLoad.state());
		assertNull(duplicateLoad.value());
		assertTrue(duplicateLoad.diagnostics().toString().contains("duplicate node ID"));

		String wrongValue = baseline.replace(entry,
				"{\"node_id\":\"" + f.skill.id().value() + "\",\"value\":\"four\"}");
		CanonicalLoadResult<ClassRuntimeState> valueLoad = codec.deserialize(wrongValue);
		assertEquals(DependencyState.HARD_CONFLICT, valueLoad.state());
		assertNull(valueLoad.value());
		assertTrue(valueLoad.diagnostics().toString().contains("must be an integer number"));
	}

	private static void assertDiagnostic(DependencyReport report, String key, DependencyState state) {
		DependencyDiagnostic found = report.diagnostics().stream()
				.filter(d -> key.equals(d.messageKey()) && state == d.state()).findFirst().orElse(null);
		assertNotNull(key + " missing from diagnostics", found);
		assertTrue(found.fieldPath().startsWith("runtime.cooldowns["));
	}
}
