package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyResolver;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.edit.BuildEditService;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalLoadResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalRuntimeCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Emits review evidence from the real P01 codecs only when explicitly requested by the gate runner. */
public class P01DeliveryArtifactTest {
	@Test public void canonicalExamplesDiagnosticsAndLoadMatrixComeFromRealBehavior() throws IOException {
		P01Fixtures fixture = new P01Fixtures();
		CanonicalBuildCodec builds = new CanonicalBuildCodec();
		CanonicalRuntimeCodec runtime = new CanonicalRuntimeCodec();
		String buildJson = builds.serialize(fixture.build());
		String runtimeJson = runtime.serialize(fixture.runtime());
		assertEquals(DependencyState.RESOLVED, builds.deserialize(buildJson).state());
		assertEquals(DependencyState.RESOLVED, runtime.deserialize(runtimeJson).state());

		ClassBuildSpec deleted = new BuildEditService().delete(fixture.declarationsOnly(), fixture.resource.id());
		DependencyReport unresolved = new DependencyResolver().resolve(deleted);
		DependencyDiagnostic missing = unresolved.diagnostics().stream()
				.filter(value -> value.state() == DependencyState.UNRESOLVED)
				.findFirst().orElseThrow(AssertionError::new);
		assertTrue(missing.fieldPath().contains("resource"));

		String output = System.getenv("P01_ARTIFACT_DIR");
		if (output == null || output.isEmpty()) return;
		Path directory = Paths.get(output).toAbsolutePath().normalize();
		Files.createDirectories(directory);
		write(directory.resolve("canonical_build_spec_v6.json"), buildJson + "\n");
		write(directory.resolve("canonical_runtime_state_v6.json"), runtimeJson + "\n");
		write(directory.resolve("unresolved_diagnostic.json"), diagnosticJson(missing));
		write(directory.resolve("schema_version_load_matrix.json"), loadMatrix(builds, runtime, buildJson, runtimeJson));
	}

	private static String diagnosticJson(DependencyDiagnostic value) {
		return "{\n"
				+ "  \"owner_node_id\": \"" + escape(value.ownerNodeId().value()) + "\",\n"
				+ "  \"field_path\": \"" + escape(value.fieldPath()) + "\",\n"
				+ "  \"state\": \"" + value.state().name() + "\",\n"
				+ "  \"target_id\": \"" + escape(value.targetId().value()) + "\",\n"
				+ "  \"message_key\": \"" + escape(value.messageKey()) + "\"\n"
				+ "}\n";
	}

	private static String loadMatrix(CanonicalBuildCodec builds, CanonicalRuntimeCodec runtime,
			String buildJson, String runtimeJson) {
		List<String> rows = new ArrayList<>();
		rows.add(row("build", "schema_5", builds.deserialize(buildJson.replace("\"schema_version\":6", "\"schema_version\":5"))));
		rows.add(row("build", "schema_6", builds.deserialize(buildJson)));
		rows.add(row("build", "schema_7", builds.deserialize(buildJson.replace("\"schema_version\":6", "\"schema_version\":7"))));
		rows.add(row("build", "unknown_contract", builds.deserialize(buildJson.replace("\"contract_version\":\"0.2-final\"", "\"contract_version\":\"future\""))));
		rows.add(row("build", "unknown_variant", builds.deserialize(buildJson.replace("\"entity_type\":\"DEVICE\"", "\"entity_type\":\"FUTURE_ENTITY\""))));
		rows.add(row("runtime", "schema_5", runtime.deserialize(runtimeJson.replace("\"schema_version\":6", "\"schema_version\":5"))));
		rows.add(row("runtime", "schema_6", runtime.deserialize(runtimeJson)));
		rows.add(row("runtime", "schema_7", runtime.deserialize(runtimeJson.replace("\"schema_version\":6", "\"schema_version\":7"))));
		return "{\n  \"rows\": [\n    " + String.join(",\n    ", rows) + "\n  ]\n}\n";
	}

	private static String row(String payload, String caseName, CanonicalLoadResult<?> result) {
		return "{\"payload\":\"" + payload + "\",\"case\":\"" + caseName
				+ "\",\"state\":\"" + result.state().name() + "\",\"value_present\":"
				+ (result.value() != null) + ",\"diagnostics\":\"" + escape(result.diagnostics().toString()) + "\"}";
	}

	private static void write(Path path, String value) throws IOException {
		Files.write(path, value.getBytes(StandardCharsets.UTF_8));
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"")
				.replace("\n", "\\n").replace("\r", "\\r");
	}
}
