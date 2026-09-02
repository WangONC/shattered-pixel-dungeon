package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Callback;

import java.util.ArrayList;
import java.util.EnumSet;

/** Generic effect implementation which delegates to existing SPD gameplay primitives. */
public class RuleEffect implements RuleModule, Bundlable {
	public enum Type {
		PUSH, POISON, FIRE,
		PULL, TELEPORT, SWAP_POSITION, BLEED, SLOW, HASTE, HEAL, SHIELD, CLEANSE,
		CREATE_WATER, CREATE_GAS
	}

	public Type type = Type.PUSH;
	public int power = 2;

	public RuleEffect() {}

	public RuleEffect(Type type, int power) {
		this.type = type;
		this.power = power;
	}

	public EnumSet<RuleSemanticTag> tags() {
		switch (type) {
			case PUSH:
			case PULL:
				return EnumSet.of(RuleSemanticTag.FORCED_MOVEMENT);
			case TELEPORT:
			case SWAP_POSITION:
				return EnumSet.of(RuleSemanticTag.MOVEMENT, RuleSemanticTag.TRANSLOCATION);
			case POISON:
			case FIRE:
			case BLEED:
			case CREATE_GAS:
				return EnumSet.of(RuleSemanticTag.STATUS_APPLICATION, RuleSemanticTag.DAMAGE_OVER_TIME);
			case SLOW:
			case HASTE:
				return EnumSet.of(RuleSemanticTag.STATUS_APPLICATION);
			case HEAL:
				return EnumSet.of(RuleSemanticTag.RECOVERY);
			case SHIELD:
				return EnumSet.of(RuleSemanticTag.PROTECTION);
			case CLEANSE:
				return EnumSet.of(RuleSemanticTag.STATUS_REMOVAL);
			case CREATE_WATER:
				return EnumSet.of(RuleSemanticTag.TERRAIN_CHANGE);
			default:
				return EnumSet.noneOf(RuleSemanticTag.class);
		}
	}

	public boolean hasTag(RuleSemanticTag tag) {
		return tag != null && tags().contains(tag);
	}

	public String semanticId() {
		return type.name();
	}

	public RuleEffect copy() {
		return new RuleEffect(type, power);
	}

	public RuleEffect scaled(float multiplier) {
		return new RuleEffect(type, Math.max(1, Math.round(power * Math.max(0.1f, multiplier))));
	}

	public boolean supportsPowerScaling() {
		return type != Type.TELEPORT && type != Type.SWAP_POSITION && type != Type.CREATE_WATER;
	}

	/** Reject combinations which can never produce a meaningful target. */
	public static boolean compatibleTarget(Type effect, RuleTarget.Type target) {
		if (effect == null || target == null) return false;
		switch (effect) {
			case TELEPORT:
				return target == RuleTarget.Type.SELECTED_CELL;
			case PUSH:
			case PULL:
			case SWAP_POSITION:
				return target != RuleTarget.Type.SELF && target != RuleTarget.Type.CURRENT_TILE;
			default:
				return true;
		}
	}

	public boolean apply(RuleContext context, Char target, int cell, RuleModifier modifier) {
		if (context == null || context.hero == null || modifier == null || !modifier.compatible(type)) return false;
		int modifiedPower = Math.max(1, Math.round(power * modifier.powerMultiplier));
		float duration = Math.max(2f, modifiedPower * modifier.durationMultiplier);
		boolean applied;
		switch (type) {
			case PUSH:
				applied = throwTarget(context, target, modifiedPower, false);
				break;
			case PULL:
				applied = throwTarget(context, target, modifiedPower, true);
				break;
			case POISON:
				if (target == null) return false;
				Poison oldPoison = target.buff(Poison.class);
				Poison poison;
				RuleHooks.beginRuleStatusApplication();
				try {
					poison = Buff.affect(target, Poison.class);
					poison.set(Math.round(duration));
				} finally {
					RuleHooks.endRuleStatusApplication();
				}
				if (oldPoison == null && poison.target == target) RuleHooks.onRuleStatusApplied(context, target, poison);
				RulePresentation.burst(target.pos, 4, true);
				applied = true;
				break;
			case FIRE:
				if (!validCell(cell)) return false;
				GameScene.add(Blob.seed(cell, Math.max(2, modifiedPower), Fire.class));
				if (target != null) {
					Burning oldBurning = target.buff(Burning.class);
					Burning burning;
					RuleHooks.beginRuleStatusApplication();
					try {
						burning = Buff.affect(target, Burning.class);
						burning.reignite(target, duration);
					} finally {
						RuleHooks.endRuleStatusApplication();
					}
					if (oldBurning == null && burning.target == target) RuleHooks.onRuleStatusApplied(context, target, burning);
				}
				RulePresentation.burst(cell, 6, false);
				RulePresentation.burningSound();
				applied = true;
				break;
			case TELEPORT:
				if (!validDestination(cell) || Char.hasProp(context.hero, Char.Property.IMMOVABLE)) return false;
				int teleportFrom = context.hero.pos;
				ScrollOfTeleportation.appear(context.hero, cell);
				Dungeon.level.occupyCell(context.hero);
				Dungeon.observe();
				GameScene.updateFog();
				RuleHooks.emitSemantic(context, RuleSemanticTag.MOVEMENT, context.hero, teleportFrom, cell);
				RuleHooks.emitSemantic(context, RuleSemanticTag.TRANSLOCATION, context.hero, teleportFrom, cell);
				applied = true;
				break;
			case SWAP_POSITION:
				if (target == null || target == context.hero || Dungeon.level == null) return false;
				int heroPos = context.hero.pos;
				int targetPos = target.pos;
				target.interact(context.hero);
				applied = context.hero.pos == targetPos && target.pos == heroPos;
				if (applied) {
					RuleHooks.emitSemantic(context, RuleSemanticTag.MOVEMENT, context.hero, heroPos, context.hero.pos);
					RuleHooks.emitSemantic(context, RuleSemanticTag.TRANSLOCATION, context.hero, heroPos, context.hero.pos);
				}
				break;
			case BLEED:
				if (target == null) return false;
				Bleeding oldBleeding = target.buff(Bleeding.class);
				Bleeding bleeding;
				RuleHooks.beginRuleStatusApplication();
				try {
					bleeding = Buff.affect(target, Bleeding.class);
					bleeding.set(Math.max(2, Math.round(duration)));
				} finally {
					RuleHooks.endRuleStatusApplication();
				}
				if (oldBleeding == null && bleeding.target == target) RuleHooks.onRuleStatusApplied(context, target, bleeding);
				applied = true;
				break;
			case SLOW:
				if (target == null) return false;
				Slow oldSlow = target.buff(Slow.class);
				Slow slow;
				RuleHooks.beginRuleStatusApplication();
				try {
					slow = Buff.prolong(target, Slow.class, duration);
				} finally {
					RuleHooks.endRuleStatusApplication();
				}
				if (oldSlow == null && slow.target == target) RuleHooks.onRuleStatusApplied(context, target, slow);
				applied = true;
				break;
			case HASTE:
				if (target == null) return false;
				Haste oldHaste = target.buff(Haste.class);
				Haste haste;
				RuleHooks.beginRuleStatusApplication();
				try {
					haste = Buff.prolong(target, Haste.class, duration);
				} finally {
					RuleHooks.endRuleStatusApplication();
				}
				if (oldHaste == null && haste.target == target) RuleHooks.onRuleStatusApplied(context, target, haste);
				applied = true;
				break;
			case HEAL:
				if (target == null) target = context.hero;
				if (target instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero) {
					applied = RuleHooks.applyHealing((com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero) target,
							Math.max(2, modifiedPower * 2));
				} else {
					int before = target.HP;
					target.HP = Math.min(target.HT, target.HP + Math.max(2, modifiedPower * 2));
					applied = target.HP > before;
				}
				break;
			case SHIELD:
				if (target == null) target = context.hero;
				Barrier oldBarrier = target.buff(Barrier.class);
				Barrier barrier;
				RuleHooks.beginRuleStatusApplication();
				try {
					barrier = Buff.affect(target, Barrier.class);
					barrier.incShield(Math.max(2, modifiedPower * 2));
				} finally {
					RuleHooks.endRuleStatusApplication();
				}
				if (oldBarrier == null && barrier.target == target) RuleHooks.onRuleStatusApplied(context, target, barrier);
				applied = true;
				break;
			case CLEANSE:
				if (target == null) target = context.hero;
				applied = false;
				for (Buff buff : new ArrayList<>(target.buffs())) {
					if (buff.type == Buff.buffType.NEGATIVE) {
						buff.detach();
						applied = true;
					}
				}
				break;
			case CREATE_WATER:
				if (!WorldCapabilityValidator.supports(Dungeon.level, cell,
						WorldCapability.REPLACEABLE)) return false;
				applied = Dungeon.level.setCellToWater(true, cell);
				break;
			case CREATE_GAS:
				if (!WorldCapabilityValidator.supports(Dungeon.level, cell,
						WorldCapability.BLOB_SEEDABLE)) return false;
				GameScene.add(Blob.seed(cell, Math.max(10, Math.round(duration * 5)), ToxicGas.class));
				RulePresentation.burst(cell, 5, false);
				applied = true;
				break;
			default:
				return false;
		}

		if (!applied) return false;
		RuleHooks.onEffectApplied(context, this, target, cell);
		RulePresentation.triggered(context, this);
		return true;
	}

	private static boolean throwTarget(RuleContext context, Char target, int power, boolean pull) {
		if (target == null || target == context.hero || Dungeon.level == null) return false;
		int width = Dungeon.level.width();
		int dx = Integer.signum((pull ? context.hero.pos : target.pos) % width
				- (pull ? target.pos : context.hero.pos) % width);
		int dy = Integer.signum((pull ? context.hero.pos : target.pos) / width
				- (pull ? target.pos : context.hero.pos) / width);
		if (dx == 0 && dy == 0) return false;
		Ballistica trajectory = new Ballistica(target.pos, target.pos + dx + dy * width, Ballistica.MAGIC_BOLT);
		RulePresentation.blast(target.pos);
		final int from = target.pos;
		WandOfBlastWave.throwChar(target, trajectory, power, false, false, context.hero, new Callback() {
			@Override
			public void call() {
				RuleHooks.onForcedMovement(context, target, from, target.pos);
			}
		});
		RulePresentation.blastSound();
		return true;
	}

	private static boolean validCell(int cell) {
		return Dungeon.level != null && cell >= 0 && cell < Dungeon.level.length() && !Dungeon.level.solid[cell];
	}

	private static boolean validDestination(int cell) {
		return validCell(cell) && Dungeon.level.passable[cell] && Actor.findChar(cell) == null;
	}

	@Override
	public int capacityCost() {
		switch (type) {
			case TELEPORT:
			case SWAP_POSITION:
			case CREATE_WATER:
			case CREATE_GAS:
			case FIRE: return 3;
			case CLEANSE:
			case HEAL:
			case SHIELD:
			case PUSH:
			case PULL:
			case POISON:
			case BLEED:
			case SLOW:
			case HASTE:
			default: return 2;
		}
	}

	@Override
	public String description() {
		return Messages.get(RuleEffect.class, type.name().toLowerCase());
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("type", type);
		bundle.put("power", power);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		type = bundle.getEnum("type", Type.class);
		power = bundle.getInt("power");
	}
}
