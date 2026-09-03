package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Immutable compiler output. Only EXECUTABLE plans may cross into V6RuleRuntime. */
public final class ClassCompilePlan {
	public enum Kind { PREVIEW, PARTIAL, EXECUTABLE }
	private final StableId buildId;
	private final String canonicalSpecHash;
	private final int schemaVersion;
	private final String priceVersion;
	private final String runtimeVersion;
	private final Kind kind;
	private final List<CompiledResource> resources;
	private final List<CompiledClassComponent> components;
	private final List<CompiledClassOperation> operations;
	private final List<CompiledSkill> skills;
	private final List<CompileDiagnostic> diagnostics;
	ClassCompilePlan(StableId buildId,String canonicalSpecHash,int schemaVersion,String priceVersion,
			String runtimeVersion,Kind kind,List<CompiledSkill> skills,List<CompileDiagnostic> diagnostics){
		this(buildId,canonicalSpecHash,schemaVersion,priceVersion,runtimeVersion,kind,Collections.<CompiledResource>emptyList(),Collections.<CompiledClassComponent>emptyList(),Collections.<CompiledClassOperation>emptyList(),skills,diagnostics);
	}
	ClassCompilePlan(StableId buildId,String canonicalSpecHash,int schemaVersion,String priceVersion,
			String runtimeVersion,Kind kind,List<CompiledResource> resources,List<CompiledClassComponent> components,
			List<CompiledClassOperation> operations,List<CompiledSkill> skills,List<CompileDiagnostic> diagnostics){
		if(buildId==null||canonicalSpecHash==null||canonicalSpecHash.isEmpty()||priceVersion==null
				||runtimeVersion==null||runtimeVersion.isEmpty()||kind==null||resources==null||components==null||operations==null||skills==null||diagnostics==null)
			throw new IllegalArgumentException("compile plan fields required");
		if(kind==Kind.EXECUTABLE&&(!diagnostics.isEmpty()||(skills.isEmpty()&&components.isEmpty()&&operations.isEmpty())))
			throw new IllegalArgumentException("executable plan must be complete and diagnostic-free");
		this.buildId=buildId;this.canonicalSpecHash=canonicalSpecHash;this.schemaVersion=schemaVersion;
		this.priceVersion=priceVersion;this.runtimeVersion=runtimeVersion;this.kind=kind;
		this.resources=Collections.unmodifiableList(new ArrayList<>(resources));
		this.components=Collections.unmodifiableList(new ArrayList<>(components));
		this.operations=Collections.unmodifiableList(new ArrayList<>(operations));
		this.skills=Collections.unmodifiableList(new ArrayList<>(skills));
		this.diagnostics=Collections.unmodifiableList(new ArrayList<>(diagnostics));
	}
	public StableId buildId(){return buildId;}public String canonicalSpecHash(){return canonicalSpecHash;}
	public int schemaVersion(){return schemaVersion;}public String priceVersion(){return priceVersion;}
	public String runtimeVersion(){return runtimeVersion;}public Kind kind(){return kind;}
	public List<CompiledResource> resources(){return resources;}public List<CompiledClassComponent> components(){return components;}public List<CompiledClassOperation> operations(){return operations;}
	public List<CompiledSkill> skills(){return skills;}public List<CompileDiagnostic> diagnostics(){return diagnostics;}
	public boolean executable(){return kind==Kind.EXECUTABLE;}
	/** Kept as a source-compatible alias for existing P03 callers. */
	public boolean ready(){return executable();}
	public CompiledSkill find(StableId skillId){if(skillId==null)return null;for(CompiledSkill value:skills)if(value.skillId().equals(skillId))return value;return null;}
	public CompiledClassOperation findOperation(StableId id){if(id==null)return null;for(CompiledClassOperation value:operations)if(value.id().equals(id))return value;return null;}
	public CompiledResource findResource(StableId id){if(id==null)return null;for(CompiledResource value:resources)if(value.id().equals(id))return value;return null;}
	public Set<CompiledSkill.EffectVariant> requiredEffectVariants(){
		EnumSet<CompiledSkill.EffectVariant> result=EnumSet.noneOf(CompiledSkill.EffectVariant.class);
		for(CompiledSkill skill:skills){result.add(skill.effectChain().primary().variant());if(skill.effectChain().secondary()!=null)result.add(skill.effectChain().secondary().effect().variant());}
		return Collections.unmodifiableSet(result);
	}
}
