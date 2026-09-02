package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleHooks;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTestBuilds;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrace;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Executes scripts against real SPD gameplay objects with no GameScene or renderer. */
public class HeadlessGameplayHarness implements AutoCloseable {
	private QaScenario scenario;
	private CustomClassConfig config;
	private ClassBuild classBuild;
	private QaFixedLevel level;
	private Hero hero;
	private int turn;
	private boolean hostileInteraction;
	private boolean finiteInput;
	private boolean saveLoadMatched;
	private final ArrayList<QaSnapshot> samples = new ArrayList<>();
	private final QaTraceCollector trace = new QaTraceCollector();

	public ScenarioResult run(QaScenario value) {
		long started = System.nanoTime();
		ScenarioResult result = new ScenarioResult();
		result.id = value.id;
		result.seed = value.seed;
		result.expectedFindings.addAll(value.expectedFindings);
		try {
			setup(value);
			result.buildId = classBuild.name;
			result.buildConfig = RuleBuild.from(classBuild).fingerprint()
					+ (value.hero.qaRuntimePreset == null ? "" : "|QA=" + value.hero.qaRuntimePreset);
			BuildAnalysis analysis = new RuleBuildAnalyzer().analyze(RuleBuild.from(classBuild));
			result.classification = analysis.classification.name();
			for (BuildAnalysis.Finding finding : analysis.findings) addUnique(result.findings, finding.code);
			result.initialSnapshot = QaSnapshot.capture(turn, hero);
			samples.add(result.initialSnapshot);

			if (!value.staticOnly) {
				for (QaScenario.Action action : value.actions) {
					int repeats = Math.max(1, action.repeat);
					for (int i = 0; i < repeats; i++) execute(action);
				}
				String loop = PositiveLoopDetector.detect(samples, hostileInteraction, finiteInput);
				if (loop != null) addUnique(result.findings, loop);
				addBehaviorAssertions(result);
			}
			result.finalSnapshot = QaSnapshot.capture(turn, hero);
			result.turn = turn;
			if (hasBrokenFinding(result.findings)) result.classification = BuildAnalysis.Classification.BROKEN.name();
			result.expectationMet = result.findings.containsAll(result.expectedFindings);
		} catch (Throwable error) {
			result.runtimeFailure = true;
			result.classification = BuildAnalysis.Classification.BROKEN.name();
			Throwable root = error;
			while (root.getCause() != null && root.getCause() != root) root = root.getCause();
			result.failure = root.getClass().getName() + ": " + root.getMessage();
			trace.record("FAIL", result.failure);
			StringWriter stack = new StringWriter();
			error.printStackTrace(new PrintWriter(stack));
			for (String line : stack.toString().split("\\R")) trace.record("STACK", line);
			result.expectationMet = false;
		} finally {
			result.elapsedNanos = System.nanoTime() - started;
			result.traceText = trace.text();
			close();
		}
		return result;
	}

	private void setup(QaScenario value) {
		scenario = value;
		if (!QaScenario.SCHEMA.equals(value.schema)) throw new IllegalArgumentException("unsupported scenario schema");
		Random.pushGenerator(value.seed);
		RuleTrace.install(trace);
		Actor.clear();
		Actor.resetNextID();
		Dungeon.seed = value.seed;
		Dungeon.depth = 1;
		level = new QaFixedLevel(value.level.width, value.level.height);
		Dungeon.level = level;
		for (int cell : value.level.waterCells) level.setQaTerrain(cell, Terrain.WATER);

		classBuild = resolveBuild(value.hero);
		config = CustomClassConfig.fromClassBuild(classBuild);
		hero = new Hero();
		hero.heroClass = HeroClass.ROGUE;
		Talent.initClassTalents(hero);
		hero.HT = Math.max(1, value.hero.maxHp);
		hero.HP = Math.max(1, Math.min(hero.HT, value.hero.hp));
		hero.pos = value.level.heroCell;
		hero.damageInterrupt = false;
		hero.sprite = new NoOpCharSprite(hero);
		hero.setRuleRuntime(new RuleRuntime(classBuild));
		RuleExpressivenessFixtures.install(value.hero.qaRuntimePreset, hero);
		if ("save_load".equals(value.id) && hero.ruleRuntime().activeTechnique() != null) {
			hero.ruleRuntime().activeTechnique().modifier.cooldown = 3f;
		}
		Dungeon.hero = hero;
		level.occupyCell(hero);
		Actor.init();
		trace.turn(0);
		trace.action("SCENARIO " + value.id + " seed=" + value.seed + " build=" + classBuild.name);
	}

	private static ClassBuild resolveBuild(QaScenario.HeroSpec spec) {
		if (spec.classBuild != null) return spec.classBuild.copy();
		if (spec.build != null) return spec.build.toClassBuild();
		if (spec.preset != null) return RuleTestBuilds.config(RuleTestBuilds.Preset.valueOf(spec.preset)).toClassBuild();
		throw new IllegalArgumentException("scenario hero build is missing");
	}

	private void execute(QaScenario.Action action) {
		if (action == null || action.type == null) throw new IllegalArgumentException("action type is missing");
		trace.turn(turn);
		switch (action.type) {
			case SET_HP:
				hero.HP = Math.max(1, Math.min(hero.HT, action.value));
				trace.action("SET_HP " + hero.HP);
				break;
			case SET_RESOURCE:
				hero.ruleRuntime().setResourceForDebug(hero, action.resourceId, null, action.value);
				trace.action("SET_RESOURCE " + hero.ruleRuntime().resource());
				break;
			case SET_TILE:
				level.setQaTerrain(action.cell, terrain(action.tile));
				trace.action("SET_TILE cell=" + action.cell + " tile=" + action.tile);
				break;
			case SPAWN_MOB:
				spawnMob(action.mob, action.cell, action.value);
				trace.action("SPAWN_MOB " + action.mob + " cell=" + action.cell);
				break;
			case SPAWN_ITEM:
				spawnItem(action.item);
				trace.action("SPAWN_ITEM " + action.item);
				break;
			case APPLY_STATUS:
				applyStatus(action.status, action.value);
				finiteInput = true;
				trace.action("APPLY_STATUS " + action.status);
				break;
			case CLEAR_STATUS:
				for (Buff buff : new ArrayList<>(hero.buffs())) {
					if (buff.type == Buff.buffType.NEGATIVE) buff.detach();
				}
				trace.action("CLEAR_STATUS");
				break;
			case SAVE_RELOAD:
				saveReload();
				trace.action("SAVE_RELOAD matched=" + saveLoadMatched);
				break;
			case WAIT:
				beginTurn();
				RuleHooks.onWait(hero);
				hero.spend(Actor.TICK);
				trace.action("HERO WAIT");
				finishTurn();
				break;
			case MOVE:
				if (!level.adjacent(hero.pos, action.cell)
						|| !(level.passable[action.cell] || level.avoid[action.cell])
						|| Actor.findChar(action.cell) != null) throw new IllegalArgumentException("invalid move cell " + action.cell);
				beginTurn();
				int from = hero.pos;
				hero.move(action.cell, false);
				hero.spend(Actor.TICK);
				trace.action("HERO MOVE " + from + "->" + hero.pos);
				finishTurn();
				break;
			case ATTACK:
				Mob target = targetMob(action.targetId);
				if (target == null) {
					trace.action("HERO ATTACK no-target");
					break;
				}
				if (!level.adjacent(hero.pos, target.pos)) throw new IllegalArgumentException("attack target is not adjacent");
				beginTurn();
				int before = target.HP;
				boolean hit = hero.attack(target);
				hero.spend(hero.attackDelay());
				hostileInteraction = true;
				trace.action("HERO ATTACK MOB#" + target.id() + " hit=" + hit + " hp=" + before + "->" + target.HP);
				finishTurn();
				break;
			case USE_ACTIVE_RULE:
				beginTurn();
				RuleDefinition active = hero.ruleRuntime() == null ? null
						: hero.ruleRuntime().activeTechnique(action.skillId);
				boolean fired = RuleHooks.triggerActive(hero, action.cell < 0 ? hero.pos : action.cell,
						action.skillId);
				hero.spend(active == null ? Actor.TICK : active.cost.actionTime());
				trace.action("HERO ACTIVE cell=" + action.cell + " fired=" + fired);
				finishTurn();
				break;
			case USE_CLASS_OPERATION:
				beginTurn();
				ClassOperationSpec operation = hero.ruleRuntime() == null ? null
						: hero.ruleRuntime().classOperation(action.operationId);
				boolean operated = operation != null && ClassOperationRuntime.execute(hero, operation,
						action.cell < 0 ? hero.pos : action.cell);
				hero.spend(operation == null ? Actor.TICK : operation.actionTime);
				trace.action("HERO CLASS_OPERATION id=" + action.operationId + " cell=" + action.cell
						+ " fired=" + operated);
				finishTurn();
				break;
		}
		if (turn == 0 || turn % 10 == 0 || action.type == QaScenario.Action.Type.SAVE_RELOAD) {
			samples.add(QaSnapshot.capture(turn, hero));
		}
	}

	private void beginTurn() {
		turn++;
		trace.turn(turn);
		RuleHooks.onTurnStart(hero);
	}

	private void finishTurn() {
		int hp = hero.HP;
		Actor.processUntilTurn(hero, 10000);
		if (hero.HP < hp) hostileInteraction = true;
	}

	private void spawnMob(String type, int cell, int hp) {
		Mob mob;
		if (type == null || "RAT".equalsIgnoreCase(type)) mob = new Rat();
		else if ("SNAKE".equalsIgnoreCase(type)) mob = new Snake();
		else throw new IllegalArgumentException("unsupported mob " + type);
		if (Actor.findChar(cell) != null || !level.passable[cell]) throw new IllegalArgumentException("occupied mob cell " + cell);
		mob.pos = cell;
		if (hp > 0) mob.HT = mob.HP = hp;
		mob.EXP = 0;
		mob.maxLvl = -1;
		mob.sprite = new NoOpCharSprite(mob);
		level.mobs.add(mob);
		Actor.add(mob);
		mob.aggro(hero);
	}

	private void spawnItem(String type) {
		Item item;
		if (type == null || "HEALING_POTION".equalsIgnoreCase(type)) item = new PotionOfHealing();
		else if ("TOXIC_GAS_POTION".equalsIgnoreCase(type)) item = new PotionOfToxicGas();
		else if ("BOMB".equalsIgnoreCase(type)) item = new Bomb();
		else throw new IllegalArgumentException("unsupported item " + type);
		if (!item.collect(hero.belongings.backpack)) throw new IllegalStateException("unable to collect spawned item");
	}

	private void applyStatus(String type, int duration) {
		int value = duration > 0 ? duration : 5;
		if ("POISON".equalsIgnoreCase(type)) Buff.affect(hero, Poison.class).set(value);
		else if ("BURNING".equalsIgnoreCase(type)) Buff.affect(hero, Burning.class).reignite(hero, value);
		else throw new IllegalArgumentException("unsupported status " + type);
	}

	private void saveReload() {
		QaSnapshot before = QaSnapshot.capture(turn, hero);
		trace.action("SAVE_RELOAD before mobs=" + mobIdentitySummary(level));
		//The formal save path normalizes Actor time before serializing owners and their payloads.
		Actor.fixTime();
		Bundle bundle = new Bundle();
		Bundle levelBundle = new Bundle();
		Bundle actorBundle = new Bundle();
		hero.storeInBundle(bundle);
		level.storeInBundle(levelBundle);
		Actor.storeNextID(actorBundle);
		// Match Dungeon.loadGame ordering: clear the old Actor identity registry before restoring
		// the Hero. Otherwise Actor.restoreFromBundle sees the still-live old Hero ID as occupied,
		// assigns a new ID, and saved owned entities correctly reject that different owner.
		Actor.clear();
		Actor.restoreNextID(actorBundle);
		Hero restored = new Hero();
		restored.restoreFromBundle(bundle);
		restored.damageInterrupt = false;
		restored.sprite = new NoOpCharSprite(restored);
		hero = restored;
		QaFixedLevel restoredLevel = new QaFixedLevel();
		restoredLevel.restoreFromBundle(levelBundle);
		level = restoredLevel;
		trace.action("SAVE_RELOAD restored mobs=" + mobIdentitySummary(level));
		Dungeon.level = level;
		Dungeon.hero = restored;
		for (Mob mob : level.mobs) mob.sprite = new NoOpCharSprite(mob);
		Actor.init();
		QaSnapshot after = QaSnapshot.capture(turn, hero);
		saveLoadMatched = before.sameSavedRuleState(after);
		samples.add(before);
		samples.add(after);
	}

	private static String mobIdentitySummary(QaFixedLevel level) {
		ArrayList<String> values = new ArrayList<>();
		for (Mob mob : level.mobs) values.add(mob.getClass().getSimpleName()+"#"+mob.id()+" hp="+mob.HP);
		java.util.Collections.sort(values);
		return values.toString();
	}

	private Mob targetMob(int targetId) {
		for (Mob mob : level.mobs) {
			if (mob.isAlive() && (targetId < 0 || mob.id() == targetId)) return mob;
		}
		return null;
	}

	private static int terrain(String name) {
		if ("WATER".equalsIgnoreCase(name)) return Terrain.WATER;
		if ("CHASM".equalsIgnoreCase(name)) return Terrain.CHASM;
		if ("WALL".equalsIgnoreCase(name)) return Terrain.WALL;
		return Terrain.EMPTY;
	}

	private void addBehaviorAssertions(ScenarioResult result) {
		QaSnapshot finalState = QaSnapshot.capture(turn, hero);
		if ("momentum_loop".equals(scenario.id) && finalState.hero.resource == 0) {
			addUnique(result.findings, "MOMENTUM_BEHAVIOR_OK");
		}
		if ("affliction".equals(scenario.id) && finalState.hero.resource >= 4
				&& finalState.hero.buffs.contains("Poison") && finalState.hero.buffs.contains("Burning")) {
			addUnique(result.findings, "AFFLICTION_FLOW_OK");
		}
		if ("real_combat".equals(scenario.id) && finalState.mobs.isEmpty()
				&& hasTriggeredRule(finalState, "reaction_technique")) {
			addUnique(result.findings, "REAL_COMBAT_OK");
		}
		if ("save_load".equals(scenario.id) && saveLoadMatched) addUnique(result.findings, "SAVE_LOAD_OK");
		if ("event_chain".equals(scenario.id)) {
			Mob mob = targetMob(-1);
			if (mob != null && mob.buff(Poison.class) != null && hero.ruleRuntime().resource() >= 2
					&& traceContains("CHAIN") && traceContains("ON_STATUS_APPLIED parent=#")) {
				addUnique(result.findings, "EVENT_CHAIN_OK");
			}
		}
		if ("bridge_move".equals(scenario.id) && hasTriggeredRule(finalState, "bridge_move_rule")
				&& traceContains("TAG FORCED_MOVEMENT") && traceContains("from=25 to=26")
				&& traceContains("BRIDGE FORCED_MOVEMENT")) addUnique(result.findings, "BRIDGE_MOVE_OK");
		if ("bridge_recursion_guard".equals(scenario.id)
				&& ruleTriggerCount(finalState, "bridge_move_rule") == 1
				&& traceContains("BRIDGE_GUARD")) addUnique(result.findings, "BRIDGE_GUARD_OK");
		if ("mark_chain".equals(scenario.id)) {
			Mob mob = targetMob(-1);
			RuleMark mark = RuleMark.get(mob, RuleMark.Type.HUNTED);
			if (mark != null && mark.sourceId() == hero.id() && mark.ownerId() == mob.id()
					&& hasTriggeredRule(finalState, "mark_followup")) addUnique(result.findings, "MARK_CHAIN_OK");
		}
		if ("mark_expire".equals(scenario.id) && targetMob(-1) != null
				&& RuleMark.get(targetMob(-1), RuleMark.Type.HUNTED) == null) {
			addUnique(result.findings, "MARK_EXPIRE_OK");
		}
		if ("delay_three_turns".equals(scenario.id) && finalState.hero.shield == 4
				&& hero.ruleRuntime().pendingDelayedCount() == 0
				&& traceContains("T=4 DELAY fire")) addUnique(result.findings, "DELAY_THREE_OK");
		if ("delay_save_load".equals(scenario.id) && saveLoadMatched && finalState.hero.shield == 4
				&& hero.ruleRuntime().pendingDelayedCount() == 0
				&& traceContains("T=4 DELAY fire")) addUnique(result.findings, "DELAY_SAVE_LOAD_OK");
		if ("accumulation_echo".equals(scenario.id) && saveLoadMatched
				&& hero.ruleRuntime().pendingDelayedCount() == 0
				&& traceContains("ACCUMULATION ready") && traceContains("ACCUMULATION consumed")
				&& traceContains("ECHO schedule") && traceContains("kind=ECHO")) {
			addUnique(result.findings, "ACCUMULATION_ECHO_OK");
		}
		if ("overflow_overdraw".equals(scenario.id) && saveLoadMatched && hero.HP < hero.HT
				&& finalState.hero.shield > 0 && traceContains("OVERFLOW input=")
				&& traceContains("OVERDRAW missing=")) addUnique(result.findings, "OVERFLOW_OVERDRAW_OK");
		if ("compensation".equals(scenario.id) && saveLoadMatched && finalState.hero.shield > 0
				&& RuleMark.has(hero, RuleMark.Type.COMPENSATION_LOCK, 1)
				&& traceContains("COMPENSATION applied=true")) addUnique(result.findings, "COMPENSATION_OK");
		if ("phase_blood".equals(scenario.id) && saveLoadMatched
				&& RuleMark.has(hero, RuleMark.Type.LOW_PHASE, 1)
				&& anyMobHas(Barrier.class) && traceContains("PHASE_SHIFT modifier=AREA")) {
			addUnique(result.findings, "PHASE_BLOOD_OK");
		}
		if ("hunt_mark".equals(scenario.id) && traceContains("HUNT_MARK consumed")
				&& anyMobHas(Slow.class)) addUnique(result.findings, "HUNT_MARK_VOCAB_OK");
		if ("hunt_propagation".equals(scenario.id) && traceContains("PROPAGATION from=")
				&& anyMobHasMark(RuleMark.Type.HUNTED, 1)) addUnique(result.findings, "HUNT_PROPAGATION_OK");
		if ("inertia_momentum".equals(scenario.id) && traceContains("BRIDGE FORCED_MOVEMENT")
				&& hasTriggeredRule(finalState, "reaction_technique")) addUnique(result.findings, "INERTIA_MOMENTUM_OK");
		if ("translocation_bridge".equals(scenario.id) && hero.pos == 25 && level.water[25]
				&& traceContains("BRIDGE TRANSLOCATION")
				&& hasTriggeredRule(finalState, "reaction_technique")) {
			addUnique(result.findings, "TRANSLOCATION_BRIDGE_OK");
		}
		if ("translocation_backlash".equals(scenario.id) && hero.buff(Slow.class) != null
				&& traceContains("TRANSLOCATION_BACKLASH")) addUnique(result.findings, "TRANSLOCATION_BACKLASH_OK");
		if ("shield_backlash".equals(scenario.id) && hero.buff(Vulnerable.class) != null
				&& traceContains("SHIELD_BACKLASH")) addUnique(result.findings, "SHIELD_BACKLASH_OK");
		if ("class_zero_resource".equals(scenario.id) && hero.ruleRuntime().engine() == null
				&& finalState.hero.resourcePools.isEmpty()) addUnique(result.findings, "CLASS_ZERO_RESOURCE_OK");
		if ("class_multiple_resource".equals(scenario.id) && saveLoadMatched
				&& finalState.hero.resourcePools.size() == 2
				&& finalState.hero.resourcePools.containsKey("mana")
				&& finalState.hero.resourcePools.containsKey("momentum")) {
			addUnique(result.findings, "CLASS_MULTIPLE_RESOURCE_OK");
		}
		if ("class_multiple_active".equals(scenario.id)
				&& ruleTriggerCount(finalState, "active_one") == 1
				&& ruleTriggerCount(finalState, "active_two") == 1) {
			addUnique(result.findings, "CLASS_MULTIPLE_ACTIVE_OK");
		}
		if ("class_reaction_only".equals(scenario.id)
				&& hero.ruleRuntime().activeTechniques().isEmpty()
				&& ruleTriggerCount(finalState, "wait_guard") == 1) {
			addUnique(result.findings, "CLASS_REACTION_ONLY_OK");
		}
		if ("class_multiple_laws_traits".equals(scenario.id) && saveLoadMatched
				&& finalState.hero.classLaws.size() == 2 && finalState.hero.vocabularies.size() == 2) {
			addUnique(result.findings, "CLASS_MULTIPLE_COMPONENT_OK");
		}
		if ("class_movement".equals(scenario.id) && finalState.hero.pos == 25
				&& ruleTriggerCount(finalState, "free_step") == 1) {
			addUnique(result.findings, "CLASS_MOVEMENT_OK");
		}
		if("ref_ammo_projectile".equals(scenario.id)&&mobHp(finalState,0)==12
				&&resourceValue(finalState,"ammo")==9&&traceContains("fired=true"))addUnique(result.findings,"PROJECTILE_WALL_BLOCK_OK");
		if("ref_piercing_beam".equals(scenario.id)&&finalState.mobs.size()==2&&allMobsDamaged(finalState,10))addUnique(result.findings,"BEAM_MULTI_HIT_OK");
		if("ref_cone_controller".equals(scenario.id)&&traceContains("TAG FORCED_MOVEMENT"))addUnique(result.findings,"CONE_TARGETING_OK");
		if("ref_chain_status".equals(scenario.id)&&mobsWithBuff(finalState,"Poison")>=3)addUnique(result.findings,"CHAIN_STATUS_OK");
		if("ref_temporary_hp".equals(scenario.id)&&!finalState.hero.buffs.contains("RuleTemporaryHP")&&finalState.hero.shield==0)addUnique(result.findings,"TEMP_HP_EXPIRY_OK");
		if("ref_owned_summon".equals(scenario.id)&&saveLoadMatched&&mobType(finalState,"RuleOwnedEntity")&&mobsWithBuff(finalState,"RuleOwnership")>0&&traceContains("OWNED_ACTOR turn"))addUnique(result.findings,"OWNED_SUMMON_SAVE_OK");
		if("ref_summon_mark".equals(scenario.id)&&anyMobHasMark(RuleMark.Type.HUNTED,1))addUnique(result.findings,"SUMMON_MARK_OK");
		if("ref_persistent_field".equals(scenario.id)&&saveLoadMatched&&mobsWithBuff(finalState,"Poison")>0&&mobType(finalState,"RuleOwnedEntity"))addUnique(result.findings,"PERSISTENT_FIELD_OK");
		if("ref_terrain_control".equals(scenario.id)&&level.water[25])addUnique(result.findings,"TERRAIN_CONTROL_OK");
		if("ref_mode_shift".equals(scenario.id)&&saveLoadMatched&&finalState.hero.buffs.contains("RuleMode")&&finalState.hero.shield>0)addUnique(result.findings,"MODE_SHIFT_OK");
		if("ref_status_copy".equals(scenario.id)&&mobsWithBuff(finalState,"Poison")>0)addUnique(result.findings,"STATUS_COPY_OK");
		if("ref_reaction_only".equals(scenario.id)&&mobsWithBuff(finalState,"Bleeding")>0&&ruleTriggerCount(finalState,"hit_bleed")==1)addUnique(result.findings,"REACTION_ONLY_RUNTIME_OK");
		if("ref_attachment_save".equals(scenario.id)&&saveLoadMatched&&mobsWithBuff(finalState,"Poison")>0
				&&mobsWithBuff(finalState,"Slow")>0
				&&!finalState.hero.buffs.contains("RuleActionAttachment")&&traceContains("ATTACHMENT rule=venom_next_hit"))addUnique(result.findings,"ATTACHMENT_SAVE_CONSUME_OK");
		if("ref_trap_carrier".equals(scenario.id)&&finalState.hero.buffs.contains("Poison")
				&&traceContains("CARRIER trap"))addUnique(result.findings,"TRAP_CARRIER_TRIGGER_OK");
		if("ref_entity_expiry".equals(scenario.id)&&!mobType(finalState,"RuleOwnedEntity")
				&&resourceValue(finalState,"scrap")==2&&traceContains("OWNED_REMOVED"))addUnique(result.findings,"OWNED_ENTITY_EXPIRY_OK");
		if("ref_blob_creation".equals(scenario.id)&&level.blobs.get(ToxicGas.class)!=null
				&&level.blobs.get(ToxicGas.class).volume>0)addUnique(result.findings,"BLOB_CREATION_OK");
		if("ref_owned_command_inherit".equals(scenario.id)&&saveLoadMatched&&mobsWithBuff(finalState,"RuleMode")>0
				&&traceContains("COMMAND attack"))addUnique(result.findings,"OWNED_COMMAND_INHERIT_OK");
	}

	private static int mobHp(QaSnapshot snapshot,int index){return snapshot.mobs.size()>index?snapshot.mobs.get(index).hp:-1;}
	private static int resourceValue(QaSnapshot snapshot,String id){QaSnapshot.ResourceState state=snapshot.hero.resourcePools.get(id);return state==null?-1:state.value;}
	private static boolean allMobsDamaged(QaSnapshot snapshot,int original){if(snapshot.mobs.isEmpty())return false;for(QaSnapshot.MobState mob:snapshot.mobs)if(mob.hp>=original)return false;return true;}
	private static int mobsWithBuff(QaSnapshot snapshot,String buff){int count=0;for(QaSnapshot.MobState mob:snapshot.mobs)if(mob.buffs.contains(buff))count++;return count;}
	private static boolean mobType(QaSnapshot snapshot,String type){for(QaSnapshot.MobState mob:snapshot.mobs)if(type.equals(mob.type))return true;return false;}

	private boolean anyMobHas(Class<? extends Buff> type) {
		for (Mob mob : level.mobs) if (mob.isAlive() && mob.buff(type) != null) return true;
		return false;
	}

	private boolean anyMobHasMark(RuleMark.Type type, int stacks) {
		for (Mob mob : level.mobs) if (mob.isAlive() && RuleMark.has(mob, type, stacks)) return true;
		return false;
	}

	private boolean traceContains(String value) {
		for (String line : trace.lines) if (line.contains(value)) return true;
		return false;
	}

	private static int ruleTriggerCount(QaSnapshot snapshot, String id) {
		for (QaSnapshot.RuleState rule : snapshot.hero.activeRules) if (id.equals(rule.id)) return rule.triggerCount;
		return 0;
	}

	private static boolean hasTriggeredRule(QaSnapshot snapshot, String id) {
		for (QaSnapshot.RuleState rule : snapshot.hero.activeRules) {
			if (id.equals(rule.id) && rule.triggerCount > 0) return true;
		}
		return false;
	}

	private static boolean hasBrokenFinding(ArrayList<String> findings) {
		return findings.contains("UNBOUNDED_POWER_LOOP") || findings.contains("UNBOUNDED_RESOURCE_LOOP")
				|| findings.contains("UNBOUNDED_ACTOR_LOOP") || findings.contains("RUNTIME_FAILURE");
	}

	private static void addUnique(ArrayList<String> findings, String value) {
		if (!findings.contains(value)) findings.add(value);
	}

	@Override
	public void close() {
		RuleTrace.clear();
		Actor.clear();
		Dungeon.hero = null;
		Dungeon.level = null;
		try { Random.popGenerator(); } catch (Throwable ignored) {}
	}

	/** Presentation-only stand-in. Hero, Mob, combat, Buffs, and scheduling remain formal classes. */
	private static class NoOpCharSprite extends CharSprite {
		NoOpCharSprite(Char owner) { ch = owner; visible = false; }
		@Override public void showStatus(int color, String text, Object... args) {}
		@Override public void showStatusWithIcon(int color, String text, int icon, Object... args) {}
		@Override public void place(int cell) {}
		@Override public void move(int from, int to) {}
		@Override public void turnTo(int from, int to) {}
		@Override public void interruptMotion() { isMoving = false; }
		@Override public void die() {}
		@Override public void bloodBurstA(PointF from, int damage) {}
		@Override public void flash() {}
		@Override public void showSleep() {}
		@Override public void hideSleep() {}
		@Override public void showAlert() {}
		@Override public void hideAlert() {}
		@Override public void showInvestigate() {}
		@Override public void hideInvestigate() {}
		@Override public void showLost() {}
		@Override public void hideLost() {}
	}
}
