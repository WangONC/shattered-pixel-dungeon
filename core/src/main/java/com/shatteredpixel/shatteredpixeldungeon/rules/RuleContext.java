package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.EnumSet;

/** Transient data for one event dispatch. This object is never retained or saved. */
public final class RuleContext {
	public final RuleEvent event;
	public final Hero hero;
	public Char source;
	public Char target;
	public Item item;
	public Buff status;
	public int cell = -1;
	public int amount;
	public boolean melee;
	private RuleEvent originalEvent;
	private long eventId;
	private long parentEventId;
	private long rootEventId;
	private String sourceRuleId;
	private EnumSet<RuleEvent> eventChain = EnumSet.noneOf(RuleEvent.class);

	public RuleContext(RuleEvent event, Hero hero) {
		this.event = event;
		this.hero = hero;
		this.originalEvent = event;
		if (event != null) eventChain.add(event);
	}

	private RuleContext(RuleEvent event, Hero hero, RuleContext copy) {
		this.event = event;
		this.hero = hero;
		source = copy.source;
		target = copy.target;
		item = copy.item;
		status = copy.status;
		cell = copy.cell;
		amount = copy.amount;
		melee = copy.melee;
		originalEvent = copy.originalEvent;
		eventId = copy.eventId;
		parentEventId = copy.parentEventId;
		rootEventId = copy.rootEventId;
		sourceRuleId = copy.sourceRuleId;
		eventChain = copy.eventChain.clone();
	}

	/** Copy used while one Rule is producing child gameplay events. */
	public RuleContext causedByRule(String ruleId) {
		RuleContext result = new RuleContext(event, hero, this);
		result.sourceRuleId = ruleId;
		return result;
	}

	/** Returns null when this event already exists in the causal chain. */
	public RuleContext child(RuleEvent childEvent) {
		if (childEvent == null || eventChain.contains(childEvent)) return null;
		RuleContext result = new RuleContext(childEvent, hero, this);
		result.parentEventId = eventId;
		result.eventId = 0;
		result.eventChain.add(childEvent);
		return result;
	}

	void assignEventId(long id) {
		if (eventId != 0) return;
		eventId = id;
		if (rootEventId == 0) rootEventId = id;
	}

	static RuleContext restoreProvenance(RuleEvent event, Hero hero, RuleEvent originalEvent,
			long eventId, long parentEventId, long rootEventId, String sourceRuleId,
			EnumSet<RuleEvent> chain) {
		RuleContext result = new RuleContext(event, hero);
		result.originalEvent = originalEvent == null ? event : originalEvent;
		result.eventId = eventId;
		result.parentEventId = parentEventId;
		result.rootEventId = rootEventId;
		result.sourceRuleId = sourceRuleId;
		result.eventChain = chain == null || chain.isEmpty()
				? EnumSet.of(event) : chain.clone();
		return result;
	}

	public RuleEvent originalEvent() { return originalEvent; }
	public long eventId() { return eventId; }
	public long parentEventId() { return parentEventId; }
	public long rootEventId() { return rootEventId; }
	public String sourceRuleId() { return sourceRuleId; }
	public boolean hasSeen(RuleEvent value) { return eventChain.contains(value); }
	public EnumSet<RuleEvent> eventChain() { return eventChain.clone(); }
}
