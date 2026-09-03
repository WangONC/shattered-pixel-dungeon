package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.BuilderFormController;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.RandomIdGenerator;

/**
 * Stable public entrypoint for the v6 player builder. The view receives only the form controller;
 * field rendering and edits cannot bypass the shared PlayerBuildSession reducer.
 */
public final class WndCreateClassV6 {
	private WndCreateClassV6() {}

	public static void show() {
		show(PlayerBuildSession.empty(new RandomIdGenerator()));
	}

	public static void show(PlayerBuildSession session) {
		if (session == null) throw new IllegalArgumentException("player build session is required");
		WndCreateClassV6ControllerView.show(new BuilderFormController(session));
	}
}
