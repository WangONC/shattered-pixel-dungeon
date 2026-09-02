package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import com.watabou.utils.Bundle;

/** Contract-side Hero bundle adapter. The two authoritative payloads are deliberately separate. */
public final class HeroClassBundleCodec {
	public static final String CLASS_BUILD_SPEC="class_build_spec_v6";public static final String CLASS_RUNTIME_STATE="class_runtime_state_v6";
	private final CanonicalBuildCodec builds=new CanonicalBuildCodec();private final CanonicalRuntimeCodec runtime=new CanonicalRuntimeCodec();
	public void store(Bundle heroBundle,ClassBuildSpec build,ClassRuntimeState state){if(heroBundle==null||build==null||state==null)throw new IllegalArgumentException("hero bundle, build, and state are required");if(!build.buildId().equals(state.buildId()))throw new IllegalArgumentException("runtime build id mismatch");heroBundle.put(CLASS_BUILD_SPEC,builds.serialize(build));heroBundle.put(CLASS_RUNTIME_STATE,runtime.serialize(state));}
	public LoadPair load(Bundle heroBundle){if(heroBundle==null||!heroBundle.contains(CLASS_BUILD_SPEC)||!heroBundle.contains(CLASS_RUNTIME_STATE))return new LoadPair(DependencyState.UNRESOLVED,null,null,"v6 hero bundle payload missing");CanonicalLoadResult<ClassBuildSpec> build=builds.deserialize(heroBundle.getString(CLASS_BUILD_SPEC));if(build.value()==null)return new LoadPair(build.state(),null,null,build.diagnostics().toString());CanonicalLoadResult<ClassRuntimeState> state=runtime.deserialize(heroBundle.getString(CLASS_RUNTIME_STATE));if(state.value()==null)return new LoadPair(state.state(),build.value(),null,state.diagnostics().toString());if(!build.value().buildId().equals(state.value().buildId()))return new LoadPair(DependencyState.HARD_CONFLICT,build.value(),state.value(),"runtime build id mismatch");return new LoadPair(DependencyState.RESOLVED,build.value(),state.value(),"");}
	public static final class LoadPair{private final DependencyState state;private final ClassBuildSpec build;private final ClassRuntimeState runtime;private final String diagnostic;LoadPair(DependencyState state,ClassBuildSpec build,ClassRuntimeState runtime,String diagnostic){this.state=state;this.build=build;this.runtime=runtime;this.diagnostic=diagnostic;}public DependencyState state(){return state;}public ClassBuildSpec build(){return build;}public ClassRuntimeState runtime(){return runtime;}public String diagnostic(){return diagnostic;}}
}
