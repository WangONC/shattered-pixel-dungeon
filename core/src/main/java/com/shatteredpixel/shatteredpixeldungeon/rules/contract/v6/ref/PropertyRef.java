package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class PropertyRef extends TypedRef {
	public PropertyRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.PROPERTY; }
	@Override public PropertyRef withTarget(StableId id, String name) { return new PropertyRef(id, name); }
}
