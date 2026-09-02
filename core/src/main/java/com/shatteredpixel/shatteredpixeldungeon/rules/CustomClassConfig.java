package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;

/**
 * Saved custom-class envelope. {@link #classBuild} is the only authoritative model for new saves.
 * The scalar fields below are MIGRATION_ONLY input for pre-ClassBuild saves and old QA fixtures.
 */
public class CustomClassConfig implements Bundlable {
	/** MIGRATION_ONLY value used by the removed fixed-slot schema. */
	@Deprecated public static final int BASE_CAPACITY = 14;

	private static CustomClassConfig pending;
	/** Present for every newly-created class. Legacy scalar fields remain migration input only. */
	public ClassBuild classBuild;

	// MIGRATION_ONLY: removed fixed-slot schema. Never read by the player builder.
	public String name = "custom";
	public ResourceEngine resource = ResourceEngine.MANA;
	public ClassLaw law = ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD;
	public CoreRuleVocabulary vocabulary1 = CoreRuleVocabulary.NONE;
	public CoreRuleVocabulary vocabulary2 = CoreRuleVocabulary.NONE;

	public RuleCondition.Type activeCondition = RuleCondition.Type.ALWAYS;
	public int activeConditionParameter;
	public RuleTarget.Type activeTarget = RuleTarget.Type.SELECTED_CELL;
	public RuleEffect.Type activeEffect = RuleEffect.Type.FIRE;
	public RuleModifier.Type activeModifier = RuleModifier.Type.NONE;

	public RuleEvent reactionTrigger = RuleEvent.ON_HIT;
	public RuleCondition.Type reactionCondition = RuleCondition.Type.TARGET_HAS_POISON;
	public int reactionConditionParameter;
	public RuleCondition.Type reactionCondition2 = RuleCondition.Type.ALWAYS;
	public int reactionConditionParameter2;
	public RuleTarget.Type reactionTarget = RuleTarget.Type.HIT_TARGET;
	public RuleEffect.Type reactionEffect = RuleEffect.Type.PUSH;
	public RuleModifier.Type reactionModifier = RuleModifier.Type.NONE;

	public Restriction restriction = Restriction.NONE;

	/** V0.1 source compatibility for old debug/tests; mapped to the active technique. */
	@Deprecated public RuleEvent trigger = RuleEvent.ACTIVE;
	@Deprecated public RuleEffect.Type effect = RuleEffect.Type.FIRE;

	public CustomClassConfig() {}

	public CustomClassConfig copy() {
		CustomClassConfig result = new CustomClassConfig();
		result.classBuild = classBuild == null ? null : classBuild.copy();
		result.name = name;
		result.resource = resource;
		result.law = law;
		result.vocabulary1 = vocabulary1;
		result.vocabulary2 = vocabulary2;
		result.activeCondition = activeCondition;
		result.activeConditionParameter = activeConditionParameter;
		result.activeTarget = activeTarget;
		result.activeEffect = activeEffect;
		result.activeModifier = activeModifier;
		result.reactionTrigger = reactionTrigger;
		result.reactionCondition = reactionCondition;
		result.reactionConditionParameter = reactionConditionParameter;
		result.reactionCondition2 = reactionCondition2;
		result.reactionConditionParameter2 = reactionConditionParameter2;
		result.reactionTarget = reactionTarget;
		result.reactionEffect = reactionEffect;
		result.reactionModifier = reactionModifier;
		result.restriction = restriction;
		return result;
	}

	public ArrayList<RuleDefinition> buildRules() {
		if (classBuild != null) return classBuild.compileRules();
		ArrayList<RuleDefinition> result = new ArrayList<>();
		result.add(buildActiveRuleLegacy());
		result.add(buildReactionRuleLegacy());
		return result;
	}

	public RuleDefinition buildActiveRule() {
		if (classBuild != null) {
			for (SkillSpec skill : classBuild.skills) if (skill.activation == RuleEvent.ACTIVE) return skill.compile();
		}
		return buildActiveRuleLegacy();
	}

	RuleDefinition buildActiveRuleLegacy() {
		RuleEffect.Type chosenEffect = activeEffect == null ? effect : activeEffect;
		RuleDefinition rule = RuleDefinition.create(RuleEvent.ACTIVE, resource, chosenEffect);
		rule.id = "active_technique";
		rule.target = new RuleTarget(activeTarget);
		rule.effect = new RuleEffect(chosenEffect, defaultPower(chosenEffect));
		rule.modifier = new RuleModifier(activeModifier);
		rule.setConditions(new RuleCondition(activeCondition, activeConditionParameter));
		rule.priority = 10;
		return rule;
	}

	public RuleDefinition buildReactionRule() {
		if (classBuild != null) {
			for (SkillSpec skill : classBuild.skills) if (skill.activation != RuleEvent.ACTIVE) return skill.compile();
		}
		return buildReactionRuleLegacy();
	}

	RuleDefinition buildReactionRuleLegacy() {
		RuleDefinition rule = RuleDefinition.create(reactionTrigger, resource, reactionEffect);
		rule.id = "reaction_technique";
		rule.target = new RuleTarget(reactionTarget);
		rule.effect = new RuleEffect(reactionEffect, defaultPower(reactionEffect));
		rule.modifier = new RuleModifier(reactionModifier);
		if (reactionCondition2 == RuleCondition.Type.ALWAYS) {
			rule.setConditions(new RuleCondition(reactionCondition, reactionConditionParameter));
		} else {
			rule.setConditions(new RuleCondition(reactionCondition, reactionConditionParameter),
					new RuleCondition(reactionCondition2, reactionConditionParameter2));
		}
		rule.priority = 0;
		return rule;
	}

	/** V0.1 compatibility: callers asking for one rule receive the active rule. */
	public RuleDefinition buildRule() { return buildActiveRule(); }

	private static int defaultPower(RuleEffect.Type type) {
		switch (type) {
			case FIRE: return 5;
			case CREATE_GAS: return 3;
			case HEAL:
			case SHIELD: return 3;
			default: return 2;
		}
	}

	public int usedCapacity() {
		if (classBuild != null) return classBuild.usedBudget();
		int result = law.capacityCost;
		result += vocabulary1.capacityCost + vocabulary2.capacityCost;
		for (RuleDefinition rule : buildRules()) result += rule.capacityCost();
		return result;
	}

	public int maxCapacity() {
		if (classBuild != null) return classBuild.maxBudget();
		return ClassBudgetPolicy.newBuildBudget() + restriction.capacityBonus;
	}

	public boolean valid() {
		if (classBuild != null) return classBuild.valid();
		if (name == null || name.trim().isEmpty() || reactionTrigger == RuleEvent.ACTIVE) return false;
		if (!CoreRuleVocabulary.validSelection(this)) return false;
		if (!new RuleModifier(activeModifier).compatible(activeEffect)
				|| !new RuleModifier(reactionModifier).compatible(reactionEffect)) return false;
		if (!RuleTarget.availableForEvent(activeTarget, RuleEvent.ACTIVE)
				|| !RuleTarget.availableForEvent(reactionTarget, reactionTrigger)) return false;
		if (!RuleEffect.compatibleTarget(activeEffect, activeTarget)
				|| !RuleEffect.compatibleTarget(reactionEffect, reactionTarget)) return false;
		return usedCapacity() <= maxCapacity();
	}

	public String summary() {
		return CustomClassSummaryFormatter.buildSheet(this);
	}

	public ClassBuild toClassBuild() {
		return classBuild == null ? ClassBuildMigrator.fromLegacy(this) : classBuild.copy();
	}

	boolean migrationOnlyModel() { return classBuild == null; }

	public static CustomClassConfig fromClassBuild(ClassBuild build) {
		CustomClassConfig result = new CustomClassConfig();
		result.classBuild = build.copy();
		result.name = build.name;
		return result;
	}

	public static void setPending(CustomClassConfig config) { pending = config; }
	public static CustomClassConfig consumePending() {
		CustomClassConfig result = pending;
		pending = null;
		return result;
	}
	public static void clearPending() { pending = null; }

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("class_build_schema_version", ClassBuild.SCHEMA_VERSION);
		bundle.put("class_build", toClassBuild());
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		if (bundle.contains("class_build")) {
			classBuild = (ClassBuild)bundle.get("class_build");
			name = classBuild.name;
			return;
		}
		name = bundle.getString("name");
		resource = bundle.getEnum("resource", ResourceEngine.class);
		if (!bundle.contains("law")) {
			// A V0.1 pending blueprint was never part of a run save, but preserving it is cheap.
			law = ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD;
			trigger = bundle.getEnum("trigger", RuleEvent.class);
			effect = bundle.getEnum("effect", RuleEffect.Type.class);
			activeEffect = effect;
			if (trigger != RuleEvent.ACTIVE) reactionTrigger = trigger;
		} else {
			law = bundle.getEnum("law", ClassLaw.class);
			vocabulary1 = bundle.contains("vocabulary_1")
					? bundle.getEnum("vocabulary_1", CoreRuleVocabulary.class) : CoreRuleVocabulary.NONE;
			vocabulary2 = bundle.contains("vocabulary_2")
					? bundle.getEnum("vocabulary_2", CoreRuleVocabulary.class) : CoreRuleVocabulary.NONE;
			activeCondition = bundle.getEnum("active_condition", RuleCondition.Type.class);
			activeConditionParameter = bundle.getInt("active_condition_parameter");
			activeTarget = bundle.getEnum("active_target", RuleTarget.Type.class);
			activeEffect = bundle.getEnum("active_effect", RuleEffect.Type.class);
			activeModifier = bundle.getEnum("active_modifier", RuleModifier.Type.class);
			reactionTrigger = bundle.getEnum("reaction_trigger", RuleEvent.class);
			reactionCondition = bundle.getEnum("reaction_condition", RuleCondition.Type.class);
			reactionConditionParameter = bundle.getInt("reaction_condition_parameter");
			reactionCondition2 = bundle.getEnum("reaction_condition_2", RuleCondition.Type.class);
			reactionConditionParameter2 = bundle.getInt("reaction_condition_parameter_2");
			reactionTarget = bundle.getEnum("reaction_target", RuleTarget.Type.class);
			reactionEffect = bundle.getEnum("reaction_effect", RuleEffect.Type.class);
			reactionModifier = bundle.getEnum("reaction_modifier", RuleModifier.Type.class);
		}
		restriction = bundle.getEnum("restriction", Restriction.class);
		classBuild = ClassBuildMigrator.fromLegacy(this);
	}

}
