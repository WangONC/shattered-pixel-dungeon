package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;

public final class ResourceValueSource implements ValueSourceSpec {
	private final ResourceHolderSelector holder;private final ResourceRef resource;
	public ResourceValueSource(ResourceHolderSelector holder,ResourceRef resource){if(holder==null||resource==null)throw new IllegalArgumentException("resource source fields required");this.holder=holder;this.resource=resource;}
	public ResourceHolderSelector holder(){return holder;}public ResourceRef resource(){return resource;}@Override public String variantKey(){return "RESOURCE_VALUE";}
}
