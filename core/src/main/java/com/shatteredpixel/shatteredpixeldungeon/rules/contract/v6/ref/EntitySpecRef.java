package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class EntitySpecRef extends TypedRef {
	public EntitySpecRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.ENTITY; }
	@Override public EntitySpecRef withTarget(StableId id, String name) { return new EntitySpecRef(id, name); }
}
