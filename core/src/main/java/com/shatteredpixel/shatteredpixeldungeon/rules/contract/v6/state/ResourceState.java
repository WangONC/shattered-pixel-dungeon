package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResourceState {
	public static final class Reservation {
		private final long reservationId;private final int amount;private final int remainingTurns;
		public Reservation(long reservationId,int amount,int remainingTurns){if(reservationId<0||amount<1||remainingTurns<0)throw new IllegalArgumentException("invalid reservation");this.reservationId=reservationId;this.amount=amount;this.remainingTurns=remainingTurns;}
		public long reservationId(){return reservationId;}public int amount(){return amount;}public int remainingTurns(){return remainingTurns;}
	}
	public static final class Suppression {
		private final long suppressionId;private final int remainingTurns;private final String modeKey;
		public Suppression(long suppressionId,int remainingTurns,String modeKey){if(suppressionId<0||remainingTurns<0||modeKey==null||modeKey.isEmpty())throw new IllegalArgumentException("invalid suppression");this.suppressionId=suppressionId;this.remainingTurns=remainingTurns;this.modeKey=modeKey;}
		public long suppressionId(){return suppressionId;}public int remainingTurns(){return remainingTurns;}public String modeKey(){return modeKey;}
	}
	private final ResourceRef resource;private final int current;private final List<Reservation> reservations;private final List<Suppression> suppressions;
	public ResourceState(ResourceRef resource,int current,List<Reservation> reservations,List<Suppression> suppressions){
		if(resource==null||reservations==null||suppressions==null)throw new IllegalArgumentException("resource state fields are required");
		this.resource=resource;this.current=current;this.reservations=Collections.unmodifiableList(new ArrayList<>(reservations));this.suppressions=Collections.unmodifiableList(new ArrayList<>(suppressions));}
	public ResourceRef resource(){return resource;}public int current(){return current;}public List<Reservation> reservations(){return reservations;}public List<Suppression> suppressions(){return suppressions;}
}
