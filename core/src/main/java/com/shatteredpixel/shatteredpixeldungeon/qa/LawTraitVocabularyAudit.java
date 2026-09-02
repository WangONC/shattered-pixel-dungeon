package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rules.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Bidirectional content contract for formal Laws and parameterized Traits. */
public final class LawTraitVocabularyAudit {
	public static final class Entry {
		public String id,kind,name,summary,detail,referenceBuild;
		public int budget;
		public boolean builderExposed,runtimeImplemented,runtimeTriggered,roundtrip,formatted,compatible;
	}
	public static final class Result {
		public String schema="law-trait-vocabulary-1";
		public String evidenceClassification=LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION;
		public boolean v6CompletionEligible=LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE;
		public int laws,traits,missingEnName,missingEnSummary,missingEnDetail;
		public int missingZhName,missingZhSummary,missingZhDetail,missingBudget;
		public int runtimeMissing,roundtripFailures,formatterFailures,compatibilityFailures,neverTriggered;
		public final ArrayList<Entry> entries=new ArrayList<>();
		public final ArrayList<String> failures=new ArrayList<>();
		public boolean passed;
	}
	private LawTraitVocabularyAudit() {}

	public static Result run(){
		Result result=new Result();
		for(ClassLaw law:LawTraitRegistry.laws()){result.laws++;Entry row=new Entry();row.id=law.name();row.kind="LAW";row.budget=law.capacityCost;row.builderExposed=LawTraitRegistry.exposed(law);row.runtimeImplemented=runtimeLaw(law);QaScenario scenario=LawTraitReferenceBuilds.scenarioForLaw(law);row.referenceBuild=scenario.id;row.runtimeTriggered=triggered(scenario,LawTraitReferenceBuilds.lawEvidence(law));
			ClassBuild build=LawTraitReferenceBuilds.forLaw(law);row.compatible=LawTraitRegistry.lawIssue(build,law)==null;row.roundtrip=roundtrip(build);row.formatted=!missing(law.detail())&&!missing(PlayerFacingClassBuildFormatter.buildSheet(build));result.entries.add(row);check(result,row,law.displayName(),law.summary(),law.detail(),false);}
		for(CoreRuleVocabulary type:LawTraitRegistry.traits()){result.traits++;Entry row=new Entry();row.id=type.name();row.kind="TRAIT";row.budget=type.capacityCost;row.builderExposed=LawTraitRegistry.exposed(type);row.runtimeImplemented=runtimeTrait(type);QaScenario scenario=LawTraitReferenceBuilds.scenarioForTrait(type);row.referenceBuild=scenario.id;row.runtimeTriggered=triggered(scenario,LawTraitReferenceBuilds.traitEvidence(type));
			ClassBuild build=LawTraitReferenceBuilds.forTrait(type);TraitSpec spec=find(build,type);row.compatible=spec!=null&&LawTraitRegistry.traitIssue(build,spec)==null;row.roundtrip=roundtrip(build);row.formatted=spec!=null&&!missing(spec.detail(build))&&!missing(PlayerFacingClassBuildFormatter.buildSheet(build));result.entries.add(row);check(result,row,spec==null?"":spec.displayName(build),spec==null?"":spec.shortSummary(build),spec==null?"":spec.detail(build),false);}
		checkLanguage(result,Languages.CHI_SMPL,true);Messages.setup(Languages.ENGLISH);
		result.passed=result.failures.isEmpty();return result;
	}

	private static void checkLanguage(Result result,Languages language,boolean chinese){Messages.setup(language);for(Entry row:result.entries){ClassBuild build="LAW".equals(row.kind)?LawTraitReferenceBuilds.forLaw(ClassLaw.valueOf(row.id)):LawTraitReferenceBuilds.forTrait(CoreRuleVocabulary.valueOf(row.id));String name,summary,detail;if("LAW".equals(row.kind)){ClassLaw value=ClassLaw.valueOf(row.id);name=value.displayName();summary=value.summary();detail=value.detail();}else{TraitSpec value=find(build,CoreRuleVocabulary.valueOf(row.id));name=value.displayName(build);summary=value.shortSummary(build);detail=value.detail(build);}if(missing(name)){if(chinese)result.missingZhName++;else result.missingEnName++;result.failures.add("MISSING_NAME:"+language+":"+row.id);}if(missing(summary)||summary.equals(name)){if(chinese)result.missingZhSummary++;else result.missingEnSummary++;result.failures.add("MISSING_SUMMARY:"+language+":"+row.id);}if(missing(detail)||detail.equals(summary)){if(chinese)result.missingZhDetail++;else result.missingEnDetail++;result.failures.add("MISSING_DETAIL:"+language+":"+row.id);}}}
	private static void check(Result result,Entry row,String name,String summary,String detail,boolean chinese){row.name=name;row.summary=summary;row.detail=detail;if(row.budget<=0){result.missingBudget++;result.failures.add("MISSING_BUDGET:"+row.id);}if(!row.builderExposed)result.failures.add("NOT_EXPOSED:"+row.id);if(!row.runtimeImplemented){result.runtimeMissing++;result.failures.add("RUNTIME_MISSING:"+row.id);}if(!row.roundtrip){result.roundtripFailures++;result.failures.add("ROUNDTRIP:"+row.id);}if(!row.formatted){result.formatterFailures++;result.failures.add("FORMATTER:"+row.id);}if(!row.compatible){result.compatibilityFailures++;result.failures.add("REFERENCE_INCOMPATIBLE:"+row.id);}if(!row.runtimeTriggered){result.neverTriggered++;result.failures.add("NEVER_TRIGGERED:"+row.id);}if(missing(name)){result.missingEnName++;result.failures.add("MISSING_EN_NAME:"+row.id);}if(missing(summary)||summary.equals(name)){result.missingEnSummary++;result.failures.add("MISSING_EN_SUMMARY:"+row.id);}if(missing(detail)||detail.equals(summary)){result.missingEnDetail++;result.failures.add("MISSING_EN_DETAIL:"+row.id);}}
	private static boolean runtimeLaw(ClassLaw law){return LawTraitRegistry.exposed(law);}
	private static boolean runtimeTrait(CoreRuleVocabulary trait){return LawTraitRegistry.exposed(trait);}
	private static TraitSpec find(ClassBuild build,CoreRuleVocabulary type){for(TraitSpec value:build.traits)if(value.type==type)return value;return null;}
	private static boolean roundtrip(ClassBuild build){try{Bundle bundle=new Bundle();build.storeInBundle(bundle);ClassBuild restored=new ClassBuild();restored.restoreFromBundle(bundle);return RuleBuild.from(build).fingerprint().equals(RuleBuild.from(restored).fingerprint());}catch(Throwable ignored){return false;}}
	private static boolean triggered(QaScenario scenario,String evidence){try{ScenarioResult result=new HeadlessGameplayHarness().run(scenario);return !result.runtimeFailure&&evidence!=null&&!evidence.isEmpty()&&result.traceText.contains(evidence);}catch(Throwable ignored){return false;}}
	private static boolean missing(String value){return value==null||value.trim().isEmpty()||value.contains(Messages.NO_TEXT_FOUND);}
}
