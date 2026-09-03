package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderBudgetLedger;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderBudgetPolicy;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyResolver;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation.SkillValidation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/** Fail-closed authoring-to-runtime compiler. It never repairs a draft. */
public final class SkillCompiler {
	public static final String RUNTIME_VERSION="v6-p03-r1-1";
	private final EffectExecutorRegistry executors;
	public SkillCompiler(EffectExecutorRegistry executors){if(executors==null)throw new IllegalArgumentException("executor registry is required");this.executors=executors;}
	public ClassCompilePlan compile(ClassBuildSpec build){return compile(build,false);}
	public ClassCompilePlan preview(ClassBuildSpec build){return compile(build,true);}
	private ClassCompilePlan compile(ClassBuildSpec build,boolean preview){
		if(build==null)throw new IllegalArgumentException("build is required");
		List<CompiledSkill> compiled=new ArrayList<>();List<CompileDiagnostic> diagnostics=new ArrayList<>();
		if(build.schemaVersion()!=ClassBuildSpec.SCHEMA_VERSION)diagnostics.add(new CompileDiagnostic(build.buildId(),"schema_version",DependencyState.UNSUPPORTED,"schema.unsupported"));
		if(!ClassBuildSpec.CONTRACT_VERSION.equals(build.contractVersion()))diagnostics.add(new CompileDiagnostic(build.buildId(),"contract_version",DependencyState.HARD_CONFLICT,"contract.version_mismatch"));
		addBuildDiagnostics(diagnostics,new DependencyResolver().resolve(build));
		BuilderBudgetLedger ledger=new BuilderBudgetPolicy.P03TypedSkill().evaluate(build);
		if(!BuilderBudgetPolicy.P03TypedSkill.PRICE_VERSION.equals(build.budgetMetadata().priceVersion()))
			diagnostics.add(new CompileDiagnostic(build.buildId(),"budget.price_version",DependencyState.UNSUPPORTED,"compile.price_version_unsupported"));
		if(ledger.overBudget())diagnostics.add(new CompileDiagnostic(build.buildId(),"budget",DependencyState.HARD_CONFLICT,"compile.budget_over_limit"));
		for(SkillSpec skill:build.skills()){
			DependencyReport report=SkillValidation.validate(skill,executors);
			for(DependencyDiagnostic diagnostic:report.diagnostics())diagnostics.add(toCompile(skill,diagnostic));
			if(!report.finalizationAllowed())continue;
			if(skill.implementationState()!=ImplementationState.IMPLEMENTED){diagnostics.add(new CompileDiagnostic(skill.id(),"implementation_state",DependencyState.UNSUPPORTED,"compile.skill_not_implemented"));continue;}
			compiled.add(compileSkill(skill));
		}
		if(build.skills().isEmpty())diagnostics.add(new CompileDiagnostic(build.buildId(),"skills",DependencyState.UNRESOLVED,"compile.no_skill"));
		ClassCompilePlan.Kind kind=preview?ClassCompilePlan.Kind.PREVIEW
				:diagnostics.isEmpty()&&compiled.size()==build.skills().size()&&!compiled.isEmpty()?ClassCompilePlan.Kind.EXECUTABLE:ClassCompilePlan.Kind.PARTIAL;
		return new ClassCompilePlan(build.buildId(),hash(build),build.schemaVersion(),build.budgetMetadata().priceVersion(),RUNTIME_VERSION,kind,compiled,diagnostics);
	}
	private static CompiledSkill compileSkill(SkillSpec skill){
		ActiveTriggerSpec trigger=(ActiveTriggerSpec)skill.activation();
		DirectDeliverySpec delivery=(DirectDeliverySpec)skill.delivery();TargetingSpec target=skill.targeting();NoCostSpec cost=(NoCostSpec)skill.cost();
		CompiledSkill.SecondaryEffect secondary=null;
		if(skill.effects().secondary()!=null)secondary=new CompiledSkill.SecondaryEffect(compileEffect(skill.effects().secondary().effect()),CompiledSkill.SecondaryActivation.IMMEDIATE_ON_PRIMARY_SUCCESS);
		return new CompiledSkill(skill.id(),new CompiledSkill.Trigger(trigger.nodeId(),CompiledSkill.TriggerVariant.ACTIVE),
				new CompiledSkill.Condition(CompiledSkill.ConditionVariant.ALWAYS),
				new CompiledSkill.Delivery(delivery.nodeId(),CompiledSkill.DeliveryVariant.DIRECT,delivery.requiresLineOfSight()),
				new CompiledSkill.Targeting(target.nodeId(),CompiledSkill.SelectorVariant.SELECTED_ACTOR,CompiledSkill.CoverageVariant.SINGLE,
						CompiledSkill.FilterVariant.ENEMY_EXCLUDE_SELF,target.range(),target.maximumTargets(),CompiledSkill.LineOfSightPolicy.DELIVERY,CompiledSkill.OrderingPolicy.DISTANCE_CELL_ACTOR_ID),
				new CompiledSkill.EffectChain(skill.effects().chainId(),compileEffect(skill.effects().primary()),secondary),
				new CompiledSkill.Modifier(CompiledSkill.ModifierVariant.NONE),
				new CompiledSkill.Cost(cost.nodeId(),CompiledSkill.CostVariant.NO_COST),
				new CompiledSkill.Constraint(CompiledSkill.ConstraintVariant.NONE));
	}
	private static CompiledSkill.Effect compileEffect(EffectSpec raw){
		DirectDamageEffectSpec effect=(DirectDamageEffectSpec)raw;FixedValueSpec amount=(FixedValueSpec)effect.amount();
		return new CompiledSkill.DirectDamageEffect(effect.effectId(),amount.value(),CompiledSkill.DamageType.valueOf(effect.damageType().name()),CompiledSkill.DefensePolicy.valueOf(effect.defensePolicy().name()));
	}
	private static CompileDiagnostic toCompile(SkillSpec skill,DependencyDiagnostic diagnostic){return new CompileDiagnostic(skill.id(),diagnostic.fieldPath(),diagnostic.state(),diagnostic.messageKey());}
	private static void addBuildDiagnostics(List<CompileDiagnostic> out,DependencyReport report){for(DependencyDiagnostic diagnostic:report.diagnostics())out.add(new CompileDiagnostic(diagnostic.ownerNodeId(),diagnostic.fieldPath(),diagnostic.state(),diagnostic.messageKey()));}
	private static String hash(ClassBuildSpec build){
		try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(new CanonicalBuildCodec().serialize(build).getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder("sha256:");for(byte value:bytes)out.append(String.format("%02x",value&0xff));return out.toString();}
		catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
	}
}
