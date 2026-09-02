package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

public final class MarkSpec implements DeclarationSpec {
	public enum MarkKind { MARK, STACK, CHARGE, COUNTER, FLAG }
	public enum MarkDurationPolicy { PERMANENT_UNTIL_REMOVED, TURN_BASED, END_OF_LEVEL }
	public enum MarkRefreshPolicy { REPLACE_DURATION, KEEP_LONGER, ADD_DURATION }
	public enum MarkOverflowPolicy { CLAMP, REJECT }
	public enum MarkProvenancePolicy { NONE, TRACK_LAST_SOURCE }

	private final StableId id;
	private final DisplayName displayName;
	private final MarkKind kind;
	private final int minimum, maximum, initialValue, defaultDurationTurns;
	private final MarkDurationPolicy durationPolicy;
	private final MarkRefreshPolicy refreshPolicy;
	private final MarkOverflowPolicy overflowPolicy;
	private final MarkProvenancePolicy provenancePolicy;

	public MarkSpec(StableId id, DisplayName displayName, MarkKind kind, int minimum, int maximum,
			int initialValue, MarkDurationPolicy durationPolicy, int defaultDurationTurns,
			MarkRefreshPolicy refreshPolicy, MarkOverflowPolicy overflowPolicy,
			MarkProvenancePolicy provenancePolicy) {
		if (id == null || displayName == null || kind == null || durationPolicy == null || refreshPolicy == null
				|| overflowPolicy == null || provenancePolicy == null) throw new IllegalArgumentException("mark fields are required");
		if (maximum < 1 || maximum < minimum || initialValue < minimum || initialValue > maximum
				|| durationPolicy == MarkDurationPolicy.TURN_BASED && defaultDurationTurns < 1
				|| durationPolicy != MarkDurationPolicy.TURN_BASED && defaultDurationTurns != 0) {
			throw new IllegalArgumentException("invalid mark bounds or duration");
		}
		this.id=id; this.displayName=displayName; this.kind=kind; this.minimum=minimum; this.maximum=maximum;
		this.initialValue=initialValue; this.durationPolicy=durationPolicy; this.defaultDurationTurns=defaultDurationTurns;
		this.refreshPolicy=refreshPolicy; this.overflowPolicy=overflowPolicy; this.provenancePolicy=provenancePolicy;
	}

	@Override public StableId id(){return id;} @Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return RefKind.MARK;} @Override public ImplementationState implementationState(){return ImplementationState.DECLARED;}
	public MarkKind kind(){return kind;} public int minimum(){return minimum;} public int maximum(){return maximum;}
	public int initialValue(){return initialValue;} public MarkDurationPolicy durationPolicy(){return durationPolicy;}
	public int defaultDurationTurns(){return defaultDurationTurns;} public MarkRefreshPolicy refreshPolicy(){return refreshPolicy;}
	public MarkOverflowPolicy overflowPolicy(){return overflowPolicy;} public MarkProvenancePolicy provenancePolicy(){return provenancePolicy;}
	public MarkSpec withIdentity(StableId value){return new MarkSpec(value,displayName,kind,minimum,maximum,initialValue,durationPolicy,defaultDurationTurns,refreshPolicy,overflowPolicy,provenancePolicy);}
	public MarkSpec withDisplayName(DisplayName value){return new MarkSpec(id,value,kind,minimum,maximum,initialValue,durationPolicy,defaultDurationTurns,refreshPolicy,overflowPolicy,provenancePolicy);}
}
