package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class StatusFilterSpec {
	public enum Kind { SPECIFIC, ANY_NEGATIVE }
	private final Kind kind;private final StatusRef status;
	private StatusFilterSpec(Kind kind,StatusRef status){this.kind=kind;this.status=status;}
	public static StatusFilterSpec specific(StatusRef status){if(status==null)throw new IllegalArgumentException("status required");return new StatusFilterSpec(Kind.SPECIFIC,status);}
	public static StatusFilterSpec anyNegative(){return new StatusFilterSpec(Kind.ANY_NEGATIVE,null);}
	public Kind kind(){return kind;}public StatusRef status(){return status;}
}
