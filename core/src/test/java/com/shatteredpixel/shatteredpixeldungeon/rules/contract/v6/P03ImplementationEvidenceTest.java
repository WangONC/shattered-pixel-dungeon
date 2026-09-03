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
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/** QA owns implementation evidence and actively runs every referenced test method. */
public class P03ImplementationEvidenceTest {
	@Test public void everyPlayerExposedVariantHasCompleteRunnableEvidence(){
		List<String> keys=new ArrayList<>();for(VariantDescriptor descriptor:GameplayVariantCatalog.all()){if(descriptor.state()==ImplementationState.IMPLEMENTED){assertTrue("implemented variant hidden from evidence set: "+descriptor.qualifiedKey(),descriptor.playerExposed());assertFalse(descriptor.priceKey().isEmpty());keys.add(descriptor.qualifiedKey());}else assertFalse("unimplemented variant exposed: "+descriptor.qualifiedKey(),descriptor.playerExposed());}
		List<P03ImplementationEvidence.Row> rows=P03ImplementationEvidence.rows(keys);assertEquals(keys.size(),rows.size());Set<String> covered=new HashSet<>();Set<String> executed=new HashSet<>();
		for(P03ImplementationEvidence.Row row:rows){assertEquals(ImplementationState.IMPLEMENTED,row.state);assertTrue("duplicate evidence row "+row.variantKey,covered.add(row.variantKey));assertEquals(10,row.testIds().size());for(String id:row.testIds())if(executed.add(id))verifyAndRun(id);}
		assertEquals(new HashSet<>(keys),covered);
	}
	@Test public void deletedOrMisspelledEvidenceIdFailsTheGate(){try{verifyAndRun("P03R1CompilePlanTest#compiledSkillIsIndependentImmutableGrap_typo");fail("misspelled evidence was accepted");}catch(AssertionError expected){assertTrue(expected.getMessage().contains("missing evidence method"));}}
	private static void verifyAndRun(String id){
		int split=id.indexOf('#');assertTrue("invalid evidence id "+id,split>0&&split<id.length()-1);String className=P03ImplementationEvidenceTest.class.getPackage().getName()+"."+id.substring(0,split);String methodName=id.substring(split+1);
		try{Class<?> type=Class.forName(className);Method method=type.getDeclaredMethod(methodName);assertNotNull("evidence method lacks @Test: "+id,method.getAnnotation(Test.class));Result result=new JUnitCore().run(Request.method(type,methodName));assertEquals("evidence did not run exactly once: "+id,1,result.getRunCount());assertTrue("evidence failed: "+id+" "+result.getFailures(),result.wasSuccessful());}
		catch(ClassNotFoundException error){throw new AssertionError("missing evidence class: "+id,error);}catch(NoSuchMethodException error){throw new AssertionError("missing evidence method: "+id,error);}
	}
}
