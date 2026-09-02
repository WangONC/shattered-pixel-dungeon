package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectVocabularyRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerFacingClassBuildFormatter;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Bidirectional Runtime ↔ player Builder exposure contract. */
public final class BuilderVocabularyExposureAudit {
	public static final class Result {
		public String schema = "builder-vocabulary-exposure-1";
		public String evidenceClassification = LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION;
		public boolean v6CompletionEligible = LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE;
		public int runtimeSupported;
		public int builderExposed;
		public int supportedButNotExposed;
		public int builderExposedButRuntimeUnsupported;
		public int missingEnName, missingEnSummary, missingZhName, missingZhSummary;
		public int missingBudget, factoryFailures, roundtripFailures, formatterFailures, compatibilityFailures;
		public final LinkedHashMap<String, Integer> variantsPerFamily = new LinkedHashMap<>();
		public final ArrayList<String> failures = new ArrayList<>();
		public boolean passed;
	}

	private BuilderVocabularyExposureAudit() {}

	public static Result run() {
		Result result = new Result();
		for (EffectSpec.Operation operation : EffectSpec.Operation.values()) {
			if (operation == EffectSpec.Operation.LEGACY) continue;
			EffectFamily family = EffectSpec.familyFor(operation);
			if (family != null && new EffectSpec(family, operation, 2).implemented()) {
				result.runtimeSupported++;
				if (!EffectVocabularyRegistry.exposed(operation)) {
					result.supportedButNotExposed++;
					result.failures.add("SUPPORTED_BUT_NOT_EXPOSED:" + operation);
				}
			}
		}
		for (EffectVocabularyRegistry.FamilyEntry family : EffectVocabularyRegistry.families()) {
			result.variantsPerFamily.put(family.family.name(),
					EffectVocabularyRegistry.variants(family.family).size());
		}
		for (EffectVocabularyRegistry.EffectEntry entry : EffectVocabularyRegistry.exposed()) {
			result.builderExposed++;
			if (!entry.supported()) {
				result.builderExposedButRuntimeUnsupported++;
				result.failures.add("BUILDER_EXPOSED_BUT_RUNTIME_UNSUPPORTED:" + entry.operation);
				continue;
			}
			EffectSpec spec;
			try { spec = entry.create(); }
			catch (Throwable error) {
				result.factoryFailures++; result.failures.add("FACTORY:" + entry.operation); continue;
			}
			if (spec.powerCost() <= 0 || spec.powerCost() >= 999) {
				result.missingBudget++; result.failures.add("BUDGET:" + entry.operation);
			}
			Bundle bundle = new Bundle();
			spec.storeInBundle(bundle);
			EffectSpec restored = new EffectSpec();
			restored.restoreFromBundle(bundle);
			if (restored.operation != spec.operation || restored.family != spec.family) {
				result.roundtripFailures++; result.failures.add("ROUNDTRIP:" + entry.operation);
			}
			TargetingSpec compatible = compatibleTarget(spec);
			if (compatible == null) {
				result.compatibilityFailures++; result.failures.add("COMPATIBILITY:" + entry.operation);
			} else {
				SkillSpec skill = new SkillSpec();
				skill.primary = spec.copy();
				skill.targeting = compatible;
				skill.delivery = compatible.selector == TargetingSpec.Selector.SELF ? SkillDelivery.SELF
						: compatible.selector == TargetingSpec.Selector.SELECTED_CELL
						? SkillDelivery.GROUND_PLACEMENT : SkillDelivery.DIRECT_TARGET;
				String formatted = PlayerFacingClassBuildFormatter.skillDetail(skill, new ClassBuild());
				if (missing(formatted)) {
					result.formatterFailures++; result.failures.add("FORMATTER:" + entry.operation);
				}
			}
		}
		checkLanguage(result, Languages.ENGLISH, false);
		checkLanguage(result, Languages.CHI_SMPL, true);
		Messages.setup(Languages.ENGLISH);
		result.passed = result.supportedButNotExposed == 0
				&& result.builderExposedButRuntimeUnsupported == 0
				&& result.missingEnName == 0 && result.missingEnSummary == 0
				&& result.missingZhName == 0 && result.missingZhSummary == 0
				&& result.missingBudget == 0 && result.factoryFailures == 0
				&& result.roundtripFailures == 0 && result.formatterFailures == 0
				&& result.compatibilityFailures == 0;
		return result;
	}

	private static void checkLanguage(Result result, Languages language, boolean chinese) {
		Messages.setup(language);
		for (EffectVocabularyRegistry.EffectEntry entry : EffectVocabularyRegistry.exposed()) {
			if (missing(entry.name())) {
				if (chinese) result.missingZhName++; else result.missingEnName++;
				result.failures.add((chinese ? "MISSING_ZH_NAME:" : "MISSING_EN_NAME:") + entry.operation);
			}
			if (missing(entry.summary()) || entry.summary().equals(entry.name())) {
				if (chinese) result.missingZhSummary++; else result.missingEnSummary++;
				result.failures.add((chinese ? "MISSING_ZH_SUMMARY:" : "MISSING_EN_SUMMARY:") + entry.operation);
			}
		}
	}

	private static boolean missing(String value) {
		return value == null || value.trim().isEmpty() || value.contains(Messages.NO_TEXT_FOUND);
	}

	private static TargetingSpec compatibleTarget(EffectSpec spec) {
		for (TargetingSpec.Selector selector : TargetingRegistry.SELECTORS) {
			for (TargetingSpec.Coverage coverage : TargetingRegistry.COVERAGES) {
				for (TargetingSpec.Filter filter : TargetingRegistry.FILTERS) {
					TargetingSpec target = new TargetingSpec();
					target.selector = selector; target.coverage = coverage; target.filter = filter;
					if (target.implemented(RuleEvent.ACTIVE) && spec.compatibleTargeting(target)) return target;
				}
			}
		}
		return null;
	}
}
