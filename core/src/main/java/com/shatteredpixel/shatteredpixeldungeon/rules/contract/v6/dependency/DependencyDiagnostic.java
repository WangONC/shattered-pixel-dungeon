package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class DependencyDiagnostic {
	private final StableId ownerNodeId;private final String fieldPath;private final DependencyState state;
	private final StableId targetId;private final String messageKey;
	public DependencyDiagnostic(StableId ownerNodeId,String fieldPath,DependencyState state,StableId targetId,String messageKey){
		if(ownerNodeId==null||fieldPath==null||state==null||messageKey==null||messageKey.isEmpty())throw new IllegalArgumentException("diagnostic fields are required");
		this.ownerNodeId=ownerNodeId;this.fieldPath=fieldPath;this.state=state;this.targetId=targetId;this.messageKey=messageKey;}
	public StableId ownerNodeId(){return ownerNodeId;}public String fieldPath(){return fieldPath;}public DependencyState state(){return state;}
	public StableId targetId(){return targetId;}public String messageKey(){return messageKey;}
}
