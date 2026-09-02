package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

public final class ModeSpec implements DeclarationSpec {
	public enum ModeDurationPolicy { PERSISTENT, TURN_BASED }
	private final StableId id; private final DisplayName displayName; private final ModeGroupRef group;
	private final boolean initial; private final ModeDurationPolicy durationPolicy; private final int defaultDurationTurns;
	public ModeSpec(StableId id, DisplayName displayName, ModeGroupRef group, boolean initial,
			ModeDurationPolicy durationPolicy, int defaultDurationTurns) {
		if(id==null||displayName==null||group==null||durationPolicy==null)throw new IllegalArgumentException("mode fields are required");
		if(durationPolicy==ModeDurationPolicy.TURN_BASED&&defaultDurationTurns<1
				||durationPolicy==ModeDurationPolicy.PERSISTENT&&defaultDurationTurns!=0)throw new IllegalArgumentException("invalid mode duration");
		this.id=id;this.displayName=displayName;this.group=group;this.initial=initial;
		this.durationPolicy=durationPolicy;this.defaultDurationTurns=defaultDurationTurns;
	}
	@Override public StableId id(){return id;} @Override public DisplayName displayName(){return displayName;}
	@Override public RefKind refKind(){return RefKind.MODE;} @Override public ImplementationState implementationState(){return ImplementationState.DECLARED;}
	public ModeGroupRef group(){return group;} public boolean initial(){return initial;}
	public ModeDurationPolicy durationPolicy(){return durationPolicy;} public int defaultDurationTurns(){return defaultDurationTurns;}
	public ModeSpec withIdentity(StableId value){return new ModeSpec(value,displayName,group,initial,durationPolicy,defaultDurationTurns);}
	public ModeSpec withDisplayName(DisplayName value){return new ModeSpec(id,value,group,initial,durationPolicy,defaultDurationTurns);}
	public ModeSpec withGroup(ModeGroupRef value){return new ModeSpec(id,displayName,value,initial,durationPolicy,defaultDurationTurns);}
}
