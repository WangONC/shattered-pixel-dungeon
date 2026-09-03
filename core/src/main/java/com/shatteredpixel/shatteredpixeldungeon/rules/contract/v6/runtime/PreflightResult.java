package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PreflightResult {
	public enum Status { READY, NO_TARGET, BLOCKED, UNSUPPORTED }
	private final Status status;
	private final List<Char> targets;
	private final String diagnostic;
	private PreflightResult(Status status, List<Char> targets, String diagnostic) {
		this.status = status; this.targets = Collections.unmodifiableList(new ArrayList<>(targets)); this.diagnostic = diagnostic;
	}
	public static PreflightResult ready(Char target) { return ready(Collections.singletonList(target)); }
	public static PreflightResult ready(List<Char> targets) { if(targets==null||targets.isEmpty()||targets.contains(null))throw new IllegalArgumentException("ready targets required");return new PreflightResult(Status.READY,targets,"ready"); }
	public static PreflightResult failed(Status status, String diagnostic) {
		if (status == Status.READY) throw new IllegalArgumentException("failed preflight cannot be READY");
		return new PreflightResult(status, Collections.<Char>emptyList(), diagnostic);
	}
	public Status status() { return status; }
	public Char target() { return targets.isEmpty()?null:targets.get(0); }
	public List<Char> targets() { return targets; }
	public String diagnostic() { return diagnostic; }
}
