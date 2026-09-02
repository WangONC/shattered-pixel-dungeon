package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.edit.BuildEditService;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalDeepEquivalence;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalRuntimeCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.HeroClassBundleCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ResourceState;
import com.watabou.utils.Bundle;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class AuthoritativeHeroLoadTest {
	@Test public void deletingModeGroupLoadsAsUnresolvedWithoutFallback(){P01Fixtures f=new P01Fixtures();ClassBuildSpec deleted=new BuildEditService().delete(f.declarationsOnly(),f.modeGroup.id());HeroClassBundleCodec.LoadPair loaded=roundtrip(deleted,emptyRuntime(f.buildId));assertEquals(DependencyState.UNRESOLVED,loaded.state());assertEquals(f.modeGroup.id(),loaded.build().modes().get(0).group().targetId());}

	@Test public void duplicateDeclarationIdLoadsAsHardConflict(){P01Fixtures f=new P01Fixtures();ResourceSpec duplicate=new ResourceSpec(f.resource.id(),DisplayName.of("Duplicate Rage"),0,12,1,ResourceSpec.ResourceOverflowPolicy.CLAMP,new ResourceSpec.ResourceHudSpec(false,2,"duplicate"));ClassBuildSpec build=f.declarationsOnly().toBuilder().addResource(duplicate).build();assertEquals(DependencyState.HARD_CONFLICT,roundtrip(build,emptyRuntime(f.buildId)).state());}

	@Test public void missingRuntimeResourceReferenceLoadsAsUnresolved(){P01Fixtures f=new P01Fixtures();StableId missing=f.ids.nextId("res");ClassRuntimeState state=ClassRuntimeState.builder(f.buildId).addResource(new ResourceState(new ResourceRef(missing,"Missing Resource"),1,Collections.emptyList(),Collections.emptyList())).build();assertEquals(DependencyState.UNRESOLVED,roundtrip(f.declarationsOnly(),state).state());}

	@Test public void deferredAndUnsupportedNodesNeverLoadAsFullyResolved(){P01Fixtures f=new P01Fixtures();assertEquals(DependencyState.UNSUPPORTED,roundtrip(f.build(),emptyRuntime(f.buildId)).state());}

	@Test public void buildIdMismatchLoadsAsHardConflict(){P01Fixtures f=new P01Fixtures();ClassRuntimeState other=emptyRuntime(f.ids.nextId("build"));Hero restored=restoreRaw(new CanonicalBuildCodec().serialize(f.declarationsOnly()),new CanonicalRuntimeCodec().serialize(other));assertEquals(DependencyState.HARD_CONFLICT,new HeroClassBundleCodec().load(restored).state());}

	@Test public void duplicateRuntimeResourceTargetIsRejectedEvenWhenDisplayNamesDiffer(){P01Fixtures f=new P01Fixtures();ClassRuntimeState one=ClassRuntimeState.builder(f.buildId).addResource(new ResourceState(new ResourceRef(f.resource.id(),"Rage"),3,Collections.emptyList(),Collections.emptyList())).build();String json=new CanonicalRuntimeCodec().serialize(one);int arrayStart=json.indexOf("\"resources\":[")+"\"resources\":[".length();int objectEnd=matchingObjectEnd(json,arrayStart);String entry=json.substring(arrayStart,objectEnd+1);String duplicate=entry.replace("Rage","Renamed Rage");String invalid=json.substring(0,objectEnd+1)+","+duplicate+json.substring(objectEnd+1);Hero restored=restoreRaw(new CanonicalBuildCodec().serialize(f.declarationsOnly()),invalid);assertEquals(DependencyState.HARD_CONFLICT,new HeroClassBundleCodec().load(restored).state());}

	@Test public void authoritativeLoadDoesNotMutateCanonicalBuildOrRuntime(){P01Fixtures f=new P01Fixtures();ClassBuildSpec build=f.build();ClassRuntimeState state=f.runtime();CanonicalBuildCodec builds=new CanonicalBuildCodec();CanonicalRuntimeCodec states=new CanonicalRuntimeCodec();String buildBefore=builds.serialize(build);String stateBefore=states.serialize(state);HeroClassBundleCodec.LoadPair loaded=roundtrip(build,state);assertEquals(buildBefore,builds.serialize(build));assertEquals(stateBefore,states.serialize(state));assertEquals(buildBefore,builds.serialize(loaded.build()));assertEquals(stateBefore,states.serialize(loaded.runtime()));assertTrue(new CanonicalDeepEquivalence().equivalent(build,loaded.build()));assertTrue(new CanonicalDeepEquivalence().equivalent(state,loaded.runtime()));}

	@Test(expected=IllegalArgumentException.class)public void runtimeBuilderRejectsSameResourceTargetWithDifferentDisplayNames(){P01Fixtures f=new P01Fixtures();ClassRuntimeState.builder(f.buildId).addResource(new ResourceState(new ResourceRef(f.resource.id(),"Before"),1,Collections.emptyList(),Collections.emptyList())).addResource(new ResourceState(new ResourceRef(f.resource.id(),"After"),2,Collections.emptyList(),Collections.emptyList()));}

	private static ClassRuntimeState emptyRuntime(StableId buildId){return ClassRuntimeState.builder(buildId).build();}
	private static HeroClassBundleCodec.LoadPair roundtrip(ClassBuildSpec build,ClassRuntimeState state){Hero original=new Hero();Talent.initClassTalents(original);HeroClassBundleCodec codec=new HeroClassBundleCodec();codec.store(original,build,state);Bundle bundle=new Bundle();original.storeInBundle(bundle);Hero restored=new Hero();restored.restoreFromBundle(bundle);return codec.load(restored);}
	private static Hero restoreRaw(String build,String runtime){Hero original=new Hero();Talent.initClassTalents(original);original.setGameplayComponentsV6Payloads(build,runtime);Bundle bundle=new Bundle();original.storeInBundle(bundle);Hero restored=new Hero();restored.restoreFromBundle(bundle);return restored;}
	private static int matchingObjectEnd(String json,int start){int depth=0;boolean quoted=false;for(int i=start;i<json.length();i++){char c=json.charAt(i);if(c=='"'&&(i==0||json.charAt(i-1)!='\\'))quoted=!quoted;if(quoted)continue;if(c=='{')depth++;else if(c=='}'&&--depth==0)return i;}throw new AssertionError("resource object not found");}
}
