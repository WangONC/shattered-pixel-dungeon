package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ResourceHolderSelector;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ValueSpec;import java.util.Collections;import java.util.List;
public final class SetResourceSpec implements ResourceOperationSpec {
	private final StableId operationId;private final ResourceHolderSelector holder;private final ResourceRef resource;private final ValueSpec value;
	public SetResourceSpec(StableId operationId,ResourceHolderSelector holder,ResourceRef resource,ValueSpec value){if(operationId==null||holder==null||resource==null||value==null)throw new IllegalArgumentException("set resource fields required");this.operationId=operationId;this.holder=holder;this.resource=resource;this.value=value;}
	@Override public StableId operationId(){return operationId;}@Override public Variant variant(){return Variant.SET;}@Override public List<ResourceRef> referencedResources(){return Collections.singletonList(resource);}public ResourceHolderSelector holder(){return holder;}public ResourceRef resource(){return resource;}public ValueSpec value(){return value;}
}
