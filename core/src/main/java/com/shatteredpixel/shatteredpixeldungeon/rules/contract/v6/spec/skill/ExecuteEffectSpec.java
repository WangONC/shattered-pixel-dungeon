package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class ExecuteEffectSpec implements EffectSpec {
	public enum ProtectedTargetPolicy { BLOCK, FALLBACK_DAMAGE }
	private final StableId effectId;private final int hpPercentThreshold;private final ValueSpec fallbackDamage;private final ProtectedTargetPolicy protectedTargetPolicy;
	public ExecuteEffectSpec(StableId effectId,int threshold,ValueSpec fallbackDamage,ProtectedTargetPolicy policy){if(effectId==null||policy==null||threshold<1||threshold>99)throw new IllegalArgumentException("invalid execute fields");if(policy==ProtectedTargetPolicy.FALLBACK_DAMAGE&&fallbackDamage==null)throw new IllegalArgumentException("fallback damage required by policy");this.effectId=effectId;this.hpPercentThreshold=threshold;this.fallbackDamage=fallbackDamage;this.protectedTargetPolicy=policy;}
	@Override public StableId effectId(){return effectId;}@Override public EffectFamily family(){return EffectFamily.DAMAGE;}@Override public EffectVariantKey variantKey(){return EffectVariantKey.EXECUTE;}public int hpPercentThreshold(){return hpPercentThreshold;}public ValueSpec fallbackDamage(){return fallbackDamage;}public ProtectedTargetPolicy protectedTargetPolicy(){return protectedTargetPolicy;}
}
