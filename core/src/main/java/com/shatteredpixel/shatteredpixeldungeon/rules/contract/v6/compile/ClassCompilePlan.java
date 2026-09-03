package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClassCompilePlan {
	private final List<CompiledSkill> skills;
	private final List<CompileDiagnostic> diagnostics;
	ClassCompilePlan(List<CompiledSkill> skills, List<CompileDiagnostic> diagnostics) {
		this.skills = Collections.unmodifiableList(new ArrayList<>(skills));
		this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
	}
	public List<CompiledSkill> skills() { return skills; }
	public List<CompileDiagnostic> diagnostics() { return diagnostics; }
	public boolean ready() { return diagnostics.isEmpty(); }
	public CompiledSkill find(StableId skillId) {
		for (CompiledSkill value : skills) if (value.source().id().equals(skillId)) return value;
		return null;
	}
}
