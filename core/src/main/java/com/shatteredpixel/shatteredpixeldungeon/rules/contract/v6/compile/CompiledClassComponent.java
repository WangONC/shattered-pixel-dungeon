package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Independent immutable Class Component runtime node with no authoring-model types. */
public final class CompiledClassComponent {
	public enum Variant { BASIC_ATTACK, RESOURCE_FLOW, ACTIVE_RESOURCE_OPERATION }
	public enum BasicAttackAvailability { FULL, WEAK, NONE }
	private final StableId id;private final Variant variant;private final BasicAttackAvailability attackAvailability;
	private final int first,second,actionTurns;private final CompiledSkill.Trigger trigger;private final CompiledSkill.Condition condition;
	private final CompiledSkill.ResourceOperation operation;private final CompiledSkill.Cost cost;private final StableId operationId;private final CompiledSkill.ItemCategory allowedWeapons;
	public CompiledClassComponent(StableId id,Variant variant,BasicAttackAvailability availability,int first,int second,int turns,CompiledSkill.ItemCategory allowedWeapons,CompiledSkill.Trigger trigger,CompiledSkill.Condition condition,CompiledSkill.ResourceOperation operation,CompiledSkill.Cost cost,StableId operationId){if(id==null||variant==null)throw new IllegalArgumentException("compiled component identity required");this.id=id;this.variant=variant;attackAvailability=availability;this.first=first;this.second=second;actionTurns=turns;this.allowedWeapons=allowedWeapons;this.trigger=trigger;this.condition=condition;this.operation=operation;this.cost=cost;this.operationId=operationId;}
	public StableId id(){return id;}public Variant variant(){return variant;}public BasicAttackAvailability attackAvailability(){return attackAvailability;}
	public int first(){return first;}public int second(){return second;}public int actionTurns(){return actionTurns;}public CompiledSkill.Trigger trigger(){return trigger;}
	public CompiledSkill.ItemCategory allowedWeapons(){return allowedWeapons;}public CompiledSkill.Condition condition(){return condition;}public CompiledSkill.ResourceOperation operation(){return operation;}public CompiledSkill.Cost cost(){return cost;}public StableId operationId(){return operationId;}
}
