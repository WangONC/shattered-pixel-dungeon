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
		return supportsP04Slice() ? ImplementationState.IMPLEMENTED : ImplementationState.DECLARED;
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
	private boolean supportsP04Slice() {
		if (!(activation instanceof ActiveTriggerSpec)||!(condition instanceof AllOfCondition)||constraint!=null)return false;
		for(ConditionExpr child:((AllOfCondition)condition).children())if(!(child instanceof AlwaysCondition||child instanceof BuiltinStatCompareCondition||child instanceof ResourceCompareCondition))return false;
		if(!effectSupported(effects.primary()))return false;
		if(effects.secondary()!=null&&(!(effects.secondary().activation() instanceof ImmediateOnPrimarySuccess)||!effectSupported(effects.secondary().effect())))return false;
		if(!(delivery instanceof DirectDeliverySpec||delivery instanceof SelfDeliverySpec||delivery instanceof ContactDeliverySpec||delivery instanceof ProjectileDeliverySpec||delivery instanceof TraceDeliverySpec||delivery instanceof GroundDeliverySpec))return false;
		if(!(targeting.selector() instanceof SelectedActorSelector||targeting.selector() instanceof SelfSelector||targeting.selector() instanceof SelectedCellSelector))return false;
		if(!(targeting.coverage() instanceof SingleCoverageSpec||targeting.coverage() instanceof AdjacentCoverageSpec||targeting.coverage() instanceof RadiusCoverageSpec||targeting.coverage() instanceof LineCoverageSpec))return false;
		if(!(targeting.filter() instanceof RelationFilterSpec||targeting.filter() instanceof AnyActorFilterSpec||targeting.filter() instanceof SelfFilterSpec))return false;
		if(modifier!=null&&!(modifier instanceof RepeatModifierSpec||modifier instanceof IntensityModifierSpec||modifier instanceof ExtendDurationModifierSpec||modifier instanceof PierceModifierSpec||modifier instanceof BounceModifierSpec))return false;
		return cost instanceof NoCostSpec||cost instanceof ResourceCostSpec||cost instanceof HpCostSpec||cost instanceof ActionTimeCostSpec||cost instanceof CooldownCostSpec||cost instanceof ItemCostSpec;
	}
	private static boolean effectSupported(EffectSpec effect){
		if(effect instanceof DirectDamageEffectSpec)return ((DirectDamageEffectSpec)effect).defensePolicy()==DirectDamageEffectSpec.NativeDefensePolicy.SPD_NATIVE;
		return effect instanceof PercentMaxHpDamageEffectSpec||effect instanceof MissingHpDamageEffectSpec||effect instanceof ExecuteEffectSpec||effect instanceof ApplyStatusEffectSpec||effect instanceof PushEffectSpec||effect instanceof PullEffectSpec||effect instanceof ThrowEffectSpec||effect instanceof DashEffectSpec||effect instanceof TeleportEffectSpec||effect instanceof SwapPositionEffectSpec||effect instanceof HealEffectSpec||effect instanceof BarrierEffectSpec||effect instanceof TemporaryHpEffectSpec||effect instanceof MitigateEffectSpec||effect instanceof RedirectDamageEffectSpec||effect instanceof CleanseEffectSpec||effect instanceof ResourceOperationEffectSpec;
	}
}
