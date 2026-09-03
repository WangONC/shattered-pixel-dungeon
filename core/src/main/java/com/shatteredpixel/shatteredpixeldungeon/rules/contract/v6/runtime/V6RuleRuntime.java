package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.ActiveTriggerSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.AllOfCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.NoCostSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.SkillSpec;

/** Runtime adapter from immutable compile plan to native SPD actors. */
public final class V6RuleRuntime {
	private final ClassCompilePlan plan;
	private final EffectExecutorRegistry executors;
	public V6RuleRuntime(ClassCompilePlan plan, EffectExecutorRegistry executors) {
		if (plan == null || executors == null) throw new IllegalArgumentException("compile plan and executors are required");
		this.plan = plan; this.executors = executors;
	}
	public SkillExecutionResult execute(StableId skillId, GameplayEventContext context) {
		RuntimeTrace trace = new RuntimeTrace();
		trace.record("event", "event_id=" + context.eventId() + " cause_event_id=" + context.causeEventId() + " type=" + context.eventType());
		CompiledSkill compiled = plan.find(skillId);
		if (compiled == null) return result(SkillExecutionResult.Status.UNSUPPORTED, 0, trace, "runtime.skill_not_compiled");
		SkillSpec skill = compiled.source();
		trace.record("skill", "skill=" + skill.id().value() + " variant=" + skill.variantKey());
		if (!(skill.activation() instanceof ActiveTriggerSpec) || context.eventType() != GameplayEventContext.RuleEventType.ACTIVE) {
			return result(SkillExecutionResult.Status.BLOCKED, 0, trace, "runtime.trigger_not_satisfied");
		}
		if (!(skill.condition() instanceof AllOfCondition) || !((AllOfCondition) skill.condition()).children().isEmpty()) {
			return result(SkillExecutionResult.Status.UNSUPPORTED, 0, trace, "runtime.condition_unsupported");
		}
		if (!(skill.cost() instanceof NoCostSpec)) return result(SkillExecutionResult.Status.UNSUPPORTED, 0, trace, "runtime.cost_unsupported");
		PreflightResult preflight = new SkillTargetPreflight().resolve(skill, context, trace);
		if (preflight.status() != PreflightResult.Status.READY) return result(map(preflight.status()), 0, trace, preflight.diagnostic());
		trace.record("cost", "variant=NO_COST status=COMMITTED");
		EffectResult primary = executors.execute(skill.effects().primary(), preflight.target(), context, trace);
		if (primary.status() != EffectResult.Status.APPLIED) return result(map(primary.status()), 0, trace, primary.diagnostic());
		int applied = primary.appliedAmount();
		trace.record("chain", "chain=" + skill.effects().chainId().value() + " primary=APPLIED");
		if (skill.effects().secondary() != null) {
			EffectResult secondary = executors.execute(skill.effects().secondary().effect(), preflight.target(), context, trace);
			trace.record("chain", "chain=" + skill.effects().chainId().value() + " secondary=" + secondary.status());
			if (secondary.status() != EffectResult.Status.APPLIED) return result(map(secondary.status()), applied, trace, secondary.diagnostic());
			applied += secondary.appliedAmount();
		}
		return result(SkillExecutionResult.Status.APPLIED, applied, trace, "runtime.applied");
	}
	private static SkillExecutionResult result(SkillExecutionResult.Status status, int amount, RuntimeTrace trace, String diagnostic) {
		trace.record("result", "status=" + status + " applied=" + amount + " diagnostic=" + diagnostic);
		return new SkillExecutionResult(status, amount, trace, diagnostic);
	}
	private static SkillExecutionResult.Status map(PreflightResult.Status status) {
		switch (status) { case NO_TARGET: return SkillExecutionResult.Status.NO_TARGET; case BLOCKED: return SkillExecutionResult.Status.BLOCKED; default: return SkillExecutionResult.Status.UNSUPPORTED; }
	}
	private static SkillExecutionResult.Status map(EffectResult.Status status) {
		switch (status) { case BLOCKED: return SkillExecutionResult.Status.BLOCKED; case MISSING_EXECUTOR: return SkillExecutionResult.Status.MISSING_EXECUTOR; default: return SkillExecutionResult.Status.UNSUPPORTED; }
	}
}
