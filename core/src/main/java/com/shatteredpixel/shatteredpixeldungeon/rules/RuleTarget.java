package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;

/** Generic target selection shared by every effect. */
public class RuleTarget implements RuleModule, Bundlable {
	public enum Type {
		SELF,
		ATTACKER,
		ATTACK_TARGET,
		HIT_TARGET,
		SELECTED_TARGET,
		SELECTED_CELL,
		NEAREST_ENEMY,
		ALL_ADJACENT_ENEMIES,
		CURRENT_TILE
	}

	public Type type = Type.SELF;

	public RuleTarget() {}

	public RuleTarget(Type type) {
		this.type = type;
	}

	/** Whether an event integration point actually supplies enough context for this selector. */
	public static boolean availableForEvent(Type type, RuleEvent event) {
		if (type == null || event == null) return false;
		if (type == Type.ATTACK_TARGET) return false; // legacy V0.1 alias, not offered to new builds
		if (type == Type.ATTACKER) {
			return event == RuleEvent.ON_DAMAGED || event == RuleEvent.ON_LOW_HP;
		}
		if (type == Type.HIT_TARGET) {
			return event == RuleEvent.ON_HIT || event == RuleEvent.ON_KILL || event == RuleEvent.ON_ATTACK;
		}
		if (type == Type.SELECTED_TARGET || type == Type.SELECTED_CELL) {
			return event == RuleEvent.ACTIVE;
		}
		return true;
	}

	public ArrayList<Char> resolveChars(RuleContext context) {
		ArrayList<Char> result = new ArrayList<>();
		if (context == null || context.hero == null) return result;
		switch (type) {
			case ATTACKER:
				addUnique(result, context.source);
				break;
			case ATTACK_TARGET:
			case HIT_TARGET:
				addUnique(result, context.target);
				break;
			case SELECTED_TARGET:
			case SELECTED_CELL:
				if (context.cell >= 0) addUnique(result, Actor.findChar(context.cell));
				break;
			case NEAREST_ENEMY:
				if (Dungeon.level != null) {
					Mob nearest = null;
					int nearestDistance = Integer.MAX_VALUE;
					for (Mob mob : Dungeon.level.mobs) {
						if (mob.alignment != Char.Alignment.ENEMY) continue;
						int distance = Dungeon.level.distance(context.hero.pos, mob.pos);
						if (distance < nearestDistance) {
							nearest = mob;
							nearestDistance = distance;
						}
					}
					addUnique(result, nearest);
				}
				break;
			case ALL_ADJACENT_ENEMIES:
				if (Dungeon.level != null) {
					for (Mob mob : Dungeon.level.mobs) {
						if (mob.alignment == Char.Alignment.ENEMY
								&& Dungeon.level.adjacent(context.hero.pos, mob.pos)) addUnique(result, mob);
					}
				}
				break;
			case CURRENT_TILE:
			case SELF:
			default:
				addUnique(result, context.hero);
				break;
		}
		return result;
	}

	public ArrayList<Integer> resolveCells(RuleContext context, ArrayList<Char> chars) {
		ArrayList<Integer> result = new ArrayList<>();
		if (context == null || context.hero == null) return result;
		if ((type == Type.SELECTED_CELL || type == Type.SELECTED_TARGET) && context.cell >= 0) {
			result.add(context.cell);
		} else if (type == Type.CURRENT_TILE || type == Type.SELF) {
			result.add(context.hero.pos);
		} else {
			for (Char ch : chars) if (ch != null && !result.contains(ch.pos)) result.add(ch.pos);
		}
		return result;
	}

	private static void addUnique(ArrayList<Char> chars, Char ch) {
		if (ch != null && !chars.contains(ch)) chars.add(ch);
	}

	public Char resolveChar(RuleContext context) {
		ArrayList<Char> chars = resolveChars(context);
		return chars.isEmpty() ? null : chars.get(0);
	}

	public int resolveCell(RuleContext context, Char target) {
		ArrayList<Char> chars = new ArrayList<>();
		if (target != null) chars.add(target);
		ArrayList<Integer> cells = resolveCells(context, chars);
		return cells.isEmpty() ? -1 : cells.get(0);
	}

	@Override
	public int capacityCost() {
		return type == Type.SELF || type == Type.CURRENT_TILE ? 0 : 1;
	}

	@Override
	public String description() {
		return Messages.get(RuleTarget.class, type.name().toLowerCase());
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("type", type);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		type = bundle.getEnum("type", Type.class);
	}
}
