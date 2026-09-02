package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

/**
 * One installed rule instance. Module fields are its saved blueprint; cooldown/count/order are
 * independent runtime state, so two equivalent rules never share state.
 */
public class RuleDefinition implements Bundlable {
	public String id = "rule";
	public RuleTrigger trigger = new RuleTrigger();
	public final ArrayList<RuleCondition> conditions = new ArrayList<>();
	public RuleCost cost = new RuleCost();
	public RuleTarget target = new RuleTarget();
	public RuleEffect effect = new RuleEffect();
	/** Optional second effect paid and triggered as part of the same Skill execution. */
	public RuleEffect secondaryEffect;
	public RuleModifier modifier = new RuleModifier();
	/** Additive V0.1 Skill runtime data. Legacy target/effect remain the old-save adapter. */
	public SkillDelivery delivery;
	public TargetingSpec targetingSpec;
	public EffectSpec effectSpec;
	public EffectSpec secondaryEffectSpec;
	public RuleEvent attachmentEvent = RuleEvent.ON_HIT;
	public int attachmentCharges = 1;
	public int priority;
	private int runtimeOrder = -1;
	private float cooldownRemaining;
	private int triggerCount;
	private int delayTurns;
	private int useLimit;

	public RuleDefinition() {
		conditions.add(new RuleCondition());
	}

	public boolean matches(RuleEvent event) {
		return trigger.event == event;
	}

	public boolean execute(RuleRuntime runtime, RuleContext context) {
		if (!matches(context.event) || cooldownRemaining > 0
				|| useLimit > 0 && triggerCount >= useLimit || !modifier.compatible(effect.type)) return false;
		// DEV/QA integrations predating SkillSpec legitimately replace the public legacy modules on an
		// installed rule.  Do not let the compiled SkillSpec mirror silently override that replacement.
		// A normally compiled Skill remains on the structured path because both representations match.
		boolean structuredRuntime = structuredRuntimeCurrent();
		RuleTrace.record("RULE", id + " event=" + context.event + " order=" + runtimeOrder);
		if (!cost.canPay(runtime, context.hero) || !runtime.activeSurchargeCanPay(context, cost)) {
			RuleTrace.record("COST", id + " payable=false type=" + cost.type + " amount=" + cost.amount);
			return false;
		}
		RuleModifier executionModifier = runtime.shapeModifier(effect, modifier, context.hero);
		if (structuredRuntime && delivery == SkillDelivery.ACTION_ATTACHMENT) {
			boolean installed = com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleActionAttachment.install(
					context.hero, effectSpec == null ? new EffectSpec(effect.type,effect.power) : effectSpec,
					secondaryEffectSpec,
					attachmentEvent, attachmentCharges, id);
			if (!installed) return false;
			cost.pay(runtime, context.hero); runtime.payActiveSurcharge(context);
			cooldownRemaining=modifier.cooldown; triggerCount++;
			RuleTrace.record("ATTACHMENT","install rule="+id+" event="+attachmentEvent+" charges="+attachmentCharges);
			return true;
		}

		ArrayList<Char> resolvedChars = target.resolveChars(context);
		ArrayList<Integer> resolvedCells = !structuredRuntime || delivery == null || targetingSpec == null
				? target.resolveCells(context, resolvedChars)
				: SkillTargetResolver.resolve(context, delivery, targetingSpec, executionModifier);
		if (structuredRuntime && delivery != null && targetingSpec != null) {
			resolvedChars.clear();
			for (int cell : resolvedCells) {
				Char ch = cell < 0 ? null : Actor.findChar(cell);
				if (ch != null && !resolvedChars.contains(ch)) resolvedChars.add(ch);
			}
		}
		if (executionModifier.radius() > 0) resolvedCells = expandArea(resolvedCells, executionModifier.radius());
		if (resolvedCells.isEmpty() && resolvedChars.isEmpty()) resolvedCells.add(-1);

		boolean applied = false;
		boolean attempted = false;
		boolean activeCostPaid = false;
		Char echoTarget = null;
		int echoCell = -1;
		RuleContext effectContext = context.causedByRule(id);
		int repetitions = executionModifier.repeats();
		for (int repeat = 0; repeat < repetitions; repeat++) {
			if (!resolvedCells.isEmpty()) {
				for (int cell : resolvedCells) {
					Char resolved = originalAt(resolvedChars, cell);
					if (resolved == null && cell >= 0) resolved = Actor.findChar(cell);
					if (!conditionsPass(runtime, context, resolved)) continue;
					attempted = true;
					if (context.event == RuleEvent.ACTIVE && !activeCostPaid) {
						commitCost(runtime, context);
						activeCostPaid = true;
					}
					if (applyOrSchedule(runtime, effectContext, resolved, cell, executionModifier, structuredRuntime)) {
						if (!applied) { echoTarget = resolved; echoCell = cell; }
						applied = true;
						runtime.onPrimaryEffectSuccess(effectContext, resolved, cell);
					}
				}
			} else {
				for (Char resolved : resolvedChars) {
					if (!conditionsPass(runtime, context, resolved)) continue;
					attempted = true;
					if (context.event == RuleEvent.ACTIVE && !activeCostPaid) {
						commitCost(runtime, context);
						activeCostPaid = true;
					}
					int cell = resolved == null ? -1 : resolved.pos;
					if (applyOrSchedule(runtime, effectContext, resolved, cell, executionModifier, structuredRuntime)) {
						if (!applied) { echoTarget = resolved; echoCell = cell; }
						applied = true;
						runtime.onPrimaryEffectSuccess(effectContext, resolved, cell);
					}
				}
			}
		}
		boolean primaryApplied = applied;
		if (!applied && attempted) applied = runtime.compensateFailure(effectContext);

		if (!applied) {
			RuleTrace.record("EFFECT", id + " applied=false effect=" + effect.semanticId());
			// An ACTIVE Skill has already been fired once its legal execution began.
			// A miss or resisted/no-op effect does not refund its ordinary Cost or action.
			return activeCostPaid;
		}
		if (!activeCostPaid) commitCost(runtime, context);
		if (primaryApplied) runtime.scheduleEcho(this, effectContext, echoTarget, echoCell);
		RuleTrace.record("EFFECT", id + " applied=true effect=" + effect.semanticId()
				+ " target=" + target.type + " count=" + triggerCount);
		return true;
	}

	private void commitCost(RuleRuntime runtime, RuleContext context) {
		RuleTrace.record("COST", id + " payable=true type=" + cost.type + " amount=" + cost.amount);
		cost.pay(runtime, context.hero);
		runtime.payActiveSurcharge(context);
		cooldownRemaining = modifier.cooldown;
		triggerCount++;
	}

	private boolean applySecondary(RuleRuntime runtime, RuleContext context, Char target, int cell,
			boolean structuredRuntime) {
		if (secondaryEffect == null || context == null) return false;
		if (target == null && cell >= 0) target = Actor.findChar(cell);
		// The RuleRuntime executing-rule guard remains active while the secondary effect emits events,
		// so it cannot recursively re-enter its owning Skill.
		return !structuredRuntime || secondaryEffectSpec == null
				? secondaryEffect.apply(context, target, cell, new RuleModifier())
				: SkillEffectRuntime.apply(secondaryEffectSpec, runtime, context, target, cell, new RuleModifier());
	}

	private boolean applyOrSchedule(RuleRuntime runtime, RuleContext context, Char resolved, int cell,
			RuleModifier executionModifier, boolean structuredRuntime) {
		RuleModifier effective = runtime.empowerModifier(effect, executionModifier, context.hero);
		if (structuredRuntime && delivery == SkillDelivery.PERSISTENT_CARRIER) {
			EffectSpec payload = effectSpec == null ? new EffectSpec(effect.type, effect.power) : effectSpec;
			return SkillEffectRuntime.createPersistentCarrier(context, payload, secondaryEffectSpec, cell,
					Math.max(3, payload.lifetime), Math.max(1, payload.period),
					targetingSpec == null ? TargetingSpec.Filter.ENEMY : targetingSpec.filter);
		}
		int effectiveDelay = Math.max(delayTurns, executionModifier.delayTurns());
		boolean applied;
		if (effectiveDelay > 0) {
			applied = runtime.scheduleDelayed(context, structuredRuntime ? effectSpec : null, effect,
					resolved, cell, effective, effectiveDelay);
			if (applied && secondaryEffect != null) {
				boolean secondaryScheduled = runtime.scheduleDelayed(context,
						structuredRuntime ? secondaryEffectSpec : null, secondaryEffect,
						resolved, cell, new RuleModifier(), effectiveDelay);
				RuleTrace.record("SECONDARY", id + " effect=" + secondaryEffect.semanticId()
						+ " scheduled=" + secondaryScheduled + " delay=" + effectiveDelay
						+ " cause=#" + context.eventId());
			}
		} else {
			applied = !structuredRuntime || effectSpec == null ? effect.apply(context, resolved, cell, effective)
					: SkillEffectRuntime.apply(effectSpec, runtime, context, resolved, cell, effective);
			if (applied && secondaryEffect != null) {
				boolean secondaryApplied = applySecondary(runtime, context, resolved, cell, structuredRuntime);
				RuleTrace.record("SECONDARY", id + " effect=" + secondaryEffect.semanticId()
						+ " applied=" + secondaryApplied + " cause=#" + context.eventId());
			}
		}
		if (applied) runtime.consumeEmpowerment(effect, context.hero);
		return applied;
	}

	private boolean structuredRuntimeCurrent() {
		if (delivery == null || targetingSpec == null || effectSpec == null || effect == null) return false;
		if (effectSpec.operation != EffectSpec.Operation.LEGACY) return true;
		return effectSpec.variant == effect.type && effectSpec.power == effect.power
				&& !(effect.getClass() != RuleEffect.class && effectSpec.compile().getClass() != effect.getClass());
	}

	private boolean conditionsPass(RuleRuntime runtime, RuleContext context, Char resolved) {
		for (RuleCondition condition : conditions) {
			if (condition != null) {
				boolean passed = condition.passes(runtime, context, resolved);
				RuleTrace.record("CONDITION", id + " " + condition.semanticId() + "=" + passed);
				if (!passed) return false;
			}
		}
		return true;
	}

	private static Char originalAt(ArrayList<Char> chars, int cell) {
		for (Char ch : chars) if (ch != null && ch.pos == cell) return ch;
		return null;
	}

	private static ArrayList<Integer> expandArea(ArrayList<Integer> centers, int radius) {
		ArrayList<Integer> result = new ArrayList<>();
		if (Dungeon.level == null) return centers;
		for (int center : centers) {
			if (center < 0) continue;
			for (int cell = 0; cell < Dungeon.level.length(); cell++) {
				if (Dungeon.level.distance(center, cell) <= radius && !result.contains(cell)) result.add(cell);
			}
		}
		return result;
	}

	public void setConditions(RuleCondition... values) {
		conditions.clear();
		if (values != null) {
			for (RuleCondition value : values) if (value != null) conditions.add(value);
		}
		if (conditions.isEmpty()) conditions.add(new RuleCondition());
	}

	public void tick() {
		cooldownRemaining = Math.max(0, cooldownRemaining - 1f);
	}

	public void accelerate(float turns) {
		cooldownRemaining = Math.max(0, cooldownRemaining - Math.max(0, turns));
	}

	public int capacityCost() {
		int result = trigger.capacityCost() + target.capacityCost() + effect.capacityCost() + modifier.capacityCost();
		if (secondaryEffect != null) result += Math.max(1, secondaryEffect.capacityCost() - 1);
		for (RuleCondition condition : conditions) result += condition.capacityCost();
		return Math.max(0, result - cost.budgetRebate());
	}

	public int triggerCount() { return triggerCount; }
	public float cooldownRemaining() { return cooldownRemaining; }
	public int runtimeOrder() { return runtimeOrder; }
	public int delayTurns() { return delayTurns; }
	public void setDelayTurns(int turns) { delayTurns = Math.max(0, turns); }
	public int useLimit() { return useLimit; }
	public int usesRemaining() { return useLimit <= 0 ? Integer.MAX_VALUE : Math.max(0, useLimit-triggerCount); }
	public void setUseLimit(int value) { useLimit = Math.max(0, value); }
	public EnumSet<RuleSemanticTag> tags() { return effect.tags(); }
	public boolean hasTag(RuleSemanticTag tag) { return effect.hasTag(tag); }
	void assignRuntimeOrder(int value) { if (runtimeOrder < 0) runtimeOrder = value; }

	public String description() { return description(null); }

	public String description(ResourceEngine engine) {
		return RuleSemanticFormatter.techniqueDescription(this, engine);
	}

	/** Internal structural dump used only by DEV RULE LAB. */
	public String debugDescription(ResourceEngine engine) {
		StringBuilder conditionText = new StringBuilder();
		for (RuleCondition condition : conditions) {
			if (condition.type == RuleCondition.Type.ALWAYS) continue;
			if (conditionText.length() > 0) conditionText.append(Messages.get(RuleDefinition.class, "and"));
			conditionText.append(condition.description());
		}
		if (conditionText.length() == 0) conditionText.append(Messages.get(RuleDefinition.class, "always"));
		return Messages.get(RuleDefinition.class, "description", trigger.description(), conditionText,
				cost.description(engine), target.description(), effect.description(), modifier.description());
	}

	public static RuleDefinition create(RuleEvent event, ResourceEngine engine, RuleEffect.Type effectType) {
		RuleDefinition rule = new RuleDefinition();
		rule.id = event.name().toLowerCase() + "_" + effectType.name().toLowerCase();
		rule.trigger = new RuleTrigger(event);
		rule.setConditions(new RuleCondition(RuleCondition.Type.ALWAYS));
		int amount = event == RuleEvent.ACTIVE ? 3 : 2;
		rule.cost = engine == ResourceEngine.BLOOD
				? new RuleCost(RuleCost.Type.HP, amount)
				: new RuleCost(RuleCost.Type.RESOURCE, amount);
		rule.effect = new RuleEffect(effectType, effectType == RuleEffect.Type.FIRE ? 5 : 2);
		rule.modifier = new RuleModifier();
		if (event == RuleEvent.ON_DAMAGED) {
			rule.target = new RuleTarget(RuleTarget.Type.ATTACKER);
		} else if (event == RuleEvent.ACTIVE) {
			rule.target = new RuleTarget(RuleTarget.Type.SELECTED_CELL);
		} else if (event == RuleEvent.ON_HIT || event == RuleEvent.ON_KILL) {
			rule.target = new RuleTarget(RuleTarget.Type.HIT_TARGET);
		} else {
			rule.target = new RuleTarget(RuleTarget.Type.SELF);
		}
		return rule;
	}

	public static RuleDefinition ragePushOnDamaged() {
		return create(RuleEvent.ON_DAMAGED, ResourceEngine.RAGE, RuleEffect.Type.PUSH);
	}

	public static RuleDefinition bloodFireActive() {
		return create(RuleEvent.ACTIVE, ResourceEngine.BLOOD, RuleEffect.Type.FIRE);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("id", id);
		bundle.put("trigger", trigger);
		bundle.put("conditions", conditions);
		bundle.put("cost", cost);
		bundle.put("target", target);
		bundle.put("effect", effect);
		if (secondaryEffect != null) bundle.put("secondary_effect", secondaryEffect);
		bundle.put("modifier", modifier);
		if (delivery != null) bundle.put("skill_delivery", delivery);
		if (targetingSpec != null) bundle.put("targeting_spec", targetingSpec);
		if (effectSpec != null) bundle.put("effect_spec", effectSpec);
		if (secondaryEffectSpec != null) bundle.put("secondary_effect_spec", secondaryEffectSpec);
		bundle.put("attachment_event", attachmentEvent);
		bundle.put("attachment_charges", attachmentCharges);
		bundle.put("priority", priority);
		bundle.put("order", runtimeOrder);
		bundle.put("cooldown", cooldownRemaining);
		bundle.put("count", triggerCount);
		bundle.put("delay_turns", delayTurns);
		bundle.put("use_limit", useLimit);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		trigger = (RuleTrigger) bundle.get("trigger");
		conditions.clear();
		if (bundle.contains("conditions")) {
			Collection<Bundlable> stored = bundle.getCollection("conditions");
			for (Bundlable value : stored) if (value instanceof RuleCondition) conditions.add((RuleCondition) value);
		} else if (bundle.contains("condition")) {
			Bundlable old = bundle.get("condition");
			if (old instanceof RuleCondition) conditions.add((RuleCondition) old);
		}
		if (conditions.isEmpty()) conditions.add(new RuleCondition());
		cost = (RuleCost) bundle.get("cost");
		target = (RuleTarget) bundle.get("target");
		effect = (RuleEffect) bundle.get("effect");
		secondaryEffect = bundle.contains("secondary_effect")
				? (RuleEffect)bundle.get("secondary_effect") : null;
		modifier = (RuleModifier) bundle.get("modifier");
		delivery = bundle.contains("skill_delivery") ? bundle.getEnum("skill_delivery", SkillDelivery.class) : null;
		targetingSpec = bundle.contains("targeting_spec") ? (TargetingSpec)bundle.get("targeting_spec") : null;
		effectSpec = bundle.contains("effect_spec") ? (EffectSpec)bundle.get("effect_spec") : null;
		secondaryEffectSpec = bundle.contains("secondary_effect_spec")
				? (EffectSpec)bundle.get("secondary_effect_spec") : null;
		attachmentEvent = bundle.contains("attachment_event") ? bundle.getEnum("attachment_event",RuleEvent.class) : RuleEvent.ON_HIT;
		attachmentCharges = bundle.contains("attachment_charges") ? Math.max(1,bundle.getInt("attachment_charges")) : 1;
		priority = bundle.getInt("priority");
		runtimeOrder = bundle.contains("order") ? bundle.getInt("order") : -1;
		cooldownRemaining = bundle.getFloat("cooldown");
		triggerCount = bundle.getInt("count");
		delayTurns = Math.max(0, bundle.getInt("delay_turns"));
		useLimit = bundle.contains("use_limit") ? Math.max(0, bundle.getInt("use_limit")) : 0;
	}
}
