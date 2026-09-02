package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalDeepEquivalence;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalLoadResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalRuntimeCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/** Every canonical P01 field is enumerated; each non-discriminator field gets its own mutation. */
public class CanonicalFieldMutationSuiteTest {
	private static final Set<String> EXPECTED_FIELDS = new HashSet<>(Arrays.asList(
			"format","schema_version","contract_version","build_id","display_name","resources","variant","id",
			"implementation_state","minimum","maximum","initial_value","default_overflow_policy","hud","visible",
			"order","presentation_key","marks","kind","duration_policy","default_duration_turns","refresh_policy",
			"overflow_policy","provenance_policy","mode_groups","policy","modes","group","ref_kind","target_id",
			"last_known_display_name","initial","capacities","entity_types","entities","entity_type","body","facet_kind",
			"variant_key","spawn_policy","ownership","relation","capacity","persistence","capabilities","capability_kind",
			"slots","resource","maximum_override","ability_pools","properties","value_kind","maximum_stack","recipes",
			"inputs","property","amount","output_variant","class_components","class_constraints","class_operations","skills",
			"node_kind","starting_kit","section_key","progression","budget_metadata","price_version","base_budget","current",
			"reservations","reservation_id","remaining_turns","suppressions","suppression_id","mode_key","subject_actor_id",
			"source_actor_id","value","transfer_depth","instance_id","blueprint","owner_actor_id","cell","created_order",
			"capability_states","state_id","scheduled_payloads","attachments","cooldowns","node_id","uses_this_floor",
			"snapshots","learned_abilities","provenance","next_runtime_entity_id","next_payload_instance_id","next_event_id","mode","mark"));

	@Test public void everyCanonicalBuildAndRuntimeFieldHasAnEnumeratedMutationCase(){
		P01Fixtures fixture=new P01Fixtures();CanonicalBuildCodec builds=new CanonicalBuildCodec();CanonicalRuntimeCodec states=new CanonicalRuntimeCodec();
		String build=builds.serialize(fixture.build());String runtime=states.serialize(fixture.runtime());Set<String> fields=new HashSet<>();
		List<MutationCase> buildCases=cases(build,fields);List<MutationCase> runtimeCases=cases(runtime,fields);assertEquals("Canonical schema changed without updating the mutation manifest",EXPECTED_FIELDS,fields);
		assertTrue("Build mutation suite unexpectedly small",buildCases.size()>100);assertTrue("Runtime mutation suite unexpectedly small",runtimeCases.size()>65);
		for(MutationCase mutation:buildCases)verifyBuildMutation(build,mutation,builds);for(MutationCase mutation:runtimeCases)verifyRuntimeMutation(runtime,mutation,states);
	}

	private static void verifyBuildMutation(String baseline,MutationCase mutation,CanonicalBuildCodec codec){JsonValue root=new JsonReader().parse(baseline);JsonValue target=locate(root,mutation.positions);mutate(target,mutation.path);String raw=root.toJson(JsonWriter.OutputType.json);assertNotEquals(mutation.path,baseline,raw);CanonicalLoadResult<ClassBuildSpec> first=codec.deserialize(raw);assertNotNull(mutation.path+" "+first.diagnostics(),first.value());String canonical=codec.serialize(first.value());assertNotEquals(mutation.path,baseline,canonical);CanonicalLoadResult<ClassBuildSpec> second=codec.deserialize(canonical);assertNotNull(mutation.path,second.value());assertTrue(mutation.path,new CanonicalDeepEquivalence().equivalent(first.value(),second.value()));}
	private static void verifyRuntimeMutation(String baseline,MutationCase mutation,CanonicalRuntimeCodec codec){JsonValue root=new JsonReader().parse(baseline);JsonValue target=locate(root,mutation.positions);mutate(target,mutation.path);String raw=root.toJson(JsonWriter.OutputType.json);assertNotEquals(mutation.path,baseline,raw);CanonicalLoadResult<ClassRuntimeState> first=codec.deserialize(raw);assertNotNull(mutation.path+" "+first.diagnostics(),first.value());String canonical=codec.serialize(first.value());assertNotEquals(mutation.path,baseline,canonical);CanonicalLoadResult<ClassRuntimeState> second=codec.deserialize(canonical);assertNotNull(mutation.path,second.value());assertTrue(mutation.path,new CanonicalDeepEquivalence().equivalent(first.value(),second.value()));}

	private static List<MutationCase> cases(String json,Set<String> fields){List<MutationCase> result=new ArrayList<>();collect(new JsonReader().parse(json),new ArrayList<Integer>(),"",null,result,fields);return result;}
	private static void collect(JsonValue value,List<Integer> positions,String path,String arrayName,List<MutationCase> result,Set<String> fields){int index=0;for(JsonValue child=value.child;child!=null;child=child.next,index++){List<Integer> childPositions=new ArrayList<>(positions);childPositions.add(index);String name=child.name==null?"["+index+"]":child.name;String childPath=path+"/"+name;if(child.name!=null)fields.add(child.name);if(child.isArray()){if(child.size>0)result.add(new MutationCase(childPath,childPositions));collect(child,childPositions,childPath,child.name,result,fields);}else if(child.isObject())collect(child,childPositions,childPath,arrayName,result,fields);else if(!structural(child,childPath,arrayName))result.add(new MutationCase(childPath,childPositions));}}
	private static boolean structural(JsonValue value,String path,String arrayName){String name=value.name;if("format".equals(name)||"schema_version".equals(name)||"contract_version".equals(name)||"variant".equals(name)||"ref_kind".equals(name)||"facet_kind".equals(name)||"capability_kind".equals(name)||"node_kind".equals(name))return true;if("variant_key".equals(name)&&"RESOURCE_STORAGE".equals(value.asString()))return true;if("implementation_state".equals(name)){JsonValue parent=value.parent;if(parent!=null&&parent.get("capability_kind")!=null&&"RESOURCE_STORAGE".equals(parent.getString("capability_kind")))return true;if("DECLARED".equals(value.asString())&&path.indexOf("/entities/")<0&&path.indexOf("/recipes/")<0)return true;}return false;}

	private static JsonValue locate(JsonValue root,List<Integer> positions){JsonValue value=root;for(Integer wanted:positions){JsonValue child=value.child;for(int i=0;i<wanted;i++)child=child.next;value=child;}return value;}
	private static void mutate(JsonValue value,String path){if(value.isArray()){value.remove(value.size-1);return;}if(value.isBoolean()){value.set(!value.asBoolean());return;}if(value.isNumber()){long current=value.asLong();if("minimum".equals(value.name))setLong(value,current-1);else if("default_duration_turns".equals(value.name)&&path.contains("/modes/")){setLong(value,1L);value.parent.get("duration_policy").set("TURN_BASED");}else setLong(value,current+1);return;}if(!value.isString())throw new AssertionError("No mutation for "+path+" type "+value.type());String current=value.asString();String name=value.name;if("duration_policy".equals(name)&&"PERSISTENT".equals(current)){value.set("TURN_BASED");setLong(value.parent.get("default_duration_turns"),1L);return;}if("duration_policy".equals(name)&&"TURN_BASED".equals(current)&&path.contains("/marks/")){value.set("END_OF_LEVEL");setLong(value.parent.get("default_duration_turns"),0L);return;}String alternative=enumAlternative(name,current,path);if(alternative!=null){value.set(alternative);return;}if(name!=null&&(name.endsWith("_id")||"id".equals(name))){value.set(mutateId(current));return;}value.set(current+"-mutated");}
	private static void setLong(JsonValue value,long replacement){value.set(new JsonValue(replacement));}
	private static String enumAlternative(String name,String current,String path){if("implementation_state".equals(name))return "UNSUPPORTED".equals(current)?"DEFERRED":"UNSUPPORTED";if("default_overflow_policy".equals(name))return "CLAMP";if("kind".equals(name))return "CHARGE";if("duration_policy".equals(name))return "END_OF_LEVEL";if("refresh_policy".equals(name))return "ADD_DURATION";if("overflow_policy".equals(name)){if("CLAMP".equals(current))return "REJECT";return path.contains("/ability_pools/")?"REPLACE_OLDEST":"REMOVE_OLDEST";}if("provenance_policy".equals(name))return "NONE";if("policy".equals(name))return "INDEPENDENT";if("entity_type".equals(name))return "TRAP";if("value_kind".equals(name))return "MATERIAL";if(name==null&&path.contains("/entity_types/"))return "DEVICE".equals(current)?"ACTOR":"FIELD";return null;}
	private static String mutateId(String value){if(value.isEmpty())return "mutated";char last=value.charAt(value.length()-1);return value.substring(0,value.length()-1)+(last=='a'?'b':'a');}

	private static final class MutationCase{private final String path;private final List<Integer> positions;private MutationCase(String path,List<Integer> positions){this.path=path;this.positions=positions;}}
}
