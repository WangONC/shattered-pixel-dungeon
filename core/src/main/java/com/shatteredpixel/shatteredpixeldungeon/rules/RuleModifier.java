package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** A generic execution-shape modifier. Compatibility is checked before a rule can fire. */
public class RuleModifier implements RuleModule, Bundlable {
	public enum Type { NONE, AREA, REPEAT, EXTEND_DURATION, INTENSITY, PIERCE, BOUNCE, DELAY }

	public Type type = Type.NONE;
	public int magnitude;
	public float powerMultiplier = 1f;
	public float durationMultiplier = 1f;
	public float cooldown;

	public RuleModifier() {}

	public RuleModifier(Type type) {
		this.type = type;
		switch (type) {
			case AREA: magnitude = 1; break;
			case REPEAT: magnitude = 2; break;
			case EXTEND_DURATION:
				magnitude = 2;
				durationMultiplier = 2f;
				break;
			case INTENSITY:
				magnitude = 2;
				powerMultiplier = 1.5f;
				break;
			case PIERCE:
			case BOUNCE:
			case DELAY: magnitude = 2; break;
			default: magnitude = 0;
		}
	}

	public int repeats() {
		return type == Type.REPEAT ? Math.max(2, magnitude) : 1;
	}

	public int radius() {
		return type == Type.AREA ? Math.max(1, magnitude) : 0;
	}

	public int pierces() { return type == Type.PIERCE ? Math.max(1, magnitude) : 0; }
	public int bounces() { return type == Type.BOUNCE ? Math.max(1, magnitude) : 0; }
	public int delayTurns() { return type == Type.DELAY ? Math.max(1, magnitude) : 0; }

	public RuleModifier copy() {
		RuleModifier result = new RuleModifier();
		result.type = type;
		result.magnitude = magnitude;
		result.powerMultiplier = powerMultiplier;
		result.durationMultiplier = durationMultiplier;
		result.cooldown = cooldown;
		return result;
	}

	public boolean compatible(RuleEffect.Type effect) {
		if (type == Type.REPEAT && (effect == RuleEffect.Type.TELEPORT
				|| effect == RuleEffect.Type.SWAP_POSITION)) return false;
		if (type == Type.EXTEND_DURATION) {
			return effect == RuleEffect.Type.POISON || effect == RuleEffect.Type.FIRE
					|| effect == RuleEffect.Type.BLEED || effect == RuleEffect.Type.SLOW
					|| effect == RuleEffect.Type.HASTE || effect == RuleEffect.Type.CREATE_GAS;
		}
		if (type == Type.BOUNCE && (effect == RuleEffect.Type.TELEPORT
				|| effect == RuleEffect.Type.SWAP_POSITION)) return false;
		return true;
	}

	public boolean compatible(EffectSpec effect, SkillDelivery delivery) {
		if (effect == null || !effect.configurationValid() || !compatible(effect.compile().type)) return false;
		if (type == Type.PIERCE) return delivery == SkillDelivery.PROJECTILE || delivery == SkillDelivery.TRACE_BEAM;
		if (type == Type.BOUNCE) return delivery == SkillDelivery.DIRECT_TARGET
				|| delivery == SkillDelivery.PROJECTILE || delivery == SkillDelivery.TRACE_BEAM;
		if (type == Type.EXTEND_DURATION) return effect.operation != EffectSpec.Operation.DAMAGE_STANDARD
				&& effect.operation != EffectSpec.Operation.DAMAGE_PERCENT
				&& effect.operation != EffectSpec.Operation.DAMAGE_MISSING_HP
				&& effect.operation != EffectSpec.Operation.DAMAGE_EXECUTE;
		return true;
	}

	@Override
	public int capacityCost() {
		switch (type) {
			case AREA: return 2;
			case REPEAT: return 2 + Math.max(0, magnitude - 1) / 2;
			case EXTEND_DURATION: return 1;
			case INTENSITY: return 2;
			case PIERCE: return 3 + Math.max(0, magnitude - 2);
			case BOUNCE: return 3 + Math.max(0, magnitude - 2);
			case DELAY: return 1;
			case NONE:
			default: return powerMultiplier > 1f || durationMultiplier > 1f ? 1 : 0;
		}
	}

	@Override
	public String description() {
		if (type != Type.NONE) {
			return Messages.get(RuleModifier.class, type.name().toLowerCase(), magnitude);
		}
		return Messages.get(RuleModifier.class, "none");
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("type", type);
		bundle.put("magnitude", magnitude);
		bundle.put("power", powerMultiplier);
		bundle.put("duration", durationMultiplier);
		bundle.put("cooldown", cooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		type = bundle.contains("type") ? bundle.getEnum("type", Type.class) : Type.NONE;
		magnitude = bundle.getInt("magnitude");
		powerMultiplier = bundle.getFloat("power");
		durationMultiplier = bundle.getFloat("duration");
		cooldown = bundle.getFloat("cooldown");
		if (powerMultiplier <= 0) powerMultiplier = 1f;
		if (durationMultiplier <= 0) durationMultiplier = type == Type.EXTEND_DURATION ? 2f : 1f;
		if (magnitude <= 0 && type != Type.NONE) magnitude = type == Type.AREA ? 1 : 2;
	}
}
