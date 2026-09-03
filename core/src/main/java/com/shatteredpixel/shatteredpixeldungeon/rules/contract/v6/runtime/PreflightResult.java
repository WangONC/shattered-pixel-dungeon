package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;

public final class PreflightResult {
	public enum Status { READY, NO_TARGET, BLOCKED, UNSUPPORTED }
	private final Status status;
	private final Char target;
	private final String diagnostic;
	private PreflightResult(Status status, Char target, String diagnostic) {
		this.status = status; this.target = target; this.diagnostic = diagnostic;
	}
	public static PreflightResult ready(Char target) { return new PreflightResult(Status.READY, target, "ready"); }
	public static PreflightResult failed(Status status, String diagnostic) {
		if (status == Status.READY) throw new IllegalArgumentException("failed preflight cannot be READY");
		return new PreflightResult(status, null, diagnostic);
	}
	public Status status() { return status; }
	public Char target() { return target; }
	public String diagnostic() { return diagnostic; }
}
