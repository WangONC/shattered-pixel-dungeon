package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.utils.Bundle;

/** Persistent, intentionally small ownership relation used by targeting and command effects. */
public class RuleOwnership extends Buff {
	private int ownerId = -1;

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	public static RuleOwnership assign(Char entity, Char owner) {
		if (entity == null || owner == null || entity == owner) return null;
		RuleOwnership value = Buff.affect(entity, RuleOwnership.class);
		value.ownerId = owner.id();
		return value;
	}

	public static boolean isOwnedBy(Char entity, Char owner) {
		RuleOwnership value = entity == null ? null : entity.buff(RuleOwnership.class);
		return value != null && owner != null && value.ownerId == owner.id();
	}

	public int ownerId() { return ownerId; }
	public Char owner() {
		Actor actor = Actor.findById(ownerId);
		return actor instanceof Char ? (Char)actor : null;
	}

	@Override public boolean act() {
		Char currentOwner = owner();
		if (ownerId < 0 || currentOwner == null || !currentOwner.isAlive()) {
			detach();
		} else {
			spend(TICK);
		}
		return true;
	}

	@Override public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("owner_id", ownerId);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		ownerId = bundle.getInt("owner_id");
	}
}
