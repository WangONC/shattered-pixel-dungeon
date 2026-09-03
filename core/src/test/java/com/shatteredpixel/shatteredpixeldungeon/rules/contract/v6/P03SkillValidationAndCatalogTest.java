package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.format.SkillFormatter;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.validation.SkillValidation;
import com.shatteredpixel.shatteredpixeldungeon.rules.migration.v5.P03SkillMigrationPlaceholder;
import org.junit.Test;

import static org.junit.Assert.*;

/** Layer C: structural, compatibility, capability, formatter, budget, and migration evidence. */
public class P03SkillValidationAndCatalogTest {
	@Test public void completeSliceResolvesWhileIncompleteAndUnsupportedRemainDistinct() {
		PlayerBuildSession complete=P03TestBuilds.directDamage("p03-validation",7,false,false);
		SkillSpec skill=complete.state().draft().skills().get(0);
		DependencyReport valid=SkillValidation.validate(skill,EffectExecutorRegistry.standard());
		assertEquals(DependencyState.RESOLVED,valid.aggregateState());assertEquals(ImplementationState.IMPLEMENTED,skill.implementationState());

		PlayerBuildSession incomplete=PlayerBuildSession.empty(new com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator("p03-incomplete"));
		incomplete.dispatch(new BuilderCommand.CreateSkill("Incomplete"));
		assertEquals(DependencyState.UNRESOLVED,SkillValidation.validate(incomplete.state().draft().skills().get(0),EffectExecutorRegistry.standard()).aggregateState());

		DirectDamageEffectSpec unsupportedDamage=((DirectDamageEffectSpec)skill.effects().primary()).withDefensePolicy(DirectDamageEffectSpec.NativeDefensePolicy.IGNORE_ARMOR);
		SkillSpec unsupported=skill.withEffects(skill.effects().withPrimary(unsupportedDamage));
		assertEquals(DependencyState.UNSUPPORTED,SkillValidation.validate(unsupported,EffectExecutorRegistry.standard()).aggregateState());
		assertEquals(ImplementationState.DECLARED,unsupported.implementationState());
	}

	@Test public void bilingualFormatterAndPriceLedgerDescribeTheExactTypedFields() {
		PlayerBuildSession session=P03TestBuilds.directDamage("p03-format-budget",7,true,true);SkillSpec skill=session.state().draft().skills().get(0);
		SkillFormatter formatter=new SkillFormatter();String en=formatter.format(skill,SkillFormatter.Language.ENGLISH);String zh=formatter.format(skill,SkillFormatter.Language.SIMPLIFIED_CHINESE);
		assertTrue(en,en.contains("deal 7")&&en.contains("line of sight")&&en.contains("no cost"));
		assertTrue(zh,zh.contains("7 点")&&zh.contains("需要视线")&&zh.contains("无消耗"));
		BuilderBudgetLedger budget=session.state().budget();assertEquals(BuilderBudgetPolicy.P03TypedSkill.PRICE_VERSION,budget.priceVersion());assertEquals(11,budget.entries().size());
		assertEquals(11,budget.entries().stream().map(BuilderBudgetLedger.Entry::priceKey).distinct().count());
		for (com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.VariantDescriptor descriptor :
				com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.GameplayVariantCatalog.playerExposed()) {
			assertTrue(descriptor.qualifiedKey(), budget.entries().stream().anyMatch(v->descriptor.priceKey().equals(v.priceKey())));
		}
		assertTrue(budget.entries().stream().anyMatch(v->"skill.effect.direct_damage".equals(v.priceKey())));
		assertTrue(budget.entries().stream().anyMatch(v->"skill.chain.immediate_secondary".equals(v.priceKey())));
		assertEquals(budget.spent(),budget.entries().stream().mapToInt(BuilderBudgetLedger.Entry::amount).sum());
	}

	@Test public void v5SkillMigrationIsExplicitlyUnsupportedUntilAnExactMappingExists() {
		P03SkillMigrationPlaceholder migration=new P03SkillMigrationPlaceholder();
		assertEquals(P03SkillMigrationPlaceholder.Disposition.UNSUPPORTED_TYPED_MAPPING_REQUIRED,migration.classify(new com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec()));
		assertEquals("migration.v5.skill.typed_mapping_required",migration.diagnosticKey());
	}
}
