package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.PropertyRef;
import java.util.ArrayList;import java.util.Collections;import java.util.List;
public final class PropertyInventoryState {
	private final PropertyRef property;private final int amount;private final List<String> provenance;
	public PropertyInventoryState(PropertyRef property,int amount,List<String> provenance){if(property==null||amount<0||provenance==null)throw new IllegalArgumentException("invalid property inventory");this.property=property;this.amount=amount;this.provenance=Collections.unmodifiableList(new ArrayList<>(provenance));}
	public PropertyRef property(){return property;}public int amount(){return amount;}public List<String> provenance(){return provenance;}
}
