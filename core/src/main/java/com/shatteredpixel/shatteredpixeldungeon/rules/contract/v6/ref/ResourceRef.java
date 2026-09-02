package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class ResourceRef extends TypedRef {
	public ResourceRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.RESOURCE; }
	@Override public ResourceRef withTarget(StableId id, String name) { return new ResourceRef(id, name); }
}
