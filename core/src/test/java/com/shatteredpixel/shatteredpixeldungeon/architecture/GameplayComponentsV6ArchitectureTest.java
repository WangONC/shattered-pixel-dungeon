package com.shatteredpixel.shatteredpixeldungeon.architecture;

import com.shatteredpixel.shatteredpixeldungeon.qa.ArchetypeStressReport;
import com.shatteredpixel.shatteredpixeldungeon.qa.BuilderVocabularyExposureAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.CompatibilityReport;
import com.shatteredpixel.shatteredpixeldungeon.qa.FuzzReport;
import com.shatteredpixel.shatteredpixeldungeon.qa.GameplayComponentCoverageAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.LawTraitVocabularyAudit;
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
			"EffectFamily family", "RuleEffect.Type variant", "Operation operation", "int power",
			"int duration", "int count", "int period", "int lifetime", "int secondaryParameter",
			"String resourceId", "String targetResourceId", "String templateId", "String stateId",
			"DamageType damageType", "ScalingSource scalingSource", "StatusStacking statusStacking");
	private static final Set<String> COMPONENT_FIELDS = set(
			"String id", "String name", "Type type", "BasicAttackProfile basicAttack",
			"String resourceId", "String targetResourceId", "ResourceFlowSpec.Trigger trigger",
			"ResourceFlowSpec.Operation resourceOperation", "int amount", "int targetAmount",
			"int interval", "int delay", "boolean meleeOnly", "float actionTime",
			"EntityFilter entityFilter", "int capacity", "int lifetime",
			"final ArrayList<String> modes", "Restriction restriction");
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
	private static final Set<String> EFFECT_REGISTRY_ENTRIES = set(
			"DAMAGE_STANDARD:DAMAGE", "DAMAGE_PERCENT:DAMAGE", "DAMAGE_MISSING_HP:DAMAGE",
			"DAMAGE_EXECUTE:DAMAGE", "STATUS_POISON:STATUS", "STATUS_BURNING:STATUS",
			"STATUS_BLEEDING:STATUS", "STATUS_SLOW:STATUS", "STATUS_HASTE:STATUS",
			"STATUS_PARALYSIS:STATUS", "STATUS_ROOTS:STATUS", "STATUS_AMOK:STATUS",
			"STATUS_TERROR:STATUS", "STATUS_VULNERABLE:STATUS", "MOVE_PUSH:DISTANCE",
			"MOVE_PULL:DISTANCE", "MOVE_THROW:DISTANCE", "MOVE_DASH:DISTANCE",
			"MOVE_TELEPORT:NONE", "MOVE_SWAP:NONE", "RECOVER_HEAL:POWER_DURATION",
			"DEFENSE_BARRIER:POWER_DURATION", "DEFENSE_TEMP_HP:TEMPORARY_HP",
			"DEFENSE_MITIGATE:POWER_DURATION", "DEFENSE_REDIRECT:POWER_DURATION",
			"DEFENSE_CLEANSE:NONE", "RESOURCE_GAIN:RESOURCE", "RESOURCE_DRAIN:RESOURCE",
			"RESOURCE_CONVERT:RESOURCE_CONVERSION", "RESOURCE_RESERVE:RESOURCE",
			"RESOURCE_SUPPRESS:RESOURCE", "MARK_APPLY:MARK", "MARK_STACK:MARK",
			"MARK_COUNTER:MARK", "MARK_CONSUME:MARK", "MARK_SPREAD:MARK",
			"CREATE_ACTOR:ENTITY", "CREATE_DEVICE:CARRIER", "CREATE_TRAP:CARRIER",
			"CREATE_FIELD:CARRIER", "WORLD_WATER:POWER_DURATION", "WORLD_GRASS:POWER_DURATION",
			"WORLD_DESTROY:NONE", "WORLD_TOXIC_GAS:POWER_DURATION", "WORLD_FIRE:POWER_DURATION",
			"WORLD_CLEAR_HAZARD:NONE", "RELATION_OWNERSHIP:NONE", "RELATION_LINK:POWER_DURATION",
			"RELATION_COMMAND_FOLLOW:NONE", "RELATION_COMMAND_ATTACK:NONE",
			"RELATION_COMMAND_GUARD:NONE", "RELATION_INHERIT:INHERIT", "RELATION_BREAK:NONE",
			"COPY_STATUS:POWER_DURATION", "TRANSFER_MARK:MARK", "SWAP_BARRIER:NONE",
			"TRANSFORM_MODE:MODE");
	private static final Set<String> COMPONENT_REGISTRY_ENTRIES = set(
			"BASIC_ATTACK:BASIC_COMBAT:BASIC_ATTACK", "RESOURCE_POOL:RESOURCE_ECONOMY:null",
			"RESOURCE_GAIN:RESOURCE_ECONOMY:RESOURCE_FLOW", "RESOURCE_LOSS:RESOURCE_ECONOMY:RESOURCE_FLOW",
			"RESOURCE_CONVERT:RESOURCE_ECONOMY:RESOURCE_FLOW", "ACTIVE_REFILL:RESOURCE_ECONOMY:ACTIVE_REFILL",
			"OWNERSHIP:ENTITY_RELATION:OWNERSHIP", "COMMAND:ENTITY_RELATION:COMMAND",
			"RECYCLE:ENTITY_RELATION:RECYCLE", "ENTITY_CAPACITY:PERSISTENCE_CAPACITY:ENTITY_CAPACITY",
			"PERSISTENCE:PERSISTENCE_CAPACITY:PERSISTENCE", "MODE_ENGINE:MODE_STATE:MODE_ENGINE",
			"GLOBAL_CONSTRAINT:TRADEOFF:GLOBAL_CONSTRAINT");
	private static final Set<String> EFFECT_REGISTRY_METHODS = set(
			"List<FamilyEntry> families()", "List<EffectEntry> variants(EffectFamily family)",
			"List<EffectEntry> exposed()", "EffectEntry find(EffectSpec effect)",
			"boolean exposed(EffectSpec.Operation operation)",
			"List<ParameterOption> parameterOptions(EffectSpec current, ClassBuild build)",
			"boolean playerReachable(EffectSpec value, ClassBuild build)");
	private static final Set<String> COMPONENT_REGISTRY_METHODS = set(
			"List<Template> templates()", "ArrayList<Template> templates(Category category)",
			"ClassGameplayComponentSpec create(Template template, String id)",
			"boolean playerExposed(ClassGameplayComponentSpec value)");
	private static final Set<String> V6_BANNED_SYMBOLS = set(
			"ClassBuild", "SkillSpec", "EffectSpec", "ResourceSpec", "ClassGameplayComponentSpec",
			"RuleCost", "RuleModifier", "SkillConstraint", "TargetingSpec", "RuleMark", "RuleMode",
			"PlayerBuildAssembler", "EffectVocabularyRegistry", "GameplayComponentRegistry",
			"resolvePendingBindings");

	@Test public void legacyPublicFieldsAndAdjacentEnumsAreExactFrozenSnapshots() throws Exception {
		String effect = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/EffectSpec.java");
		String component = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/ClassGameplayComponentSpec.java");
		String ruleEffect = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/RuleEffect.java");
		String mark = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/RuleMark.java");
		assertExact("EffectSpec public fields", publicTopLevelFields(effect), EFFECT_FIELDS);
		assertExact("ClassGameplayComponentSpec public fields", publicTopLevelFields(component), COMPONENT_FIELDS);
		assertExact("EffectSpec.Operation", enumValues(effect, "Operation"), EFFECT_OPERATIONS);
		assertExact("EffectSpec.DamageType", enumValues(effect, "DamageType"),
				set("UNTYPED", "FIRE", "POISON", "BLEEDING"));
		assertExact("EffectSpec.ScalingSource", enumValues(effect, "ScalingSource"),
				set("FIXED", "HERO_LEVEL", "CURRENT_RESOURCE"));
		assertExact("EffectSpec.StatusStacking", enumValues(effect, "StatusStacking"),
				set("NATIVE", "EXTEND", "REPLACE"));
		assertExact("RuleEffect.Type", enumValues(ruleEffect, "Type"), set(
				"PUSH", "POISON", "FIRE", "PULL", "TELEPORT", "SWAP_POSITION", "BLEED",
				"SLOW", "HASTE", "HEAL", "SHIELD", "CLEANSE", "CREATE_WATER", "CREATE_GAS"));
		assertExact("ClassGameplayComponentSpec.Type", enumValues(component, "Type"), COMPONENT_TYPES);
		assertExact("ClassGameplayComponentSpec.EntityFilter", enumValues(component, "EntityFilter"),
				set("OWNED_ACTOR", "OWNED_DEVICE", "OWNED_CARRIER", "OWNED_ENTITY"));
		assertExact("RuleMark.Type", enumValues(mark, "Type"),
				set("HUNTED", "ACCUMULATION", "CHARGED", "LOW_PHASE", "COMPENSATION_LOCK"));
		assertExact("Restriction", enumValues(source(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/Restriction.java"), "Restriction"),
				set("NONE", "NO_ORDINARY_WEAPONS", "WEAK_HEALING", "FRAIL",
						"NO_TRADITIONAL_HEALING", "WAIT_CLEARS_RESOURCE", "ACTIVE_COSTS_HP"));
		assertExact("EffectFamily", enumValues(source(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/EffectFamily.java"), "EffectFamily"),
				set("DAMAGE", "STATUS", "MOVEMENT", "RECOVERY_DEFENSE", "RESOURCE_OPERATION",
						"MARK_ACCUMULATION", "CREATE_ENTITY", "WORLD_TERRAIN", "RELATION_CONTROL",
						"TRANSFER_COPY", "TRANSFORM"));
		assertExact("RuleCost.Type", enumValues(source(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/RuleCost.java"), "Type"),
				set("NONE", "RESOURCE", "HP", "ACTION", "COOLDOWN", "CONSUMABLE", "STATE"));
		assertExact("RuleModifier.Type", enumValues(source(
				"core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/RuleModifier.java"), "Type"),
				set("NONE", "AREA", "REPEAT", "EXTEND_DURATION", "INTENSITY", "PIERCE", "BOUNCE", "DELAY"));
		String constraint = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/SkillConstraint.java");
		assertExact("SkillConstraint.Type", enumValues(constraint, "Type"),
				set("NONE", "TARGET", "SELF_STATE", "POSITION", "TIMING", "COMMITMENT", "FREQUENCY"));
		assertExact("SkillConstraint.Variant", enumValues(constraint, "Variant"),
				set("NONE", "TARGET_MARKED", "SELF_LOW_HP", "SELF_IN_WATER", "COOLDOWN",
						"HP_COMMITMENT", "LIMITED_USE"));
		String targeting = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/TargetingSpec.java");
		assertExact("TargetingSpec.Selector", enumValues(targeting, "Selector"),
				set("SELF", "SELECTED_ACTOR", "SELECTED_CELL", "NEAREST", "RANDOM", "ALL_MATCHING"));
		assertExact("TargetingSpec.Coverage", enumValues(targeting, "Coverage"),
				set("SINGLE", "ADJACENT", "RADIUS", "LINE", "CONE", "RING", "CHAIN"));
		assertExact("TargetingSpec.Filter", enumValues(targeting, "Filter"),
				set("ANY", "ENEMY", "ALLY", "SELF", "OWNED_ENTITY", "MARKED", "HAS_STATUS",
						"HP_THRESHOLD", "COMPATIBLE_ENTITY_TYPE"));
	}

	@Test public void legacyRegistriesAndCarrierPayloadAreExactFrozenSnapshots() throws Exception {
		String effects = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/EffectVocabularyRegistry.java");
		String components = source("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/rules/GameplayComponentRegistry.java");
		assertExact("EffectVocabularyRegistry.Parameters", enumValues(effects, "Parameters"),
				set("NONE", "DAMAGE", "STATUS", "DISTANCE", "POWER_DURATION", "TEMPORARY_HP",
						"RESOURCE", "RESOURCE_CONVERSION", "MARK", "ENTITY", "CARRIER", "MODE", "INHERIT"));
		assertExact("EffectVocabularyRegistry operation/parameter entries",
				effectRegistryEntries(effects), EFFECT_REGISTRY_ENTRIES);
		assertExact("EffectVocabularyRegistry public parameter entrypoints",
				publicStaticMethods(effects), EFFECT_REGISTRY_METHODS);
		assertExact("GameplayComponentRegistry.Template", enumValues(components, "Template"),
				componentTemplateNames(COMPONENT_REGISTRY_ENTRIES));
		assertExact("GameplayComponentRegistry component/category/type entries",
				componentRegistryEntries(components), COMPONENT_REGISTRY_ENTRIES);
		assertExact("GameplayComponentRegistry public entrypoints",
				publicStaticMethods(components), COMPONENT_REGISTRY_METHODS);
		String carrierOptions = between(effects, "entry.parameters == Parameters.CARRIER",
				"entry.parameters == Parameters.MARK");
		assertExact("carrier payload option strings", stringLiterals(carrierOptions),
				set("fire", "poison", "heal", "parameter_lifetime", "parameter_period",
						"parameter_payload_"));
		String carrierValidation = between(effects, "case CARRIER:", "case MODE:");
		Set<String> validationStrings = stringLiterals(carrierValidation);
		validationStrings.retainAll(set("fire", "poison", "heal"));
		assertExact("carrier payload validation strings", validationStrings, set("fire", "poison", "heal"));
	}

	@Test public void v6ProductionRootAndOneWayMigrationBoundaryAreEnforcedAcrossProductionTree() throws Exception {
		Path production = repoRoot().resolve("core/src/main/java");
		Path contractRoot = production.resolve(
				"com/shatteredpixel/shatteredpixeldungeon/rules/contract/v6");
		Path migrationRoot = production.resolve(
				"com/shatteredpixel/shatteredpixeldungeon/rules/migration/v5");
		assertTrue("v6 contract production root missing", Files.isDirectory(contractRoot));
		assertTrue("v5-to-v6 migration bridge root missing", Files.isDirectory(migrationRoot));
		try (Stream<Path> files = Files.walk(production)) {
			files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
				String code = codeOnly(read(path));
				boolean inContract = path.startsWith(contractRoot);
				boolean inMigration = path.startsWith(migrationRoot);
				String packageName = packageName(code);
				boolean declaresV6Package = packageName.matches("(?:.*\\.)?v6(?:\\..*)?");
				boolean declaresV6Type = Pattern.compile("\\b(?:class|interface|enum|record)\\s+V6[A-Za-z0-9_]*\\b")
						.matcher(code).find();
				boolean referencesV6 = code.contains("rules.contract.v6") || declaresV6Type;
				if ((declaresV6Package || referencesV6) && !inContract && !inMigration) {
					fail(path + " places or references v6 production code outside rules/contract/v6 or rules/migration/v5");
				}
				boolean referencesLegacy = code.contains("rules.legacy.v5") || containsAnyIdentifier(code, V6_BANNED_SYMBOLS);
				if (referencesLegacy && referencesV6 && !inMigration) {
					fail(path + " depends on both v5 and v6 outside the migration bridge");
				}
				if (inContract) {
					assertFalse(path + " imports the migration bridge", code.contains("rules.migration.v5"));
					assertFalse(path + " imports the legacy boundary", code.contains("rules.legacy.v5"));
					for (String banned : V6_BANNED_SYMBOLS) {
						assertFalse(path + " depends on frozen legacy symbol " + banned,
								containsIdentifier(code, banned));
					}
					for (String domain : Arrays.asList("GUNNER_CORE", "SUMMONER_DOMAIN", "ENGINEERING_DOMAIN",
							"BLUE_MAGE_DOMAIN", "NECROMANCER_DOMAIN")) {
						assertFalse(path + " creates a forbidden fixed domain", containsIdentifier(code, domain));
					}
				}
			});
		}
		assertFalse(V6GameplayBoundary.PUBLIC_GAMEPLAY_ENABLED);
		assertEquals(6, V6GameplayBoundary.TARGET_SCHEMA);
	}

	@Test public void oldQaReportsAreNeverV6CompletionEvidence() {
		assertLegacy(new LawTraitVocabularyAudit.Result().evidenceClassification,
				new LawTraitVocabularyAudit.Result().v6CompletionEligible);
		assertLegacy(new CompatibilityReport().evidenceClassification,
				new CompatibilityReport().v6CompletionEligible);
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
		assertLegacy(new ArchetypeStressReport.BuildSummary().evidenceClassification,
				new ArchetypeStressReport.BuildSummary().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.ScenarioAggregate().evidenceClassification,
				new ArchetypeStressReport.ScenarioAggregate().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.DominanceFinding().evidenceClassification,
				new ArchetypeStressReport.DominanceFinding().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.ComponentUsage().evidenceClassification,
				new ArchetypeStressReport.ComponentUsage().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.ConstraintResult().evidenceClassification,
				new ArchetypeStressReport.ConstraintResult().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.SpecialtyReport().evidenceClassification,
				new ArchetypeStressReport.SpecialtyReport().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.ComponentValueAttribution().evidenceClassification,
				new ArchetypeStressReport.ComponentValueAttribution().v6CompletionEligible);
		assertLegacy(new ArchetypeStressReport.MissingCapability().evidenceClassification,
				new ArchetypeStressReport.MissingCapability().v6CompletionEligible);
	}

	private static void assertLegacy(String classification, boolean eligible) {
		assertEquals(LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION, classification);
		assertFalse(eligible);
	}

	private static void assertExact(String label, Set<String> actual, Set<String> expected) {
		assertFalse(label + " was not parsed", actual.isEmpty());
		Set<String> unexpected = new HashSet<>(actual);
		unexpected.removeAll(expected);
		Set<String> missing = new HashSet<>(expected);
		missing.removeAll(actual);
		assertTrue(label + " changed; unexpected=" + unexpected + ", missing=" + missing,
				unexpected.isEmpty() && missing.isEmpty());
	}

	private static Set<String> publicTopLevelFields(String source) {
		Set<String> result = new HashSet<>();
		Matcher field = Pattern.compile("\\bpublic\\s+([^;(){}]+?)(?:\\s*=\\s*[^;]*)?;")
				.matcher(topLevelFieldsOnly(codeOnly(source)));
		while (field.find()) result.add(normalize(field.group(1)));
		return result;
	}

	private static Set<String> publicStaticMethods(String source) {
		Set<String> result = new HashSet<>();
		Matcher method = Pattern.compile("\\bpublic\\s+static\\s+([^;{}]+?\\([^;{}]*?\\))\\s*")
				.matcher(topLevelOnly(codeOnly(source)));
		while (method.find()) result.add(normalize(method.group(1)));
		return result;
	}

	private static Set<String> effectRegistryEntries(String source) {
		Set<String> result = new HashSet<>();
		Matcher entry = Pattern.compile("e\\(EffectSpec\\.Operation\\.([A-Z0-9_]+),\\s*Parameters\\.([A-Z0-9_]+)\\)")
				.matcher(codeOnly(source));
		while (entry.find()) result.add(entry.group(1) + ":" + entry.group(2));
		return result;
	}

	private static Set<String> componentRegistryEntries(String source) {
		Set<String> result = new HashSet<>();
		Matcher entry = Pattern.compile("([A-Z][A-Z0-9_]*)\\(Category\\.([A-Z0-9_]+),\\s*"
				+ "(?:ClassGameplayComponentSpec\\.Type\\.([A-Z0-9_]+)|(null))\\)")
				.matcher(codeOnly(source));
		while (entry.find()) result.add(entry.group(1) + ":" + entry.group(2) + ":"
				+ (entry.group(3) == null ? entry.group(4) : entry.group(3)));
		return result;
	}

	private static Set<String> componentTemplateNames(Set<String> entries) {
		Set<String> result = new HashSet<>();
		for (String entry : entries) result.add(entry.substring(0, entry.indexOf(':')));
		return result;
	}

	private static String between(String source, String start, String end) {
		int from = source.indexOf(start);
		int to = from < 0 ? -1 : source.indexOf(end, from + start.length());
		assertTrue("source section missing: " + start + " .. " + end, from >= 0 && to > from);
		return source.substring(from, to);
	}

	private static Set<String> stringLiterals(String source) {
		Set<String> result = new HashSet<>();
		Matcher value = Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"").matcher(source);
		while (value.find()) result.add(value.group(1));
		return result;
	}

	private static boolean containsAnyIdentifier(String source, Set<String> identifiers) {
		for (String identifier : identifiers) if (containsIdentifier(source, identifier)) return true;
		return false;
	}

	private static boolean containsIdentifier(String source, String identifier) {
		return Pattern.compile("(?<![A-Za-z0-9_$])" + Pattern.quote(identifier) + "(?![A-Za-z0-9_$])")
				.matcher(source).find();
	}

	private static String packageName(String source) {
		Matcher value = Pattern.compile("\\bpackage\\s+([A-Za-z0-9_.]+)\\s*;").matcher(source);
		assertTrue("package declaration missing", value.find());
		return value.group(1);
	}

	private static String codeOnly(String source) {
		StringBuilder result = new StringBuilder(source.length());
		boolean lineComment = false, blockComment = false, string = false, character = false, escaped = false;
		for (int i = 0; i < source.length(); i++) {
			char value = source.charAt(i);
			char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
			if (lineComment) {
				if (value == '\n') { lineComment = false; result.append('\n'); }
				else result.append(' ');
			} else if (blockComment) {
				if (value == '*' && next == '/') {
					result.append("  "); i++; blockComment = false;
				} else result.append(value == '\n' ? '\n' : ' ');
			} else if (string || character) {
				result.append(value == '\n' ? '\n' : ' ');
				if (escaped) escaped = false;
				else if (value == '\\') escaped = true;
				else if (string && value == '"') string = false;
				else if (character && value == '\'') character = false;
			} else if (value == '/' && next == '/') {
				result.append("  "); i++; lineComment = true;
			} else if (value == '/' && next == '*') {
				result.append("  "); i++; blockComment = true;
			} else if (value == '"') {
				result.append(' '); string = true;
			} else if (value == '\'') {
				result.append(' '); character = true;
			} else result.append(value);
		}
		return result.toString();
	}

	private static String topLevelFieldsOnly(String source) {
		StringBuilder result = new StringBuilder(source.length());
		int depth = 0;
		int boundary = 0;
		for (int i = 0; i < source.length(); i++) {
			char value = source.charAt(i);
			if (value == '{') {
				if (depth == 0) {
					depth = 1;
					result.append(' ');
					boundary = i + 1;
				} else if (depth == 1) {
					for (int j = boundary; j < result.length(); j++) result.setCharAt(j, ' ');
					depth = 2;
					result.append(' ');
				} else {
					depth++;
					result.append(' ');
				}
			} else if (value == '}') {
				depth--;
				result.append(' ');
				if (depth == 1) boundary = i + 1;
			} else {
				result.append(depth == 1 ? value : ' ');
				if (depth == 1 && value == ';') boundary = i + 1;
			}
		}
		return result.toString();
	}

	private static String topLevelOnly(String source) {
		StringBuilder result = new StringBuilder(source.length());
		int depth = 0;
		for (int i = 0; i < source.length(); i++) {
			char value = source.charAt(i);
			if (value == '{') {
				depth++;
				result.append(' ');
			} else if (value == '}') {
				depth--;
				result.append(' ');
			} else {
				result.append(depth == 1 ? value : ' ');
			}
		}
		return result.toString();
	}

	private static String normalize(String value) { return value.trim().replaceAll("\\s+", " "); }

	private static Set<String> enumValues(String source, String name) {
		String code = codeOnly(source);
		Matcher start = Pattern.compile("enum\\s+" + Pattern.quote(name) + "\\s*\\{").matcher(code);
		assertTrue("enum missing: " + name, start.find());
		int open = code.indexOf('{', start.start());
		int depth = 1;
		int end = open + 1;
		while (end < code.length() && depth > 0) {
			char value = code.charAt(end++);
			if (value == '{') depth++;
			else if (value == '}') depth--;
		}
		String body = code.substring(open + 1, end - 1);
		Set<String> result = new HashSet<>();
		int parentheses = 0, brackets = 0, braces = 0, segment = 0;
		for (int i = 0; i <= body.length(); i++) {
			char value = i == body.length() ? ';' : body.charAt(i);
			if (value == '(') parentheses++;
			else if (value == ')') parentheses--;
			else if (value == '[') brackets++;
			else if (value == ']') brackets--;
			else if (value == '{') braces++;
			else if (value == '}') braces--;
			else if ((value == ',' || value == ';') && parentheses == 0 && brackets == 0 && braces == 0) {
				Matcher constant = Pattern.compile("^\\s*([A-Z][A-Z0-9_]*)").matcher(body.substring(segment, i));
				if (constant.find()) result.add(constant.group(1));
				segment = i + 1;
				if (value == ';') break;
			}
		}
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
