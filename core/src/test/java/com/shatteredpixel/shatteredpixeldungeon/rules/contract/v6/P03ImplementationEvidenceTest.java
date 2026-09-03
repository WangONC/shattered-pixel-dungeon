package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.GameplayVariantCatalog;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.VariantDescriptor;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/** QA owns explicit per-variant evidence and actively runs every referenced test method. */
public class P03ImplementationEvidenceTest {
	@Test public void everyQualifiedVariantHasAnExplicitCompleteRunnableEvidenceRow(){
		List<VariantDescriptor> catalog=GameplayVariantCatalog.all();
		Map<String,P03ImplementationEvidence.Row> registry=P03ImplementationEvidence.registry();
		assertEvidenceCoverage(catalog,registry,P03ImplementationEvidence.requiredVariants());
		Set<String> executed=new HashSet<>();
		for(P03ImplementationEvidence.Row row:registry.values()){
			assertEquals(ImplementationState.IMPLEMENTED,row.state);
			assertEquals(10,row.testIds().size());
			for(String id:row.testIds())if(executed.add(id))verifyAndRun(id);
		}
	}

	@Test public void deletingDirectDamageExplicitRowFailsTheGate(){
		Map<String,P03ImplementationEvidence.Row> missing=new LinkedHashMap<>(P03ImplementationEvidence.registry());
		assertNotNull(missing.remove("EFFECT.DIRECT_DAMAGE"));
		assertCoverageFails(GameplayVariantCatalog.all(),missing,P03ImplementationEvidence.requiredVariants(),"missing evidence row EFFECT.DIRECT_DAMAGE");
	}

	@Test public void laterPhaseImplementedVariantsDoNotContaminateTheP03Snapshot(){
		List<VariantDescriptor> expanded=new ArrayList<>(GameplayVariantCatalog.all());
		expanded.add(new VariantDescriptor("EFFECT","FUTURE_IMPLEMENTED",ImplementationState.IMPLEMENTED,true,"skill.effect.future"));
		assertEvidenceCoverage(expanded,P03ImplementationEvidence.registry(),P03ImplementationEvidence.requiredVariants());
	}

	@Test public void unknownEvidenceVariantAndMisspelledTestIdFailTheGate(){
		Map<String,P03ImplementationEvidence.Row> unknown=new LinkedHashMap<>(P03ImplementationEvidence.registry());
		P03ImplementationEvidence.Row direct=unknown.get("EFFECT.DIRECT_DAMAGE");
		unknown.put("EFFECT.DOES_NOT_EXIST",new P03ImplementationEvidence.Row("EFFECT.DOES_NOT_EXIST",
				direct.schemaTestId,direct.builderPathTestId,direct.dependencyTestId,direct.formatterTestId,
				direct.budgetTestId,direct.saveLoadTestId,direct.compilerTestId,direct.executorTestId,
				direct.runtimeBehaviorTestId,direct.adversarialTestId));
		assertCoverageFails(GameplayVariantCatalog.all(),unknown,P03ImplementationEvidence.requiredVariants(),"unknown evidence variant EFFECT.DOES_NOT_EXIST");
		try{verifyAndRun("P03R2CompileAdmissionTest#directDamageOnlyBuildIsExecutabl_typo");fail("misspelled evidence was accepted");}
		catch(AssertionError expected){assertTrue(expected.getMessage().contains("missing evidence method"));}
	}

	private static void assertEvidenceCoverage(List<VariantDescriptor> catalog,
			Map<String,P03ImplementationEvidence.Row> registry,Set<String> required){
		Set<String> known=new HashSet<>();
		for(VariantDescriptor descriptor:catalog){
			assertTrue("duplicate catalog variant "+descriptor.qualifiedKey(),known.add(descriptor.qualifiedKey()));
			if(descriptor.state()==ImplementationState.IMPLEMENTED||descriptor.playerExposed()){
				assertEquals("player exposure and implementation must agree: "+descriptor.qualifiedKey(),
						ImplementationState.IMPLEMENTED,descriptor.state());
				assertTrue("implemented variant must be player exposed: "+descriptor.qualifiedKey(),descriptor.playerExposed());
				assertFalse("implemented variant requires price: "+descriptor.qualifiedKey(),descriptor.priceKey().isEmpty());
			}else assertFalse("unimplemented variant exposed: "+descriptor.qualifiedKey(),descriptor.playerExposed());
		}
		for(String key:registry.keySet())assertTrue("unknown evidence variant "+key,known.contains(key));
		for(String key:required)assertTrue("missing evidence row "+key,registry.containsKey(key));
		assertEquals("evidence registry contains non-qualified rows",required,registry.keySet());
	}

	private static void assertCoverageFails(List<VariantDescriptor> catalog,
			Map<String,P03ImplementationEvidence.Row> registry,Set<String> required,String message){
		try{assertEvidenceCoverage(catalog,registry,required);fail("adversarial registry was accepted");}
		catch(AssertionError expected){assertTrue(expected.getMessage(),expected.getMessage().contains(message));}
	}

	private static void verifyAndRun(String id){
		int split=id.indexOf("#");assertTrue("invalid evidence id "+id,split>0&&split<id.length()-1);
		String className=P03ImplementationEvidenceTest.class.getPackage().getName()+"."+id.substring(0,split);
		String methodName=id.substring(split+1);
		try{Class<?> type=Class.forName(className);Method method=type.getDeclaredMethod(methodName);
			assertNotNull("evidence method lacks @Test: "+id,method.getAnnotation(Test.class));
			Result result=new JUnitCore().run(Request.method(type,methodName));
			assertEquals("evidence did not run exactly once: "+id,1,result.getRunCount());
			assertTrue("evidence failed: "+id+" "+result.getFailures(),result.wasSuccessful());}
		catch(ClassNotFoundException error){throw new AssertionError("missing evidence class: "+id,error);}
		catch(NoSuchMethodException error){throw new AssertionError("missing evidence method: "+id,error);}
	}
}
