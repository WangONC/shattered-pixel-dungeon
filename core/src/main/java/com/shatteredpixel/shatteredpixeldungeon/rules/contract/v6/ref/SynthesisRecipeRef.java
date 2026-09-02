package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
public final class SynthesisRecipeRef extends TypedRef {
	public SynthesisRecipeRef(StableId id, String name) { super(id, name); }
	@Override public RefKind kind() { return RefKind.RECIPE; }
	@Override public SynthesisRecipeRef withTarget(StableId id, String name) { return new SynthesisRecipeRef(id, name); }
}
