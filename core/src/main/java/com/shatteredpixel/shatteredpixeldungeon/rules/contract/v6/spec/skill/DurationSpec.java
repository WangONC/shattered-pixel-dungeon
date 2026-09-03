package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class DurationSpec {
	public enum DurationKind { INSTANT, TURN_BASED, UNTIL_LEVEL_END, UNTIL_REMOVED }
	private final DurationKind kind; private final int turns;
	public DurationSpec(DurationKind kind,int turns){if(kind==null)throw new IllegalArgumentException("duration kind required");if(kind==DurationKind.TURN_BASED&&turns<1)throw new IllegalArgumentException("turn duration must be positive");if(kind!=DurationKind.TURN_BASED&&turns!=0)throw new IllegalArgumentException("turns only apply to TURN_BASED");this.kind=kind;this.turns=turns;}
	public static DurationSpec turns(int turns){return new DurationSpec(DurationKind.TURN_BASED,turns);}
	public DurationKind kind(){return kind;} public int turns(){return turns;}
}
