package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;
import com.watabou.noosa.Game;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Pure-shell entry point. HeadlessApplication supplies files/audio mocks but never creates GLFW. */
public final class RuleQaCli {
	private static final String[] LEGACY_QA_MODES = {
			"headless", "fuzz", "static", "archetype", "builder-vocabulary",
			"law-trait-vocabulary", "player-build-equivalence", "gameplay-component-coverage",
			"player-archetype-reconstruction", "player-class-core"
	};

	private RuleQaCli() {}

	public static class Summary {
		public String schema = "ruleqa-summary-1";
		public String evidenceClassification = LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION;
		public boolean v6CompletionEligible = LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE;
		public String mode;
		public String generatedAt;
		public int scenarios;
		public int expectationFailures;
		public int runtimeFailures;
		public int staticBuilds;
		public int valid;
		public int risky;
		public int broken;
		public int headlessSampled;
		public int infiniteLoops;
		public double basicEffectiveCombinationDensity;
		public double validDensity, riskyDensity, brokenDensity, nonBrokenDensity;
		public double runtimeFailureRate, exploitRate;
		public long scenario100Millis;
		public long static1000Millis;
		public long fuzz1000Millis;
		public double headlessTurnsPerSecond;
		public final LinkedHashMap<String, Integer> topFindings = new LinkedHashMap<>();
		public String metricNote = "VALID/RISKY/BROKEN are separate; NON-BROKEN does not imply a high-quality build.";
	}

	public static void main(String[] args) throws Exception {
		CountDownLatch finished = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		config.updatesPerSecond = -1;
		HeadlessApplication app = new HeadlessApplication(new ApplicationAdapter() {
			@Override public void create() {
				try {
					// The report is the command-line product; ordinary in-game GLog messages
					// remain observable in traces but should not flood stress/fuzz stdout.
					Gdx.app.setLogLevel(Application.LOG_ERROR);
					// Formal death/save paths may still run during combat. Keep them real but
					// isolate every file operation under the build directory.
					com.watabou.utils.FileUtils.setDefaultFileProperties(
							com.badlogic.gdx.Files.FileType.Local, "build/tmp/ruleqa/");
					Game.version = "3.3.8-INDEV-headless-qa";
					Game.versionCode = 896;
					Messages.setup(Languages.ENGLISH);
					run(args);
				} catch (Throwable error) {
					failure.set(error);
				} finally {
					finished.countDown();
				}
			}
		}, config);
		finished.await();
		app.exit();
		if (failure.get() != null) {
			if (failure.get() instanceof Exception) throw (Exception) failure.get();
			throw new RuntimeException(failure.get());
		}
	}

	private static void run(String[] args) throws Exception {
		Options options = new Options(args);
		String mode = System.getProperty("ruleqa.mode", "headless");
		if (!legacyMode(mode)) throw new IllegalArgumentException("unknown ruleqa.mode " + mode);
		Path project = Paths.get(System.getProperty("ruleqa.projectDir", ".")).toAbsolutePath().normalize();
		Path reports = options.reportDir == null ? project.resolve("build/reports/ruleqa")
				: project.resolve(options.reportDir).normalize();
		Files.createDirectories(reports);
		printLegacyEvidenceBanner(mode);
		if ("archetype".equals(mode)) runArchetype(options, reports);
		else if ("builder-vocabulary".equals(mode)) runBuilderVocabulary(reports);
		else if ("law-trait-vocabulary".equals(mode)) runLawTraitVocabulary(reports);
		else if ("player-build-equivalence".equals(mode)) runPlayerBuildEquivalence(reports);
		else if ("gameplay-component-coverage".equals(mode)) runGameplayComponentCoverage(reports);
		else if ("player-archetype-reconstruction".equals(mode)) runPlayerArchetypeReconstruction(reports);
		else if ("player-class-core".equals(mode)) runPlayerClassCore(reports);
		else if ("fuzz".equals(mode)) runFuzz(options, reports);
		else if ("static".equals(mode)) runStatic(options, reports);
		else runHeadless(options, project, reports);
	}

	/** Single inventory used by CLI dispatch validation and the output-enumerating integration test. */
	public static String[] legacyQaModes() { return LEGACY_QA_MODES.clone(); }

	private static boolean legacyMode(String mode) {
		for (String value : LEGACY_QA_MODES) if (value.equals(mode)) return true;
		return false;
	}

	private static void printLegacyEvidenceBanner(String mode) {
		System.out.println("LEGACY QA - REGRESSION EVIDENCE ONLY"
				+ "\nmode=" + mode
				+ "\nevidenceClassification=" + LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION
				+ "\nv6CompletionEligible=" + LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE
				+ "\nThis output cannot establish Gameplay Components v6 completion.");
	}

	private static void runPlayerClassCore(Path reports) throws Exception {
		PlayerClassCoreAudit.Result result=PlayerClassCoreAudit.run();
		writeJson(reports.resolve("player_class_core.json"),result);
		System.out.println("PLAYER CLASS CORE QA\nChecks "+result.checks
				+"\nFailures "+result.failures
				+"\nFoundation budget "+result.foundationBudget
				+"\nGunner budget "+result.gunnerUsedBudget+"/"+result.foundationBudget
				+"\nGunner FULL basic-attack cost "+result.gunnerFullAttackCost
				+"\nPlayer text missing "+result.playerTextMissing
				+"\nHUD actions "+result.gunnerHudActions
				+"\nHUD labels "+result.gunnerHudLabels
				+"\nResource HUD "+result.resourceHud);
		if(!result.passed)throw new IllegalStateException("player class core failed: "+result.findings);
	}

	private static void runGameplayComponentCoverage(Path reports) throws Exception {
		GameplayComponentCoverageAudit.Result result=GameplayComponentCoverageAudit.run();
		writeJson(reports.resolve("gameplay_component_coverage.json"),result);
		System.out.println("GAMEPLAY COMPONENT COVERAGE QA\nComponents "+result.components
				+"\ndiscussed_component_not_player_exposed "+result.discussedComponentNotPlayerExposed
				+"\nincomplete "+result.incomplete);
		if(!result.passed)throw new IllegalStateException("gameplay component coverage failed: "+result.failures);
	}

	private static void runPlayerArchetypeReconstruction(Path reports) throws Exception {
		PlayerArchetypeReconstructionAudit.Result result=PlayerArchetypeReconstructionAudit.run();
		writeJson(reports.resolve("player_archetype_reconstruction.json"),result);
		System.out.println("PLAYER ARCHETYPE RECONSTRUCTION QA\nArchetypes "+result.archetypes
				+"\nqa_build_not_player_constructible "+result.qaBuildNotPlayerConstructible
				+"\ndiscussed_component_not_player_exposed "+result.discussedComponentNotPlayerExposed
				+"\ndependency_missing_but_disabled "+result.dependencyMissingButDisabled
				+"\ndependency_copy_validation_mismatch "+result.dependencyCopyValidationMismatch
				+"\nruntime_failure "+result.runtimeFailures);
		if(!result.passed)throw new IllegalStateException("player archetype reconstruction failed");
	}

	private static void runLawTraitVocabulary(Path reports) throws Exception {
		LawTraitVocabularyAudit.Result result=LawTraitVocabularyAudit.run();
		writeJson(reports.resolve("law_trait_vocabulary.json"),result);
		System.out.println("LAW / TRAIT VOCABULARY QA\nLaws "+result.laws+"\nTraits "+result.traits
				+"\nRuntime missing "+result.runtimeMissing+"\nNever triggered "+result.neverTriggered
				+"\nEN missing name/summary/detail "+result.missingEnName+"/"+result.missingEnSummary+"/"+result.missingEnDetail
				+"\nZH missing name/summary/detail "+result.missingZhName+"/"+result.missingZhSummary+"/"+result.missingZhDetail
				+"\nRoundtrip failures "+result.roundtripFailures+"\nCompatibility failures "+result.compatibilityFailures);
		if(!result.passed)throw new IllegalStateException("law/trait vocabulary failed: "+result.failures);
	}

	private static void runPlayerBuildEquivalence(Path reports) throws Exception {
		PlayerBuildEquivalenceAudit.Result result=PlayerBuildEquivalenceAudit.run();
		writeJson(reports.resolve("player_build_equivalence.json"),result);
		System.out.println("PLAYER BUILD EQUIVALENCE QA\nBuilds "+result.builds
				+"\nqa_build_not_player_constructible "+result.qaBuildNotPlayerConstructible
				+"\nBudget authority mismatches "+result.budgetMismatches
				+"\nRoundtrip failures "+result.roundtripFailures);
		if(!result.passed)throw new IllegalStateException("player build equivalence failed");
	}

	private static void runBuilderVocabulary(Path reports) throws Exception {
		BuilderVocabularyExposureAudit.Result result = BuilderVocabularyExposureAudit.run();
		writeJson(reports.resolve("builder_vocabulary_exposure.json"), result);
		System.out.println("BUILDER VOCABULARY EXPOSURE"
				+ "\nRuntime supported                    " + result.runtimeSupported
				+ "\nBuilder exposed                      " + result.builderExposed
				+ "\nSUPPORTED_BUT_NOT_EXPOSED            " + result.supportedButNotExposed
				+ "\nBUILDER_EXPOSED_BUT_RUNTIME_UNSUPPORTED " + result.builderExposedButRuntimeUnsupported
				+ "\nmissing_en_name                      " + result.missingEnName
				+ "\nmissing_en_summary                   " + result.missingEnSummary
				+ "\nmissing_zh_name                      " + result.missingZhName
				+ "\nmissing_zh_summary                   " + result.missingZhSummary
				+ "\nVariants per family                  " + result.variantsPerFamily);
		if (!result.passed) throw new IllegalStateException("builder vocabulary exposure failed: " + result.failures);
	}

	private static void runArchetype(Options options, Path reports) throws Exception {
		int seeds=options.seeds==null?5:options.seeds;
		long seed=options.seedOverride==null?424242L:options.seedOverride;
		ArchetypeStressAnalyzer.Result result=ArchetypeStressAnalyzer.run(seeds,seed);
		writeJson(reports.resolve("archetype_summary.json"),result.summary);
		writeJson(reports.resolve("archetype_scenarios.json"),result.runs);
		writeJson(reports.resolve("archetype_scenario_aggregates.json"),result.scenarioAggregates);
		writeJson(reports.resolve("archetype_behavior_profiles.json"),result.summary.builds);
		writeJson(reports.resolve("archetype_dominance.json"),result.dominance);
		writeJson(reports.resolve("archetype_component_usage.json"),result.componentUsage);
		writeJson(reports.resolve("archetype_constraint_effectiveness.json"),result.constraints);
		writeJson(reports.resolve("archetype_specialty.json"),result.specialties);
		writeJson(reports.resolve("archetype_component_value.json"),result.componentValues);
		writeJson(reports.resolve("archetype_missing_capabilities.json"),result.missingCapabilities);
		System.out.println("CLASS ARCHETYPE STRESS QA\nArchetypes             "+result.summary.archetypes
				+"\nScenarios              "+result.summary.scenarios
				+"\nSeeds/scenario         "+result.summary.seedsPerScenario
				+"\nTotal runs             "+result.summary.totalRuns
				+"\nVictories              "+result.summary.victories
				+"\nSurvival failures      "+result.summary.survivalFailures
				+"\nRuntime failures       "+result.summary.runtimeFailures
				+"\nPrimary budget range   "+result.summary.primaryMinimumBudget+".."+result.summary.primaryMaximumBudget
				+"\nPrimary budget spread  "+String.format("%.2f%%",result.summary.primaryBudgetSpreadPercent)
				+"\nDominance findings     "+result.dominance.size()
				+"\nConclusion             "+result.summary.conclusion
				+"\nElapsed                "+result.summary.elapsedMillis+" ms");
		for(ArchetypeStressReport.BuildSummary build:result.summary.builds)System.out.println("- "+build.buildId
				+" budget="+build.usedBudget+"/"+build.maxBudget+" victory="+String.format("%.1f%%",build.victoryRate*100)
				+" meanTurns="+String.format("%.1f",build.meanTurnsToVictory)+" signature="+build.signature);
		if(result.summary.runtimeFailures>0)throw new IllegalStateException("archetype stress runtime failures");
	}

	private static void runHeadless(Options options, Path project, Path reports) throws Exception {
		ArrayList<QaScenario> scenarios = new ArrayList<>();
		if (options.scenarioFile != null) {
			Path path = project.resolve(options.scenarioFile).normalize();
			scenarios.add(QaScenario.fromJson(new String(Files.readAllBytes(path), StandardCharsets.UTF_8)));
		} else {
			scenarios.addAll(RegressionScenarios.all());
		}
		ArrayList<ScenarioResult> results = new ArrayList<>();
		Summary summary = baseSummary("headless");
		for (QaScenario scenario : scenarios) {
			if (options.seedOverride != null) scenario.seed = options.seedOverride;
			ScenarioResult result = new HeadlessGameplayHarness().run(scenario);
			results.add(result);
			summary.scenarios++;
			if (!result.expectationMet) summary.expectationFailures++;
			if (result.runtimeFailure) summary.runtimeFailures++;
			if (result.findings.contains("UNBOUNDED_POWER_LOOP")) summary.infiniteLoops++;
			for (String finding : result.findings) increment(summary.topFindings, finding);
			if (options.trace || options.traceOnFailure && (!result.expectationMet || result.runtimeFailure
					|| "BROKEN".equals(result.classification))) writeTrace(reports, result);
		}

		long benchmarkStart = System.nanoTime();
		long benchmarkTurns = 0;
		for (int i = 0; i < 100; i++) {
			QaScenario scenario = RegressionScenarios.momentumLoop();
			scenario.seed += i;
			ScenarioResult result = new HeadlessGameplayHarness().run(scenario);
			if (result.runtimeFailure) throw new IllegalStateException("100-scenario benchmark failed: " + result.failure);
			benchmarkTurns += result.turn;
		}
		long benchmarkNanos = System.nanoTime() - benchmarkStart;
		summary.scenario100Millis = benchmarkNanos / 1_000_000L;
		summary.headlessTurnsPerSecond = benchmarkNanos == 0 ? 0
				: benchmarkTurns * 1_000_000_000.0 / benchmarkNanos;
		writeJson(reports.resolve("scenarios.json"), results);
		writeJson(reports.resolve("summary.json"), summary);
		printHeadless(summary, results);
		if (summary.expectationFailures > 0 || summary.runtimeFailures > 0) {
			throw new IllegalStateException("headless QA expectations failed");
		}
	}

	private static void runFuzz(Options options, Path reports) throws Exception {
		int count = options.count == null ? 1000 : options.count;
		long seed = options.seedOverride == null ? 12345L : options.seedOverride;
		int sample = options.headlessSample == null ? Math.min(100, count) : options.headlessSample;
		RuleBuildFuzzer.Result result = new RuleBuildFuzzer().run(count, seed, sample);
		for (FuzzReport.Failure failure : result.fuzz.failures) {
			if ((options.trace || options.traceOnFailure) && failure.traceText != null) writeTrace(reports, failure);
		}
		writeJson(reports.resolve("fuzz.json"), result.fuzz);
		writeJson(reports.resolve("compatibility.json"), result.compatibility);
		Summary summary = baseSummary("fuzz");
		summary.staticBuilds = result.fuzz.totalGenerated;
		summary.valid = result.fuzz.staticValid;
		summary.risky = result.fuzz.risky;
		summary.broken = result.fuzz.broken;
		summary.headlessSampled = result.fuzz.headlessSampled;
		summary.runtimeFailures = result.fuzz.runtimeFailed;
		summary.infiniteLoops = result.fuzz.dynamicFailures;
		summary.basicEffectiveCombinationDensity = result.fuzz.basicEffectiveCombinationDensity;
		summary.validDensity=result.fuzz.validDensity; summary.riskyDensity=result.fuzz.riskyDensity;
		summary.brokenDensity=result.fuzz.brokenDensity; summary.nonBrokenDensity=result.fuzz.nonBrokenDensity;
		summary.runtimeFailureRate=result.fuzz.runtimeFailureRate; summary.exploitRate=result.fuzz.exploitRate;
		summary.static1000Millis = count == 1000 ? result.fuzz.staticAnalysisNanos / 1_000_000L : 0;
		summary.fuzz1000Millis = count == 1000 ? result.fuzz.elapsedMillis : 0;
		summary.headlessTurnsPerSecond = result.fuzz.headlessTurnsPerSecond;
		summary.topFindings.putAll(result.fuzz.findingCounts);
		writeJson(reports.resolve("summary.json"), summary);
		printFuzz(summary, result.fuzz);
	}

	private static void runStatic(Options options, Path reports) throws Exception {
		int count = options.count == null ? 1000 : options.count;
		long seed = options.seedOverride == null ? 12345L : options.seedOverride;
		RuleBuildFuzzer fuzzer = new RuleBuildFuzzer();
		RuleBuildAnalyzer analyzer = new RuleBuildAnalyzer();
		Summary summary = baseSummary("static");
		long started = System.nanoTime();
		for (int i = 0; i < count; i++) {
			long buildSeed = RuleBuildFuzzer.buildSeed(seed, i);
			com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild config = fuzzer.generateClassBuild(buildSeed, i);
			BuildAnalysis analysis = analyzer.analyze(RuleBuild.from(config));
			if (analysis.classification == BuildAnalysis.Classification.VALID) summary.valid++;
			else if (analysis.classification == BuildAnalysis.Classification.RISKY) summary.risky++;
			else summary.broken++;
			for (BuildAnalysis.Finding finding : analysis.findings) increment(summary.topFindings, finding.code);
		}
		summary.staticBuilds = count;
		summary.static1000Millis = count == 1000 ? (System.nanoTime() - started) / 1_000_000L : 0;
		writeJson(reports.resolve("summary.json"), summary);
		System.out.println("RULE BUILD INTEGRITY\nStatic builds " + count + "\nValid " + summary.valid
				+ "\nRisky " + summary.risky + "\nBroken " + summary.broken
				+ "\nElapsed " + summary.static1000Millis + " ms");
	}

	private static Summary baseSummary(String mode) {
		Summary summary = new Summary();
		summary.mode = mode;
		summary.generatedAt = Instant.now().toString();
		return summary;
	}

	private static void writeTrace(Path reports, ScenarioResult result) throws Exception {
		Path path = reports.resolve("traces").resolve(safe(result.id) + "-" + result.seed + ".log");
		Files.createDirectories(path.getParent());
		Files.write(path, result.traceText.getBytes(StandardCharsets.UTF_8));
		result.tracePath = path.toAbsolutePath().normalize().toString().replace('\\', '/');
	}

	private static void writeTrace(Path reports, FuzzReport.Failure failure) throws Exception {
		Path path = reports.resolve("traces").resolve(safe(failure.id) + "-" + failure.seed + ".log");
		Files.createDirectories(path.getParent());
		Files.write(path, failure.traceText.getBytes(StandardCharsets.UTF_8));
		failure.tracePath = path.toAbsolutePath().normalize().toString().replace('\\', '/');
	}

	private static void writeJson(Path path, Object value) throws Exception {
		Json json = new Json(JsonWriter.OutputType.json);
		json.setUsePrototypes(false);
		JsonValue root = new JsonReader().parse(json.prettyPrint(value));
		if (!root.isObject()) {
			JsonValue records = root;
			root = new JsonValue(JsonValue.ValueType.object);
			root.addChild("evidenceClassification", new JsonValue(LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION));
			root.addChild("v6CompletionEligible", new JsonValue(LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE));
			root.addChild("records", records);
		} else {
			JsonValue classification = root.get("evidenceClassification");
			JsonValue eligible = root.get("v6CompletionEligible");
			if (classification == null) {
				root.addChild("evidenceClassification", new JsonValue(LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION));
			} else if (!LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION.equals(classification.asString())) {
				throw new IllegalStateException("legacy QA emitted an invalid evidenceClassification: " + path);
			}
			if (eligible == null) {
				root.addChild("v6CompletionEligible", new JsonValue(LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE));
			} else if (eligible.asBoolean()) {
				throw new IllegalStateException("legacy QA emitted v6CompletionEligible=true: " + path);
			}
		}
		Files.write(path, root.prettyPrint(JsonWriter.OutputType.json, 100).getBytes(StandardCharsets.UTF_8));
	}

	private static void printHeadless(Summary summary, ArrayList<ScenarioResult> results) {
		System.out.println("RULE QA\nScenarios             " + summary.scenarios
				+ "\nExpectation failures  " + summary.expectationFailures
				+ "\nRuntime failures      " + summary.runtimeFailures
				+ "\nInfinite loops found  " + summary.infiniteLoops
				+ "\n100 scenarios         " + summary.scenario100Millis + " ms"
				+ "\nHeadless turns/sec    " + String.format("%.1f", summary.headlessTurnsPerSecond));
		for (ScenarioResult result : results) {
			System.out.println("- " + result.id + " " + result.classification + " " + result.findings);
		}
	}

	private static void printFuzz(Summary summary, FuzzReport report) {
		System.out.println("RULE FUZZ\nStatic builds          " + report.totalGenerated
				+ "\nValid                  " + report.staticValid
				+ "\nRisky                  " + report.risky
				+ "\nBroken                 " + report.broken
				+ "\nHeadless sampled       " + report.headlessSampled
				+ "\nRuntime failures       " + report.runtimeFailed
				+ "\nInfinite loops         " + report.dynamicFailures
				+ "\nVALID density          " + String.format("%.2f%%", report.validDensity)
				+ "\nRISKY density          " + String.format("%.2f%%", report.riskyDensity)
				+ "\nBROKEN density         " + String.format("%.2f%%", report.brokenDensity)
				+ "\nNON-BROKEN density     " + String.format("%.2f%%", report.nonBrokenDensity)
				+ "\nRuntime failure rate   " + String.format("%.2f%%", report.runtimeFailureRate)
				+ "\nExploit rate           " + String.format("%.2f%%", report.exploitRate)
				+ "\nElapsed                " + report.elapsedMillis + " ms"
				+ "\nHeadless turns/sec     " + String.format("%.1f", report.headlessTurnsPerSecond));
		System.out.println("Top findings: " + report.findingCounts);
		System.out.println("Potential dead vocabulary: " + report.potentialDeadVocabulary);
	}

	private static void increment(LinkedHashMap<String, Integer> values, String key) {
		values.put(key, values.containsKey(key) ? values.get(key) + 1 : 1);
	}

	private static String safe(String value) { return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_.-]", "_"); }

	private static class Options {
		Integer count;
		Integer seeds;
		Long seedOverride;
		Integer headlessSample;
		String scenarioFile;
		String reportDir;
		boolean trace;
		boolean traceOnFailure;

		Options(String[] args) {
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if ("--count".equals(arg)) count = Integer.parseInt(args[++i]);
				else if ("--seeds".equals(arg)) seeds = Integer.parseInt(args[++i]);
				else if ("--seed".equals(arg)) seedOverride = Long.parseLong(args[++i]);
				else if ("--headless-sample".equals(arg)) headlessSample = Integer.parseInt(args[++i]);
				else if ("--scenario".equals(arg)) scenarioFile = args[++i];
				else if ("--report-dir".equals(arg)) reportDir = args[++i];
				else if ("--trace".equals(arg)) trace = true;
				else if ("--trace-on-failure".equals(arg)) traceOnFailure = true;
				else throw new IllegalArgumentException("unknown argument " + arg);
			}
			if (count != null && count <= 0) throw new IllegalArgumentException("count must be positive");
			if (seeds != null && seeds <= 0) throw new IllegalArgumentException("seeds must be positive");
		}
	}
}
