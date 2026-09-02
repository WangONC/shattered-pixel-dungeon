package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/** Runs every registered RuleQaCli mode, then validates every JSON file it actually emitted. */
public class RuleQaCliLegacyEvidenceTest {

	@Test public void everyGeneratedJsonAndConsoleOutputRejectsV6CompletionCredit() throws Exception {
		Path root = repoRoot();
		Path temporaryRoot = root.resolve("headless/build/tmp");
		Files.createDirectories(temporaryRoot);
		Path runRoot = Files.createTempDirectory(temporaryRoot, "p00-r1-ruleqa-evidence-");
		int jsonFilesChecked = 0;
		try {
			for (String mode : RuleQaCli.legacyQaModes()) {
				Path reports = runRoot.resolve(mode);
				Files.createDirectories(reports);
				String console = runCli(root, reports, mode);
				assertTrue(mode + " did not identify its console output as Legacy QA",
						console.contains("LEGACY QA - REGRESSION EVIDENCE ONLY"));
				assertTrue(mode + " omitted the evidence classification from console output",
						console.contains("evidenceClassification=" + LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION));
				assertTrue(mode + " omitted v6CompletionEligible=false from console output",
						console.contains("v6CompletionEligible=false"));
				String upper = console.toUpperCase(Locale.ROOT);
				assertFalse(mode + " emitted an unsafe QA PASS claim", upper.contains("QA PASS"));
				assertFalse(mode + " emitted an unsafe COVERAGE COMPLETE claim", upper.contains("COVERAGE COMPLETE"));

				List<Path> generated;
				try (Stream<Path> files = Files.walk(reports)) {
					generated = files.filter(Files::isRegularFile)
							.filter(path -> path.getFileName().toString().endsWith(".json"))
							.sorted()
							.collect(Collectors.toList());
				}
				assertFalse(mode + " did not generate any JSON output", generated.isEmpty());
				for (Path json : generated) {
					JsonValue rootValue = new JsonReader().parse(new String(
							Files.readAllBytes(json), StandardCharsets.UTF_8));
					assertTrue(json + " must be a self-describing object", rootValue.isObject());
					assertEquals(json.toString(), LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION,
							rootValue.getString("evidenceClassification", null));
					assertFalse(json.toString(), rootValue.getBoolean("v6CompletionEligible", true));
					jsonFilesChecked++;
				}
			}
			assertTrue("RuleQaCli mode inventory produced too few JSON files",
					jsonFilesChecked >= RuleQaCli.legacyQaModes().length);
			System.out.println("RULE_QA_LEGACY_JSON_FILES_VERIFIED=" + jsonFilesChecked);
		} finally {
			deleteTree(runRoot);
		}
	}

	private static String runCli(Path root, Path reports, String mode) throws Exception {
		Path javaHome = Paths.get(System.getProperty("java.home"));
		Path java = javaHome.resolve("bin").resolve(isWindows() ? "java.exe" : "java");
		ArrayList<String> command = new ArrayList<>();
		command.add(java.toString());
		command.add("-Dfile.encoding=UTF-8");
		command.add("-Druleqa.mode=" + mode);
		command.add("-Druleqa.projectDir=" + root);
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.add(RuleQaCli.class.getName());
		command.add("--report-dir");
		command.add(reports.toString());
		if ("fuzz".equals(mode)) {
			command.add("--count"); command.add("20");
			command.add("--headless-sample"); command.add("5");
		} else if ("static".equals(mode)) {
			command.add("--count"); command.add("20");
		} else if ("archetype".equals(mode)) {
			command.add("--seeds"); command.add("1");
		}
		Path console = reports.resolve("console.log");
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(root.resolve("core/src/main/assets").toFile());
		builder.redirectErrorStream(true);
		builder.redirectOutput(console.toFile());
		Process process = builder.start();
		boolean finished = process.waitFor(180, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
			fail(mode + " RuleQaCli process timed out");
		}
		String output = new String(Files.readAllBytes(console), StandardCharsets.UTF_8);
		assertEquals(mode + " RuleQaCli failed:\n" + output, 0, process.exitValue());
		return output;
	}

	private static Path repoRoot() {
		Path current = Paths.get("").toAbsolutePath().normalize();
		for (int i = 0; i < 10 && current != null; i++, current = current.getParent()) {
			if (Files.isRegularFile(current.resolve("settings.gradle"))) return current;
		}
		throw new AssertionError("repository root not found from " + Paths.get("").toAbsolutePath());
	}

	private static void deleteTree(Path root) throws IOException {
		if (!Files.exists(root)) return;
		List<Path> paths;
		try (Stream<Path> values = Files.walk(root)) {
			paths = values.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
		}
		for (Path path : paths) Files.deleteIfExists(path);
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}
}
