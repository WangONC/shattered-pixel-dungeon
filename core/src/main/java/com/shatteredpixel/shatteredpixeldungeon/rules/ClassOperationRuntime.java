package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMode;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleOwnership;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;

/** Executes formal class operations by reusing real resource, Buff, Actor, and ownership paths. */
public final class ClassOperationRuntime {
	private ClassOperationRuntime() {}

	public static boolean needsTarget(ClassOperationSpec operation) {
		return operation != null && (operation.type == ClassOperationSpec.Type.COMMAND
				|| operation.type == ClassOperationSpec.Type.RECYCLE);
	}

	public static String unavailableReason(Hero hero, ClassOperationSpec operation) {
		if (hero == null || hero.ruleRuntime() == null || operation == null) return Messages.get(ClassOperationRuntime.class, "no_runtime");
		RuleRuntime runtime = hero.ruleRuntime();
		switch (operation.type) {
			case RELOAD:
				int current = runtime.resourceValue(operation.resourceId, null);
				int maximum = runtime.resourceMax(operation.resourceId, null);
				if (maximum <= 0) return Messages.get(ClassOperationRuntime.class, "missing_resource");
				if (current >= maximum) return Messages.get(ClassOperationRuntime.class, "resource_full");
				return null;
			case COMMAND:
			case RECYCLE:
				for (Char ch : Actor.chars()) if (ch.isAlive() && RuleOwnership.isOwnedBy(ch, hero)
						&& matches(operation.entityFilter, ch)) return null;
				return Messages.get(ClassOperationRuntime.class, "no_owned_entity");
			case MODE_SWITCH:
				return runtime.classBuild() != null && runtime.classBuild().hasModeEngine()
						? null : Messages.get(ClassOperationRuntime.class, "no_modes");
			default: return Messages.get(ClassOperationRuntime.class, "unsupported");
		}
	}

	public static boolean execute(Hero hero, ClassOperationSpec operation, int cell) {
		if (unavailableReason(hero, operation) != null) return false;
		RuleRuntime runtime = hero.ruleRuntime();
		boolean applied;
		switch (operation.type) {
			case RELOAD:
				int current = runtime.resourceValue(operation.resourceId, null);
				int maximum = runtime.resourceMax(operation.resourceId, null);
				int amount = operation.amount <= 0 ? maximum - current : Math.min(operation.amount, maximum - current);
				applied = runtime.changeResource(hero, operation.resourceId, null, amount, true) > 0;
				break;
			case MODE_SWITCH:
				ArrayList<String> modes = runtime.classBuild().modes();
				RuleMode currentMode = hero.buff(RuleMode.class);
				int index = currentMode == null ? -1 : modes.indexOf(currentMode.modeId());
				String next = modes.get((index + 1) % modes.size());
				Buff.affect(hero, RuleMode.class).setPersistent(next);
				// A class operation is not a Skill, but it is still a real gameplay effect.
				// Feed the shared success hook so mode-aware Traits and causal traces do not
				// depend on a player manufacturing an ACTIVE -> MODE_SHIFT pseudo-skill.
				EffectSpec changed = new EffectSpec(EffectFamily.TRANSFORM,
						EffectSpec.Operation.TRANSFORM_MODE, 1);
				changed.stateId = next;
				runtime.onEffectSpecSuccess(changed, new RuleContext(RuleEvent.ACTIVE, hero), hero);
				applied = true;
				break;
			case COMMAND:
				applied = command(hero, operation, cell);
				break;
			case RECYCLE:
				Char target = Actor.findChar(cell);
				applied = target != null && target != hero && RuleOwnership.isOwnedBy(target, hero)
						&& matches(operation.entityFilter, target);
				if (applied) {
					target.die(ClassOperationRuntime.class);
					runtime.changeResource(hero, operation.resourceId, null, Math.max(1, operation.amount), true);
				}
				break;
			default: applied = false;
		}
		if (applied) RuleTrace.record("CLASS_OPERATION", operation.id + " type=" + operation.type + " cell=" + cell);
		return applied;
	}

	private static boolean command(Hero hero, ClassOperationSpec operation, int cell) {
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return false;
		Char selected = Actor.findChar(cell);
		boolean issued = false;
		for (Char ch : Actor.chars()) {
			if (!(ch instanceof DirectableAlly) || !ch.isAlive() || !RuleOwnership.isOwnedBy(ch, hero)
					|| !matches(operation.entityFilter, ch)) continue;
			DirectableAlly ally = (DirectableAlly)ch;
			if (selected != null && selected.alignment == Char.Alignment.ENEMY) ally.targetChar(selected);
			else if (cell == hero.pos) ally.followHero();
			else if (!Dungeon.level.solid[cell]) ally.defendPos(cell);
			else continue;
			issued = true;
		}
		if (issued) RuleTrace.record("COMMAND", (selected != null && selected.alignment == Char.Alignment.ENEMY
				? "attack" : cell == hero.pos ? "follow" : "guard") + " cell=" + cell);
		return issued;
	}

	private static boolean matches(ClassGameplayComponentSpec.EntityFilter filter, Char ch) {
		if (filter == null || filter == ClassGameplayComponentSpec.EntityFilter.OWNED_ENTITY) return true;
		if (!(ch instanceof RuleOwnedEntity)) return filter == ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR;
		RuleOwnedEntity.Kind kind = ((RuleOwnedEntity)ch).kind();
		return filter == ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR && kind == RuleOwnedEntity.Kind.ACTOR
				|| filter == ClassGameplayComponentSpec.EntityFilter.OWNED_DEVICE && kind == RuleOwnedEntity.Kind.DEVICE
				|| filter == ClassGameplayComponentSpec.EntityFilter.OWNED_CARRIER && kind == RuleOwnedEntity.Kind.FIELD;
	}
}
