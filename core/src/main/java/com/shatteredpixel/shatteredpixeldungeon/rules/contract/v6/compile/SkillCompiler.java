package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.component.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation.SkillValidation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/** Fail-closed P04 compiler. Every admitted collection becomes its own immutable node list. */
public final class SkillCompiler {
	public static final String RUNTIME_VERSION="v6-p04-r1-1";
	private final EffectExecutorRegistry executors;
	public SkillCompiler(EffectExecutorRegistry executors){if(executors==null)throw new IllegalArgumentException("executor registry is required");this.executors=executors;}
	public ClassCompilePlan compile(ClassBuildSpec build){return compile(build,false);}
	public ClassCompilePlan preview(ClassBuildSpec build){return compile(build,true);}
	private ClassCompilePlan compile(ClassBuildSpec build,boolean preview){
		if(build==null)throw new IllegalArgumentException("build is required");
		List<CompileDiagnostic> diagnostics=new ArrayList<>();
		diagnostics.addAll(new ExecutableBuildAdmissionPolicy().evaluate(build));
		addBuildDiagnostics(diagnostics,new DependencyResolver().resolve(build));
		BuilderBudgetLedger ledger=new BuilderBudgetPolicy.P03TypedSkill().evaluate(build);
		if(!BuilderBudgetPolicy.P03TypedSkill.PRICE_VERSION.equals(build.budgetMetadata().priceVersion()))diagnostics.add(new CompileDiagnostic(build.buildId(),"budget.price_version",DependencyState.UNSUPPORTED,"compile.price_version_unsupported"));
		if(ledger.overBudget())diagnostics.add(new CompileDiagnostic(build.buildId(),"budget",DependencyState.HARD_CONFLICT,"compile.budget_over_limit"));
		List<CompiledResource> resources=new ArrayList<>();for(ResourceSpec value:build.resources())resources.add(compileResource(value));
		List<CompiledClassComponent> components=new ArrayList<>();for(ContractNodeSpec value:build.classComponents())if(value instanceof BasicAttackComponentSpec||value instanceof ResourceFlowComponentSpec||value instanceof ActiveResourceOperationComponentSpec)components.add(compileComponent(value));
		List<CompiledClassOperation> operations=new ArrayList<>();for(ContractNodeSpec value:build.classOperations())if(value instanceof ResourceClassOperationSpec)operations.add(compileOperation((ResourceClassOperationSpec)value));
		List<CompiledSkill> skills=new ArrayList<>();
		for(SkillSpec skill:build.skills()){
			DependencyReport report=SkillValidation.validate(skill,executors);
			for(DependencyDiagnostic diagnostic:report.diagnostics())diagnostics.add(toCompile(skill,diagnostic));
			if(!report.finalizationAllowed())continue;
			if(skill.implementationState()!=ImplementationState.IMPLEMENTED){diagnostics.add(new CompileDiagnostic(skill.id(),"implementation_state",DependencyState.UNSUPPORTED,"compile.skill_not_implemented"));continue;}
			skills.add(compileSkill(skill));
		}
		boolean complete=resources.size()==build.resources().size()&&components.size()==build.classComponents().size()&&operations.size()==build.classOperations().size()&&skills.size()==build.skills().size();
		boolean any=!skills.isEmpty()||!components.isEmpty()||!operations.isEmpty();
		if(!any)diagnostics.add(new CompileDiagnostic(build.buildId(),"skills",DependencyState.UNRESOLVED,"compile.no_skill"));
		ClassCompilePlan.Kind kind=preview?ClassCompilePlan.Kind.PREVIEW:diagnostics.isEmpty()&&complete&&any?ClassCompilePlan.Kind.EXECUTABLE:ClassCompilePlan.Kind.PARTIAL;
		return new ClassCompilePlan(build.buildId(),hash(build),build.schemaVersion(),build.budgetMetadata().priceVersion(),RUNTIME_VERSION,kind,resources,components,operations,skills,diagnostics);
	}
	private static CompiledResource compileResource(ResourceSpec r){return new CompiledResource(r.id(),r.displayName().text(),r.minimum(),r.maximum(),r.initialValue(),CompiledResource.OverflowPolicy.valueOf(r.defaultOverflowPolicy().name()),r.hud().visible(),r.hud().order(),r.hud().presentationKey());}
	private static CompiledClassComponent compileComponent(ContractNodeSpec node){
		if(node instanceof BasicAttackComponentSpec){BasicAttackComponentSpec x=(BasicAttackComponentSpec)node;return new CompiledClassComponent(x.id(),CompiledClassComponent.Variant.BASIC_ATTACK,CompiledClassComponent.BasicAttackAvailability.valueOf(x.availability().name()),x.damageNumerator(),x.damageDenominator(),x.actionTimeTurns(),CompiledSkill.ItemCategory.valueOf(x.allowedWeapons().category().name()),null,null,null,null,null);}
		if(node instanceof ResourceFlowComponentSpec){ResourceFlowComponentSpec x=(ResourceFlowComponentSpec)node;return new CompiledClassComponent(x.id(),CompiledClassComponent.Variant.RESOURCE_FLOW,null,0,0,0,null,compileTrigger(x.trigger()),compileCondition(x.condition()),compileResourceOperation(x.operation()),null,null);}
		ActiveResourceOperationComponentSpec x=(ActiveResourceOperationComponentSpec)node;return new CompiledClassComponent(x.id(),CompiledClassComponent.Variant.ACTIVE_RESOURCE_OPERATION,null,0,0,x.actionTimeTurns(),null,null,null,compileResourceOperation(x.operation()),compileCost(x.cost()),x.classOperationId());
	}
	private static CompiledClassOperation compileOperation(ResourceClassOperationSpec x){return new CompiledClassOperation(x.id(),x.displayName().text(),x.sourceComponent()==null?null:x.sourceComponent().targetId(),compileTrigger(x.activation()),compileCondition(x.condition()),compileCost(x.cost()),x.actionTimeTurns(),compileResourceOperation(x.operation()),x.hudVisible(),x.hudOrder());}
	private static CompiledSkill compileSkill(SkillSpec skill){
		CompiledSkill.SecondaryEffect secondary=null;
		if(skill.effects().secondary()!=null)secondary=new CompiledSkill.SecondaryEffect(compileEffect(skill.effects().secondary().effect()),CompiledSkill.SecondaryActivation.IMMEDIATE_ON_PRIMARY_SUCCESS);
		return new CompiledSkill(skill.id(),compileTrigger(skill.activation()),compileCondition(skill.condition()),compileDelivery(skill.delivery()),compileTargeting(skill.targeting()),new CompiledSkill.EffectChain(skill.effects().chainId(),compileEffect(skill.effects().primary()),secondary),compileModifier(skill.modifier()),compileCost(skill.cost()),new CompiledSkill.Constraint(CompiledSkill.ConstraintVariant.NONE));
	}
	private static CompiledSkill.Trigger compileTrigger(TriggerSpec trigger){return trigger instanceof EventTriggerSpec?new CompiledSkill.Trigger(trigger.nodeId(),CompiledSkill.TriggerVariant.EVENT,((EventTriggerSpec)trigger).event()):new CompiledSkill.Trigger(trigger.nodeId(),CompiledSkill.TriggerVariant.ACTIVE);}
	private static CompiledSkill.Condition compileCondition(ConditionExpr raw){
		if(raw instanceof AlwaysCondition)return new CompiledSkill.Condition(CompiledSkill.ConditionVariant.ALWAYS);
		if(raw instanceof AllOfCondition){List<CompiledSkill.Condition> children=new ArrayList<>();for(ConditionExpr child:((AllOfCondition)raw).children())children.add(compileCondition(child));return children.isEmpty()?new CompiledSkill.Condition(CompiledSkill.ConditionVariant.ALWAYS):CompiledSkill.Condition.allOf(children);}
		if(raw instanceof BuiltinStatCompareCondition){BuiltinStatCompareCondition x=(BuiltinStatCompareCondition)raw;return CompiledSkill.Condition.builtin(x.nodeId(),CompiledSkill.Subject.valueOf(x.subject().name()),CompiledSkill.BuiltinStat.valueOf(x.stat().name()),CompiledSkill.Comparison.valueOf(x.operator().name()),compileValue(x.value()));}
		ResourceCompareCondition x=(ResourceCompareCondition)raw;return CompiledSkill.Condition.resource(x.nodeId(),x.resource().targetId(),CompiledSkill.Comparison.valueOf(x.operator().name()),x.value());
	}
	private static CompiledSkill.Delivery compileDelivery(DeliverySpec raw){
		if(raw instanceof DirectDeliverySpec){DirectDeliverySpec x=(DirectDeliverySpec)raw;return new CompiledSkill.Delivery(x.nodeId(),CompiledSkill.DeliveryVariant.DIRECT,x.requiresLineOfSight());}
		if(raw instanceof SelfDeliverySpec)return new CompiledSkill.Delivery(raw.nodeId(),CompiledSkill.DeliveryVariant.SELF,false);
		if(raw instanceof ContactDeliverySpec)return new CompiledSkill.Delivery(raw.nodeId(),CompiledSkill.DeliveryVariant.CONTACT,false);
		if(raw instanceof ProjectileDeliverySpec){ProjectileDeliverySpec x=(ProjectileDeliverySpec)raw;return new CompiledSkill.Delivery(x.nodeId(),CompiledSkill.DeliveryVariant.PROJECTILE,true,x.speedClass(),0,x.presentationKey());}
		if(raw instanceof TraceDeliverySpec){TraceDeliverySpec x=(TraceDeliverySpec)raw;return new CompiledSkill.Delivery(x.nodeId(),CompiledSkill.DeliveryVariant.TRACE,x.stopsAtFirstBlockingCell(),x.width(),0,"");}
		GroundDeliverySpec x=(GroundDeliverySpec)raw;return new CompiledSkill.Delivery(x.nodeId(),CompiledSkill.DeliveryVariant.GROUND,x.requiresVisibleCell());
	}
	private static CompiledSkill.Targeting compileTargeting(TargetingSpec x){
		CompiledSkill.SelectorVariant selector=x.selector() instanceof SelfSelector?CompiledSkill.SelectorVariant.SELF:x.selector() instanceof SelectedCellSelector?CompiledSkill.SelectorVariant.SELECTED_CELL:CompiledSkill.SelectorVariant.SELECTED_ACTOR;
		CompiledSkill.CoverageVariant coverage;int first=0,second=0;
		if(x.coverage() instanceof AdjacentCoverageSpec)coverage=CompiledSkill.CoverageVariant.ADJACENT;else if(x.coverage() instanceof RadiusCoverageSpec){coverage=CompiledSkill.CoverageVariant.RADIUS;first=((RadiusCoverageSpec)x.coverage()).radius();}else if(x.coverage() instanceof LineCoverageSpec){coverage=CompiledSkill.CoverageVariant.LINE;first=((LineCoverageSpec)x.coverage()).length();second=((LineCoverageSpec)x.coverage()).width();}else coverage=CompiledSkill.CoverageVariant.SINGLE;
		CompiledSkill.FilterVariant filter=CompiledSkill.FilterVariant.ANY_ACTOR;
		if(x.filter() instanceof SelfFilterSpec)filter=CompiledSkill.FilterVariant.SELF;else if(x.filter() instanceof RelationFilterSpec){RelationFilterSpec relation=(RelationFilterSpec)x.filter();filter=relation.relationToClassOwner()==RelationFilterSpec.RelationAlignment.ENEMY?CompiledSkill.FilterVariant.ENEMY_EXCLUDE_SELF:relation.includeSelf()?CompiledSkill.FilterVariant.ALLY_INCLUDE_SELF:CompiledSkill.FilterVariant.ALLY_EXCLUDE_SELF;}
		return new CompiledSkill.Targeting(x.nodeId(),selector,coverage,filter,x.range(),x.maximumTargets(),CompiledSkill.LineOfSightPolicy.DELIVERY,CompiledSkill.OrderingPolicy.DISTANCE_CELL_ACTOR_ID,first,second);
	}
	private static CompiledSkill.Modifier compileModifier(ModifierSpec raw){
		if(raw==null)return new CompiledSkill.Modifier(CompiledSkill.ModifierVariant.NONE);
		if(raw instanceof RepeatModifierSpec)return new CompiledSkill.Modifier(raw.nodeId(),CompiledSkill.ModifierVariant.REPEAT,((RepeatModifierSpec)raw).repeatCount(),0);
		if(raw instanceof IntensityModifierSpec)return new CompiledSkill.Modifier(raw.nodeId(),CompiledSkill.ModifierVariant.INTENSITY,((IntensityModifierSpec)raw).numerator(),((IntensityModifierSpec)raw).denominator());
		if(raw instanceof ExtendDurationModifierSpec)return new CompiledSkill.Modifier(raw.nodeId(),CompiledSkill.ModifierVariant.EXTEND_DURATION,((ExtendDurationModifierSpec)raw).additionalTurns(),0);
		if(raw instanceof PierceModifierSpec)return new CompiledSkill.Modifier(raw.nodeId(),CompiledSkill.ModifierVariant.PIERCE,((PierceModifierSpec)raw).additionalTargets(),0);
		BounceModifierSpec x=(BounceModifierSpec)raw;return new CompiledSkill.Modifier(raw.nodeId(),CompiledSkill.ModifierVariant.BOUNCE,x.bounces(),x.bounceRange());
	}
	private static CompiledSkill.Cost compileCost(CostSpec raw){
		if(raw instanceof NoCostSpec)return new CompiledSkill.Cost(raw.nodeId(),CompiledSkill.CostVariant.NO_COST);
		if(raw instanceof ResourceCostSpec){ResourceCostSpec x=(ResourceCostSpec)raw;return new CompiledSkill.Cost(x.nodeId(),CompiledSkill.CostVariant.RESOURCE,x.resource().targetId(),x.amount(),0,null,null);}
		if(raw instanceof HpCostSpec){HpCostSpec x=(HpCostSpec)raw;return new CompiledSkill.Cost(x.nodeId(),CompiledSkill.CostVariant.HP,null,x.amount(),x.minimumRemainingHp(),CompiledSkill.HpLethalPolicy.valueOf(x.lethalPolicy().name()),null);}
		if(raw instanceof ActionTimeCostSpec)return new CompiledSkill.Cost(raw.nodeId(),CompiledSkill.CostVariant.ACTION_TIME,null,((ActionTimeCostSpec)raw).turns(),0,null,null);
		if(raw instanceof CooldownCostSpec)return new CompiledSkill.Cost(raw.nodeId(),CompiledSkill.CostVariant.COOLDOWN,null,((CooldownCostSpec)raw).turns(),0,null,null);
		ItemCostSpec x=(ItemCostSpec)raw;return new CompiledSkill.Cost(x.nodeId(),CompiledSkill.CostVariant.ITEM,null,x.count(),0,null,CompiledSkill.ItemCategory.valueOf(x.itemFilter().category().name()));
	}
	private static CompiledSkill.Effect compileEffect(EffectSpec raw){
		if(raw instanceof DirectDamageEffectSpec){DirectDamageEffectSpec x=(DirectDamageEffectSpec)raw;return new CompiledSkill.DirectDamageEffect(x.effectId(),compileValue(x.amount()),CompiledSkill.DamageType.valueOf(x.damageType().name()),CompiledSkill.DefensePolicy.valueOf(x.defensePolicy().name()));}
		if(raw instanceof PercentMaxHpDamageEffectSpec){PercentMaxHpDamageEffectSpec x=(PercentMaxHpDamageEffectSpec)raw;return new CompiledSkill.PercentMaxHpDamageEffect(x.effectId(),x.percent(),x.absoluteCap(),CompiledSkill.ProtectedTargetPolicy.valueOf(x.protectedTargetPolicy().name()));}
		if(raw instanceof MissingHpDamageEffectSpec){MissingHpDamageEffectSpec x=(MissingHpDamageEffectSpec)raw;return new CompiledSkill.MissingHpDamageEffect(x.effectId(),compileValue(x.baseAmount()),x.missingHpNumerator(),x.missingHpDenominator(),x.absoluteCap(),CompiledSkill.DamageType.valueOf(x.damageType().name()));}
		if(raw instanceof ExecuteEffectSpec){ExecuteEffectSpec x=(ExecuteEffectSpec)raw;return new CompiledSkill.ExecuteEffect(x.effectId(),x.hpPercentThreshold(),x.fallbackDamage()==null?null:compileValue(x.fallbackDamage()),CompiledSkill.ProtectedTargetPolicy.valueOf(x.protectedTargetPolicy().name()));}
		if(raw instanceof ApplyStatusEffectSpec){ApplyStatusEffectSpec x=(ApplyStatusEffectSpec)raw;return new CompiledSkill.ApplyStatusEffect(x.effectId(),CompiledSkill.Status.valueOf(x.status().name()),compileValue(x.intensity()),compileDuration(x.duration()),CompiledSkill.StatusStacking.valueOf(x.stacking().name()));}
		if(raw instanceof PushEffectSpec){PushEffectSpec x=(PushEffectSpec)raw;return new CompiledSkill.PushEffect(x.effectId(),x.distance(),CompiledSkill.CollisionPolicy.valueOf(x.collisionPolicy().name()));}
		if(raw instanceof PullEffectSpec){PullEffectSpec x=(PullEffectSpec)raw;return new CompiledSkill.PullEffect(x.effectId(),x.distance(),CompiledSkill.CollisionPolicy.valueOf(x.collisionPolicy().name()));}
		if(raw instanceof ThrowEffectSpec){ThrowEffectSpec x=(ThrowEffectSpec)raw;return new CompiledSkill.ThrowEffect(x.effectId(),x.distance(),CompiledSkill.CollisionPolicy.valueOf(x.collisionPolicy().name()));}
		if(raw instanceof DashEffectSpec){DashEffectSpec x=(DashEffectSpec)raw;return new CompiledSkill.DashEffect(x.effectId(),x.maximumDistance(),CompiledSkill.PathPolicy.valueOf(x.pathPolicy().name()));}
		if(raw instanceof TeleportEffectSpec){TeleportEffectSpec x=(TeleportEffectSpec)raw;return new CompiledSkill.TeleportEffect(x.effectId(),x.maximumRange(),CompiledSkill.DestinationPolicy.valueOf(x.destinationPolicy().name()));}
		if(raw instanceof SwapPositionEffectSpec){SwapPositionEffectSpec x=(SwapPositionEffectSpec)raw;return new CompiledSkill.SwapPositionEffect(x.effectId(),CompiledSkill.SwapLegalityPolicy.valueOf(x.legalityPolicy().name()));}
		if(raw instanceof HealEffectSpec)return new CompiledSkill.HealEffect(raw.effectId(),compileValue(((HealEffectSpec)raw).amount()));
		if(raw instanceof BarrierEffectSpec){BarrierEffectSpec x=(BarrierEffectSpec)raw;return new CompiledSkill.BarrierEffect(x.effectId(),compileValue(x.amount()),CompiledSkill.BarrierOverflowPolicy.valueOf(x.overflowPolicy().name()));}
		if(raw instanceof TemporaryHpEffectSpec){TemporaryHpEffectSpec x=(TemporaryHpEffectSpec)raw;return new CompiledSkill.TemporaryHpEffect(x.effectId(),compileValue(x.amount()),compileDuration(x.duration()),CompiledSkill.TemporaryHpStacking.valueOf(x.stacking().name()));}
		if(raw instanceof MitigateEffectSpec){MitigateEffectSpec x=(MitigateEffectSpec)raw;return new CompiledSkill.MitigateEffect(x.effectId(),x.percent(),compileDuration(x.duration()));}
		if(raw instanceof RedirectDamageEffectSpec){RedirectDamageEffectSpec x=(RedirectDamageEffectSpec)raw;return new CompiledSkill.RedirectDamageEffect(x.effectId(),x.percent(),CompiledSkill.Subject.valueOf(x.recipient().name()),compileDuration(x.duration()));}
		if(raw instanceof CleanseEffectSpec){CleanseEffectSpec x=(CleanseEffectSpec)raw;return new CompiledSkill.CleanseEffect(x.effectId(),new CompiledSkill.StatusFilter(CompiledSkill.StatusFilterKind.valueOf(x.filter().kind().name()),x.filter().status()==null?null:CompiledSkill.Status.valueOf(x.filter().status().name())),x.maximumCount());}
		ResourceOperationEffectSpec x=(ResourceOperationEffectSpec)raw;return new CompiledSkill.ResourceOperationEffect(x.effectId(),compileResourceOperation(x.operation()));
	}
	private static CompiledSkill.Duration compileDuration(DurationSpec x){return new CompiledSkill.Duration(CompiledSkill.DurationKind.valueOf(x.kind().name()),x.turns());}
	private static CompiledSkill.Value compileValue(ValueSpec raw){
		if(raw instanceof FixedValueSpec)return CompiledSkill.Value.fixed(((FixedValueSpec)raw).value());
		ScaledValueSpec x=(ScaledValueSpec)raw;ValueSourceSpec source=x.source();CompiledSkill.ValueSource compiled;
		if(source instanceof BuiltinStatSource){BuiltinStatSource v=(BuiltinStatSource)source;compiled=new CompiledSkill.ValueSource(CompiledSkill.ValueSourceVariant.BUILTIN_STAT,CompiledSkill.Subject.valueOf(v.subject().name()),CompiledSkill.BuiltinStat.valueOf(v.stat().name()),null);}
		else compiled=new CompiledSkill.ValueSource(CompiledSkill.ValueSourceVariant.RESOURCE,null,null,((ResourceValueSource)source).resource().targetId());
		return CompiledSkill.Value.scaled(x.base(),compiled,x.numerator(),x.denominator(),x.minimum(),x.maximum());
	}
	private static CompiledSkill.ResourceOperation compileResourceOperation(ResourceOperationSpec raw){
		StableId first=null,second=null;CompiledSkill.Value value=null;int a=0,b=0,d=0;CompiledSkill.ResourceOverflowPolicy overflow=null;CompiledSkill.InsufficientResourcePolicy insufficient=null;CompiledSkill.ConversionPolicy conversion=null;CompiledSkill.ReservationExpiryPolicy expiry=null;CompiledSkill.ResourceSuppressionMode suppression=null;
		if(raw instanceof GainResourceSpec){GainResourceSpec x=(GainResourceSpec)raw;first=x.resource().targetId();value=compileValue(x.amount());overflow=CompiledSkill.ResourceOverflowPolicy.valueOf(x.overflowPolicy().name());}
		else if(raw instanceof DrainResourceSpec){DrainResourceSpec x=(DrainResourceSpec)raw;first=x.resource().targetId();value=compileValue(x.amount());insufficient=CompiledSkill.InsufficientResourcePolicy.valueOf(x.insufficientPolicy().name());}
		else if(raw instanceof SetResourceSpec){SetResourceSpec x=(SetResourceSpec)raw;first=x.resource().targetId();value=compileValue(x.value());}
		else if(raw instanceof ClearResourceSpec)first=((ClearResourceSpec)raw).resource().targetId();
		else if(raw instanceof ConvertResourceSpec){ConvertResourceSpec x=(ConvertResourceSpec)raw;first=x.source().targetId();second=x.target().targetId();a=x.sourceAmount();b=x.targetAmount();conversion=CompiledSkill.ConversionPolicy.valueOf(x.conversionPolicy().name());overflow=CompiledSkill.ResourceOverflowPolicy.valueOf(x.targetOverflowPolicy().name());}
		else if(raw instanceof ReserveResourceSpec){ReserveResourceSpec x=(ReserveResourceSpec)raw;first=x.resource().targetId();a=x.amount();d=x.durationTurns();expiry=CompiledSkill.ReservationExpiryPolicy.valueOf(x.expiryPolicy().name());}
		else{SuppressResourceSpec x=(SuppressResourceSpec)raw;first=x.resource().targetId();d=x.durationTurns();suppression=CompiledSkill.ResourceSuppressionMode.valueOf(x.mode().name());}
		return new CompiledSkill.ResourceOperation(raw.operationId(),CompiledSkill.ResourceOperationVariant.valueOf(raw.variant().name()),first,second,value,a,b,d,overflow,insufficient,conversion,expiry,suppression);
	}
	private static CompileDiagnostic toCompile(SkillSpec skill,DependencyDiagnostic d){return new CompileDiagnostic(skill.id(),d.fieldPath(),d.state(),d.messageKey());}
	private static void addBuildDiagnostics(List<CompileDiagnostic> out,DependencyReport report){for(DependencyDiagnostic d:report.diagnostics())if(d.state()!=DependencyState.RESOLVED)out.add(new CompileDiagnostic(d.ownerNodeId(),d.fieldPath(),d.state(),d.messageKey()));}
	private static String hash(ClassBuildSpec build){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(new CanonicalBuildCodec().serialize(build).getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder("sha256:");for(byte value:bytes)out.append(String.format("%02x",value&0xff));return out.toString();}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
}
