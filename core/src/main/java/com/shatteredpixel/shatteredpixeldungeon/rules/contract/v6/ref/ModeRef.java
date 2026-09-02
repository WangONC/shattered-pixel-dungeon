package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class ModeRef extends TypedRef {
	public ModeRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.MODE; }
	@Override public ModeRef withTarget(StableId id, String name) { return new ModeRef(id, name); }
}
