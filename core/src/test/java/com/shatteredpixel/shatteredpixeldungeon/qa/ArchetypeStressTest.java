package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.watabou.noosa.Game;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/** Regression coverage for the shared archetype fixtures and their non-scripted gameplay policy. */
public class ArchetypeStressTest {
	private static HeadlessApplication app;

	@BeforeClass public static void startHeadless() {
		Game.version = "3.3.8-INDEV-archetype-stress-test";
		Game.versionCode = 896;
		app = new HeadlessApplication(new ApplicationAdapter() {}, new HeadlessApplicationConfiguration());
	}

	@AfterClass public static void stopHeadless() { if (app != null) app.exit(); }

	@Test public void primaryBuildsAreSlotFreeValidAndBudgetComparable() {
		int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
		for (ArchetypeReferenceBuilds.Id id : ArchetypeReferenceBuilds.primaryIds()) {
			ClassBuild build=ArchetypeReferenceBuilds.build(id);
			assertTrue(id+" is structurally invalid",build.structurallyValid());
			assertTrue(id+" exceeds its class budget",build.budgetValid());
			min=Math.min(min,build.usedBudget());max=Math.max(max,build.usedBudget());
		}
		assertTrue("primary budget floor drifted too low: "+min,min>=32);assertEquals(35,max);
		assertTrue((max-min)*100.0/min <= 10.0);
	}

	@Test public void optionalBuildsRemainOrdinaryValidClassBuilds() {
		for (ArchetypeReferenceBuilds.Id id : ArchetypeReferenceBuilds.allIds()) {
			ClassBuild build=ArchetypeReferenceBuilds.build(id);
			assertTrue(id+" invalid",build.valid());
			assertFalse(id+" leaked an archetype domain into the model",build.name.contains("_domain"));
		}
	}

	@Test public void finiteManualPoolHasNoHiddenStartingOrRegenerationSource() {
		assertEquals(6,ResourceEngine.MANUAL.max);
		assertEquals(0,ResourceEngine.MANUAL.initial);
		ArchetypeStressReport.RunResult run=new ArchetypeStressHarness().run(
				ArchetypeReferenceBuilds.Id.AMMO_GUNNER,
				ArchetypeStressHarness.ScenarioId.RESOURCE_STARVATION,424242L);
		assertFalse(run.failure,run.runtimeFailure);
		assertTrue("reload never generated ammunition",run.metrics.resourceGeneratedByPool.get("ammo")>0);
		assertTrue("shots never spent ammunition",run.metrics.resourceSpentByPool.get("ammo")>0);
		assertTrue("reload did not commit actor time",run.metrics.actionCostTurns>0);
	}

	@Test public void savedLimitedUseConstraintActuallyCapsExecution() {
		ArchetypeStressReport.RunResult run=new ArchetypeStressHarness().run(
				ArchetypeReferenceBuilds.Id.RESOURCELESS_COOLDOWN,
				ArchetypeStressHarness.ScenarioId.SUSTAINED_ENCOUNTER,9123L);
		assertFalse(run.failure,run.runtimeFailure);
		Integer uses=run.metrics.skillUsage.get("limited_barrier");
		assertNotNull(uses);
		assertTrue("limited barrier exceeded three uses: "+uses,uses<=3);
	}

	@Test public void sameSeedProducesTheSameBehaviorProfile() {
		ArchetypeStressReport.RunResult first=new ArchetypeStressHarness().run(
				ArchetypeReferenceBuilds.Id.MODE_SHIFTER,
				ArchetypeStressHarness.ScenarioId.MIXED_THREAT,77123L);
		ArchetypeStressReport.RunResult second=new ArchetypeStressHarness().run(
				ArchetypeReferenceBuilds.Id.MODE_SHIFTER,
				ArchetypeStressHarness.ScenarioId.MIXED_THREAT,77123L);
		assertFalse(first.failure,first.runtimeFailure);assertFalse(second.failure,second.runtimeFailure);
		assertEquals(first.victory,second.victory);assertEquals(first.turns,second.turns);
		assertEquals(first.metrics.playerDirectDamage,second.metrics.playerDirectDamage);
		assertEquals(first.metrics.damageReceived,second.metrics.damageReceived);
		assertEquals(first.metrics.modeSwitches,second.metrics.modeSwitches);
	}

	@Test public void sharedScenarioMatrixCapturesEntityAndConstraintBehavior() {
		ArchetypeStressAnalyzer.Result result=ArchetypeStressAnalyzer.run(1,424242L);
		assertEquals(15*12,result.runs.size());
		assertEquals(15*12,result.scenarioAggregates.size());
		assertEquals(0,result.summary.runtimeFailures);
		assertTrue("unexpected foundation conclusion "+result.summary.conclusion,
				"C".equals(result.summary.conclusion)||"D".equals(result.summary.conclusion));
		ArchetypeStressReport.BuildSummary summon=summary(result,"OWNED_SUMMONER");
		ArchetypeStressReport.BuildSummary engineer=summary(result,"DEVICE_ENGINEER");
		assertTrue("owned actors never dealt damage",summon.mean.entityDamage>0);
		assertTrue("device deployment produced no persistent value",engineer.mean.entitiesCreated>0||engineer.mean.carrierDamage>0);
		assertTrue(hasConstraint(result,"TARGET_MARKED"));
		assertTrue(hasConstraint(result,"SELF_IN_WATER"));
		assertTrue(hasConstraint(result,"SELF_LOW_HP"));
		assertTrue(hasConstraint(result,"LIMITED_USE"));
		assertTrue(hasConstraint(result,"COST_COOLDOWN"));
		assertFalse(result.specialties.isEmpty());
		assertFalse(result.componentValues.isEmpty());
	}

	@Test public void gapPassSignalsAreBehavioralNotArchetypeFlags() {
		ArchetypeStressAnalyzer.Result result=ArchetypeStressAnalyzer.run(1,424242L);
		ArchetypeStressReport.BuildSummary terrain=summary(result,"TERRAIN_CONTROLLER");
		assertTrue("terrain-rich did not outperform equal-threat terrain-poor",
				terrain.scenarioVictoryRate.get("TERRAIN_RICH")>terrain.scenarioVictoryRate.get("TERRAIN_POOR"));
		ArchetypeStressReport.BuildSummary assassin=summary(result,"MARK_ASSASSIN");
		assertTrue("finisher loop never reached a legal execute",assassin.mean.executeLegalTurns>0);
		assertTrue("legal execute was not used",assassin.mean.executeUsedTurns>0);
		ArchetypeStressReport.BuildSummary modes=summary(result,"MODE_SHIFTER");
		assertTrue(modes.mean.damageByMode.get("offense")>0);
		assertTrue(modes.mean.mitigationByMode.get("defense")>0);
		ArchetypeStressReport.ConstraintResult burst=constraint(result,"AMMO_GUNNER","controlled_burst","COOLDOWN");
		assertTrue("cooldown was nominal text, not a blocked opportunity",burst.desiredButCooldownBlocked>0);
		assertTrue("effective cooldown rebate was not discounted",burst.effectiveValue<burst.nominalValue);
	}

	private static ArchetypeStressReport.BuildSummary summary(ArchetypeStressAnalyzer.Result result,String id){
		for(ArchetypeStressReport.BuildSummary value:result.summary.builds)if(value.buildId.equals(id))return value;
		throw new AssertionError("missing summary "+id);
	}

	private static boolean hasConstraint(ArchetypeStressAnalyzer.Result result,String type){
		for(ArchetypeStressReport.ConstraintResult value:result.constraints)if(value.constraint.equals(type))return true;
		return false;
	}

	private static ArchetypeStressReport.ConstraintResult constraint(ArchetypeStressAnalyzer.Result result,String build,String skill,String type){
		for(ArchetypeStressReport.ConstraintResult value:result.constraints)if(value.buildId.equals(build)&&value.skillId.equals(skill)&&value.constraint.equals(type))return value;
		throw new AssertionError("missing constraint "+build+":"+skill+":"+type);
	}
}
