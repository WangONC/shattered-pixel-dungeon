package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class DirectDamageEffectSpec implements EffectSpec {
	public enum DamageType { UNTYPED, PHYSICAL, MAGICAL, FIRE, POISON, BLEEDING }
	public enum NativeDefensePolicy { SPD_NATIVE, IGNORE_ARMOR }

	private final StableId effectId;
	private final ValueSpec amount;
	private final DamageType damageType;
	private final NativeDefensePolicy defensePolicy;

	public DirectDamageEffectSpec(StableId effectId, ValueSpec amount, DamageType damageType,
			NativeDefensePolicy defensePolicy) {
		if (effectId == null || amount == null || damageType == null || defensePolicy == null) {
			throw new IllegalArgumentException("direct damage fields are required");
		}
		this.effectId = effectId;
		this.amount = amount;
		this.damageType = damageType;
		this.defensePolicy = defensePolicy;
	}
	@Override public StableId effectId() { return effectId; }
	@Override public EffectFamily family() { return EffectFamily.DAMAGE; }
	@Override public EffectVariantKey variantKey() { return EffectVariantKey.DIRECT_DAMAGE; }
	public ValueSpec amount() { return amount; }
	public DamageType damageType() { return damageType; }
	public NativeDefensePolicy defensePolicy() { return defensePolicy; }
	public DirectDamageEffectSpec withAmount(ValueSpec value) {
		return new DirectDamageEffectSpec(effectId, value, damageType, defensePolicy);
	}
	public DirectDamageEffectSpec withDamageType(DamageType value) {
		return new DirectDamageEffectSpec(effectId, amount, value, defensePolicy);
	}
	public DirectDamageEffectSpec withDefensePolicy(NativeDefensePolicy value) {
		return new DirectDamageEffectSpec(effectId, amount, damageType, value);
	}
}
