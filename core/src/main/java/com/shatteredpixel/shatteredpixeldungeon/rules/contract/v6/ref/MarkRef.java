package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class MarkRef extends TypedRef {
	public MarkRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.MARK; }
	@Override public MarkRef withTarget(StableId id, String name) { return new MarkRef(id, name); }
}
