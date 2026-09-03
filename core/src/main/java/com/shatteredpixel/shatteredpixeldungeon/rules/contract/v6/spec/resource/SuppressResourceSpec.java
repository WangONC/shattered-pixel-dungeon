package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ResourceHolderSelector;import java.util.Collections;import java.util.List;
public final class SuppressResourceSpec implements ResourceOperationSpec {
	public enum ResourceSuppressionMode { BLOCK_GAIN }
	private final StableId operationId;private final ResourceHolderSelector holder;private final ResourceRef resource;private final int durationTurns;private final ResourceSuppressionMode mode;
	public SuppressResourceSpec(StableId operationId,ResourceHolderSelector holder,ResourceRef resource,int durationTurns,ResourceSuppressionMode mode){if(operationId==null||holder==null||resource==null||mode==null||durationTurns<1)throw new IllegalArgumentException("invalid suppress resource fields");this.operationId=operationId;this.holder=holder;this.resource=resource;this.durationTurns=durationTurns;this.mode=mode;}
	@Override public StableId operationId(){return operationId;}@Override public Variant variant(){return Variant.SUPPRESS;}@Override public List<ResourceRef> referencedResources(){return Collections.singletonList(resource);}public ResourceHolderSelector holder(){return holder;}public ResourceRef resource(){return resource;}public int durationTurns(){return durationTurns;}public ResourceSuppressionMode mode(){return mode;}
}
