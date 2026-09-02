package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.EntitySpecRef;
import java.util.ArrayList;import java.util.Collections;import java.util.List;
public final class EntityInstanceState {
	private final long instanceId;private final EntitySpecRef blueprint;private final long ownerActorId,sourceActorId;
	private final int cell,remainingTurns,createdOrder;private final List<OpaqueRuntimeState> capabilityStates;
	public EntityInstanceState(long instanceId,EntitySpecRef blueprint,long ownerActorId,long sourceActorId,int cell,int remainingTurns,int createdOrder){
		this(instanceId,blueprint,ownerActorId,sourceActorId,cell,remainingTurns,createdOrder,Collections.emptyList());}
	public EntityInstanceState(long instanceId,EntitySpecRef blueprint,long ownerActorId,long sourceActorId,int cell,int remainingTurns,int createdOrder,List<OpaqueRuntimeState> capabilityStates){
		if(instanceId<0||blueprint==null||ownerActorId< -1||sourceActorId< -1||cell< -1||remainingTurns< -1||createdOrder<0)throw new IllegalArgumentException("invalid entity instance state");
		if(capabilityStates==null)throw new IllegalArgumentException("capability states are required");
		this.instanceId=instanceId;this.blueprint=blueprint;this.ownerActorId=ownerActorId;this.sourceActorId=sourceActorId;this.cell=cell;this.remainingTurns=remainingTurns;this.createdOrder=createdOrder;this.capabilityStates=Collections.unmodifiableList(new ArrayList<>(capabilityStates));}
	public long instanceId(){return instanceId;}public EntitySpecRef blueprint(){return blueprint;}public long ownerActorId(){return ownerActorId;}
	public long sourceActorId(){return sourceActorId;}public int cell(){return cell;}public int remainingTurns(){return remainingTurns;}public int createdOrder(){return createdOrder;}public List<OpaqueRuntimeState> capabilityStates(){return capabilityStates;}
}
