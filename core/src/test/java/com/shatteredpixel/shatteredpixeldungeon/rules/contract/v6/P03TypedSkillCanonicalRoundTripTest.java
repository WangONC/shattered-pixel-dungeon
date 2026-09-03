package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalDeepEquivalence;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalLoadResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import org.junit.Test;

import static org.junit.Assert.*;

/** Layer E: every typed P03 field survives canonical deep roundtrip. */
public class P03TypedSkillCanonicalRoundTripTest {
	@Test public void typedPrimaryAndImmediateSecondaryRoundTripByteCanonically() {
		PlayerBuildSession session=P03TestBuilds.directDamage("p03-canonical",7,true,true);CanonicalBuildCodec codec=new CanonicalBuildCodec();String json=codec.serialize(session.state().draft());
		for(String key:new String[]{"skill_version","activation","condition","effects","chain_id","primary","secondary","effect_id","amount","damage_type","defense_policy","delivery","requires_line_of_sight","targeting","selector","coverage","filter","filter_relation","filter_include_self","range","maximum_targets","line_of_sight","ordering","modifier","cost","constraint"})assertTrue(key,json.contains("\""+key+"\""));
		CanonicalLoadResult<ClassBuildSpec> loaded=codec.deserialize(json);assertEquals(DependencyState.RESOLVED,loaded.state());assertNotNull(loaded.value());assertEquals(json,codec.serialize(loaded.value()));assertTrue(new CanonicalDeepEquivalence().equivalent(session.state().draft(),loaded.value()));
	}

	@Test public void unknownOrDeclaredButUnavailableEffectNeverFallsBack() {
		CanonicalBuildCodec codec=new CanonicalBuildCodec();String json=codec.serialize(P03TestBuilds.directDamage("p03-canonical-reject",7,false,false).state().draft());
		CanonicalLoadResult<ClassBuildSpec> unknown=codec.deserialize(json.replace("\"variant_key\":\"DIRECT_DAMAGE\"","\"variant_key\":\"DOES_NOT_EXIST\""));
		assertEquals(DependencyState.UNSUPPORTED,unknown.state());assertNull(unknown.value());
		CanonicalLoadResult<ClassBuildSpec> declared=codec.deserialize(json.replace("\"variant_key\":\"DIRECT_DAMAGE\"","\"variant_key\":\"ADD_MARK\""));
		assertEquals(DependencyState.UNSUPPORTED,declared.state());assertNull(declared.value());assertTrue(declared.diagnostics().toString().contains("unavailable"));
	}
}
