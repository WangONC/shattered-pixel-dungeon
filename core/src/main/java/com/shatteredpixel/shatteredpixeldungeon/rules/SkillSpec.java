package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;
import java.util.Collection;

/** Unified active/reactive Skill blueprint compiled into one existing RuleDefinition instance. */
public class SkillSpec implements Bundlable {
	public String id = "skill";
	/** Player-authored name; id remains the stable runtime/save identity. */
	public String name = "";
	public RuleEvent activation = RuleEvent.ACTIVE;
	public final ArrayList<RuleCondition> conditions = new ArrayList<>();
	public EffectSpec primary = new EffectSpec(EffectFamily.MOVEMENT, EffectSpec.Operation.MOVE_PUSH, 2);
	public EffectSpec secondary;
	public SkillDelivery delivery = SkillDelivery.DIRECT_TARGET;
	public TargetingSpec targeting = new TargetingSpec(RuleTarget.Type.SELECTED_TARGET);
	public RuleModifier modifier = new RuleModifier();
	public RuleCost cost = new RuleCost(RuleCost.Type.NONE, 0);
	public SkillConstraint constraint = new SkillConstraint();
	public int priority;
	public String requiredCarrier = "";
	public RuleEvent attachmentEvent = RuleEvent.ON_HIT;
	public int attachmentCharges = 1;

	public SkillSpec() { conditions.add(new RuleCondition()); }

	public SkillSpec copy() {
		SkillSpec result = new SkillSpec();
		result.id = id;
		result.name = name;
		result.activation = activation;
		result.conditions.clear();
		for (RuleCondition value : conditions) result.conditions.add(copyCondition(value));
		result.primary = primary == null ? null : primary.copy();
		result.secondary = secondary == null ? null : secondary.copy();
		result.delivery = delivery;
		result.targeting = targeting == null ? null : targeting.copy();
		result.modifier = modifier == null ? null : modifier.copy();
		result.cost = copyCost(cost);
		result.constraint = constraint == null ? null : constraint.copy();
		result.priority = priority;
		result.requiredCarrier = requiredCarrier;
		result.attachmentEvent = attachmentEvent;
		result.attachmentCharges = attachmentCharges;
		return result;
	}

	public RuleDefinition compile() {
		RuleDefinition rule = new RuleDefinition();
		rule.id = id;
		rule.trigger = new RuleTrigger(activation);
		rule.setConditions();
		for (RuleCondition value : conditions) {
			if (value != null && value.type != RuleCondition.Type.ALWAYS) rule.conditions.add(copyCondition(value));
		}
		if (rule.conditions.isEmpty()) rule.conditions.add(new RuleCondition());
		rule.cost = copyCost(cost);
		rule.target = new RuleTarget(targeting.compileType(activation));
		rule.effect = primary.compile();
		rule.secondaryEffect = secondary == null ? null : secondary.compile();
		rule.delivery = delivery;
		rule.targetingSpec = targeting.copy();
		rule.effectSpec = primary.copy();
		rule.secondaryEffectSpec = secondary == null ? null : secondary.copy();
		rule.attachmentEvent = attachmentEvent;
		rule.attachmentCharges = attachmentCharges;
		rule.modifier = modifier.copy();
		if (cost.type == RuleCost.Type.COOLDOWN) rule.modifier.cooldown = Math.max(rule.modifier.cooldown, cost.amount);
		if ((targeting.coverage == TargetingSpec.Coverage.RADIUS
				|| targeting.coverage == TargetingSpec.Coverage.ADJACENT)
				&& rule.modifier.type == RuleModifier.Type.NONE) {
			rule.modifier = new RuleModifier(RuleModifier.Type.AREA);
			rule.modifier.magnitude = Math.max(1, targeting.magnitude);
		}
		rule.priority = priority;
		if (constraint != null) constraint.applyTo(rule);
		return rule;
	}

	public boolean structurallyValid() {
		if (activation == null || primary == null || !primary.configurationValid() || delivery == null
				|| !delivery.implemented() || targeting == null || !targeting.implemented(activation)
				|| modifier == null || cost == null || constraint == null || !constraint.implemented()) return false;
		RuleDefinition compiled = compile();
		return (cost.type != RuleCost.Type.ACTION || activation == RuleEvent.ACTIVE)
				&& cost.referenceValid()
				&& primary.compatibleTargeting(targeting)
				&& (secondary == null || secondary.compatibleTargeting(targeting))
				&& (constraint.variant != SkillConstraint.Variant.HP_COMMITMENT
						|| cost.type == RuleCost.Type.HP)
				&& !(constraint.variant == SkillConstraint.Variant.COOLDOWN
						&& cost.type == RuleCost.Type.COOLDOWN)
				&& compiled.modifier.compatible(primary, delivery)
				&& DeliveryRegistry.parametersValid(this)
				&& (delivery != SkillDelivery.SELF || targeting.selector == TargetingSpec.Selector.SELF)
				&& (delivery != SkillDelivery.GROUND_PLACEMENT
						|| targeting.selector == TargetingSpec.Selector.SELECTED_CELL
						|| targeting.selector == TargetingSpec.Selector.SELF);
	}

	public int nominalPowerCost() {
		if (activation == null || primary == null || delivery == null || targeting == null
				|| modifier == null || cost == null || constraint == null) return 999;
		int result = new RuleTrigger(activation).capacityCost() + delivery.powerCost()
				+ targeting.powerCost() + primary.powerCost() + modifier.capacityCost();
		if (delivery == SkillDelivery.PERSISTENT_CARRIER) {
			result += Math.max(1, primary.lifetime / 6) + (primary.period <= 1 ? 1 : 0);
		}
		if (delivery == SkillDelivery.ACTION_ATTACHMENT) result += Math.max(0, attachmentCharges - 1);
		if (modifier.type == RuleModifier.Type.PIERCE && targeting.range > 6) result++;
		if (secondary != null) result += Math.max(2, secondary.powerCost());
		for (RuleCondition condition : conditions) if (condition != null) result += condition.capacityCost();
		return Math.max(0, result - cost.effectiveBudgetRebate(primary));
	}

	public static SkillSpec fromRule(RuleDefinition rule, ResourceEngine engine) {
		SkillSpec result = new SkillSpec();
		result.id = rule.id;
		result.name = "";
		result.activation = rule.trigger.event;
		result.conditions.clear();
		for (RuleCondition value : rule.conditions) result.conditions.add(copyCondition(value));
		result.primary = EffectSpec.fromLegacy(rule.effect.type, rule.effect.power,
				rule.target == null ? null : rule.target.type);
		result.secondary = rule.secondaryEffect == null ? null
				: EffectSpec.fromLegacy(rule.secondaryEffect.type, rule.secondaryEffect.power,
					rule.target == null ? null : rule.target.type);
		result.targeting = new TargetingSpec(rule.target.type);
		result.delivery = rule.target.type == RuleTarget.Type.SELECTED_CELL ? SkillDelivery.GROUND_PLACEMENT
				: rule.target.type == RuleTarget.Type.SELF ? SkillDelivery.SELF : SkillDelivery.DIRECT_TARGET;
		result.modifier = rule.modifier.copy();
		result.cost = copyCost(rule.cost);
		if (result.cost.type == RuleCost.Type.RESOURCE && result.cost.resourceEngine == null) {
			result.cost.resourceEngine = engine;
			result.cost.resourceId = engine == null ? "" : engine.name().toLowerCase();
		}
		result.priority = rule.priority;
		if (rule.effectSpec != null) result.primary = rule.effectSpec.copy();
		if (rule.secondaryEffectSpec != null) result.secondary = rule.secondaryEffectSpec.copy();
		if (rule.delivery != null) result.delivery = rule.delivery;
		if (rule.targetingSpec != null) result.targeting = rule.targetingSpec.copy();
		result.attachmentEvent = rule.attachmentEvent;
		result.attachmentCharges = rule.attachmentCharges;
		return result;
	}

	private static RuleCondition copyCondition(RuleCondition value) {
		if (value instanceof RuleMarkCondition) {
			RuleMarkCondition old = (RuleMarkCondition)value;
			RuleMarkCondition copy = new RuleMarkCondition(old.mark, old.minimumStacks);
			copy.requireOwnerSource = old.requireOwnerSource;
			return copy;
		}
		RuleCondition copy = new RuleCondition(value == null ? RuleCondition.Type.ALWAYS : value.type,
				value == null ? 0 : value.parameter);
		if (value != null) copy.reference = value.reference;
		return copy;
	}

	private static RuleCost copyCost(RuleCost value) {
		RuleCost result = new RuleCost(value == null ? RuleCost.Type.NONE : value.type,
				value == null ? 0 : value.amount);
		if (value != null) {
			result.resourceId = value.resourceId;
			result.resourceEngine = value.resourceEngine;
			result.reference = value.reference;
		}
		return result;
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("id", id);
		bundle.put("name", name);
		bundle.put("activation", activation);
		bundle.put("conditions", conditions);
		bundle.put("primary", primary);
		if (secondary != null) bundle.put("secondary", secondary);
		bundle.put("delivery", delivery);
		bundle.put("targeting", targeting);
		bundle.put("modifier", modifier);
		bundle.put("cost", cost);
		bundle.put("constraint", constraint);
		bundle.put("priority", priority);
		bundle.put("required_carrier", requiredCarrier);
		bundle.put("attachment_event", attachmentEvent);
		bundle.put("attachment_charges", attachmentCharges);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		name = bundle.contains("name") ? bundle.getString("name") : "";
		activation = bundle.getEnum("activation", RuleEvent.class);
		conditions.clear();
		if (bundle.contains("conditions")) {
			Collection<Bundlable> values = bundle.getCollection("conditions");
			for (Bundlable value : values) if (value instanceof RuleCondition) conditions.add((RuleCondition)value);
		}
		if (conditions.isEmpty()) conditions.add(new RuleCondition());
		primary = (EffectSpec)bundle.get("primary");
		secondary = bundle.contains("secondary") ? (EffectSpec)bundle.get("secondary") : null;
		delivery = bundle.getEnum("delivery", SkillDelivery.class);
		targeting = (TargetingSpec)bundle.get("targeting");
		modifier = (RuleModifier)bundle.get("modifier");
		cost = (RuleCost)bundle.get("cost");
		constraint = (SkillConstraint)bundle.get("constraint");
		priority = bundle.getInt("priority");
		requiredCarrier = bundle.getString("required_carrier");
		boolean explicitAttachment = bundle.contains("attachment_event");
		attachmentEvent = explicitAttachment ? bundle.getEnum("attachment_event", RuleEvent.class) : RuleEvent.ON_HIT;
		attachmentCharges = bundle.contains("attachment_charges") ? Math.max(1,bundle.getInt("attachment_charges")) : 1;
		// Pre-FULL-RUNTIME saves used ACTION_ATTACHMENT as a generic reactive placeholder.
		if (!explicitAttachment && delivery == SkillDelivery.ACTION_ATTACHMENT && activation != RuleEvent.ACTIVE) {
			delivery = targeting.selector == TargetingSpec.Selector.SELF ? SkillDelivery.SELF
					: targeting.selector == TargetingSpec.Selector.SELECTED_CELL ? SkillDelivery.GROUND_PLACEMENT
					: SkillDelivery.DIRECT_TARGET;
		}
	}
}
