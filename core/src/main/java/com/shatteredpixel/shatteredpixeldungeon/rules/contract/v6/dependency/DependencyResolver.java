package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;
import java.util.ArrayList;import java.util.Collections;import java.util.HashMap;import java.util.HashSet;import java.util.LinkedHashMap;import java.util.List;import java.util.Map;import java.util.Set;

/** Pure read-only declaration resolver. It never fills, rewrites, removes, or reorders references. */
public final class DependencyResolver {
	public DependencyReport resolve(ClassBuildSpec build){return resolve(build,Collections.emptyList());}
	public DependencyReport resolveReferences(ClassBuildSpec build,List<DependencyRequest> requests){if(build==null||requests==null)throw new IllegalArgumentException("build and requests are required");Map<StableId,List<StableTarget>> index=index(build);List<DependencyDiagnostic> result=new ArrayList<>();for(DependencyRequest request:requests)result.add(resolveOne(index,request));return new DependencyReport(result);}
	public DependencyReport resolve(ClassBuildSpec build,List<DependencyRequest> additional){
		if(build==null||additional==null)throw new IllegalArgumentException("build and requests are required");
		List<DependencyDiagnostic> result=new ArrayList<>();Map<StableId,List<StableTarget>> index=index(build);
		for(Map.Entry<StableId,List<StableTarget>> entry:index.entrySet())if(entry.getValue().size()>1){
			for(StableTarget duplicate:entry.getValue())result.add(new DependencyDiagnostic(duplicate.id(),"id",DependencyState.HARD_CONFLICT,entry.getKey(),"dependency.duplicate_id"));
		}
		List<DependencyRequest> requests=new ArrayList<>();
		for(ModeSpec mode:build.modes())requests.add(new DependencyRequest(mode.id(),"group",mode.group()));
		for(EntitySpec entity:build.entities()){
			if(entity.capacity()!=null)requests.add(new DependencyRequest(entity.id(),"capacity",entity.capacity()));
			for(int capabilityIndex=0;capabilityIndex<entity.capabilities().size();capabilityIndex++){
				EntityCapabilitySpec capability=entity.capabilities().get(capabilityIndex);
				if(capability instanceof ResourceStorageCapability){
					List<EntityResourceSlotSpec> slots=((ResourceStorageCapability)capability).slots();
					for(int slotIndex=0;slotIndex<slots.size();slotIndex++)requests.add(new DependencyRequest(entity.id(),"capabilities["+capabilityIndex+"]"+".slots["+slotIndex+"].resource",slots.get(slotIndex).resource()));
				}
			}
		}
		for(SynthesisRecipeSpec recipe:build.recipes())for(int i=0;i<recipe.inputs().size();i++)requests.add(new DependencyRequest(recipe.id(),"inputs["+i+"].property",recipe.inputs().get(i).property()));
		requests.addAll(additional);
		Set<StableId> cyclicNodes=cyclicNodes(index,requests);
		for(DependencyRequest request:requests){
			if(cyclicNodes.contains(request.ownerNodeId())&&cyclicNodes.contains(request.reference().targetId()))result.add(diagnostic(request,DependencyState.HARD_CONFLICT,"dependency.illegal_cycle"));
			else result.add(resolveOne(index,request));
		}
		return new DependencyReport(result);
	}

	private static Map<StableId,List<StableTarget>> index(ClassBuildSpec build){Map<StableId,List<StableTarget>> result=new LinkedHashMap<>();for(StableTarget value:build.allTargets())result.computeIfAbsent(value.id(),key->new ArrayList<>()).add(value);return result;}
	private static DependencyDiagnostic resolveOne(Map<StableId,List<StableTarget>> index,DependencyRequest request){
		List<StableTarget> matches=index.get(request.reference().targetId());
		if(matches==null||matches.isEmpty())return diagnostic(request,DependencyState.UNRESOLVED,"dependency.target_missing");
		if(matches.size()>1)return diagnostic(request,DependencyState.HARD_CONFLICT,"dependency.target_duplicate");
		StableTarget target=matches.get(0);
		if(target.refKind()!=request.reference().kind())return diagnostic(request,DependencyState.HARD_CONFLICT,"dependency.target_type_mismatch");
		if(target.implementationState()==ImplementationState.UNSUPPORTED||target.implementationState()==ImplementationState.DEFERRED)return diagnostic(request,DependencyState.UNSUPPORTED,"dependency.target_unsupported");
		return diagnostic(request,DependencyState.RESOLVED,"dependency.resolved");
	}
	private static DependencyDiagnostic diagnostic(DependencyRequest request,DependencyState state,String key){return new DependencyDiagnostic(request.ownerNodeId(),request.fieldPath(),state,request.reference().targetId(),key);}

	private static Set<StableId> cyclicNodes(Map<StableId,List<StableTarget>> index,List<DependencyRequest> requests){
		Map<StableId,List<DependencyRequest>> outgoing=new HashMap<>();
		for(DependencyRequest request:requests){List<StableTarget> target=index.get(request.reference().targetId());if(index.containsKey(request.ownerNodeId())&&target!=null&&target.size()==1&&target.get(0).refKind()==request.reference().kind())outgoing.computeIfAbsent(request.ownerNodeId(),key->new ArrayList<>()).add(request);}
		Set<StableId> visiting=new HashSet<>(),visited=new HashSet<>(),cycles=new HashSet<>();List<StableId> stack=new ArrayList<>();
		for(StableId node:outgoing.keySet())dfs(node,outgoing,visiting,visited,stack,cycles);
		return cycles;
	}
	private static void dfs(StableId node,Map<StableId,List<DependencyRequest>> outgoing,Set<StableId> visiting,Set<StableId> visited,List<StableId> stack,Set<StableId> cycles){
		if(visited.contains(node))return;visiting.add(node);stack.add(node);
		for(DependencyRequest edge:outgoing.getOrDefault(node,Collections.emptyList())){StableId target=edge.reference().targetId();if(visiting.contains(target)){int start=stack.indexOf(target);for(int i=start;i<stack.size();i++)cycles.add(stack.get(i));}else if(!visited.contains(target))dfs(target,outgoing,visiting,visited,stack,cycles);}
		stack.remove(stack.size()-1);visiting.remove(node);visited.add(node);
	}
}
