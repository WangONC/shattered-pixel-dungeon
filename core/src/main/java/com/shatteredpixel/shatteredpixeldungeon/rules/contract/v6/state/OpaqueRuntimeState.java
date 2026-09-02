package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;

/** Fail-closed persistence envelope for runtime variants implemented after P01. */
public final class OpaqueRuntimeState {
	private final StableId stateId;private final String variantKey;private final ImplementationState implementationState;
	public OpaqueRuntimeState(StableId stateId,String variantKey,ImplementationState implementationState){
		if(stateId==null||variantKey==null||variantKey.isEmpty()||implementationState==null)throw new IllegalArgumentException("invalid runtime envelope");
		if(implementationState!=ImplementationState.UNSUPPORTED&&implementationState!=ImplementationState.DEFERRED)throw new IllegalArgumentException("opaque runtime variants must fail closed");
		this.stateId=stateId;this.variantKey=variantKey;this.implementationState=implementationState;}
	public StableId stateId(){return stateId;}public String variantKey(){return variantKey;}public ImplementationState implementationState(){return implementationState;}
}
