package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class ScaledValueSpec implements ValueSpec {
	public enum RoundingMode { FLOOR }
	private final int base;private final ValueSourceSpec source;private final int numerator,denominator,minimum,maximum;private final RoundingMode rounding;
	public ScaledValueSpec(int base,ValueSourceSpec source,int numerator,int denominator,int minimum,int maximum,RoundingMode rounding){if(source==null||rounding==null)throw new IllegalArgumentException("scaled source/rounding required");if(numerator<0||denominator<1||maximum<minimum)throw new IllegalArgumentException("invalid scaled value bounds");this.base=base;this.source=source;this.numerator=numerator;this.denominator=denominator;this.minimum=minimum;this.maximum=maximum;this.rounding=rounding;}
	public int base(){return base;}public ValueSourceSpec source(){return source;}public int numerator(){return numerator;}public int denominator(){return denominator;}public int minimum(){return minimum;}public int maximum(){return maximum;}public RoundingMode rounding(){return rounding;}@Override public String variantKey(){return "SCALED";}
}
