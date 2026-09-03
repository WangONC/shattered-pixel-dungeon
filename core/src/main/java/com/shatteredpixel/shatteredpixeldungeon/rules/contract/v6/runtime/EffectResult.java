package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

public final class EffectResult {
	public enum Status { APPLIED, BLOCKED, UNSUPPORTED, MISSING_EXECUTOR }
	private final Status status;
	private final int appliedAmount;
	private final String diagnostic;
	private EffectResult(Status status, int appliedAmount, String diagnostic) {
		this.status = status; this.appliedAmount = appliedAmount; this.diagnostic = diagnostic;
	}
	public static EffectResult applied(int amount) { return new EffectResult(Status.APPLIED, amount, "applied"); }
	public static EffectResult failed(Status status, String diagnostic) {
		if (status == Status.APPLIED) throw new IllegalArgumentException("failure cannot be APPLIED");
		return new EffectResult(status, 0, diagnostic);
	}
	public Status status() { return status; }
	public int appliedAmount() { return appliedAmount; }
	public String diagnostic() { return diagnostic; }
}
