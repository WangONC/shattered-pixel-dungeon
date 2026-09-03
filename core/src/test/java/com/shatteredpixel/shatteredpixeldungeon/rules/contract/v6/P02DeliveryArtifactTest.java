package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.RuntimeStateValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeGroupSpec;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.*;

/** Emits P02 evidence only when P02_ARTIFACT_DIR is supplied by the gate runner. */
public class P02DeliveryArtifactTest {
	private final CanonicalBuildCodec codec = new CanonicalBuildCodec();

	@Test public void threeRequiredPlayerCommandTracesReplayCanonically() throws IOException {
		PlayerBuildSession resources = PlayerBuildSession.empty(new DeterministicIdGenerator("p02-artifact-resources"));
		resources.dispatch(new BuilderCommand.CreateResource("Rage"));
		resources.dispatch(new BuilderCommand.CreateResource("Focus"));
		String rage = resources.state().draft().resources().get(0).id().value();
		resources.dispatch(new BuilderCommand.RenameDeclaration(rage, "Fury"));
		resources.dispatch(new BuilderCommand.SaveDraft("two-resources"));
		resources.dispatch(new BuilderCommand.EditResourceField(rage, "maximum", "30"));
		resources.dispatch(new BuilderCommand.LoadDraft("two-resources"));
		assertEquals(10, resources.state().draft().resources().get(0).maximum());

		PlayerBuildSession mark = PlayerBuildSession.empty(new DeterministicIdGenerator("p02-artifact-mark"));
		mark.dispatch(new BuilderCommand.CreateMark("灼痕"));
		String markId = mark.state().draft().marks().get(0).id().value();
		mark.dispatch(new BuilderCommand.EditMarkField(markId, "maximum", "12"));
		mark.dispatch(new BuilderCommand.RenameDeclaration(markId, "炽热灼痕"));

		PlayerBuildSession modes = PlayerBuildSession.empty(new DeterministicIdGenerator("p02-artifact-modes"));
		modes.dispatch(new BuilderCommand.CreateModeGroup("姿态"));
		ModeGroupSpec deleted = modes.state().draft().modeGroups().get(0);
		modes.dispatch(new BuilderCommand.CreateMode("防守", new ModeGroupRef(deleted.id(), deleted.displayName().text())));
		modes.dispatch(new BuilderCommand.CreateMode("进攻", new ModeGroupRef(deleted.id(), deleted.displayName().text())));
		modes.dispatch(new BuilderCommand.DeleteDeclaration(deleted.id().value()));
		assertTrue(modes.state().dependencies().unresolvedCount() > 0);
		modes.dispatch(new BuilderCommand.CreateModeGroup("姿态"));
		ModeGroupSpec replacement = modes.state().draft().modeGroups().get(0);
		assertNotEquals(deleted.id(), replacement.id());
		for (int i = 0; i < modes.state().draft().modes().size(); i++) {
			String modeId = modes.state().draft().modes().get(i).id().value();
			modes.dispatch(new BuilderCommand.RebindReference(modeId, "group",
					new ModeGroupRef(replacement.id(), replacement.displayName().text())));
		}
		assertEquals(0, modes.state().dependencies().unresolvedCount());

		assertReplay("p02-artifact-resources", resources);
		assertReplay("p02-artifact-mark", mark);
		assertReplay("p02-artifact-modes", modes);

		String output = System.getenv("P02_ARTIFACT_DIR");
		if (output == null || output.isEmpty()) return;
		Path root = Paths.get(output).toAbsolutePath().normalize();
		write(root.resolve("builder-command-traces/dual-resource.trace"), resources.saveTrace());
		write(root.resolve("builder-command-traces/chinese-mark.trace"), mark.saveTrace());
		write(root.resolve("builder-command-traces/same-group-dual-mode-delete-rebind.trace"), modes.saveTrace());
		write(root.resolve("canonical/dual-resource-build.json"), codec.serialize(resources.state().draft()) + "\n");
		write(root.resolve("canonical/chinese-mark-build.json"), codec.serialize(mark.state().draft()) + "\n");
		write(root.resolve("canonical/same-group-dual-mode-build.json"), codec.serialize(modes.state().draft()) + "\n");
	}

	@Test public void runtimeAndFinalizeEvidenceComesFromRealValidators() throws IOException {
		P01Fixtures f = new P01Fixtures();
		RuntimeStateValidator validator = new RuntimeStateValidator();
		DependencyReport counters = validator.validateNodeCounters(f.build(), "runtime.uses_this_floor", Arrays.asList(
				new RuntimeStateValidator.NodeCounterValue(f.skill.id(), 1),
				new RuntimeStateValidator.NodeCounterValue(f.skill.id(), 2),
				new RuntimeStateValidator.NodeCounterValue(f.resource.id(), 1),
				new RuntimeStateValidator.NodeCounterValue(null, 1)));
		assertTrue(counters.hardConflictCount() >= 3);

		PlayerBuildSession deferred = PlayerBuildSession.empty(new DeterministicIdGenerator("p02-artifact-finalize"));
		deferred.dispatch(new BuilderCommand.CreateSkill("Deferred Skill", "P03_NOT_IMPLEMENTED"));
		deferred.dispatch(new BuilderCommand.FinalizeBuild());
		assertFalse(deferred.state().finalization().allowed());

		String output = System.getenv("P02_ARTIFACT_DIR");
		if (output == null || output.isEmpty()) return;
		Path root = Paths.get(output).toAbsolutePath().normalize();
		StringBuilder diagnostics = new StringBuilder("{\n  \"diagnostics\": [\n");
		for (int i = 0; i < counters.diagnostics().size(); i++) {
			DependencyDiagnostic d = counters.diagnostics().get(i);
			if (i > 0) diagnostics.append(",\n");
			diagnostics.append("    {\"field_path\":\"").append(escape(d.fieldPath()))
					.append("\",\"state\":\"").append(d.state()).append("\",\"message_key\":\"")
					.append(escape(d.messageKey())).append("\"}");
		}
		diagnostics.append("\n  ]\n}\n");
		write(root.resolve("runtime/runtime-node-counter-validation.json"), diagnostics.toString());
		write(root.resolve("finalization/fail-closed-deferred.json"),
				"{\n  \"allowed\": false,\n  \"diagnostics\": \"" + escape(deferred.state().finalization().diagnostics().toString()) + "\"\n}\n");
	}

	private void assertReplay(String seed, PlayerBuildSession source) {
		PlayerBuildSession replay = BuilderCommandTrace.deserialize(source.saveTrace())
				.replay(new DeterministicIdGenerator(seed));
		assertEquals(codec.serialize(source.state().draft()), codec.serialize(replay.state().draft()));
	}

	private static void write(Path path, String value) throws IOException {
		Files.createDirectories(path.getParent());
		Files.write(path, value.getBytes(StandardCharsets.UTF_8));
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"")
				.replace("\n", "\\n").replace("\r", "\\r");
	}
}
