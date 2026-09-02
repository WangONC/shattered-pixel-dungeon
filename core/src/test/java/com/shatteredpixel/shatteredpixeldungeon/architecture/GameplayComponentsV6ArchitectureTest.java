package com.shatteredpixel.shatteredpixeldungeon.architecture;

import com.shatteredpixel.shatteredpixeldungeon.qa.ArchetypeStressReport;
import com.shatteredpixel.shatteredpixeldungeon.qa.BuilderVocabularyExposureAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.FuzzReport;
import com.shatteredpixel.shatteredpixeldungeon.qa.GameplayComponentCoverageAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.PlayerArchetypeReconstructionAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.PlayerBuildEquivalenceAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.PlayerClassCoreAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.ScenarioResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.V6GameplayBoundary;
import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/** Source-structure guard for the P00 Legacy/v6 boundary; it never reads Markdown as evidence. */
public class GameplayComponentsV6ArchitectureTest {
	private static final Set<String> EFFECT_FIELDS = set(
			"family", "variant", "operation", "power", "duration", "count", "period", "lifetime",
			"secondaryParameter", "resourceId", "targetResourceId", "templateId", "stateId",
			"damageType", "scalingSource", "statusStacking");
	private static final Set<String> COMPONENT_FIELDS = set(
			"id", "name", "type", "basicAttack", "resourceId", "targetResourceId", "trigger",
			"resourceOperation", "amount", "targetAmount", "interval", "delay", "meleeOnly",
			"actionTime", "entityFilter", "capacity", "lifetime", "modes", "restriction");
	private static final Set<String> EFFECT_OPERATIONS = set(
			"LEGACY", "DAMAGE_STANDARD", "DAMAGE_PERCENT", "DAMAGE_MISSING_HP", "DAMAGE_EXECUTE",
			"STATUS_POISON", "STATUS_BURNING", "STATUS_BLEEDING", "STATUS_SLOW", "STATUS_HASTE",
			"STATUS_PARALYSIS", "STATUS_ROOTS", "STATUS_AMOK", "STATUS_TERROR", "STATUS_VULNERABLE",
			"MOVE_PUSH", "MOVE_PULL", "MOVE_THROW", "MOVE_DASH", "MOVE_TELEPORT", "MOVE_SWAP",
			"RECOVER_HEAL", "DEFENSE_BARRIER", "DEFENSE_TEMP_HP", "DEFENSE_MITIGATE",
			"DEFENSE_REDIRECT", "DEFENSE_CLEANSE", "RESOURCE_GAIN", "RESOURCE_DRAIN",
			"RESOURCE_CONVERT", "RESOURCE_RESERVE", "RESOURCE_SUPPRESS", "MARK_APPLY", "MARK_STACK",
			"MARK_COUNTER", "MARK_CONSUME", "MARK_SPREAD", "CREATE_ACTOR", "CREATE_DEVICE",
			"CREATE_TRAP", "CREATE_FIELD", "WORLD_WATER", "WORLD_GRASS", "WORLD_DESTROY",
			"WORLD_TOXIC_GAS", "WORLD_FIRE", "WORLD_CLEAR_HAZARD", "RELATION_OWNERSHIP",
			"RELATION_LINK", "RELATION_COMMAND_FOLLOW", "RELATION_COMMAND_ATTACK",
			"RELATION_COMMAND_GUARD", "RELATION_INHERIT", "RELATION_BREAK", "TRANSFER_RESOURCE",
			"COPY_STATUS", "TRANSFER_MARK", "SWAP_BARRIER", "TRANSFORM_MODE",
			"TRANSFORM_CAPABILITY", "TRANSFORM_BEHAVIOR");
	private static final Set<String> COMPONENT_TYPES = set("BASIC_ATTACK", "RESOURCE_FLOW",
			"ACTIVE_REFILL", "OWNERSHIP", "ENTITY_CAPACITY", "PERSISTENCE", "COMMAND", "RECYCLE",
			"MODE_ENGINE", "GLOBAL_CONSTRAINT");

	@Test public void legacyFieldBagsAndFixedEnumsCannotGrow() throws Exception {
		String effect = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/EffectSpec.java");
		String component = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/ClassGameplayComponentSpec.java");
		String mark = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/RuleMark.java");
		assertNoUnexpected("EffectSpec fields", publicInstanceFields(effect), EFFECT_FIELDS);
		assertNoUnexpected("ClassGameplayComponentSpec fields", publicInstanceFields(component), COMPONENT_FIELDS);
		assertNoUnexpected("EffectSpec.Operation", enumValues(effect, "Operation"), EFFECT_OPERATIONS);
		assertNoUnexpected("ClassGameplayComponentSpec.Type", enumValues(component, "Type"), COMPONENT_TYPES);
		assertNoUnexpected("RuleMark.Type", enumValues(mark, "Type"),
				set("HUNTED", "ACCUMULATION", "CHARGED", "LOW_PHASE", "COMPENSATION_LOCK"));
	}

	@Test public void dedicatedCarrierPayloadVocabularyCannotGrow() throws Exception {
		String registry = compact(source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/EffectVocabularyRegistry.java"));
		assertTrue("legacy carrier payload list changed; replace it in a later contract phase, do not extend it",
				registry.contains("newString[]{\"fire\",\"poison\",\"heal\"}"));
		assertFalse("legacy carrier payload list was expanded", registry.contains("\"fire\",\"poison\",\"heal\","));
	}

	@Test public void v6NamespaceCannotDependOnLegacyGameplayLanguage() throws Exception {
		Path root = repoRoot().resolve("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6");
		assertTrue("v6 boundary directory missing", Files.isDirectory(root));
		try (Stream<Path> files = Files.walk(root)) {
			files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
				String value = read(path);
				for (String banned : Arrays.asList("rules.legacy.v5", "rules.EffectSpec",
						"rules.ClassGameplayComponentSpec", "actors.buffs.RuleMark", "actors.buffs.RuleMode",
						"EffectVocabularyRegistry", "PlayerBuildAssembler", "resolvePendingBindings")) {
					assertFalse(path + " depends on frozen legacy symbol " + banned, value.contains(banned));
				}
				for (String domain : Arrays.asList("GUNNER_CORE", "SUMMONER_DOMAIN", "ENGINEERING_DOMAIN",
						"BLUE_MAGE_DOMAIN", "NECROMANCER_DOMAIN")) {
					assertFalse(path + " creates a forbidden fixed domain", value.contains(domain));
				}
			});
		}
		assertFalse(V6GameplayBoundary.PUBLIC_GAMEPLAY_ENABLED);
		assertEquals(6, V6GameplayBoundary.TARGET_SCHEMA);
	}

	@Test public void oldQaReportsAreNeverV6CompletionEvidence() {
		assertLegacy(new GameplayComponentCoverageAudit.Result().evidenceClassification,
				new GameplayComponentCoverageAudit.Result().v6CompletionEligible);
		assertLegacy(new BuilderVocabularyExposureAudit.Result().evidenceClassification,
				new BuilderVocabularyExposureAudit.Result().v6CompletionEligible);
		assertLegacy(new PlayerBuildEquivalenceAudit.Result().evidenceClassification,
				new PlayerBuildEquivalenceAudit.Result().v6CompletionEligible);
		assertLegacy(new PlayerArchetypeReconstructionAudit.Result().evidenceClassification,
				new PlayerArchetypeReconstructionAudit.Result().v6CompletionEligible);
		assertLegacy(new PlayerClassCoreAudit.Result().evidenceClassification,
				new PlayerClassCoreAudit.Result().v6CompletionEligible);
		assertLegacy(new ScenarioResult().evidenceClassification, new ScenarioResult().v6CompletionEligible);
		assertLegacy(new FuzzReport().evidenceClassification, new FuzzReport().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.Summary().evidenceClassification,
				new ArchetypeStressReport.Summary().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.RunResult().evidenceClassification,
				new ArchetypeStressReport.RunResult().v6CompletionEligible);
	}

	private static void assertLegacy(String classification, boolean eligible) {
		assertEquals(LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION, classification);
		assertFalse(eligible);
	}

	private static void assertNoUnexpected(String label, Set<String> actual, Set<String> allowed) {
		assertFalse(label + " was not parsed", actual.isEmpty());
		Set<String> unexpected = new HashSet<>(actual);
		unexpected.removeAll(allowed);
		assertTrue(label + " expanded with " + unexpected, unexpected.isEmpty());
	}

	private static Set<String> publicInstanceFields(String source) {
		Set<String> result = new HashSet<>();
		Pattern field = Pattern.compile("^public\\s+(?!static\\b)(?:final\\s+)?[A-Za-z_$][A-Za-z0-9_.$<>?, \\[\\]]*\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?:=.*)?;$");
		int depth = 0;
		for (String raw : source.split("\\R")) {
			String line = raw.replaceFirst("//.*$", "").trim();
			if (depth == 1) {
				Matcher matcher = field.matcher(line);
				if (matcher.matches()) result.add(matcher.group(1));
			}
			depth += count(line, '{') - count(line, '}');
		}
		return result;
	}

	private static Set<String> enumValues(String source, String name) {
		Matcher start = Pattern.compile("enum\\s+" + Pattern.quote(name) + "\\s*\\{").matcher(source);
		assertTrue("enum missing: " + name, start.find());
		int open = source.indexOf('{', start.start());
		int depth = 1;
		int end = open + 1;
		while (end < source.length() && depth > 0) {
			char value = source.charAt(end++);
			if (value == '{') depth++;
			else if (value == '}') depth--;
		}
		String body = source.substring(open + 1, end - 1).replaceAll("(?s)/\\*.*?\\*/", "")
				.replaceAll("(?m)//.*$", "");
		Set<String> result = new HashSet<>();
		Matcher constant = Pattern.compile("(?:^|,)\\s*([A-Z][A-Z0-9_]*)").matcher(body);
		while (constant.find()) result.add(constant.group(1));
		return result;
	}

	private static String source(String relative) throws IOException {
		return new String(Files.readAllBytes(repoRoot().resolve(relative)), StandardCharsets.UTF_8);
	}

	private static String read(Path path) {
		try { return new String(Files.readAllBytes(path), StandardCharsets.UTF_8); }
		catch (IOException error) { throw new AssertionError(error); }
	}

	private static Path repoRoot() {
		Path current = Paths.get("").toAbsolutePath().normalize();
		for (int i = 0; i < 10 && current != null; i++, current = current.getParent()) {
			if (Files.isRegularFile(current.resolve("settings.gradle"))) return current;
		}
		throw new AssertionError("repository root not found from " + Paths.get("").toAbsolutePath());
	}

	private static int count(String value, char wanted) {
		int result = 0;
		for (int i = 0; i < value.length(); i++) if (value.charAt(i) == wanted) result++;
		return result;
	}

	private static String compact(String value) { return value.replaceAll("\\s+", ""); }
	private static Set<String> set(String... values) { return new HashSet<>(Arrays.asList(values)); }
}
