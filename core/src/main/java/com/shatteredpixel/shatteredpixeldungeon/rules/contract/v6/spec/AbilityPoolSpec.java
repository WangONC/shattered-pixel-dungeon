package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

public final class AbilityPoolSpec implements DeclarationSpec {
	public enum AbilityOverflowPolicy { REJECT_NEW, REPLACE_OLDEST }
	private final StableId id;private final DisplayName displayName;private final int capacity;private final AbilityOverflowPolicy overflowPolicy;
	public AbilityPoolSpec(StableId id,DisplayName displayName,int capacity,AbilityOverflowPolicy overflowPolicy){
		if(id==null||displayName==null||overflowPolicy==null||capacity<1)throw new IllegalArgumentException("invalid ability pool");
		this.id=id;this.displayName=displayName;this.capacity=capacity;this.overflowPolicy=overflowPolicy;}
	@Override public StableId id(){return id;}@Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return RefKind.ABILITY_POOL;}@Override public ImplementationState implementationState(){return ImplementationState.DECLARED;}
	public int capacity(){return capacity;}public AbilityOverflowPolicy overflowPolicy(){return overflowPolicy;}
	public AbilityPoolSpec withIdentity(StableId value){return new AbilityPoolSpec(value,displayName,capacity,overflowPolicy);}
	public AbilityPoolSpec withDisplayName(DisplayName value){return new AbilityPoolSpec(id,value,capacity,overflowPolicy);}
}
