package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

/** Base mechanics only; every public reference remains a distinct final type. */
public abstract class TypedRef {
	private final StableId targetId;
	private final String lastKnownDisplayName;

	protected TypedRef(StableId targetId, String lastKnownDisplayName) {
		if (targetId == null) throw new IllegalArgumentException("target id is required");
		this.targetId = targetId;
		this.lastKnownDisplayName = lastKnownDisplayName == null ? "" : lastKnownDisplayName;
	}

	public final StableId targetId() { return targetId; }
	public final String lastKnownDisplayName() { return lastKnownDisplayName; }
	public abstract RefKind kind();
	public abstract TypedRef withTarget(StableId targetId, String displayName);

	@Override public final boolean equals(Object other) {
		return other != null && getClass() == other.getClass()
				&& targetId.equals(((TypedRef)other).targetId);
	}
	@Override public final int hashCode() { return 31 * getClass().hashCode() + targetId.hashCode(); }
}
