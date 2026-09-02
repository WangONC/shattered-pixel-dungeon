package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

public final class PropertySpec implements DeclarationSpec {
	public enum PropertyValueKind { MATERIAL, ELEMENT, BEHAVIOR_TRAIT, EFFECT_TRAIT }
	private final StableId id;private final DisplayName displayName;private final PropertyValueKind valueKind;private final int maximumStack;
	public PropertySpec(StableId id,DisplayName displayName,PropertyValueKind valueKind,int maximumStack){
		if(id==null||displayName==null||valueKind==null||maximumStack<1)throw new IllegalArgumentException("invalid property");
		this.id=id;this.displayName=displayName;this.valueKind=valueKind;this.maximumStack=maximumStack;}
	@Override public StableId id(){return id;}@Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return RefKind.PROPERTY;}@Override public ImplementationState implementationState(){return ImplementationState.DECLARED;}
	public PropertyValueKind valueKind(){return valueKind;}public int maximumStack(){return maximumStack;}
	public PropertySpec withIdentity(StableId value){return new PropertySpec(value,displayName,valueKind,maximumStack);}
	public PropertySpec withDisplayName(DisplayName value){return new PropertySpec(id,value,valueKind,maximumStack);}
}
