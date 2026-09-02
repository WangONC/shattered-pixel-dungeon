package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class ComponentRef extends TypedRef {
	public ComponentRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.COMPONENT; }
	@Override public ComponentRef withTarget(StableId id, String name) { return new ComponentRef(id, name); }
}
