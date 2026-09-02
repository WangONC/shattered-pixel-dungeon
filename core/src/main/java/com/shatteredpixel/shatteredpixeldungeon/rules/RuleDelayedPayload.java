package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.EnumSet;

/** A saved Rule Effect payload whose deadline is executed by the real Actor scheduler. */
public class RuleDelayedPayload extends Actor {
	public enum Kind { NORMAL, ECHO }

	private long payloadId;
	private Kind kind = Kind.NORMAL;
	private int remainingTurns;
	private int targetId = -1;
	private int targetCell = -1;
	private int sourceId = -1;
	private RuleEffect effect = new RuleEffect();
	private EffectSpec effectSpec;
	private RuleModifier modifier = new RuleModifier();
	private RuleEvent causeEvent = RuleEvent.ACTIVE;
	private RuleEvent originalEvent = RuleEvent.ACTIVE;
	private long causeEventId;
	private long parentEventId;
	private long rootEventId;
	private String sourceRuleId;
	private EnumSet<RuleEvent> eventChain = EnumSet.of(RuleEvent.ACTIVE);
	private int amount;
	private boolean melee;
	private transient RuleRuntime runtime;

	{
		actPriority = BLOB_PRIO + 2;
	}

	public RuleDelayedPayload() {}

	RuleDelayedPayload(long payloadId, RuleContext context, RuleEffect effect,
			Char target, int cell, RuleModifier modifier, int turns) {
		this(payloadId, context, effect, target, cell, modifier, turns, Kind.NORMAL);
	}

	RuleDelayedPayload(long payloadId, RuleContext context, EffectSpec effectSpec, RuleEffect legacyEffect,
			Char target, int cell, RuleModifier modifier, int turns, Kind kind) {
		this(payloadId, context, legacyEffect, target, cell, modifier, turns, kind);
		this.effectSpec = effectSpec == null ? null : effectSpec.copy();
	}

	RuleDelayedPayload(long payloadId, RuleContext context, RuleEffect effect,
			Char target, int cell, RuleModifier modifier, int turns, Kind kind) {
		this.payloadId = payloadId;
		this.kind = kind == null ? Kind.NORMAL : kind;
		this.remainingTurns = Math.max(1, turns);
		this.targetId = target == null ? -1 : target.id();
		this.targetCell = cell;
		this.sourceId = context.source == null ? context.hero.id() : context.source.id();
		this.effect = effect.copy();
		this.modifier = modifier.copy();
		this.causeEvent = context.event;
		this.originalEvent = context.originalEvent();
		this.causeEventId = context.eventId();
		this.parentEventId = context.parentEventId();
		this.rootEventId = context.rootEventId();
		this.sourceRuleId = context.sourceRuleId();
		this.eventChain = context.eventChain();
		this.amount = context.amount;
		this.melee = context.melee;
	}

	void activate(RuleRuntime runtime) {
		this.runtime = runtime;
		if (!Actor.all().contains(this)) Actor.addDelayed(this, remainingTurns);
	}

	public long payloadId() { return payloadId; }
	public String effectId() { return effect.semanticId(); }
	public Kind kind() { return kind; }

	public int remainingTurns() {
		return Actor.all().contains(this)
				? Math.max(0, (int)Math.ceil(cooldown())) : Math.max(0, remainingTurns);
	}

	@Override
	protected boolean act() {
		Hero hero = Dungeon.hero;
		RuleTrace.record("DELAY", "fire payload=#" + payloadId + " kind=" + kind + " cause=#" + causeEventId
				+ " root=#" + rootEventId + " effect=" + effect.semanticId());
		try {
			if (runtime == null || hero == null || hero.ruleRuntime() != runtime) return true;
			Actor foundTarget = targetId < 0 ? null : Actor.findById(targetId);
			Char target = foundTarget instanceof Char ? (Char)foundTarget : null;
			if (target == null && targetCell >= 0) target = Actor.findChar(targetCell);
			Actor foundSource = sourceId < 0 ? null : Actor.findById(sourceId);
			RuleContext context = RuleContext.restoreProvenance(causeEvent, hero, originalEvent,
					causeEventId, parentEventId, rootEventId, sourceRuleId, eventChain);
			context.source = foundSource instanceof Char ? (Char)foundSource : hero;
			context.target = target;
			context.cell = target == null ? targetCell : target.pos;
			context.amount = amount;
			context.melee = melee;
			boolean applied = effectSpec == null ? effect.apply(context, target, context.cell, modifier)
					: SkillEffectRuntime.apply(effectSpec, runtime, context, target, context.cell, modifier);
			RuleTrace.record("DELAY", "complete payload=#" + payloadId + " applied=" + applied);
			return true;
		} finally {
			if (runtime != null) runtime.completeDelayed(this);
			else Actor.remove(this);
		}
	}

	private static final String PAYLOAD_ID = "payload_id";
	private static final String KIND = "kind";
	private static final String REMAINING = "remaining";
	private static final String TARGET_ID = "target_id";
	private static final String TARGET_CELL = "target_cell";
	private static final String SOURCE_ID = "source_id";
	private static final String EFFECT = "effect";
	private static final String MODIFIER = "modifier";
	private static final String CAUSE_EVENT = "cause_event";
	private static final String ORIGINAL_EVENT = "original_event";
	private static final String CAUSE_ID = "cause_id";
	private static final String PARENT_ID = "parent_id";
	private static final String ROOT_ID = "root_id";
	private static final String SOURCE_RULE = "source_rule";
	private static final String EVENT_CHAIN = "event_chain";

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(PAYLOAD_ID, payloadId);
		bundle.put(KIND, kind);
		bundle.put(REMAINING, remainingTurns());
		bundle.put(TARGET_ID, targetId);
		bundle.put(TARGET_CELL, targetCell);
		bundle.put(SOURCE_ID, sourceId);
		bundle.put(EFFECT, effect);
		if (effectSpec != null) bundle.put("effect_spec", effectSpec);
		bundle.put(MODIFIER, modifier);
		bundle.put(CAUSE_EVENT, causeEvent);
		bundle.put(ORIGINAL_EVENT, originalEvent);
		bundle.put(CAUSE_ID, causeEventId);
		bundle.put(PARENT_ID, parentEventId);
		bundle.put(ROOT_ID, rootEventId);
		bundle.put(SOURCE_RULE, sourceRuleId == null ? "" : sourceRuleId);
		String[] chain = new String[eventChain.size()];
		int index = 0;
		for (RuleEvent event : eventChain) chain[index++] = event.name();
		bundle.put(EVENT_CHAIN, chain);
		bundle.put("amount", amount);
		bundle.put("melee", melee);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		payloadId = bundle.getLong(PAYLOAD_ID);
		kind = bundle.contains(KIND) ? bundle.getEnum(KIND, Kind.class) : Kind.NORMAL;
		remainingTurns = Math.max(0, bundle.getInt(REMAINING));
		targetId = bundle.getInt(TARGET_ID);
		targetCell = bundle.getInt(TARGET_CELL);
		sourceId = bundle.getInt(SOURCE_ID);
		Bundlable storedEffect = bundle.get(EFFECT);
		if (storedEffect instanceof RuleEffect) effect = (RuleEffect)storedEffect;
		effectSpec = bundle.contains("effect_spec") ? (EffectSpec)bundle.get("effect_spec") : null;
		Bundlable storedModifier = bundle.get(MODIFIER);
		if (storedModifier instanceof RuleModifier) modifier = (RuleModifier)storedModifier;
		causeEvent = bundle.getEnum(CAUSE_EVENT, RuleEvent.class);
		originalEvent = bundle.getEnum(ORIGINAL_EVENT, RuleEvent.class);
		causeEventId = bundle.getLong(CAUSE_ID);
		parentEventId = bundle.getLong(PARENT_ID);
		rootEventId = bundle.getLong(ROOT_ID);
		sourceRuleId = bundle.getString(SOURCE_RULE);
		if (sourceRuleId != null && sourceRuleId.isEmpty()) sourceRuleId = null;
		eventChain = EnumSet.noneOf(RuleEvent.class);
		String[] chain = bundle.getStringArray(EVENT_CHAIN);
		if (chain != null) {
			for (String name : chain) {
				try { eventChain.add(RuleEvent.valueOf(name)); } catch (IllegalArgumentException ignored) {}
			}
		}
		if (eventChain.isEmpty()) eventChain.add(causeEvent);
		amount = bundle.getInt("amount");
		melee = bundle.getBoolean("melee");
	}
}
