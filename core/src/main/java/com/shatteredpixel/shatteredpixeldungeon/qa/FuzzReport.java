package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class FuzzReport {
	public String evidenceClassification = LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION;
	public boolean v6CompletionEligible = LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE;
	public long seed;
	public int totalGenerated;
	public int syntacticValid;
	public int staticValid;
	public int risky;
	public int broken;
	public int headlessSampled;
	public int headlessRunnable;
	public int runtimeFailed;
	public int dynamicFailures;
	public double basicEffectiveCombinationDensity;
	public double validDensity;
	public double riskyDensity;
	public double brokenDensity;
	public double nonBrokenDensity;
	public double runtimeFailureRate;
	public double exploitRate;
	public long elapsedMillis;
	public long staticAnalysisNanos;
	public long headlessNanos;
	public double headlessTurnsPerSecond;
	public String metricNote = "Densities are reported separately; NON-BROKEN is not a claim of quality or fun.";
	public final LinkedHashMap<String, Integer> findingCounts = new LinkedHashMap<>();
	public final LinkedHashMap<String, LinkedHashMap<String, Usage>> vocabularyUsage = new LinkedHashMap<>();
	public final ArrayList<String> potentialDeadVocabulary = new ArrayList<>();
	public final ArrayList<Failure> failures = new ArrayList<>();

	public static class Usage {
		public int total;
		public int valid;
		public int risky;
		public int broken;
		public int runtimeFailed;
	}

	public static class Failure {
		public String id;
		public String classification;
		public String reason;
		public long seed;
		public String buildConfig;
		public String scenario;
		public int turn;
		public String tracePath;
		public transient String traceText;
	}
}
