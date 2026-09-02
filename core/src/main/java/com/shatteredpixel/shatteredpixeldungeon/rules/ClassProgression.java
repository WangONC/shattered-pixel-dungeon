package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;

/** Saved progression envelope; later tiers edit the same ClassBuild rather than fixed skill slots. */
public class ClassProgression implements Bundlable {
	public enum Tier { TIER_1, TIER_2, TIER_3_SPECIALIZATION, HIGH_LEVEL, TIER_4 }
	public Tier tier = Tier.TIER_1;
	public int budgetBonus;
	public final ArrayList<String> additions = new ArrayList<>();

	public ClassProgression copy() {
		ClassProgression result = new ClassProgression();
		result.tier = tier;
		result.budgetBonus = budgetBonus;
		result.additions.addAll(additions);
		return result;
	}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("tier", tier);
		bundle.put("budget_bonus", budgetBonus);
		bundle.put("additions", additions.toArray(new String[0]));
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		tier = bundle.getEnum("tier", Tier.class);
		budgetBonus = bundle.getInt("budget_bonus");
		additions.clear();
		if (bundle.contains("additions")) {
			String[] stored = bundle.getStringArray("additions");
			if (stored != null) java.util.Collections.addAll(additions, stored);
		}
	}
}
