package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;
import java.util.ArrayList;import java.util.HashMap;import java.util.List;import java.util.Map;

/** Structural validation is also read-only and never normalizes a draft. */
public final class ClassBuildValidator {
	public DependencyReport validate(ClassBuildSpec build){
		List<DependencyDiagnostic> result=new ArrayList<>();
		if(build.schemaVersion()!=ClassBuildSpec.SCHEMA_VERSION)result.add(new DependencyDiagnostic(build.buildId(),"schema_version",DependencyState.UNSUPPORTED,build.buildId(),"schema.unsupported"));
		if(!ClassBuildSpec.CONTRACT_VERSION.equals(build.contractVersion()))result.add(new DependencyDiagnostic(build.buildId(),"contract_version",DependencyState.HARD_CONFLICT,build.buildId(),"contract.version_mismatch"));
		for(StableTarget target:build.allTargets()){
			String expected=expectedPrefix(target);if(com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId.isCanonical(target.id().value())&&!expected.equals(target.id().prefix()))result.add(new DependencyDiagnostic(target.id(),"id",DependencyState.HARD_CONFLICT,target.id(),"identity.prefix_mismatch"));
			if(target.implementationState()==ImplementationState.UNSUPPORTED||target.implementationState()==ImplementationState.DEFERRED)result.add(new DependencyDiagnostic(target.id(),"implementation_state",DependencyState.UNSUPPORTED,target.id(),"node.unsupported"));
		}
		for (EntitySpec entity : build.entities()) {
			unsupported(result, entity.id(), "body", entity.body().implementationState());
			unsupported(result, entity.id(), "spawn_policy", entity.spawnPolicy().implementationState());
			unsupported(result, entity.id(), "ownership", entity.ownership().implementationState());
			unsupported(result, entity.id(), "relation", entity.relation().implementationState());
			unsupported(result, entity.id(), "persistence", entity.persistence().implementationState());
			for (int i = 0; i < entity.capabilities().size(); i++)
				unsupported(result, entity.id(), "capabilities[" + i + "]", entity.capabilities().get(i).implementationState());
		}
		unsupported(result, build.buildId(), "starting_kit", build.startingKit().state());
		unsupported(result, build.buildId(), "progression", build.progression().state());
		Map<String,Integer> initialByGroup=new HashMap<>();for(ModeSpec mode:build.modes())if(mode.initial())initialByGroup.put(mode.group().targetId().value(),initialByGroup.getOrDefault(mode.group().targetId().value(),0)+1);
		for(ModeGroupSpec group:build.modeGroups())if(group.policy()==ModeGroupSpec.ModeGroupPolicy.EXCLUSIVE&&initialByGroup.getOrDefault(group.id().value(),0)>1)result.add(new DependencyDiagnostic(group.id(),"initial_modes",DependencyState.HARD_CONFLICT,group.id(),"mode_group.multiple_initial"));
		return new DependencyReport(result);
	}
	private static void unsupported(List<DependencyDiagnostic> result, com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId owner, String path, ImplementationState state){
		if(state==ImplementationState.UNSUPPORTED||state==ImplementationState.DEFERRED)
			result.add(new DependencyDiagnostic(owner,path,DependencyState.UNSUPPORTED,owner,"node.unsupported"));
	}
	private static String expectedPrefix(StableTarget target){if(target instanceof ContractNodeSpec){switch(((ContractNodeSpec)target).nodeKind()){case COMPONENT:return"component";case CONSTRAINT:return"constraint";case OPERATION:return"op";case SKILL:return"skill";default:throw new AssertionError();}}switch(target.refKind()){case RESOURCE:return"res";case MARK:return"mark";case MODE_GROUP:return"modegrp";case MODE:return"mode";case ENTITY:return"entity";case CAPACITY:return"capacity";case ABILITY_POOL:return"abilitypool";case PROPERTY:return"property";case RECIPE:return"recipe";case COMPONENT:return"component";default:throw new AssertionError();}}
}
