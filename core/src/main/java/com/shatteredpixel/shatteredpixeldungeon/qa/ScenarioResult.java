package com.shatteredpixel.shatteredpixeldungeon.qa;

import java.util.ArrayList;

public class ScenarioResult {
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
