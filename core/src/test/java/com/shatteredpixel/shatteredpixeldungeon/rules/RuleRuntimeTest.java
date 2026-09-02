package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Slime;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class RuleRuntimeTest {
	@After
	public void clearLevel() {
		Dungeon.level = null;
	}

	private static Hero heroWith(CustomClassConfig config) {
		Hero hero = new Hero();
		hero.HT = hero.HP = 20;
		hero.setRuleRuntime(new RuleRuntime(config));
		return hero;
	}

	private static CustomClassConfig base(ResourceEngine engine) {
		CustomClassConfig config = new CustomClassConfig();
		config.name = "test";
		config.resource = engine;
		config.law = ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD;
		return config;
	}

	private static RuleDefinition recordingRule(RuleEvent event) {
		RuleDefinition rule = RuleDefinition.create(event, ResourceEngine.MANA, RuleEffect.Type.SHIELD);
		rule.cost = new RuleCost(RuleCost.Type.NONE, 0);
		rule.target = new RuleTarget(RuleTarget.Type.SELF);
		rule.effect = new RecordingEffect();
		return rule;
	}

	@Test
	public void requiredV01CompositionsRemainRealModules() {
		RuleDefinition ragePush = RuleDefinition.ragePushOnDamaged();
		assertEquals(RuleEvent.ON_DAMAGED, ragePush.trigger.event);
		assertEquals(RuleCost.Type.RESOURCE, ragePush.cost.type);
		assertEquals(2, ragePush.cost.amount);
		assertEquals(RuleEffect.Type.PUSH, ragePush.effect.type);
		assertEquals(RuleTarget.Type.ATTACKER, ragePush.target.type);

		RuleDefinition bloodFire = RuleDefinition.bloodFireActive();
		assertEquals(RuleEvent.ACTIVE, bloodFire.trigger.event);
		assertEquals(RuleCost.Type.HP, bloodFire.cost.type);
		assertEquals(RuleEffect.Type.FIRE, bloodFire.effect.type);
		assertEquals(RuleTarget.Type.SELECTED_CELL, bloodFire.target.type);
	}

	@Test
	public void allSixAcceptanceBuildsAreValidAndDistinct() {
		ArrayList<ResourceEngine> engines = new ArrayList<>();
		for (RuleTestBuilds.Preset preset : RuleTestBuilds.Preset.values()) {
			CustomClassConfig config = RuleTestBuilds.config(preset);
			assertTrue(preset.name() + " capacity", config.valid());
			assertEquals(2, config.buildRules().size());
			assertTrue(config.usedCapacity() <= config.maxCapacity());
			engines.add(config.resource);
		}
		assertTrue(engines.containsAll(Arrays.asList(ResourceEngine.MOMENTUM, ResourceEngine.FOCUS,
				ResourceEngine.AFFLICTION, ResourceEngine.BLOOD, ResourceEngine.MANA)));
	}

	@Test
	public void momentumRewardsChainsAndDecaysWhenStopping() {
		Hero hero = heroWith(base(ResourceEngine.MOMENTUM));
		RuleRuntime runtime = hero.ruleRuntime();
		for (int i = 0; i < 3; i++) runtime.dispatch(new RuleContext(RuleEvent.ON_MOVE, hero));
		assertEquals(3, runtime.resource());
		assertEquals(3, runtime.consecutiveMoves());
		runtime.dispatch(new RuleContext(RuleEvent.ON_TURN_START, hero));
		assertEquals(3, runtime.resource());
		runtime.dispatch(new RuleContext(RuleEvent.ON_TURN_START, hero));
		assertEquals(1, runtime.resource());
		assertEquals(0, runtime.consecutiveMoves());
		runtime.dispatch(new RuleContext(RuleEvent.ON_WAIT, hero));
		assertEquals(0, runtime.resource());
	}

	@Test
	public void focusBuildsFromWaitingAndDamageBreaksIt() {
		Hero hero = heroWith(base(ResourceEngine.FOCUS));
		RuleRuntime runtime = hero.ruleRuntime();
		runtime.dispatch(new RuleContext(RuleEvent.ON_WAIT, hero));
		assertEquals(2, runtime.resource());
		runtime.dispatch(new RuleContext(RuleEvent.ON_TURN_START, hero));
		assertEquals(3, runtime.resource());
		RuleContext damage = new RuleContext(RuleEvent.ON_DAMAGED, hero);
		damage.amount = 2;
		runtime.dispatch(damage);
		assertEquals(0, runtime.resource());
		assertEquals(0, runtime.focusSafeTurns());
	}

	@Test
	public void afflictionUsesGenericNegativeBuffClassification() {
		Hero hero = heroWith(base(ResourceEngine.AFFLICTION));
		RuleRuntime runtime = hero.ruleRuntime();
		RuleContext poison = new RuleContext(RuleEvent.ON_STATUS_APPLIED, hero);
		poison.status = new Poison();
		runtime.dispatch(poison);
		RuleContext burning = new RuleContext(RuleEvent.ON_STATUS_APPLIED, hero);
		burning.status = new Burning();
		runtime.dispatch(burning);
		assertEquals(4, runtime.resource());
	}

	@Test
	public void conditionsHaveTrueFalseAndOrderedAndSemantics() {
		Hero hero = heroWith(base(ResourceEngine.MANA));
		hero.HP = 8;
		hero.ruleRuntime().setResourceForDebug(hero, 5);
		RuleDefinition rule = recordingRule(RuleEvent.ON_ATTACK);
		rule.setConditions(new RuleCondition(RuleCondition.Type.SELF_HP_BELOW, 50),
				new RuleCondition(RuleCondition.Type.RESOURCE_AT_LEAST, 5));
		hero.ruleRuntime().addRule(rule, true);
		assertTrue(hero.ruleRuntime().dispatch(new RuleContext(RuleEvent.ON_ATTACK, hero)));
		assertEquals(1, rule.triggerCount());
		hero.HP = 15;
		assertFalse(hero.ruleRuntime().dispatch(new RuleContext(RuleEvent.ON_ATTACK, hero)));
		assertEquals(1, rule.triggerCount());

		Bundle saved = new Bundle();
		rule.storeInBundle(saved);
		RuleDefinition restored = new RuleDefinition();
		restored.restoreFromBundle(saved);
		assertEquals(2, restored.conditions.size());
		assertEquals(50, restored.conditions.get(0).parameter);
		assertEquals(5, restored.conditions.get(1).parameter);
	}

	@Test
	public void targetSelectorsUseEventContextWithoutEffectSpecificClasses() {
		Hero hero = new Hero();
		Hero attacker = new Hero();
		Hero target = new Hero();
		RuleContext context = new RuleContext(RuleEvent.ON_HIT, hero);
		context.source = attacker;
		context.target = target;
		assertEquals(hero, new RuleTarget(RuleTarget.Type.SELF).resolveChar(context));
		assertEquals(attacker, new RuleTarget(RuleTarget.Type.ATTACKER).resolveChar(context));
		assertEquals(target, new RuleTarget(RuleTarget.Type.HIT_TARGET).resolveChar(context));
		assertEquals(hero.pos, new RuleTarget(RuleTarget.Type.CURRENT_TILE).resolveCell(context, hero));
	}

	@Test
	public void terrainAndEnemySelectorsUseRealLevelState() {
		Dungeon.level = new TestLevel();
		Hero hero = heroWith(base(ResourceEngine.MANA));
		hero.pos = 12;
		Dungeon.level.water[12] = true;
		Rat near = new Rat();
		near.pos = 13;
		Slime far = new Slime();
		far.pos = 24;
		Dungeon.level.mobs.add(near);
		Dungeon.level.mobs.add(far);
		RuleContext context = new RuleContext(RuleEvent.ON_ENTER_TILE, hero);
		assertTrue(new RuleCondition(RuleCondition.Type.SELF_IN_WATER).passes(hero.ruleRuntime(), context, hero));
		assertEquals(near, new RuleTarget(RuleTarget.Type.NEAREST_ENEMY).resolveChar(context));
		assertEquals(1, new RuleTarget(RuleTarget.Type.ALL_ADJACENT_ENEMIES).resolveChars(context).size());
		assertTrue(new RuleCondition(RuleCondition.Type.ADJACENT_ENEMIES_AT_LEAST, 1)
				.passes(hero.ruleRuntime(), context, hero));
	}

	@Test
	public void waterAffinityAndAreaModifierUseMapPosition() {
		Dungeon.level = new TestLevel();
		CustomClassConfig config = base(ResourceEngine.MANA);
		config.law = ClassLaw.WATER_AFFINITY;
		Hero hero = heroWith(config);
		hero.pos = 12;
		Dungeon.level.water[12] = true;
		hero.ruleRuntime().changeResource(hero, 1, false);
		assertEquals(7, hero.ruleRuntime().resource());

		CountingEffect.calls = 0;
		RuleDefinition area = recordingRule(RuleEvent.ON_ATTACK);
		area.target = new RuleTarget(RuleTarget.Type.CURRENT_TILE);
		area.effect = new CountingEffect();
		area.modifier = new RuleModifier(RuleModifier.Type.AREA);
		hero.ruleRuntime().addRule(area, true);
		hero.ruleRuntime().dispatch(new RuleContext(RuleEvent.ON_ATTACK, hero));
		assertEquals(9, CountingEffect.calls);
	}

	@Test
	public void repeatModifierActuallyRepeatsAndChargesOnce() {
		Hero hero = heroWith(base(ResourceEngine.MANA));
		CountingEffect.calls = 0;
		RuleDefinition rule = recordingRule(RuleEvent.ON_ATTACK);
		rule.effect = new CountingEffect();
		rule.modifier = new RuleModifier(RuleModifier.Type.REPEAT);
		rule.modifier.magnitude = 3;
		hero.ruleRuntime().addRule(rule, true);
		assertTrue(hero.ruleRuntime().dispatch(new RuleContext(RuleEvent.ON_ATTACK, hero)));
		assertEquals(3, CountingEffect.calls);
		assertEquals(1, rule.triggerCount());
		assertFalse(new RuleModifier(RuleModifier.Type.REPEAT).compatible(RuleEffect.Type.TELEPORT));
		assertTrue(new RuleModifier(RuleModifier.Type.EXTEND_DURATION).compatible(RuleEffect.Type.POISON));
	}

	@Test
	public void multipleRulesExecuteByPriorityThenCreationOrder() {
		Hero hero = heroWith(base(ResourceEngine.MANA));
		OrderingEffect.order.clear();
		RuleDefinition first = recordingRule(RuleEvent.ON_ATTACK);
		first.effect = new OrderingEffect("first");
		RuleDefinition high = recordingRule(RuleEvent.ON_ATTACK);
		high.priority = 5;
		high.effect = new OrderingEffect("high");
		RuleDefinition second = recordingRule(RuleEvent.ON_ATTACK);
		second.effect = new OrderingEffect("second");
		hero.ruleRuntime().addRule(first, true);
		hero.ruleRuntime().addRule(high, true);
		hero.ruleRuntime().addRule(second, true);
		hero.ruleRuntime().dispatch(new RuleContext(RuleEvent.ON_ATTACK, hero));
		assertEquals(Arrays.asList("high", "first", "second"), OrderingEffect.order);
	}

	@Test
	public void recursiveRuleGuardStopsSelfReentryButAllowsDispatchToReturn() {
		Hero hero = heroWith(base(ResourceEngine.MANA));
		RecursiveEffect.calls = 0;
		RuleDefinition rule = recordingRule(RuleEvent.ON_ATTACK);
		rule.effect = new RecursiveEffect();
		hero.ruleRuntime().addRule(rule, true);
		assertTrue(hero.ruleRuntime().dispatch(new RuleContext(RuleEvent.ON_ATTACK, hero)));
		assertEquals(1, RecursiveEffect.calls);
		assertEquals(0, hero.ruleRuntime().executionDepth());
	}

	@Test
	public void secondaryEffectSharesTheSameRecursionGuard() {
		Hero hero = heroWith(base(ResourceEngine.MANA));
		RecursiveEffect.calls = 0;
		RuleDefinition rule = recordingRule(RuleEvent.ON_ATTACK);
		rule.effect = new RecordingEffect();
		rule.secondaryEffect = new RecursiveEffect();
		hero.ruleRuntime().addRule(rule, true);
		assertTrue(hero.ruleRuntime().dispatch(new RuleContext(RuleEvent.ON_ATTACK, hero)));
		assertEquals(1, RecursiveEffect.calls);
		assertEquals(1, rule.triggerCount());
		assertEquals(0, hero.ruleRuntime().executionDepth());
	}

	@Test
	public void classLawStatusAbsorptionChangesResourceLoop() {
		CustomClassConfig config = base(ResourceEngine.MANA);
		config.law = ClassLaw.STATUS_ABSORPTION;
		Hero hero = heroWith(config);
		int before = hero.ruleRuntime().resource();
		RuleContext status = new RuleContext(RuleEvent.ON_STATUS_APPLIED, hero);
		status.status = new Poison();
		hero.ruleRuntime().dispatch(status);
		assertEquals(before + 1, hero.ruleRuntime().resource());
	}

	@Test
	public void healingAndOverflowLawsCreateRealBarrierState() {
		CustomClassConfig healing = base(ResourceEngine.MANA);
		healing.law = ClassLaw.HEALING_TO_SHIELD;
		Hero healer = heroWith(healing);
		healer.HP = 10;
		assertTrue(RuleHooks.applyHealing(healer, 6));
		assertEquals(10, healer.HP);
		assertEquals(6, healer.buff(Barrier.class).shielding());

		CustomClassConfig overflow = base(ResourceEngine.MANA);
		overflow.law = ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD;
		Hero overflowing = heroWith(overflow);
		overflowing.ruleRuntime().setResourceForDebug(overflowing, 9);
		overflowing.ruleRuntime().changeResource(overflowing, 4, false);
		assertEquals(10, overflowing.ruleRuntime().resource());
		assertEquals(3, overflowing.buff(Barrier.class).shielding());
	}

	@Test
	public void gameplayRestrictionsChangeWaitHealingWeaponAndActiveCosts() {
		CustomClassConfig wait = base(ResourceEngine.MANA);
		wait.restriction = Restriction.WAIT_CLEARS_RESOURCE;
		Hero waiter = heroWith(wait);
		waiter.ruleRuntime().setResourceForDebug(waiter, 9);
		waiter.ruleRuntime().dispatch(new RuleContext(RuleEvent.ON_WAIT, waiter));
		assertEquals(0, waiter.ruleRuntime().resource());

		CustomClassConfig healing = base(ResourceEngine.MANA);
		healing.restriction = Restriction.NO_TRADITIONAL_HEALING;
		Hero noHealing = heroWith(healing);
		assertEquals(0f, RuleHooks.healingPotionMultiplier(noHealing), 0.001f);

		CustomClassConfig weapons = base(ResourceEngine.MANA);
		weapons.restriction = Restriction.NO_ORDINARY_WEAPONS;
		assertTrue(RuleHooks.ordinaryWeaponsRestricted(heroWith(weapons)));

		CustomClassConfig active = base(ResourceEngine.MANA);
		active.restriction = Restriction.ACTIVE_COSTS_HP;
		Hero caster = heroWith(active);
		RuleDefinition activeRule = caster.ruleRuntime().rules().get(0);
		activeRule.cost = new RuleCost(RuleCost.Type.NONE, 0);
		activeRule.target = new RuleTarget(RuleTarget.Type.SELF);
		activeRule.effect = new RecordingEffect();
		activeRule.effectSpec = null;
		activeRule.targetingSpec = null;
		activeRule.delivery = null;
		assertTrue(caster.ruleRuntime().dispatch(new RuleContext(RuleEvent.ACTIVE, caster)));
		assertEquals(18, caster.HP);
	}

	@Test
	public void equivalentBlueprintsKeepIndependentRuntimeState() {
		Hero hero = heroWith(base(ResourceEngine.MANA));
		RuleDefinition one = recordingRule(RuleEvent.ON_ATTACK);
		RuleDefinition two = recordingRule(RuleEvent.ON_ATTACK);
		one.modifier.cooldown = 4;
		two.modifier.cooldown = 4;
		assertNotSame(one, two);
		assertTrue(one.execute(hero.ruleRuntime(), new RuleContext(RuleEvent.ON_ATTACK, hero)));
		assertEquals(4f, one.cooldownRemaining(), 0.001f);
		assertEquals(0f, two.cooldownRemaining(), 0.001f);
	}

	@Test
	public void multiRuleConfigurationAndStateRoundTripThroughBundle() {
		CustomClassConfig config = RuleTestBuilds.config(RuleTestBuilds.Preset.E_STATUS_COMBO);
		config.name = "Stored Combo";
		Hero hero = heroWith(config);
		RuleRuntime runtime = hero.ruleRuntime();
		runtime.setResourceForDebug(hero, 9);
		RuleDefinition extra = recordingRule(RuleEvent.ON_ATTACK);
		extra.modifier.cooldown = 4f;
		runtime.addRule(extra, true);
		assertTrue(runtime.dispatch(new RuleContext(RuleEvent.ON_ATTACK, hero)));

		Bundle bundle = new Bundle();
		runtime.storeInBundle(bundle);
		RuleRuntime restored = new RuleRuntime();
		restored.restoreFromBundle(bundle);

		assertEquals("Stored Combo", restored.customName());
		assertEquals(ResourceEngine.MANUAL, restored.engine());
		assertEquals("mana", restored.primaryResourceSpec().id);
		assertEquals(ClassLaw.KILL_ACCELERATES_RULES, restored.law());
		assertEquals(9, restored.resource());
		assertEquals(3, restored.rules().size());
		assertEquals(1, restored.rules().get(2).triggerCount());
		assertEquals(4f, restored.rules().get(2).cooldownRemaining(), 0.001f);
		assertTrue(restored.rules().get(0).runtimeOrder() < restored.rules().get(1).runtimeOrder());
	}

	@Test
	public void vanillaHeroHasNoRuleBehavior() {
		Hero vanilla = new Hero();
		assertFalse(RuleHooks.ordinaryWeaponsRestricted(vanilla));
		assertEquals(1f, RuleHooks.healingPotionMultiplier(vanilla), 0.001f);
		assertFalse(RuleHooks.triggerActive(vanilla, 0));
	}

	@Test
	public void activeSkillPaysResourceWhenItsEffectMisses() {
		Hero hero = heroWith(base(ResourceEngine.MANA));
		RuleRuntime runtime = hero.ruleRuntime();
		runtime.setResourceForDebug(hero, 6);
		RuleDefinition shot = RuleDefinition.create(RuleEvent.ACTIVE, ResourceEngine.MANA, RuleEffect.Type.FIRE);
		shot.id = "missed_shot";
		shot.cost = new RuleCost(RuleCost.Type.RESOURCE, 1);
		shot.effect = new FailingEffect();
		runtime.addRule(shot, true);
		RuleContext context = new RuleContext(RuleEvent.ACTIVE, hero);
		context.cell = 12;
		assertTrue("a legally fired ACTIVE Skill still consumes its action when the effect misses",
				runtime.dispatchActive(shot.id, context));
		assertEquals(5, runtime.resource());
		assertEquals(1, shot.triggerCount());
	}

	@Test
	public void invalidEffectAndEventTargetsAreRejected() {
		CustomClassConfig config = base(ResourceEngine.MANA);
		config.activeEffect = RuleEffect.Type.PUSH;
		config.activeTarget = RuleTarget.Type.SELF;
		assertFalse(config.valid());

		config.activeTarget = RuleTarget.Type.SELECTED_TARGET;
		config.reactionTrigger = RuleEvent.ON_WAIT;
		config.reactionTarget = RuleTarget.Type.ATTACKER;
		assertFalse(config.valid());

		config.reactionTarget = RuleTarget.Type.SELF;
		config.reactionEffect = RuleEffect.Type.SHIELD;
		assertTrue(config.valid());
	}

	@Test
	public void heroBundleAndSavePreviewKeepV02ClassAndState() {
		CustomClassConfig config = RuleTestBuilds.config(RuleTestBuilds.Preset.A_MOMENTUM_SKIRMISHER);
		config.name = "Bundle Walker";
		Hero original = new Hero();
		Talent.initClassTalents(original);
		original.setRuleRuntime(new RuleRuntime(config));
		original.ruleRuntime().setResourceForDebug(null, 9);

		Bundle bundle = new Bundle();
		original.storeInBundle(bundle);
		Hero restored = new Hero();
		restored.restoreFromBundle(bundle);

		assertEquals("Bundle Walker", restored.className());
		assertEquals(ResourceEngine.MANUAL, restored.ruleRuntime().engine());
		assertEquals("momentum", restored.ruleRuntime().primaryResourceSpec().id);
		assertEquals(9, restored.ruleRuntime().resource());
		assertEquals(2, restored.ruleRuntime().rules().size());

		GamesInProgress.Info preview = new GamesInProgress.Info();
		Hero.preview(preview, bundle);
		assertEquals("Bundle Walker", preview.customClassName);
	}

	public static class RecordingEffect extends RuleEffect {
		public RecordingEffect() {}
		@Override public boolean apply(RuleContext context, Char target, int cell, RuleModifier modifier) { return true; }
	}

	public static class FailingEffect extends RuleEffect {
		public FailingEffect() {}
		@Override public boolean apply(RuleContext context, Char target, int cell, RuleModifier modifier) { return false; }
	}

	public static class CountingEffect extends RuleEffect {
		static int calls;
		public CountingEffect() {}
		@Override public boolean apply(RuleContext context, Char target, int cell, RuleModifier modifier) {
			calls++;
			return true;
		}
	}

	public static class OrderingEffect extends RuleEffect {
		static final ArrayList<String> order = new ArrayList<>();
		private String label;
		public OrderingEffect() {}
		OrderingEffect(String label) { this.label = label; }
		@Override public boolean apply(RuleContext context, Char target, int cell, RuleModifier modifier) {
			order.add(label);
			return true;
		}
	}

	public static class RecursiveEffect extends RuleEffect {
		static int calls;
		public RecursiveEffect() {}
		@Override public boolean apply(RuleContext context, Char target, int cell, RuleModifier modifier) {
			calls++;
			context.hero.ruleRuntime().dispatch(new RuleContext(context.event, context.hero));
			return true;
		}
	}

	public static class TestLevel extends Level {
		public TestLevel() {
			mobs = new HashSet<>();
			setSize(5, 5);
			Arrays.fill(map, Terrain.EMPTY);
			Arrays.fill(passable, true);
			Arrays.fill(openSpace, true);
		}
		@Override protected boolean build() { return true; }
		@Override protected void createMobs() {}
		@Override protected void createItems() {}
	}
}
