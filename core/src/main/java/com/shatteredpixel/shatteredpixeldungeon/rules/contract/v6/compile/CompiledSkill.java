package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.GameplayEventContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable runtime skill graph. It contains no authoring-model field types. */
public final class CompiledSkill {
	public enum TriggerVariant { ACTIVE, EVENT }
	public enum ConditionVariant { ALWAYS, ALL_OF, BUILTIN_STAT_COMPARE, RESOURCE_COMPARE }
	public enum DeliveryVariant { SELF, CONTACT, DIRECT, PROJECTILE, TRACE, GROUND }
	public enum SelectorVariant { SELF, SELECTED_ACTOR, SELECTED_CELL }
	public enum CoverageVariant { SINGLE, ADJACENT, RADIUS, LINE }
	public enum FilterVariant { ANY_ACTOR, SELF, ENEMY_EXCLUDE_SELF, ALLY_EXCLUDE_SELF, ALLY_INCLUDE_SELF }
	public enum LineOfSightPolicy { DELIVERY }
	public enum OrderingPolicy { DISTANCE_CELL_ACTOR_ID }
	public enum EffectVariant { DIRECT_DAMAGE,PERCENT_MAX_HP_DAMAGE,MISSING_HP_DAMAGE,EXECUTE,APPLY_STATUS,PUSH,PULL,THROW,DASH,TELEPORT,SWAP_POSITION,HEAL,BARRIER,TEMPORARY_HP,MITIGATE,REDIRECT_DAMAGE,CLEANSE,RESOURCE_OPERATION }
	public enum DamageType { UNTYPED, PHYSICAL, MAGICAL, FIRE, POISON, BLEEDING }
	public enum DefensePolicy { SPD_NATIVE, IGNORE_ARMOR }
	public enum SecondaryActivation { IMMEDIATE_ON_PRIMARY_SUCCESS }
	public enum ModifierVariant { NONE, REPEAT, INTENSITY, EXTEND_DURATION, PIERCE, BOUNCE }
	public enum CostVariant { NO_COST, RESOURCE, HP, ACTION_TIME, COOLDOWN, ITEM }
	public enum ConstraintVariant { NONE }
	public enum ResourceOperationVariant { GAIN, DRAIN, SET, CLEAR, CONVERT, RESERVE, SUPPRESS }
	public enum ValueVariant { FIXED, SCALED }
	public enum ValueSourceVariant { BUILTIN_STAT, RESOURCE }
	public enum Subject { CLASS_OWNER, EVENT_SOURCE, EVENT_TARGET, SELECTED_ACTOR }
	public enum BuiltinStat { HP_CURRENT,HP_MAX,HP_MISSING,HP_PERCENT,BARRIER,TEMPORARY_HP,HERO_LEVEL,DAMAGE_TAKEN_CURRENT_EVENT,DISTANCE_CURRENT_EVENT }
	public enum Comparison { LT,LTE,EQ,GTE,GT }
	public enum ProtectedTargetPolicy { BLOCK, APPLY_CAPPED_DAMAGE, FALLBACK_DAMAGE }
	public enum Status { POISON,BURNING,BLEEDING,SLOW,HASTE,PARALYSIS,ROOTS,AMOK,TERROR,VULNERABLE }
	public enum DurationKind { INSTANT,TURN_BASED,UNTIL_LEVEL_END,UNTIL_REMOVED }
	public enum StatusStacking { SPD_NATIVE,REPLACE,EXTEND,KEEP_LONGER }
	public enum CollisionPolicy { STOP_BEFORE_BLOCKED,SPD_NATIVE_COLLISION }
	public enum PathPolicy { REQUIRE_CLEAR_PATH,ALLOW_PASSABLE_PATH }
	public enum DestinationPolicy { EXACT_CELL_OR_FAIL,NEAREST_VALID_CELL }
	public enum SwapLegalityPolicy { BOTH_CELLS_PASSABLE }
	public enum BarrierOverflowPolicy { CLAMP_TO_MAX_HP,ALLOW_OVERFLOW }
	public enum TemporaryHpStacking { MAX,ADD,REPLACE }
	public enum StatusFilterKind { SPECIFIC,ANY_NEGATIVE }
	public enum ResourceOverflowPolicy { FAIL,CLAMP,DISCARD_EXCESS }
	public enum InsufficientResourcePolicy { FAIL,DRAIN_AVAILABLE }
	public enum ConversionPolicy { EXACT_ATOMIC }
	public enum ReservationExpiryPolicy { RETURN_TO_HOLDER }
	public enum ResourceSuppressionMode { BLOCK_GAIN }
	public enum HpLethalPolicy { REJECT_IF_WOULD_KILL,ALLOW_LETHAL }
	public enum ItemCategory { ANY_WEAPON,ANY_CONSUMABLE,SCROLL,POTION,SEED,RUNESTONE }

	public static final class Trigger {
		private final StableId nodeId; private final TriggerVariant variant; private final GameplayEventContext.RuleEventType event;
		public Trigger(StableId id,TriggerVariant variant){this(id,variant,null);}
		public Trigger(StableId id,TriggerVariant variant,GameplayEventContext.RuleEventType event){require(id,variant);nodeId=id;this.variant=variant;this.event=event;}
		public StableId nodeId(){return nodeId;}public TriggerVariant variant(){return variant;}public GameplayEventContext.RuleEventType event(){return event;}
	}
	public static final class ValueSource {
		private final ValueSourceVariant variant;private final Subject subject;private final BuiltinStat stat;private final StableId resourceId;
		public ValueSource(ValueSourceVariant variant,Subject subject,BuiltinStat stat,StableId resourceId){require(variant);this.variant=variant;this.subject=subject;this.stat=stat;this.resourceId=resourceId;}
		public ValueSourceVariant variant(){return variant;}public Subject subject(){return subject;}public BuiltinStat stat(){return stat;}public StableId resourceId(){return resourceId;}
	}
	public static final class Value {
		private final ValueVariant variant;private final int fixed,base,numerator,denominator,minimum,maximum;private final ValueSource source;
		public static Value fixed(int value){return new Value(ValueVariant.FIXED,value,0,0,1,0,0,null);}
		public static Value scaled(int base,ValueSource source,int numerator,int denominator,int minimum,int maximum){return new Value(ValueVariant.SCALED,0,base,numerator,denominator,minimum,maximum,source);}
		private Value(ValueVariant variant,int fixed,int base,int numerator,int denominator,int minimum,int maximum,ValueSource source){require(variant);if(variant==ValueVariant.SCALED)require(source);this.variant=variant;this.fixed=fixed;this.base=base;this.numerator=numerator;this.denominator=denominator;this.minimum=minimum;this.maximum=maximum;this.source=source;}
		public ValueVariant variant(){return variant;}public int fixed(){return fixed;}public int base(){return base;}public int numerator(){return numerator;}public int denominator(){return denominator;}public int minimum(){return minimum;}public int maximum(){return maximum;}public ValueSource source(){return source;}
	}
	public static final class Duration {
		private final DurationKind kind;private final int turns;public Duration(DurationKind kind,int turns){require(kind);this.kind=kind;this.turns=turns;}public DurationKind kind(){return kind;}public int turns(){return turns;}
	}
	public static final class Condition {
		private final ConditionVariant variant;private final List<Condition> children;private final StableId nodeId;private final Subject subject;private final BuiltinStat stat;private final Comparison comparison;private final Value value;private final StableId resourceId;
		public Condition(ConditionVariant variant){this(variant,Collections.<Condition>emptyList(),null,null,null,null,null,null);}
		public static Condition allOf(List<Condition> children){return new Condition(ConditionVariant.ALL_OF,children,null,null,null,null,null,null);}
		public static Condition builtin(StableId id,Subject subject,BuiltinStat stat,Comparison comparison,Value value){return new Condition(ConditionVariant.BUILTIN_STAT_COMPARE,Collections.<Condition>emptyList(),id,subject,stat,comparison,value,null);}
		public static Condition resource(StableId id,StableId resource,Comparison comparison,int value){return new Condition(ConditionVariant.RESOURCE_COMPARE,Collections.<Condition>emptyList(),id,null,null,comparison,Value.fixed(value),resource);}
		private Condition(ConditionVariant variant,List<Condition> children,StableId id,Subject subject,BuiltinStat stat,Comparison comparison,Value value,StableId resourceId){require(variant,children);this.variant=variant;this.children=Collections.unmodifiableList(new ArrayList<>(children));nodeId=id;this.subject=subject;this.stat=stat;this.comparison=comparison;this.value=value;this.resourceId=resourceId;}
		public ConditionVariant variant(){return variant;}public List<Condition> children(){return children;}public StableId nodeId(){return nodeId;}public Subject subject(){return subject;}public BuiltinStat stat(){return stat;}public Comparison comparison(){return comparison;}public Value value(){return value;}public StableId resourceId(){return resourceId;}
	}
	public static final class Delivery {
		private final StableId nodeId;private final DeliveryVariant variant;private final boolean flag;private final int first,second;private final String presentationKey;
		public Delivery(StableId id,DeliveryVariant variant,boolean flag){this(id,variant,flag,0,0,"");}
		public Delivery(StableId id,DeliveryVariant variant,boolean flag,int first,int second,String key){require(id,variant,key);nodeId=id;this.variant=variant;this.flag=flag;this.first=first;this.second=second;presentationKey=key;}
		public StableId nodeId(){return nodeId;}public DeliveryVariant variant(){return variant;}public boolean requiresLineOfSight(){return flag;}public boolean flag(){return flag;}public int first(){return first;}public int second(){return second;}public String presentationKey(){return presentationKey;}
	}
	public static final class Targeting {
		private final StableId nodeId;private final SelectorVariant selector;private final CoverageVariant coverage;private final FilterVariant filter;private final int range,maximumTargets,coverageFirst,coverageSecond;private final LineOfSightPolicy lineOfSight;private final OrderingPolicy ordering;
		public Targeting(StableId id,SelectorVariant selector,CoverageVariant coverage,FilterVariant filter,int range,int max,LineOfSightPolicy los,OrderingPolicy order){this(id,selector,coverage,filter,range,max,los,order,0,0);}
		public Targeting(StableId id,SelectorVariant selector,CoverageVariant coverage,FilterVariant filter,int range,int max,LineOfSightPolicy los,OrderingPolicy order,int first,int second){require(id,selector,coverage,filter,los,order);nodeId=id;this.selector=selector;this.coverage=coverage;this.filter=filter;this.range=range;maximumTargets=max;lineOfSight=los;ordering=order;coverageFirst=first;coverageSecond=second;}
		public StableId nodeId(){return nodeId;}public SelectorVariant selector(){return selector;}public CoverageVariant coverage(){return coverage;}public FilterVariant filter(){return filter;}public int range(){return range;}public int maximumTargets(){return maximumTargets;}public LineOfSightPolicy lineOfSight(){return lineOfSight;}public OrderingPolicy ordering(){return ordering;}public int coverageFirst(){return coverageFirst;}public int coverageSecond(){return coverageSecond;}
	}
	public interface Effect { StableId effectId(); EffectVariant variant(); }
	public abstract static class ValueEffect implements Effect {private final StableId effectId;private final Value value;protected ValueEffect(StableId id,Value value){require(id,value);effectId=id;this.value=value;}@Override public StableId effectId(){return effectId;}public Value value(){return value;}}
	public static final class DirectDamageEffect extends ValueEffect {private final DamageType damageType;private final DefensePolicy defensePolicy;public DirectDamageEffect(StableId id,int amount,DamageType type,DefensePolicy policy){this(id,Value.fixed(amount),type,policy);}public DirectDamageEffect(StableId id,Value amount,DamageType type,DefensePolicy policy){super(id,amount);require(type,policy);damageType=type;defensePolicy=policy;}@Override public EffectVariant variant(){return EffectVariant.DIRECT_DAMAGE;}public int amount(){return value().variant()==ValueVariant.FIXED?value().fixed():value().base();}public DamageType damageType(){return damageType;}public DefensePolicy defensePolicy(){return defensePolicy;}}
	public static final class PercentMaxHpDamageEffect implements Effect {private final StableId id;private final int percent,cap;private final ProtectedTargetPolicy policy;public PercentMaxHpDamageEffect(StableId id,int percent,int cap,ProtectedTargetPolicy policy){require(id,policy);this.id=id;this.percent=percent;this.cap=cap;this.policy=policy;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.PERCENT_MAX_HP_DAMAGE;}public int percent(){return percent;}public int absoluteCap(){return cap;}public ProtectedTargetPolicy protectedTargetPolicy(){return policy;}}
	public static final class MissingHpDamageEffect extends ValueEffect {private final int numerator,denominator,cap;private final DamageType damageType;public MissingHpDamageEffect(StableId id,Value base,int numerator,int denominator,int cap,DamageType type){super(id,base);require(type);this.numerator=numerator;this.denominator=denominator;this.cap=cap;damageType=type;}@Override public EffectVariant variant(){return EffectVariant.MISSING_HP_DAMAGE;}public int numerator(){return numerator;}public int denominator(){return denominator;}public int absoluteCap(){return cap;}public DamageType damageType(){return damageType;}}
	public static final class ExecuteEffect implements Effect {private final StableId id;private final int threshold;private final Value fallback;private final ProtectedTargetPolicy policy;public ExecuteEffect(StableId id,int threshold,Value fallback,ProtectedTargetPolicy policy){require(id,policy);this.id=id;this.threshold=threshold;this.fallback=fallback;this.policy=policy;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.EXECUTE;}public int threshold(){return threshold;}public Value fallback(){return fallback;}public ProtectedTargetPolicy protectedTargetPolicy(){return policy;}}
	public static final class ApplyStatusEffect extends ValueEffect {private final Status status;private final Duration duration;private final StatusStacking stacking;public ApplyStatusEffect(StableId id,Status status,Value intensity,Duration duration,StatusStacking stacking){super(id,intensity);require(status,duration,stacking);this.status=status;this.duration=duration;this.stacking=stacking;}@Override public EffectVariant variant(){return EffectVariant.APPLY_STATUS;}public Status status(){return status;}public Duration duration(){return duration;}public StatusStacking stacking(){return stacking;}}
	public abstract static class DistanceEffect implements Effect {private final StableId id;private final int distance;private final CollisionPolicy policy;protected DistanceEffect(StableId id,int distance,CollisionPolicy policy){require(id,policy);this.id=id;this.distance=distance;this.policy=policy;}@Override public StableId effectId(){return id;}public int distance(){return distance;}public CollisionPolicy collisionPolicy(){return policy;}}
	public static final class PushEffect extends DistanceEffect {public PushEffect(StableId id,int d,CollisionPolicy p){super(id,d,p);}@Override public EffectVariant variant(){return EffectVariant.PUSH;}}
	public static final class PullEffect extends DistanceEffect {public PullEffect(StableId id,int d,CollisionPolicy p){super(id,d,p);}@Override public EffectVariant variant(){return EffectVariant.PULL;}}
	public static final class ThrowEffect extends DistanceEffect {public ThrowEffect(StableId id,int d,CollisionPolicy p){super(id,d,p);}@Override public EffectVariant variant(){return EffectVariant.THROW;}}
	public static final class DashEffect implements Effect {private final StableId id;private final int distance;private final PathPolicy policy;public DashEffect(StableId id,int d,PathPolicy p){require(id,p);this.id=id;distance=d;policy=p;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.DASH;}public int maximumDistance(){return distance;}public PathPolicy pathPolicy(){return policy;}}
	public static final class TeleportEffect implements Effect {private final StableId id;private final int range;private final DestinationPolicy policy;public TeleportEffect(StableId id,int r,DestinationPolicy p){require(id,p);this.id=id;range=r;policy=p;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.TELEPORT;}public int maximumRange(){return range;}public DestinationPolicy destinationPolicy(){return policy;}}
	public static final class SwapPositionEffect implements Effect {private final StableId id;private final SwapLegalityPolicy policy;public SwapPositionEffect(StableId id,SwapLegalityPolicy p){require(id,p);this.id=id;policy=p;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.SWAP_POSITION;}public SwapLegalityPolicy legalityPolicy(){return policy;}}
	public static final class HealEffect extends ValueEffect {public HealEffect(StableId id,Value value){super(id,value);}@Override public EffectVariant variant(){return EffectVariant.HEAL;}}
	public static final class BarrierEffect extends ValueEffect {private final BarrierOverflowPolicy policy;public BarrierEffect(StableId id,Value value,BarrierOverflowPolicy policy){super(id,value);require(policy);this.policy=policy;}@Override public EffectVariant variant(){return EffectVariant.BARRIER;}public BarrierOverflowPolicy overflowPolicy(){return policy;}}
	public static final class TemporaryHpEffect extends ValueEffect {private final Duration duration;private final TemporaryHpStacking stacking;public TemporaryHpEffect(StableId id,Value value,Duration duration,TemporaryHpStacking stacking){super(id,value);require(duration,stacking);this.duration=duration;this.stacking=stacking;}@Override public EffectVariant variant(){return EffectVariant.TEMPORARY_HP;}public Duration duration(){return duration;}public TemporaryHpStacking stacking(){return stacking;}}
	public static final class MitigateEffect implements Effect {private final StableId id;private final int percent;private final Duration duration;public MitigateEffect(StableId id,int percent,Duration duration){require(id,duration);this.id=id;this.percent=percent;this.duration=duration;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.MITIGATE;}public int percent(){return percent;}public Duration duration(){return duration;}}
	public static final class RedirectDamageEffect implements Effect {private final StableId id;private final int percent;private final Subject recipient;private final Duration duration;public RedirectDamageEffect(StableId id,int percent,Subject recipient,Duration duration){require(id,recipient,duration);this.id=id;this.percent=percent;this.recipient=recipient;this.duration=duration;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.REDIRECT_DAMAGE;}public int percent(){return percent;}public Subject recipient(){return recipient;}public Duration duration(){return duration;}}
	public static final class StatusFilter {private final StatusFilterKind kind;private final Status status;public StatusFilter(StatusFilterKind kind,Status status){require(kind);this.kind=kind;this.status=status;}public StatusFilterKind kind(){return kind;}public Status status(){return status;}}
	public static final class CleanseEffect implements Effect {private final StableId id;private final StatusFilter filter;private final int maximumCount;public CleanseEffect(StableId id,StatusFilter filter,int count){require(id,filter);this.id=id;this.filter=filter;maximumCount=count;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.CLEANSE;}public StatusFilter filter(){return filter;}public int maximumCount(){return maximumCount;}}
	public static final class ResourceOperation {private final StableId operationId;private final ResourceOperationVariant variant;private final StableId firstResource,secondResource;private final Value value;private final int firstAmount,secondAmount,durationTurns;private final ResourceOverflowPolicy overflow;private final InsufficientResourcePolicy insufficient;private final ConversionPolicy conversion;private final ReservationExpiryPolicy expiry;private final ResourceSuppressionMode suppression;public ResourceOperation(StableId id,ResourceOperationVariant variant,StableId first,StableId second,Value value,int firstAmount,int secondAmount,int duration,ResourceOverflowPolicy overflow,InsufficientResourcePolicy insufficient,ConversionPolicy conversion,ReservationExpiryPolicy expiry,ResourceSuppressionMode suppression){require(id,variant);operationId=id;this.variant=variant;firstResource=first;secondResource=second;this.value=value;this.firstAmount=firstAmount;this.secondAmount=secondAmount;durationTurns=duration;this.overflow=overflow;this.insufficient=insufficient;this.conversion=conversion;this.expiry=expiry;this.suppression=suppression;}public StableId operationId(){return operationId;}public ResourceOperationVariant variant(){return variant;}public StableId firstResource(){return firstResource;}public StableId secondResource(){return secondResource;}public Value value(){return value;}public int firstAmount(){return firstAmount;}public int secondAmount(){return secondAmount;}public int durationTurns(){return durationTurns;}public ResourceOverflowPolicy overflow(){return overflow;}public InsufficientResourcePolicy insufficient(){return insufficient;}public ConversionPolicy conversion(){return conversion;}public ReservationExpiryPolicy expiry(){return expiry;}public ResourceSuppressionMode suppression(){return suppression;}}
	public static final class ResourceOperationEffect implements Effect {private final StableId id;private final ResourceOperation operation;public ResourceOperationEffect(StableId id,ResourceOperation operation){require(id,operation);this.id=id;this.operation=operation;}@Override public StableId effectId(){return id;}@Override public EffectVariant variant(){return EffectVariant.RESOURCE_OPERATION;}public ResourceOperation operation(){return operation;}}
	public static final class SecondaryEffect {private final Effect effect;private final SecondaryActivation activation;public SecondaryEffect(Effect effect,SecondaryActivation activation){require(effect,activation);this.effect=effect;this.activation=activation;}public Effect effect(){return effect;}public SecondaryActivation activation(){return activation;}}
	public static final class EffectChain {private final StableId chainId;private final Effect primary;private final SecondaryEffect secondary;public EffectChain(StableId id,Effect primary,SecondaryEffect secondary){require(id,primary);chainId=id;this.primary=primary;this.secondary=secondary;}public StableId chainId(){return chainId;}public Effect primary(){return primary;}public SecondaryEffect secondary(){return secondary;}}
	public static final class Modifier {private final StableId nodeId;private final ModifierVariant variant;private final int first,second;public Modifier(ModifierVariant variant){this(null,variant,0,0);}public Modifier(StableId id,ModifierVariant variant,int first,int second){require(variant);nodeId=id;this.variant=variant;this.first=first;this.second=second;}public StableId nodeId(){return nodeId;}public ModifierVariant variant(){return variant;}public int first(){return first;}public int second(){return second;}}
	public static final class Cost {private final StableId nodeId;private final CostVariant variant;private final StableId resourceId;private final int amount,minimum;private final HpLethalPolicy lethalPolicy;private final ItemCategory itemCategory;public Cost(StableId id,CostVariant variant){this(id,variant,null,0,0,null,null);}public Cost(StableId id,CostVariant variant,StableId resource,int amount,int minimum,HpLethalPolicy lethalPolicy,ItemCategory itemCategory){require(id,variant);nodeId=id;this.variant=variant;resourceId=resource;this.amount=amount;this.minimum=minimum;this.lethalPolicy=lethalPolicy;this.itemCategory=itemCategory;}public StableId nodeId(){return nodeId;}public CostVariant variant(){return variant;}public StableId resourceId(){return resourceId;}public int amount(){return amount;}public int minimum(){return minimum;}public HpLethalPolicy lethalPolicy(){return lethalPolicy;}public ItemCategory itemCategory(){return itemCategory;}}
	public static final class Constraint {private final ConstraintVariant variant;public Constraint(ConstraintVariant variant){require(variant);this.variant=variant;}public ConstraintVariant variant(){return variant;}}

	private final StableId skillId;private final Trigger trigger;private final Condition condition;private final Delivery delivery;private final Targeting targeting;private final EffectChain effectChain;private final Modifier modifier;private final Cost cost;private final Constraint constraint;
	public CompiledSkill(StableId id,Trigger trigger,Condition condition,Delivery delivery,Targeting targeting,EffectChain chain,Modifier modifier,Cost cost,Constraint constraint){require(id,trigger,condition,delivery,targeting,chain,modifier,cost,constraint);skillId=id;this.trigger=trigger;this.condition=condition;this.delivery=delivery;this.targeting=targeting;effectChain=chain;this.modifier=modifier;this.cost=cost;this.constraint=constraint;}
	public StableId skillId(){return skillId;}public Trigger trigger(){return trigger;}public Condition condition(){return condition;}public Delivery delivery(){return delivery;}public Targeting targeting(){return targeting;}public EffectChain effectChain(){return effectChain;}public Modifier modifier(){return modifier;}public Cost cost(){return cost;}public Constraint constraint(){return constraint;}
	private static void require(Object...values){for(Object value:values)if(value==null)throw new IllegalArgumentException("compiled node fields required");}
}
