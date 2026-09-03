package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;
import java.util.List;

/** One typed resource-operation algebra shared by skills, components and class operations. */
public interface ResourceOperationSpec {
	enum Variant { GAIN, DRAIN, SET, CLEAR, CONVERT, RESERVE, SUPPRESS }
	StableId operationId();
	Variant variant();
	List<ResourceRef> referencedResources();
}
