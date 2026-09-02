package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Structured validation result shared by the Builder, overview and equivalence QA. */
public final class PlayerFacingValidationIssue {
	public enum Code {
		MISSING_REQUIRED_SELECTION,
		INCOMPATIBLE_DELIVERY,
		INCOMPATIBLE_TARGET,
		INCOMPATIBLE_EFFECT,
		MISSING_EFFECT_PARAMETER,
		MISSING_RESOURCE,
		INVALID_COST,
		INVALID_CONSTRAINT,
		SECONDARY_INCOMPATIBLE,
		UNSUPPORTED_RUNTIME_CAPABILITY,
		SKILL_BUDGET_OVERFLOW,
		CLASS_BUDGET_OVERFLOW,
		BUILD_INTEGRITY_BROKEN,
		LAW_TRAIT_INCOMPATIBLE,
		UNRESOLVED_DEPENDENCY
	}
	public enum Severity { ERROR, WARNING }
	public enum Field { NAME, EFFECT, PARAMETERS, DELIVERY, TARGET, MODIFIER, COST, CONSTRAINT,
		SECONDARY, COMPONENT, OPERATION, LAW, TRAIT, BUDGET, INTEGRITY }

	public final Code code;
	public final Severity severity;
	public final Field affectedField;
	public final String shortMessage;
	public final String detailMessage;

	public PlayerFacingValidationIssue(Code code, Severity severity, Field field, Object... args) {
		this.code = code;
		this.severity = severity;
		this.affectedField = field;
		String key = code.name().toLowerCase();
		this.shortMessage = Messages.get(PlayerFacingValidationIssue.class, key + "_short", args);
		this.detailMessage = Messages.get(PlayerFacingValidationIssue.class, key + "_detail", args);
	}
}
