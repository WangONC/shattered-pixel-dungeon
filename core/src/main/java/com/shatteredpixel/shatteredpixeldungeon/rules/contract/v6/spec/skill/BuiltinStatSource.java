package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

public final class BuiltinStatSource implements ValueSourceSpec {
	private final SubjectSelector subject;private final BuiltinStatRef stat;
	public BuiltinStatSource(SubjectSelector subject,BuiltinStatRef stat){if(subject==null||stat==null)throw new IllegalArgumentException("builtin source fields required");this.subject=subject;this.stat=stat;}
	public SubjectSelector subject(){return subject;}public BuiltinStatRef stat(){return stat;}@Override public String variantKey(){return "BUILTIN_STAT";}
}
