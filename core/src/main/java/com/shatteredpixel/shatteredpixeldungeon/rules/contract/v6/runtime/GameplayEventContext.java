package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;

/** Immutable event input shared by trigger, targeting, delivery, and effects. */
public final class GameplayEventContext {
	public enum RuleEventType {
		ACTIVE, TURN_START, WAIT, MOVE, ENTER_TILE, ATTACK_DECLARED, ATTACK_HIT, DAMAGED,
		KILL, DEATH, ITEM_USED, STATUS_APPLIED, LOW_HP_ENTERED, EQUIPMENT_CHANGED,
		ENTITY_CREATED, ENTITY_DESTROYED
	}
	private final long eventId;
	private final long causeEventId;
	private final RuleEventType eventType;
	private final Char classOwner;
	private final Char sourceActor;
	private final Char selectedActor;
	private final int selectedCell;

	public GameplayEventContext(long eventId, long causeEventId, RuleEventType eventType,
			Char classOwner, Char sourceActor, Char selectedActor, int selectedCell) {
		if (eventId <= 0 || causeEventId < 0 || eventType == null || classOwner == null) {
			throw new IllegalArgumentException("event id, type, and class owner are required");
		}
		this.eventId = eventId;
		this.causeEventId = causeEventId;
		this.eventType = eventType;
		this.classOwner = classOwner;
		this.sourceActor = sourceActor;
		this.selectedActor = selectedActor;
		this.selectedCell = selectedCell;
	}
	public static GameplayEventContext active(long eventId, Char owner, Char selectedActor) {
		return new GameplayEventContext(eventId, 0, RuleEventType.ACTIVE, owner, owner, selectedActor,
				selectedActor == null ? -1 : selectedActor.pos);
	}
	public long eventId() { return eventId; }
	public long causeEventId() { return causeEventId; }
	public RuleEventType eventType() { return eventType; }
	public Char classOwner() { return classOwner; }
	public Char sourceActor() { return sourceActor; }
	public Char selectedActor() { return selectedActor; }
	public int selectedCell() { return selectedCell; }
}
