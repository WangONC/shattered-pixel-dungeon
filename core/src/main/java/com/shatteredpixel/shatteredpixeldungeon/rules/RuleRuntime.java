package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleResourceBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleResourceSuppression;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMode;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleOwnership;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleTemporaryHP;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;

/** Saved, per-owner rule instances plus deterministic dispatch and resource engine state. */
public class RuleRuntime implements Bundlable {
	private static final int MAX_EXECUTION_DEPTH = 4;

	private String customName = "custom";
	private ResourceEngine engine = ResourceEngine.MANA;
	private ResourceSpec primaryResourceSpec;
	private ClassLaw law = ClassLaw.HEALING_TO_SHIELD;
	private ClassBuild classBuild;
	private int resource;
	private int maxResource;
	private int turnCounter;
	private int turnsSinceCombat;
	private int turnsSinceMove;
	private int consecutiveMoves;
	private int focusSafeTurns;
	private int overflowRemainder;
	private int[] primaryFlowCounters = new int[0];
	private int traitMoveCounter;
	private int debugCapacityBonus;
	private int nextRuleOrder;
	private long nextEventId = 1;
	private long nextPayloadId = 1;
	private final ArrayList<RuleDefinition> rules = new ArrayList<>();
	private final ArrayList<RuleResourceState> additionalResources = new ArrayList<>();
	private final ArrayList<ClassLaw> laws = new ArrayList<>();
	private final ArrayList<RuleEventBridge> bridges = new ArrayList<>();
	private final ArrayList<RuleDelayedPayload> delayedPayloads = new ArrayList<>();
	private final ArrayList<CoreRuleVocabulary> vocabularies = new ArrayList<>();
	private final ArrayList<TraitSpec> traitSpecs = new ArrayList<>();
	private final EnumSet<Restriction> restrictions = EnumSet.noneOf(Restriction.class);
	private transient int executionDepth;
	private transient HashSet<Integer> executingRules = new HashSet<>();

	public RuleRuntime() {}

	public RuleRuntime(CustomClassConfig config) {
		this(config.toClassBuild());
		if (config.migrationOnlyModel()) preserveLegacyRuntimeSemantics(config.law);
	}

	private void preserveLegacyRuntimeSemantics(ClassLaw legacyLaw) {
		if (legacyLaw == null) return;
		laws.clear(); laws.add(legacyLaw); law=legacyLaw;
		CoreRuleVocabulary replacement=replacementForLegacyLaw(legacyLaw);
		if(replacement!=null){vocabularies.remove(replacement);for(int i=traitSpecs.size()-1;i>=0;i--)
			if(traitSpecs.get(i).type==replacement)traitSpecs.remove(i);}
		installLawBridges();
	}

	private static CoreRuleVocabulary replacementForLegacyLaw(ClassLaw value){
		if (value == null) return null;
		switch(value){case RESOURCE_OVERFLOW_TO_SHIELD:return CoreRuleVocabulary.OVERFLOW;
			case STATUS_ABSORPTION:return CoreRuleVocabulary.STATUS_FEEDBACK;
			case WATER_AFFINITY:return CoreRuleVocabulary.WATER_FLOW;
			case KILL_ACCELERATES_RULES:return CoreRuleVocabulary.KILL_TEMPO;
			default:return null;}
	}

	public RuleRuntime(ClassBuild build) {
		classBuild = build.copy();
		classBuild.normalizeLegacyComponents();
		classBuild.resolvePendingBindings();
		classBuild.syncClassOperations();
		customName = classBuild.name.trim();
		ResourceSpec primary = classBuild.primaryResource();
		if (primary == null) {
			engine = null;
			resource = maxResource = 0;
		} else {
			engine = ResourceEngine.MANUAL;
			primaryResourceSpec = primary.copy();
			primaryFlowCounters = new int[resourceFlowComponents(primary.id).size()];
			resource = primary.initialValue;
			primaryResourceSpec.current = resource;
			maxResource = primary.capacity;
			for (int i = 1; i < classBuild.resources.size(); i++) {
				RuleResourceState state = new RuleResourceState(classBuild.resources.get(i));
				state.flowCounters = new int[resourceFlowComponents(state.id).size()];
				additionalResources.add(state);
			}
		}
		laws.addAll(classBuild.laws);
		law = laws.isEmpty() ? null : laws.get(0);
		installLawBridges();
		for (RuleDefinition rule : classBuild.compileRules()) addRule(rule, true);
		for (Restriction restriction : classBuild.allRestrictions()) {
			if (restriction != null && restriction != Restriction.NONE) restrictions.add(restriction);
		}
		for (TraitSpec trait : classBuild.traits) installTrait(trait);
	}

	private void installLawBridges() {
		if (hasLaw(ClassLaw.FORCED_MOVEMENT_COUNTS_AS_MOVE)) {
			addBridge(new RuleEventBridge(RuleSemanticTag.FORCED_MOVEMENT, RuleEvent.ON_MOVE));
		}
		if (hasLaw(ClassLaw.TRANSLOCATION_COUNTS_AS_ENTER_TILE)) {
			addBridge(new RuleEventBridge(RuleSemanticTag.TRANSLOCATION, RuleEvent.ON_ENTER_TILE));
		}
	}

	private void installTrait(TraitSpec spec) {
		if (spec == null || spec.type == null || spec.type == CoreRuleVocabulary.NONE) return;
		for (TraitSpec current : traitSpecs) if (current.stableId().equals(spec.stableId())) return;
		traitSpecs.add(spec.copy());
		installVocabulary(spec.type);
	}

	private void installVocabulary(CoreRuleVocabulary entry) {
		if (entry == null || entry == CoreRuleVocabulary.NONE || vocabularies.contains(entry)) return;
		vocabularies.add(entry);
		if (entry == CoreRuleVocabulary.INERTIA_BRIDGE) {
			addBridge(new RuleEventBridge(RuleSemanticTag.FORCED_MOVEMENT, RuleEvent.ON_MOVE));
		} else if (entry == CoreRuleVocabulary.TRANSLOCATION_BRIDGE) {
			addBridge(new RuleEventBridge(RuleSemanticTag.TRANSLOCATION, RuleEvent.ON_ENTER_TILE));
		}
	}

	public static void installPending(Hero hero) {
		CustomClassConfig config = CustomClassConfig.consumePending();
		if (config == null) return;
		RuleRuntime runtime = new RuleRuntime(config);
		hero.setRuleRuntime(runtime);
		runtime.installNewHeroFeatures(hero);
	}

	public static RuleRuntime installDebug(Hero hero) {
		CustomClassConfig config = RuleTestBuilds.config(RuleTestBuilds.Preset.A_MOMENTUM_SKIRMISHER);
		config.name = Messages.get(RuleRuntime.class, "dev_name");
		return installDebug(hero, config);
	}

	public static RuleRuntime installDebug(Hero hero, CustomClassConfig config) {
		if (hero.ruleRuntime() != null) hero.ruleRuntime().uninstallDebugFeatures(hero);
		RuleRuntime runtime = new RuleRuntime(config);
		runtime.debugCapacityBonus = 20;
		hero.setRuleRuntime(runtime);
		runtime.installNewHeroFeatures(hero);
		return runtime;
	}

	private void uninstallDebugFeatures(Hero hero) {
		if (hasRestriction(Restriction.FRAIL)) {
			hero.HTBoost += 5;
			hero.updateHT(false);
		}
	}

	private void installNewHeroFeatures(Hero hero) {
		// The temporary Warrior chassis must not leak its signature Broken Seal into custom classes.
		if (hero.belongings.armor != null && hero.belongings.armor.checkSeal() != null) {
			hero.belongings.armor.detachSeal();
		}
		Buff.detach(hero, com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal.WarriorShield.class);
		boolean unarmedKit = classBuild != null && classBuild.startingKit != null
				&& classBuild.startingKit.mode == StartingKitSpec.Mode.UNARMED;
		if ((hasRestriction(Restriction.NO_ORDINARY_WEAPONS) || unarmedKit) && hero.belongings.weapon != null) {
			KindOfWeapon weapon = hero.belongings.weapon;
			hero.belongings.weapon = null;
			weapon.collect(hero.belongings.backpack);
		}
		if (hasRestriction(Restriction.FRAIL)) {
			hero.HTBoost -= 5;
			hero.updateHT(false);
		}
		ensureVisuals(hero);
	}

	public void ensureVisuals(Hero hero) {
		if (engine != null) Buff.affect(hero, RuleResourceBuff.class);
		else Buff.detach(hero, RuleResourceBuff.class);
		// RuleAbility was the V0.2 shared-book UI. New builds use ClassActionBar; remove the old
		// carrier when loading a migrated save so there is only one player-facing active path.
		com.shatteredpixel.shatteredpixeldungeon.items.RuleAbility legacy = hero.belongings.getItem(
				com.shatteredpixel.shatteredpixeldungeon.items.RuleAbility.class);
		if (legacy != null) legacy.detachAll(hero.belongings.backpack);
	}

	private boolean hasActiveRule() {
		for (RuleDefinition rule : rules) if (rule.trigger.event == RuleEvent.ACTIVE) return true;
		return false;
	}

	public boolean dispatch(RuleContext context) {
		return dispatch(context, null);
	}

	public boolean dispatchActive(String skillId, RuleContext context) {
		if (context == null || context.event != RuleEvent.ACTIVE) return false;
		return dispatch(context, skillId);
	}

	private boolean dispatch(RuleContext context, String onlyRuleId) {
		if (context == null || context.hero == null) return false;
		activateDelayedPayloads();
		if (executionDepth >= MAX_EXECUTION_DEPTH) {
			RuleTrace.record("DEPTH_GUARD", context.event.name() + " depth=" + executionDepth);
			return false;
		}
		context.assignEventId(nextEventId++);
		nextEventId = Math.max(nextEventId, context.eventId() + 1);
		RuleTrace.record("EVENT", "#" + context.eventId() + " " + context.event.name()
				+ " parent=#" + context.parentEventId() + " root=#" + context.rootEventId()
				+ " original=" + context.originalEvent().name()
				+ (context.sourceRuleId() == null ? "" : " sourceRule=" + context.sourceRuleId())
				+ " depth=" + executionDepth);
		if (executingRules == null) executingRules = new HashSet<>();
		executionDepth++;
		boolean fired = false;
		try {
			updateEngine(context);
			syncPhase(context.hero);
			beforeVocabularyEvent(context);
			if (context.event == RuleEvent.ON_TURN_START) for (RuleDefinition rule : rules) rule.tick();
			ArrayList<RuleDefinition> snapshot = new ArrayList<>(rules);
			Collections.sort(snapshot, new Comparator<RuleDefinition>() {
				@Override
				public int compare(RuleDefinition a, RuleDefinition b) {
					int priorityOrder = Integer.compare(b.priority, a.priority);
					return priorityOrder != 0 ? priorityOrder : Integer.compare(a.runtimeOrder(), b.runtimeOrder());
				}
			});
			for (RuleDefinition rule : snapshot) {
				if (onlyRuleId != null && !onlyRuleId.equals(rule.id)) continue;
				if (executingRules.contains(rule.runtimeOrder())) continue;
				executingRules.add(rule.runtimeOrder());
				try {
					if (rule.execute(this, context)) fired = true;
				} finally {
					executingRules.remove(rule.runtimeOrder());
				}
			}
			afterVocabularyEvent(context);
		} finally {
			executionDepth--;
		}
		return fired;
	}

	/** Emits a semantic fact and applies saved bridges without replacing the original event. */
	void emitSemantic(RuleContext parent, RuleSemanticTag tag, Char moved, int from, int to) {
		RuleTrace.record("TAG", tag.name() + " cause=#" + parent.eventId()
				+ " sourceRule=" + parent.sourceRuleId() + " from=" + from + " to=" + to);
		applyBacklash(parent, tag);
		if (tag == RuleSemanticTag.FORCED_MOVEMENT && moved != null && moved != parent.hero
				&& hasVocabulary(CoreRuleVocabulary.KINETIC_MARK)) {
			RuleMark mark = applyInternalMark(moved, RuleMark.Type.HUNTED, parent.hero, 1, 8, false);
			if (mark != null) RuleTrace.record("TRAIT", "KINETIC_MARK target=#" + moved.id()
					+ " stacks=" + mark.stacks());
		}
		if (tag == RuleSemanticTag.FORCED_MOVEMENT && isHazardCell(to)) {
			changeTraitResource(parent.hero, CoreRuleVocabulary.HAZARD_FEEDBACK, 2, true);
		}
		for (RuleEventBridge bridge : new ArrayList<>(bridges)) {
			if (!bridge.matches(tag)) continue;
			RuleContext child = parent.child(bridge.targetEvent);
			if (child == null) {
				RuleTrace.record("BRIDGE_GUARD", tag.name() + " -> " + bridge.targetEvent.name()
						+ " already in chain root=#" + parent.rootEventId());
				continue;
			}
			if (child.source == null) child.source = parent.hero;
			child.target = moved;
			child.cell = to;
			child.amount = Dungeon.level == null || from < 0 || to < 0
					? Math.abs(to - from) : Dungeon.level.distance(from, to);
			RuleTrace.record("BRIDGE", tag.name() + " cause=#" + parent.eventId()
					+ " -> " + bridge.targetEvent.name() + " original=" + child.originalEvent().name());
			dispatch(child);
		}
	}

	public boolean addBridge(RuleEventBridge bridge) {
		if (bridge == null || bridge.sourceTag == null || bridge.targetEvent == null) return false;
		for (RuleEventBridge existing : bridges) {
			if (existing.sourceTag == bridge.sourceTag && existing.targetEvent == bridge.targetEvent) return false;
		}
		bridges.add(bridge);
		return true;
	}

	public ArrayList<RuleEventBridge> bridges() { return new ArrayList<>(bridges); }

	private void beforeVocabularyEvent(RuleContext context) {
		if (hasVocabulary(CoreRuleVocabulary.HUNT_MARK) && context.event == RuleEvent.ON_HIT
				&& context.target != null && context.target != context.hero) {
			RuleMark mark = applyInternalMark(context.target, RuleMark.Type.HUNTED, context.hero, 1, 8, false);
			if (mark != null) RuleTrace.record("VOCAB", "HUNT_MARK target=#" + context.target.id()
					+ " stacks=" + mark.stacks());
		}
		if (hasVocabulary(CoreRuleVocabulary.PROPAGATION) && context.event == RuleEvent.ON_KILL
				&& context.target != null) propagateHuntMark(context);
	}

	private void afterVocabularyEvent(RuleContext context) {
		if (!hasVocabulary(CoreRuleVocabulary.ACCUMULATION)
				|| !CoreRuleVocabulary.supportsAccumulation(context.event)) return;
		RuleDefinition reaction = reactionTechnique();
		if (reaction == null || reaction.trigger.event != context.event
				|| RuleMark.has(context.hero, RuleMark.Type.CHARGED, 1)) return;
		RuleMark mark = applyInternalMark(context.hero, RuleMark.Type.ACCUMULATION, context.hero, 1, 12, false);
		if (mark == null) return;
		RuleTrace.record("VOCAB", "ACCUMULATION event=" + context.event + " stacks=" + mark.stacks());
		if (mark.stacks() >= 3) {
			RuleMark.remove(context.hero, RuleMark.Type.ACCUMULATION);
			setInternalMark(context.hero, RuleMark.Type.CHARGED, context.hero, 1, 20);
			RuleTrace.record("VOCAB", "ACCUMULATION ready root=#" + context.rootEventId());
		}
	}

	private void propagateHuntMark(RuleContext context) {
		RuleMark sourceMark = RuleMark.get(context.target, RuleMark.Type.HUNTED);
		if (sourceMark == null || sourceMark.transferDepth() >= 1 || Dungeon.level == null) return;
		Char nearest = null;
		int bestDistance = Integer.MAX_VALUE;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob : Dungeon.level.mobs) {
			if (mob == context.target || !mob.isAlive()) continue;
			int distance = Dungeon.level.distance(context.target.pos, mob.pos);
			if (distance < bestDistance) {
				bestDistance = distance;
				nearest = mob;
			}
		}
		if (nearest == null) return;
		int transferredStacks = Math.max(1, Math.min(2, sourceMark.stacks()));
		RuleHooks.beginRuleStatusApplication();
		RuleMark transferred;
		try {
			transferred = RuleMark.transfer(sourceMark, nearest, transferredStacks, 8, 1);
		} finally {
			RuleHooks.endRuleStatusApplication();
		}
		if (transferred != null) {
			RuleTrace.record("VOCAB", "PROPAGATION from=#" + context.target.id() + " to=#"
					+ nearest.id() + " stacks=" + transferred.stacks() + " depth=" + transferred.transferDepth());
		}
	}

	private RuleMark applyInternalMark(Char target, RuleMark.Type type, Char source,
			int stacks, int duration, boolean set) {
		RuleHooks.beginRuleStatusApplication();
		try {
			return set ? RuleMark.set(target, type, source, stacks, duration)
					: RuleMark.apply(target, type, source, stacks, duration);
		} finally {
			RuleHooks.endRuleStatusApplication();
		}
	}

	private RuleMark setInternalMark(Char target, RuleMark.Type type, Char source, int stacks, int duration) {
		return applyInternalMark(target, type, source, stacks, duration, true);
	}

	private void syncPhase(Hero hero) {
		if (!hasVocabulary(CoreRuleVocabulary.PHASE_SHIFT) || hero == null) return;
		if (hero.HP * 100 <= hero.HT * 30) {
			if (RuleMark.get(hero, RuleMark.Type.LOW_PHASE) == null) {
				RuleTrace.record("VOCAB", "PHASE_SHIFT enter hp=" + hero.HP + "/" + hero.HT);
			}
			setInternalMark(hero, RuleMark.Type.LOW_PHASE, hero, 1, 2);
		} else if (RuleMark.remove(hero, RuleMark.Type.LOW_PHASE)) {
			RuleTrace.record("VOCAB", "PHASE_SHIFT leave hp=" + hero.HP + "/" + hero.HT);
		}
	}

	RuleModifier shapeModifier(RuleEffect effect, RuleModifier base, Hero hero) {
		RuleModifier result = base == null ? new RuleModifier() : base.copy();
		if (hasVocabulary(CoreRuleVocabulary.PHASE_SHIFT) && hero != null
				&& RuleMark.has(hero, RuleMark.Type.LOW_PHASE, 1)
				&& result.type == RuleModifier.Type.NONE
				&& effect != null && effect.type != RuleEffect.Type.TELEPORT
				&& effect.type != RuleEffect.Type.SWAP_POSITION) {
			result = new RuleModifier(RuleModifier.Type.AREA);
			RuleTrace.record("VOCAB", "PHASE_SHIFT modifier=AREA");
		}
		return result;
	}

	RuleModifier empowerModifier(RuleEffect effect, RuleModifier base, Hero hero) {
		RuleModifier result = base == null ? new RuleModifier() : base.copy();
		if (hasVocabulary(CoreRuleVocabulary.ACCUMULATION) && effect != null && effect.supportsPowerScaling()
				&& RuleMark.has(hero, RuleMark.Type.CHARGED, 1)) {
			result.powerMultiplier *= 1.5f;
		}
		return result;
	}

	boolean consumeEmpowerment(RuleEffect effect, Hero hero) {
		if (!hasVocabulary(CoreRuleVocabulary.ACCUMULATION) || effect == null || !effect.supportsPowerScaling()) {
			return false;
		}
		boolean consumed = RuleMark.consume(hero, RuleMark.Type.CHARGED, 1);
		if (consumed) RuleTrace.record("VOCAB", "ACCUMULATION consumed effect=" + effect.semanticId());
		return consumed;
	}

	boolean compensateFailure(RuleContext context) {
		if (!hasVocabulary(CoreRuleVocabulary.COMPENSATION) || context == null || context.hero == null
				|| RuleMark.has(context.hero, RuleMark.Type.COMPENSATION_LOCK, 1)) return false;
		setInternalMark(context.hero, RuleMark.Type.COMPENSATION_LOCK, context.hero, 1, 5);
		boolean applied = new RuleEffect(RuleEffect.Type.SHIELD, 1)
				.apply(context, context.hero, context.hero.pos, new RuleModifier());
		RuleTrace.record("VOCAB", "COMPENSATION applied=" + applied + " lock=5");
		return applied;
	}

	void onPrimaryEffectSuccess(RuleContext context, Char target, int cell) {
		if (!hasVocabulary(CoreRuleVocabulary.HUNT_MARK) || target == null || target == context.hero
				|| !RuleMark.has(target, RuleMark.Type.HUNTED, 3)) return;
		if (!RuleMark.consume(target, RuleMark.Type.HUNTED, 3)) return;
		new RuleEffect(RuleEffect.Type.SLOW, 1).apply(context, target, cell, new RuleModifier());
		RuleTrace.record("VOCAB", "HUNT_MARK consumed target=#" + target.id() + " payoff=SLOW");
	}

	void scheduleEcho(RuleDefinition rule, RuleContext context, Char target, int cell) {
		if (!hasVocabulary(CoreRuleVocabulary.ECHO) || rule == null || context == null
				|| rule.trigger.event != RuleEvent.ACTIVE || rule.delayTurns() > 0) return;
		RuleEffect echoed = rule.effect.scaled(0.5f);
		EffectSpec echoedSpec = rule.effectSpec == null ? null : rule.effectSpec.copy();
		if (echoedSpec != null) echoedSpec.power = Math.max(1, Math.round(echoedSpec.power * 0.5f));
		RuleDelayedPayload payload = echoedSpec == null
				? new RuleDelayedPayload(nextPayloadId++, context, echoed, target, cell,
						new RuleModifier(), 3, RuleDelayedPayload.Kind.ECHO)
				: new RuleDelayedPayload(nextPayloadId++, context, echoedSpec, echoed, target, cell,
						new RuleModifier(), 3, RuleDelayedPayload.Kind.ECHO);
		delayedPayloads.add(payload);
		payload.activate(this);
		RuleTrace.record("ECHO", "schedule payload=#" + payload.payloadId() + " turns=3 cause=#"
				+ context.eventId() + " effect=" + echoed.semanticId());
	}

	/** Cross-system Trait hooks run only after a real EffectSpec has reported success. */
	public void onEffectSpecSuccess(EffectSpec spec, RuleContext context, Char target) {
		if (spec == null || context == null || context.hero == null) return;
		RuleDefinition sourceRule = rule(context.sourceRuleId());
		if (hasVocabulary(CoreRuleVocabulary.PIERCING_MARK) && target != null && target != context.hero
				&& sourceRule != null && (sourceRule.delivery == SkillDelivery.PROJECTILE
				|| sourceRule.delivery == SkillDelivery.TRACE_BEAM)) {
			RuleMark mark = applyInternalMark(target, RuleMark.Type.HUNTED, context.hero, 1, 8, false);
			if (mark != null) RuleTrace.record("TRAIT", "PIERCING_MARK target=#" + target.id());
		}
		if (hasVocabulary(CoreRuleVocabulary.STATUS_CHAIN) && spec.family == EffectFamily.STATUS
				&& target != null && target != context.hero && RuleMark.has(target, RuleMark.Type.HUNTED, 1)
				&& RuleMark.consume(target, RuleMark.Type.HUNTED, 1)) {
			RuleHooks.beginRuleStatusApplication();
			try { Buff.prolong(target, Vulnerable.class, 3f); }
			finally { RuleHooks.endRuleStatusApplication(); }
			RuleTrace.record("TRAIT", "STATUS_CHAIN target=#" + target.id() + " vulnerable=3");
		}
		if (hasVocabulary(CoreRuleVocabulary.MODE_GUARD) && spec.operation == EffectSpec.Operation.TRANSFORM_MODE) {
			TraitSpec trait = trait(CoreRuleVocabulary.MODE_GUARD);
			if (trait != null && (trait.stateId.isEmpty() || trait.stateId.equals(spec.stateId))) {
				RuleTemporaryHP temp = Buff.affect(context.hero, RuleTemporaryHP.class);
				temp.grant(3, 4, 0);
				RuleTrace.record("TRAIT", "MODE_GUARD mode=" + spec.stateId + " tempHp=3");
			}
		}
		if (spec.family == EffectFamily.TRANSFER_COPY) {
			changeTraitResource(context.hero, CoreRuleVocabulary.TRANSFER_FEEDBACK, 1, true);
		}
		if (context.source instanceof RuleOwnedEntity) {
			RuleOwnedEntity entity = (RuleOwnedEntity)context.source;
			if (entity.kind() == RuleOwnedEntity.Kind.DEVICE || entity.kind() == RuleOwnedEntity.Kind.FIELD) {
				changeTraitResource(context.hero, CoreRuleVocabulary.CARRIER_RESOURCE_FEEDBACK, 1, true);
			}
		}
	}

	/** Called after a real owned Actor completes a normal attack. */
	public void onOwnedEntityAction(Hero hero, Char source, Char target) {
		if (hero == null || source == null || target == null || !RuleOwnership.isOwnedBy(source, hero)) return;
		changeTraitResource(hero, CoreRuleVocabulary.OWNED_RESOURCE_FEEDBACK, 1, true);
		if (hasLaw(ClassLaw.OWNED_ACTIONS_COUNT_AS_YOURS)) {
			RuleContext context = new RuleContext(RuleEvent.ON_HIT, hero);
			context.source = source;
			context.target = target;
			context.cell = target.pos;
			RuleTrace.record("LAW", "OWNED_ACTIONS_COUNT_AS_YOURS source=#" + source.id()
					+ " target=#" + target.id());
			dispatch(context);
		}
	}

	public void onCarrierTriggered(Hero hero) {
		changeTraitResource(hero, CoreRuleVocabulary.CARRIER_RESOURCE_FEEDBACK, 1, true);
	}

	/**
	 * Called once when a real owned Actor/device/field dies or reaches its lifetime. This is a
	 * resource-economy component rather than a new RuleEvent: removing a creation may be recycled,
	 * but must not masquerade as a hero kill or recursively trigger kill rules.
	 */
	public void onOwnedEntityRemoved(Hero hero, Char source) {
		if (hero == null || source == null || !RuleOwnership.isOwnedBy(source, hero)) return;
		RuleContext context = new RuleContext(RuleEvent.ON_TURN_START, hero); context.source = source;
		if (primaryResourceSpec != null) applyComponentFlows(primaryResourceSpec.id, null, context,
				ResourceFlowSpec.Trigger.OWNED_ENTITY_REMOVED);
		for (RuleResourceState state : additionalResources) applyComponentFlows(state.id, state, context,
				ResourceFlowSpec.Trigger.OWNED_ENTITY_REMOVED);
		RuleTrace.record("OWNED_REMOVED", "source=#" + source.id() + " owner=#" + hero.id());
	}

	private RuleDefinition rule(String id) {
		if (id == null || id.isEmpty()) return null;
		for (RuleDefinition rule : rules) if (id.equals(rule.id)) return rule;
		return null;
	}

	private static boolean isHazardCell(int cell) {
		if (Dungeon.level == null || cell < 0 || cell >= Dungeon.level.length()) return false;
		return Dungeon.level.traps.get(cell) != null || Dungeon.level.water[cell];
	}

	private void applyBacklash(RuleContext parent, RuleSemanticTag tag) {
		if (parent == null || parent.hero == null || parent.sourceRuleId() == null) return;
		if (tag == RuleSemanticTag.TRANSLOCATION && hasVocabulary(CoreRuleVocabulary.TRANSLOCATION_BACKLASH)) {
			Slow existing = parent.hero.buff(Slow.class);
			RuleHooks.beginRuleStatusApplication();
			Slow slow;
			try { slow = Buff.prolong(parent.hero, Slow.class, 3f); }
			finally { RuleHooks.endRuleStatusApplication(); }
			if (existing == null) RuleHooks.onRuleStatusApplied(parent, parent.hero, slow);
			RuleTrace.record("VOCAB", "TRANSLOCATION_BACKLASH slow=3");
		} else if (tag == RuleSemanticTag.PROTECTION && hasVocabulary(CoreRuleVocabulary.SHIELD_BACKLASH)) {
			Vulnerable existing = parent.hero.buff(Vulnerable.class);
			RuleHooks.beginRuleStatusApplication();
			Vulnerable vulnerable;
			try { vulnerable = Buff.prolong(parent.hero, Vulnerable.class, 3f); }
			finally { RuleHooks.endRuleStatusApplication(); }
			if (existing == null) RuleHooks.onRuleStatusApplied(parent, parent.hero, vulnerable);
			RuleTrace.record("VOCAB", "SHIELD_BACKLASH vulnerable=3");
		}
	}

	boolean scheduleDelayed(RuleContext context, RuleEffect effect, Char target, int cell,
			RuleModifier modifier, int turns) {
		if (context == null || effect == null || modifier == null || turns <= 0) return false;
		RuleDelayedPayload payload = new RuleDelayedPayload(nextPayloadId++, context, effect,
				target, cell, modifier, turns);
		delayedPayloads.add(payload);
		payload.activate(this);
		RuleTrace.record("DELAY", "schedule payload=#" + payload.payloadId() + " turns=" + turns
				+ " cause=#" + context.eventId() + " effect=" + effect.semanticId());
		return true;
	}

	boolean scheduleDelayed(RuleContext context, EffectSpec effectSpec, RuleEffect legacyEffect,
			Char target, int cell, RuleModifier modifier, int turns) {
		if (effectSpec == null) return scheduleDelayed(context, legacyEffect, target, cell, modifier, turns);
		RuleDelayedPayload payload = new RuleDelayedPayload(nextPayloadId++, context, effectSpec,
				legacyEffect, target, cell, modifier, turns, RuleDelayedPayload.Kind.NORMAL);
		delayedPayloads.add(payload);
		payload.activate(this);
		RuleTrace.record("DELAY", "schedule payload=#" + payload.payloadId() + " turns=" + turns
				+ " cause=#" + context.eventId() + " effect=" + effectSpec.operation);
		return true;
	}

	void completeDelayed(RuleDelayedPayload payload) {
		delayedPayloads.remove(payload);
		Actor.remove(payload);
	}

	private void activateDelayedPayloads() {
		for (RuleDelayedPayload payload : new ArrayList<>(delayedPayloads)) payload.activate(this);
	}

	public int pendingDelayedCount() { return delayedPayloads.size(); }
	public ArrayList<RuleDelayedPayload> delayedPayloads() { return new ArrayList<>(delayedPayloads); }

	private void updateEngine(RuleContext context) {
		if (classBuild != null) {
			updateComponentResources(context);
			updateSharedResourceRules(context);
			return;
		}
		if (context.event == RuleEvent.ON_DAMAGED || context.event == RuleEvent.ON_HIT) turnsSinceCombat = 0;

		if (engine == ResourceEngine.RAGE) {
			if (context.event == RuleEvent.ON_DAMAGED) {
				changeResource(context.hero, Math.max(1, Math.min(3, 1 + context.amount / 3)), true);
			} else if (context.event == RuleEvent.ON_HIT && context.melee) {
				changeResource(context.hero, 1, true);
			}
		} else if (engine == ResourceEngine.MOMENTUM) {
			if (context.event == RuleEvent.ON_MOVE) {
				turnsSinceMove = 0;
				consecutiveMoves++;
				changeResource(context.hero, Math.min(3, 1 + consecutiveMoves / 3), true);
			} else if (context.event == RuleEvent.ON_WAIT) {
				consecutiveMoves = 0;
				setResourceForDebug(context.hero, 0);
			}
		} else if (engine == ResourceEngine.FOCUS) {
			if (context.event == RuleEvent.ON_WAIT) {
				focusSafeTurns++;
				changeResource(context.hero, 2, true);
			} else if (context.event == RuleEvent.ON_DAMAGED) {
				focusSafeTurns = 0;
				changeResource(context.hero, -Math.max(4, resource), true);
			}
		} else if (engine == ResourceEngine.AFFLICTION
				&& context.event == RuleEvent.ON_STATUS_APPLIED && isNegativeStatus(context)) {
			changeResource(context.hero, 2, true);
		}

		updateSharedResourceRules(context);

		if (context.event == RuleEvent.ON_TURN_START) {
			turnCounter++;
			turnsSinceCombat++;
			turnsSinceMove++;
			if (engine == ResourceEngine.MANA && turnCounter % 3 == 0) {
				changeResource(context.hero, 1, false);
			} else if (engine == ResourceEngine.RAGE && turnsSinceCombat > 5 && turnCounter % 2 == 0) {
				changeResource(context.hero, -1, false);
			} else if (engine == ResourceEngine.MOMENTUM && turnsSinceMove > 1) {
				consecutiveMoves = 0;
				changeResource(context.hero, -2, false);
			} else if (engine == ResourceEngine.FOCUS) {
				focusSafeTurns++;
				if (focusSafeTurns >= 2 && focusSafeTurns % 2 == 0) changeResource(context.hero, 1, false);
			}
		}

		for (RuleResourceState state : additionalResources) updateAdditionalEngine(state, context);
	}

	private void updateSharedResourceRules(RuleContext context) {
		if (context.event == RuleEvent.ON_MOVE && hasVocabulary(CoreRuleVocabulary.MOBILE_CHARGE)) {
			traitMoveCounter++;
			if (traitMoveCounter >= 3) {
				traitMoveCounter = 0;
				changeTraitResource(context.hero, CoreRuleVocabulary.MOBILE_CHARGE, 1, true);
			}
		}

		if (engine != null && context.event == RuleEvent.ON_STATUS_APPLIED && isNegativeStatus(context)
				&& hasLaw(ClassLaw.STATUS_ABSORPTION)) {
			changeResource(context.hero, 1, true);
		}
		if (context.event == RuleEvent.ON_STATUS_APPLIED && isNegativeStatus(context)) {
			changeTraitResource(context.hero, CoreRuleVocabulary.STATUS_FEEDBACK, 2, true);
		}

		if (context.event == RuleEvent.ON_KILL && hasLaw(ClassLaw.KILL_ACCELERATES_RULES)) {
			for (RuleDefinition rule : rules) rule.accelerate(2f);
			if (engine != null) changeResource(context.hero, 1, true);
		}
		if (context.event == RuleEvent.ON_KILL && hasVocabulary(CoreRuleVocabulary.KILL_TEMPO)) {
			for (RuleDefinition rule : rules) rule.accelerate(2f);
			changeTraitResource(context.hero, CoreRuleVocabulary.KILL_TEMPO, 1, true);
			RuleTrace.record("TRAIT", "KILL_TEMPO accelerate=2");
		}

		if (context.event == RuleEvent.ON_WAIT && hasRestriction(Restriction.WAIT_CLEARS_RESOURCE)) {
			setResourceForDebug(context.hero, 0);
		}

	}

	private void updateAdditionalEngine(RuleResourceState state, RuleContext context) {
		if (context.event == RuleEvent.ON_DAMAGED || context.event == RuleEvent.ON_HIT) state.turnsSinceCombat = 0;
		if (state.engine == ResourceEngine.RAGE) {
			if (context.event == RuleEvent.ON_DAMAGED) changeAdditionalResource(context.hero, state,
					Math.max(1, Math.min(3, 1 + context.amount / 3)), true);
			else if (context.event == RuleEvent.ON_HIT && context.melee) changeAdditionalResource(context.hero, state, 1, true);
		} else if (state.engine == ResourceEngine.MOMENTUM) {
			if (context.event == RuleEvent.ON_MOVE) {
				state.turnsSinceMove = 0;
				state.consecutiveMoves++;
				changeAdditionalResource(context.hero, state, Math.min(3, 1 + state.consecutiveMoves / 3), true);
			} else if (context.event == RuleEvent.ON_WAIT) {
				state.consecutiveMoves = 0;
				state.value = 0;
			}
		} else if (state.engine == ResourceEngine.FOCUS) {
			if (context.event == RuleEvent.ON_WAIT) {
				state.focusSafeTurns++;
				changeAdditionalResource(context.hero, state, 2, true);
			} else if (context.event == RuleEvent.ON_DAMAGED) {
				state.focusSafeTurns = 0;
				changeAdditionalResource(context.hero, state, -Math.max(4, state.value), true);
			}
		} else if (state.engine == ResourceEngine.AFFLICTION
				&& context.event == RuleEvent.ON_STATUS_APPLIED && isNegativeStatus(context)) {
			changeAdditionalResource(context.hero, state, 2, true);
		}
		if (context.event == RuleEvent.ON_STATUS_APPLIED && isNegativeStatus(context)
				&& hasLaw(ClassLaw.STATUS_ABSORPTION)) changeAdditionalResource(context.hero, state, 1, true);
		if (context.event == RuleEvent.ON_KILL && hasLaw(ClassLaw.KILL_ACCELERATES_RULES)) {
			changeAdditionalResource(context.hero, state, 1, true);
		}
		if (context.event == RuleEvent.ON_WAIT && hasRestriction(Restriction.WAIT_CLEARS_RESOURCE)) state.value = 0;
		if (context.event == RuleEvent.ON_TURN_START) {
			state.turnCounter++;
			state.turnsSinceCombat++;
			state.turnsSinceMove++;
			if (state.engine == ResourceEngine.MANA && state.turnCounter % 3 == 0) {
				changeAdditionalResource(context.hero, state, 1, false);
			} else if (state.engine == ResourceEngine.RAGE && state.turnsSinceCombat > 5 && state.turnCounter % 2 == 0) {
				changeAdditionalResource(context.hero, state, -1, false);
			} else if (state.engine == ResourceEngine.MOMENTUM && state.turnsSinceMove > 1) {
				state.consecutiveMoves = 0;
				changeAdditionalResource(context.hero, state, -2, false);
			} else if (state.engine == ResourceEngine.FOCUS) {
				state.focusSafeTurns++;
				if (state.focusSafeTurns >= 2 && state.focusSafeTurns % 2 == 0) {
					changeAdditionalResource(context.hero, state, 1, false);
				}
			}
		}
	}

	private void updateComponentResources(RuleContext context) {
		if (context == null) return;
		advanceResourceClock(context, null);
		for (RuleResourceState state : additionalResources) advanceResourceClock(context, state);
		if (primaryResourceSpec != null) applyComponentFlows(primaryResourceSpec.id, null, context, null);
		for (RuleResourceState state : additionalResources) applyComponentFlows(state.id, state, context, null);
	}

	private void advanceResourceClock(RuleContext context, RuleResourceState state) {
		if (context.event == RuleEvent.ON_DAMAGED || context.event == RuleEvent.ON_HIT) {
			if (state == null) turnsSinceCombat = 0; else state.turnsSinceCombat = 0;
		}
		if (context.event == RuleEvent.ON_MOVE) {
			if (state == null) { turnsSinceMove = 0; consecutiveMoves++; }
			else { state.turnsSinceMove = 0; state.consecutiveMoves++; }
		}
		if (context.event == RuleEvent.ON_TURN_START) {
			if (state == null) { turnCounter++; turnsSinceCombat++; turnsSinceMove++; }
			else { state.turnCounter++; state.turnsSinceCombat++; state.turnsSinceMove++; }
		}
	}

	private ArrayList<ClassGameplayComponentSpec> resourceFlowComponents(String resourceId) {
		ArrayList<ClassGameplayComponentSpec> result = new ArrayList<>();
		if (classBuild == null) return result;
		for (ClassGameplayComponentSpec component : classBuild.gameplayComponents) if (component != null
				&& component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW
				&& resourceId != null && resourceId.equals(component.resourceId)) result.add(component);
		return result;
	}

	private void applyComponentFlows(String resourceId, RuleResourceState state, RuleContext context,
			ResourceFlowSpec.Trigger explicitTrigger) {
		ArrayList<ClassGameplayComponentSpec> components = resourceFlowComponents(resourceId);
		for (int i = 0; i < components.size(); i++) {
			ClassGameplayComponentSpec component = components.get(i);
			ResourceFlowSpec flow = component.asResourceFlow();
			boolean matches = explicitTrigger == null ? flowMatches(flow, context, state, i)
					: flow.trigger == explicitTrigger && flowIntervalReady(state, i, flow.interval);
			if (matches) applyResourceFlow(component, state, context.hero,
					context.event != RuleEvent.ON_TURN_START || explicitTrigger != null);
		}
	}

	private void applyResourceFlow(ClassGameplayComponentSpec component, RuleResourceState state,
			Hero hero, boolean feedback) {
		ResourceFlowSpec flow = component.asResourceFlow();
		// Stopped-moving is a class-level continuity break, not merely a numerical loss.
		// Keep the shared movement state truthful for Conditions, traces, and save/load even
		// when a preset has been expanded into ordinary editable components.
		if (flow.trigger == ResourceFlowSpec.Trigger.STOPPED_MOVING) {
			if (state == null) consecutiveMoves = 0; else state.consecutiveMoves = 0;
		}
		if (flow.operation == ResourceFlowSpec.Operation.CONVERT) {
			int available = resourceValue(component.resourceId, null);
			int targetBefore = resourceValue(component.targetResourceId, null);
			int targetMax = resourceMax(component.targetResourceId, null);
			if (available < component.amount || targetBefore >= targetMax) return;
			int gained = Math.min(component.targetAmount, targetMax - targetBefore);
			if (gained <= 0) return;
			changeResource(hero, component.resourceId, null, -component.amount, feedback);
			changeResource(hero, component.targetResourceId, null, gained, feedback);
			RuleTrace.record("RESOURCE_CONVERT", component.id + " " + component.resourceId + " -"
					+ component.amount + " " + component.targetResourceId + " +" + gained);
			return;
		}
		int delta = flow.operation == ResourceFlowSpec.Operation.CLEAR
				? -(state == null ? resource : state.value)
				: (flow.operation == ResourceFlowSpec.Operation.GAIN ? flow.amount : -flow.amount);
		if (delta == 0) return;
		changeResource(hero, component.resourceId, null, delta, feedback);
		RuleTrace.record("RESOURCE_FLOW", component.id + " resource=" + component.resourceId + " "
				+ flow.trigger + " " + flow.operation + " " + Math.abs(delta));
	}

	private boolean flowMatches(ResourceFlowSpec flow, RuleContext context, RuleResourceState state, int flowIndex) {
		if (flow == null || context == null || flow.meleeOnly && !context.melee) return false;
		int turns = state == null ? turnCounter : state.turnCounter;
		int combat = state == null ? turnsSinceCombat : state.turnsSinceCombat;
		int move = state == null ? turnsSinceMove : state.turnsSinceMove;
		switch (flow.trigger) {
			case TURN: return context.event == RuleEvent.ON_TURN_START && turns >= flow.delay
					&& (turns - flow.delay) % flow.interval == 0;
			case HIT: return context.event == RuleEvent.ON_HIT && flowIntervalReady(state, flowIndex, flow.interval);
			case DAMAGED: return context.event == RuleEvent.ON_DAMAGED && flowIntervalReady(state, flowIndex, flow.interval);
			case MOVE: return context.event == RuleEvent.ON_MOVE && flowIntervalReady(state, flowIndex, flow.interval);
			case WAIT: return context.event == RuleEvent.ON_WAIT && flowIntervalReady(state, flowIndex, flow.interval);
			case KILL: return context.event == RuleEvent.ON_KILL && flowIntervalReady(state, flowIndex, flow.interval);
			case NEGATIVE_STATUS: return context.event == RuleEvent.ON_STATUS_APPLIED && isNegativeStatus(context)
					&& flowIntervalReady(state, flowIndex, flow.interval);
			case OWNED_ENTITY_REMOVED: return false; // Delivered by onOwnedEntityRemoved; not a public RuleEvent.
			case OUT_OF_COMBAT: return context.event == RuleEvent.ON_TURN_START && combat > flow.delay
					&& (combat - flow.delay - 1) % flow.interval == 0;
			case STOPPED_MOVING: return context.event == RuleEvent.ON_TURN_START && move > flow.delay
					&& (move - flow.delay - 1) % flow.interval == 0;
			default: return false;
		}
	}

	private boolean flowIntervalReady(RuleResourceState state, int flowIndex, int interval) {
		int[] counters = state == null ? primaryFlowCounters : state.flowCounters;
		if (counters == null || counters.length <= flowIndex) {
			int[] grown = new int[flowIndex + 1];
			if (counters != null) System.arraycopy(counters, 0, grown, 0, counters.length);
			counters = grown;
			if (state == null) primaryFlowCounters = counters; else state.flowCounters = counters;
		}
		counters[flowIndex]++;
		return counters[flowIndex] % Math.max(1, interval) == 0;
	}

	private static boolean isNegativeStatus(RuleContext context) {
		return context.status != null && context.status.type == Buff.buffType.NEGATIVE;
	}

	public void changeResource(Hero hero, int delta, boolean feedback) {
		if (engine == null || engine == ResourceEngine.BLOOD) return;
		if (delta > 0 && suppressed(hero, classBuild != null && classBuild.primaryResource() != null
				? classBuild.primaryResource().id : engine.name().toLowerCase())) return;
		int adjusted = delta;
		boolean waterFlow = traitTargetsResource(CoreRuleVocabulary.WATER_FLOW, primaryResourceId());
		if (delta > 0 && (hasLaw(ClassLaw.WATER_AFFINITY) || waterFlow)
				&& hero != null && Dungeon.level != null && hero.pos >= 0
				&& hero.pos < Dungeon.level.water.length && Dungeon.level.water[hero.pos]) {
			adjusted++;
			if (waterFlow) RuleTrace.record("TRAIT", "WATER_FLOW resource=" + primaryResourceId()
					+ " gain=" + delta + "->" + adjusted);
		}
		int before = resource;
		int desired = resource + adjusted;
		int overflow = Math.max(0, desired - maxResource);
		int minimum = primaryResourceSpec == null ? 0 : primaryResourceSpec.minimum;
		resource = Math.max(minimum, Math.min(maxResource, desired));
		if (primaryResourceSpec != null) primaryResourceSpec.current = resource;
		if (classBuild != null && classBuild.primaryResource() != null) classBuild.primaryResource().current = resource;
		int actual = resource - before;
		if (before != resource || overflow > 0) {
			RuleTrace.record("RESOURCE", engine.name() + " " + before + "->" + resource
					+ (overflow > 0 ? " overflow=" + overflow : ""));
		}
		if (overflow > 0 && hasLaw(ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD) && hero != null) {
			Buff.affect(hero, Barrier.class).incShield(overflow);
		} else if (overflow > 0 && traitTargetsResource(CoreRuleVocabulary.OVERFLOW, primaryResourceId()) && hero != null) {
			overflowRemainder += overflow;
			int converted = overflowRemainder / 2;
			overflowRemainder %= 2;
			if (converted > 0) Buff.affect(hero, Barrier.class).incShield(converted);
			RuleTrace.record("VOCAB", "OVERFLOW input=" + overflow + " shield=" + converted
					+ " remainder=" + overflowRemainder);
		}
		BuffIndicator.refreshHero();
		if (feedback && actual != 0 && hero != null && hero.sprite != null) {
			hero.sprite.showStatus(resourceColor(), Messages.get(RuleRuntime.class, "resource_change",
					(actual > 0 ? "+" : "") + actual, primaryResourceName()));
		}
	}

	private void changeAdditionalResource(Hero hero, RuleResourceState state, int delta, boolean feedback) {
		if (state.engine == ResourceEngine.BLOOD) return;
		if (delta > 0 && suppressed(hero, state.id)) return;
		int adjusted = delta;
		if (delta > 0 && (hasLaw(ClassLaw.WATER_AFFINITY)
				|| traitTargetsResource(CoreRuleVocabulary.WATER_FLOW, state.id)) && hero != null && Dungeon.level != null
				&& hero.pos >= 0 && hero.pos < Dungeon.level.water.length && Dungeon.level.water[hero.pos]) adjusted++;
		int before = state.value;
		int desired = state.value + adjusted;
		int overflow = Math.max(0, desired - state.max);
		int minimum = state.spec == null ? 0 : state.spec.minimum;
		state.value = Math.max(minimum, Math.min(state.max, desired));
		if (state.spec != null) state.spec.current = state.value;
		if (classBuild != null && classBuild.resource(state.id) != null) classBuild.resource(state.id).current = state.value;
		if (before != state.value || overflow > 0) {
			RuleTrace.record("RESOURCE", state.engine.name() + "[" + state.id + "] " + before + "->" + state.value
					+ (overflow > 0 ? " overflow=" + overflow : ""));
		}
		if (overflow > 0 && hasLaw(ClassLaw.RESOURCE_OVERFLOW_TO_SHIELD) && hero != null) {
			Buff.affect(hero, Barrier.class).incShield(overflow);
		} else if (overflow > 0 && traitTargetsResource(CoreRuleVocabulary.OVERFLOW, state.id) && hero != null) {
			state.overflowRemainder += overflow;
			int converted = state.overflowRemainder / 2;
			state.overflowRemainder %= 2;
			if (converted > 0) Buff.affect(hero, Barrier.class).incShield(converted);
		}
		BuffIndicator.refreshHero();
		if (feedback && before != state.value && hero != null && hero.sprite != null) {
			hero.sprite.showStatus(resourceColor(state.engine), Messages.get(RuleRuntime.class, "resource_change",
					(state.value > before ? "+" : "") + (state.value - before),
					state.spec == null ? state.engine.displayName() : state.spec.displayName()));
		}
	}

	private int resourceColor() {
		return resourceColor(engine);
	}

	private static int resourceColor(ResourceEngine value) {
		if (value == null) return 0xFFFFFF;
			switch (value) {
			case MANA: return 0x4488FF;
			case RAGE: return 0xFF6644;
			case MOMENTUM: return 0x66DD88;
			case FOCUS: return 0xFFD966;
				case AFFLICTION: return 0xAA66CC;
				case MANUAL: return 0xD9A441;
				case BLOOD:
			default: return 0xAA2233;
		}
	}

	public int availableResource(Hero hero) {
		if (engine == null) return 0;
		return engine == ResourceEngine.BLOOD ? Math.max(0, hero == null ? 0 : hero.HP - 1) : resource;
	}

	public int overdrawHpCost(int amount) {
		if (!(hasVocabulary(CoreRuleVocabulary.OVERDRAW) || hasLaw(ClassLaw.RESOURCE_OVERDRAFT_USES_HP))
				|| engine == null || engine == ResourceEngine.BLOOD) return 0;
		return Math.max(0, amount - resource) * 2;
	}

	public boolean canPayResourceCost(Hero hero, int amount) {
		return canPayResourceCost(hero, amount, "", engine);
	}

	public boolean canPayResourceCost(Hero hero, int amount, String resourceId, ResourceEngine preferred) {
		if (classBuild != null && resourceId != null && !resourceId.isEmpty() && classBuild.resource(resourceId) == null) return false;
		amount = effectiveResourceCost(hero, amount, resourceId, preferred);
		RuleResourceState state = findAdditionalResource(resourceId, preferred);
		int minimum = state == null ? primaryResourceSpec == null ? 0 : primaryResourceSpec.minimum
				: state.spec == null ? 0 : state.spec.minimum;
		int available = Math.max(0, (state == null ? resource : state.value) - minimum);
		if (available >= amount) return true;
		int hpCost = overdrawHpCost(amount, available, state == null ? engine : state.engine);
		return hpCost > 0 && hero != null && hero.HP > hpCost;
	}

	public void payResourceCost(Hero hero, int amount) {
		payResourceCost(hero, amount, "", engine);
	}

	public void payResourceCost(Hero hero, int amount, String resourceId, ResourceEngine preferred) {
		if (classBuild != null && resourceId != null && !resourceId.isEmpty() && classBuild.resource(resourceId) == null) return;
		int requested = amount;
		amount = effectiveResourceCost(hero, amount, resourceId, preferred);
		if (requested != amount) RuleTrace.record("LAW", "WATER_AFFINITY cost="+requested+"->"+amount);
		RuleResourceState state = findAdditionalResource(resourceId, preferred);
		int minimum = state == null ? primaryResourceSpec == null ? 0 : primaryResourceSpec.minimum
				: state.spec == null ? 0 : state.spec.minimum;
		int available = Math.max(0, (state == null ? resource : state.value) - minimum);
		ResourceEngine selectedEngine = state == null ? engine : state.engine;
		int hpCost = overdrawHpCost(amount, available, selectedEngine);
		int paidResource = Math.min(available, Math.max(0, amount));
		if (paidResource > 0) {
			if (state == null) changeResource(hero, -paidResource, true);
			else changeAdditionalResource(hero, state, -paidResource, true);
		}
		if (hpCost > 0 && hero != null) {
			hero.HP -= hpCost;
			RuleTrace.record("VOCAB", "OVERDRAW missing=" + Math.max(0, amount - paidResource)
					+ " hp=" + hpCost + " remainingHp=" + hero.HP);
			if (hero.sprite != null) hero.sprite.showStatus(CharSprite.NEGATIVE,
					Messages.get(RuleRuntime.class, "overdraw_hp_cost", hpCost));
			syncPhase(hero);
		}
	}

	private int effectiveResourceCost(Hero hero, int amount, String resourceId, ResourceEngine preferred) {
		RuleResourceState state = findAdditionalResource(resourceId, preferred);
		String selectedId = state == null ? primaryResourceId() : state.id;
		if (amount <= 1 || !(hasLaw(ClassLaw.WATER_AFFINITY)
				|| traitTargetsResource(CoreRuleVocabulary.WATER_FLOW, selectedId)) || hero == null || Dungeon.level == null
				|| hero.pos < 0 || hero.pos >= Dungeon.level.water.length || !Dungeon.level.water[hero.pos]) return amount;
		return amount - 1;
	}

	private int overdrawHpCost(int amount, int available, ResourceEngine selectedEngine) {
		if (!(hasVocabulary(CoreRuleVocabulary.OVERDRAW) || hasLaw(ClassLaw.RESOURCE_OVERDRAFT_USES_HP)) || selectedEngine == null
				|| selectedEngine == ResourceEngine.BLOOD) return 0;
		return Math.max(0, amount - available) * 2;
	}

	private RuleResourceState findAdditionalResource(String id, ResourceEngine preferred) {
		String wanted = id == null ? "" : id;
		if (wanted.isEmpty() || classBuild != null && classBuild.primaryResource() != null
				&& wanted.equals(classBuild.primaryResource().id)
				|| preferred != null && preferred == engine) return null;
		for (RuleResourceState state : additionalResources) {
			if (wanted.equals(state.id) || wanted.isEmpty() && preferred == state.engine) return state;
		}
		return null;
	}

	private String primaryResourceId() {
		return classBuild != null && classBuild.primaryResource() != null
				? classBuild.primaryResource().id : engine == null ? "" : engine.name().toLowerCase();
	}

	private TraitSpec trait(CoreRuleVocabulary type) {
		for (TraitSpec spec : traitSpecs) if (spec.type == type) return spec;
		return null;
	}

	private boolean traitTargetsResource(CoreRuleVocabulary type, String resourceId) {
		TraitSpec spec = trait(type);
		if (spec == null) return false;
		return spec.resourceId == null || spec.resourceId.isEmpty() || spec.resourceId.equals(resourceId);
	}

	private int changeTraitResource(Hero hero, CoreRuleVocabulary type, int amount, boolean feedback) {
		TraitSpec spec = trait(type);
		if (spec == null || amount == 0) return 0;
		String id = spec.resourceId == null || spec.resourceId.isEmpty() ? primaryResourceId() : spec.resourceId;
		if (id.isEmpty()) return 0;
		int changed = changeResource(hero, id, null, amount, feedback);
		if (changed != 0) RuleTrace.record("TRAIT", type.name() + " resource=" + id + " delta=" + changed);
		return changed;
	}

	public boolean canPayHpCost(Hero hero, int amount) {
		if (hero == null || amount < 0) return false;
		int temporary = 0;
		RuleTemporaryHP temp = hero.buff(RuleTemporaryHP.class);
		if (hasVocabulary(CoreRuleVocabulary.TEMP_HP_PAYMENT) && temp != null) temporary = temp.shielding();
		return hero.HP + temporary > amount;
	}

	/** Returns how much real HP was paid after an optional Temporary-HP Trait contribution. */
	public int payHpCost(Hero hero, int amount) {
		if (hero == null || amount <= 0) return 0;
		int remaining = amount;
		if (hasVocabulary(CoreRuleVocabulary.TEMP_HP_PAYMENT)) {
			RuleTemporaryHP temp = hero.buff(RuleTemporaryHP.class);
			if (temp != null) {
				int absorbed = temp.absorbCost(remaining);
				remaining -= absorbed;
				if (absorbed > 0) RuleTrace.record("TRAIT", "TEMP_HP_PAYMENT temporary=" + absorbed);
			}
		}
		hero.HP -= remaining;
		return remaining;
	}

	public int resourceValue(String id, ResourceEngine preferred) {
		if (id != null && !id.isEmpty()) {
			if (id.equals(primaryResourceId())) return resource;
			for (RuleResourceState candidate : additionalResources) if (id.equals(candidate.id)) return candidate.value;
			return 0;
		}
		RuleResourceState state = findAdditionalResource(id, preferred);
		return state == null ? resource : state.value;
	}

	public int resourceMax(String id, ResourceEngine preferred) {
		if (id != null && !id.isEmpty()) {
			if (id.equals(primaryResourceId())) return maxResource;
			for (RuleResourceState candidate : additionalResources) if (id.equals(candidate.id)) return candidate.max;
			return 0;
		}
		RuleResourceState state = findAdditionalResource(id, preferred);
		return state == null ? maxResource : state.max;
	}

	/** Generic family operation shared by gain, drain, convert, reserve, and transfer. */
	public int changeResource(Hero hero, String id, ResourceEngine preferred, int delta, boolean feedback) {
		if (id != null && !id.isEmpty() && classBuild != null && classBuild.resource(id) == null) return 0;
		int before = resourceValue(id, preferred);
		RuleResourceState state = findAdditionalResource(id, preferred);
		if (state == null) changeResource(hero, delta, feedback);
		else changeAdditionalResource(hero, state, delta, feedback);
		return resourceValue(id, preferred) - before;
	}

	private static boolean suppressed(Hero hero, String id) {
		if (hero == null) return false;
		for (RuleResourceSuppression value : hero.buffs(RuleResourceSuppression.class)) {
			if (value.suppresses(id)) return true;
		}
		return false;
	}

	public void setResourceForDebug(Hero hero, int value) {
		if (engine != null && engine != ResourceEngine.BLOOD) {
			resource = Math.max(primaryResourceSpec == null ? 0 : primaryResourceSpec.minimum, Math.min(maxResource, value));
			if (primaryResourceSpec != null) primaryResourceSpec.current = resource;
			if (classBuild != null && classBuild.primaryResource() != null) classBuild.primaryResource().current = resource;
			BuffIndicator.refreshHero();
		}
	}

	public void setResourceForDebug(Hero hero, String id, ResourceEngine preferred, int value) {
		RuleResourceState state = findAdditionalResource(id, preferred);
		if (state == null) setResourceForDebug(hero, value);
		else if (state.engine != ResourceEngine.BLOOD) {
			state.value = Math.max(state.spec == null ? 0 : state.spec.minimum, Math.min(state.max, value));
			if (state.spec != null) state.spec.current = state.value;
			if (classBuild != null && classBuild.resource(state.id) != null) classBuild.resource(state.id).current = state.value;
		}
		BuffIndicator.refreshHero();
	}

	public void setEngineForDebug(Hero hero, ResourceEngine newEngine) {
		engine = newEngine;
		primaryResourceSpec = new ResourceSpec(newEngine);
		maxResource = primaryResourceSpec.capacity;
		resource = primaryResourceSpec.initialValue;
		consecutiveMoves = focusSafeTurns = turnsSinceMove = overflowRemainder = 0;
		BuffIndicator.refreshHero();
	}

	public void setLawForDebug(ClassLaw newLaw) {
		laws.clear();
		if (newLaw != null) laws.add(newLaw);
		law = newLaw;
		installLawBridges();
	}

	public void setLawsForDebug(ArrayList<ClassLaw> values) {
		laws.clear();
		if (values != null) laws.addAll(values);
		law = laws.isEmpty() ? null : laws.get(0);
		installLawBridges();
	}

	public boolean addRule(RuleDefinition rule, boolean ignoreCapacity) {
		if (rule == null || (!ignoreCapacity && usedCapacity() + rule.capacityCost() > maxCapacity())) return false;
		rule.assignRuntimeOrder(nextRuleOrder++);
		rules.add(rule);
		return true;
	}

	public boolean removeLastRule() {
		if (rules.isEmpty()) return false;
		rules.remove(rules.size() - 1);
		return true;
	}

	public boolean activeSurchargeCanPay(RuleContext context, RuleCost cost) {
		if (context.event != RuleEvent.ACTIVE || !hasRestriction(Restriction.ACTIVE_COSTS_HP)) return true;
		int baseHp = cost.type == RuleCost.Type.HP ? cost.amount
				: cost.type == RuleCost.Type.RESOURCE
				? overdrawHpCost(cost.amount, resourceValue(cost.resourceId, cost.resourceEngine), cost.resourceEngine) : 0;
		return context.hero.HP > baseHp + 2;
	}

	public void payActiveSurcharge(RuleContext context) {
		if (context.event == RuleEvent.ACTIVE && hasRestriction(Restriction.ACTIVE_COSTS_HP)) {
			context.hero.HP -= 2;
			if (context.hero.sprite != null) context.hero.sprite.showStatus(CharSprite.NEGATIVE,
					Messages.get(RuleRuntime.class, "active_hp_cost", 2));
		}
	}

	public String customName() { return customName; }
	public ClassBuild classBuild() { return classBuild == null ? null : classBuild.copy(); }
	/** MIGRATION compatibility view; runtime authority is ClassBuild.gameplayComponents. */
	@Deprecated
	public ClassGameplaySpec gameplay() {
		ClassGameplaySpec result = new ClassGameplaySpec();
		if (classBuild == null) return result;
		result.basicAttack = classBuild.basicAttackProfile();
		result.ownership = classBuild.hasGameplayComponent(ClassGameplayComponentSpec.Type.OWNERSHIP);
		result.command = classBuild.hasGameplayComponent(ClassGameplayComponentSpec.Type.COMMAND);
		result.entityCapacity = classBuild.ownedCapacity(RuleOwnedEntity.Kind.ACTOR);
		result.deviceCapacity = classBuild.ownedCapacity(RuleOwnedEntity.Kind.DEVICE);
		result.modes.addAll(classBuild.modes());
		for (ClassGameplayComponentSpec component : classBuild.gameplayComponents) if (component != null
				&& component.type == ClassGameplayComponentSpec.Type.RECYCLE) {
			result.recycle = true; result.recycleResourceId = component.resourceId; result.recycleAmount = component.amount; break;
		}
		return result;
	}
	public ArrayList<ClassOperationSpec> classOperations() {
		ArrayList<ClassOperationSpec> result = new ArrayList<>();
		if (classBuild != null) for (ClassOperationSpec value : classBuild.operations) result.add(value.copy());
		return result;
	}
	public ClassOperationSpec classOperation(String id) {
		if (classBuild == null) return null;
		ClassOperationSpec value = classBuild.operation(id); return value == null ? null : value.copy();
	}
	public float basicAttackDamageMultiplier() {
		return classBuild == null ? 1f : classBuild.basicAttackProfile().damageMultiplier();
	}
	public int ownedCapacity(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind kind) {
		if (classBuild == null) return 6;
		return classBuild.ownedCapacity(kind);
	}
	public int persistenceLifetime(RuleOwnedEntity.Kind kind) {
		return classBuild == null ? 0 : classBuild.persistenceLifetime(kind);
	}
	public ResourceEngine engine() { return engine; }
	public ResourceSpec primaryResourceSpec() { return primaryResourceSpec == null ? null : primaryResourceSpec.copy(); }
	public String primaryResourceName() {
		return primaryResourceSpec == null ? (engine == null ? "" : engine.displayName()) : primaryResourceSpec.displayName();
	}
	public ClassLaw law() { return law; }
	public int resource() { return resource; }
	public int maxResource() { return maxResource; }
	public int overflowRemainder() { return overflowRemainder; }
	public int consecutiveMoves() { return consecutiveMoves; }
	public int focusSafeTurns() { return focusSafeTurns; }
	public int executionDepth() { return executionDepth; }
	public ArrayList<RuleDefinition> rules() { return new ArrayList<>(rules); }
	public ArrayList<RuleDefinition> activeTechniques() {
		ArrayList<RuleDefinition> result = new ArrayList<>();
		for (RuleDefinition rule : rules) if (rule.trigger.event == RuleEvent.ACTIVE) result.add(rule);
		return result;
	}
	public RuleDefinition activeTechnique() {
		for (RuleDefinition rule : rules) if (rule.trigger.event == RuleEvent.ACTIVE) return rule;
		return null;
	}
	public RuleDefinition activeTechnique(String id) {
		if (id == null || id.isEmpty()) return activeTechnique();
		for (RuleDefinition rule : rules) {
			if (rule.trigger.event == RuleEvent.ACTIVE && id.equals(rule.id)) return rule;
		}
		return null;
	}
	public RuleDefinition reactionTechnique() {
		for (RuleDefinition rule : rules) if (rule.trigger.event != RuleEvent.ACTIVE) return rule;
		return null;
	}
	public boolean hasRestriction(Restriction restriction) { return restrictions.contains(restriction); }
	public boolean hasLaw(ClassLaw value) { return value != null && laws.contains(value); }
	public ArrayList<ClassLaw> laws() { return new ArrayList<>(laws); }
	public ArrayList<RuleResourceState> additionalResourceStates() {
		ArrayList<RuleResourceState> result = new ArrayList<>();
		for (RuleResourceState value : additionalResources) result.add(value.copy());
		return result;
	}
	public boolean hasVocabulary(CoreRuleVocabulary vocabulary) { return vocabularies.contains(vocabulary); }
	public ArrayList<CoreRuleVocabulary> vocabularies() { return new ArrayList<>(vocabularies); }
	public ArrayList<TraitSpec> traitSpecs() {
		ArrayList<TraitSpec> result = new ArrayList<>();
		for (TraitSpec value : traitSpecs) result.add(value.copy());
		return result;
	}
	public boolean convertsHealingToShield() { return hasLaw(ClassLaw.HEALING_TO_SHIELD); }
	public boolean traditionalHealingAllowed() { return !hasRestriction(Restriction.NO_TRADITIONAL_HEALING); }

	public int usedCapacity() {
		if (classBuild != null) return classBuild.usedBudget();
		int result = 0;
		for (ClassLaw value : laws) result += value.capacityCost;
		for (CoreRuleVocabulary vocabulary : vocabularies) result += vocabulary.capacityCost;
		for (RuleDefinition rule : rules) result += rule.capacityCost();
		return result;
	}

	public int maxCapacity() {
		int result = (classBuild == null ? ClassBudgetPolicy.newBuildBudget() : classBuild.baseBudget
				+ (classBuild.progression == null ? 0 : classBuild.progression.budgetBonus)) + debugCapacityBonus;
		for (Restriction restriction : restrictions) result += restriction.capacityBonus;
		return result;
	}

	public String configDescription() {
		String restrictionText;
		if (restrictions.isEmpty()) restrictionText = Messages.get(RuleRuntime.class, "none");
		else {
			StringBuilder names = new StringBuilder();
			for (Restriction restriction : restrictions) {
				if (names.length() > 0) names.append(Messages.get(RuleRuntime.class, "separator"));
				names.append(restriction.displayName());
			}
			restrictionText = names.toString();
		}
		String resourceText = engine == null ? Messages.get(RuleRuntime.class, "none") : primaryResourceName();
		String lawText = laws.isEmpty() ? Messages.get(RuleRuntime.class, "none") : laws.get(0).displayName();
		StringBuilder text = new StringBuilder(Messages.get(RuleRuntime.class, "config_v2",
				customName, resourceText, lawText, usedCapacity(), maxCapacity(), restrictionText));
		if (!traitSpecs.isEmpty()) {
			StringBuilder vocabularyText = new StringBuilder();
			for (TraitSpec trait : traitSpecs) {
				if (vocabularyText.length() > 0) vocabularyText.append(Messages.get(RuleRuntime.class, "separator"));
				vocabularyText.append(trait.displayName(classBuild));
			}
			text.append(Messages.get(RuleRuntime.class, "vocabulary", vocabularyText));
		}
		for (int i = 0; i < rules.size(); i++) text.append(Messages.get(RuleRuntime.class, "rule", i + 1,
				rules.get(i).debugDescription(engine)));
		return text.toString();
	}

	/** A detached player-facing blueprint reconstructed from saved runtime data. */
	public CustomClassConfig presentationConfig() {
		return CustomClassConfig.fromClassBuild(presentationBuild());
	}

	public ClassBuild presentationBuild() {
		ClassBuild result = classBuild == null ? new ClassBuild() : classBuild.copy();
		result.name = customName;
		if (classBuild == null) {
			result.baseBudget = ClassBudgetPolicy.newBuildBudget();
			result.resources.clear();
			if (engine != null) result.resources.add(primaryResourceSpec == null
					? new ResourceSpec(engine) : primaryResourceSpec.copy());
			for (RuleResourceState state : additionalResources) {
				ResourceSpec spec = state.spec == null ? new ResourceSpec(state.engine) : state.spec.copy();
				spec.id = state.id;
				result.resources.add(spec);
			}
			result.skills.clear();
			for (RuleDefinition rule : rules) result.skills.add(SkillSpec.fromRule(rule, engine));
			result.laws.clear();
			result.laws.addAll(laws);
			result.traits.clear();
			for (TraitSpec trait : traitSpecs) result.traits.add(trait.copy());
			result.restrictions.clear();
			result.restrictions.addAll(restrictions);
		}
		return result;
	}

	private static RuleCondition.Type conditionType(RuleDefinition rule, int index) {
		return rule.conditions.size() > index ? rule.conditions.get(index).type : RuleCondition.Type.ALWAYS;
	}

	private static int conditionParameter(RuleDefinition rule, int index) {
		return rule.conditions.size() > index ? rule.conditions.get(index).parameter : 0;
	}

	public String stateDescription() {
		StringBuilder text = new StringBuilder(configDescription());
		text.append(Messages.get(RuleRuntime.class, "state_v2", resource, maxResource, turnCounter,
				turnsSinceCombat, consecutiveMoves, focusSafeTurns));
		for (int i = 0; i < rules.size(); i++) {
			RuleDefinition rule = rules.get(i);
			text.append(Messages.get(RuleRuntime.class, "runtime", i + 1,
					rule.triggerCount(), rule.cooldownRemaining()));
		}
		return text.toString();
	}

	private static final String NAME = "name";
	private static final String ENGINE = "engine";
	private static final String HAS_ENGINE = "has_engine";
	private static final String CLASS_BUILD = "class_build";
	private static final String LAWS = "laws";
	private static final String ADDITIONAL_RESOURCES = "additional_resources";
	private static final String LAW = "law";
	private static final String RESOURCE = "resource";
	private static final String MAX_RESOURCE = "max_resource";
	private static final String TURN = "turn";
	private static final String COMBAT = "combat";
	private static final String MOVE = "move";
	private static final String MOVES = "moves";
	private static final String FOCUS = "focus";
	private static final String OVERFLOW_REMAINDER = "overflow_remainder";
	private static final String TRAIT_MOVE_COUNTER = "trait_move_counter";
	private static final String PRIMARY_FLOW_COUNTERS = "primary_flow_counters";
	private static final String DEBUG_CAPACITY = "debug_capacity";
	private static final String NEXT_ORDER = "next_order";
	private static final String NEXT_EVENT_ID = "next_event_id";
	private static final String NEXT_PAYLOAD_ID = "next_payload_id";
	private static final String RULES = "rules";
	private static final String BRIDGES = "bridges";
	private static final String DELAYED = "delayed";
	private static final String VOCABULARIES = "vocabularies";
	private static final String RESTRICTIONS = "restrictions";

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(NAME, customName);
		bundle.put(HAS_ENGINE, engine != null);
		if (engine != null) bundle.put(ENGINE, engine);
		if (law != null) bundle.put(LAW, law);
		bundle.put(CLASS_BUILD, presentationBuild());
		String[] lawNames = new String[laws.size()];
		for (int i = 0; i < laws.size(); i++) lawNames[i] = laws.get(i).name();
		bundle.put(LAWS, lawNames);
		bundle.put(ADDITIONAL_RESOURCES, additionalResources);
		bundle.put(RESOURCE, resource);
		bundle.put(MAX_RESOURCE, maxResource);
		bundle.put(TURN, turnCounter);
		bundle.put(COMBAT, turnsSinceCombat);
		bundle.put(MOVE, turnsSinceMove);
		bundle.put(MOVES, consecutiveMoves);
		bundle.put(FOCUS, focusSafeTurns);
		bundle.put(OVERFLOW_REMAINDER, overflowRemainder);
		bundle.put(TRAIT_MOVE_COUNTER, traitMoveCounter);
		bundle.put(PRIMARY_FLOW_COUNTERS, primaryFlowCounters);
		bundle.put(DEBUG_CAPACITY, debugCapacityBonus);
		bundle.put(NEXT_ORDER, nextRuleOrder);
		bundle.put(NEXT_EVENT_ID, nextEventId);
		bundle.put(NEXT_PAYLOAD_ID, nextPayloadId);
		bundle.put(RULES, rules);
		bundle.put(BRIDGES, bridges);
		bundle.put(DELAYED, delayedPayloads);
		String[] vocabularyNames = new String[vocabularies.size()];
		for (int i = 0; i < vocabularies.size(); i++) vocabularyNames[i] = vocabularies.get(i).name();
		bundle.put(VOCABULARIES, vocabularyNames);
		String[] restrictionNames = new String[restrictions.size()];
		int index = 0;
		for (Restriction restriction : restrictions) restrictionNames[index++] = restriction.name();
		bundle.put(RESTRICTIONS, restrictionNames);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		customName = bundle.getString(NAME);
		classBuild = bundle.contains(CLASS_BUILD) ? (ClassBuild)bundle.get(CLASS_BUILD) : null;
		boolean hasEngine = bundle.contains(HAS_ENGINE) ? bundle.getBoolean(HAS_ENGINE) : bundle.contains(ENGINE);
		engine = hasEngine ? bundle.getEnum(ENGINE, ResourceEngine.class) : null;
		primaryResourceSpec = classBuild == null ? (engine == null ? null : new ResourceSpec(engine))
				: classBuild.primaryResource() == null ? null : classBuild.primaryResource().copy();
		law = bundle.contains(LAW) ? bundle.getEnum(LAW, ClassLaw.class) : null;
		laws.clear();
		if (bundle.contains(LAWS)) {
			String[] storedLawNames = bundle.getStringArray(LAWS);
			if (storedLawNames != null) for (String name : storedLawNames) {
				try { laws.add(ClassLaw.valueOf(name)); } catch (IllegalArgumentException ignored) {}
			}
		} else if (law != null) laws.add(law);
		law = laws.isEmpty() ? null : laws.get(0);
		additionalResources.clear();
		if (bundle.contains(ADDITIONAL_RESOURCES)) {
			for (Bundlable stored : bundle.getCollection(ADDITIONAL_RESOURCES)) {
				if (stored instanceof RuleResourceState) additionalResources.add((RuleResourceState)stored);
			}
		}
		if (classBuild != null) for (RuleResourceState state : additionalResources) {
			if (state.spec == null) {
				ResourceSpec declared = classBuild.resource(state.id);
				state.spec = declared == null ? new ResourceSpec(state.engine) : declared.copy();
			}
		}
		resource = bundle.getInt(RESOURCE);
		maxResource = bundle.getInt(MAX_RESOURCE);
		if (primaryResourceSpec != null) primaryResourceSpec.current = resource;
		if (classBuild != null && classBuild.primaryResource() != null) classBuild.primaryResource().current = resource;
		if (classBuild != null) for (RuleResourceState state : additionalResources) {
			if (state.spec != null) state.spec.current = state.value;
			if (classBuild.resource(state.id) != null) classBuild.resource(state.id).current = state.value;
		}
		turnCounter = bundle.getInt(TURN);
		turnsSinceCombat = bundle.getInt(COMBAT);
		turnsSinceMove = bundle.getInt(MOVE);
		consecutiveMoves = bundle.getInt(MOVES);
		focusSafeTurns = bundle.getInt(FOCUS);
		overflowRemainder = bundle.getInt(OVERFLOW_REMAINDER);
		traitMoveCounter = bundle.contains(TRAIT_MOVE_COUNTER) ? bundle.getInt(TRAIT_MOVE_COUNTER) : 0;
		primaryFlowCounters = bundle.contains(PRIMARY_FLOW_COUNTERS)
				? bundle.getIntArray(PRIMARY_FLOW_COUNTERS) : new int[0];
		debugCapacityBonus = bundle.getInt(DEBUG_CAPACITY);
		nextRuleOrder = bundle.getInt(NEXT_ORDER);
		nextEventId = bundle.contains(NEXT_EVENT_ID) ? Math.max(1L, bundle.getLong(NEXT_EVENT_ID)) : 1L;
		nextPayloadId = bundle.contains(NEXT_PAYLOAD_ID) ? Math.max(1L, bundle.getLong(NEXT_PAYLOAD_ID)) : 1L;
		rules.clear();
		Collection<Bundlable> storedRules = bundle.getCollection(RULES);
		for (Bundlable stored : storedRules) {
			if (stored instanceof RuleDefinition) {
				RuleDefinition rule = (RuleDefinition) stored;
				rule.assignRuntimeOrder(nextRuleOrder++);
				rules.add(rule);
				nextRuleOrder = Math.max(nextRuleOrder, rule.runtimeOrder() + 1);
			}
		}
		bridges.clear();
		if (bundle.contains(BRIDGES)) {
			for (Bundlable stored : bundle.getCollection(BRIDGES)) {
				if (stored instanceof RuleEventBridge) bridges.add((RuleEventBridge)stored);
			}
		}
		delayedPayloads.clear();
		if (bundle.contains(DELAYED)) {
			for (Bundlable stored : bundle.getCollection(DELAYED)) {
				if (stored instanceof RuleDelayedPayload) delayedPayloads.add((RuleDelayedPayload)stored);
			}
		}
		vocabularies.clear();
		traitSpecs.clear();
		String[] vocabularyNames = bundle.contains(VOCABULARIES) ? bundle.getStringArray(VOCABULARIES) : null;
		if (vocabularyNames != null) {
			for (String name : vocabularyNames) {
				try {
					CoreRuleVocabulary value = CoreRuleVocabulary.valueOf(name);
					if (value != CoreRuleVocabulary.NONE && !vocabularies.contains(value)) vocabularies.add(value);
				} catch (IllegalArgumentException ignored) {}
			}
		}
		if (classBuild != null) {
			vocabularies.clear();
			CoreRuleVocabulary migratedReplacement=replacementForLegacyLaw(law);
			for (TraitSpec spec : classBuild.traits) if(spec.type!=migratedReplacement) installTrait(spec);
		} else {
			for (CoreRuleVocabulary value : new ArrayList<>(vocabularies)) traitSpecs.add(TraitSpec.of(value));
		}
		restrictions.clear();
		String[] restrictionNames = bundle.contains(RESTRICTIONS) ? bundle.getStringArray(RESTRICTIONS) : null;
		if (restrictionNames != null) {
			for (String name : restrictionNames) {
				try { restrictions.add(Restriction.valueOf(name)); } catch (IllegalArgumentException ignored) {}
			}
		}
		executionDepth = 0;
		executingRules = new HashSet<>();
		installLawBridges();
		if (classBuild == null) classBuild = presentationBuild();
	}

	public static String previewName(Bundle heroBundle) {
		if (!heroBundle.contains(Hero.RULE_RUNTIME)) return "";
		return heroBundle.getBundle(Hero.RULE_RUNTIME).getString(NAME);
	}

	public static CustomClassConfig previewConfig(Bundle heroBundle) {
		if (!heroBundle.contains(Hero.RULE_RUNTIME)) return null;
		RuleRuntime runtime = new RuleRuntime();
		runtime.restoreFromBundle(heroBundle.getBundle(Hero.RULE_RUNTIME));
		return runtime.presentationConfig();
	}
}
