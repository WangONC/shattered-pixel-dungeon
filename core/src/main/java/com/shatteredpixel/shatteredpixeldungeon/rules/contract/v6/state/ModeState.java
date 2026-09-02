package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeRef;
public final class ModeState {
	private final ModeRef mode;private final long subjectActorId;private final int remainingTurns;
	public ModeState(ModeRef mode,long subjectActorId,int remainingTurns){if(mode==null||subjectActorId<0||remainingTurns< -1)throw new IllegalArgumentException("invalid mode state");this.mode=mode;this.subjectActorId=subjectActorId;this.remainingTurns=remainingTurns;}
	public ModeRef mode(){return mode;}public long subjectActorId(){return subjectActorId;}public int remainingTurns(){return remainingTurns;}
}
