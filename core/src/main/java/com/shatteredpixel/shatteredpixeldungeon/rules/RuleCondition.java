package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMode;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleOwnership;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleRelation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** One parameterized predicate. RuleDefinition evaluates its ordered list with AND semantics. */
public class RuleCondition implements RuleModule, Bundlable {
	public enum Type {
		ALWAYS,
		TARGET_EXISTS,
		HERO_BELOW_HALF,
		SELF_HP_BELOW,
		TARGET_HP_BELOW,
		TARGET_HAS_POISON,
		TARGET_IS_BURNING,
		SELF_IN_WATER,
		DISTANCE_AT_LEAST,
		ADJACENT_ENEMIES_AT_LEAST,
		RESOURCE_AT_LEAST,
		MODE_IS,
		TARGET_OWNED,
		LINK_EXISTS
	}

	public Type type = Type.ALWAYS;
	/** Percentage, distance, count, or resource threshold depending on type. */
	public int parameter;
	public String reference = "";

	public RuleCondition() {}

	public RuleCondition(Type type) {
		this(type, defaultParameter(type));
	}

	public RuleCondition(Type type, int parameter) {
		this.type = type;
		this.parameter = parameter > 0 ? parameter : defaultParameter(type);
	}

	private static int defaultParameter(Type type) {
		switch (type) {
			case SELF_HP_BELOW:
			case TARGET_HP_BELOW:
			case HERO_BELOW_HALF: return 50;
			case DISTANCE_AT_LEAST: return 3;
			case ADJACENT_ENEMIES_AT_LEAST: return 2;
			case RESOURCE_AT_LEAST: return 3;
			default: return 0;
		}
	}

	public boolean passes(RuleRuntime runtime, RuleContext context, Char resolvedTarget) {
		if (context == null || context.hero == null) return false;
		switch (type) {
			case TARGET_EXISTS:
				return resolvedTarget != null;
			case HERO_BELOW_HALF:
				return context.hero.HP * 2 <= context.hero.HT;
			case SELF_HP_BELOW:
				return context.hero.HP * 100 <= context.hero.HT * Math.max(1, parameter);
			case TARGET_HP_BELOW:
				return resolvedTarget != null
						&& resolvedTarget.HP * 100 <= resolvedTarget.HT * Math.max(1, parameter);
			case TARGET_HAS_POISON:
				return resolvedTarget != null && resolvedTarget.buff(Poison.class) != null;
			case TARGET_IS_BURNING:
				return resolvedTarget != null && resolvedTarget.buff(Burning.class) != null;
			case SELF_IN_WATER:
				return Dungeon.level != null && context.hero.pos >= 0
						&& context.hero.pos < Dungeon.level.water.length && Dungeon.level.water[context.hero.pos];
			case DISTANCE_AT_LEAST:
				return resolvedTarget != null && Dungeon.level != null
						&& Dungeon.level.distance(context.hero.pos, resolvedTarget.pos) >= Math.max(1, parameter);
			case ADJACENT_ENEMIES_AT_LEAST:
				if (Dungeon.level == null) return false;
				int count = 0;
				for (Char ch : Dungeon.level.mobs) {
					if (ch.alignment == Char.Alignment.ENEMY
							&& Dungeon.level.adjacent(context.hero.pos, ch.pos)) count++;
				}
				return count >= Math.max(1, parameter);
			case RESOURCE_AT_LEAST:
				return runtime != null && (reference == null || reference.isEmpty()
						? runtime.availableResource(context.hero)
						: runtime.resourceValue(reference, null)) >= Math.max(1, parameter);
			case MODE_IS:
				RuleMode mode = context.hero.buff(RuleMode.class);
				return mode != null && mode.modeId().equals(reference);
			case TARGET_OWNED:
				return resolvedTarget != null && RuleOwnership.isOwnedBy(resolvedTarget, context.hero);
			case LINK_EXISTS:
				if (resolvedTarget == null) return false;
				for (RuleRelation relation : resolvedTarget.buffs(RuleRelation.class)) {
					if (relation.relationType() == RuleRelation.Type.LINK
							&& (reference.isEmpty() || reference.equals(relation.capability()))) return true;
				}
				return false;
			case ALWAYS:
			default:
				return true;
		}
	}

	public boolean passes(RuleContext context, Char resolvedTarget) {
		return passes(context == null || context.hero == null ? null : context.hero.ruleRuntime(), context, resolvedTarget);
	}

	public String semanticId() {
		return type.name() + (parameter > 0 ? ":" + parameter : "")
				+ (reference.isEmpty() ? "" : ":" + reference);
	}

	@Override
	public int capacityCost() {
		return 0;
	}

	@Override
	public String description() {
		if (type == Type.SELF_HP_BELOW || type == Type.TARGET_HP_BELOW
				|| type == Type.DISTANCE_AT_LEAST || type == Type.ADJACENT_ENEMIES_AT_LEAST
				|| type == Type.RESOURCE_AT_LEAST) {
			return Messages.get(RuleCondition.class, type.name().toLowerCase(), parameter);
		}
		if ((type == Type.MODE_IS || type == Type.LINK_EXISTS || type == Type.RESOURCE_AT_LEAST)
				&& reference != null && !reference.isEmpty()) {
			return Messages.get(RuleCondition.class, type.name().toLowerCase() + "_bound", reference);
		}
		return Messages.get(RuleCondition.class, type.name().toLowerCase());
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("type", type);
		bundle.put("parameter", parameter);
		bundle.put("reference", reference);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		type = bundle.getEnum("type", Type.class);
		parameter = bundle.contains("parameter") ? bundle.getInt("parameter") : defaultParameter(type);
		reference = bundle.getString("reference");
		if (reference == null) reference = "";
	}
}
