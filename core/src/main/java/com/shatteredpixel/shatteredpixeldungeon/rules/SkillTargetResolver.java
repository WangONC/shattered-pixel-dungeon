package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleOwnership;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

/** Deterministic selector/coverage/filter executor shared by every Skill delivery. */
public final class SkillTargetResolver {
	private SkillTargetResolver() {}

	public static ArrayList<Integer> resolve(RuleContext context, SkillDelivery delivery,
			TargetingSpec spec, RuleModifier modifier) {
		ArrayList<Integer> result = new ArrayList<>();
		if (context == null || context.hero == null || Dungeon.level == null || spec == null) return result;
		int origin = context.hero.pos;
		int selected = selectedCell(context, spec);

		if (delivery == SkillDelivery.CONTACT_ATTACK && selected >= 0
				&& Dungeon.level.distance(origin, selected) > 1) return result;
		if (delivery == SkillDelivery.PROJECTILE || delivery == SkillDelivery.TRACE_BEAM) {
			return ballistic(context, delivery, spec, modifier, origin, selected);
		}

		ArrayList<Integer> centers = selectorCells(context, spec, selected);
		for (int center : centers) addCoverage(result, origin, center, spec);
		filter(result, context, spec);
		if (spec.coverage == TargetingSpec.Coverage.CHAIN && !result.isEmpty()) {
			appendChain(result, context, spec, Math.max(0, spec.maxTargets-result.size()));
		}
		limit(result, spec.maxTargets);
		if (modifier != null && modifier.bounces() > 0 && !result.isEmpty()) {
			appendChain(result, context, spec, modifier.bounces());
		}
		return result;
	}

	private static int selectedCell(RuleContext context, TargetingSpec spec) {
		if (spec.selector == TargetingSpec.Selector.SELF) return context.hero.pos;
		if (context.target != null) return context.target.pos;
		if (context.cell >= 0) return context.cell;
		if (context.source != null && context.source != context.hero) return context.source.pos;
		return context.hero.pos;
	}

	private static ArrayList<Integer> selectorCells(RuleContext context, TargetingSpec spec, int selected) {
		ArrayList<Integer> values = new ArrayList<>();
		if (spec.selector == TargetingSpec.Selector.SELF) values.add(context.hero.pos);
		else if (spec.selector == TargetingSpec.Selector.SELECTED_ACTOR
				|| spec.selector == TargetingSpec.Selector.SELECTED_CELL) values.add(selected);
		else {
			ArrayList<Char> candidates = candidates(context, spec, context.hero.pos, spec.range);
			if (spec.selector == TargetingSpec.Selector.NEAREST && !candidates.isEmpty()) values.add(candidates.get(0).pos);
			else if (spec.selector == TargetingSpec.Selector.RANDOM && !candidates.isEmpty()) {
				values.add(candidates.get(Random.Int(candidates.size())).pos);
			} else if (spec.selector == TargetingSpec.Selector.ALL_MATCHING) {
				for (Char ch : candidates) values.add(ch.pos);
			}
		}
		return values;
	}

	private static ArrayList<Integer> ballistic(RuleContext context, SkillDelivery delivery,
			TargetingSpec spec, RuleModifier modifier, int origin, int selected) {
		ArrayList<Integer> result = new ArrayList<>();
		if (selected < 0 || selected >= Dungeon.level.length()) return result;
		boolean pierce = delivery == SkillDelivery.TRACE_BEAM || modifier != null && modifier.pierces() > 0;
		Ballistica shot = new Ballistica(origin, selected, pierce ? Ballistica.STOP_SOLID : Ballistica.PROJECTILE);
		int remaining = pierce && modifier != null ? Math.max(1, modifier.pierces()) : Integer.MAX_VALUE;
		// Ballistica.path intentionally continues past collision; dist is the authoritative reachable end.
		int reachableEnd = Math.min(shot.dist, Math.min(spec.range, shot.path.size() - 1));
		for (int i = 1; i <= reachableEnd; i++) {
			int cell = shot.path.get(i);
			Char ch = Actor.findChar(cell);
			if (delivery == SkillDelivery.TRACE_BEAM && spec.coverage == TargetingSpec.Coverage.LINE) {
				if (ch != null && matches(ch, context, spec)) result.add(cell);
			} else if (ch != null && matches(ch, context, spec)) {
				result.add(cell);
				if (!pierce || --remaining <= 0) break;
			}
		}
		if (delivery == SkillDelivery.PROJECTILE && result.isEmpty()
				&& shot.collisionPos >= 0 && shot.dist <= spec.range
				&& spec.selector == TargetingSpec.Selector.SELECTED_CELL) result.add(shot.collisionPos);
		if (spec.coverage == TargetingSpec.Coverage.CHAIN && !result.isEmpty()) appendChain(result, context, spec,
				Math.max(0, spec.maxTargets - result.size()));
		return result;
	}

	private static void addCoverage(ArrayList<Integer> result, int origin, int center, TargetingSpec spec) {
		if (center < 0 || center >= Dungeon.level.length()) return;
		switch (spec.coverage) {
			case SINGLE: add(result, center); break;
			case ADJACENT:
			case RADIUS:
				for (int cell = 0; cell < Dungeon.level.length(); cell++) {
					if (Dungeon.level.distance(center, cell) <= Math.max(1, spec.magnitude)) add(result, cell);
				}
				break;
			case RING:
				for (int cell = 0; cell < Dungeon.level.length(); cell++) {
					if (Dungeon.level.distance(center, cell) == Math.max(1, spec.magnitude)) add(result, cell);
				}
				break;
			case LINE:
				Ballistica line = new Ballistica(origin, center, Ballistica.STOP_SOLID);
				for (int i = 1; i <= line.dist && i < line.path.size() && i <= spec.range; i++) add(result, line.path.get(i));
				break;
			case CONE:
				int width = Dungeon.level.width();
				int ox = origin % width, oy = origin / width;
				int tx = center % width, ty = center / width;
				int dx = Integer.signum(tx - ox), dy = Integer.signum(ty - oy);
				for (int cell = 0; cell < Dungeon.level.length(); cell++) {
					int cx = cell % width - ox, cy = cell / width - oy;
					int forward = cx * dx + cy * dy;
					int side = Math.abs(cx * dy - cy * dx);
					if (forward > 0 && forward <= spec.range && side <= Math.max(1, forward / 2)) add(result, cell);
				}
				break;
			case CHAIN: add(result, center); break;
		}
	}

	private static void appendChain(ArrayList<Integer> result, RuleContext context, TargetingSpec spec, int jumps) {
		HashSet<Integer> used = new HashSet<>(result);
		int current = result.get(result.size() - 1);
		while (jumps-- > 0 && result.size() < spec.maxTargets) {
			ArrayList<Char> next = candidates(context, spec, current, Math.max(1, spec.magnitude));
			Char chosen = null;
			for (Char candidate : next) if (!used.contains(candidate.pos)) { chosen = candidate; break; }
			if (chosen == null) break;
			result.add(chosen.pos);
			used.add(chosen.pos);
			current = chosen.pos;
		}
	}

	private static ArrayList<Char> candidates(RuleContext context, TargetingSpec spec, final int origin, int range) {
		ArrayList<Char> values = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch.isAlive() && Dungeon.level.distance(origin, ch.pos) <= range && matches(ch, context, spec)) values.add(ch);
		}
		Collections.sort(values, new Comparator<Char>() {
			@Override public int compare(Char a, Char b) {
				int distance = Integer.compare(Dungeon.level.distance(origin, a.pos), Dungeon.level.distance(origin, b.pos));
				return distance != 0 ? distance : Integer.compare(a.id(), b.id());
			}
		});
		return values;
	}

	private static void filter(ArrayList<Integer> cells, RuleContext context, TargetingSpec spec) {
		for (int i = cells.size() - 1; i >= 0; i--) {
			Char ch = Actor.findChar(cells.get(i));
			if (spec.filter != TargetingSpec.Filter.ANY && !matches(ch, context, spec)) cells.remove(i);
		}
	}

	private static boolean matches(Char ch, RuleContext context, TargetingSpec spec) {
		if (spec.filter == TargetingSpec.Filter.ANY) return true;
		if (ch == null) return false;
		switch (spec.filter) {
			case ENEMY: return ch.alignment == Char.Alignment.ENEMY;
			case ALLY: return ch != context.hero && ch.alignment == context.hero.alignment;
			case SELF: return ch == context.hero;
			case OWNED_ENTITY: return RuleOwnership.isOwnedBy(ch, context.hero);
			case MARKED: return !ch.buffs(RuleMark.class).isEmpty()
					&& (spec.filterParameter <= 1 || maxMarkStacks(ch) >= spec.filterParameter);
			case HAS_STATUS:
				for (Buff buff : ch.buffs()) if (buff.type == Buff.buffType.NEGATIVE) return true;
				return false;
			case HP_THRESHOLD: return ch.HP * 100 <= ch.HT * Math.max(1, spec.filterParameter);
			case COMPATIBLE_ENTITY_TYPE: return ch != context.hero;
			case ANY:
			default: return true;
		}
	}

	private static int maxMarkStacks(Char ch) {
		int result = 0;
		for (RuleMark mark : ch.buffs(RuleMark.class)) result = Math.max(result, mark.stacks());
		return result;
	}

	private static void add(ArrayList<Integer> values, int cell) {
		if (cell >= 0 && cell < Dungeon.level.length() && !values.contains(cell)) values.add(cell);
	}

	private static void limit(ArrayList<Integer> values, int max) {
		while (values.size() > Math.max(1, max)) values.remove(values.size() - 1);
	}
}
