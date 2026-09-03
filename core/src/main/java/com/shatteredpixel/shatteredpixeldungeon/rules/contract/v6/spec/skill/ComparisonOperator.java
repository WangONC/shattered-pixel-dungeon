package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public enum ComparisonOperator {
	LT, LTE, EQ, GTE, GT;
	public boolean test(int left,int right){switch(this){case LT:return left<right;case LTE:return left<=right;case EQ:return left==right;case GTE:return left>=right;case GT:return left>right;default:throw new AssertionError(this);}}
}
