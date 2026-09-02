package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.CapacityRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** P01 declaration boundary only. Body, payload, behavior, and runtime arrive in later phases. */
public final class EntitySpec implements DeclarationSpec {
	private final StableId id; private final DisplayName displayName; private final EntityType type;
	private final EntityBodySpec body; private final SpawnPolicySpec spawnPolicy;
	private final OwnershipSpec ownership; private final RelationSpec relation;
	private final CapacityRef capacity; private final PersistenceSpec persistence;
	private final List<EntityCapabilitySpec> capabilities;
	private final ImplementationState implementationState;
	public EntitySpec(StableId id, DisplayName displayName, EntityType type, CapacityRef capacity,
			ImplementationState implementationState) {
		this(id, displayName, type,
				deferred(EntityFacetKind.BODY, "P01_BODY_BOUNDARY"),
				deferred(EntityFacetKind.SPAWN_POLICY, "P01_SPAWN_POLICY_BOUNDARY"),
				deferred(EntityFacetKind.OWNERSHIP, "P01_OWNERSHIP_BOUNDARY"),
				deferred(EntityFacetKind.RELATION, "P01_RELATION_BOUNDARY"),
				capacity, deferred(EntityFacetKind.PERSISTENCE, "P01_PERSISTENCE_BOUNDARY"),
				Collections.emptyList(), implementationState);
	}
	public EntitySpec(StableId id, DisplayName displayName, EntityType type,
			EntityBodySpec body, SpawnPolicySpec spawnPolicy, OwnershipSpec ownership,
			RelationSpec relation, CapacityRef capacity, PersistenceSpec persistence,
			List<EntityCapabilitySpec> capabilities, ImplementationState implementationState) {
		if(id==null||displayName==null||type==null||body==null||spawnPolicy==null||ownership==null
				||relation==null||persistence==null||capabilities==null||implementationState==null) {
			throw new IllegalArgumentException("entity fields are required");
		}
		requireFacet(body, EntityFacetKind.BODY);requireFacet(spawnPolicy, EntityFacetKind.SPAWN_POLICY);
		requireFacet(ownership, EntityFacetKind.OWNERSHIP);requireFacet(relation, EntityFacetKind.RELATION);
		requireFacet(persistence, EntityFacetKind.PERSISTENCE);
		this.id=id;this.displayName=displayName;this.type=type;this.body=body;this.spawnPolicy=spawnPolicy;
		this.ownership=ownership;this.relation=relation;this.capacity=capacity;this.persistence=persistence;
		this.capabilities=Collections.unmodifiableList(new ArrayList<>(capabilities));this.implementationState=implementationState;
	}
	private static DeferredEntityFacetSpec deferred(EntityFacetKind kind,String key){return new DeferredEntityFacetSpec(kind,key,ImplementationState.DEFERRED);}
	private static void requireFacet(EntityFacetSpec value,EntityFacetKind expected){if(value.facetKind()!=expected)throw new IllegalArgumentException("expected entity facet "+expected);}
	@Override public StableId id(){return id;} @Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return RefKind.ENTITY;} @Override public ImplementationState implementationState(){return implementationState;}
	public EntityType type(){return type;} public EntityBodySpec body(){return body;} public SpawnPolicySpec spawnPolicy(){return spawnPolicy;}
	public OwnershipSpec ownership(){return ownership;} public RelationSpec relation(){return relation;} public CapacityRef capacity(){return capacity;}
	public PersistenceSpec persistence(){return persistence;} public List<EntityCapabilitySpec> capabilities(){return capabilities;}
	public EntitySpec withIdentity(StableId value){return copy(value,displayName,capacity,capabilities);}
	public EntitySpec withDisplayName(DisplayName value){return copy(id,value,capacity,capabilities);}
	public EntitySpec withCapacity(CapacityRef value){return copy(id,displayName,value,capabilities);}
	public EntitySpec withCapabilities(List<EntityCapabilitySpec> value){return copy(id,displayName,capacity,value);}
	private EntitySpec copy(StableId newId,DisplayName newName,CapacityRef newCapacity,List<EntityCapabilitySpec> newCapabilities){
		return new EntitySpec(newId,newName,type,body,spawnPolicy,ownership,relation,newCapacity,persistence,newCapabilities,implementationState);
	}
}
