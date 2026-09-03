package com.shatteredpixel.shatteredpixeldungeon.rules.migration.v5;

import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;

/** Explicit one-way migration disposition until a legacy shape has an exact typed mapping. */
public final class P03SkillMigrationPlaceholder {
	public enum Disposition { UNSUPPORTED_TYPED_MAPPING_REQUIRED }
	public Disposition classify(SkillSpec legacy) {
		if (legacy == null) throw new IllegalArgumentException("legacy skill is required");
		return Disposition.UNSUPPORTED_TYPED_MAPPING_REQUIRED;
	}
	public String diagnosticKey() { return "migration.v5.skill.typed_mapping_required"; }
}
