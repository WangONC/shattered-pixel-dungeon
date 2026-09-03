package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

public final class BuilderBudgetLedger {
	private final String priceVersion;
	private final int spent;
	private final int limit;
	public BuilderBudgetLedger(String priceVersion, int spent, int limit) {
		if (priceVersion == null || spent < 0 || limit < 0) throw new IllegalArgumentException("invalid builder budget ledger");
		this.priceVersion = priceVersion; this.spent = spent; this.limit = limit;
	}
	public String priceVersion() { return priceVersion; }
	public int spent() { return spent; }
	public int limit() { return limit; }
	public boolean overBudget() { return spent > limit; }
}
