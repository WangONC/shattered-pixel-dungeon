package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleResourceState;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;

/** Compact multi-resource combat readout. The primary pool is always visible. */
public class ClassResourceHUD extends com.watabou.noosa.ui.Component {
	private static final int NORMAL_COLOR = 0xFFE7A8;
	private RenderedTextBlock text;
	private String last = "";
	private float changePulse;
	@Override protected void createChildren() { text = PixelScene.renderTextBlock(7); text.hardlight(NORMAL_COLOR); add(text); }
	@Override protected void layout() { text.maxWidth((int)width); text.setPos(x, y); height = Math.max(8, text.height()); }
	@Override public void update() {
		super.update();
		RuleRuntime runtime = Dungeon.hero == null ? null : Dungeon.hero.ruleRuntime();
		visible = runtime != null && runtime.classBuild() != null && !runtime.classBuild().resources.isEmpty();
		if (!visible) return;
		String value = readout(runtime);
		if (!last.equals(value)) {
			boolean firstValue = last.isEmpty();
			last = value;
			text.text(last);
			if (!firstValue) changePulse = 0.35f;
			layout();
		}
		if (changePulse > 0f) {
			changePulse -= Game.elapsed;
			text.hardlight(0xFFFFFF);
		} else text.hardlight(NORMAL_COLOR);
	}

	/** Machine-checkable text model shared with the rendered combat HUD. */
	public static String readout(RuleRuntime runtime) {
		if (runtime == null || runtime.classBuild() == null || runtime.classBuild().resources.isEmpty()) return "";
		ClassBuild build = runtime.classBuild(); ResourceSpec primary = build.primaryResource();
		StringBuilder value = new StringBuilder(primary.displayName()).append(' ')
				.append(runtime.resourceValue(primary.id, primary.engine)).append('/').append(runtime.resourceMax(primary.id, primary.engine));
		for (RuleResourceState state : runtime.additionalResourceStates()) value.append("  ")
				.append(state.spec == null ? state.engine.displayName() : state.spec.displayName()).append(' ')
				.append(state.value).append('/').append(state.max);
		return value.toString();
	}
}
