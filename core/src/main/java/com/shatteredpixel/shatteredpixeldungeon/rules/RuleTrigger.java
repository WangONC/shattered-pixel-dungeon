package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

public class RuleTrigger implements RuleModule, Bundlable {
	public RuleEvent event = RuleEvent.ACTIVE;

	public RuleTrigger() {}

	public RuleTrigger(RuleEvent event) {
		this.event = event;
	}

	@Override
	public int capacityCost() {
		return event == RuleEvent.ACTIVE ? 2 : 1;
	}

	@Override
	public String description() {
		return Messages.get(RuleTrigger.class, event.name().toLowerCase());
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("event", event);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		event = bundle.getEnum("event", RuleEvent.class);
	}
}
