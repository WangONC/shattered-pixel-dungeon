package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ResourceHolderSelector;import java.util.Collections;import java.util.List;
public final class ClearResourceSpec implements ResourceOperationSpec {
	private final StableId operationId;private final ResourceHolderSelector holder;private final ResourceRef resource;
	public ClearResourceSpec(StableId operationId,ResourceHolderSelector holder,ResourceRef resource){if(operationId==null||holder==null||resource==null)throw new IllegalArgumentException("clear resource fields required");this.operationId=operationId;this.holder=holder;this.resource=resource;}
	@Override public StableId operationId(){return operationId;}@Override public Variant variant(){return Variant.CLEAR;}@Override public List<ResourceRef> referencedResources(){return Collections.singletonList(resource);}public ResourceHolderSelector holder(){return holder;}public ResourceRef resource(){return resource;}
}
