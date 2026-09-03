package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.GameplayVariantCatalog;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.catalog.VariantDescriptor;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.BuilderFormController;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.V6FormSchemas;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.EffectExecutorRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState;
import org.junit.Test;

import static org.junit.Assert.*;

/** Layer A: schema exposure is explicit and does not imply executor support. */
public class P03SkillFormSchemaTest {
	@Test public void onlyImplementedVariantsArePlayerExposed() {
		assertTrue(GameplayVariantCatalog.playerExposed().size()>11);
		for(VariantDescriptor descriptor:GameplayVariantCatalog.playerExposed()){
			assertEquals(ImplementationState.IMPLEMENTED,descriptor.state());
			assertFalse(descriptor.priceKey().isEmpty());
		}
		for(String key:new String[]{"TRIGGER.ACTIVE","CONDITION_EXPR.ALL_OF","CONDITION.ALWAYS","EFFECT.DIRECT_DAMAGE","EFFECT_CHAIN.PRIMARY","SECONDARY_ACTIVATION.IMMEDIATE_ON_PRIMARY_SUCCESS","DELIVERY.DIRECT","SELECTOR.SELECTED_ACTOR","COVERAGE.SINGLE","FILTER.RELATION_ENEMY_EXCLUDE_SELF","COST.NO_COST"})assertTrue(key,GameplayVariantCatalog.require(key.substring(0,key.indexOf('.')),key.substring(key.indexOf('.')+1)).playerExposed());
		assertFalse(GameplayVariantCatalog.require("EFFECT","ADD_MARK").playerExposed());
		assertTrue(EffectExecutorRegistry.standard().has(
				com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill.EffectVariant.DIRECT_DAMAGE));
		assertNotNull(V6FormSchemas.require(V6FormSchemas.SKILL).requireField("effect_primary_amount"));
	}

	@Test public void enabledSkillFormActionsUseDedicatedCommandsAndReachImplementedState() {
		PlayerBuildSession session=PlayerBuildSession.empty(new DeterministicIdGenerator("p03-form"));
		session.dispatch(new BuilderCommand.CreateSkill("Form Strike"));
		String id=session.state().draft().skills().get(0).id().value();
		BuilderFormController form=new BuilderFormController(session);
		assertTrue(form.commandForValue(id,"activation_variant","ACTIVE") instanceof BuilderCommand.SelectTriggerVariant);
		form.dispatchValue(id,"activation_variant","ACTIVE");
		form.dispatchValue(id,"condition_variant","ALWAYS");
		form.dispatchValue(id,"effect_primary_family","DAMAGE");
		form.dispatchValue(id,"effect_primary_variant","DIRECT_DAMAGE");
		form.dispatchValue(id,"effect_primary_amount","2");
		form.dispatchValue(id,"delivery_variant","DIRECT");
		form.dispatchValue(id,"targeting_selector","SELECTED_ACTOR");
		form.dispatchValue(id,"targeting_coverage","SINGLE");
		form.dispatchValue(id,"targeting_filter","RELATION_ENEMY_EXCLUDE_SELF");
		form.dispatchValue(id,"targeting_range","2");
		form.dispatchValue(id,"modifier_variant","NONE");
		form.dispatchValue(id,"cost_variant","NO_COST");
		form.dispatchValue(id,"constraint_variant","NONE");
		assertEquals(ImplementationState.IMPLEMENTED,session.state().draft().skills().get(0).implementationState());
		assertTrue(session.state().commandDiagnostics().toString(),session.state().commandDiagnostics().isEmpty());
	}

	@Test public void unsupportedOrGenericEditsDoNotMutateTypedSkill() {
		PlayerBuildSession session=P03TestBuilds.directDamage("p03-form-reject",7,false,false);
		String id=session.state().draft().skills().get(0).id().value();CanonicalBuildCodec codec=new CanonicalBuildCodec();String before=codec.serialize(session.state().draft());
		session.dispatch(new BuilderCommand.SelectEffectVariant(id,"PRIMARY","ADD_MARK"));
		assertFalse(session.state().commandDiagnostics().isEmpty());assertEquals(before,codec.serialize(session.state().draft()));
		session.dispatch(new BuilderCommand.SetFieldValue(id,"SKILL_V0_2","power","99"));
		assertFalse(session.state().commandDiagnostics().isEmpty());assertEquals(before,codec.serialize(session.state().draft()));
	}
}
