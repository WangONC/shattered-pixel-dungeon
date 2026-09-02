package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Saved semantic-event remap installed on one RuleRuntime. */
public class RuleEventBridge implements Bundlable {
	public RuleSemanticTag sourceTag = RuleSemanticTag.FORCED_MOVEMENT;
	public RuleEvent targetEvent = RuleEvent.ON_MOVE;

	public RuleEventBridge() {}

	public RuleEventBridge(RuleSemanticTag sourceTag, RuleEvent targetEvent) {
		this.sourceTag = sourceTag;
		this.targetEvent = targetEvent;
	}

	public boolean matches(RuleSemanticTag tag) {
		return sourceTag == tag;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("source_tag", sourceTag);
		bundle.put("target_event", targetEvent);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		sourceTag = bundle.getEnum("source_tag", RuleSemanticTag.class);
		targetEvent = bundle.getEnum("target_event", RuleEvent.class);
	}
}
