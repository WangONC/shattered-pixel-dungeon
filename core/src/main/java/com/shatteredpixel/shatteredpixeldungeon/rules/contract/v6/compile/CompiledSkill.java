package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.SkillSpec;

/** Immutable compiled node; construction is restricted to a validation-successful compiler. */
public final class CompiledSkill {
	private final SkillSpec source;
	CompiledSkill(SkillSpec source) {
		if (source == null) throw new IllegalArgumentException("compiled source is required");
		this.source = source;
	}
	public SkillSpec source() { return source; }
}
