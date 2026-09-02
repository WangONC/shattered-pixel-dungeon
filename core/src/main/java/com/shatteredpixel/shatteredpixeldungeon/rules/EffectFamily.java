package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Player-facing effect taxonomy. Concrete execution remains in {@link RuleEffect}. */
public enum EffectFamily {
	DAMAGE,
	STATUS,
	MOVEMENT,
	RECOVERY_DEFENSE,
	RESOURCE_OPERATION,
	MARK_ACCUMULATION,
	CREATE_ENTITY,
	WORLD_TERRAIN,
	RELATION_CONTROL,
	TRANSFER_COPY,
	TRANSFORM;

	public String displayName() {
		return Messages.get(EffectFamily.class, name().toLowerCase());
	}

	public static EffectFamily forEffect(RuleEffect.Type type) {
		if (type == null) return DAMAGE;
		switch (type) {
			case POISON:
			case FIRE:
			case BLEED:
			case SLOW:
			case HASTE:
			case CREATE_GAS:
				return STATUS;
			case PUSH:
			case PULL:
			case TELEPORT:
			case SWAP_POSITION:
				return MOVEMENT;
			case HEAL:
			case SHIELD:
			case CLEANSE:
				return RECOVERY_DEFENSE;
			case CREATE_WATER:
				return WORLD_TERRAIN;
			default:
				return DAMAGE;
		}
	}
}
