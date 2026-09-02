package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/** Generic, stackable Rule token implemented as a real saved SPD Buff. */
public class RuleMark extends Buff {
	public enum Type { HUNTED, ACCUMULATION, CHARGED, LOW_PHASE, COMPENSATION_LOCK }

	private Type mark = Type.HUNTED;
	private int stacks;
	private int sourceId = -1;
	private int ownerId = -1;
	private int transferDepth;

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	public static RuleMark apply(Char owner, Type type, Char source, int stacks, int duration) {
		return apply(owner, type, source, stacks, duration, 0);
	}

	public static RuleMark apply(Char owner, Type type, Char source, int stacks, int duration, int transferDepth) {
		if (owner == null || type == null || stacks <= 0) return null;
		RuleMark mark = get(owner, type);
		if (mark == null) {
			mark = new RuleMark();
			mark.mark = type;
			if (!mark.attachTo(owner)) return null;
		}
		mark.stacks += stacks;
		mark.sourceId = source == null ? -1 : source.id();
		mark.ownerId = owner.id();
		mark.transferDepth = Math.max(mark.transferDepth, transferDepth);
		mark.postpone(Math.max(1, duration));
		return mark;
	}

	public static RuleMark set(Char owner, Type type, Char source, int stacks, int duration) {
		RuleMark mark = get(owner, type);
		if (mark == null) mark = apply(owner, type, source, Math.max(1, stacks), duration);
		if (mark != null) {
			mark.stacks = Math.max(1, stacks);
			mark.sourceId = source == null ? -1 : source.id();
			mark.ownerId = owner.id();
			mark.postpone(Math.max(1, duration));
		}
		return mark;
	}

	/** Transfers a Mark once while retaining source ownership and recording propagation depth. */
	public static RuleMark transfer(RuleMark from, Char nextOwner, int stacks, int duration, int maxDepth) {
		if (from == null || nextOwner == null || from.transferDepth >= Math.max(0, maxDepth)) return null;
		Actor source = Actor.findById(from.sourceId);
		return apply(nextOwner, from.mark, source instanceof Char ? (Char)source : null,
				Math.max(1, stacks), duration, from.transferDepth + 1);
	}

	public static RuleMark get(Char owner, Type type) {
		if (owner == null || type == null) return null;
		for (RuleMark mark : owner.buffs(RuleMark.class)) if (mark.mark == type) return mark;
		return null;
	}

	public static boolean has(Char owner, Type type, int minimumStacks) {
		RuleMark mark = get(owner, type);
		return mark != null && mark.stacks >= Math.max(1, minimumStacks);
	}

	public static boolean consume(Char owner, Type type, int amount) {
		RuleMark mark = get(owner, type);
		if (mark == null || mark.stacks < Math.max(1, amount)) return false;
		mark.stacks -= Math.max(1, amount);
		if (mark.stacks <= 0) mark.detach();
		return true;
	}

	public static boolean remove(Char owner, Type type) {
		RuleMark mark = get(owner, type);
		if (mark == null) return false;
		mark.detach();
		return true;
	}

	public Type mark() { return mark; }
	public int stacks() { return stacks; }
	public int sourceId() { return sourceId; }
	public int ownerId() { return ownerId; }
	public int transferDepth() { return transferDepth; }
	public int remainingTurns() { return Math.max(0, (int)Math.ceil(cooldown())); }

	@Override
	public int icon() {
		return BuffIndicator.MARK;
	}

	@Override
	public String name() {
		return Messages.get(RuleMark.class, mark.name().toLowerCase() + "_name");
	}

	@Override
	public String desc() {
		return Messages.get(RuleMark.class, mark.name().toLowerCase() + "_desc", stacks, remainingTurns());
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public boolean act() {
		detach();
		return true;
	}

	private static final String MARK = "mark";
	private static final String STACKS = "stacks";
	private static final String SOURCE = "source";
	private static final String OWNER = "owner";
	private static final String TRANSFER_DEPTH = "transfer_depth";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(MARK, mark);
		bundle.put(STACKS, stacks);
		bundle.put(SOURCE, sourceId);
		bundle.put(OWNER, ownerId);
		bundle.put(TRANSFER_DEPTH, transferDepth);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		mark = bundle.contains(MARK) ? bundle.getEnum(MARK, Type.class) : Type.HUNTED;
		stacks = bundle.getInt(STACKS);
		sourceId = bundle.getInt(SOURCE);
		ownerId = bundle.getInt(OWNER);
		transferDepth = bundle.getInt(TRANSFER_DEPTH);
	}
}
