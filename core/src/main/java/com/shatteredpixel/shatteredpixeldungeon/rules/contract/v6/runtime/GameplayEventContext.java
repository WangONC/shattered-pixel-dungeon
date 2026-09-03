package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Serializable-shaped immutable event provenance: IDs and cells only, never mutable Actors. */
public final class GameplayEventContext {
	public enum RuleEventType { ACTIVE,TURN_START,WAIT,MOVE,ENTER_TILE,ATTACK_DECLARED,ATTACK_HIT,DAMAGED,KILL,DEATH,ITEM_USED,STATUS_APPLIED,LOW_HP_ENTERED,EQUIPMENT_CHANGED,ENTITY_CREATED,ENTITY_DESTROYED }
	private final long eventId;private final long causeEventId;private final RuleEventType eventType;
	private final int classOwnerActorId;private final int sourceActorId;private final int selectedActorId;
	private final int classOwnerCell;private final int sourceCell;private final int selectedCell;
	private final StableId originatingSkillId;
	public GameplayEventContext(long eventId,long causeEventId,RuleEventType eventType,int classOwnerActorId,int sourceActorId,
			int selectedActorId,int classOwnerCell,int sourceCell,int selectedCell,StableId originatingSkillId){
		if(eventId<=0||causeEventId<0||eventType==null||classOwnerActorId<=0||originatingSkillId==null)throw new IllegalArgumentException("event provenance fields required");
		this.eventId=eventId;this.causeEventId=causeEventId;this.eventType=eventType;this.classOwnerActorId=classOwnerActorId;this.sourceActorId=sourceActorId;
		this.selectedActorId=selectedActorId;this.classOwnerCell=classOwnerCell;this.sourceCell=sourceCell;this.selectedCell=selectedCell;this.originatingSkillId=originatingSkillId;
	}
	public long eventId(){return eventId;}public long causeEventId(){return causeEventId;}public RuleEventType eventType(){return eventType;}
	public int classOwnerActorId(){return classOwnerActorId;}public int sourceActorId(){return sourceActorId;}public int selectedActorId(){return selectedActorId;}
	public int classOwnerCell(){return classOwnerCell;}public int sourceCell(){return sourceCell;}public int selectedCell(){return selectedCell;}
	public StableId originatingSkillId(){return originatingSkillId;}
}
