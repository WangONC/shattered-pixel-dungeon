package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Immutable runtime skill graph. No node retains an authoring-model object. */
public final class CompiledSkill {
	public enum TriggerVariant { ACTIVE }
	public enum ConditionVariant { ALWAYS }
	public enum DeliveryVariant { DIRECT }
	public enum SelectorVariant { SELECTED_ACTOR }
	public enum CoverageVariant { SINGLE }
	public enum FilterVariant { ENEMY_EXCLUDE_SELF }
	public enum LineOfSightPolicy { DELIVERY }
	public enum OrderingPolicy { DISTANCE_CELL_ACTOR_ID }
	public enum EffectVariant { DIRECT_DAMAGE }
	public enum DamageType { UNTYPED, PHYSICAL, MAGICAL, FIRE, POISON, BLEEDING }
	public enum DefensePolicy { SPD_NATIVE, IGNORE_ARMOR }
	public enum SecondaryActivation { IMMEDIATE_ON_PRIMARY_SUCCESS }
	public enum ModifierVariant { NONE }
	public enum CostVariant { NO_COST }
	public enum ConstraintVariant { NONE }

	public static final class Trigger {
		private final StableId nodeId; private final TriggerVariant variant;
		public Trigger(StableId nodeId, TriggerVariant variant) { require(nodeId, variant); this.nodeId=nodeId; this.variant=variant; }
		public StableId nodeId(){return nodeId;} public TriggerVariant variant(){return variant;}
	}
	public static final class Condition {
		private final ConditionVariant variant;
		public Condition(ConditionVariant variant){require(variant);this.variant=variant;}
		public ConditionVariant variant(){return variant;}
	}
	public static final class Delivery {
		private final StableId nodeId; private final DeliveryVariant variant; private final boolean requiresLineOfSight;
		public Delivery(StableId nodeId,DeliveryVariant variant,boolean requiresLineOfSight){require(nodeId,variant);this.nodeId=nodeId;this.variant=variant;this.requiresLineOfSight=requiresLineOfSight;}
		public StableId nodeId(){return nodeId;}public DeliveryVariant variant(){return variant;}public boolean requiresLineOfSight(){return requiresLineOfSight;}
	}
	public static final class Targeting {
		private final StableId nodeId; private final SelectorVariant selector; private final CoverageVariant coverage;
		private final FilterVariant filter; private final int range; private final int maximumTargets;
		private final LineOfSightPolicy lineOfSight; private final OrderingPolicy ordering;
		public Targeting(StableId nodeId,SelectorVariant selector,CoverageVariant coverage,FilterVariant filter,
				int range,int maximumTargets,LineOfSightPolicy lineOfSight,OrderingPolicy ordering){
			require(nodeId,selector,coverage,filter,lineOfSight,ordering);if(range<0||maximumTargets<1)throw new IllegalArgumentException("compiled targeting fields invalid");
			this.nodeId=nodeId;this.selector=selector;this.coverage=coverage;this.filter=filter;this.range=range;this.maximumTargets=maximumTargets;this.lineOfSight=lineOfSight;this.ordering=ordering;}
		public StableId nodeId(){return nodeId;}public SelectorVariant selector(){return selector;}public CoverageVariant coverage(){return coverage;}
		public FilterVariant filter(){return filter;}public int range(){return range;}public int maximumTargets(){return maximumTargets;}
		public LineOfSightPolicy lineOfSight(){return lineOfSight;}public OrderingPolicy ordering(){return ordering;}
	}
	public interface Effect { StableId effectId(); EffectVariant variant(); }
	public static final class DirectDamageEffect implements Effect {
		private final StableId effectId;private final int amount;private final DamageType damageType;private final DefensePolicy defensePolicy;
		public DirectDamageEffect(StableId effectId,int amount,DamageType damageType,DefensePolicy defensePolicy){require(effectId,damageType,defensePolicy);this.effectId=effectId;this.amount=amount;this.damageType=damageType;this.defensePolicy=defensePolicy;}
		@Override public StableId effectId(){return effectId;}@Override public EffectVariant variant(){return EffectVariant.DIRECT_DAMAGE;}
		public int amount(){return amount;}public DamageType damageType(){return damageType;}public DefensePolicy defensePolicy(){return defensePolicy;}
	}
	public static final class SecondaryEffect {
		private final Effect effect;private final SecondaryActivation activation;
		public SecondaryEffect(Effect effect,SecondaryActivation activation){require(effect,activation);this.effect=effect;this.activation=activation;}
		public Effect effect(){return effect;}public SecondaryActivation activation(){return activation;}
	}
	public static final class EffectChain {
		private final StableId chainId;private final Effect primary;private final SecondaryEffect secondary;
		public EffectChain(StableId chainId,Effect primary,SecondaryEffect secondary){require(chainId,primary);this.chainId=chainId;this.primary=primary;this.secondary=secondary;}
		public StableId chainId(){return chainId;}public Effect primary(){return primary;}public SecondaryEffect secondary(){return secondary;}
	}
	public static final class Modifier {
		private final ModifierVariant variant;public Modifier(ModifierVariant variant){require(variant);this.variant=variant;}public ModifierVariant variant(){return variant;}
	}
	public static final class Cost {
		private final StableId nodeId;private final CostVariant variant;public Cost(StableId nodeId,CostVariant variant){require(nodeId,variant);this.nodeId=nodeId;this.variant=variant;}
		public StableId nodeId(){return nodeId;}public CostVariant variant(){return variant;}
	}
	public static final class Constraint {
		private final ConstraintVariant variant;public Constraint(ConstraintVariant variant){require(variant);this.variant=variant;}public ConstraintVariant variant(){return variant;}
	}

	private final StableId skillId;private final Trigger trigger;private final Condition condition;private final Delivery delivery;
	private final Targeting targeting;private final EffectChain effectChain;private final Modifier modifier;private final Cost cost;private final Constraint constraint;
	CompiledSkill(StableId skillId,Trigger trigger,Condition condition,Delivery delivery,Targeting targeting,
			EffectChain effectChain,Modifier modifier,Cost cost,Constraint constraint){require(skillId,trigger,condition,delivery,targeting,effectChain,modifier,cost,constraint);
		this.skillId=skillId;this.trigger=trigger;this.condition=condition;this.delivery=delivery;this.targeting=targeting;this.effectChain=effectChain;this.modifier=modifier;this.cost=cost;this.constraint=constraint;}
	public StableId skillId(){return skillId;}public Trigger trigger(){return trigger;}public Condition condition(){return condition;}
	public Delivery delivery(){return delivery;}public Targeting targeting(){return targeting;}public EffectChain effectChain(){return effectChain;}
	public Modifier modifier(){return modifier;}public Cost cost(){return cost;}public Constraint constraint(){return constraint;}
	private static void require(Object...values){for(Object value:values)if(value==null)throw new IllegalArgumentException("compiled node fields required");}
}
