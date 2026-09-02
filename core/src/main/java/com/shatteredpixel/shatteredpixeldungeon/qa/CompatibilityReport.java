package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;

import java.util.LinkedHashMap;

public class CompatibilityReport {
	public String evidenceClassification = LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION;
	public boolean v6CompletionEligible = LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE;

	public static class Cell {
		public int generated;
		public int valid;
		public int risky;
		public int broken;
		public int runtimeFailed;
	}

	public final LinkedHashMap<String, Cell> triggerTarget = new LinkedHashMap<>();
	public final LinkedHashMap<String, Cell> conditionEffect = new LinkedHashMap<>();
	public final LinkedHashMap<String, Cell> effectModifier = new LinkedHashMap<>();
	public final LinkedHashMap<String, Cell> deliveryEffect = new LinkedHashMap<>();
	public final LinkedHashMap<String, Cell> selectorCoverage = new LinkedHashMap<>();
	public final LinkedHashMap<String, Cell> coverageFilter = new LinkedHashMap<>();
	public final LinkedHashMap<String, Cell> resourceCost = new LinkedHashMap<>();
	public final LinkedHashMap<String, Cell> lawRestriction = new LinkedHashMap<>();
	public final LinkedHashMap<String, Cell> vocabularyPair = new LinkedHashMap<>();
}
