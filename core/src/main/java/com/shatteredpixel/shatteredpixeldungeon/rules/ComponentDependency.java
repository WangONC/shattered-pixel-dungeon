package com.shatteredpixel.shatteredpixeldungeon.rules;

/** One authoritative dependency result shared by Builder, final validation and QA. */
public final class ComponentDependency {
	public enum State { RESOLVED, UNRESOLVED, HARD_CONFLICT, UNSUPPORTED }
	public final State state;
	public final String code;

	private ComponentDependency(State state, String code) {
		this.state = state;
		this.code = code;
	}

	public static ComponentDependency resolved() { return new ComponentDependency(State.RESOLVED, null); }
	public static ComponentDependency unresolved(String code) { return new ComponentDependency(State.UNRESOLVED, code); }
	public static ComponentDependency hard(String code) { return new ComponentDependency(State.HARD_CONFLICT, code); }
	public static ComponentDependency unsupported() { return new ComponentDependency(State.UNSUPPORTED, "unsupported"); }
	public boolean resolvedState() { return state == State.RESOLVED; }
	public boolean playerSelectable() { return state == State.RESOLVED || state == State.UNRESOLVED; }
}
