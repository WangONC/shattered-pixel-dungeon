package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ResourceHolderSelector;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ValueSpec;
import java.util.Collections;import java.util.List;

public final class DrainResourceSpec implements ResourceOperationSpec {
	public enum InsufficientResourcePolicy { FAIL, DRAIN_AVAILABLE }
	private final StableId operationId;private final ResourceHolderSelector holder;private final ResourceRef resource;private final ValueSpec amount;private final InsufficientResourcePolicy insufficientPolicy;
	public DrainResourceSpec(StableId operationId,ResourceHolderSelector holder,ResourceRef resource,ValueSpec amount,InsufficientResourcePolicy insufficientPolicy){if(operationId==null||holder==null||resource==null||amount==null||insufficientPolicy==null)throw new IllegalArgumentException("drain resource fields required");this.operationId=operationId;this.holder=holder;this.resource=resource;this.amount=amount;this.insufficientPolicy=insufficientPolicy;}
	@Override public StableId operationId(){return operationId;}@Override public Variant variant(){return Variant.DRAIN;}@Override public List<ResourceRef> referencedResources(){return Collections.singletonList(resource);}public ResourceHolderSelector holder(){return holder;}public ResourceRef resource(){return resource;}public ValueSpec amount(){return amount;}public InsufficientResourcePolicy insufficientPolicy(){return insufficientPolicy;}
}
