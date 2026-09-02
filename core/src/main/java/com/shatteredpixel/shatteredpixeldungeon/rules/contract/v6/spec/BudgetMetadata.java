package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

/** P01 persistence boundary only; the authoritative catalog and ledger arrive in P09. */
public final class BudgetMetadata {
	private final String priceVersion;
	private final int baseBudget;
	public BudgetMetadata(String priceVersion,int baseBudget){
		if(priceVersion==null||baseBudget<0)throw new IllegalArgumentException("invalid budget metadata");
		this.priceVersion=priceVersion;this.baseBudget=baseBudget;}
	public String priceVersion(){return priceVersion;}public int baseBudget(){return baseBudget;}
}
