package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderBudgetPolicy;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.V6RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.BudgetMetadata;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class P03R1CompilePlanTest {
	@Test public void compiledSkillIsIndependentImmutableGraph(){
		ClassBuildSpec build=P03TestBuilds.directDamage("p03-r1-plan",11,true,true).finalizeOrThrow();SkillCompiler compiler=new SkillCompiler(EffectExecutorRegistry.standard());ClassCompilePlan plan=compiler.compile(build);
		assertEquals(ClassCompilePlan.Kind.EXECUTABLE,plan.kind());assertTrue(plan.executable());assertEquals(build.buildId(),plan.buildId());assertEquals(build.schemaVersion(),plan.schemaVersion());assertEquals(BuilderBudgetPolicy.P03TypedSkill.PRICE_VERSION,plan.priceVersion());assertEquals(SkillCompiler.RUNTIME_VERSION,plan.runtimeVersion());assertTrue(plan.canonicalSpecHash().matches("sha256:[0-9a-f]{64}"));assertTrue(plan.diagnostics().isEmpty());
		CompiledSkill skill=plan.skills().get(0);assertEquals(build.skills().get(0).id(),skill.skillId());assertEquals(CompiledSkill.TriggerVariant.ACTIVE,skill.trigger().variant());assertEquals(CompiledSkill.ConditionVariant.ALWAYS,skill.condition().variant());assertEquals(CompiledSkill.DeliveryVariant.DIRECT,skill.delivery().variant());assertEquals(CompiledSkill.SelectorVariant.SELECTED_ACTOR,skill.targeting().selector());assertEquals(CompiledSkill.CoverageVariant.SINGLE,skill.targeting().coverage());assertEquals(CompiledSkill.FilterVariant.ENEMY_EXCLUDE_SELF,skill.targeting().filter());assertEquals(CompiledSkill.EffectVariant.DIRECT_DAMAGE,skill.effectChain().primary().variant());assertEquals(CompiledSkill.ModifierVariant.NONE,skill.modifier().variant());assertEquals(CompiledSkill.CostVariant.NO_COST,skill.cost().variant());assertEquals(CompiledSkill.ConstraintVariant.NONE,skill.constraint().variant());
		assertCompiledGraphHasNoAuthoringReferences(CompiledSkill.class,new HashSet<Class<?>>());for(Field field:CompiledSkill.class.getDeclaredFields())assertTrue("compiled fields must be final: "+field,Modifier.isFinal(field.getModifiers()));for(Method method:CompiledSkill.class.getDeclaredMethods())assertNotEquals("source",method.getName());
		CanonicalBuildCodec codec=new CanonicalBuildCodec();ClassBuildSpec loaded=codec.deserialize(codec.serialize(build)).value();assertEquals(plan.canonicalSpecHash(),compiler.compile(loaded).canonicalSpecHash());
	}

	@Test public void previewPartialAndMissingExecutorPlansCannotExecute(){
		ClassBuildSpec valid=P03TestBuilds.directDamage("p03-r1-reject",7,false,false).finalizeOrThrow();SkillCompiler standard=new SkillCompiler(EffectExecutorRegistry.standard());
		ClassCompilePlan preview=standard.preview(valid);assertEquals(ClassCompilePlan.Kind.PREVIEW,preview.kind());assertRuntimeRejected(preview,EffectExecutorRegistry.standard());
		ClassCompilePlan missingExecutor=new SkillCompiler(EffectExecutorRegistry.empty()).compile(valid);assertEquals(ClassCompilePlan.Kind.PARTIAL,missingExecutor.kind());assertTrue(missingExecutor.diagnostics().stream().anyMatch(value->"skill.executor_missing".equals(value.messageKey())));assertRuntimeRejected(missingExecutor,EffectExecutorRegistry.empty());
		ClassBuildSpec overBudget=valid.toBuilder().budgetMetadata(new BudgetMetadata(BuilderBudgetPolicy.P03TypedSkill.PRICE_VERSION,0)).build();ClassCompilePlan budget=standard.compile(overBudget);assertEquals(ClassCompilePlan.Kind.PARTIAL,budget.kind());assertTrue(budget.diagnostics().stream().anyMatch(value->"compile.budget_over_limit".equals(value.messageKey())));assertRuntimeRejected(budget,EffectExecutorRegistry.standard());
		ClassBuildSpec hardConflict=valid.toBuilder().contractVersion("wrong").build();ClassCompilePlan conflict=standard.compile(hardConflict);assertEquals(ClassCompilePlan.Kind.PARTIAL,conflict.kind());assertRuntimeRejected(conflict,EffectExecutorRegistry.standard());
		ClassCompilePlan unresolved=standard.compile(PlayerBuildSession.empty(new DeterministicIdGenerator("p03-r1-empty")).state().draft());assertEquals(ClassCompilePlan.Kind.PARTIAL,unresolved.kind());assertRuntimeRejected(unresolved,EffectExecutorRegistry.standard());
		try{new V6RuleRuntime(standard.compile(valid),EffectExecutorRegistry.empty());fail("runtime must reject a registry missing a compiled executor");}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("executors"));}
	}
	private static void assertRuntimeRejected(ClassCompilePlan plan,EffectExecutorRegistry registry){try{new V6RuleRuntime(plan,registry);fail("non-executable plan accepted");}catch(IllegalArgumentException expected){assertNotNull(expected.getMessage());}}
	private static void assertCompiledGraphHasNoAuthoringReferences(Class<?> type,Set<Class<?>> seen){if(!seen.add(type))return;for(Field field:type.getDeclaredFields()){assertFalse("authoring type leaked through "+type.getName()+"."+field.getName(),field.getType().getName().contains(".rules.contract.v6.spec."));assertTrue("compiled node field must be final: "+field,Modifier.isFinal(field.getModifiers()));}for(Class<?> nested:type.getDeclaredClasses())assertCompiledGraphHasNoAuthoringReferences(nested,seen);}
}
