package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ContractNodeSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;

/** Immutable SkillSpec v0.2 aggregate. Legacy envelopes never acquire executable fields. */
public final class SkillSpec implements StableTarget {
	public static final String VERSION = "0.2";
	public static final String VARIANT = "SKILL_V0_2";

	private final StableId id;
	private final DisplayName displayName;
	private final String variantKey;
	private final ImplementationState envelopeState;
	private final TriggerSpec activation;
	private final ConditionExpr condition;
	private final EffectChainSpec effects;
	private final DeliverySpec delivery;
	private final TargetingSpec targeting;
	private final ModifierSpec modifier;
	private final CostSpec cost;
	private final SkillConstraintSpec constraint;

	public SkillSpec(StableId id, DisplayName displayName, TriggerSpec activation,
			ConditionExpr condition, EffectChainSpec effects, DeliverySpec delivery,
			TargetingSpec targeting, ModifierSpec modifier, CostSpec cost,
			SkillConstraintSpec constraint) {
		if (id == null || displayName == null || activation == null || condition == null
				|| effects == null || delivery == null || targeting == null || cost == null) {
			throw new IllegalArgumentException("typed skill fields are required");
		}
		this.id = id;
		this.displayName = displayName;
		this.variantKey = VARIANT;
		this.envelopeState = null;
		this.activation = activation;
		this.condition = condition;
		this.effects = effects;
		this.delivery = delivery;
		this.targeting = targeting;
		this.modifier = modifier;
		this.cost = cost;
		this.constraint = constraint;
	}

	private SkillSpec(StableId id, DisplayName displayName, String variantKey,
			ImplementationState envelopeState) {
		if (id == null || displayName == null || variantKey == null || variantKey.isEmpty()
				|| envelopeState == null) throw new IllegalArgumentException("legacy skill envelope fields are required");
		this.id = id;
		this.displayName = displayName;
		this.variantKey = variantKey;
		this.envelopeState = envelopeState;
		this.activation = null;
		this.condition = null;
		this.effects = null;
		this.delivery = null;
		this.targeting = null;
		this.modifier = null;
		this.cost = null;
		this.constraint = null;
	}

	public static SkillSpec fromLegacyEnvelope(ContractNodeSpec value) {
		if (value == null || value.nodeKind() != ContractNodeSpec.NodeKind.SKILL) {
			throw new IllegalArgumentException("skill envelope is required");
		}
		return deferredEnvelope(value.id(), value.displayName(), value.variantKey(), value.implementationState());
	}
	public static SkillSpec deferredEnvelope(StableId id, DisplayName name, String variantKey,
			ImplementationState state) {
		if (state != ImplementationState.DEFERRED && state != ImplementationState.UNSUPPORTED) {
			throw new IllegalArgumentException("legacy skill envelope must be deferred or unsupported");
		}
		return new SkillSpec(id, name, variantKey, state);
	}

	@Override public StableId id() { return id; }
	@Override public DisplayName displayName() { return displayName; }
	@Override public RefKind refKind() { return null; }
	@Override public ImplementationState implementationState() {
		if (!typed()) return envelopeState;
		return supportsP03Slice() ? ImplementationState.IMPLEMENTED : ImplementationState.DECLARED;
	}
	public String variantKey() { return variantKey; }
	public boolean typed() { return VARIANT.equals(variantKey); }
	public TriggerSpec activation() { requireTyped(); return activation; }
	public ConditionExpr condition() { requireTyped(); return condition; }
	public EffectChainSpec effects() { requireTyped(); return effects; }
	public DeliverySpec delivery() { requireTyped(); return delivery; }
	public TargetingSpec targeting() { requireTyped(); return targeting; }
	public ModifierSpec modifier() { requireTyped(); return modifier; }
	public CostSpec cost() { requireTyped(); return cost; }
	public SkillConstraintSpec constraint() { requireTyped(); return constraint; }

	public SkillSpec withDisplayName(DisplayName value) {
		return typed() ? new SkillSpec(id, value, activation, condition, effects, delivery, targeting, modifier, cost, constraint)
				: deferredEnvelope(id, value, variantKey, envelopeState);
	}
	public SkillSpec withIdentity(StableId value) {
		return typed() ? new SkillSpec(value, displayName, activation, condition, effects, delivery, targeting, modifier, cost, constraint)
				: deferredEnvelope(value, displayName, variantKey, envelopeState);
	}
	public SkillSpec withActivation(TriggerSpec value) { return typedCopy(value, condition, effects, delivery, targeting, modifier, cost, constraint); }
	public SkillSpec withCondition(ConditionExpr value) { return typedCopy(activation, value, effects, delivery, targeting, modifier, cost, constraint); }
	public SkillSpec withEffects(EffectChainSpec value) { return typedCopy(activation, condition, value, delivery, targeting, modifier, cost, constraint); }
	public SkillSpec withDelivery(DeliverySpec value) { return typedCopy(activation, condition, effects, value, targeting, modifier, cost, constraint); }
	public SkillSpec withTargeting(TargetingSpec value) { return typedCopy(activation, condition, effects, delivery, value, modifier, cost, constraint); }
	public SkillSpec withModifier(ModifierSpec value) { return typedCopy(activation, condition, effects, delivery, targeting, value, cost, constraint); }
	public SkillSpec withCost(CostSpec value) { return typedCopy(activation, condition, effects, delivery, targeting, modifier, value, constraint); }
	public SkillSpec withConstraint(SkillConstraintSpec value) { return typedCopy(activation, condition, effects, delivery, targeting, modifier, cost, value); }

	private SkillSpec typedCopy(TriggerSpec a, ConditionExpr c, EffectChainSpec e, DeliverySpec d,
			TargetingSpec t, ModifierSpec m, CostSpec co, SkillConstraintSpec sc) {
		requireTyped();
		return new SkillSpec(id, displayName, a, c, e, d, t, m, co, sc);
	}
	private void requireTyped() {
		if (!typed()) throw new IllegalStateException("legacy skill envelope has no typed payload");
	}
	private boolean supportsP03Slice() {
		if (!(activation instanceof ActiveTriggerSpec) || !(condition instanceof AllOfCondition)
				|| !((AllOfCondition) condition).children().isEmpty()
				|| !(effects.primary() instanceof DirectDamageEffectSpec)
				|| !(delivery instanceof DirectDeliverySpec)
				|| !(targeting.selector() instanceof SelectedActorSelector)
				|| !(targeting.coverage() instanceof SingleCoverageSpec)
				|| !(targeting.filter() instanceof RelationFilterSpec)
				|| ((RelationFilterSpec) targeting.filter()).relationToClassOwner() != RelationFilterSpec.RelationAlignment.ENEMY
				|| ((RelationFilterSpec) targeting.filter()).includeSelf()
				|| modifier != null || !(cost instanceof NoCostSpec) || constraint != null) return false;
		DirectDamageEffectSpec damage = (DirectDamageEffectSpec) effects.primary();
		if (!(damage.amount() instanceof FixedValueSpec)
				|| damage.damageType() != DirectDamageEffectSpec.DamageType.UNTYPED
				|| damage.defensePolicy() != DirectDamageEffectSpec.NativeDefensePolicy.SPD_NATIVE) return false;
		if (effects.secondary() == null) return true;
		return effects.secondary().activation() instanceof ImmediateOnPrimarySuccess
				&& effects.secondary().effect() instanceof DirectDamageEffectSpec
				&& ((DirectDamageEffectSpec) effects.secondary().effect()).amount() instanceof FixedValueSpec
				&& ((DirectDamageEffectSpec) effects.secondary().effect()).damageType()
				== DirectDamageEffectSpec.DamageType.UNTYPED
				&& ((DirectDamageEffectSpec) effects.secondary().effect()).defensePolicy()
				== DirectDamageEffectSpec.NativeDefensePolicy.SPD_NATIVE;
	}
}
