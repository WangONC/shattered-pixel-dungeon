package com.shatteredpixel.shatteredpixeldungeon.qa;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** JSON-friendly records emitted by classArchetypeStressQa. */
public final class ArchetypeStressReport {
	private ArchetypeStressReport() {}

	public static class Summary {
		public String schema="class-archetype-stress-summary-1";
		public int archetypes, primaryArchetypes, scenarios, seedsPerScenario, totalRuns, victories, survivalFailures, runtimeFailures;
		public int minimumBudget, maximumBudget;
		public double budgetSpreadPercent;
		public int primaryMinimumBudget,primaryMaximumBudget;
		public double primaryBudgetSpreadPercent;
		public long elapsedMillis;
		public String conclusion;
		public final ArrayList<BuildSummary> builds=new ArrayList<>();
		public final ArrayList<String> findings=new ArrayList<>();
	}

	public static class RunResult {
		public String schema="class-archetype-stress-run-1";
		public String buildId,scenarioId,integrityClassification,failure;
		public long seed;
		public int usedBudget,maxBudget,turns,turnsToVictory;
		public boolean victory,survived,runtimeFailure;
		public BehaviorMetrics metrics=new BehaviorMetrics();
	}

	public static class BehaviorMetrics {
		public int turnsSurvived,playerDirectDamage,statusDamage,entityDamage,carrierDamage;
		public int damageReceived,damageMitigated,barrierConsumed,temporaryHpConsumed,healing;
		public int forcedMovementCells,enemiesControlled,controlTurns,skillUsageCount,normalAttackCount;
		public int moveCount,waitCount,actionCostTurns,resourceGenerated,resourceSpent,resourceOverflowWaste;
		public int marksApplied,marksConsumed,entitiesCreated,entityLifetimeTurns,terrainCellsAltered;
		public int modeSwitches,attachmentTriggers,failedSkillAttempts;
		public int hpCostPaid;
		public int executeLegalTurns,executeUsedTurns,executeMissedOpportunities;
		public int targetKilledBeforeExecute,executeOverkillValue,marksReadyButExecuteUnavailable;
		public int switchActionCost,redundantModeSwitches,immediateSwitchBacks;
		public final LinkedHashMap<String,Integer> skillUsage=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> skillFailures=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> resourceGeneratedByPool=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> resourceSpentByPool=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> componentSignals=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> costPayments=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> cooldownBlockedOpportunities=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> cooldownLegalNotSelected=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> cooldownReuseIntervalTotal=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> cooldownReuseSamples=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> modeUptime=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> skillsUsedByMode=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> damageByMode=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> mitigationByMode=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> movementByMode=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> componentDamage=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> componentMitigation=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> componentControl=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> componentResource=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> componentEntity=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> componentTerrain=new LinkedHashMap<>();
	}

	public static class BuildSummary {
		public String buildId,displayName,playstyle,integrityClassification;
		public int usedBudget,maxBudget,runs,victories,survivalFailures,runtimeFailures;
		public double victoryRate,meanTurnsToVictory,medianTurnsToVictory,minTurnsToVictory,maxTurnsToVictory;
		public BehaviorMetrics mean=new BehaviorMetrics();
		public final ArrayList<String> signature=new ArrayList<>();
		public final LinkedHashMap<String,Double> scenarioVictoryRate=new LinkedHashMap<>();
	}

	/** Aggregate for one build/scenario pair across all fixed seeds. */
	public static class ScenarioAggregate {
		public String schema="class-archetype-scenario-aggregate-1";
		public String buildId,scenarioId;
		public int seeds,victories,survivalFailures,runtimeFailures;
		public double victoryRate,meanTurnsToVictory,medianTurnsToVictory,minTurnsToVictory,maxTurnsToVictory;
		public BehaviorMetrics mean=new BehaviorMetrics();
	}

	public static class DominanceFinding {
		public String schema="class-archetype-dominance-1";
		public String stronger,weaker,classification,reason;
		public int comparableScenarioWins,comparableScenarioLosses;
		public double strongerVictoryRate,weakerVictoryRate,strongerMeanTurns,weakerMeanTurns;
		public double strongerSingleTarget,weakerSingleTarget,strongerCrowd,weakerCrowd;
		public double strongerDamageReceived,weakerDamageReceived,strongerControl,weakerControl;
		public double strongerResourceSustainability,weakerResourceSustainability;
		public double strongerActionEfficiency,weakerActionEfficiency;
	}

	public static class ComponentUsage {
		public String schema="class-archetype-component-usage-1";
		public String buildId;
		public final LinkedHashMap<String,Integer> skillUses=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> resourceGenerated=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> resourceSpent=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> componentSignals=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> costPayments=new LinkedHashMap<>();
		public final ArrayList<String> deadOrLowValueComponents=new ArrayList<>();
		public final LinkedHashMap<String,String> classifications=new LinkedHashMap<>();
	}

	public static class ConstraintResult {
		public String schema="class-archetype-constraint-1";
		public String buildId,skillId,constraint;
		public int nominalValue,effectiveValue,successfulUses,blockedAttempts;
		public int desiredButCooldownBlocked,legalButNotSelected,cooldownUptime;
		public double actualReuseInterval;
		public boolean automaticallySatisfied;
	}

	public static class SpecialtyReport {
		public String schema="class-archetype-specialty-1";
		public String buildId;
		public final ArrayList<String> bestScenarios=new ArrayList<>();
		public final ArrayList<String> worstScenarios=new ArrayList<>();
		public final ArrayList<String> relativeStrengths=new ArrayList<>();
		public final ArrayList<String> relativeWeaknesses=new ArrayList<>();
		public final ArrayList<String> uniqueBehaviorSignals=new ArrayList<>();
	}

	public static class ComponentValueAttribution {
		public String schema="class-archetype-component-value-1";
		public String buildId;
		public final LinkedHashMap<String,Integer> damage=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> mitigation=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> control=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> resource=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> entity=new LinkedHashMap<>();
		public final LinkedHashMap<String,Integer> terrain=new LinkedHashMap<>();
	}

	public static class MissingCapability {
		public String schema="class-archetype-missing-capability-1";
		public String capability,status,evidence,reusableUses,implementation;
	}
}
