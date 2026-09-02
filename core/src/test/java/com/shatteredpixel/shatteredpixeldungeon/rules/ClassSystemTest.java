package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.qa.BuildAnalysis;
import com.shatteredpixel.shatteredpixeldungeon.qa.RuleBuild;
import com.shatteredpixel.shatteredpixeldungeon.qa.RuleBuildAnalyzer;
import com.watabou.utils.Bundle;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClassSystemTest {
	public static class TestConsumable extends Item {}

	@After public void clearDungeon() { Dungeon.level = null; }

	@Test public void classBuildSerializationRoundTripPreservesFreeComponents() {
		ClassBuild source = fullBuild();
		Bundle bundle = new Bundle();
		source.storeInBundle(bundle);
		ClassBuild restored = new ClassBuild();
		restored.restoreFromBundle(bundle);
		assertEquals(ClassBuild.SCHEMA_VERSION, restored.schemaVersion);
		assertEquals(source.name, restored.name);
		assertEquals(2, restored.resources.size());
		assertEquals(2, restored.skills.size());
		assertEquals(2, restored.laws.size());
		assertEquals(2, restored.traits.size());
		assertEquals(1, restored.allRestrictions().size());
		assertEquals(source.usedBudget(), restored.usedBudget());
		assertEquals(source.compileRules().size(), restored.compileRules().size());
	}

	@Test public void skillSpecSerializationRoundTripPreservesUnifiedModel() {
		SkillSpec source = activeSkill("roundtrip", RuleEffect.Type.PUSH);
		source.primary = new EffectSpec(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,4);
		source.primary.damageType=EffectSpec.DamageType.FIRE;
		source.primary.scalingSource=EffectSpec.ScalingSource.HERO_LEVEL;
		source.secondary = new EffectSpec(RuleEffect.Type.POISON, 2);
		source.modifier = new RuleModifier(RuleModifier.Type.AREA);
		source.constraint = new SkillConstraint(SkillConstraint.Variant.COOLDOWN, 3);
		source.cost = new RuleCost(RuleCost.Type.HP, 2);
		Bundle bundle = new Bundle(); source.storeInBundle(bundle);
		SkillSpec restored = new SkillSpec(); restored.restoreFromBundle(bundle);
		assertEquals(source.activation, restored.activation);
		assertEquals(source.primary.family, restored.primary.family);
		assertEquals(EffectSpec.DamageType.FIRE,restored.primary.damageType);
		assertEquals(EffectSpec.ScalingSource.HERO_LEVEL,restored.primary.scalingSource);
		assertEquals(source.secondary.variant, restored.secondary.variant);
		assertEquals(source.delivery, restored.delivery);
		assertEquals(source.targeting.selector, restored.targeting.selector);
		assertEquals(source.modifier.type, restored.modifier.type);
		assertEquals(source.cost.type, restored.cost.type);
		assertEquals(source.constraint.variant, restored.constraint.variant);
		assertTrue(restored.structurallyValid());
	}

	@Test public void allCostModelsShareOneSavedAndExecutableRuleCost() {
		RuleCost action = new RuleCost(RuleCost.Type.ACTION, 2);
		assertEquals(2f, action.actionTime(), 0f);
		assertEquals(1, action.budgetRebate());

		RuleCost cooldown = new RuleCost(RuleCost.Type.COOLDOWN, 3);
		SkillSpec cooled = activeSkill("cooled", RuleEffect.Type.PUSH);
		cooled.cost = cooldown;
		assertEquals(3f, cooled.compile().modifier.cooldown, 0f);

		Hero hero = new Hero(); hero.HT = hero.HP = 20;
		RuleRuntime runtime = new RuleRuntime(new ClassBuild());
		hero.setRuleRuntime(runtime);
		TestConsumable potion = new TestConsumable();
		assertTrue(potion.collect(hero.belongings.backpack));
		RuleCost consumable = new RuleCost(RuleCost.Type.CONSUMABLE, 1);
		consumable.reference = TestConsumable.class.getName();
		assertTrue(consumable.referenceValid());
		assertTrue(consumable.canPay(runtime, hero));
		consumable.pay(runtime, hero);
		assertTrue(hero.belongings.getAllItems(TestConsumable.class).isEmpty());

		RuleMark.apply(hero, RuleMark.Type.CHARGED, hero, 2, 10);
		RuleCost state = new RuleCost(RuleCost.Type.STATE, 1);
		state.reference = RuleMark.Type.CHARGED.name();
		assertTrue(state.canPay(runtime, hero));
		state.pay(runtime, hero);
		assertEquals(1, RuleMark.get(hero, RuleMark.Type.CHARGED).stacks());

		Bundle bundle = new Bundle(); state.storeInBundle(bundle);
		RuleCost restored = new RuleCost(); restored.restoreFromBundle(bundle);
		assertEquals(RuleCost.Type.STATE, restored.type);
		assertEquals(RuleMark.Type.CHARGED.name(), restored.reference);
	}

	@Test public void duplicateOrMeaninglessCostConstraintPairsAreRejected() {
		SkillSpec reactionAction = reactionSkill("reaction_action");
		reactionAction.cost = new RuleCost(RuleCost.Type.ACTION, 2);
		assertFalse(reactionAction.structurallyValid());

		SkillSpec duplicateCooldown = activeSkill("duplicate_cooldown", RuleEffect.Type.PUSH);
		duplicateCooldown.cost = new RuleCost(RuleCost.Type.COOLDOWN, 3);
		duplicateCooldown.constraint = new SkillConstraint(SkillConstraint.Variant.COOLDOWN, 3);
		assertFalse(duplicateCooldown.structurallyValid());

		SkillSpec commitment = activeSkill("commitment", RuleEffect.Type.PUSH);
		commitment.constraint = new SkillConstraint(SkillConstraint.Variant.HP_COMMITMENT, 2);
		assertFalse(commitment.structurallyValid());
		commitment.cost = new RuleCost(RuleCost.Type.HP, 2);
		assertTrue(commitment.structurallyValid());
	}

	@Test public void zeroResourceClassIsLegal() {
		ClassBuild build = new ClassBuild(); build.name = "zero";
		assertTrue(build.valid());
		BuildAnalysis analysis = analyze(build);
		assertEquals(BuildAnalysis.Classification.VALID, analysis.classification);
		RuleRuntime runtime = new RuleRuntime(build);
		assertNull(runtime.engine());
		assertTrue(runtime.rules().isEmpty());
	}

	@Test public void multipleResourcesAndRuntimeStateRoundTripIndependently() {
		ClassBuild build = new ClassBuild(); build.name = "multi-resource";
		build.resources.add(new ResourceSpec(ResourceEngine.MANA));
		build.resources.add(new ResourceSpec(ResourceEngine.MOMENTUM));
		SkillSpec mana = activeSkill("mana_skill", RuleEffect.Type.SHIELD);
		mana.targeting = selfTarget(); mana.delivery = SkillDelivery.SELF;
		mana.cost = resourceCost(build.resources.get(0), 2);
		SkillSpec momentum = activeSkill("momentum_skill", RuleEffect.Type.HASTE);
		momentum.targeting = selfTarget(); momentum.delivery = SkillDelivery.SELF;
		momentum.cost = resourceCost(build.resources.get(1), 2);
		build.skills.add(mana); build.skills.add(momentum);
		assertTrue(build.valid());
		Hero hero = new Hero(); hero.HT = hero.HP = 20;
		RuleRuntime runtime = new RuleRuntime(build); hero.setRuleRuntime(runtime);
		runtime.setResourceForDebug(hero, "mana", ResourceEngine.MANA, 9);
		runtime.setResourceForDebug(hero, "momentum", ResourceEngine.MOMENTUM, 7);
		Bundle saved = new Bundle(); runtime.storeInBundle(saved);
		RuleRuntime restored = new RuleRuntime(); restored.restoreFromBundle(saved);
		assertEquals(2, restored.presentationBuild().resources.size());
		assertEquals(1, restored.additionalResourceStates().size());
		assertEquals(7, restored.additionalResourceStates().get(0).value);
	}

	@Test public void multipleActiveAndReactionOnlyClassesCompileWithoutSlots() {
		ClassBuild active = new ClassBuild(); active.name = "multiple-active";
		active.skills.add(activeSkill("one", RuleEffect.Type.PUSH));
		active.skills.add(activeSkill("two", RuleEffect.Type.POISON));
		assertTrue(active.valid());
		assertEquals(2, new RuleRuntime(active).activeTechniques().size());

		ClassBuild reaction = new ClassBuild(); reaction.name = "reaction-only";
		reaction.skills.add(reactionSkill("guard"));
		assertTrue(reaction.valid());
		RuleRuntime runtime = new RuleRuntime(reaction);
		assertTrue(runtime.activeTechniques().isEmpty());
		assertNotNull(runtime.reactionTechnique());
	}

	@Test public void multipleLawsAndTraitsAreNotFixedSlots() {
		ClassBuild build = new ClassBuild(); build.name = "many-components";
		build.laws.add(ClassLaw.HEALING_TO_SHIELD);
		build.laws.add(ClassLaw.KILL_ACCELERATES_RULES);
		build.traits.add(TraitSpec.of(CoreRuleVocabulary.ACCUMULATION));
		build.traits.add(TraitSpec.of(CoreRuleVocabulary.ECHO));
		assertTrue(build.valid());
		RuleRuntime runtime = new RuleRuntime(build);
		assertEquals(2, runtime.laws().size());
		assertEquals(2, runtime.vocabularies().size());
	}

	@Test public void createEntityFamilyIsNowExecutableAndFullCapabilityOverrideRemainsRejected() {
		ClassBuild build = new ClassBuild(); build.name = "entity";
		SkillSpec skill = activeSkill("entity_skill", RuleEffect.Type.PUSH);
		skill.primary = new EffectSpec(EffectFamily.CREATE_ENTITY, EffectSpec.Operation.CREATE_ACTOR, 2);
		skill.targeting = selectedCell(); skill.delivery = SkillDelivery.GROUND_PLACEMENT;
		skill.cost = new RuleCost(RuleCost.Type.ACTION, 1);
		build.skills.add(skill);
		BuildAnalysis analysis = analyze(build);
		assertTrue(build.valid());
		assertNotEquals(BuildAnalysis.Classification.BROKEN, analysis.classification);

		ClassBuild unsupported = new ClassBuild(); unsupported.name = "full-capability-override";
		SkillSpec override = activeSkill("override", RuleEffect.Type.HASTE);
		override.primary = new EffectSpec(EffectFamily.TRANSFORM, EffectSpec.Operation.TRANSFORM_CAPABILITY, 2);
		unsupported.skills.add(override);
		assertEquals(BuildAnalysis.Classification.BROKEN, analyze(unsupported).classification);
		assertTrue(analyze(unsupported).has("UNSUPPORTED_ENGINE_CAPABILITY"));
	}

	@Test public void implementedMovementBuildPassesStructuralValidation() {
		ClassBuild build = new ClassBuild(); build.name = "movement";
		SkillSpec skill = activeSkill("step", RuleEffect.Type.TELEPORT);
		skill.targeting = selectedCell(); skill.delivery = SkillDelivery.GROUND_PLACEMENT;
		build.skills.add(skill);
		assertTrue(build.valid());
		assertNotEquals(BuildAnalysis.Classification.BROKEN, analyze(build).classification);
	}

	@Test public void terrainCapabilitiesRejectProtectedAndIllegalCells() {
		RuleRuntimeTest.TestLevel level = new RuleRuntimeTest.TestLevel();
		level.map[0] = Terrain.ENTRANCE;
		level.map[1] = Terrain.CHASM;
		level.map[2] = Terrain.WALL;
		level.solid[2] = true;
		assertFalse(WorldCapabilityValidator.supports(level, 0, WorldCapability.REPLACEABLE));
		assertFalse(WorldCapabilityValidator.supports(level, 1, WorldCapability.REPLACEABLE));
		assertFalse(WorldCapabilityValidator.supports(level, 2, WorldCapability.BLOB_SEEDABLE));
		assertTrue(WorldCapabilityValidator.supports(level, 3, WorldCapability.REPLACEABLE));
	}

	@Test public void automaticallySatisfiedConstraintGetsDiscountedAndReported() {
		ClassBuild build = new ClassBuild(); build.name = "fake-rebate";
		build.traits.add(TraitSpec.of(CoreRuleVocabulary.HUNT_MARK));
		SkillSpec skill = activeSkill("marked", RuleEffect.Type.PUSH);
		skill.constraint = new SkillConstraint(SkillConstraint.Variant.TARGET_MARKED, 1);
		build.skills.add(skill);
		assertTrue(skill.constraint.effectiveRebate(build) < skill.constraint.nominalRebate());
		assertTrue(analyze(build).has("FAKE_CONSTRAINT_REBATE"));
	}

	@Test public void analyzerFindsDeadResourceCostWithoutSourceAndNoVictory() {
		ClassBuild dead = new ClassBuild(); dead.name = "dead-resource";
		dead.resources.add(new ResourceSpec(ResourceEngine.MANA));
		assertTrue(analyze(dead).has("DEAD_RESOURCE"));

		ClassBuild sourceMissing = new ClassBuild(); sourceMissing.name = "source-missing";
		SkillSpec paid = activeSkill("paid", RuleEffect.Type.POISON);
		paid.cost = new RuleCost(RuleCost.Type.RESOURCE, 2); paid.cost.resourceId = "missing";
		sourceMissing.skills.add(paid);
		assertTrue(analyze(sourceMissing).has("COST_WITHOUT_SOURCE"));

		ClassBuild noVictory = new ClassBuild(); noVictory.name = "no-victory";
		noVictory.startingKit.mode = StartingKitSpec.Mode.UNARMED;
		SkillSpec shield = activeSkill("shield", RuleEffect.Type.SHIELD);
		shield.targeting = selfTarget(); shield.delivery = SkillDelivery.SELF;
		noVictory.skills.add(shield);
		assertTrue(analyze(noVictory).has("NO_VICTORY_PATH"));
	}

	@Test public void analyzerCoversNewRuntimeLoopOrphanTerrainAndModeRisks() {
		ClassBuild producer = new ClassBuild(); producer.name = "free-actors";
		SkillSpec summon = activeSkill("summon", RuleEffect.Type.POISON);
		summon.primary = new EffectSpec(EffectFamily.CREATE_ENTITY, EffectSpec.Operation.CREATE_ACTOR, 2);
		summon.targeting = selectedCell(); summon.delivery = SkillDelivery.GROUND_PLACEMENT;
		producer.skills.add(summon);
		assertTrue(analyze(producer).has("UNLIMITED_ACTOR_PRODUCTION"));

		ClassBuild orphan = new ClassBuild(); orphan.name = "orphan-command";
		SkillSpec command = activeSkill("command", RuleEffect.Type.PUSH);
		command.primary = new EffectSpec(EffectFamily.RELATION_CONTROL,
				EffectSpec.Operation.RELATION_COMMAND_FOLLOW, 1);
		command.targeting = new TargetingSpec();
		command.targeting.selector = TargetingSpec.Selector.ALL_MATCHING;
		command.targeting.filter = TargetingSpec.Filter.OWNED_ENTITY;
		command.cost = new RuleCost(RuleCost.Type.COOLDOWN, 2);
		orphan.skills.add(command);
		assertTrue(analyze(orphan).has("OWNERSHIP_ORPHAN"));

		ClassBuild terrain = new ClassBuild(); terrain.name = "self-destroy";
		SkillSpec destroy = activeSkill("destroy", RuleEffect.Type.PUSH);
		destroy.primary = new EffectSpec(EffectFamily.WORLD_TERRAIN, EffectSpec.Operation.WORLD_DESTROY, 1);
		destroy.targeting = selfTarget(); destroy.delivery = SkillDelivery.SELF;
		terrain.skills.add(destroy);
		assertTrue(analyze(terrain).has("TERRAIN_INVALID_OPERATION"));

		ClassBuild mode = new ClassBuild(); mode.name = "missing-mode";
		SkillSpec gated = activeSkill("gated", RuleEffect.Type.POISON);
		gated.conditions.clear(); RuleCondition condition = new RuleCondition(RuleCondition.Type.MODE_IS);
		condition.reference = "assault"; gated.conditions.add(condition); mode.skills.add(gated);
		assertTrue(analyze(mode).has("TRANSFORM_UNREACHABLE_MODE"));

		ClassBuild attachments = new ClassBuild(); attachments.name = "recursive-attachment";
		SkillSpec attachment = activeSkill("attachment", RuleEffect.Type.POISON);
		attachment.targeting = selfTarget(); attachment.delivery = SkillDelivery.ACTION_ATTACHMENT;
		attachment.attachmentEvent = RuleEvent.ACTIVE; attachments.skills.add(attachment);
		assertTrue(analyze(attachments).has("ACTION_ATTACHMENT_RECURSION"));

		ClassBuild redirects = new ClassBuild(); redirects.name = "redirect-cycle";
		for (int i=0;i<2;i++) { SkillSpec redirect=activeSkill("redirect_"+i,RuleEffect.Type.SHIELD);
			redirect.primary=new EffectSpec(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_REDIRECT,2);
			redirect.targeting=selfTarget();redirect.delivery=SkillDelivery.SELF;redirects.skills.add(redirect); }
		assertTrue(analyze(redirects).has("INFINITE_REDIRECT_RISK"));
	}

	@Test public void budgetOverflowIsRejected() {
		ClassBuild build = new ClassBuild(); build.name = "overflow"; build.baseBudget = 0;
		build.skills.add(activeSkill("expensive", RuleEffect.Type.FIRE));
		BuildAnalysis analysis = analyze(build);
		assertFalse(build.budgetValid());
		assertTrue(analysis.has("BUDGET_OVERFLOW"));
	}

	@Test public void oldFixedBuildMigratesWithoutSilentLoss() {
		CustomClassConfig old = new CustomClassConfig();
		old.name = "legacy";
		old.resource = ResourceEngine.RAGE;
		old.law = ClassLaw.STATUS_ABSORPTION;
		old.vocabulary1 = CoreRuleVocabulary.ECHO;
		old.restriction = Restriction.NO_ORDINARY_WEAPONS;
		ClassBuild migrated = ClassBuildMigrator.fromLegacy(old);
		assertEquals(ClassBuild.SCHEMA_VERSION, migrated.schemaVersion);
		assertEquals("legacy", migrated.name);
		assertEquals(ResourceEngine.RAGE, migrated.primaryResource().engine);
		assertEquals(2, migrated.skills.size());
		assertTrue(LawTraitRegistry.hasTrait(migrated, CoreRuleVocabulary.STATUS_FEEDBACK));
		assertTrue(LawTraitRegistry.hasTrait(migrated, CoreRuleVocabulary.ECHO));
		assertEquals(StartingKitSpec.Mode.UNARMED, migrated.startingKit.mode);
	}

	private static BuildAnalysis analyze(ClassBuild build) {
		return new RuleBuildAnalyzer().analyze(RuleBuild.from(build));
	}

	private static ClassBuild fullBuild() {
		ClassBuild build = new ClassBuild(); build.name = "roundtrip";
		build.resources.add(new ResourceSpec(ResourceEngine.MANA));
		build.resources.add(new ResourceSpec(ResourceEngine.RAGE));
		SkillSpec active = activeSkill("active", RuleEffect.Type.PUSH);
		active.cost = resourceCost(build.resources.get(0), 2);
		build.skills.add(active); build.skills.add(reactionSkill("reaction"));
		build.laws.add(ClassLaw.HEALING_TO_SHIELD); build.laws.add(ClassLaw.FORCED_MOVEMENT_COUNTS_AS_MOVE);
		build.traits.add(TraitSpec.of(CoreRuleVocabulary.ACCUMULATION)); build.traits.add(TraitSpec.of(CoreRuleVocabulary.ECHO));
		build.restrictions.add(Restriction.FRAIL);
		return build;
	}

	private static SkillSpec activeSkill(String id, RuleEffect.Type effect) {
		SkillSpec skill = new SkillSpec(); skill.id = id; skill.activation = RuleEvent.ACTIVE;
		skill.primary = new EffectSpec(effect, 2); skill.targeting = selectedEnemy();
		skill.delivery = SkillDelivery.DIRECT_TARGET; skill.cost = new RuleCost(RuleCost.Type.NONE, 0);
		return skill;
	}

	private static SkillSpec reactionSkill(String id) {
		SkillSpec skill = new SkillSpec(); skill.id = id; skill.activation = RuleEvent.ON_DAMAGED;
		skill.primary = new EffectSpec(RuleEffect.Type.SHIELD, 2); skill.targeting = selfTarget();
		skill.delivery = SkillDelivery.SELF; skill.cost = new RuleCost(RuleCost.Type.NONE, 0);
		return skill;
	}

	private static RuleCost resourceCost(ResourceSpec resource, int amount) {
		RuleCost result = new RuleCost(RuleCost.Type.RESOURCE, amount);
		result.resourceId = resource.id; result.resourceEngine = resource.engine; return result;
	}

	private static TargetingSpec selectedEnemy() {
		TargetingSpec target = new TargetingSpec(); target.selector = TargetingSpec.Selector.SELECTED_ACTOR;
		target.filter = TargetingSpec.Filter.ENEMY; return target;
	}

	private static TargetingSpec selectedCell() {
		TargetingSpec target = new TargetingSpec(); target.selector = TargetingSpec.Selector.SELECTED_CELL;
		target.filter = TargetingSpec.Filter.ANY; return target;
	}

	private static TargetingSpec selfTarget() { return new TargetingSpec(); }
}
