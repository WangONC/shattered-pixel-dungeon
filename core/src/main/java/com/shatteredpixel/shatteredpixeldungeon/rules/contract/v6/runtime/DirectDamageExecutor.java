package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.DirectDamageEffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.FixedValueSpec;

/** Invokes the real SPD Char.damage path under the explicit SPD_NATIVE policy. */
public final class DirectDamageExecutor implements EffectExecutor {
	@Override public EffectResult execute(EffectSpec raw, Char target, GameplayEventContext context, RuntimeTrace trace) {
		if (!(raw instanceof DirectDamageEffectSpec)) return EffectResult.failed(EffectResult.Status.UNSUPPORTED, "direct_damage.type_mismatch");
		DirectDamageEffectSpec effect = (DirectDamageEffectSpec) raw;
		if (!(effect.amount() instanceof FixedValueSpec)
				|| effect.defensePolicy() != DirectDamageEffectSpec.NativeDefensePolicy.SPD_NATIVE
				|| effect.damageType() != DirectDamageEffectSpec.DamageType.UNTYPED) {
			return EffectResult.failed(EffectResult.Status.UNSUPPORTED, "direct_damage.parameters_unsupported");
		}
		if (target == null || !target.isAlive()) return EffectResult.failed(EffectResult.Status.BLOCKED, "direct_damage.target_not_alive");
		int requested = ((FixedValueSpec) effect.amount()).value();
		int before = target.HP;
		target.damage(requested, new DamageSource(context.eventId(), effect.effectId().value()));
		int applied = Math.max(0, before - target.HP);
		trace.record("effect", "variant=DIRECT_DAMAGE effect=" + effect.effectId().value()
				+ " target_actor=" + target.id() + " target_cell=" + target.pos
				+ " requested=" + requested + " hp_before=" + before + " hp_after=" + target.HP
				+ " applied=" + applied + " defense=SPD_NATIVE");
		return EffectResult.applied(applied);
	}
	private static final class DamageSource {
		private final long eventId;
		private final String effectId;
		private DamageSource(long eventId, String effectId) { this.eventId = eventId; this.effectId = effectId; }
		@Override public String toString() { return "v6-direct-damage:" + eventId + ":" + effectId; }
	}
}
