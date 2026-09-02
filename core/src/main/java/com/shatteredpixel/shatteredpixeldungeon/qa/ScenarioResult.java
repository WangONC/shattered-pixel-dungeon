package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;

import java.util.ArrayList;

public class ScenarioResult {
	public String evidenceClassification = LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION;
	public boolean v6CompletionEligible = LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE;
	public String id;
	public long seed;
	public String buildId;
	public String buildConfig;
	public String classification;
	public int turn;
	public long elapsedNanos;
	public boolean runtimeFailure;
	public String failure;
	public final ArrayList<String> findings = new ArrayList<>();
	public final ArrayList<String> expectedFindings = new ArrayList<>();
	public boolean expectationMet;
	public QaSnapshot initialSnapshot;
	public QaSnapshot finalSnapshot;
	public String tracePath;
	public transient String traceText;
}
