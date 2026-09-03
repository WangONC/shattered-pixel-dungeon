package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class PercentMaxHpDamageEffectSpec implements EffectSpec {
	public enum ProtectedTargetPolicy { BLOCK, APPLY_CAPPED_DAMAGE }
	private final StableId effectId;private final int percent,absoluteCap;private final ProtectedTargetPolicy protectedTargetPolicy;
	public PercentMaxHpDamageEffectSpec(StableId effectId,int percent,int absoluteCap,ProtectedTargetPolicy policy){if(effectId==null||policy==null||percent<1||percent>100||absoluteCap<1)throw new IllegalArgumentException("invalid percent max HP damage fields");this.effectId=effectId;this.percent=percent;this.absoluteCap=absoluteCap;this.protectedTargetPolicy=policy;}
	@Override public StableId effectId(){return effectId;}@Override public EffectFamily family(){return EffectFamily.DAMAGE;}@Override public EffectVariantKey variantKey(){return EffectVariantKey.PERCENT_MAX_HP_DAMAGE;}public int percent(){return percent;}public int absoluteCap(){return absoluteCap;}public ProtectedTargetPolicy protectedTargetPolicy(){return protectedTargetPolicy;}
}
