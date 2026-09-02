package com.shatteredpixel.shatteredpixeldungeon.qa;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BuilderVocabularyExposureTest {
	@Test
	public void runtimeAndBuilderExposureAreBidirectionalAndLocalized() {
		BuilderVocabularyExposureAudit.Result result = BuilderVocabularyExposureAudit.run();
		assertEquals(0, result.supportedButNotExposed);
		assertEquals(0, result.builderExposedButRuntimeUnsupported);
		assertEquals(0, result.missingEnName + result.missingEnSummary);
		assertEquals(0, result.missingZhName + result.missingZhSummary);
		assertTrue(result.failures.toString(), result.passed);
	}
}
