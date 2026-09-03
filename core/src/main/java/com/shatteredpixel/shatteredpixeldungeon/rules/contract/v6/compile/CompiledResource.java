package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Immutable compiled declaration used for initialization, validation, transactions and HUD. */
public final class CompiledResource {
	public enum OverflowPolicy { FAIL, CLAMP, DISCARD_EXCESS }
	private final StableId id; private final String displayName;
	private final int minimum, maximum, initialValue; private final OverflowPolicy overflow;
	private final boolean hudVisible; private final int hudOrder; private final String presentationKey;
	public CompiledResource(StableId id,String name,int min,int max,int initial,OverflowPolicy overflow,boolean visible,int order,String key){
		if(id==null||name==null||overflow==null||key==null||max<=min||initial<min||initial>max||order<0)throw new IllegalArgumentException("invalid compiled resource");
		this.id=id;displayName=name;minimum=min;maximum=max;initialValue=initial;this.overflow=overflow;hudVisible=visible;hudOrder=order;presentationKey=key;
	}
	public StableId id(){return id;} public String displayName(){return displayName;} public int minimum(){return minimum;}
	public int maximum(){return maximum;} public int initialValue(){return initialValue;} public OverflowPolicy overflow(){return overflow;}
	public boolean hudVisible(){return hudVisible;} public int hudOrder(){return hudOrder;} public String presentationKey(){return presentationKey;}
}
