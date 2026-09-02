package com.shatteredpixel.shatteredpixeldungeon.rules;

/** Optional gameplay-causality trace hook. The normal game pays only one null check per event. */
public final class RuleTrace {
	public interface Sink {
		void record(String category, String message);
	}

	private static final ThreadLocal<Sink> SINK = new ThreadLocal<>();

	private RuleTrace() {}

	public static void install(Sink sink) {
		if (sink == null) SINK.remove();
		else SINK.set(sink);
	}

	public static void clear() {
		SINK.remove();
	}

	public static void record(String category, String message) {
		Sink sink = SINK.get();
		if (sink != null) sink.record(category, message);
	}
}
