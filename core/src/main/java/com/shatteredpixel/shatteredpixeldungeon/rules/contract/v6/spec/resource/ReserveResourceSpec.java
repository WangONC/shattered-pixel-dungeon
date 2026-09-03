package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ResourceHolderSelector;import java.util.Collections;import java.util.List;
public final class ReserveResourceSpec implements ResourceOperationSpec {
	public enum ReservationExpiryPolicy { RETURN_TO_HOLDER }
	private final StableId operationId;private final ResourceHolderSelector holder;private final ResourceRef resource;private final int amount,durationTurns;private final ReservationExpiryPolicy expiryPolicy;
	public ReserveResourceSpec(StableId operationId,ResourceHolderSelector holder,ResourceRef resource,int amount,int durationTurns,ReservationExpiryPolicy expiryPolicy){if(operationId==null||holder==null||resource==null||expiryPolicy==null||amount<1||durationTurns<1)throw new IllegalArgumentException("invalid reserve resource fields");this.operationId=operationId;this.holder=holder;this.resource=resource;this.amount=amount;this.durationTurns=durationTurns;this.expiryPolicy=expiryPolicy;}
	@Override public StableId operationId(){return operationId;}@Override public Variant variant(){return Variant.RESERVE;}@Override public List<ResourceRef> referencedResources(){return Collections.singletonList(resource);}public ResourceHolderSelector holder(){return holder;}public ResourceRef resource(){return resource;}public int amount(){return amount;}public int durationTurns(){return durationTurns;}public ReservationExpiryPolicy expiryPolicy(){return expiryPolicy;}
}
