package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class AbilityPoolRef extends TypedRef {
	public AbilityPoolRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.ABILITY_POOL; }
	@Override public AbilityPoolRef withTarget(StableId id, String name) { return new AbilityPoolRef(id, name); }
}
