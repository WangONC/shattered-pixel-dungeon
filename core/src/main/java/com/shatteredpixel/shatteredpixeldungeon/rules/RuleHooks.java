package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleResourceBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.RuntimeExecutionContext;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.SkillExecutionResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.V6RuleRuntimeBridge;

/** The small, stable integration surface used by core SPD classes. */
public final class RuleHooks {
	private static final ThreadLocal<Integer> RULE_STATUS_DEPTH = new ThreadLocal<>();
	private static final ThreadLocal<java.util.ArrayList<RuleContext>> RULE_DAMAGE_CONTEXT = new ThreadLocal<>();
	private static long nextStandaloneV6EventId = 1_000_000_000L;

	private RuleHooks() {}

	private static RuleRuntime runtime(Hero hero) {
		return hero == null ? null : hero.ruleRuntime();
	}

	private static boolean dispatch(Hero hero, RuleContext context) {
		RuleRuntime runtime = runtime(hero);
		boolean fired = runtime != null && runtime.dispatch(context);
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleActionAttachment.trigger(hero, context);
		return fired;
	}

	public static void onTurnStart(Hero hero) {
		dispatch(hero, new RuleContext(RuleEvent.ON_TURN_START, hero));
	}

	public static void onWait(Hero hero) {
		dispatch(hero, new RuleContext(RuleEvent.ON_WAIT, hero));
	}

	public static void onMove(Hero hero, int from, int to) {
		if (from == to) return;
		RuleContext move = new RuleContext(RuleEvent.ON_MOVE, hero);
		move.cell = to;
		dispatch(hero, move);
		RuleContext enter = new RuleContext(RuleEvent.ON_ENTER_TILE, hero);
		enter.cell = to;
		dispatch(hero, enter);
	}

	public static void onAttack(Hero hero, Char target) {
		RuleContext context = new RuleContext(RuleEvent.ON_ATTACK, hero);
		context.target = target;
		dispatch(hero, context);
	}

	public static void onHit(Hero hero, Char target, boolean melee) {
		RuleContext context = new RuleContext(RuleEvent.ON_HIT, hero);
		context.target = target;
		context.cell = target == null ? -1 : target.pos;
		context.melee = melee;
		dispatch(hero, context);
	}

	public static void onDamaged(Hero hero, Object source, int damage) {
		RuleContext context = new RuleContext(RuleEvent.ON_DAMAGED, hero);
		if (source instanceof Char) context.source = (Char) source;
		context.amount = damage;
		dispatch(hero, context);
		if (hero.HP * 100 <= hero.HT * 30) {
			RuleContext low = new RuleContext(RuleEvent.ON_LOW_HP, hero);
			low.source = context.source;
			low.amount = damage;
			dispatch(hero, low);
		}
	}

	public static void onKill(Hero hero, Char target) {
		java.util.ArrayList<RuleContext> chain = RULE_DAMAGE_CONTEXT.get();
		RuleContext parent = chain == null || chain.isEmpty() ? null : chain.get(chain.size() - 1);
		RuleContext context = parent == null ? new RuleContext(RuleEvent.ON_KILL, hero) : parent.child(RuleEvent.ON_KILL);
		if (context == null) {
			RuleTrace.record("CHAIN_GUARD", "ON_KILL already present root=#" + parent.rootEventId());
			return;
		}
		context.target = target;
		context.cell = target == null ? -1 : target.pos;
		if (parent != null) RuleTrace.record("CHAIN", "#" + parent.eventId() + " -> ON_KILL target="
				+ (target == null ? -1 : target.id()));
		dispatch(hero, context);
	}

	static void beginRuleDamage(RuleContext context) {
		java.util.ArrayList<RuleContext> chain = RULE_DAMAGE_CONTEXT.get();
		if (chain == null) { chain = new java.util.ArrayList<>(); RULE_DAMAGE_CONTEXT.set(chain); }
		chain.add(context);
	}

	static void endRuleDamage() {
		java.util.ArrayList<RuleContext> chain = RULE_DAMAGE_CONTEXT.get();
		if (chain == null || chain.isEmpty()) return;
		chain.remove(chain.size() - 1);
		if (chain.isEmpty()) RULE_DAMAGE_CONTEXT.remove();
	}

	public static void onItemUse(Hero hero, Item item) {
		RuleContext context = new RuleContext(RuleEvent.ON_ITEM_USE, hero);
		context.item = item;
		dispatch(hero, context);
	}

	public static void onStatusApplied(Hero hero, Buff status) {
		if (status instanceof RuleResourceBuff || ruleStatusApplicationInProgress()) return;
		RuleContext context = new RuleContext(RuleEvent.ON_STATUS_APPLIED, hero);
		context.status = status;
		context.target = hero;
		context.cell = hero == null ? -1 : hero.pos;
		dispatch(hero, context);
	}

	static void beginRuleStatusApplication() {
		Integer depth = RULE_STATUS_DEPTH.get();
		RULE_STATUS_DEPTH.set(depth == null ? 1 : depth + 1);
	}

	static void endRuleStatusApplication() {
		Integer depth = RULE_STATUS_DEPTH.get();
		if (depth == null || depth <= 1) RULE_STATUS_DEPTH.remove();
		else RULE_STATUS_DEPTH.set(depth - 1);
	}

	private static boolean ruleStatusApplicationInProgress() {
		Integer depth = RULE_STATUS_DEPTH.get();
		return depth != null && depth > 0;
	}

	/** Dispatches the child event only after a real Buff has attached to its target. */
	static void onRuleStatusApplied(RuleContext parent, Char target, Buff status) {
		if (parent == null || parent.hero == null || status == null) return;
		emitSemantic(parent, RuleSemanticTag.STATUS_APPLICATION, target,
				target == null ? -1 : target.pos, target == null ? -1 : target.pos);
		RuleContext child = parent.child(RuleEvent.ON_STATUS_APPLIED);
		if (child == null) {
			RuleTrace.record("CHAIN_GUARD", "ON_STATUS_APPLIED already present root=#" + parent.rootEventId());
			return;
		}
		child.source = parent.source == null ? parent.hero : parent.source;
		child.target = target;
		child.status = status;
		child.cell = target == null ? -1 : target.pos;
		RuleTrace.record("CHAIN", "#" + parent.eventId() + " -> ON_STATUS_APPLIED"
				+ " status=" + status.getClass().getSimpleName());
		dispatch(parent.hero, child);
	}

	static void emitSemantic(RuleContext parent, RuleSemanticTag tag, Char target, int from, int to) {
		if (parent == null || parent.hero == null || tag == null) return;
		RuleRuntime runtime = runtime(parent.hero);
		if (runtime != null) runtime.emitSemantic(parent, tag, target, from, to);
	}

	static void onForcedMovement(RuleContext parent, Char target, int from, int to) {
		if (from != to) {
			com.shatteredpixel.shatteredpixeldungeon.qa.QaCombatMetrics.recordForcedMovement(target, from, to);
			emitSemantic(parent, RuleSemanticTag.FORCED_MOVEMENT, target, from, to);
		}
	}

	static void onEffectApplied(RuleContext parent, RuleEffect effect, Char target, int cell) {
		if (effect == null) return;
		for (RuleSemanticTag tag : effect.tags()) {
			//These tags are emitted only by their actual completion points.
			if (tag == RuleSemanticTag.FORCED_MOVEMENT || tag == RuleSemanticTag.MOVEMENT
					|| tag == RuleSemanticTag.TRANSLOCATION || tag == RuleSemanticTag.STATUS_APPLICATION
					|| tag == RuleSemanticTag.MARK) continue;
			emitSemantic(parent, tag, target, cell, target == null ? cell : target.pos);
		}
	}

	public static boolean triggerActive(Hero hero, int cell) {
		return triggerActive(hero, cell, null);
	}

	public static boolean triggerActive(Hero hero, int cell, String skillId) {
		if (hero == null) return false;
		RuleContext context = new RuleContext(RuleEvent.ACTIVE, hero);
		context.cell = cell;
		RuleRuntime runtime = runtime(hero);
		boolean legacyFired = runtime != null && (skillId == null ? runtime.dispatch(context) : runtime.dispatchActive(skillId, context));
		V6RuleRuntimeBridge bridge = hero.gameplayComponentsV6RuntimeBridge();
		if (bridge == null) return legacyFired;
		if (context.eventId() == 0) context.assignEventId(nextStandaloneV6EventId());
		try {
			SkillExecutionResult result = bridge.triggerActive(hero, cell, skillId, context.eventId(),
					context.parentEventId(), v6DamageGateway(context));
			return legacyFired || result.status() == SkillExecutionResult.Status.APPLIED;
		} catch (IllegalArgumentException rejected) {
			RuleTrace.record("V6_ACTIVE_REJECTED", rejected.getMessage());
			return legacyFired;
		}
	}

	private static synchronized long nextStandaloneV6EventId() {
		return nextStandaloneV6EventId++;
	}

	private static RuntimeExecutionContext.DamageGateway v6DamageGateway(final RuleContext activeContext) {
		return (effect, source, target, event) -> {
			RuleContext damageContext = activeContext.causedByRule(event.originatingSkillId().value());
			damageContext.source = source;
			damageContext.target = target;
			damageContext.cell = target == null ? -1 : target.pos;
			damageContext.amount = effect.amount();
			int before = target.HP;
			beginRuleDamage(damageContext);
			try {
				target.damage(effect.amount(), source);
			} finally {
				endRuleDamage();
			}
			RuleTrace.record("V6_DAMAGE", "event=#" + event.eventId() + " cause=#" + event.causeEventId()
					+ " sourceRule=" + event.originatingSkillId().value() + " source=#" + source.id()
					+ " target=#" + target.id() + " applied=" + Math.max(0, before - target.HP));
			return new RuntimeExecutionContext.DamageOutcome(before, target.HP);
		};
	}

	public static boolean ordinaryWeaponsRestricted(Hero hero) {
		return runtime(hero) != null && runtime(hero).hasRestriction(Restriction.NO_ORDINARY_WEAPONS);
	}

	public static float healingPotionMultiplier(Hero hero) {
		if (runtime(hero) != null && !runtime(hero).traditionalHealingAllowed()) return 0f;
		return runtime(hero) != null && runtime(hero).hasRestriction(Restriction.WEAK_HEALING) ? 0.35f : 1f;
	}

	public static boolean healingPotionBecomesShield(Hero hero) {
		return runtime(hero) != null && runtime(hero).convertsHealingToShield();
	}

	/** Rule-based recovery remains available even when ordinary healing is restricted. */
	public static boolean applyHealing(Hero hero, int amount) {
		if (hero == null || amount <= 0) return false;
		RuleRuntime runtime = runtime(hero);
		if (runtime != null && runtime.convertsHealingToShield()) {
			Buff.affect(hero, Barrier.class).incShield(amount);
			RuleTrace.record("LAW", "HEALING_TO_SHIELD amount=" + amount);
			return true;
		}
		int before = hero.HP;
		hero.HP = Math.min(hero.HT, hero.HP + amount);
		return hero.HP > before;
	}
}
