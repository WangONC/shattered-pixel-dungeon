package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ResourceState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledResource;
import java.util.Collections;import java.util.HashSet;import java.util.Set;

/** Explicit new-build initialization and same-build compatibility checks. */
public final class ResourceRuntimeStateFactory {
	private ResourceRuntimeStateFactory(){}
	public static ClassRuntimeState initialize(ClassBuildSpec build){ClassRuntimeState.Builder state=ClassRuntimeState.builder(build.buildId());for(ResourceSpec resource:build.resources())state.addResource(new ResourceState(new ResourceRef(resource.id(),resource.displayName().fallback()),resource.initialValue(),Collections.<ResourceState.Reservation>emptyList(),Collections.<ResourceState.Suppression>emptyList()));return state.build();}
	public static ClassRuntimeState initialize(ClassCompilePlan plan){ClassRuntimeState.Builder state=ClassRuntimeState.builder(plan.buildId());for(CompiledResource resource:plan.resources())state.addResource(new ResourceState(new ResourceRef(resource.id(),resource.displayName()),resource.initialValue(),Collections.<ResourceState.Reservation>emptyList(),Collections.<ResourceState.Suppression>emptyList()));return state.build();}
	public static String incompatibility(ClassBuildSpec build,ClassRuntimeState state){if(state==null)return "runtime.state_absent";if(!build.buildId().equals(state.buildId()))return "runtime.build_id_mismatch";Set<com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId> expected=new HashSet<>();for(ResourceSpec spec:build.resources()){expected.add(spec.id());ResourceState current=state.resources().get(new ResourceRef(spec.id(),""));if(current==null)return "runtime.resource_missing:"+spec.id().value();if(current.current()<spec.minimum()||current.current()>spec.maximum())return "runtime.resource_out_of_bounds:"+spec.id().value();}for(ResourceRef ref:state.resources().keySet())if(!expected.contains(ref.targetId()))return "runtime.resource_unknown:"+ref.targetId().value();return "";}
	public static String incompatibility(ClassCompilePlan plan,ClassRuntimeState state){if(state==null)return "runtime.state_absent";if(!plan.buildId().equals(state.buildId()))return "runtime.build_id_mismatch";Set<com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId> expected=new HashSet<>();for(CompiledResource spec:plan.resources()){expected.add(spec.id());ResourceState current=state.resources().get(new ResourceRef(spec.id(),""));if(current==null)return "runtime.resource_missing:"+spec.id().value();if(current.current()<spec.minimum()||current.current()>spec.maximum())return "runtime.resource_out_of_bounds:"+spec.id().value();}for(ResourceRef ref:state.resources().keySet())if(!expected.contains(ref.targetId()))return "runtime.resource_unknown:"+ref.targetId().value();return "";}
}
