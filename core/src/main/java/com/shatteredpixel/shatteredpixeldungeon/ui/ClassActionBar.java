package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerFacingClassBuildFormatter;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/** Edge-tag combat actions for active Skills and formal ClassOperations. */
public class ClassActionBar extends com.watabou.noosa.ui.Component {
	private static final int DIRECT_LIMIT = 4;
	/** Tag body width before the platform safe inset is added. */
	public static final int TAG_WIDTH = 30;
	public static final int TAG_HEIGHT = 24;
	private static final float ICON_SCALE = 0.75f;
	private static final int SKILL_COLOR = 0x286A98;
	private static final int OPERATION_COLOR = 0x65508E;
	private static final int MORE_COLOR = 0x555B66;

	// Component's constructor invokes createChildren() before subclass field initializers run.
	private ArrayList<Action> actions;
	private ArrayList<Tag> tags;
	private String signature;
	private int directTagCount;

	private static final class Action {
		SkillSpec skill;
		ClassOperationSpec operation;

		String id() { return skill == null ? operation.id : skill.id; }
		String name(ClassBuild build) {
			if (skill == null) return operation.displayName(build);
			if (skill.name != null && !skill.name.trim().isEmpty()) return skill.name.trim();
			return skill.primary == null ? PlayerFacingClassBuildFormatter.skillName(skill) : skill.primary.displayName();
		}
		String description(ClassBuild build) {
			return skill == null ? operation.description(build) : PlayerFacingClassBuildFormatter.skillDetail(skill, build);
		}
		Image icon() { return skill == null ? ClassActionIcon.operation(operation) : ClassActionIcon.skill(skill); }
	}

	/** Machine-checkable model of the exact actions this HUD can expose. */
	public static ArrayList<String> actionIds(ClassBuild build) {
		ArrayList<String> result = new ArrayList<>();
		if (build == null) return result;
		build.syncClassOperations();
		for (SkillSpec skill : build.skills) if (skill != null && skill.activation == RuleEvent.ACTIVE)
			result.add("skill:" + skill.id);
		for (ClassOperationSpec operation : build.operations) if (operation != null)
			result.add("operation:" + operation.id);
		return result;
	}

	/** Player-authored names in the same ordering as actionIds and the live HUD. */
	public static ArrayList<String> actionLabels(ClassBuild build) {
		ArrayList<String> result = new ArrayList<>();
		if (build == null) return result;
		build.syncClassOperations();
		for (SkillSpec skill : build.skills) if (skill != null && skill.activation == RuleEvent.ACTIVE) {
			Action action = new Action(); action.skill = skill; result.add(action.name(build));
		}
		for (ClassOperationSpec operation : build.operations) if (operation != null) {
			Action action = new Action(); action.operation = operation; result.add(action.name(build));
		}
		return result;
	}

	@Override protected void createChildren() {
		actions = new ArrayList<>();
		tags = new ArrayList<>();
		signature = "";
		directTagCount = 0;
		// Hidden bars must remain active: their first update discovers the saved build.
		active = true;
		visible = false;
	}

	private void rebuild() {
		if (actions == null) actions = new ArrayList<>();
		if (tags == null) tags = new ArrayList<>();
		for (Tag tag : tags) if (tag != null) { remove(tag); tag.destroy(); }
		tags.clear();
		actions.clear();
		directTagCount = 0;

		RuleRuntime runtime = Dungeon.hero == null ? null : Dungeon.hero.ruleRuntime();
		ClassBuild build = runtime == null ? null : runtime.classBuild();
		if (build == null) { active = true; visible = false; GameScene.layoutTags(); return; }
		build.syncClassOperations();
		for (SkillSpec skill : build.skills) if (skill != null && skill.activation == RuleEvent.ACTIVE) {
			Action action = new Action(); action.skill = skill; actions.add(action);
		}
		for (ClassOperationSpec operation : build.operations) if (operation != null) {
			Action action = new Action(); action.operation = operation; actions.add(action);
		}

		active = true;
		visible = !actions.isEmpty();
		if (actions.isEmpty()) { GameScene.layoutTags(); return; }
		int direct = actions.size() <= DIRECT_LIMIT ? actions.size() : DIRECT_LIMIT - 1;
		directTagCount = direct;
		for (int i = 0; i < direct; i++) addActionTag(actions.get(i), build);
		if (actions.size() > DIRECT_LIMIT) addMoreTag(direct, build);
		GameScene.layoutTags();
	}

	private void addActionTag(final Action action, final ClassBuild build) {
		ClassActionTag tag = new ClassActionTag(action, build);
		tags.add(tag);
		add(tag);
	}

	private void addMoreTag(final int firstHidden, final ClassBuild build) {
		MoreTag tag = new MoreTag(firstHidden, build);
		tags.add(tag);
		add(tag);
	}

	/** Called by GameScene.layoutTags; returns the next free vertical position. */
	public float layoutTags(float tagLeft, float pos, float tagWidth, boolean flipped) {
		if (!visible || tags == null) return pos;
		for (Tag tag : tags) {
			if (tag == null || !tag.visible) continue;
			tag.setRect(tagLeft, pos - TAG_HEIGHT, tagWidth, TAG_HEIGHT);
			tag.flip(flipped);
			pos = tag.top();
		}
		return pos;
	}

	public boolean hasVisibleActions() { return visible && tags != null && !tags.isEmpty(); }

	@Override protected void layout() {
		// Individual tags are positioned by GameScene.layoutTags alongside vanilla tags.
	}

	@Override public void update() {
		super.update();
		RuleRuntime runtime = Dungeon.hero == null ? null : Dungeon.hero.ruleRuntime();
		String next = signature(runtime);
		if (signature == null || !next.equals(signature)) { signature = next; rebuild(); }
		if (runtime == null || Dungeon.hero == null || actions == null || tags == null) return;
		int count = Math.min(directTagCount, Math.min(actions.size(), tags.size()));
		for (int i = 0; i < count; i++) {
			Action action = actions.get(i);
			if (action == null || !(tags.get(i) instanceof ClassActionTag)) continue;
			boolean enabled = Dungeon.hero.ready;
			if (action.operation != null) {
				enabled &= ClassOperationRuntime.unavailableReason(Dungeon.hero, action.operation) == null;
			} else {
				RuleDefinition rule = runtime.activeTechnique(action.skill.id);
				enabled &= rule != null && rule.cooldownRemaining() <= 0 && rule.usesRemaining() != 0
						&& rule.cost.canPay(runtime, Dungeon.hero);
			}
			((ClassActionTag)tags.get(i)).enabled(enabled);
		}
	}

	private static String signature(RuleRuntime runtime) {
		if (runtime == null) return "";
		ClassBuild build = runtime.classBuild();
		if (build == null) return "";
		build.syncClassOperations();
		StringBuilder out = new StringBuilder();
		for (SkillSpec skill : build.skills) if (skill != null && skill.activation == RuleEvent.ACTIVE)
			out.append('S').append(skill.id).append(skill.name);
		for (ClassOperationSpec operation : build.operations) if (operation != null)
			out.append('O').append(operation.id).append(operation.name);
		return out.toString();
	}

	private static String compactName(String value) {
		if (value == null) return "";
		int points = value.codePointCount(0, value.length());
		if (points <= 5) return value;
		return value.substring(0, value.offsetByCodePoints(0, 4)) + "…";
	}

	private final class ClassActionTag extends Tag {
		private final Action action;
		private final String fullName;
		private final String detail;
		private final Image icon;
		private final RenderedTextBlock label;
		private boolean enabled = true;

		ClassActionTag(Action action, ClassBuild build) {
			super(action.skill == null ? OPERATION_COLOR : SKILL_COLOR);
			this.action = action;
			fullName = action.name(build);
			detail = action.description(build);
			icon = action.icon();
			icon.scale.set(ICON_SCALE);
			label = PixelScene.renderTextBlock(compactName(fullName), 5);
			label.hardlight(0xFFFFFF);
			add(icon);
			add(label);
			setSize(TAG_WIDTH, TAG_HEIGHT);
		}

		void enabled(boolean value) {
			enabled = value;
			float alpha = value ? 1f : 0.35f;
			icon.alpha(alpha);
			label.alpha(alpha);
			bg.alpha(value ? 1f : 0.65f);
		}

		@Override protected void layout() {
			super.layout();
			float safeInset = Math.max(0, width - TAG_WIDTH);
			float bodyLeft = x + (flipped ? safeInset : 0);
			float bodyWidth = width - safeInset;
			icon.x = bodyLeft + (bodyWidth - icon.width() * icon.scale.x) / 2f;
			icon.y = y + 1;
			label.maxWidth(Math.max(1, (int)bodyWidth - 2));
			label.setPos(bodyLeft + Math.max(0, (bodyWidth - label.width()) / 2f), y + 16);
			PixelScene.align(icon);
			PixelScene.align(label);
		}

		@Override protected void onClick() {
			super.onClick();
			if (!enabled || Dungeon.hero == null) return;
			if (action.skill == null) ClassActionExecutor.useOperation(Dungeon.hero, action.operation.id);
			else ClassActionExecutor.useSkill(Dungeon.hero, action.skill.id);
		}

		@Override protected boolean onLongClick() {
			if (Dungeon.hero == null || Dungeon.hero.ruleRuntime() == null) return true;
			ClassBuild current = Dungeon.hero.ruleRuntime().classBuild();
			if (current == null) return true;
			GameScene.show(new WndMessage(action.name(current) + "\n\n" + action.description(current)));
			return true;
		}

		@Override protected String hoverText() { return fullName + "\n" + detail; }
	}

	private final class MoreTag extends Tag {
		private final int firstHidden;
		private final ClassBuild build;
		private final Image icon;
		private final RenderedTextBlock label;

		MoreTag(int firstHidden, ClassBuild build) {
			super(MORE_COLOR);
			this.firstHidden = firstHidden;
			this.build = build;
			icon = Icons.CHEVRON.get();
			icon.scale.set(ICON_SCALE);
			label = PixelScene.renderTextBlock(Messages.get(ClassActionBar.class, "more"), 5);
			label.hardlight(0xFFFFFF);
			add(icon);
			add(label);
			setSize(TAG_WIDTH, TAG_HEIGHT);
		}

		@Override protected void layout() {
			super.layout();
			float safeInset = Math.max(0, width - TAG_WIDTH);
			float bodyLeft = x + (flipped ? safeInset : 0);
			float bodyWidth = width - safeInset;
			icon.x = bodyLeft + (bodyWidth - icon.width() * icon.scale.x) / 2f;
			icon.y = y + 1;
			label.maxWidth(Math.max(1, (int)bodyWidth - 2));
			label.setPos(bodyLeft + Math.max(0, (bodyWidth - label.width()) / 2f), y + 16);
			PixelScene.align(icon);
			PixelScene.align(label);
		}

		@Override protected void onClick() {
			super.onClick();
			if (Dungeon.hero == null || Dungeon.hero.ruleRuntime() == null) return;
			String[] names = new String[actions.size() - firstHidden];
			for (int i = firstHidden; i < actions.size(); i++) names[i-firstHidden] = actions.get(i).name(build);
			GameScene.show(new WndOptions(null, null, names) {
				@Override protected void onSelect(int index) {
					Action action = actions.get(firstHidden + index);
					if (action.skill == null) ClassActionExecutor.useOperation(Dungeon.hero, action.operation.id);
					else ClassActionExecutor.useSkill(Dungeon.hero, action.skill.id);
				}
			});
		}

		@Override protected String hoverText() { return Messages.get(ClassActionBar.class, "more_desc"); }
	}
}
