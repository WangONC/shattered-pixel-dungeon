package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

public final class BuilderNavigationState {
	private final String route, targetId, fieldKey;
	public BuilderNavigationState(String route, String targetId, String fieldKey) {
		if (route == null || route.isEmpty()) throw new IllegalArgumentException("builder route is required");
		this.route = route; this.targetId = targetId == null ? "" : targetId; this.fieldKey = fieldKey == null ? "" : fieldKey;
	}
	public static BuilderNavigationState root() { return new BuilderNavigationState("root", "", ""); }
	public String route() { return route; }
	public String targetId() { return targetId; }
	public String fieldKey() { return fieldKey; }
}
