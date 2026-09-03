package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Explicit, deterministic trace records; no success is inferred from absence of exceptions. */
public final class RuntimeTrace {
	private final List<String> entries = new ArrayList<>();
	public void record(String stage, String detail) {
		if (stage == null || stage.isEmpty() || detail == null) throw new IllegalArgumentException("trace stage/detail required");
		entries.add(stage + "|" + detail.replace("\n", " ").replace("\r", " "));
	}
	public List<String> entries() { return Collections.unmodifiableList(new ArrayList<>(entries)); }
	public String serialize() {
		StringBuilder result = new StringBuilder("SPD_GC_V6_RUNTIME_TRACE_1\n");
		for (String entry : entries) result.append(entry).append('\n');
		return result.toString();
	}
}
