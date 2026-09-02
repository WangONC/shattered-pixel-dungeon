package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.TypedRef;
public final class DependencyRequest {
	private final StableId ownerNodeId;private final String fieldPath;private final TypedRef reference;
	public DependencyRequest(StableId ownerNodeId,String fieldPath,TypedRef reference){if(ownerNodeId==null||fieldPath==null||fieldPath.isEmpty()||reference==null)throw new IllegalArgumentException("dependency request fields are required");this.ownerNodeId=ownerNodeId;this.fieldPath=fieldPath;this.reference=reference;}
	public StableId ownerNodeId(){return ownerNodeId;}public String fieldPath(){return fieldPath;}public TypedRef reference(){return reference;}
}
