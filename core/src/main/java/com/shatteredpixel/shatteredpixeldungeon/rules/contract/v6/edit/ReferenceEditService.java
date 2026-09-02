package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.edit;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.TypedRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;
public final class ReferenceEditService {
	public TypedRef rebind(TypedRef oldReference,StableTarget newTarget){if(oldReference==null||newTarget==null)throw new IllegalArgumentException("reference and target are required");if(newTarget.refKind()!=oldReference.kind())throw new IllegalArgumentException("target type does not match reference type");return oldReference.withTarget(newTarget.id(),newTarget.displayName().text());}
}
