package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;
import java.util.ArrayList;import java.util.Collections;import java.util.LinkedHashMap;import java.util.List;import java.util.Map;

/** Mutable-run data stored independently from ClassBuildSpec. This value object is immutable. */
public final class ClassRuntimeState {
	private final StableId buildId;private final Map<ResourceRef,ResourceState> resources;private final List<ModeState> modes;
	private final List<MarkState> marks;private final List<EntityInstanceState> entities;
	private final List<OpaqueRuntimeState> scheduledPayloads,attachments,snapshots,learnedAbilities;
	private final Map<StableId,Integer> cooldowns,usesThisFloor;private final List<PropertyInventoryState> properties;
	private final long nextRuntimeEntityId,nextPayloadInstanceId,nextEventId;
	private ClassRuntimeState(Builder value){if(value.buildId==null)throw new IllegalArgumentException("runtime build id is required");
		buildId=value.buildId;resources=resourceMap(value.resources);modes=copy(value.modes);marks=copy(value.marks);entities=copy(value.entities);
		scheduledPayloads=copy(value.scheduledPayloads);attachments=copy(value.attachments);snapshots=copy(value.snapshots);learnedAbilities=copy(value.learnedAbilities);
		cooldowns=map(value.cooldowns);usesThisFloor=map(value.usesThisFloor);properties=copy(value.properties);
		if(value.nextRuntimeEntityId<0||value.nextPayloadInstanceId<0||value.nextEventId<0)throw new IllegalArgumentException("next ids must be non-negative");
		nextRuntimeEntityId=value.nextRuntimeEntityId;nextPayloadInstanceId=value.nextPayloadInstanceId;nextEventId=value.nextEventId;}
	private static<T>List<T> copy(List<T> value){return Collections.unmodifiableList(new ArrayList<>(value));}
	private static<K,V>Map<K,V> map(Map<K,V> value){return Collections.unmodifiableMap(new LinkedHashMap<>(value));}
	private static Map<ResourceRef,ResourceState> resourceMap(Map<ResourceRef,ResourceState> value){LinkedHashMap<ResourceRef,ResourceState> result=new LinkedHashMap<>();for(Map.Entry<ResourceRef,ResourceState> entry:value.entrySet()){if(entry.getKey()==null||entry.getValue()==null||!entry.getKey().equals(entry.getValue().resource()))throw new IllegalArgumentException("resource state key must match its ResourceRef");if(result.put(entry.getKey(),entry.getValue())!=null)throw new IllegalArgumentException("duplicate runtime resource state "+entry.getKey().targetId());}return Collections.unmodifiableMap(result);}
	public StableId buildId(){return buildId;}public Map<ResourceRef,ResourceState> resources(){return resources;}public List<ModeState> modes(){return modes;}
	public List<MarkState> marks(){return marks;}public List<EntityInstanceState> entities(){return entities;}
	public List<OpaqueRuntimeState> scheduledPayloads(){return scheduledPayloads;}public List<OpaqueRuntimeState> attachments(){return attachments;}
	public List<OpaqueRuntimeState> snapshots(){return snapshots;}public List<OpaqueRuntimeState> learnedAbilities(){return learnedAbilities;}
	public Map<StableId,Integer> cooldowns(){return cooldowns;}public Map<StableId,Integer> usesThisFloor(){return usesThisFloor;}
	public List<PropertyInventoryState> properties(){return properties;}public long nextRuntimeEntityId(){return nextRuntimeEntityId;}
	public long nextPayloadInstanceId(){return nextPayloadInstanceId;}public long nextEventId(){return nextEventId;}
	public Builder toBuilder(){return new Builder(this);}public static Builder builder(StableId buildId){return new Builder().buildId(buildId);}
	public static final class Builder{
		private StableId buildId;private Map<ResourceRef,ResourceState> resources=new LinkedHashMap<>();private List<ModeState> modes=new ArrayList<>();private List<MarkState> marks=new ArrayList<>();
		private List<EntityInstanceState> entities=new ArrayList<>();private List<OpaqueRuntimeState> scheduledPayloads=new ArrayList<>(),attachments=new ArrayList<>(),snapshots=new ArrayList<>(),learnedAbilities=new ArrayList<>();
		private Map<StableId,Integer> cooldowns=new LinkedHashMap<>(),usesThisFloor=new LinkedHashMap<>();private List<PropertyInventoryState> properties=new ArrayList<>();
		private long nextRuntimeEntityId,nextPayloadInstanceId,nextEventId;public Builder(){}private Builder(ClassRuntimeState s){buildId=s.buildId;resources=new LinkedHashMap<>(s.resources);modes=new ArrayList<>(s.modes);marks=new ArrayList<>(s.marks);entities=new ArrayList<>(s.entities);scheduledPayloads=new ArrayList<>(s.scheduledPayloads);attachments=new ArrayList<>(s.attachments);snapshots=new ArrayList<>(s.snapshots);learnedAbilities=new ArrayList<>(s.learnedAbilities);cooldowns=new LinkedHashMap<>(s.cooldowns);usesThisFloor=new LinkedHashMap<>(s.usesThisFloor);properties=new ArrayList<>(s.properties);nextRuntimeEntityId=s.nextRuntimeEntityId;nextPayloadInstanceId=s.nextPayloadInstanceId;nextEventId=s.nextEventId;}
		public Builder buildId(StableId v){buildId=v;return this;}public Builder resources(Map<ResourceRef,ResourceState> v){resources=new LinkedHashMap<>(v);return this;}public Builder addResource(ResourceState v){if(v==null)throw new IllegalArgumentException("resource state is required");if(resources.put(v.resource(),v)!=null)throw new IllegalArgumentException("duplicate runtime resource state "+v.resource().targetId());return this;}
		public Builder modes(List<ModeState> v){modes=new ArrayList<>(v);return this;}public Builder addMode(ModeState v){modes.add(v);return this;}public Builder marks(List<MarkState> v){marks=new ArrayList<>(v);return this;}public Builder addMark(MarkState v){marks.add(v);return this;}
		public Builder entities(List<EntityInstanceState> v){entities=new ArrayList<>(v);return this;}public Builder addEntity(EntityInstanceState v){entities.add(v);return this;}
		public Builder scheduledPayloads(List<OpaqueRuntimeState> v){scheduledPayloads=new ArrayList<>(v);return this;}public Builder attachments(List<OpaqueRuntimeState> v){attachments=new ArrayList<>(v);return this;}public Builder snapshots(List<OpaqueRuntimeState> v){snapshots=new ArrayList<>(v);return this;}public Builder learnedAbilities(List<OpaqueRuntimeState> v){learnedAbilities=new ArrayList<>(v);return this;}
		public Builder cooldowns(Map<StableId,Integer> v){cooldowns=new LinkedHashMap<>(v);return this;}public Builder usesThisFloor(Map<StableId,Integer> v){usesThisFloor=new LinkedHashMap<>(v);return this;}public Builder properties(List<PropertyInventoryState> v){properties=new ArrayList<>(v);return this;}public Builder addProperty(PropertyInventoryState v){properties.add(v);return this;}
		public Builder nextRuntimeEntityId(long v){nextRuntimeEntityId=v;return this;}public Builder nextPayloadInstanceId(long v){nextPayloadInstanceId=v;return this;}public Builder nextEventId(long v){nextEventId=v;return this;}public ClassRuntimeState build(){return new ClassRuntimeState(this);}
	}
}
