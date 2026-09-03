package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BuilderBudgetLedger {
	public static final class Entry {
		private final String ownerId, priceKey;
		private final int amount;
		public Entry(String ownerId, String priceKey, int amount) {
			if (ownerId == null || ownerId.isEmpty() || priceKey == null || priceKey.isEmpty() || amount < 0) throw new IllegalArgumentException("invalid budget entry");
			this.ownerId = ownerId; this.priceKey = priceKey; this.amount = amount;
		}
		public String ownerId() { return ownerId; }
		public String priceKey() { return priceKey; }
		public int amount() { return amount; }
	}
	private final String priceVersion;
	private final int spent;
	private final int limit;
	private final List<Entry> entries;
	public BuilderBudgetLedger(String priceVersion, int spent, int limit) {
		this(priceVersion, spent, limit, Collections.<Entry>emptyList());
	}
	public BuilderBudgetLedger(String priceVersion, int spent, int limit, List<Entry> entries) {
		if (priceVersion == null || spent < 0 || limit < 0 || entries == null) throw new IllegalArgumentException("invalid builder budget ledger");
		this.priceVersion = priceVersion; this.spent = spent; this.limit = limit;
		this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
	}
	public String priceVersion() { return priceVersion; }
	public int spent() { return spent; }
	public int limit() { return limit; }
	public List<Entry> entries() { return entries; }
	public boolean overBudget() { return spent > limit; }
}
