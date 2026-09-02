package com.shatteredpixel.shatteredpixeldungeon.rules.migration.v5;

import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.MigrationIdGenerator;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.migration.MigrationReport;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.migration.MigrationResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ResourceState;
import java.util.ArrayList;import java.util.Collections;import java.util.HashSet;import java.util.List;import java.util.Set;

/** Replayable P01 skeleton. Only Resource declaration/current-state migration is authoritative. */
public final class V5ToV6Migration {
	public MigrationResult migrate(ClassBuild legacy,String namespace){if(legacy==null)throw new IllegalArgumentException("legacy build is required");if(legacy.schemaVersion<1||legacy.schemaVersion>ClassBuild.SCHEMA_VERSION)throw new IllegalArgumentException("unsupported v5 source schema "+legacy.schemaVersion);MigrationIdGenerator ids=new MigrationIdGenerator(namespace);StableId buildId=ids.idFor("build","class_build");DisplayName buildName=safeName(legacy.name,"Migrated Class");ClassBuildSpec.Builder build=ClassBuildSpec.builder(buildId,buildName);ClassRuntimeState.Builder runtime=ClassRuntimeState.builder(buildId);List<MigrationReport.Change> changes=new ArrayList<>();List<MigrationReport.Warning> warnings=new ArrayList<>();Set<String> seen=new HashSet<>();boolean preserved=true;
		for(int i=0;i<legacy.resources.size();i++){com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec old=legacy.resources.get(i);if(old==null){warnings.add(new MigrationReport.Warning("resources["+i+"]","NULL_LEGACY_RESOURCE","null resource skipped"));preserved=false;continue;}String path="resources["+i+"]";StableId id;if(old.id!=null&&!old.id.isEmpty()&&seen.add(old.id)){id=StableId.fromStored(old.id);changes.add(new MigrationReport.Change(path+".id","PRESERVED_ID",old.id));}else{id=ids.idFor("res",path);warnings.add(new MigrationReport.Warning(path+".id",old.id==null||old.id.isEmpty()?"EMPTY_ID_REPLACED":"DUPLICATE_ID_REPLACED",id.value()));seen.add(id.value());}
			int minimum=old.minimum;int maximum=old.capacity;if(maximum<=minimum){maximum=minimum+1;warnings.add(new MigrationReport.Warning(path+".maximum","INVALID_BOUNDS_TIGHTENED","maximum raised to minimum+1"));preserved=false;}int initial=clamp(old.initialValue,minimum,maximum);int current=clamp(old.current,minimum,maximum);DisplayName name=safeName(old.name,"Resource "+(i+1));ResourceSpec resource=new ResourceSpec(id,name,minimum,maximum,initial,ResourceSpec.ResourceOverflowPolicy.CLAMP,new ResourceSpec.ResourceHudSpec(true,i,""));build.addResource(resource);runtime.addResource(new ResourceState(new ResourceRef(id,name.text()),current,Collections.emptyList(),Collections.emptyList()));changes.add(new MigrationReport.Change(path+".current","MOVED_TO_RUNTIME_STATE",Integer.toString(current)));}
		if(hasSemantic(legacy.gameplayComponents)){deferred(changes,warnings,"gameplayComponents",legacy.gameplayComponents.size());preserved=false;}
		if(hasSemantic(legacy.skills)){deferred(changes,warnings,"skills",legacy.skills.size());preserved=false;}
		if(hasSemantic(legacy.laws)){deferred(changes,warnings,"laws",legacy.laws.size());preserved=false;}
		if(hasSemantic(legacy.traits)){deferred(changes,warnings,"traits",legacy.traits.size());preserved=false;}
		if(hasSemantic(legacy.restrictions)){deferred(changes,warnings,"restrictions",legacy.restrictions.size());preserved=false;}
		if(hasSemantic(legacy.operations)){deferred(changes,warnings,"operations",legacy.operations.size());preserved=false;}
		if(legacy.startingKit!=null){deferred(changes,warnings,"startingKit",1);preserved=false;}
		if(legacy.progression!=null){deferred(changes,warnings,"progression",1);preserved=false;}
		deferred(changes,warnings,"baseBudget",1);preserved=false;
		MigrationReport report=new MigrationReport(legacy.schemaVersion,ClassBuildSpec.SCHEMA_VERSION,changes,Collections.emptyList(),warnings,preserved);return new MigrationResult(build.build(),runtime.build(),report);}
	private static boolean hasSemantic(List<?> values){if(values==null)return false;for(Object value:values)if(value!=null)return true;return false;}
	private static void deferred(List<MigrationReport.Change> changes,List<MigrationReport.Warning> warnings,String path,int count){changes.add(new MigrationReport.Change(path,"DEFERRED","P01 does not migrate "+count+" legacy value(s)"));warnings.add(new MigrationReport.Warning(path,"UNMIGRATED_SEMANTICS","legacy behavior is not preserved"));}
	private static int clamp(int value,int minimum,int maximum){return Math.max(minimum,Math.min(maximum,value));}
	private static DisplayName safeName(String value,String fallback){try{return DisplayName.of(value);}catch(RuntimeException ignored){return DisplayName.of(fallback);}}
}
