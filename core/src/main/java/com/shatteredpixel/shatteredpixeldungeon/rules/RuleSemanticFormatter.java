package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;

/**
 * Player-facing language for composable rules. Runtime modules remain the source of truth, while
 * this class keeps their enum vocabulary and debug syntax out of normal UI.
 */
public final class RuleSemanticFormatter {

	private RuleSemanticFormatter() {}

	private static String msg(String key, Object... args) {
		return Messages.get(RuleSemanticFormatter.class, key, args);
	}

	public static String techniqueName(RuleDefinition rule) {
		if (rule == null || rule.trigger == null || rule.effect == null) return msg("unnamed_technique");
		RuleEvent event = rule.trigger.event;
		RuleEffect.Type effect = rule.effect.type;
		if (event == RuleEvent.ON_HIT && hasCondition(rule, RuleCondition.Type.TARGET_HAS_POISON)
				&& effect == RuleEffect.Type.PULL) return msg("name_toxic_pull");
		if (event == RuleEvent.ON_WAIT && effect == RuleEffect.Type.SHIELD) return msg("name_calm_shield");
		if (event == RuleEvent.ON_ENTER_TILE && hasCondition(rule, RuleCondition.Type.SELF_IN_WATER)
				&& effect == RuleEffect.Type.SHIELD) return msg("name_tidal_bulwark");
		if (event == RuleEvent.ACTIVE && effect == RuleEffect.Type.TELEPORT) return msg("name_phase_step");
		if (event == RuleEvent.ACTIVE && effect == RuleEffect.Type.CREATE_WATER) return msg("name_shape_water");
		if (event == RuleEvent.ACTIVE && effect == RuleEffect.Type.PUSH) return msg("name_impact");
		if (event == RuleEvent.ON_HIT && effect == RuleEffect.Type.POISON) return msg("name_toxic_strike");
		return event == RuleEvent.ACTIVE
				? msg("name_active_fallback", effectName(effect))
				: msg("name_reactive_fallback", triggerLabel(event), effectName(effect));
	}

	public static String techniqueDescription(RuleDefinition rule, ResourceEngine engine) {
		if (rule == null || rule.trigger == null || rule.effect == null || rule.target == null) {
			return msg("unavailable_description");
		}
		String cost = costPhrase(rule.cost, engine);
		String action = effectPhrase(rule.effect.type, rule.target.type);
		String modifier = modifierPhrase(rule.modifier);
		String conditions = conditionsPhrase(rule, engine);
		if (rule.trigger.event == RuleEvent.ACTIVE) {
			return conditions.isEmpty()
					? msg("active_sentence", cost, action, modifier)
					: msg("active_condition_sentence", conditions, cost, action, modifier);
		}
		String event = eventAndConditions(rule, engine);
		return msg("reaction_sentence", msg("when_event", event), cost, action, modifier);
	}

	public static String resourceTooltip(RuleRuntime runtime, Hero hero) {
		if (runtime == null) return msg("resource_unavailable");
		ResourceEngine engine = runtime.engine();
		if (engine == null) return msg("resource_unavailable");
		if (engine == ResourceEngine.BLOOD && runtime.classBuild() == null) {
			int hp = hero == null ? 0 : hero.HP;
			int ht = hero == null ? 0 : hero.HT;
			return msg("blood_tooltip", engine.description(), hp, ht);
		}
		ResourceSpec spec = runtime.primaryResourceSpec();
		String description = resourceFlowDescription(runtime.classBuild(), spec);
		return msg("resource_tooltip", description, runtime.primaryResourceName(),
				runtime.resource(), runtime.maxResource());
	}

	private static String resourceFlowDescription(ClassBuild build, ResourceSpec spec) {
		StringBuilder result = new StringBuilder();
		if (build != null && spec != null) for (ClassGameplayComponentSpec component : build.gameplayComponents) {
			if (component != null && spec.id.equals(component.resourceId)
					&& (component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW
					|| component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL)) {
				if (result.length() > 0) result.append("\n");
				result.append("• ").append(component.description(build));
			}
		}
		return result.length() == 0 ? msg("resource_manual_desc") : result.toString();
	}

	public static String conditionChoice(RuleCondition.Type type) {
		RuleCondition condition = new RuleCondition(type);
		String title = type == RuleCondition.Type.ALWAYS
				? msg("condition_none_name") : conditionPhrase(condition, null);
		String description = type == RuleCondition.Type.ALWAYS
				? msg("condition_none_desc") : msg("condition_choice_desc");
		return msg("choice", title, description);
	}

	public static String effectChoice(RuleEffect.Type type) {
		return msg("choice", effectName(type), msg("effect_choice_" + type.name().toLowerCase()));
	}

	public static String targetChoice(RuleTarget.Type type) {
		String target = targetPhrase(type);
		return msg("choice", new RuleTarget(type).description(), msg("target_choice", target));
	}

	public static String modifierChoice(RuleModifier.Type type) {
		RuleModifier modifier = new RuleModifier(type);
		return msg("choice", modifierName(modifier), msg("modifier_choice_" + type.name().toLowerCase(),
				Math.max(1, modifier.magnitude)));
	}

	public static String triggerChoice(RuleEvent event) {
		return msg("choice", triggerLabel(event), msg("trigger_choice_" + event.name().toLowerCase()));
	}

	public static String effectName(RuleEffect.Type type) {
		return new RuleEffect(type, 1).description();
	}

	public static String triggerName(RuleEvent event) { return triggerLabel(event); }

	private static String modifierName(RuleModifier modifier) {
		if (modifier == null || modifier.type == RuleModifier.Type.NONE) return msg("modifier_none_name");
		switch (modifier.type) {
			case AREA: return msg("modifier_area_name");
			case REPEAT: return msg("modifier_repeat_name");
			case EXTEND_DURATION: return msg("modifier_duration_name");
			default: return modifier.description();
		}
	}

	private static String triggerLabel(RuleEvent event) {
		return msg("trigger_label_" + event.name().toLowerCase());
	}

	private static String eventAndConditions(RuleDefinition rule, ResourceEngine engine) {
		ArrayList<RuleCondition> visible = visibleConditions(rule);
		if (visible.size() == 1 && rule.trigger.event == RuleEvent.ON_HIT) {
			if (visible.get(0).type == RuleCondition.Type.TARGET_HAS_POISON) return msg("event_hit_poisoned");
			if (visible.get(0).type == RuleCondition.Type.TARGET_IS_BURNING) return msg("event_hit_burning");
		}
		String event = msg("event_" + rule.trigger.event.name().toLowerCase());
		if (visible.isEmpty()) return event;
		return msg("event_with_conditions", event, joinConditions(visible, engine));
	}

	private static String conditionsPhrase(RuleDefinition rule, ResourceEngine engine) {
		return joinConditions(visibleConditions(rule), engine);
	}

	private static ArrayList<RuleCondition> visibleConditions(RuleDefinition rule) {
		ArrayList<RuleCondition> result = new ArrayList<>();
		if (rule == null) return result;
		for (RuleCondition condition : rule.conditions) {
			if (condition != null && condition.type != RuleCondition.Type.ALWAYS) result.add(condition);
		}
		return result;
	}

	private static String joinConditions(ArrayList<RuleCondition> conditions, ResourceEngine engine) {
		StringBuilder result = new StringBuilder();
		for (RuleCondition condition : conditions) {
			if (result.length() > 0) result.append(msg("condition_and"));
			result.append(conditionPhrase(condition, engine));
		}
		return result.toString();
	}

	private static String conditionPhrase(RuleCondition condition, ResourceEngine engine) {
		if (condition == null || condition.type == RuleCondition.Type.ALWAYS) return "";
		switch (condition.type) {
			case SELF_HP_BELOW:
			case TARGET_HP_BELOW:
			case DISTANCE_AT_LEAST:
			case ADJACENT_ENEMIES_AT_LEAST:
				return msg("condition_" + condition.type.name().toLowerCase(), condition.parameter);
			case RESOURCE_AT_LEAST:
				return msg("condition_resource_at_least", condition.parameter,
						engine == null ? msg("generic_resource") : engine.displayName());
			default:
				return msg("condition_" + condition.type.name().toLowerCase());
		}
	}

	private static String costPhrase(RuleCost cost, ResourceEngine engine) {
		if (cost == null || cost.type == RuleCost.Type.NONE || cost.amount <= 0) return "";
		switch (cost.type) {
			case HP: return msg("cost_hp", cost.amount);
			case ACTION: return msg("cost_action", cost.amount);
			case COOLDOWN: return msg("cost_cooldown", cost.amount);
			case CONSUMABLE: return msg("cost_consumable", cost.amount);
			case STATE: return msg("cost_state", cost.amount);
			case RESOURCE: return msg("cost_resource", cost.amount,
					engine == null ? msg("generic_resource") : engine.displayName());
			default: return "";
		}
	}

	private static String targetPhrase(RuleTarget.Type type) {
		return msg("target_" + type.name().toLowerCase());
	}

	private static String effectPhrase(RuleEffect.Type effect, RuleTarget.Type target) {
		return msg("effect_" + effect.name().toLowerCase(), targetPhrase(target));
	}

	private static String modifierPhrase(RuleModifier modifier) {
		if (modifier == null || modifier.type == RuleModifier.Type.NONE) return "";
		switch (modifier.type) {
			case AREA: return msg("modifier_area", Math.max(1, modifier.radius()));
			case REPEAT: return msg("modifier_repeat", Math.max(2, modifier.repeats()));
			case EXTEND_DURATION: return msg("modifier_extend_duration", Math.max(2, modifier.magnitude));
			default: return "";
		}
	}

	private static boolean hasCondition(RuleDefinition rule, RuleCondition.Type type) {
		for (RuleCondition condition : rule.conditions) if (condition != null && condition.type == type) return true;
		return false;
	}
}
