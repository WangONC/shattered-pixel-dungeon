package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

/** Explicit placeholder for preserved sections whose gameplay schema belongs to later phases. */
public final class DeferredSectionSpec {
	private final String sectionKey;
	private final ImplementationState state;
	public DeferredSectionSpec(String sectionKey, ImplementationState state) {
		if(sectionKey==null||sectionKey.isEmpty()||state==null)throw new IllegalArgumentException("section fields are required");
		if(state!=ImplementationState.DEFERRED&&state!=ImplementationState.UNSUPPORTED)throw new IllegalArgumentException("section must remain deferred or unsupported");
		this.sectionKey=sectionKey;this.state=state;
	}
	public String sectionKey(){return sectionKey;} public ImplementationState state(){return state;}
}
