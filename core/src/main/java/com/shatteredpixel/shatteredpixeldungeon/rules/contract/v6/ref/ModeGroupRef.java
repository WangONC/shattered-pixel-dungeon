package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class ModeGroupRef extends TypedRef {
	public ModeGroupRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.MODE_GROUP; }
	@Override public ModeGroupRef withTarget(StableId id, String name) { return new ModeGroupRef(id, name); }
}
