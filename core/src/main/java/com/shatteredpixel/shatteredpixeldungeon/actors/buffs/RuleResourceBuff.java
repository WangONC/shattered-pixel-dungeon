package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleSemanticFormatter;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/** Persistent HUD indicator for a Hero-owned rule resource engine. */
public class RuleResourceBuff extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private RuleRuntime runtime() {
		return target instanceof Hero ? ((Hero) target).ruleRuntime() : null;
	}

	@Override
	public boolean act() {
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		RuleRuntime runtime = runtime();
		if (runtime == null || runtime.engine() == null) return BuffIndicator.NONE;
		switch (runtime.engine()) {
			case RAGE: return BuffIndicator.RAGE;
			case BLOOD: return BuffIndicator.HEART;
			case MOMENTUM: return BuffIndicator.HASTE;
			case FOCUS: return BuffIndicator.MIND_VISION;
			case AFFLICTION: return BuffIndicator.CORRUPT;
			case MANUAL: return BuffIndicator.RECHARGING;
			case MANA:
			default: return BuffIndicator.RECHARGING;
		}
	}

	@Override
	public void tintIcon(Image icon) {
		RuleRuntime runtime = runtime();
		if (runtime == null || runtime.engine() == null) return;
		if (runtime.engine() == ResourceEngine.MANA) icon.hardlight(0x4488FF);
		else if (runtime.engine() == ResourceEngine.RAGE) icon.hardlight(0xFF5533);
		else if (runtime.engine() == ResourceEngine.MOMENTUM) icon.hardlight(0x55DD88);
		else if (runtime.engine() == ResourceEngine.FOCUS) icon.hardlight(0xFFD966);
		else if (runtime.engine() == ResourceEngine.AFFLICTION) icon.hardlight(0xAA66CC);
		else icon.hardlight(0xAA2233);
	}

	@Override
	public float iconFadePercent() {
		RuleRuntime runtime = runtime();
		if (runtime == null || runtime.maxResource() <= 0) return 0;
		return 1f - runtime.resource() / (float) runtime.maxResource();
	}

	@Override
	public String iconTextDisplay() {
		RuleRuntime runtime = runtime();
		if (runtime == null || runtime.engine() == null) return "";
		return runtime.engine() == ResourceEngine.BLOOD
				? Messages.get(this, "hp") : Integer.toString(runtime.resource());
	}

	@Override
	public String name() {
		RuleRuntime runtime = runtime();
		return runtime == null || runtime.engine() == null ? Messages.get(this, "name")
				: runtime.primaryResourceName();
	}

	@Override
	public String desc() {
		RuleRuntime runtime = runtime();
		if (runtime == null) return Messages.get(this, "no_runtime");
		return RuleSemanticFormatter.resourceTooltip(runtime, (Hero) target);
	}
}
