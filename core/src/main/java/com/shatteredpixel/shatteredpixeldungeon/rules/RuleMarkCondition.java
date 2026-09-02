package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.watabou.utils.Bundle;

/** Internal reusable Condition module for saved Rule marks. */
public class RuleMarkCondition extends RuleCondition {
	public RuleMark.Type mark = RuleMark.Type.HUNTED;
	public int minimumStacks = 1;
	public boolean requireOwnerSource;

	public RuleMarkCondition() {
		super(Type.ALWAYS);
	}

	public RuleMarkCondition(RuleMark.Type mark, int minimumStacks) {
		this();
		this.mark = mark;
		this.minimumStacks = Math.max(1, minimumStacks);
	}

	@Override
	public boolean passes(RuleRuntime runtime, RuleContext context, Char resolvedTarget) {
		RuleMark value = RuleMark.get(resolvedTarget, mark);
		return value != null && value.stacks() >= Math.max(1, minimumStacks)
				&& (!requireOwnerSource || context.hero != null && value.sourceId() == context.hero.id());
	}

	@Override
	public String semanticId() {
		return "MARK_" + mark.name() + "_AT_LEAST_" + minimumStacks;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("mark", mark);
		bundle.put("minimum_stacks", minimumStacks);
		bundle.put("require_owner_source", requireOwnerSource);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		mark = bundle.contains("mark") ? bundle.getEnum("mark", RuleMark.Type.class) : RuleMark.Type.HUNTED;
		minimumStacks = Math.max(1, bundle.getInt("minimum_stacks"));
		requireOwnerSource = bundle.getBoolean("require_owner_source");
	}
}
