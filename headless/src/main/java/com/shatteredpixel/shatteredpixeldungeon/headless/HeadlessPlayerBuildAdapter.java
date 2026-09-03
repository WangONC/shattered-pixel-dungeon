package com.shatteredpixel.shatteredpixeldungeon.headless;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.IdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;

/** The only production headless player-path adapter; all transitions stay in the core v6 reducer. */
public final class HeadlessPlayerBuildAdapter {
	private final PlayerBuildSession session;
	private HeadlessPlayerBuildAdapter(PlayerBuildSession session) { this.session = session; }
	public static HeadlessPlayerBuildAdapter empty(IdGenerator ids) { return new HeadlessPlayerBuildAdapter(PlayerBuildSession.empty(ids)); }
	public BuilderState dispatch(BuilderCommand command) { return session.dispatch(command); }
	public BuilderState state() { return session.state(); }
	public String saveTrace() { return session.saveTrace(); }
	public ClassBuildSpec finalizeOrThrow() { return session.finalizeOrThrow(); }
}
