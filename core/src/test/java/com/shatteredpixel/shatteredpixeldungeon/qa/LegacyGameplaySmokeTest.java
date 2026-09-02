package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMode;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.legacy.v5.LegacyGameplayBoundary;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

/** P00 regression smoke. Passing these cases is explicitly not evidence of v6 completion. */
@Category(LegacySmoke.class)
public class LegacyGameplaySmokeTest {
	private static HeadlessApplication app;

	@BeforeClass public static void startHeadless() {
		Game.version = "3.3.8-INDEV-p00-legacy-smoke";
		Game.versionCode = 896;
		app = new HeadlessApplication(new ApplicationAdapter() {}, new HeadlessApplicationConfiguration());
	}

	@AfterClass public static void stopHeadless() { if (app != null) app.exit(); }

	@Test public void resourceLegacySmoke() {
		ClassBuild build = legacyBuild();
		RuleRuntime runtime = new RuleRuntime(build);
		assertEquals(3, runtime.resourceValue("legacy_mana", ResourceEngine.MANUAL));
		assertEquals(7, runtime.resourceMax("legacy_mana", ResourceEngine.MANUAL));
	}

	@Test public void markLegacySmoke() {
		Hero source = new Hero();
		Hero owner = new Hero();
		RuleMark mark = RuleMark.apply(owner, RuleMark.Type.HUNTED, source, 2, 4);
		assertNotNull(mark);
		assertEquals(RuleMark.Type.HUNTED, mark.mark());
		assertEquals(2, mark.stacks());
		mark.detach();
	}

	@Test public void modeLegacySmoke() {
		RuleMode mode = new RuleMode();
		mode.setPersistent("legacy-mode");
		Bundle stored = new Bundle();
		mode.storeInBundle(stored);
		RuleMode restored = new RuleMode();
		restored.restoreFromBundle(stored);
		assertEquals("legacy-mode", restored.modeId());
		assertTrue(restored.persistent());
	}

	@Test public void entityLegacySmoke() {
		RuleOwnedEntity source = new RuleOwnedEntity().configure(
				RuleOwnedEntity.Kind.DEVICE, null, 5, 6, 2, 1, null);
		Bundle stored = new Bundle();
		source.storeInBundle(stored);
		RuleOwnedEntity restored = new RuleOwnedEntity();
		restored.restoreFromBundle(stored);
		assertEquals(RuleOwnedEntity.Kind.DEVICE, restored.kind());
		assertEquals(5, restored.lifetime());
	}

	@Test public void delayLegacySmoke() {
		ScenarioResult result = new HeadlessGameplayHarness().run(RegressionScenarios.delayThreeTurns());
		assertFalse(result.failure, result.runtimeFailure);
		assertTrue(result.findings.toString(), result.findings.contains("DELAY_THREE_OK"));
		assertFalse(result.v6CompletionEligible);
	}

	@Test public void saveLoadLegacySmoke() {
		ClassBuild source = legacyBuild();
		Bundle stored = new Bundle();
		source.storeInBundle(stored);
		ClassBuild restored = new ClassBuild();
		restored.restoreFromBundle(stored);
		assertEquals(RuleBuild.from(source).fingerprint(), RuleBuild.from(restored).fingerprint());
	}

	@Test public void headlessStartupLegacySmoke() {
		assertNotNull(Gdx.app);
		assertEquals(LegacyGameplayBoundary.EVIDENCE_CLASSIFICATION,
				new ScenarioResult().evidenceClassification);
		assertFalse(LegacyGameplayBoundary.V6_COMPLETION_ELIGIBLE);
	}

	private static ClassBuild legacyBuild() {
		ClassBuild build = new ClassBuild();
		build.name = "p00-legacy-smoke";
		ResourceSpec resource = new ResourceSpec();
		resource.id = "legacy_mana";
		resource.name = "Legacy Mana";
		resource.engine = ResourceEngine.MANUAL;
		resource.minimum = 0;
		resource.capacity = 7;
		resource.initialValue = 3;
		resource.current = 3;
		build.resources.add(resource);
		return build;
	}
}
