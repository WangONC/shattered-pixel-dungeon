package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class BuiltinStatCompareCondition implements ConditionExpr {
	private final StableId nodeId;private final SubjectSelector subject;private final BuiltinStatRef stat;private final ComparisonOperator operator;private final ValueSpec value;
	public BuiltinStatCompareCondition(StableId nodeId,SubjectSelector subject,BuiltinStatRef stat,ComparisonOperator operator,ValueSpec value){if(nodeId==null||subject==null||stat==null||operator==null||value==null)throw new IllegalArgumentException("builtin condition fields required");this.nodeId=nodeId;this.subject=subject;this.stat=stat;this.operator=operator;this.value=value;}
	public StableId nodeId(){return nodeId;}public SubjectSelector subject(){return subject;}public BuiltinStatRef stat(){return stat;}public ComparisonOperator operator(){return operator;}public ValueSpec value(){return value;}@Override public String variantKey(){return "BUILTIN_STAT_COMPARE";}
}
