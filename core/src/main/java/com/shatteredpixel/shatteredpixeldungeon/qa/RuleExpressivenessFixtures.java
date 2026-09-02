package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEffect;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEventBridge;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleMarkCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleMarkEffect;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleSemanticTag;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTarget;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrigger;

/** QA-only build assembly; every installed module is a production RuleRuntime module. */
public final class RuleExpressivenessFixtures {
	public static final String EVENT_CHAIN = "EVENT_CHAIN";
	public static final String BRIDGE_MOVE = "BRIDGE_MOVE";
	public static final String BRIDGE_RECURSION = "BRIDGE_RECURSION";
	public static final String MARK_CHAIN = "MARK_CHAIN";
	public static final String MARK_EXPIRE = "MARK_EXPIRE";
	public static final String DELAY_THREE = "DELAY_THREE";
	public static final String DELAY_SAVE = "DELAY_SAVE";

	private RuleExpressivenessFixtures() {}

	public static void install(String preset, Hero hero) {
		if (preset == null || hero == null || hero.ruleRuntime() == null) return;
		RuleRuntime runtime = hero.ruleRuntime();
		RuleDefinition first = runtime.rules().get(0);
		RuleDefinition second = runtime.rules().get(1);

		if (EVENT_CHAIN.equals(preset)) {
			configure(first, "chain_poison", RuleEvent.ON_HIT, RuleTarget.Type.HIT_TARGET,
					new RuleEffect(RuleEffect.Type.POISON, 3));
			configure(second, "chain_observer", RuleEvent.ON_KILL, RuleTarget.Type.SELF,
					new RuleEffect(RuleEffect.Type.SHIELD, 1));
		} else if (BRIDGE_MOVE.equals(preset) || BRIDGE_RECURSION.equals(preset)) {
			configure(first, "bridge_push", RuleEvent.ACTIVE, RuleTarget.Type.SELECTED_TARGET,
					new RuleEffect(RuleEffect.Type.PUSH, 2));
			configure(second, "bridge_move_rule", RuleEvent.ON_MOVE,
					BRIDGE_RECURSION.equals(preset) ? RuleTarget.Type.NEAREST_ENEMY : RuleTarget.Type.SELF,
					new RuleEffect(BRIDGE_RECURSION.equals(preset) ? RuleEffect.Type.PULL : RuleEffect.Type.SHIELD, 2));
			runtime.addBridge(new RuleEventBridge(RuleSemanticTag.FORCED_MOVEMENT, RuleEvent.ON_MOVE));
		} else if (MARK_CHAIN.equals(preset)) {
			configure(first, "mark_apply", RuleEvent.ON_HIT, RuleTarget.Type.HIT_TARGET,
					new RuleMarkEffect(RuleMarkEffect.Operation.APPLY, RuleMark.Type.HUNTED, 1, 5));
			configure(second, "mark_followup", RuleEvent.ON_HIT, RuleTarget.Type.HIT_TARGET,
					new RuleEffect(RuleEffect.Type.SHIELD, 1));
			second.setConditions(new RuleMarkCondition(RuleMark.Type.HUNTED, 1));
		} else if (MARK_EXPIRE.equals(preset)) {
			configure(first, "mark_expire", RuleEvent.ACTIVE, RuleTarget.Type.SELECTED_TARGET,
					new RuleMarkEffect(RuleMarkEffect.Operation.APPLY, RuleMark.Type.HUNTED, 2, 3));
			configure(second, "unused_mark_rule", RuleEvent.ON_KILL, RuleTarget.Type.SELF,
					new RuleEffect(RuleEffect.Type.SHIELD, 1));
		} else if (DELAY_THREE.equals(preset) || DELAY_SAVE.equals(preset)) {
			configure(first, "delayed_shield", RuleEvent.ACTIVE, RuleTarget.Type.SELF,
					new RuleEffect(RuleEffect.Type.SHIELD, 2));
			first.setDelayTurns(3);
			configure(second, "unused_delay_rule", RuleEvent.ON_KILL, RuleTarget.Type.SELF,
					new RuleEffect(RuleEffect.Type.SHIELD, 1));
		}
	}

	private static void configure(RuleDefinition rule, String id, RuleEvent event,
			RuleTarget.Type target, RuleEffect effect) {
		rule.id = id;
		rule.trigger = new RuleTrigger(event);
		rule.setConditions(new RuleCondition(RuleCondition.Type.ALWAYS));
		rule.cost = new RuleCost(RuleCost.Type.NONE, 0);
		rule.target = new RuleTarget(target);
		rule.effect = effect;
		// This fixture intentionally installs production Rule modules after ClassBuild compilation.
		// Clear the compiled SkillSpec adapters so the just-installed modules are authoritative.
		rule.effectSpec = null;
		rule.secondaryEffectSpec = null;
		rule.targetingSpec = null;
		rule.delivery = null;
		rule.modifier = new RuleModifier();
		rule.setDelayTurns(0);
	}
}
