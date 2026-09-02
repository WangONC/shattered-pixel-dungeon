package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class EntityCapacitySpec implements DeclarationSpec {
	public enum CapacityOverflowPolicy { REJECT_NEW, REMOVE_OLDEST }
	private final StableId id; private final DisplayName displayName; private final Set<EntityType> entityTypes;
	private final int maximum; private final CapacityOverflowPolicy overflowPolicy;
	public EntityCapacitySpec(StableId id, DisplayName displayName, Set<EntityType> entityTypes,
			int maximum, CapacityOverflowPolicy overflowPolicy) {
		if(id==null||displayName==null||entityTypes==null||entityTypes.isEmpty()||overflowPolicy==null)throw new IllegalArgumentException("capacity fields are required");
		if(maximum<1)throw new IllegalArgumentException("capacity maximum must be positive");
		this.id=id;this.displayName=displayName;this.entityTypes=Collections.unmodifiableSet(EnumSet.copyOf(entityTypes));
		this.maximum=maximum;this.overflowPolicy=overflowPolicy;
	}
	@Override public StableId id(){return id;} @Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return RefKind.CAPACITY;} @Override public ImplementationState implementationState(){return ImplementationState.DECLARED;}
	public Set<EntityType> entityTypes(){return entityTypes;} public int maximum(){return maximum;}
	public CapacityOverflowPolicy overflowPolicy(){return overflowPolicy;}
	public EntityCapacitySpec withIdentity(StableId value){return new EntityCapacitySpec(value,displayName,entityTypes,maximum,overflowPolicy);}
	public EntityCapacitySpec withDisplayName(DisplayName value){return new EntityCapacitySpec(id,value,entityTypes,maximum,overflowPolicy);}
}
