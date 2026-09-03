package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;

public final class ResourceCompareCondition implements ConditionExpr {
	private final StableId nodeId;private final ResourceHolderSelector holder;private final ResourceRef resource;private final ComparisonOperator operator;private final int value;
	public ResourceCompareCondition(StableId nodeId,ResourceHolderSelector holder,ResourceRef resource,ComparisonOperator operator,int value){if(nodeId==null||holder==null||resource==null||operator==null)throw new IllegalArgumentException("resource condition fields required");this.nodeId=nodeId;this.holder=holder;this.resource=resource;this.operator=operator;this.value=value;}
	public StableId nodeId(){return nodeId;}public ResourceHolderSelector holder(){return holder;}public ResourceRef resource(){return resource;}public ComparisonOperator operator(){return operator;}public int value(){return value;}@Override public String variantKey(){return "RESOURCE_COMPARE";}
}
