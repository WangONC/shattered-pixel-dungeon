package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Explicit declaration of the compatible SPD starting-kit shell. */
public class StartingKitSpec implements Bundlable {
	public enum Mode { STANDARD_DUNGEON_KIT, UNARMED }
	public Mode mode = Mode.STANDARD_DUNGEON_KIT;
	public String requiredCarrier = "";

	public StartingKitSpec copy() {
		StartingKitSpec result = new StartingKitSpec();
		result.mode = mode;
		result.requiredCarrier = requiredCarrier;
		return result;
	}

	public int powerCost() { return 0; }
	public String displayName() { return Messages.get(StartingKitSpec.class, mode.name().toLowerCase() + "_name"); }
	public String description() { return Messages.get(StartingKitSpec.class, mode.name().toLowerCase() + "_desc"); }

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("mode", mode);
		bundle.put("required_carrier", requiredCarrier);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		mode = bundle.getEnum("mode", Mode.class);
		requiredCarrier = bundle.getString("required_carrier");
	}
}
