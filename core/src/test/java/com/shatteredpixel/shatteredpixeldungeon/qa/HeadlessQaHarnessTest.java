package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.LawTraitRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.Restriction;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEffect;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEventBridge;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleMarkCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTarget;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleSemanticTag;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.*;

public class HeadlessQaHarnessTest {
	private static HeadlessApplication app;

	@BeforeClass
	public static void startHeadless() {
		Game.version = "3.3.8-INDEV-headless-test";
		Game.versionCode = 896;
		app = new HeadlessApplication(new ApplicationAdapter() {}, new HeadlessApplicationConfiguration());
	}

	@AfterClass
	public static void stopHeadless() {
		if (app != null) app.exit();
	}

	@Test
	public void scenarioJsonIsVersionedAndRoundTrips() {
		QaScenario source = RegressionScenarios.momentumLoop();
		QaScenario restored = QaScenario.fromJson(source.toJson());
		assertEquals(QaScenario.SCHEMA, restored.schema);
		assertEquals(source.id, restored.id);
		assertEquals(source.seed, restored.seed);
		assertEquals(source.actions.size(), restored.actions.size());
	}

	@Test
	public void seedAndFuzzBuildAreDeterministic() {
		RuleBuildFuzzer fuzzer = new RuleBuildFuzzer();
		long seed = RuleBuildFuzzer.buildSeed(12345L, 42);
		String first = RuleBuild.from(fuzzer.generateClassBuild(seed, 42)).fingerprint();
		String second = RuleBuild.from(fuzzer.generateClassBuild(seed, 42)).fingerprint();
		assertEquals(first, second);
		assertNotEquals(first, RuleBuild.from(fuzzer.generateClassBuild(seed + 1, 42)).fingerprint());
	}

	@Test
	public void fuzzerOnlyEmitsSyntacticallyCompatibleBuilds() {
		RuleBuildFuzzer fuzzer = new RuleBuildFuzzer();
		for (int i = 0; i < 1000; i++) {
			long seed = RuleBuildFuzzer.buildSeed(12345L, i);
			ClassBuild build = fuzzer.generateClassBuild(seed, i);
			assertTrue("incompatible build at index=" + i + " seed=" + seed, build.valid());
		}
	}

	@Test
	public void fuzzerExercisesEveryUnifiedCostModel() {
		RuleBuildFuzzer fuzzer = new RuleBuildFuzzer();
		EnumSet<RuleCost.Type> seen = EnumSet.noneOf(RuleCost.Type.class);
		for (int i = 0; i < 1000; i++) {
			ClassBuild build = fuzzer.generateClassBuild(RuleBuildFuzzer.buildSeed(12345L, i), i);
			for (SkillSpec skill : build.skills) seen.add(skill.cost.type);
		}
		assertEquals(EnumSet.allOf(RuleCost.Type.class), seen);
	}

	@Test
	public void actionExecutionAndSnapshotUseRealMomentumRuntime() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.momentumLoop());
		assertFalse(result.runtimeFailure);
		assertTrue(result.expectationMet);
		assertEquals(4, result.turn);
		assertEquals(30001L, result.seed);
		assertEquals("momentum_loop", result.buildId);
		assertNotNull(result.buildConfig);
		assertEquals("momentum", result.finalSnapshot.hero.resourceType);
		assertEquals(0, result.finalSnapshot.hero.resource);
		assertEquals(25, result.finalSnapshot.hero.pos);
	}

	@Test
	public void noWeaponWithoutResolutionIsBroken() {
		QaScenario scenario = RegressionScenarios.noWeaponNoVictory();
		BuildAnalysis result = new RuleBuildAnalyzer().analyze(RuleBuild.from(scenario.hero.build));
		assertEquals(BuildAnalysis.Classification.BROKEN, result.classification);
		assertTrue(result.has("NO_VICTORY_PATH"));
	}

	@Test
	public void resourceAndDeadConditionDependenciesAreReported() {
		QaScenario resourceScenario = RegressionScenarios.resourceWithoutSource();
		BuildAnalysis resource = new RuleBuildAnalyzer().analyze(
				RuleBuild.from(resourceScenario.hero.classBuild));
		assertTrue(resource.has("RESOURCE_WITHOUT_SOURCE"));
		BuildAnalysis poison = new RuleBuildAnalyzer().analyze(
				RuleBuild.from(RegressionScenarios.poisonWithoutSource().hero.build));
		assertEquals(BuildAnalysis.Classification.RISKY, poison.classification);
		assertTrue(poison.has("CONDITION_SOURCE_MISSING"));
	}

	@Test
	public void duplicateAndNoOpRulesAreDetected() {
		RuleDefinition duplicateA = simpleShield();
		RuleDefinition duplicateB = simpleShield();
		RuleBuild duplicate = RuleBuild.of("duplicate", ResourceEngine.MANA,
				ClassLaw.KILL_ACCELERATES_RULES, Restriction.NONE, Arrays.asList(duplicateA, duplicateB));
		assertTrue(new RuleBuildAnalyzer().analyze(duplicate).has("DUPLICATE_RULE"));

		RuleDefinition noOp = RuleDefinition.create(RuleEvent.ACTIVE, ResourceEngine.MANA, RuleEffect.Type.PUSH);
		noOp.target = new RuleTarget(RuleTarget.Type.SELF);
		RuleBuild invalid = RuleBuild.of("noop", ResourceEngine.MANA,
				ClassLaw.KILL_ACCELERATES_RULES, Restriction.NONE, Arrays.asList(noOp));
		assertTrue(new RuleBuildAnalyzer().analyze(invalid).has("NO_OP_RULE"));
	}

	@Test
	public void unreachableResourceConditionIsBroken() {
		RuleDefinition rule = simpleShield();
		rule.setConditions(new RuleCondition(RuleCondition.Type.RESOURCE_AT_LEAST, 99));
		RuleBuild build = RuleBuild.of("unreachable", ResourceEngine.MANA,
				ClassLaw.KILL_ACCELERATES_RULES, Restriction.NONE, Arrays.asList(rule));
		BuildAnalysis result = new RuleBuildAnalyzer().analyze(build);
		assertEquals(BuildAnalysis.Classification.BROKEN, result.classification);
		assertTrue(result.has("UNREACHABLE_RULE"));
	}

	@Test
	public void realPoisonAndBurningFeedAffliction() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.affliction());
		assertFalse(result.runtimeFailure);
		assertTrue(result.findings.contains("AFFLICTION_FLOW_OK"));
		assertTrue(result.finalSnapshot.hero.buffs.contains("Poison"));
		assertTrue(result.finalSnapshot.hero.buffs.contains("Burning"));
	}

	@Test
	public void realCombatKillsRatAndDispatchesOnKill() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.realCombat());
		assertFalse(result.runtimeFailure);
		assertTrue(result.findings.contains("REAL_COMBAT_OK"));
		assertTrue(result.finalSnapshot.mobs.isEmpty());
	}

	@Test
	public void saveLoadPreservesRuleRuntimeState() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.saveLoad());
		assertFalse(result.runtimeFailure);
		assertTrue(result.findings.contains("SAVE_LOAD_OK"));
		assertEquals(1, result.finalSnapshot.hero.activeRules.get(0).triggerCount);
		assertEquals(3f, result.finalSnapshot.hero.activeRules.get(0).cooldown, 0.001f);
	}

	@Test
	public void waterFocusWaitLoopFailsWithUnboundedShield() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.waterFocusWaitLoop());
		assertFalse(result.runtimeFailure);
		assertEquals(BuildAnalysis.Classification.BROKEN.name(), result.classification);
		assertTrue(result.findings.contains("UNBOUNDED_POWER_LOOP"));
		assertTrue(result.finalSnapshot.hero.shield >= 500);
	}

	@Test
	public void semanticTagsAreQueryableOnEffectsAndRules() {
		RuleEffect push = new RuleEffect(RuleEffect.Type.PUSH, 2);
		RuleEffect pull = new RuleEffect(RuleEffect.Type.PULL, 2);
		RuleEffect fire = new RuleEffect(RuleEffect.Type.FIRE, 2);
		RuleEffect teleport = new RuleEffect(RuleEffect.Type.TELEPORT, 2);
		assertTrue(push.hasTag(RuleSemanticTag.FORCED_MOVEMENT));
		assertTrue(pull.hasTag(RuleSemanticTag.FORCED_MOVEMENT));
		assertFalse(fire.hasTag(RuleSemanticTag.FORCED_MOVEMENT));
		assertTrue(teleport.hasTag(RuleSemanticTag.MOVEMENT));
		assertTrue(teleport.hasTag(RuleSemanticTag.TRANSLOCATION));
		RuleDefinition rule = RuleDefinition.create(RuleEvent.ACTIVE, ResourceEngine.MANA, RuleEffect.Type.PUSH);
		assertTrue(rule.hasTag(RuleSemanticTag.FORCED_MOVEMENT));
	}

	@Test
	public void hitPoisonStatusResourceChainUsesRealRuntime() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.eventChain());
		assertFalse(result.runtimeFailure);
		assertTrue(result.findings.contains("EVENT_CHAIN_OK"));
		assertTrue(result.traceText.contains("CHAIN"));
		assertTrue(result.traceText.contains("original=ON_HIT"));
	}

	@Test
	public void forcedMovementBridgeAndCycleGuardUseProvenance() {
		RuleRuntime savedRuntime = new RuleRuntime(RegressionScenarios.momentumLoop().hero.build);
		savedRuntime.addBridge(new RuleEventBridge(RuleSemanticTag.FORCED_MOVEMENT, RuleEvent.ON_MOVE));
		Bundle runtimeBundle = new Bundle();
		savedRuntime.storeInBundle(runtimeBundle);
		RuleRuntime restoredRuntime = new RuleRuntime();
		restoredRuntime.restoreFromBundle(runtimeBundle);
		assertEquals(1, restoredRuntime.bridges().size());
		assertEquals(RuleSemanticTag.FORCED_MOVEMENT, restoredRuntime.bridges().get(0).sourceTag);
		assertEquals(RuleEvent.ON_MOVE, restoredRuntime.bridges().get(0).targetEvent);

		ScenarioResult bridge = new HeadlessGameplayHarness().run(RegressionScenarios.bridgeMove());
		assertFalse(bridge.runtimeFailure);
		assertTrue(bridge.findings.contains("BRIDGE_MOVE_OK"));
		assertTrue(bridge.traceText.contains("original=ACTIVE"));

		ScenarioResult guarded = new HeadlessGameplayHarness().run(RegressionScenarios.bridgeRecursionGuard());
		assertFalse(guarded.runtimeFailure);
		assertTrue(guarded.findings.contains("BRIDGE_GUARD_OK"));
		assertTrue(guarded.traceText.contains("BRIDGE_GUARD"));
	}

	@Test
	public void markCanStackCheckConsumeRemoveAndExpire() {
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero source = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero target = new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
		RuleMark mark = RuleMark.apply(target, RuleMark.Type.HUNTED, source, 1, 5);
		RuleMark.apply(target, RuleMark.Type.HUNTED, source, 2, 5);
		assertEquals(3, mark.stacks());
		assertEquals(source.id(), mark.sourceId());
		assertEquals(target.id(), mark.ownerId());
		Bundle saved = new Bundle();
		mark.storeInBundle(saved);
		RuleMark restored = new RuleMark();
		restored.restoreFromBundle(saved);
		assertEquals(RuleMark.Type.HUNTED, restored.mark());
		assertEquals(3, restored.stacks());
		assertEquals(source.id(), restored.sourceId());
		assertEquals(target.id(), restored.ownerId());
		assertEquals(mark.remainingTurns(), restored.remainingTurns());
		assertTrue(new RuleMarkCondition(RuleMark.Type.HUNTED, 3)
				.passes(null, new com.shatteredpixel.shatteredpixeldungeon.rules.RuleContext(RuleEvent.ON_HIT, source), target));
		assertTrue(RuleMark.consume(target, RuleMark.Type.HUNTED, 2));
		assertEquals(1, mark.stacks());
		assertTrue(RuleMark.remove(target, RuleMark.Type.HUNTED));
		assertNull(RuleMark.get(target, RuleMark.Type.HUNTED));

		ScenarioResult chain = new HeadlessGameplayHarness().run(RegressionScenarios.markChain());
		assertFalse(chain.runtimeFailure);
		assertTrue(chain.findings.contains("MARK_CHAIN_OK"));
		ScenarioResult expired = new HeadlessGameplayHarness().run(RegressionScenarios.markExpire());
		assertFalse(expired.runtimeFailure);
		assertTrue(expired.findings.contains("MARK_EXPIRE_OK"));
	}

	@Test
	public void delayedPayloadFiresOnThirdFollowingTurn() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.delayThreeTurns());
		assertFalse(result.runtimeFailure);
		assertTrue(result.findings.contains("DELAY_THREE_OK"));
		assertTrue(result.traceText.contains("T=4 DELAY fire"));
	}

	@Test
	public void delayedPayloadSurvivesBundleReload() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.delaySaveLoad());
		assertFalse(result.runtimeFailure);
		assertTrue(result.findings.contains("DELAY_SAVE_LOAD_OK"));
		assertTrue(result.traceText.contains("SAVE_RELOAD matched=true"));
	}

	@Test
	public void coreVocabularyConfigSurvivesBundleReload() {
		CustomClassConfig original = RegressionScenarios.accumulationEcho().hero.build;
		Bundle saved = new Bundle();
		original.storeInBundle(saved);
		CustomClassConfig restored = new CustomClassConfig();
		restored.restoreFromBundle(saved);
		assertTrue(LawTraitRegistry.hasTrait(restored.toClassBuild(), CoreRuleVocabulary.ACCUMULATION));
		assertTrue(LawTraitRegistry.hasTrait(restored.toClassBuild(), CoreRuleVocabulary.ECHO));
		assertEquals(original.toClassBuild().usedBudget(), restored.toClassBuild().usedBudget());
		assertTrue(restored.valid());
	}

	@Test
	public void everyCoreVocabularyHasAProductionHeadlessRegression() {
		List<QaScenario> scenarios = Arrays.asList(
				RegressionScenarios.accumulationEcho(),
				RegressionScenarios.overflowOverdraw(),
				RegressionScenarios.compensation(),
				RegressionScenarios.phaseBlood(),
				RegressionScenarios.huntMark(),
				RegressionScenarios.huntPropagation(),
				RegressionScenarios.inertiaMomentum(),
				RegressionScenarios.translocationBridge(),
				RegressionScenarios.translocationBacklash(),
				RegressionScenarios.shieldBacklash());
		for (QaScenario scenario : scenarios) {
			ScenarioResult result = new HeadlessGameplayHarness().run(scenario);
			assertFalse(scenario.id + ": " + result.failure, result.runtimeFailure);
			assertTrue(scenario.id + ": " + result.findings, result.expectationMet);
		}
	}

	@Test
	public void slotFreeClassBuildsRunThroughTheRealHeadlessHarness() {
		List<QaScenario> scenarios = Arrays.asList(
				RegressionScenarios.classZeroResource(), RegressionScenarios.classMultipleResource(),
				RegressionScenarios.classMultipleActive(), RegressionScenarios.classReactionOnly(),
				RegressionScenarios.classMultipleLawsTraits(), RegressionScenarios.classMovement());
		for (QaScenario scenario : scenarios) {
			ScenarioResult result = new HeadlessGameplayHarness().run(scenario);
			assertFalse(scenario.id + ": " + result.failure, result.runtimeFailure);
			assertTrue(scenario.id + ": " + result.findings, result.expectationMet);
		}
	}

	private static RuleDefinition simpleShield() {
		RuleDefinition rule = RuleDefinition.create(RuleEvent.ON_WAIT, ResourceEngine.MANA, RuleEffect.Type.SHIELD);
		rule.cost = new RuleCost(RuleCost.Type.NONE, 0);
		rule.target = new RuleTarget(RuleTarget.Type.SELF);
		rule.effect = new RuleEffect(RuleEffect.Type.SHIELD, 2);
		rule.modifier = new RuleModifier();
		return rule;
	}
}
