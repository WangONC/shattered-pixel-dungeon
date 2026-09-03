package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class ApplyStatusEffectSpec implements EffectSpec {
	public enum StatusStackingPolicy { SPD_NATIVE, REPLACE, EXTEND, KEEP_LONGER }
	private final StableId effectId;private final StatusRef status;private final ValueSpec intensity;private final DurationSpec duration;private final StatusStackingPolicy stacking;
	public ApplyStatusEffectSpec(StableId effectId,StatusRef status,ValueSpec intensity,DurationSpec duration,StatusStackingPolicy stacking){if(effectId==null||status==null||intensity==null||duration==null||stacking==null)throw new IllegalArgumentException("status fields required");this.effectId=effectId;this.status=status;this.intensity=intensity;this.duration=duration;this.stacking=stacking;}
	@Override public StableId effectId(){return effectId;}@Override public EffectFamily family(){return EffectFamily.STATUS;}@Override public EffectVariantKey variantKey(){return EffectVariantKey.APPLY_STATUS;}public StatusRef status(){return status;}public ValueSpec intensity(){return intensity;}public DurationSpec duration(){return duration;}public StatusStackingPolicy stacking(){return stacking;}
}
