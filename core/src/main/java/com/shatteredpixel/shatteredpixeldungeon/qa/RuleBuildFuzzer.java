package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.Restriction;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEffect;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTarget;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillConstraint;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.StartingKitSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.LawTraitRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.TraitSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Grammar-aware deterministic generator for current V0.2 class builds. */
public final class RuleBuildFuzzer {
	public static class Result {
		public FuzzReport fuzz;
		public CompatibilityReport compatibility;
	}

	private final RuleBuildAnalyzer analyzer = new RuleBuildAnalyzer();

	public Result run(int count, long seed, int headlessSamples) {
		long started = System.nanoTime();
		Result result = new Result();
		result.fuzz = new FuzzReport();
		result.compatibility = new CompatibilityReport();
		FuzzReport report = result.fuzz;
		report.seed = seed;
		int sampled = Math.min(Math.max(0, headlessSamples), count);
		int effective = 0;
		long totalTurns = 0;

		for (int i = 0; i < count; i++) {
			long buildSeed = buildSeed(seed, i);
			ClassBuild config = generateClassBuild(buildSeed, i);
			report.totalGenerated++;
			if (config.valid()) report.syntacticValid++;
			long staticStarted = System.nanoTime();
			BuildAnalysis analysis = analyzer.analyze(RuleBuild.from(config));
			report.staticAnalysisNanos += System.nanoTime() - staticStarted;
			incrementClassification(report, analysis.classification);
			for (BuildAnalysis.Finding finding : analysis.findings) increment(report.findingCounts, finding.code);
			recordUsage(report, config, analysis.classification);
			recordCompatibility(result.compatibility, config, analysis.classification);

			boolean runtimeFailed = false;
			boolean dynamicFailure = false;
			ScenarioResult scenarioResult = null;
			if (report.headlessSampled < sampled && analysis.classification != BuildAnalysis.Classification.BROKEN) {
				QaScenario scenario = fuzzScenario(config, buildSeed, i);
				long headlessStarted = System.nanoTime();
				scenarioResult = new HeadlessGameplayHarness().run(scenario);
				report.headlessNanos += System.nanoTime() - headlessStarted;
				report.headlessSampled++;
				totalTurns += scenarioResult.turn;
				runtimeFailed = scenarioResult.runtimeFailure;
				dynamicFailure = scenarioResult.findings.contains("UNBOUNDED_POWER_LOOP")
						|| scenarioResult.findings.contains("UNBOUNDED_RESOURCE_LOOP")
						|| scenarioResult.findings.contains("UNBOUNDED_ACTOR_LOOP");
				if (runtimeFailed) report.runtimeFailed++;
				else report.headlessRunnable++;
				if (dynamicFailure) report.dynamicFailures++;
				if (runtimeFailed || dynamicFailure) {
					recordRuntimeFailure(result.compatibility, config);
					recordUsageFailure(report, config);
					addFailure(report, config, buildSeed, scenarioResult,
							runtimeFailed ? scenarioResult.failure : "dynamic positive loop");
				}
			}
			if (analysis.classification != BuildAnalysis.Classification.BROKEN && !runtimeFailed && !dynamicFailure) effective++;
		}

		report.basicEffectiveCombinationDensity = report.syntacticValid == 0 ? 0
				: 100.0 * effective / report.syntacticValid;
		report.validDensity = report.totalGenerated == 0 ? 0 : 100.0 * report.staticValid / report.totalGenerated;
		report.riskyDensity = report.totalGenerated == 0 ? 0 : 100.0 * report.risky / report.totalGenerated;
		report.brokenDensity = report.totalGenerated == 0 ? 0 : 100.0 * report.broken / report.totalGenerated;
		report.nonBrokenDensity = report.totalGenerated == 0 ? 0
				: 100.0 * (report.staticValid + report.risky) / report.totalGenerated;
		report.runtimeFailureRate = report.headlessSampled == 0 ? 0
				: 100.0 * report.runtimeFailed / report.headlessSampled;
		report.exploitRate = report.headlessSampled == 0 ? 0
				: 100.0 * report.dynamicFailures / report.headlessSampled;
		report.elapsedMillis = (System.nanoTime() - started) / 1_000_000L;
		report.headlessTurnsPerSecond = report.headlessNanos == 0 ? 0
				: totalTurns * 1_000_000_000.0 / report.headlessNanos;
		findDeadVocabulary(report);
		return result;
	}

	/** Legacy projection retained for old callers; new QA should use generateClassBuild. */
	public CustomClassConfig generateBuild(long seed, int index) {
		return CustomClassConfig.fromClassBuild(generateClassBuild(seed, index));
	}

	public ClassBuild generateClassBuild(long seed, int index) {
		Random random = new Random(seed);
		for (int attempt = 0; attempt < 400; attempt++) {
			ClassBuild build = new ClassBuild();
			build.name = String.format("build_%05d", index);
			int resourceCount = random.nextInt(3);
			ArrayList<ResourceEngine> availableResources = new ArrayList<>();
			for (ResourceEngine engine : ResourceEngine.values()) availableResources.add(engine);
			for (int i = 0; i < resourceCount; i++) {
				build.resources.add(new ResourceSpec(availableResources.remove(random.nextInt(availableResources.size()))));
			}
			int restrictionCount = random.nextInt(3);
			addUniqueEnums(random, build.restrictions, nonEmptyRestrictions(), restrictionCount);
			build.startingKit.mode = random.nextInt(5) == 0
					? StartingKitSpec.Mode.UNARMED : StartingKitSpec.Mode.STANDARD_DUNGEON_KIT;
			boolean weaponless = build.startingKit.mode == StartingKitSpec.Mode.UNARMED
					|| build.restrictions.contains(Restriction.NO_ORDINARY_WEAPONS);
			int skillCount = weaponless ? 1 + random.nextInt(4) : random.nextInt(5);
			for (int i = 0; i < skillCount; i++) build.skills.add(generateSkill(random, build, i));
			if (weaponless && !hasEnemyResolution(build)) build.skills.set(0, enemyPoisonSkill());
			// STATE costs are only legal when the class can create the CHARGED state.  Skills are
			// generated before traits, so install the ordinary player-visible Accumulation trait
			// here instead of making STATE an unreachable branch of the fuzzer.
			if (hasStateCost(build) && !LawTraitRegistry.hasTrait(build, CoreRuleVocabulary.ACCUMULATION)) {
				build.traits.add(TraitSpec.of(CoreRuleVocabulary.ACCUMULATION));
			}
			ArrayList<ClassLaw> lawOptions = new ArrayList<>(LawTraitRegistry.laws());
			Collections.shuffle(lawOptions, random);
			int wantedLaws = random.nextInt(3);
			for (ClassLaw law : lawOptions) if (build.laws.size() < wantedLaws
					&& LawTraitRegistry.lawIssue(build, law) == null) build.laws.add(law);
			ArrayList<LawTraitRegistry.TraitOption> traitOptions = new ArrayList<>(LawTraitRegistry.traitOptions(build));
			Collections.shuffle(traitOptions, random);
			int wantedTraits = random.nextInt(4);
			for (LawTraitRegistry.TraitOption option : traitOptions) {
				if (build.traits.size() >= wantedTraits) break;
				if (option.compatible() && !containsTrait(build, option.spec)) build.traits.add(option.spec.copy());
			}
			if (build.structurallyValid() && build.budgetValid()) return build;
		}
		throw new IllegalStateException("unable to generate compatible class build for seed " + seed);
	}

	private static boolean containsTrait(ClassBuild build, TraitSpec candidate) {
		for (TraitSpec value : build.traits) if (value.stableId().equals(candidate.stableId())) return true;
		return false;
	}

	private static boolean hasStateCost(ClassBuild build) {
		for (SkillSpec skill : build.skills) if (skill.cost.type == RuleCost.Type.STATE) return true;
		return false;
	}

	private static SkillSpec generateSkill(Random random, ClassBuild build, int index) {
		SkillSpec skill = new SkillSpec();
		skill.id = "skill_" + index;
		skill.activation = random.nextInt(3) == 0 ? RuleEvent.ACTIVE : pick(random, reactions());
		boolean structured = random.nextBoolean();
		if (structured) {
			skill.primary = pickStructuredEffect(random, build);
			skill.targeting = targetingFor(random, skill.primary, skill.activation);
			skill.delivery = deliveryFor(random, skill.targeting, skill.activation, skill.primary);
		} else {
			skill.primary = new EffectSpec(pickEffect(random, skill.activation), 1 + random.nextInt(3));
			skill.targeting = new TargetingSpec(pickTarget(random, skill.activation, skill.primary.variant));
			skill.delivery = skill.targeting.selector == TargetingSpec.Selector.SELF ? SkillDelivery.SELF
					: skill.targeting.selector == TargetingSpec.Selector.SELECTED_CELL
					? SkillDelivery.GROUND_PLACEMENT : SkillDelivery.DIRECT_TARGET;
		}
		skill.conditions.clear();
		ArrayList<RuleCondition.Type> conditionValues = new ArrayList<>();
		for (RuleCondition.Type value : RuleCondition.Type.values()) {
			if (value != RuleCondition.Type.RESOURCE_AT_LEAST || !build.resources.isEmpty()) conditionValues.add(value);
		}
		if (!hasModeProducer(build)) conditionValues.remove(RuleCondition.Type.MODE_IS);
		RuleCondition.Type condition = conditionValues.get(random.nextInt(conditionValues.size()));
		int conditionParameter = parameter(condition, random);
		if (condition == RuleCondition.Type.RESOURCE_AT_LEAST && !build.resources.isEmpty()
				&& build.resources.get(0).engine != ResourceEngine.BLOOD) {
			conditionParameter = 1 + random.nextInt(Math.max(1, build.resources.get(0).engine.max));
		}
		skill.conditions.add(new RuleCondition(condition, conditionParameter));
		skill.modifier = pickModifier(random, skill.primary, skill.delivery);
		if (random.nextInt(4) == 0) {
			if (random.nextBoolean()) skill.secondary = pickStructuredEffect(random, build);
			else {
				RuleEffect.Type secondary = pickEffectForTarget(random, skill.targeting.compileType(skill.activation));
				skill.secondary = new EffectSpec(secondary, 1 + random.nextInt(2));
			}
		}
		int costRoll = random.nextInt(8);
		if (!build.resources.isEmpty() && costRoll <= 1) {
			ResourceSpec resource = build.resources.get(random.nextInt(build.resources.size()));
			skill.cost = new RuleCost(RuleCost.Type.RESOURCE, 1 + random.nextInt(3));
			skill.cost.resourceId = resource.id;
			skill.cost.resourceEngine = resource.engine;
		} else if (costRoll == 2) {
			skill.cost = new RuleCost(RuleCost.Type.HP, 1 + random.nextInt(3));
		} else if (costRoll == 3 && skill.activation == RuleEvent.ACTIVE) {
			skill.cost = new RuleCost(RuleCost.Type.ACTION, 2 + random.nextInt(2));
		} else if (costRoll == 4) {
			skill.cost = new RuleCost(RuleCost.Type.COOLDOWN, 3 + random.nextInt(3));
		} else if (costRoll == 5) {
			skill.cost = new RuleCost(RuleCost.Type.CONSUMABLE, 1);
			skill.cost.reference = PotionOfHealing.class.getName();
		} else if (costRoll == 6 && CoreRuleVocabulary.supportsAccumulation(skill.activation)) {
			skill.cost = new RuleCost(RuleCost.Type.STATE, 1);
			skill.cost.reference = RuleMark.Type.CHARGED.name();
		} else {
			skill.cost = new RuleCost(RuleCost.Type.NONE, 0);
		}
		if (random.nextInt(4) == 0) {
			ArrayList<SkillConstraint.Variant> values = new ArrayList<>();
			values.add(SkillConstraint.Variant.TARGET_MARKED);
			values.add(SkillConstraint.Variant.SELF_LOW_HP);
			values.add(SkillConstraint.Variant.SELF_IN_WATER);
			if (skill.cost.type != RuleCost.Type.COOLDOWN) values.add(SkillConstraint.Variant.COOLDOWN);
			if (skill.cost.type == RuleCost.Type.HP) values.add(SkillConstraint.Variant.HP_COMMITMENT);
			SkillConstraint.Variant value = values.get(random.nextInt(values.size()));
			skill.constraint = new SkillConstraint(value, value == SkillConstraint.Variant.SELF_LOW_HP ? 30 : 2);
		}
		return skill;
	}

	private static EffectSpec pickStructuredEffect(Random random, ClassBuild build) {
		ArrayList<EffectSpec.Operation> values = new ArrayList<>();
		for (EffectSpec.Operation operation : EffectSpec.Operation.values()) {
			if (operation == EffectSpec.Operation.LEGACY) continue;
			EffectSpec candidate = new EffectSpec(familyFor(operation), operation, 2);
			if (!candidate.implemented()) continue;
			if (operation == EffectSpec.Operation.RESOURCE_CONVERT && build.resources.size() < 2) continue;
			if (candidate.family == com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.RESOURCE_OPERATION
					&& build.resources.isEmpty()) continue;
			if ((operation == EffectSpec.Operation.RELATION_COMMAND_FOLLOW
					|| operation == EffectSpec.Operation.RELATION_COMMAND_ATTACK
					|| operation == EffectSpec.Operation.RELATION_COMMAND_GUARD)
					&& !hasOwnedEntityProducer(build)) continue;
			if (operation == EffectSpec.Operation.RELATION_INHERIT
					&& (!hasOwnedEntityProducer(build) || !hasModeProducer(build))) continue;
			values.add(operation);
		}
		EffectSpec.Operation operation = values.get(random.nextInt(values.size()));
		EffectSpec result = new EffectSpec(familyFor(operation), operation, 1 + random.nextInt(5));
		result.duration = 2 + random.nextInt(7);
		result.lifetime = 3 + random.nextInt(8);
		result.count = 1 + random.nextInt(2);
		result.period = 1 + random.nextInt(3);
		if (result.family == com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.RESOURCE_OPERATION) {
			ResourceSpec source = build.resources.get(random.nextInt(build.resources.size()));
			result.resourceId = source.id;
			if (operation == EffectSpec.Operation.RESOURCE_CONVERT) {
				ResourceSpec target = build.resources.get(random.nextInt(build.resources.size()));
				while (target.id.equals(source.id)) target = build.resources.get(random.nextInt(build.resources.size()));
				result.targetResourceId = target.id;
			}
		}
		if (result.family == com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.DAMAGE) {
			result.damageType = pick(random, EffectSpec.DamageType.values());
			EffectSpec.ScalingSource[] scaling = build.resources.isEmpty()
					? new EffectSpec.ScalingSource[]{EffectSpec.ScalingSource.FIXED,EffectSpec.ScalingSource.HERO_LEVEL}
					: EffectSpec.ScalingSource.values();
			result.scalingSource = pick(random, scaling);
			if (result.scalingSource == EffectSpec.ScalingSource.CURRENT_RESOURCE) {
				result.resourceId = build.resources.get(random.nextInt(build.resources.size())).id;
			}
		}
		if (result.family == com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.STATUS) {
			result.statusStacking = pick(random, EffectSpec.StatusStacking.values());
		}
		if (result.family == com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.MARK_ACCUMULATION) {
			result.stateId = pick(random, RuleMark.Type.values()).name();
		}
		if (operation == EffectSpec.Operation.TRANSFORM_MODE) {
			result.stateId = pick(random, new String[]{"assault","guard","flow"});
		}
		if (operation == EffectSpec.Operation.CREATE_DEVICE || operation == EffectSpec.Operation.CREATE_TRAP
				|| operation == EffectSpec.Operation.CREATE_FIELD) {
			result.stateId = pick(random, new String[]{"fire","poison","heal"});
		}
		return result;
	}

	private static com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily familyFor(EffectSpec.Operation operation) {
		String name = operation.name();
		if (name.startsWith("DAMAGE")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.DAMAGE;
		if (name.startsWith("STATUS")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.STATUS;
		if (name.startsWith("MOVE")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.MOVEMENT;
		if (name.startsWith("RECOVER") || name.startsWith("DEFENSE")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.RECOVERY_DEFENSE;
		if (name.startsWith("RESOURCE")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.RESOURCE_OPERATION;
		if (name.startsWith("MARK")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.MARK_ACCUMULATION;
		if (name.startsWith("CREATE")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.CREATE_ENTITY;
		if (name.startsWith("WORLD")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.WORLD_TERRAIN;
		if (name.startsWith("RELATION")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.RELATION_CONTROL;
		if (name.startsWith("TRANSFER") || name.startsWith("COPY") || name.startsWith("SWAP")) return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.TRANSFER_COPY;
		return com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.TRANSFORM;
	}

	private static TargetingSpec targetingFor(Random random, EffectSpec effect, RuleEvent activation) {
		TargetingSpec target = new TargetingSpec();
		if (effect.operation == EffectSpec.Operation.MOVE_DASH) {
			target.selector = TargetingSpec.Selector.SELECTED_CELL;
			target.filter = TargetingSpec.Filter.ANY;
			return target;
		}
		switch (effect.family) {
			case RESOURCE_OPERATION:
			case RECOVERY_DEFENSE:
			case TRANSFORM:
				target.selector = TargetingSpec.Selector.SELF; target.filter = TargetingSpec.Filter.SELF; break;
			case CREATE_ENTITY:
			case WORLD_TERRAIN:
				target.selector = TargetingSpec.Selector.SELECTED_CELL; target.filter = TargetingSpec.Filter.ANY; break;
			case RELATION_CONTROL:
				target.selector = TargetingSpec.Selector.ALL_MATCHING; target.filter = TargetingSpec.Filter.OWNED_ENTITY; break;
			default:
				target.selector = activation == RuleEvent.ACTIVE ? TargetingSpec.Selector.SELECTED_ACTOR : TargetingSpec.Selector.NEAREST;
				target.filter = TargetingSpec.Filter.ENEMY;
		}
		if (target.filter == TargetingSpec.Filter.ENEMY && random.nextInt(3) == 0) {
			target.coverage = pick(random, new TargetingSpec.Coverage[]{TargetingSpec.Coverage.SINGLE,
					TargetingSpec.Coverage.ADJACENT,TargetingSpec.Coverage.RADIUS,TargetingSpec.Coverage.CONE,
					TargetingSpec.Coverage.RING,TargetingSpec.Coverage.CHAIN});
			target.maxTargets = 2 + random.nextInt(4); target.magnitude = 1 + random.nextInt(3);
		}
		return target;
	}

	private static SkillDelivery deliveryFor(Random random, TargetingSpec target, RuleEvent activation, EffectSpec effect) {
		if (target.selector == TargetingSpec.Selector.SELF) {
			return activation == RuleEvent.ACTIVE && random.nextInt(8) == 0
					? SkillDelivery.ACTION_ATTACHMENT : SkillDelivery.SELF;
		}
		if (target.selector == TargetingSpec.Selector.SELECTED_CELL) {
			return random.nextInt(5) == 0 && effect.family != com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.CREATE_ENTITY
					? SkillDelivery.PERSISTENT_CARRIER : SkillDelivery.GROUND_PLACEMENT;
		}
		if (activation != RuleEvent.ACTIVE) return SkillDelivery.DIRECT_TARGET;
		return pick(random, new SkillDelivery[]{SkillDelivery.DIRECT_TARGET,SkillDelivery.CONTACT_ATTACK,
				SkillDelivery.PROJECTILE,SkillDelivery.TRACE_BEAM});
	}

	private static RuleModifier pickModifier(Random random, EffectSpec effect, SkillDelivery delivery) {
		ArrayList<RuleModifier> values = new ArrayList<>();
		for (RuleModifier.Type value : RuleModifier.Type.values()) {
			RuleModifier modifier = new RuleModifier(value);
			if (modifier.compatible(effect, delivery)) values.add(modifier);
		}
		return values.get(random.nextInt(values.size()));
	}

	private static boolean hasEnemyResolution(ClassBuild build) {
		for (SkillSpec skill : build.skills) {
			RuleTarget.Type target = skill.targeting.compileType(skill.activation);
			boolean enemyTarget = target != RuleTarget.Type.SELF;
			if (!enemyTarget) continue;
			if (isResolutionEffect(skill.primary)
					|| skill.secondary != null && isResolutionEffect(skill.secondary)) return true;
		}
		return false;
	}

	private static boolean hasOwnedEntityProducer(ClassBuild build){
		for(SkillSpec skill:build.skills)if(skill.primary!=null&&skill.primary.operation==EffectSpec.Operation.CREATE_ACTOR)return true;
		return false;
	}
	private static boolean hasModeProducer(ClassBuild build){
		for(SkillSpec skill:build.skills)if(skill.primary!=null&&skill.primary.operation==EffectSpec.Operation.TRANSFORM_MODE)return true;
		return false;
	}

	private static boolean isResolutionEffect(EffectSpec value) {
		if (value == null) return false;
		if (value.family == com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.DAMAGE) return true;
		if (value.family == com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.CREATE_ENTITY) return true;
		if (value.operation == EffectSpec.Operation.WORLD_FIRE || value.operation == EffectSpec.Operation.WORLD_TOXIC_GAS) return true;
		return isResolutionEffect(value.variant);
	}

	private static boolean isResolutionEffect(RuleEffect.Type value) {
		return value == RuleEffect.Type.POISON || value == RuleEffect.Type.FIRE
				|| value == RuleEffect.Type.BLEED || value == RuleEffect.Type.CREATE_GAS;
	}

	private static SkillSpec enemyPoisonSkill() {
		SkillSpec skill = new SkillSpec();
		skill.id = "skill_0";
		skill.activation = RuleEvent.ACTIVE;
		skill.primary = new EffectSpec(RuleEffect.Type.POISON, 2);
		skill.targeting = new TargetingSpec(RuleTarget.Type.SELECTED_TARGET);
		skill.delivery = SkillDelivery.DIRECT_TARGET;
		skill.cost = new RuleCost(RuleCost.Type.NONE, 0);
		return skill;
	}

	private static RuleEffect.Type pickEffectForTarget(Random random, RuleTarget.Type target) {
		ArrayList<RuleEffect.Type> values = new ArrayList<>();
		for (RuleEffect.Type effect : RuleEffect.Type.values()) if (RuleEffect.compatibleTarget(effect, target)) values.add(effect);
		return values.get(random.nextInt(values.size()));
	}

	private static CoreRuleVocabulary[] nonEmptyVocabulary() {
		ArrayList<CoreRuleVocabulary> result = new ArrayList<>();
		for (CoreRuleVocabulary value : CoreRuleVocabulary.values()) if (value != CoreRuleVocabulary.NONE) result.add(value);
		return result.toArray(new CoreRuleVocabulary[0]);
	}

	private static Restriction[] nonEmptyRestrictions() {
		ArrayList<Restriction> result = new ArrayList<>();
		for (Restriction value : Restriction.values()) if (value != Restriction.NONE) result.add(value);
		return result.toArray(new Restriction[0]);
	}

	private static <T> void addUniqueEnums(Random random, ArrayList<T> target, T[] values, int count) {
		ArrayList<T> remaining = new ArrayList<>();
		java.util.Collections.addAll(remaining, values);
		while (target.size() < count && !remaining.isEmpty()) target.add(remaining.remove(random.nextInt(remaining.size())));
	}

	private CustomClassConfig generateLegacyBuild(long seed, int index) {
		Random random = new Random(seed);
		for (int attempt = 0; attempt < 200; attempt++) {
			CustomClassConfig c = new CustomClassConfig();
			c.name = String.format("build_%05d", index);
			c.resource = pick(random, ResourceEngine.values());
			c.law = pick(random, ClassLaw.values());
			c.restriction = pick(random, Restriction.values());
			c.activeEffect = pickEffect(random, RuleEvent.ACTIVE);
			c.activeTarget = pickTarget(random, RuleEvent.ACTIVE, c.activeEffect);
			c.activeCondition = pick(random, RuleCondition.Type.values());
			c.activeConditionParameter = parameter(c.activeCondition, random);
			c.activeModifier = pickModifier(random, c.activeEffect);

			RuleEvent[] reactions = reactions();
			c.reactionTrigger = pick(random, reactions);
			c.reactionEffect = pickEffect(random, c.reactionTrigger);
			c.reactionTarget = pickTarget(random, c.reactionTrigger, c.reactionEffect);
			c.reactionCondition = pick(random, RuleCondition.Type.values());
			c.reactionConditionParameter = parameter(c.reactionCondition, random);
			if (random.nextInt(4) == 0) {
				c.reactionCondition2 = pick(random, RuleCondition.Type.values());
				c.reactionConditionParameter2 = parameter(c.reactionCondition2, random);
			}
			c.reactionModifier = pickModifier(random, c.reactionEffect);
			c.vocabulary1 = pick(random, CoreRuleVocabulary.values());
			c.vocabulary2 = pick(random, CoreRuleVocabulary.values());
			if (c.valid()) return c;
		}
		throw new IllegalStateException("unable to generate compatible build for seed " + seed);
	}

	public static long buildSeed(long seed, int index) {
		long value = seed + 0x9E3779B97F4A7C15L * (index + 1L);
		value ^= value >>> 30;
		value *= 0xBF58476D1CE4E5B9L;
		value ^= value >>> 27;
		value *= 0x94D049BB133111EBL;
		return value ^ value >>> 31;
	}

	private static QaScenario fuzzScenario(ClassBuild config, long seed, int index) {
		QaScenario scenario = new QaScenario();
		scenario.id = "random_combat_" + index;
		scenario.seed = seed;
		scenario.hero.classBuild = config.copy();
		ExploitPolicy.values()[index % ExploitPolicy.values().length].append(scenario, config);
		return scenario;
	}

	private static RuleTarget.Type pickTarget(Random random, RuleEvent event, RuleEffect.Type effect) {
		ArrayList<RuleTarget.Type> values = new ArrayList<>();
		for (RuleTarget.Type target : RuleTarget.Type.values()) {
			if (RuleTarget.availableForEvent(target, event) && RuleEffect.compatibleTarget(effect, target)) values.add(target);
		}
		if (values.isEmpty()) throw new IllegalStateException("no compatible target for " + event + "/" + effect);
		return values.get(random.nextInt(values.size()));
	}

	private static RuleEffect.Type pickEffect(Random random, RuleEvent event) {
		ArrayList<RuleEffect.Type> values = new ArrayList<>();
		for (RuleEffect.Type effect : RuleEffect.Type.values()) {
			for (RuleTarget.Type target : RuleTarget.Type.values()) {
				if (RuleTarget.availableForEvent(target, event) && RuleEffect.compatibleTarget(effect, target)) {
					values.add(effect);
					break;
				}
			}
		}
		if (values.isEmpty()) throw new IllegalStateException("no compatible effect for " + event);
		return values.get(random.nextInt(values.size()));
	}

	private static RuleModifier.Type pickModifier(Random random, RuleEffect.Type effect) {
		ArrayList<RuleModifier.Type> values = new ArrayList<>();
		for (RuleModifier.Type modifier : RuleModifier.Type.values()) {
			if (new RuleModifier(modifier).compatible(effect)) values.add(modifier);
		}
		return values.get(random.nextInt(values.size()));
	}

	private static int parameter(RuleCondition.Type condition, Random random) {
		switch (condition) {
			case SELF_HP_BELOW:
			case TARGET_HP_BELOW: return 20 + random.nextInt(61);
			case DISTANCE_AT_LEAST: return 2 + random.nextInt(4);
			case ADJACENT_ENEMIES_AT_LEAST: return 1 + random.nextInt(3);
			case RESOURCE_AT_LEAST: return 1 + random.nextInt(15);
			default: return 0;
		}
	}

	private static RuleEvent[] reactions() {
		ArrayList<RuleEvent> result = new ArrayList<>();
		for (RuleEvent event : RuleEvent.values()) if (event != RuleEvent.ACTIVE) result.add(event);
		return result.toArray(new RuleEvent[0]);
	}

	private static <T> T pick(Random random, T[] values) { return values[random.nextInt(values.length)]; }

	private static void incrementClassification(FuzzReport report, BuildAnalysis.Classification value) {
		if (value == BuildAnalysis.Classification.VALID) report.staticValid++;
		else if (value == BuildAnalysis.Classification.RISKY) report.risky++;
		else report.broken++;
	}

	private static void increment(Map<String, Integer> values, String key) {
		values.put(key, values.containsKey(key) ? values.get(key) + 1 : 1);
	}

	private static void recordUsage(FuzzReport report, ClassBuild c, BuildAnalysis.Classification classification) {
		for (ResourceSpec value : c.resources) usage(report, "Resource", value.engine.name(), classification);
		for (ClassLaw value : c.laws) usage(report, "Law", value.name(), classification);
		for (Restriction value : c.restrictions) usage(report, "Restriction", value.name(), classification);
		for (TraitSpec value : c.traits) usage(report, "CoreVocabulary", value.type.name(), classification);
		for (RuleDefinition rule : c.compileRules()) {
			usage(report, "Trigger", rule.trigger.event.name(), classification);
			for (RuleCondition condition : rule.conditions) usage(report, "Condition", condition.type.name(), classification);
			usage(report, "Effect", rule.effect.type.name(), classification);
			usage(report, "Target", rule.target.type.name(), classification);
			usage(report, "Modifier", rule.modifier.type.name(), classification);
			if (rule.effectSpec != null) {
				usage(report, "EffectFamily", rule.effectSpec.family.name(), classification);
				usage(report, "EffectOperation", rule.effectSpec.operation.name(), classification);
				if(rule.effectSpec.family==com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.DAMAGE){usage(report,"DamageType",rule.effectSpec.damageType.name(),classification);usage(report,"ScalingSource",rule.effectSpec.scalingSource.name(),classification);}
				if(rule.effectSpec.family==com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.STATUS)usage(report,"StatusStacking",rule.effectSpec.statusStacking.name(),classification);
			}
			if (rule.delivery != null) usage(report, "Delivery", rule.delivery.name(), classification);
			if (rule.targetingSpec != null) {
				usage(report, "Selector", rule.targetingSpec.selector.name(), classification);
				usage(report, "Coverage", rule.targetingSpec.coverage.name(), classification);
				usage(report, "Filter", rule.targetingSpec.filter.name(), classification);
			}
		}
	}

	private static void usage(FuzzReport report, String category, String value,
			BuildAnalysis.Classification classification) {
		LinkedHashMap<String, FuzzReport.Usage> categoryMap = report.vocabularyUsage.computeIfAbsent(category,
				ignored -> new LinkedHashMap<>());
		FuzzReport.Usage usage = categoryMap.computeIfAbsent(value, ignored -> new FuzzReport.Usage());
		usage.total++;
		if (classification == BuildAnalysis.Classification.VALID) usage.valid++;
		else if (classification == BuildAnalysis.Classification.RISKY) usage.risky++;
		else usage.broken++;
	}

	private static void recordUsageFailure(FuzzReport report, ClassBuild c) {
		for (ResourceSpec value : c.resources) failureUsage(report, "Resource", value.engine.name());
		for (ClassLaw value : c.laws) failureUsage(report, "Law", value.name());
		for (Restriction value : c.restrictions) failureUsage(report, "Restriction", value.name());
		for (TraitSpec value : c.traits) failureUsage(report, "CoreVocabulary", value.type.name());
		for (RuleDefinition rule : c.compileRules()) {
			failureUsage(report, "Trigger", rule.trigger.event.name());
			for (RuleCondition condition : rule.conditions) failureUsage(report, "Condition", condition.type.name());
			failureUsage(report, "Effect", rule.effect.type.name());
			failureUsage(report, "Target", rule.target.type.name());
			failureUsage(report, "Modifier", rule.modifier.type.name());
			if (rule.effectSpec != null) {
				failureUsage(report, "EffectFamily", rule.effectSpec.family.name());
				failureUsage(report, "EffectOperation", rule.effectSpec.operation.name());
				if(rule.effectSpec.family==com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.DAMAGE){failureUsage(report,"DamageType",rule.effectSpec.damageType.name());failureUsage(report,"ScalingSource",rule.effectSpec.scalingSource.name());}
				if(rule.effectSpec.family==com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily.STATUS)failureUsage(report,"StatusStacking",rule.effectSpec.statusStacking.name());
			}
			if (rule.delivery != null) failureUsage(report, "Delivery", rule.delivery.name());
			if (rule.targetingSpec != null) {
				failureUsage(report, "Selector", rule.targetingSpec.selector.name());
				failureUsage(report, "Coverage", rule.targetingSpec.coverage.name());
				failureUsage(report, "Filter", rule.targetingSpec.filter.name());
			}
		}
	}

	private static void failureUsage(FuzzReport report, String category, String value) {
		report.vocabularyUsage.get(category).get(value).runtimeFailed++;
	}

	private static void findDeadVocabulary(FuzzReport report) {
		for (Map.Entry<String, LinkedHashMap<String, FuzzReport.Usage>> category : report.vocabularyUsage.entrySet()) {
			for (Map.Entry<String, FuzzReport.Usage> entry : category.getValue().entrySet()) {
				FuzzReport.Usage usage = entry.getValue();
				int nonBroken = usage.valid + usage.risky;
				if (usage.total >= 10 && (nonBroken == 0 || nonBroken * 10 < usage.total)) {
					report.potentialDeadVocabulary.add(category.getKey() + ":" + entry.getKey()
							+ " nonBroken=" + nonBroken + "/" + usage.total);
				}
			}
		}
	}

	private static void recordCompatibility(CompatibilityReport report, ClassBuild c,
			BuildAnalysis.Classification classification) {
		for (RuleDefinition rule : c.compileRules()) {
			cell(report.triggerTarget, rule.trigger.event + "|" + rule.target.type, classification);
			for (RuleCondition condition : rule.conditions) {
				cell(report.conditionEffect, condition.type + "|" + rule.effect.type, classification);
			}
			cell(report.effectModifier, rule.effect.type + "|" + rule.modifier.type, classification);
			if (rule.effectSpec != null && rule.delivery != null) {
				cell(report.deliveryEffect, rule.delivery + "|" + rule.effectSpec.operation, classification);
			}
			if (rule.targetingSpec != null) {
				cell(report.selectorCoverage, rule.targetingSpec.selector + "|" + rule.targetingSpec.coverage, classification);
				cell(report.coverageFilter, rule.targetingSpec.coverage + "|" + rule.targetingSpec.filter, classification);
			}
			cell(report.resourceCost, costResource(c, rule.cost) + "|" + rule.cost.type, classification);
		}
		for (ClassLaw law : c.laws) for (Restriction restriction : c.restrictions) {
			cell(report.lawRestriction, law + "|" + restriction, classification);
		}
		for (TraitSpec left : c.traits) for (TraitSpec right : c.traits) {
			if (left.type.ordinal() < right.type.ordinal()) cell(report.vocabularyPair, left.type + "|" + right.type, classification);
		}
	}

	private static void cell(LinkedHashMap<String, CompatibilityReport.Cell> values, String key,
			BuildAnalysis.Classification classification) {
		CompatibilityReport.Cell cell = values.computeIfAbsent(key, ignored -> new CompatibilityReport.Cell());
		cell.generated++;
		if (classification == BuildAnalysis.Classification.VALID) cell.valid++;
		else if (classification == BuildAnalysis.Classification.RISKY) cell.risky++;
		else cell.broken++;
	}

	private static void recordRuntimeFailure(CompatibilityReport report, ClassBuild c) {
		for (RuleDefinition rule : c.compileRules()) {
			failureCell(report.triggerTarget, rule.trigger.event + "|" + rule.target.type);
			for (RuleCondition condition : rule.conditions) failureCell(report.conditionEffect, condition.type + "|" + rule.effect.type);
			failureCell(report.effectModifier, rule.effect.type + "|" + rule.modifier.type);
			if (rule.effectSpec != null && rule.delivery != null) failureCell(report.deliveryEffect,
					rule.delivery + "|" + rule.effectSpec.operation);
			if (rule.targetingSpec != null) {
				failureCell(report.selectorCoverage, rule.targetingSpec.selector + "|" + rule.targetingSpec.coverage);
				failureCell(report.coverageFilter, rule.targetingSpec.coverage + "|" + rule.targetingSpec.filter);
			}
			failureCell(report.resourceCost, costResource(c, rule.cost) + "|" + rule.cost.type);
		}
		for (ClassLaw law : c.laws) for (Restriction restriction : c.restrictions) {
			failureCell(report.lawRestriction, law + "|" + restriction);
		}
		for (TraitSpec left : c.traits) for (TraitSpec right : c.traits) {
			if (left.type.ordinal() < right.type.ordinal()) failureCell(report.vocabularyPair, left.type + "|" + right.type);
		}
	}

	private static String costResource(ClassBuild build, RuleCost cost) {
		if (cost.type != RuleCost.Type.RESOURCE) return "NONE";
		ResourceSpec value = cost.resourceId == null ? null : build.resource(cost.resourceId);
		return value == null ? "MISSING" : value.engine.name();
	}

	private static void failureCell(LinkedHashMap<String, CompatibilityReport.Cell> values, String key) {
		CompatibilityReport.Cell cell = values.get(key);
		if (cell != null) cell.runtimeFailed++;
	}

	private static void addFailure(FuzzReport report, ClassBuild config, long seed,
			ScenarioResult scenario, String reason) {
		FuzzReport.Failure failure = new FuzzReport.Failure();
		failure.id = config.name;
		failure.classification = scenario.classification;
		failure.reason = reason;
		failure.seed = seed;
		failure.buildConfig = RuleBuild.from(config).fingerprint();
		failure.scenario = scenario.id;
		failure.turn = scenario.turn;
		failure.tracePath = scenario.tracePath;
		failure.traceText = scenario.traceText;
		report.failures.add(failure);
	}
}
