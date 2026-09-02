package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.watabou.utils.Bundle;

/** Timed percentage mitigation, clamped so it can never become immunity. */
public class RuleMitigation extends Buff {
	private int percent;
	private int turns;
	{ type = buffType.POSITIVE; }

	public void set(int percent, int turns) {
		this.percent = Math.max(this.percent, Math.min(75, Math.max(1, percent)));
		this.turns = Math.max(this.turns, Math.max(1, turns));
		spend(TICK);
	}

	public int reduce(int damage) { return Math.max(0, damage * (100 - percent) / 100); }
	public int percent() { return percent; }
	public int remainingTurns() { return turns; }
	@Override public boolean act() { if (--turns <= 0) detach(); else spend(TICK); return true; }
	@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("percent", percent); b.put("turns", turns); }
	@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); percent=b.getInt("percent"); turns=b.getInt("turns"); }
}
