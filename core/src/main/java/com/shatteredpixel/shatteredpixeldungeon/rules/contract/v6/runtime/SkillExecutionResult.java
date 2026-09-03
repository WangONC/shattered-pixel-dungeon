package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

public final class SkillExecutionResult {
	public enum Status { APPLIED,NO_TARGET,BLOCKED,UNSUPPORTED,MISSING_EXECUTOR,TYPE_MISMATCH,TARGET_UNAVAILABLE,IMMUNE,UNSUPPORTED_PARAMETERS }
	private final Status status;private final int appliedAmount;private final RuntimeTrace trace;private final String diagnostic;
	SkillExecutionResult(Status status,int appliedAmount,RuntimeTrace trace,String diagnostic){this.status=status;this.appliedAmount=appliedAmount;this.trace=trace;this.diagnostic=diagnostic;}
	public Status status(){return status;}public int appliedAmount(){return appliedAmount;}public RuntimeTrace trace(){return trace;}public String diagnostic(){return diagnostic;}
}
