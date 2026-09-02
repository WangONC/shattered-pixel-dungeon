package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/** A saved, independently expiring damage pool. It participates in SPD's normal shield ordering. */
public class RuleTemporaryHP extends ShieldBuff {
	private int turns;
	private int stackingRule; // 0=max, 1=add, 2=replace

	{
		type = buffType.POSITIVE;
		shieldUsePriority = 2;
	}

	public void grant(int amount, int duration, int stacking) {
		stackingRule = stacking;
		if (stacking == 1) incShield(Math.max(0, amount));
		else if (stacking == 2) {
			int current = shielding();
			if (current > 0) decShield(current);
			incShield(Math.max(0, amount));
		} else setShield(Math.max(0, amount));
		turns = Math.max(turns, Math.max(1, duration));
		spend(TICK);
	}

	public int remainingTurns() { return turns; }

	/** Trait cost payment uses this pool without touching real HP or ordinary Barrier. */
	public int absorbCost(int amount) {
		int paid = Math.min(Math.max(0, amount), shielding());
		if (paid > 0) decShield(paid);
		if (shielding() <= 0) detach();
		return paid;
	}

	@Override public boolean act() {
		if (--turns <= 0) detach();
		else spend(TICK);
		return true;
	}

	@Override public int icon() { return BuffIndicator.ARMOR; }
	@Override public String iconTextDisplay() { return Integer.toString(shielding()); }

	@Override public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("turns", turns);
		bundle.put("stacking", stackingRule);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		turns = bundle.getInt("turns");
		stackingRule = bundle.getInt("stacking");
	}
}
