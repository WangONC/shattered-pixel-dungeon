package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RulePresentationTest {
	private static HeadlessApplication app;

	@BeforeClass
	public static void startHeadlessMessages() {
		app = new HeadlessApplication(new ApplicationAdapter() {}, new HeadlessApplicationConfiguration());
	}

	@AfterClass
	public static void stopHeadlessMessages() {
		if (app != null) app.exit();
	}

	@After
	public void restoreEnglish() {
		Messages.setup(Languages.ENGLISH);
	}

	private static RuleDefinition activePush() {
		RuleDefinition rule = RuleDefinition.create(RuleEvent.ACTIVE, ResourceEngine.MOMENTUM,
				RuleEffect.Type.PUSH);
		rule.target = new RuleTarget(RuleTarget.Type.SELECTED_TARGET);
		return rule;
	}

	private static CustomClassConfig waterBuild() {
		CustomClassConfig config = RuleTestBuilds.config(RuleTestBuilds.Preset.F_WATER_SHAPER);
		config.name = "Tidewright";
		return config;
	}

	@Test
	public void alwaysIsOmittedFromPlayerDescriptionsInEnglishAndChinese() {
		RuleDefinition rule = activePush();
		Messages.setup(Languages.ENGLISH);
		String english = RuleSemanticFormatter.techniqueDescription(rule, ResourceEngine.MOMENTUM);
		assertFalse(english, english.contains("Always"));
		assertFalse(english, english.contains("ALWAYS"));

		Messages.setup(Languages.CHI_SMPL);
		String chinese = RuleSemanticFormatter.techniqueDescription(rule, ResourceEngine.MOMENTUM);
		assertFalse(chinese, chinese.contains("始终"));
		assertFalse(chinese, chinese.contains("ALWAYS"));
	}

	@Test
	public void activePushProducesNaturalEnglishAndChinese() {
		RuleDefinition rule = activePush();
		Messages.setup(Languages.ENGLISH);
		String english = RuleSemanticFormatter.techniqueDescription(rule, ResourceEngine.MOMENTUM);
		assertTrue(english, english.contains("Active:"));
		assertTrue(english, english.contains("Spend 3 Momentum"));
		assertTrue(english, english.contains("push the selected target away"));

		Messages.setup(Languages.CHI_SMPL);
		String chinese = RuleSemanticFormatter.techniqueDescription(rule, ResourceEngine.MOMENTUM);
		assertTrue(chinese, chinese.contains("主动使用"));
		assertTrue(chinese, chinese.contains("3 点动量"));
		assertTrue(chinese, chinese.contains("击退选择的目标"));
	}

	@Test
	public void poisonedHitPullFusesTriggerAndCondition() {
		RuleDefinition rule = RuleDefinition.create(RuleEvent.ON_HIT, ResourceEngine.MANA,
				RuleEffect.Type.PULL);
		rule.target = new RuleTarget(RuleTarget.Type.HIT_TARGET);
		rule.setConditions(new RuleCondition(RuleCondition.Type.TARGET_HAS_POISON));

		Messages.setup(Languages.ENGLISH);
		String english = RuleSemanticFormatter.techniqueDescription(rule, ResourceEngine.MANA);
		assertTrue(english, english.contains("When you hit a poisoned target"));
		assertTrue(english, english.contains("pull the enemy you hit toward you"));
		assertEquals("Toxic Pull", RuleSemanticFormatter.techniqueName(rule));

		Messages.setup(Languages.CHI_SMPL);
		String chinese = RuleSemanticFormatter.techniqueDescription(rule, ResourceEngine.MANA);
		assertTrue(chinese, chinese.contains("命中中毒目标时"));
		assertTrue(chinese, chinese.contains("拉向自己"));
		assertEquals("毒性牵引", RuleSemanticFormatter.techniqueName(rule));
	}

	@Test
	public void multipleConditionsAndModifierRemainReadable() {
		RuleDefinition rule = RuleDefinition.create(RuleEvent.ON_DAMAGED, ResourceEngine.RAGE,
				RuleEffect.Type.FIRE);
		rule.target = new RuleTarget(RuleTarget.Type.ATTACKER);
		rule.setConditions(new RuleCondition(RuleCondition.Type.SELF_HP_BELOW, 30),
				new RuleCondition(RuleCondition.Type.ADJACENT_ENEMIES_AT_LEAST, 2));
		rule.modifier = new RuleModifier(RuleModifier.Type.AREA);

		Messages.setup(Languages.ENGLISH);
		String english = RuleSemanticFormatter.techniqueDescription(rule, ResourceEngine.RAGE);
		assertTrue(english, english.contains("30%"));
		assertTrue(english, english.contains("at least 2 enemies"));
		assertTrue(english, english.contains("affecting enemies"));
		assertFalse(english, english.contains("SELF_HP_BELOW"));
		assertFalse(english, english.contains("AREA"));

		Messages.setup(Languages.CHI_SMPL);
		String chinese = RuleSemanticFormatter.techniqueDescription(rule, ResourceEngine.RAGE);
		assertTrue(chinese, chinese.contains("30%"));
		assertTrue(chinese, chinese.contains("至少有 2 名敌人"));
		assertTrue(chinese, chinese.contains("影响附近"));
	}

	@Test
	public void resourceTooltipContainsOnlyItsResource() {
		CustomClassConfig config = waterBuild();
		RuleRuntime runtime = new RuleRuntime(config);
		Hero hero = new Hero();
		hero.HT = hero.HP = 20;

		Messages.setup(Languages.ENGLISH);
		String tooltip = RuleSemanticFormatter.resourceTooltip(runtime, hero);
		assertTrue(tooltip, tooltip.contains("Current Focus"));
		assertFalse(tooltip, tooltip.contains(config.name));
		assertFalse(tooltip, tooltip.contains(config.law.displayName()));
		assertFalse(tooltip, tooltip.contains("Rule 1"));
		assertFalse(tooltip, tooltip.contains("Runtime"));
	}

	@Test
	public void classBuildSheetContainsPlayerSectionsWithoutInternalIds() {
		CustomClassConfig config = waterBuild();
		Messages.setup(Languages.ENGLISH);
		String sheet = CustomClassSummaryFormatter.buildSheet(config);
		assertTrue(sheet, sheet.contains("Resources / Cost Models"));
		assertTrue(sheet, sheet.contains("Class Laws"));
		assertTrue(sheet, sheet.contains("Gameplay Traits"));
		assertTrue(sheet, sheet.contains("Skills"));
		assertTrue(sheet, sheet.contains("Class Budget"));
		assertTrue(sheet, sheet.contains("Create Water"));
		assertTrue(sheet, sheet.contains("Main effect"));
		assertTrue(sheet, sheet.contains("Skill affix"));
		assertFalse(sheet, sheet.contains("ON_ENTER_TILE"));
		assertFalse(sheet, sheet.contains("runtimeOrder"));
	}

	@Test
	public void builderStatusKeepsEverySelectedSectionVisibleWithoutDebugTerms() {
		CustomClassConfig config = waterBuild();
		config.vocabulary1 = CoreRuleVocabulary.TRANSLOCATION_BRIDGE;
		Messages.setup(Languages.ENGLISH);
		String status = CustomClassSummaryFormatter.builderStatus(config);
		assertTrue(status, status.contains(config.name));
		assertTrue(status, status.contains("Resources 1"));
		assertTrue(status, status.contains("Skills 2"));
		assertTrue(status, status.contains("Laws/Traits"));
		assertTrue(status, status.contains("Restrictions"));
		assertTrue(status, status.contains("Budget"));
		assertFalse(status, status.contains("RuleDefinition"));
		assertFalse(status, status.contains("runtimeOrder"));
		assertFalse(status, status.contains("TRANSLOCATION_BRIDGE"));
	}

	@Test
	public void compactPreviewIsShorterThanDetailsAndLocalized() {
		CustomClassConfig config = waterBuild();
		config.vocabulary1 = CoreRuleVocabulary.TRANSLOCATION_BRIDGE;
		Messages.setup(Languages.ENGLISH);
		String english = CustomClassSummaryFormatter.compactPreview(config);
		String details = CustomClassSummaryFormatter.buildSheet(config);
		assertTrue(english, english.contains("Resources"));
		assertTrue(english, english.contains("Class Budget"));
		assertTrue(english.length() < details.length());
		assertFalse(english, english.contains(config.resource.description()));

		Messages.setup(Languages.CHI_SMPL);
		String chinese = CustomClassSummaryFormatter.compactPreview(config);
		assertTrue(chinese, chinese.contains("资源"));
		assertTrue(chinese, chinese.contains("职业预算"));
		assertFalse(chinese, chinese.contains("RuleDefinition"));
		assertFalse(chinese, chinese.contains("TRANSLOCATION_BRIDGE"));
	}

	@Test
	public void heroSelectUsesBuilderNameOnlyBeforeConfiguration() {
		Messages.setup(Languages.ENGLISH);
		assertEquals("Create a Class", HeroSelectScene.customClassTitle(null));
		CustomClassConfig config = waterBuild();
		assertEquals("Tidewright", HeroSelectScene.customClassTitle(config));
		String desc = HeroSelectScene.customClassDescription(config);
		assertTrue(desc, desc.contains("Self-shaped class"));
		assertTrue(desc, desc.contains("terrain-aware play"));
	}

	@Test
	public void devRuntimeDumpRemainsAvailable() {
		Messages.setup(Languages.ENGLISH);
		RuleRuntime runtime = new RuleRuntime(waterBuild());
		String debug = runtime.stateDescription();
		assertTrue(debug, debug.contains("Rule 1"));
		assertTrue(debug, debug.contains("Runtime 1"));
		assertTrue(debug, debug.contains("If Always"));
	}
}
