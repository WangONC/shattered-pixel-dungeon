package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

public final class EffectPreflightResult {
	public enum Status { READY,MISSING_EXECUTOR,TYPE_MISMATCH,TARGET_UNAVAILABLE,IMMUNE,UNSUPPORTED_PARAMETERS }
	private final Status status;private final String diagnostic;
	private EffectPreflightResult(Status status,String diagnostic){if(status==null||diagnostic==null)throw new IllegalArgumentException("effect preflight fields required");this.status=status;this.diagnostic=diagnostic;}
	public static EffectPreflightResult ready(){return new EffectPreflightResult(Status.READY,"ready");}
	public static EffectPreflightResult failed(Status status,String diagnostic){if(status==Status.READY)throw new IllegalArgumentException("failed preflight cannot be ready");return new EffectPreflightResult(status,diagnostic);}
	public Status status(){return status;}public String diagnostic(){return diagnostic;}public boolean readyForExecute(){return status==Status.READY;}
}
