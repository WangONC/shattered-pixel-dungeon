package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassGameplayComponentSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.Restriction;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEffect;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTarget;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;

import java.util.ArrayList;
import java.util.Arrays;

import static com.shatteredpixel.shatteredpixeldungeon.qa.QaScenario.Action.Type.*;

/** Frozen V0.1 regression corpus. */
public final class RegressionScenarios {
	private RegressionScenarios() {}

	public static ArrayList<QaScenario> all() {
		ArrayList<QaScenario> result = new ArrayList<>(Arrays.asList(waterFocusWaitLoop(), noWeaponNoVictory(),
				poisonWithoutSource(), resourceWithoutSource(), momentumLoop(), affliction(),
				realCombat(), saveLoad(), eventChain(), bridgeMove(), bridgeRecursionGuard(),
				markChain(), markExpire(), delayThreeTurns(), delaySaveLoad(),
				accumulationEcho(), overflowOverdraw(), compensation(), phaseBlood(), huntMark(),
				huntPropagation(), inertiaMomentum(), translocationBridge(),
				translocationBacklash(), shieldBacklash(), classZeroResource(),
				classMultipleResource(), classMultipleActive(), classReactionOnly(),
				classMultipleLawsTraits(), classMovement()));
		result.addAll(FullSkillReferenceBuilds.scenarios());
		return result;
	}

	public static QaScenario classZeroResource() {
		ClassBuild build = new ClassBuild(); build.name = "class_zero_resource";
		QaScenario scenario = classScenario("class_zero_resource", 81001, build);
		scenario.actions.add(QaScenario.Action.of(WAIT));
		scenario.expectedFindings.add("CLASS_ZERO_RESOURCE_OK");
		return scenario;
	}

	public static QaScenario classMultipleResource() {
		ClassBuild build = new ClassBuild(); build.name = "class_multiple_resource";
		build.resources.add(new ResourceSpec(ResourceEngine.MANA));
		build.resources.add(new ResourceSpec(ResourceEngine.MOMENTUM));
		SkillSpec skill = qaSkill("mana_guard", RuleEvent.ACTIVE, RuleEffect.Type.SHIELD, selfTarget());
		skill.delivery = SkillDelivery.SELF;
		skill.cost = new RuleCost(RuleCost.Type.RESOURCE, 2);
		skill.cost.resourceId = "mana"; skill.cost.resourceEngine = ResourceEngine.MANA;
		build.skills.add(skill);
		QaScenario scenario = classScenario("class_multiple_resource", 81002, build);
		scenario.actions.add(QaScenario.Action.of(SET_RESOURCE).resource("mana").value(9));
		scenario.actions.add(QaScenario.Action.of(MOVE).cell(25));
		scenario.actions.add(QaScenario.Action.of(SAVE_RELOAD));
		scenario.expectedFindings.add("CLASS_MULTIPLE_RESOURCE_OK");
		return scenario;
	}

	public static QaScenario classMultipleActive() {
		ClassBuild build = new ClassBuild(); build.name = "class_multiple_active";
		SkillSpec one = qaSkill("active_one", RuleEvent.ACTIVE, RuleEffect.Type.SHIELD, selfTarget());
		one.delivery = SkillDelivery.SELF;
		SkillSpec two = qaSkill("active_two", RuleEvent.ACTIVE, RuleEffect.Type.HASTE, selfTarget());
		two.delivery = SkillDelivery.SELF;
		build.skills.add(one); build.skills.add(two);
		QaScenario scenario = classScenario("class_multiple_active", 81003, build);
		scenario.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).skill("active_one").cell(24));
		scenario.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).skill("active_two").cell(24));
		scenario.expectedFindings.add("CLASS_MULTIPLE_ACTIVE_OK");
		return scenario;
	}

	public static QaScenario classReactionOnly() {
		ClassBuild build = new ClassBuild(); build.name = "class_reaction_only";
		SkillSpec skill = qaSkill("wait_guard", RuleEvent.ON_WAIT, RuleEffect.Type.SHIELD, selfTarget());
		skill.delivery = SkillDelivery.SELF; build.skills.add(skill);
		QaScenario scenario = classScenario("class_reaction_only", 81004, build);
		scenario.actions.add(QaScenario.Action.of(WAIT));
		scenario.expectedFindings.add("CLASS_REACTION_ONLY_OK");
		return scenario;
	}

	public static QaScenario classMultipleLawsTraits() {
		ClassBuild build = new ClassBuild(); build.name = "class_multiple_laws_traits";
		build.laws.add(ClassLaw.HEALING_TO_SHIELD); build.laws.add(ClassLaw.FORCED_MOVEMENT_COUNTS_AS_MOVE);
		build.traits.add(com.shatteredpixel.shatteredpixeldungeon.rules.TraitSpec.of(CoreRuleVocabulary.ACCUMULATION));
		build.traits.add(com.shatteredpixel.shatteredpixeldungeon.rules.TraitSpec.of(CoreRuleVocabulary.ECHO));
		build.skills.add(qaSkill("multi_push",RuleEvent.ACTIVE,RuleEffect.Type.PUSH,
				new TargetingSpec(com.shatteredpixel.shatteredpixeldungeon.rules.RuleTarget.Type.SELECTED_TARGET)));
		build.skills.add(qaSkill("multi_wait",RuleEvent.ON_WAIT,RuleEffect.Type.SHIELD,selfTarget()));
		QaScenario scenario = classScenario("class_multiple_laws_traits", 81005, build);
		scenario.actions.add(QaScenario.Action.of(SAVE_RELOAD));
		scenario.expectedFindings.add("CLASS_MULTIPLE_COMPONENT_OK");
		return scenario;
	}

	public static QaScenario classMovement() {
		ClassBuild build = new ClassBuild(); build.name = "class_movement";
		SkillSpec skill = qaSkill("free_step", RuleEvent.ACTIVE, RuleEffect.Type.TELEPORT, selectedCellTarget());
		skill.delivery = SkillDelivery.GROUND_PLACEMENT; build.skills.add(skill);
		QaScenario scenario = classScenario("class_movement", 81006, build);
		scenario.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).skill("free_step").cell(25));
		scenario.expectedFindings.add("CLASS_MOVEMENT_OK");
		return scenario;
	}

	public static QaScenario waterFocusWaitLoop() {
		CustomClassConfig c = base("water_focus_wait_loop", ResourceEngine.FOCUS);
		c.law = ClassLaw.WATER_AFFINITY;
		c.activeTarget = RuleTarget.Type.SELECTED_TARGET;
		c.activeEffect = RuleEffect.Type.POISON;
		c.reactionTrigger = RuleEvent.ON_WAIT;
		c.reactionCondition = RuleCondition.Type.SELF_IN_WATER;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.SHIELD;
		QaScenario s = scenario("water_focus_wait_loop", 10086, c);
		s.level.waterCells = new int[]{s.level.heroCell};
		s.actions.add(QaScenario.Action.of(WAIT).repeat(500));
		s.expectedFindings.add("UNBOUNDED_POWER_LOOP");
		return s;
	}

	public static QaScenario noWeaponNoVictory() {
		CustomClassConfig c = base("no_weapon_no_victory", ResourceEngine.MANA);
		c.law = ClassLaw.HEALING_TO_SHIELD;
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_WAIT;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HEAL;
		c.restriction = Restriction.NO_ORDINARY_WEAPONS;
		QaScenario s = scenario("no_weapon_no_victory", 20001, c);
		s.staticOnly = true;
		s.expectedFindings.add("NO_VICTORY_PATH");
		return s;
	}

	public static QaScenario poisonWithoutSource() {
		CustomClassConfig c = base("poison_condition_without_source", ResourceEngine.MANA);
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_HIT;
		c.reactionCondition = RuleCondition.Type.TARGET_HAS_POISON;
		c.reactionTarget = RuleTarget.Type.HIT_TARGET;
		c.reactionEffect = RuleEffect.Type.PULL;
		QaScenario s = scenario("poison_condition_without_source", 20002, c);
		s.staticOnly = true;
		s.expectedFindings.add("CONDITION_SOURCE_MISSING");
		return s;
	}

	public static QaScenario resourceWithoutSource() {
		CustomClassConfig c = base("resource_without_source", ResourceEngine.AFFLICTION);
		c.law = ClassLaw.WATER_AFFINITY;
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_WAIT;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HASTE;
		ClassBuild build = c.toClassBuild();
		// Materialize the legacy preset once, then deliberately remove its ordinary
		// economy components.  Leaving the preset marker unnormalized allowed a later
		// RuleBuild conversion to recreate the source and invalidated this regression.
		build.normalizeLegacyComponents();
		for (int i = build.gameplayComponents.size() - 1; i >= 0; i--) {
			ClassGameplayComponentSpec component = build.gameplayComponents.get(i);
			if (component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW
					|| component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL) {
				build.gameplayComponents.remove(i);
			}
		}
		QaScenario s = classScenario("resource_without_source", 20003, build);
		s.staticOnly = true;
		s.expectedFindings.add("RESOURCE_WITHOUT_SOURCE");
		return s;
	}

	public static QaScenario momentumLoop() {
		CustomClassConfig c = base("momentum_loop", ResourceEngine.MOMENTUM);
		c.activeTarget = RuleTarget.Type.SELECTED_CELL;
		c.activeEffect = RuleEffect.Type.TELEPORT;
		c.reactionTrigger = RuleEvent.ON_MOVE;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HASTE;
		QaScenario s = scenario("momentum_loop", 30001, c);
		s.actions.add(QaScenario.Action.of(MOVE).cell(25));
		s.actions.add(QaScenario.Action.of(MOVE).cell(26));
		s.actions.add(QaScenario.Action.of(MOVE).cell(25));
		s.actions.add(QaScenario.Action.of(WAIT));
		s.expectedFindings.add("MOMENTUM_BEHAVIOR_OK");
		return s;
	}

	public static QaScenario affliction() {
		CustomClassConfig c = base("affliction", ResourceEngine.AFFLICTION);
		c.law = ClassLaw.STATUS_ABSORPTION;
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.CLEANSE;
		c.reactionTrigger = RuleEvent.ON_MOVE;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HASTE;
		QaScenario s = scenario("affliction", 30002, c);
		s.actions.add(QaScenario.Action.of(APPLY_STATUS).status("POISON").value(8));
		s.actions.add(QaScenario.Action.of(APPLY_STATUS).status("BURNING").value(8));
		s.expectedFindings.add("AFFLICTION_FLOW_OK");
		return s;
	}

	public static QaScenario realCombat() {
		CustomClassConfig c = base("real_combat", ResourceEngine.MANA);
		c.law = ClassLaw.KILL_ACCELERATES_RULES;
		c.activeTarget = RuleTarget.Type.SELECTED_TARGET;
		c.activeEffect = RuleEffect.Type.POISON;
		c.reactionTrigger = RuleEvent.ON_KILL;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.SHIELD;
		QaScenario s = scenario("real_combat", 40001, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(1));
		s.actions.add(QaScenario.Action.of(ATTACK).repeat(10));
		s.expectedFindings.add("REAL_COMBAT_OK");
		return s;
	}

	public static QaScenario saveLoad() {
		CustomClassConfig c = base("save_load", ResourceEngine.MANA);
		c.law = ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD;
		c.activeCondition = RuleCondition.Type.ALWAYS;
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.activeModifier = RuleModifier.Type.NONE;
		c.reactionTrigger = RuleEvent.ON_MOVE;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HASTE;
		QaScenario s = scenario("save_load", 50001, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(24));
		s.actions.add(QaScenario.Action.of(SAVE_RELOAD));
		s.expectedFindings.add("SAVE_LOAD_OK");
		return s;
	}

	public static QaScenario eventChain() {
		CustomClassConfig c = base("event_chain", ResourceEngine.AFFLICTION);
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_HIT;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.HIT_TARGET;
		c.reactionEffect = RuleEffect.Type.POISON;
		QaScenario s = scenario("event_chain", 60001, c);
		s.hero.qaRuntimePreset = RuleExpressivenessFixtures.EVENT_CHAIN;
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(200));
		s.actions.add(QaScenario.Action.of(ATTACK).repeat(3));
		s.expectedFindings.add("EVENT_CHAIN_OK");
		return s;
	}

	public static QaScenario bridgeMove() {
		QaScenario s = scenario("bridge_move", 60002, bridgeConfig("bridge_move"));
		s.hero.qaRuntimePreset = RuleExpressivenessFixtures.BRIDGE_MOVE;
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(100));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.expectedFindings.add("BRIDGE_MOVE_OK");
		return s;
	}

	public static QaScenario bridgeRecursionGuard() {
		QaScenario s = scenario("bridge_recursion_guard", 60003, bridgeConfig("bridge_recursion_guard"));
		s.hero.qaRuntimePreset = RuleExpressivenessFixtures.BRIDGE_RECURSION;
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(100));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.expectedFindings.add("BRIDGE_GUARD_OK");
		return s;
	}

	public static QaScenario markChain() {
		CustomClassConfig c = base("mark_chain", ResourceEngine.MANA);
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_HIT;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.HIT_TARGET;
		c.reactionEffect = RuleEffect.Type.SHIELD;
		QaScenario s = scenario("mark_chain", 60004, c);
		s.hero.qaRuntimePreset = RuleExpressivenessFixtures.MARK_CHAIN;
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(200));
		s.actions.add(QaScenario.Action.of(ATTACK).repeat(3));
		s.expectedFindings.add("MARK_CHAIN_OK");
		return s;
	}

	public static QaScenario markExpire() {
		QaScenario s = scenario("mark_expire", 60005, activeSelfConfig("mark_expire"));
		s.hero.qaRuntimePreset = RuleExpressivenessFixtures.MARK_EXPIRE;
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(100));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.actions.add(QaScenario.Action.of(WAIT).repeat(3));
		s.expectedFindings.add("MARK_EXPIRE_OK");
		return s;
	}

	public static QaScenario delayThreeTurns() {
		QaScenario s = scenario("delay_three_turns", 60006, activeSelfConfig("delay_three_turns"));
		s.hero.qaRuntimePreset = RuleExpressivenessFixtures.DELAY_THREE;
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(24));
		s.actions.add(QaScenario.Action.of(WAIT).repeat(3));
		s.expectedFindings.add("DELAY_THREE_OK");
		return s;
	}

	public static QaScenario delaySaveLoad() {
		QaScenario s = scenario("delay_save_load", 60007, activeSelfConfig("delay_save_load"));
		s.hero.qaRuntimePreset = RuleExpressivenessFixtures.DELAY_SAVE;
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(24));
		s.actions.add(QaScenario.Action.of(SAVE_RELOAD));
		s.actions.add(QaScenario.Action.of(WAIT).repeat(3));
		s.expectedFindings.add("DELAY_SAVE_LOAD_OK");
		return s;
	}

	public static QaScenario accumulationEcho() {
		CustomClassConfig c = base("accumulation_echo", ResourceEngine.MANA);
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_MOVE;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HASTE;
		c.vocabulary1 = CoreRuleVocabulary.ACCUMULATION;
		c.vocabulary2 = CoreRuleVocabulary.ECHO;
		c.restriction = Restriction.WEAK_HEALING;
		QaScenario s = scenario("accumulation_echo", 70001, c);
		s.actions.add(QaScenario.Action.of(MOVE).cell(25));
		s.actions.add(QaScenario.Action.of(MOVE).cell(26));
		s.actions.add(QaScenario.Action.of(MOVE).cell(25));
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.actions.add(QaScenario.Action.of(SAVE_RELOAD));
		s.actions.add(QaScenario.Action.of(WAIT).repeat(3));
		s.expectedFindings.add("ACCUMULATION_ECHO_OK");
		return s;
	}

	public static QaScenario overflowOverdraw() {
		CustomClassConfig c = base("overflow_overdraw", ResourceEngine.MANA);
		c.law = ClassLaw.WATER_AFFINITY;
		c.activeTarget = RuleTarget.Type.SELECTED_CELL;
		c.activeEffect = RuleEffect.Type.FIRE;
		c.reactionTrigger = RuleEvent.ON_KILL;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HASTE;
		c.vocabulary1 = CoreRuleVocabulary.OVERFLOW;
		c.vocabulary2 = CoreRuleVocabulary.OVERDRAW;
		c.restriction = Restriction.NO_TRADITIONAL_HEALING;
		QaScenario s = scenario("overflow_overdraw", 70002, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(WAIT).repeat(3));
		s.actions.add(QaScenario.Action.of(SAVE_RELOAD));
		s.actions.add(QaScenario.Action.of(WAIT).repeat(3));
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(0));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.expectedFindings.add("OVERFLOW_OVERDRAW_OK");
		return s;
	}

	public static QaScenario compensation() {
		CustomClassConfig c = base("compensation", ResourceEngine.MANA);
		c.law = ClassLaw.WATER_AFFINITY;
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.HEAL;
		c.reactionTrigger = RuleEvent.ON_KILL;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.POISON;
		c.vocabulary1 = CoreRuleVocabulary.COMPENSATION;
		c.vocabulary2 = CoreRuleVocabulary.NONE;
		QaScenario s = scenario("compensation", 70003, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(24));
		s.actions.add(QaScenario.Action.of(SAVE_RELOAD));
		s.expectedFindings.add("COMPENSATION_OK");
		return s;
	}

	public static QaScenario phaseBlood() {
		CustomClassConfig c = base("phase_blood", ResourceEngine.BLOOD);
		c.law = ClassLaw.WATER_AFFINITY;
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_KILL;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HEAL;
		c.vocabulary1 = CoreRuleVocabulary.PHASE_SHIFT;
		c.vocabulary2 = CoreRuleVocabulary.NONE;
		QaScenario s = scenario("phase_blood", 70004, c);
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(100));
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("SNAKE").cell(17).value(100));
		s.actions.add(QaScenario.Action.of(SET_HP).value(25));
		s.actions.add(QaScenario.Action.of(WAIT));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(24));
		s.actions.add(QaScenario.Action.of(SAVE_RELOAD));
		s.expectedFindings.add("PHASE_BLOOD_OK");
		return s;
	}

	public static QaScenario huntMark() {
		CustomClassConfig c = huntConfig("hunt_mark");
		c.vocabulary1 = CoreRuleVocabulary.HUNT_MARK;
		QaScenario s = scenario("hunt_mark", 70005, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(200));
		s.actions.add(QaScenario.Action.of(ATTACK).repeat(8));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.expectedFindings.add("HUNT_MARK_VOCAB_OK");
		return s;
	}

	public static QaScenario huntPropagation() {
		CustomClassConfig c = huntConfig("hunt_propagation");
		c.vocabulary1 = CoreRuleVocabulary.HUNT_MARK;
		c.vocabulary2 = CoreRuleVocabulary.PROPAGATION;
		QaScenario s = scenario("hunt_propagation", 70006, c);
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(1));
		// Hero is actor #1 after reset; the spawn order deterministically assigns Rat #2 and
		// Snake #3. Target the Rat explicitly instead of depending on level.mobs set order.
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("SNAKE").cell(17).value(100));
		s.actions.add(QaScenario.Action.of(ATTACK).target(2).repeat(10));
		s.expectedFindings.add("HUNT_PROPAGATION_OK");
		return s;
	}

	public static QaScenario inertiaMomentum() {
		CustomClassConfig c = base("inertia_momentum", ResourceEngine.MOMENTUM);
		c.activeTarget = RuleTarget.Type.SELECTED_TARGET;
		c.activeEffect = RuleEffect.Type.PUSH;
		c.reactionTrigger = RuleEvent.ON_MOVE;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.HASTE;
		c.vocabulary1 = CoreRuleVocabulary.INERTIA_BRIDGE;
		QaScenario s = scenario("inertia_momentum", 70007, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(SPAWN_MOB).mob("RAT").cell(25).value(100));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.expectedFindings.add("INERTIA_MOMENTUM_OK");
		return s;
	}

	public static QaScenario translocationBridge() {
		CustomClassConfig c = translocationConfig("translocation_bridge");
		c.reactionTrigger = RuleEvent.ON_ENTER_TILE;
		c.reactionTarget = RuleTarget.Type.CURRENT_TILE;
		c.reactionEffect = RuleEffect.Type.CREATE_WATER;
		c.vocabulary1 = CoreRuleVocabulary.TRANSLOCATION_BRIDGE;
		QaScenario s = scenario("translocation_bridge", 70008, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.expectedFindings.add("TRANSLOCATION_BRIDGE_OK");
		return s;
	}

	public static QaScenario translocationBacklash() {
		CustomClassConfig c = translocationConfig("translocation_backlash");
		c.vocabulary1 = CoreRuleVocabulary.TRANSLOCATION_BACKLASH;
		QaScenario s = scenario("translocation_backlash", 70009, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(25));
		s.expectedFindings.add("TRANSLOCATION_BACKLASH_OK");
		return s;
	}

	public static QaScenario shieldBacklash() {
		CustomClassConfig c = base("shield_backlash", ResourceEngine.AFFLICTION);
		c.law = ClassLaw.STATUS_ABSORPTION;
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_KILL;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.CLEANSE;
		c.vocabulary1 = CoreRuleVocabulary.SHIELD_BACKLASH;
		c.restriction = Restriction.WEAK_HEALING;
		QaScenario s = scenario("shield_backlash", 70010, c);
		s.actions.add(QaScenario.Action.of(SET_RESOURCE).value(10));
		s.actions.add(QaScenario.Action.of(USE_ACTIVE_RULE).cell(24));
		s.expectedFindings.add("SHIELD_BACKLASH_OK");
		return s;
	}

	private static CustomClassConfig huntConfig(String name) {
		CustomClassConfig c = base(name, ResourceEngine.MANA);
		c.activeTarget = RuleTarget.Type.SELECTED_TARGET;
		c.activeEffect = RuleEffect.Type.PUSH;
		c.reactionTrigger = RuleEvent.ON_KILL;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.SHIELD;
		return c;
	}

	private static CustomClassConfig translocationConfig(String name) {
		CustomClassConfig c = base(name, ResourceEngine.MANA);
		c.activeTarget = RuleTarget.Type.SELECTED_CELL;
		c.activeEffect = RuleEffect.Type.TELEPORT;
		c.reactionTrigger = RuleEvent.ON_KILL;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.SHIELD;
		return c;
	}

	private static CustomClassConfig bridgeConfig(String name) {
		CustomClassConfig c = activeSelfConfig(name);
		c.activeTarget = RuleTarget.Type.SELECTED_TARGET;
		c.activeEffect = RuleEffect.Type.PUSH;
		c.reactionTrigger = RuleEvent.ON_MOVE;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		return c;
	}

	private static CustomClassConfig activeSelfConfig(String name) {
		CustomClassConfig c = base(name, ResourceEngine.MANA);
		c.activeTarget = RuleTarget.Type.SELF;
		c.activeEffect = RuleEffect.Type.SHIELD;
		c.reactionTrigger = RuleEvent.ON_KILL;
		c.reactionCondition = RuleCondition.Type.ALWAYS;
		c.reactionTarget = RuleTarget.Type.SELF;
		c.reactionEffect = RuleEffect.Type.SHIELD;
		return c;
	}

	private static CustomClassConfig base(String name, ResourceEngine resource) {
		CustomClassConfig c = new CustomClassConfig();
		c.name = name;
		c.resource = resource;
		c.law = ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD;
		c.restriction = Restriction.NONE;
		return c;
	}

	private static QaScenario scenario(String id, long seed, CustomClassConfig config) {
		QaScenario scenario = new QaScenario();
		scenario.id = id;
		scenario.seed = seed;
		scenario.hero.build = config;
		return scenario;
	}

	private static QaScenario classScenario(String id, long seed, ClassBuild build) {
		QaScenario scenario = new QaScenario();
		scenario.id = id;
		scenario.seed = seed;
		scenario.hero.classBuild = build;
		return scenario;
	}

	private static SkillSpec qaSkill(String id, RuleEvent event, RuleEffect.Type effect,
			TargetingSpec targeting) {
		SkillSpec result = new SkillSpec();
		result.id = id;
		result.activation = event;
		result.primary = new EffectSpec(effect, 2);
		result.targeting = targeting;
		result.cost = new RuleCost(RuleCost.Type.NONE, 0);
		return result;
	}

	private static TargetingSpec selfTarget() { return new TargetingSpec(); }

	private static TargetingSpec selectedCellTarget() {
		TargetingSpec result = new TargetingSpec();
		result.selector = TargetingSpec.Selector.SELECTED_CELL;
		result.filter = TargetingSpec.Filter.ANY;
		return result;
	}
}
