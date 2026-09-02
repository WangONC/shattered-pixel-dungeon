package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleHooks;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

/** Shared direct-action entry used by every ClassActionButton. */
public final class ClassActionExecutor {
	private static String pendingSkill;
	private static String pendingOperation;
	private ClassActionExecutor() {}

	public static void useSkill(Hero hero, String skillId) {
		if (!ready(hero)) return;
		RuleDefinition rule = hero.ruleRuntime().activeTechnique(skillId);
		if (rule == null) { GLog.w(Messages.get(ClassActionExecutor.class, "missing_skill")); return; }
		pendingSkill = skillId; pendingOperation = null;
		if (needsTarget(rule)) GameScene.selectCell(targeter); else perform(hero, hero.pos);
	}

	public static void useOperation(Hero hero, String operationId) {
		if (!ready(hero)) return;
		ClassOperationSpec operation = hero.ruleRuntime().classOperation(operationId);
		if (operation == null) { GLog.w(Messages.get(ClassActionExecutor.class, "missing_operation")); return; }
		String reason = ClassOperationRuntime.unavailableReason(hero, operation);
		if (reason != null) { GLog.w(reason); return; }
		pendingSkill = null; pendingOperation = operationId;
		if (ClassOperationRuntime.needsTarget(operation)) GameScene.selectCell(targeter); else perform(hero, hero.pos);
	}

	private static boolean ready(Hero hero) {
		return hero != null && hero.ruleRuntime() != null && hero.ready && !GameScene.cancel();
	}

	private static boolean needsTarget(RuleDefinition rule) {
		if (rule == null || rule.targetingSpec == null) return true;
		switch (rule.targetingSpec.selector) {
			case SELF:
			case NEAREST:
			case RANDOM:
			case ALL_MATCHING: return false;
			default: return true;
		}
	}

	private static void perform(Hero hero, int cell) {
		if (pendingSkill != null) {
			RuleDefinition rule = hero.ruleRuntime().activeTechnique(pendingSkill);
			if (rule == null) return;
			if (usesVisibleProjectile(rule) && canShowProjectile(hero, cell)) {
				final String skillId = pendingSkill; final int targetCell = cell;
				hero.busy();
				showProjectile(rule, hero, cell, new Callback() {
					@Override public void call() { resolveSkill(hero, skillId, targetCell); }
				});
			} else resolveSkill(hero, pendingSkill, cell);
		} else if (pendingOperation != null) {
			ClassOperationSpec operation = hero.ruleRuntime().classOperation(pendingOperation);
			if (operation == null || !ClassOperationRuntime.execute(hero, operation, cell)) {
				String reason = operation == null ? null : ClassOperationRuntime.unavailableReason(hero, operation);
				GLog.w(reason == null ? Messages.get(ClassActionExecutor.class, "operation_unavailable") : reason);
				return;
			}
			showOperationFeedback(hero, operation);
			Invisibility.dispel(hero);
			hero.spendAndNext(operation.actionTime);
		}
	}

	/** The same predicate used by the live action path; exposed so player-path QA cannot drift. */
	public static boolean usesVisibleProjectile(RuleDefinition rule) {
		return rule != null && rule.delivery == SkillDelivery.PROJECTILE;
	}

	private static void resolveSkill(Hero hero, String skillId, int cell) {
		RuleDefinition rule = hero.ruleRuntime() == null ? null : hero.ruleRuntime().activeTechnique(skillId);
		if (rule == null || !RuleHooks.triggerActive(hero, cell, skillId)) {
			GLog.w(Messages.get(ClassActionExecutor.class, "skill_unavailable"));
			hero.spendAndNext(0f);
			return;
		}
		Invisibility.dispel(hero);
		hero.spendAndNext(rule.cost.actionTime());
	}

	/** Gameplay has already resolved by Ballistica; this renders the same reachable destination. */
	private static boolean canShowProjectile(Hero hero, int cell) {
		return GameScene.sceneIsActive() && hero.sprite != null && hero.sprite.parent != null
				&& Dungeon.level != null && cell >= 0 && cell < Dungeon.level.length();
	}

	private static void showProjectile(RuleDefinition rule, Hero hero, int cell, Callback callback) {
		int missile = MagicMissile.MAGIC_MISSILE;
		if (rule.effectSpec != null) {
			EffectSpec.Operation operation = rule.effectSpec.operation;
			if (operation == EffectSpec.Operation.STATUS_BURNING || operation == EffectSpec.Operation.WORLD_FIRE) missile = MagicMissile.FIRE;
			else if (operation == EffectSpec.Operation.STATUS_POISON) missile = MagicMissile.POISON;
			else if (operation == EffectSpec.Operation.MOVE_PUSH || operation == EffectSpec.Operation.MOVE_PULL) missile = MagicMissile.FORCE;
		}
		boolean pierce = rule.modifier != null && rule.modifier.pierces() > 0;
		Ballistica path = new Ballistica(hero.pos, cell, pierce ? Ballistica.STOP_SOLID : Ballistica.PROJECTILE);
		int pathIndex = Math.min(path.dist, Math.min(rule.targetingSpec.range, path.path.size() - 1));
		int destination = pathIndex < 0 ? cell : path.path.get(pathIndex);
		MagicMissile.boltFromChar(hero.sprite.parent, missile, hero.sprite, destination, callback);
	}

	private static void showOperationFeedback(Hero hero, ClassOperationSpec operation) {
		if (!GameScene.sceneIsActive() || hero.sprite == null) return;
		switch (operation.type) {
			case RELOAD:
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				hero.sprite.showStatus(0x66CCFF, Messages.get(ClassActionExecutor.class, "reload_feedback"));
				break;
			case COMMAND: Sample.INSTANCE.play(Assets.Sounds.CLICK); break;
			case MODE_SWITCH: Sample.INSTANCE.play(Assets.Sounds.UNLOCK); break;
			case RECYCLE: Sample.INSTANCE.play(Assets.Sounds.TRAP); break;
		}
	}

	private static final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override public void onSelect(Integer cell) { if (cell != null && Dungeon.hero != null) perform(Dungeon.hero, cell); }
		@Override public String prompt() { return Messages.get(ClassActionExecutor.class, "select_target"); }
	};
}
