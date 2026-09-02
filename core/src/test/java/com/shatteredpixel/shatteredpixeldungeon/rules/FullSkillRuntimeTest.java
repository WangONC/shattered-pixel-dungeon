package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleDamageRedirect;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMitigation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMode;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleOwnership;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleRelation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleTemporaryHP;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity;
import com.shatteredpixel.shatteredpixeldungeon.qa.FullSkillReferenceBuilds;
import com.shatteredpixel.shatteredpixeldungeon.qa.HeadlessGameplayHarness;
import com.shatteredpixel.shatteredpixeldungeon.qa.QaFixedLevel;
import com.shatteredpixel.shatteredpixeldungeon.qa.QaScenario;
import com.shatteredpixel.shatteredpixeldungeon.qa.ScenarioResult;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.Random;
import com.watabou.noosa.Game;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/** Behavior-level coverage for the structured Skill runtime; no renderer or fake combat model. */
public class FullSkillRuntimeTest {
	public static class BurningImmuneRat extends Rat {{ immunities.add(Burning.class); }}
	private static HeadlessApplication app;
	private QaFixedLevel level;
	private Hero hero;

	@BeforeClass public static void startHeadless() {
		Game.version = "3.3.8-INDEV-full-skill-test";
		Game.versionCode = 896;
		app = new HeadlessApplication(new ApplicationAdapter() {}, new HeadlessApplicationConfiguration());
	}

	@AfterClass public static void stopHeadless() { if (app != null) app.exit(); }

	@Before public void setup() {
		Actor.clear();
		Actor.resetNextID();
		level = new QaFixedLevel(7, 7);
		Dungeon.level = level;
		Dungeon.depth = 1;
		hero = new Hero();
		hero.HT = hero.HP = 100;
		hero.pos = 24;
		hero.damageInterrupt = false;
		ClassBuild build = new ClassBuild(); build.name = "runtime-test"; build.baseBudget = 100;
		build.gameplayComponents.add(new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.OWNERSHIP, "ownership"));
		ClassGameplayComponentSpec actors = new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.ENTITY_CAPACITY, "actor_capacity");
		actors.entityFilter = ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR; actors.capacity = 6;
		build.gameplayComponents.add(actors);
		ClassGameplayComponentSpec devices = new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.ENTITY_CAPACITY, "device_capacity");
		devices.entityFilter = ClassGameplayComponentSpec.EntityFilter.OWNED_DEVICE; devices.capacity = 6;
		build.gameplayComponents.add(devices);
		hero.setRuleRuntime(new RuleRuntime(build));
		Dungeon.hero = hero;
		level.occupyCell(hero);
		Actor.init();
	}

	@After public void teardown() {
		Actor.clear();
		Dungeon.hero = null;
		Dungeon.level = null;
	}

	private Mob mob(int cell, int hp) {
		Mob mob = cell % 2 == 0 ? new Rat() : new Snake();
		mob.pos = cell; mob.HT = mob.HP = hp; mob.EXP = 0; mob.maxLvl = -1;
		level.mobs.add(mob); Actor.add(mob);
		return mob;
	}

	private RuleContext active(int cell) {
		RuleContext context = new RuleContext(RuleEvent.ACTIVE, hero);
		context.cell = cell;
		return context;
	}

	private TargetingSpec enemyTarget() {
		TargetingSpec target = new TargetingSpec();
		target.selector = TargetingSpec.Selector.SELECTED_ACTOR;
		target.filter = TargetingSpec.Filter.ENEMY;
		return target;
	}

	@Test public void projectileUsesRealCollisionActorPierceAndRange() {
		Mob far = mob(26, 20);
		TargetingSpec target = enemyTarget();
		RuleContext context = active(far.pos); context.target = far;
		level.setQaTerrain(25, Terrain.WALL);
		assertTrue(SkillTargetResolver.resolve(context, SkillDelivery.PROJECTILE, target, new RuleModifier()).isEmpty());

		level.setQaTerrain(25, Terrain.EMPTY);
		assertEquals(java.util.Arrays.asList(26), SkillTargetResolver.resolve(context,
				SkillDelivery.PROJECTILE, target, new RuleModifier()));
		target.range = 1;
		assertTrue(SkillTargetResolver.resolve(context, SkillDelivery.PROJECTILE, target, new RuleModifier()).isEmpty());
		target.range = 6;
		Mob near = mob(25, 20);
		RuleModifier pierce = new RuleModifier(RuleModifier.Type.PIERCE); pierce.magnitude = 3;
		ArrayList<Integer> pierced = SkillTargetResolver.resolve(context, SkillDelivery.PROJECTILE, target, pierce);
		assertEquals(java.util.Arrays.asList(25, 26), pierced);
		assertSame(near, Actor.findChar(25));
	}

	@Test public void beamAndOrthogonalCoverageAreDeterministic() {
		Mob east = mob(25, 20); mob(26, 20); mob(17, 20); mob(31, 20);
		TargetingSpec beam = enemyTarget(); beam.coverage = TargetingSpec.Coverage.LINE; beam.maxTargets = 6;
		RuleContext context = active(26); context.target = Actor.findChar(26);
		assertEquals(java.util.Arrays.asList(25, 26), SkillTargetResolver.resolve(context,
				SkillDelivery.TRACE_BEAM, beam, new RuleModifier()));

		TargetingSpec ring = new TargetingSpec(); ring.selector = TargetingSpec.Selector.SELF;
		ring.filter = TargetingSpec.Filter.ENEMY; ring.coverage = TargetingSpec.Coverage.RING;
		ring.magnitude = 1; ring.maxTargets = 8;
		assertEquals(3, SkillTargetResolver.resolve(active(hero.pos), SkillDelivery.DIRECT_TARGET,
				ring, new RuleModifier()).size());

		TargetingSpec cone = enemyTarget(); cone.coverage = TargetingSpec.Coverage.CONE;
		cone.range = 3; cone.maxTargets = 8;
		RuleContext coneContext = active(east.pos); coneContext.target = east;
		assertTrue(SkillTargetResolver.resolve(coneContext, SkillDelivery.DIRECT_TARGET,
				cone, new RuleModifier()).contains(25));

		TargetingSpec chain = enemyTarget(); chain.coverage = TargetingSpec.Coverage.CHAIN;
		chain.magnitude = 2; chain.maxTargets = 3;
		assertEquals(3, SkillTargetResolver.resolve(coneContext, SkillDelivery.DIRECT_TARGET,
				chain, new RuleModifier()).size());
	}

	@Test public void filtersAndSeededRandomUseRealActorRegistry() {
		Mob one = mob(25, 20); Mob two = mob(26, 20);
		RuleMark.apply(one, RuleMark.Type.HUNTED, hero, 2, 5);
		TargetingSpec marked = enemyTarget(); marked.selector = TargetingSpec.Selector.ALL_MATCHING;
		marked.filter = TargetingSpec.Filter.MARKED;
		assertEquals(java.util.Arrays.asList(one.pos), SkillTargetResolver.resolve(active(one.pos),
				SkillDelivery.DIRECT_TARGET, marked, new RuleModifier()));

		RuleOwnership.assign(two, hero);
		TargetingSpec owned = new TargetingSpec(); owned.selector = TargetingSpec.Selector.ALL_MATCHING;
		owned.filter = TargetingSpec.Filter.OWNED_ENTITY;
		assertEquals(java.util.Arrays.asList(two.pos), SkillTargetResolver.resolve(active(two.pos),
				SkillDelivery.DIRECT_TARGET, owned, new RuleModifier()));

		TargetingSpec random = enemyTarget(); random.selector = TargetingSpec.Selector.RANDOM;
		Random.pushGenerator(9981L);
		int first;
		try { first = SkillTargetResolver.resolve(active(25), SkillDelivery.DIRECT_TARGET,
				random, new RuleModifier()).get(0); } finally { Random.popGenerator(); }
		Random.pushGenerator(9981L);
		try { assertEquals(first, (int)SkillTargetResolver.resolve(active(25), SkillDelivery.DIRECT_TARGET,
				random, new RuleModifier()).get(0)); } finally { Random.popGenerator(); }
	}

	@Test public void temporaryHpPrecedesRealHpAndExpiresIndependently() {
		RuleTemporaryHP temporary = Buff.affect(hero, RuleTemporaryHP.class);
		temporary.grant(8, 3, 2);
		hero.damage(5, new Object());
		assertEquals(100, hero.HP);
		assertEquals(3, temporary.shielding());
		temporary.act(); temporary.act(); temporary.act();
		assertNull(hero.buff(RuleTemporaryHP.class));
		assertEquals(100, hero.HP);
	}

	@Test public void redirectTransfersDamageAndStopsReciprocalCycle() {
		Mob recipient = mob(25, 30);
		Buff.affect(hero, RuleDamageRedirect.class).set(recipient, 50, 5);
		Buff.affect(recipient, RuleDamageRedirect.class).set(hero, 50, 5);
		hero.damage(10, new Object());
		assertEquals(95, hero.HP);
		assertEquals(25, recipient.HP);
	}

	@Test public void terrainValidationAllowsInternalWallButRejectsProtectedCellsAndSeedsBlob() {
		level.setQaTerrain(25, Terrain.WALL);
		assertTrue(WorldCapabilityValidator.canAlterTerrain(level, 25, WorldCapability.DESTRUCTIBLE));
		assertFalse(WorldCapabilityValidator.canAlterTerrain(level, 0, WorldCapability.DESTRUCTIBLE));
		assertFalse(WorldCapabilityValidator.canAlterTerrain(level, level.entrance, WorldCapability.DESTRUCTIBLE));
		EffectSpec destroy = new EffectSpec(EffectFamily.WORLD_TERRAIN, EffectSpec.Operation.WORLD_DESTROY, 1);
		assertTrue(SkillEffectRuntime.apply(destroy, hero.ruleRuntime(), active(25), null, 25, new RuleModifier()));
		assertEquals(Terrain.EMPTY, level.map[25]);
		EffectSpec gas = new EffectSpec(EffectFamily.WORLD_TERRAIN, EffectSpec.Operation.WORLD_TOXIC_GAS, 2);
		assertTrue(SkillEffectRuntime.apply(gas, hero.ruleRuntime(), active(26), null, 26, new RuleModifier()));
		assertNotNull(level.blobs.get(ToxicGas.class));
	}

	@Test public void modeIsQueryableExpiresAndProtectedTransferFailsClosed() {
		EffectSpec mode = new EffectSpec(EffectFamily.TRANSFORM, EffectSpec.Operation.TRANSFORM_MODE, 1);
		mode.stateId = "assault"; mode.duration = 2;
		assertTrue(SkillEffectRuntime.apply(mode, hero.ruleRuntime(), active(hero.pos), hero, hero.pos, new RuleModifier()));
		RuleCondition condition = new RuleCondition(RuleCondition.Type.MODE_IS); condition.reference = "assault";
		assertTrue(condition.passes(hero.ruleRuntime(), active(hero.pos), hero));
		RuleMode buff = hero.buff(RuleMode.class); buff.act(); buff.act();
		assertFalse(condition.passes(hero.ruleRuntime(), active(hero.pos), hero));

		Mob target = mob(25, 20);
		Buff.affect(hero, RuleMode.class).set("protected", 5);
		EffectSpec copy = new EffectSpec(EffectFamily.TRANSFER_COPY, EffectSpec.Operation.COPY_STATUS, 1);
		assertFalse(SkillEffectRuntime.apply(copy, hero.ruleRuntime(), active(target.pos), target, target.pos, new RuleModifier()));
		Buff.affect(hero, Poison.class).set(5);
		assertTrue(SkillEffectRuntime.apply(copy, hero.ruleRuntime(), active(target.pos), target, target.pos, new RuleModifier()));
		assertNotNull(target.buff(Poison.class));
		assertFalse(new EffectSpec(EffectFamily.TRANSFER_COPY, EffectSpec.Operation.TRANSFER_RESOURCE, 1).implemented());
	}

	@Test public void entityProductionHasRuntimeCapAndOwnership() {
		EffectSpec actor = new EffectSpec(EffectFamily.CREATE_ENTITY, EffectSpec.Operation.CREATE_ACTOR, 2);
		actor.count = 3; actor.lifetime = 5;
		for (int i = 0; i < 4; i++) SkillEffectRuntime.apply(actor, hero.ruleRuntime(), active(25), null, 25, new RuleModifier());
		int owned = 0;
		for (Char value : Actor.chars()) if (value instanceof RuleOwnedEntity) {
			owned++;
			assertTrue(RuleOwnership.isOwnedBy(value, hero));
		}
		assertEquals(6, owned);
	}

	@Test public void inheritanceCopiesOnlyWhitelistedRealOwnerState() {
		RuleOwnedEntity ally = new RuleOwnedEntity().configure(RuleOwnedEntity.Kind.ACTOR,
				hero, 8, 20, 2, 1, null);
		ally.pos = 25;
		level.mobs.add(ally);
		Actor.add(ally);
		Buff.affect(hero, RuleMode.class).set("assault", 5);
		Buff.affect(hero, RuleMitigation.class).set(35, 5);

		EffectSpec inherit = new EffectSpec(EffectFamily.RELATION_CONTROL,
				EffectSpec.Operation.RELATION_INHERIT, 1);
		inherit.stateId = "mode";
		assertTrue(SkillEffectRuntime.apply(inherit, hero.ruleRuntime(), active(ally.pos),
				ally, ally.pos, new RuleModifier()));
		assertEquals("assault", ally.buff(RuleMode.class).modeId());
		assertNotNull(ally.buff(RuleRelation.class));

		inherit.stateId = "mitigation";
		assertTrue(SkillEffectRuntime.apply(inherit, hero.ruleRuntime(), active(ally.pos),
				ally, ally.pos, new RuleModifier()));
		assertEquals(35, ally.buff(RuleMitigation.class).percent());

		inherit.stateId = "arbitrary_rule_runtime";
		assertFalse(SkillEffectRuntime.apply(inherit, hero.ruleRuntime(), active(ally.pos),
				ally, ally.pos, new RuleModifier()));
		hero.HP = 0;
		ally.buff(RuleOwnership.class).act();
		assertNull(ally.buff(RuleOwnership.class));
	}

	@Test public void secondaryEffectRunsForEveryHitAndSharesDelay() {
		Mob first = mob(25, 20);
		Mob second = mob(26, 20);
		SkillSpec skill = new SkillSpec();
		skill.id = "area_secondary";
		skill.primary = new EffectSpec(EffectFamily.DAMAGE, EffectSpec.Operation.DAMAGE_STANDARD, 2);
		skill.secondary = new EffectSpec(EffectFamily.STATUS, EffectSpec.Operation.STATUS_POISON, 1);
		skill.targeting = enemyTarget();
		skill.targeting.coverage = TargetingSpec.Coverage.RADIUS;
		skill.targeting.magnitude = 1;
		skill.targeting.maxTargets = 4;
		assertTrue(hero.ruleRuntime().addRule(skill.compile(), true));
		RuleContext context = active(first.pos); context.target = first;
		assertTrue(hero.ruleRuntime().dispatchActive(skill.id, context));
		assertEquals(18, first.HP);
		assertEquals(18, second.HP);
		assertNotNull(first.buff(Poison.class));
		assertNotNull(second.buff(Poison.class));

		SkillSpec delayed = new SkillSpec();
		delayed.id = "delayed_secondary";
		delayed.primary = new EffectSpec(EffectFamily.DAMAGE, EffectSpec.Operation.DAMAGE_STANDARD, 1);
		delayed.secondary = new EffectSpec(EffectFamily.STATUS, EffectSpec.Operation.STATUS_SLOW, 1);
		delayed.targeting = enemyTarget();
		delayed.modifier = new RuleModifier(RuleModifier.Type.DELAY);
		delayed.modifier.magnitude = 3;
		assertTrue(hero.ruleRuntime().addRule(delayed.compile(), true));
		RuleContext delayedContext = active(first.pos); delayedContext.target = first;
		assertTrue(hero.ruleRuntime().dispatchActive(delayed.id, delayedContext));
		assertEquals(2, hero.ruleRuntime().pendingDelayedCount());
	}

	@Test public void damageTypeScalingAndStatusStackingUseRealSpdPipelines() {
		BurningImmuneRat immune = new BurningImmuneRat();
		immune.pos=25;immune.HT=immune.HP=20;immune.EXP=0;immune.maxLvl=-1;
		level.mobs.add(immune);Actor.add(immune);
		EffectSpec typed = new EffectSpec(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,8);
		typed.damageType=EffectSpec.DamageType.FIRE;
		assertFalse(SkillEffectRuntime.apply(typed,hero.ruleRuntime(),active(25),immune,25,new RuleModifier()));
		assertEquals(20,immune.HP);
		typed.damageType=EffectSpec.DamageType.UNTYPED;
		SkillEffectRuntime.apply(typed,hero.ruleRuntime(),active(25),immune,25,new RuleModifier());
		assertEquals(12,immune.HP);

		Mob scalingTarget=mob(26,30);hero.lvl=10;
		EffectSpec scaling=new EffectSpec(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,2);
		scaling.scalingSource=EffectSpec.ScalingSource.HERO_LEVEL;
		SkillEffectRuntime.apply(scaling,hero.ruleRuntime(),active(26),scalingTarget,26,new RuleModifier());
		assertEquals(25,scalingTarget.HP);

		EffectSpec bleed=new EffectSpec(EffectFamily.STATUS,EffectSpec.Operation.STATUS_BLEEDING,4);
		bleed.statusStacking=EffectSpec.StatusStacking.EXTEND;
		SkillEffectRuntime.apply(bleed,hero.ruleRuntime(),active(26),scalingTarget,26,new RuleModifier());
		SkillEffectRuntime.apply(bleed,hero.ruleRuntime(),active(26),scalingTarget,26,new RuleModifier());
		assertEquals(8f,scalingTarget.buff(Bleeding.class).level(),0f);
	}

	@Test public void everyFullSkillReferenceScenarioRunsRealGameplay() {
		for (QaScenario scenario : FullSkillReferenceBuilds.scenarios()) {
			ScenarioResult result = new HeadlessGameplayHarness().run(scenario);
			assertFalse(scenario.id + ": " + result.failure, result.runtimeFailure);
			assertTrue(scenario.id + ": " + result.findings, result.expectationMet);
		}
	}

	@Test public void unsupportedCapabilitiesAndNewPowerCostsFailClosedOrCostBudget() {
		assertFalse(new EffectSpec(EffectFamily.TRANSFORM, EffectSpec.Operation.TRANSFORM_CAPABILITY, 2).implemented());
		assertFalse(new EffectSpec(EffectFamily.TRANSFORM, EffectSpec.Operation.TRANSFORM_BEHAVIOR, 2).implemented());
		assertTrue(new EffectSpec(EffectFamily.CREATE_ENTITY, EffectSpec.Operation.CREATE_ACTOR, 2).powerCost() > 0);
		assertTrue(SkillDelivery.PROJECTILE.powerCost() > 0);
		assertTrue(SkillDelivery.PERSISTENT_CARRIER.powerCost() > 0);
		assertTrue(new RuleModifier(RuleModifier.Type.PIERCE).capacityCost() > 0);
		EffectSpec dash = new EffectSpec(EffectFamily.MOVEMENT, EffectSpec.Operation.MOVE_DASH, 2);
		assertFalse(dash.compatibleTargeting(enemyTarget()));
		TargetingSpec destination = new TargetingSpec();
		destination.selector = TargetingSpec.Selector.SELECTED_CELL;
		destination.filter = TargetingSpec.Filter.ANY;
		assertTrue(dash.compatibleTargeting(destination));
	}
}
