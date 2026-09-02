package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.PropertyRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** P01 recipe identity and input boundary; synthesis output execution remains unsupported. */
public final class SynthesisRecipeSpec implements DeclarationSpec {
	public static final class PropertyInput {
		private final PropertyRef property;private final int amount;
		public PropertyInput(PropertyRef property,int amount){if(property==null||amount<1)throw new IllegalArgumentException("invalid property input");this.property=property;this.amount=amount;}
		public PropertyRef property(){return property;}public int amount(){return amount;}
	}
	private final StableId id;private final DisplayName displayName;private final List<PropertyInput> inputs;
	private final String outputVariantKey;private final ImplementationState implementationState;
	public SynthesisRecipeSpec(StableId id,DisplayName displayName,List<PropertyInput> inputs,String outputVariantKey,ImplementationState implementationState){
		if(id==null||displayName==null||inputs==null||outputVariantKey==null||outputVariantKey.isEmpty()||implementationState==null)throw new IllegalArgumentException("invalid recipe");
		this.id=id;this.displayName=displayName;this.inputs=Collections.unmodifiableList(new ArrayList<>(inputs));this.outputVariantKey=outputVariantKey;this.implementationState=implementationState;}
	@Override public StableId id(){return id;}@Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return RefKind.RECIPE;}@Override public ImplementationState implementationState(){return implementationState;}
	public List<PropertyInput> inputs(){return inputs;}public String outputVariantKey(){return outputVariantKey;}
	public SynthesisRecipeSpec withIdentity(StableId value){return new SynthesisRecipeSpec(value,displayName,inputs,outputVariantKey,implementationState);}
	public SynthesisRecipeSpec withDisplayName(DisplayName value){return new SynthesisRecipeSpec(id,value,inputs,outputVariantKey,implementationState);}
}
