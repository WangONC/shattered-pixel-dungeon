package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.utils.Bundle;

/** Saved one-way incoming-damage relation. Cycles are rejected by RuleDefenseRuntime. */
public class RuleDamageRedirect extends Buff {
	private int recipientId = -1;
	private int percent;
	private int turns;
	{ type = buffType.POSITIVE; }

	public void set(Char recipient, int percent, int turns) {
		recipientId = recipient == null ? -1 : recipient.id();
		this.percent = Math.min(75, Math.max(1, percent));
		this.turns = Math.max(1, turns);
		spend(TICK);
	}
	public Char recipient() { Actor a = Actor.findById(recipientId); return a instanceof Char ? (Char)a : null; }
	public int percent() { return percent; }
	@Override public boolean act() { Char current=recipient(); if (--turns <= 0 || current == null || !current.isAlive()) detach(); else spend(TICK); return true; }
	@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("recipient",recipientId); b.put("percent",percent); b.put("turns",turns); }
	@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); recipientId=b.getInt("recipient"); percent=b.getInt("percent"); turns=b.getInt("turns"); }
}
