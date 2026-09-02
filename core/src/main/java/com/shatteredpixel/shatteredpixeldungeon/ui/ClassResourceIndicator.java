package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;

import java.util.ArrayList;

/** StatusPane-native readout for class resources. This is UI state, never a Buff. */
public class ClassResourceIndicator extends com.watabou.noosa.ui.Component {
	private static final int NORMAL_COLOR = 0xFFE7A8;
	private ArrayList<ResourceSpec> resources;
	private ArrayList<RenderedTextBlock> values;
	private String structureSignature;
	private String valueSignature;
	private float changePulse;
	private boolean large;
	private BuffIndicator buffs;

	public ClassResourceIndicator(boolean large, BuffIndicator buffs) {
		super();
		this.large = large;
		this.buffs = buffs;
		visible = false;
	}

	@Override protected void createChildren() {
		resources = new ArrayList<>();
		values = new ArrayList<>();
		structureSignature = "";
		valueSignature = "";
	}

	@Override public void update() {
		super.update();
		RuleRuntime runtime = Dungeon.hero == null ? null : Dungeon.hero.ruleRuntime();
		ClassBuild build = runtime == null ? null : runtime.classBuild();
		if (build == null || build.resources.isEmpty()) {
			visible = false;
			return;
		}
		visible = true;

		String structure = structureSignature(build);
		if (!structure.equals(structureSignature)) rebuild(build, structure);
		String currentValues = valueSignature(runtime);
		if (!currentValues.equals(valueSignature)) {
			boolean first = valueSignature.isEmpty();
			valueSignature = currentValues;
			refreshValues(runtime);
			if (!first) changePulse = 0.35f;
			layout();
		}

		if (changePulse > 0f) {
			changePulse -= Game.elapsed;
			for (RenderedTextBlock value : values) value.hardlight(0xFFFFFF);
		} else {
			for (RenderedTextBlock value : values) value.hardlight(NORMAL_COLOR);
		}
		//Buffs can be added or removed without changing any resource value.
		layout();
	}

	private void rebuild(ClassBuild build, String structure) {
		for (RenderedTextBlock value : values) { remove(value); value.destroy(); }
		resources.clear(); values.clear();
		for (ResourceSpec resource : build.resources) {
			if (resource == null) continue;
			ResourceSpec copy = resource.copy();
			resources.add(copy);
			RenderedTextBlock value = PixelScene.renderTextBlock(large ? 6 : 5);
			value.hardlight(NORMAL_COLOR);
			values.add(value); add(value);
		}
		structureSignature = structure;
		valueSignature = "";
	}

	private void refreshValues(RuleRuntime runtime) {
		for (int i = 0; i < resources.size(); i++) {
			ResourceSpec resource = resources.get(i);
			RenderedTextBlock text = values.get(i);
			text.text(compactName(resource.displayName()) + " "
					+ runtime.resourceValue(resource.id, resource.engine) + "/"
					+ runtime.resourceMax(resource.id, resource.engine));
		}
	}

	@Override protected void layout() {
		if (values == null || values.isEmpty()) return;
		PointF anchor = buffs == null ? null : buffs.resourceReadoutAnchor();
		float cursor = anchor == null ? x : anchor.x + 1;
		float rowTop = anchor == null ? y : anchor.y;
		float gap = large ? 3 : 1;
		for (int i = 0; i < values.size(); i++) {
			RenderedTextBlock value = values.get(i);
			float needed = value.width();
			if (cursor + needed > right() && i > 0) {
				value.visible = false;
				continue;
			}
			value.visible = true;
			value.setPos(cursor, anchor == null ? rowTop + (height - value.height()) / 2f
					: rowTop - value.height() / 2f);
			PixelScene.align(value);
			cursor += needed + gap;
		}
	}

	public void alpha(float alpha) {
		for (RenderedTextBlock value : values) value.alpha(alpha);
	}

	private String valueSignature(RuleRuntime runtime) {
		StringBuilder result = new StringBuilder();
		for (ResourceSpec resource : resources) result.append(resource.id).append(':')
				.append(runtime.resourceValue(resource.id, resource.engine)).append('/')
				.append(runtime.resourceMax(resource.id, resource.engine)).append(';');
		return result.toString();
	}

	private static String structureSignature(ClassBuild build) {
		StringBuilder result = new StringBuilder();
		for (ResourceSpec resource : build.resources) if (resource != null) result.append(resource.id)
				.append(':').append(resource.engine).append(':').append(resource.displayName()).append(';');
		return result.toString();
	}

	private String compactName(String name) {
		if (name == null) return "";
		int limit = large ? 10 : 6;
		int points = name.codePointCount(0, name.length());
		if (points <= limit) return name;
		return name.substring(0, name.offsetByCodePoints(0, limit - 1)) + "…";
	}
}
