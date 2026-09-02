package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrace;

import java.util.ArrayList;

public class QaTraceCollector implements RuleTrace.Sink {
	public final ArrayList<String> lines = new ArrayList<>();
	private int turn;

	public void turn(int value) { turn = value; }

	public void action(String message) { record("ACTION", message); }

	@Override
	public void record(String category, String message) {
		if (lines.size() < 20000) lines.add("T=" + turn + " " + category + " " + message);
	}

	public String text() {
		StringBuilder out = new StringBuilder();
		for (String line : lines) out.append(line).append('\n');
		return out.toString();
	}
}
