package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class CapacityRef extends TypedRef {
	public CapacityRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.CAPACITY; }
	@Override public CapacityRef withTarget(StableId id, String name) { return new CapacityRef(id, name); }
}
