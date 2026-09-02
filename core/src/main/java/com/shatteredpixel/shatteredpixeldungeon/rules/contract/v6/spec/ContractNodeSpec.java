package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

/** Identity boundary for P02/P03 nodes; it deliberately contains no gameplay payload. */
public final class ContractNodeSpec implements StableTarget {
	public enum NodeKind { COMPONENT, CONSTRAINT, OPERATION, SKILL }
	private final StableId id; private final DisplayName displayName; private final NodeKind nodeKind;
	private final String variantKey; private final ImplementationState implementationState;
	public ContractNodeSpec(StableId id, DisplayName displayName, NodeKind nodeKind, String variantKey,
			ImplementationState implementationState) {
		if(id==null||displayName==null||nodeKind==null||variantKey==null||variantKey.isEmpty()||implementationState==null) {
			throw new IllegalArgumentException("node identity fields are required");
		}
		this.id=id;this.displayName=displayName;this.nodeKind=nodeKind;this.variantKey=variantKey;
		this.implementationState=implementationState;
	}
	@Override public StableId id(){return id;} @Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return nodeKind==NodeKind.COMPONENT?RefKind.COMPONENT:null;}
	@Override public ImplementationState implementationState(){return implementationState;}
	public NodeKind nodeKind(){return nodeKind;} public String variantKey(){return variantKey;}
	public ContractNodeSpec withIdentity(StableId value){return new ContractNodeSpec(value,displayName,nodeKind,variantKey,implementationState);}
	public ContractNodeSpec withDisplayName(DisplayName value){return new ContractNodeSpec(id,value,nodeKind,variantKey,implementationState);}
}
