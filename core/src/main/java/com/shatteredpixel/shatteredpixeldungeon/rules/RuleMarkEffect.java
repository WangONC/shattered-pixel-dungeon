package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.watabou.utils.Bundle;

import java.util.EnumSet;

/** Internal reusable Effect module for applying, stacking, consuming, or removing a Mark Buff. */
public class RuleMarkEffect extends RuleEffect {
	public enum Operation { APPLY, CONSUME, REMOVE }

	public RuleMark.Type mark = RuleMark.Type.HUNTED;
	public Operation operation = Operation.APPLY;
	public int stacks = 1;
	public int duration = 5;

	public RuleMarkEffect() {
		super(Type.SHIELD, 1);
	}

	public RuleMarkEffect(Operation operation, RuleMark.Type mark, int stacks, int duration) {
		this();
		this.operation = operation;
		this.mark = mark;
		this.stacks = Math.max(1, stacks);
		this.duration = Math.max(1, duration);
	}

	@Override
	public boolean apply(RuleContext context, Char target, int cell, RuleModifier modifier) {
		if (context == null || target == null) return false;
		if (operation == Operation.CONSUME) return RuleMark.consume(target, mark, stacks);
		if (operation == Operation.REMOVE) return RuleMark.remove(target, mark);
		RuleMark before = RuleMark.get(target, mark);
		RuleHooks.beginRuleStatusApplication();
		RuleMark applied;
		try {
			applied = RuleMark.apply(target, mark, context.hero, stacks, duration);
		} finally {
			RuleHooks.endRuleStatusApplication();
		}
		if (applied != null && before == null) RuleHooks.onRuleStatusApplied(context, target, applied);
		if (applied != null) RuleHooks.emitSemantic(context, RuleSemanticTag.MARK, target, cell, cell);
		return applied != null;
	}

	@Override
	public EnumSet<RuleSemanticTag> tags() {
		return operation == Operation.APPLY
				? EnumSet.of(RuleSemanticTag.MARK, RuleSemanticTag.STATUS_APPLICATION)
				: EnumSet.of(RuleSemanticTag.MARK);
	}

	@Override
	public String semanticId() {
		return operation.name() + "_MARK_" + mark.name();
	}

	@Override
	public RuleEffect copy() {
		return new RuleMarkEffect(operation, mark, stacks, duration);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("mark", mark);
		bundle.put("operation", operation);
		bundle.put("stacks", stacks);
		bundle.put("duration", duration);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		mark = bundle.contains("mark") ? bundle.getEnum("mark", RuleMark.Type.class) : RuleMark.Type.HUNTED;
		operation = bundle.contains("operation") ? bundle.getEnum("operation", Operation.class) : Operation.APPLY;
		stacks = Math.max(1, bundle.getInt("stacks"));
		duration = Math.max(1, bundle.getInt("duration"));
	}
}
