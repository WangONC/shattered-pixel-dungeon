package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.MarkRef;
public final class MarkState {
	private final MarkRef mark;private final long subjectActorId;private final long sourceActorId;
	private final int value,remainingTurns,transferDepth;
	public MarkState(MarkRef mark,long subjectActorId,long sourceActorId,int value,int remainingTurns,int transferDepth){
		if(mark==null||subjectActorId<0||sourceActorId< -1||remainingTurns< -1||transferDepth<0)throw new IllegalArgumentException("invalid mark state");
		this.mark=mark;this.subjectActorId=subjectActorId;this.sourceActorId=sourceActorId;this.value=value;this.remainingTurns=remainingTurns;this.transferDepth=transferDepth;}
	public MarkRef mark(){return mark;}public long subjectActorId(){return subjectActorId;}public long sourceActorId(){return sourceActorId;}
	public int value(){return value;}public int remainingTurns(){return remainingTurns;}public int transferDepth(){return transferDepth;}
}
