package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class CompileDiagnostic {
	private final StableId skillId;
	private final String fieldPath;
	private final DependencyState state;
	private final String messageKey;
	public CompileDiagnostic(StableId skillId, String fieldPath, DependencyState state, String messageKey) {
		if (skillId == null || fieldPath == null || state == null || messageKey == null || messageKey.isEmpty()) throw new IllegalArgumentException("compile diagnostic fields required");
		this.skillId = skillId; this.fieldPath = fieldPath; this.state = state; this.messageKey = messageKey;
	}
	public StableId skillId() { return skillId; }
	public String fieldPath() { return fieldPath; }
	public DependencyState state() { return state; }
	public String messageKey() { return messageKey; }
}
