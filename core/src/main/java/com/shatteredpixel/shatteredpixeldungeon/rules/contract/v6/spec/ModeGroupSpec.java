package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

public final class ModeGroupSpec implements DeclarationSpec {
	public enum ModeGroupPolicy { EXCLUSIVE, INDEPENDENT }
	private final StableId id; private final DisplayName displayName; private final ModeGroupPolicy policy;
	public ModeGroupSpec(StableId id, DisplayName displayName, ModeGroupPolicy policy) {
		if(id==null||displayName==null||policy==null)throw new IllegalArgumentException("mode group fields are required");
		this.id=id;this.displayName=displayName;this.policy=policy;
	}
	@Override public StableId id(){return id;} @Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return RefKind.MODE_GROUP;} @Override public ImplementationState implementationState(){return ImplementationState.DECLARED;}
	public ModeGroupPolicy policy(){return policy;}
	public ModeGroupSpec withIdentity(StableId value){return new ModeGroupSpec(value,displayName,policy);}
	public ModeGroupSpec withDisplayName(DisplayName value){return new ModeGroupSpec(id,value,policy);}
}
