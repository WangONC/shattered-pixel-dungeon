package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.rules.Restriction;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDelayedPayload;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleResourceState;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Deliberately small semantic state for assertions and machine-readable reports. */
public class QaSnapshot {
	public int turn;
	public int depth;
	public HeroState hero;
	public final ArrayList<MobState> mobs = new ArrayList<>();
	public final LinkedHashMap<String, Integer> levelTiles = new LinkedHashMap<>();
	public int actorCount;

	public static class HeroState {
		public int id;
		public int pos;
		public int hp;
		public int maxHp;
		public int shield;
		public String customName;
		public String classLaw;
		public final ArrayList<String> classLaws = new ArrayList<>();
		public String resourceType;
		public int resource;
		public int maxResource;
		public int overflowRemainder;
		public final LinkedHashMap<String, ResourceState> resourcePools = new LinkedHashMap<>();
		public final ArrayList<String> buffs = new ArrayList<>();
		public final ArrayList<String> marks = new ArrayList<>();
		public final ArrayList<String> vocabularies = new ArrayList<>();
		public final ArrayList<String> equippedItems = new ArrayList<>();
		public final ArrayList<RuleState> activeRules = new ArrayList<>();
		public final ArrayList<DelayedState> delayedPayloads = new ArrayList<>();
		public final ArrayList<String> restrictions = new ArrayList<>();
	}

	public static class ResourceState {
		public String engine;
		public int value;
		public int max;
		public int overflowRemainder;
	}

	public static class RuleState {
		public String id;
		public String event;
		public int runtimeOrder;
		public float cooldown;
		public int triggerCount;
	}

	public static class DelayedState {
		public long id;
		public int remainingTurns;
		public String effect;
	}

	public static class MobState {
		public String type;
		public int id;
		public int pos;
		public int hp;
		public int maxHp;
		public final ArrayList<String> buffs = new ArrayList<>();
		public final ArrayList<String> marks = new ArrayList<>();
	}

	public static QaSnapshot capture(int turn, Hero hero) {
		QaSnapshot snapshot = new QaSnapshot();
		snapshot.turn = turn;
		snapshot.depth = Dungeon.depth;
		snapshot.actorCount = Actor.all().size();
		snapshot.hero = heroState(hero);
		if (Dungeon.level != null) {
			addTile(snapshot, hero.pos);
			for (Mob mob : Dungeon.level.mobs) {
				if (!mob.isAlive()) continue;
				snapshot.mobs.add(mobState(mob));
				addTile(snapshot, mob.pos);
			}
		}
		Collections.sort(snapshot.mobs, (a, b) -> Integer.compare(a.id, b.id));
		return snapshot;
	}

	private static HeroState heroState(Hero hero) {
		HeroState state = new HeroState();
		state.id = hero.id();
		state.pos = hero.pos;
		state.hp = hero.HP;
		state.maxHp = hero.HT;
		state.shield = hero.shielding();
		for (Buff buff : hero.buffs()) {
			state.buffs.add(buff.getClass().getSimpleName());
			if (buff instanceof RuleMark) state.marks.add(markState((RuleMark)buff));
		}
		Collections.sort(state.buffs);
		Collections.sort(state.marks);
		addItem(state.equippedItems, hero.belongings.weapon);
		addItem(state.equippedItems, hero.belongings.armor);
		addItem(state.equippedItems, hero.belongings.artifact);
		addItem(state.equippedItems, hero.belongings.misc);
		addItem(state.equippedItems, hero.belongings.ring);
		RuleRuntime runtime = hero.ruleRuntime();
		if (runtime != null) {
			state.customName = runtime.customName();
			state.classLaw = runtime.law() == null ? null : runtime.law().name();
			for (ClassLaw law : runtime.laws()) state.classLaws.add(law.name());
			ResourceSpec primarySpec = runtime.primaryResourceSpec();
			state.resourceType = primarySpec == null ? null : primarySpec.id;
			state.resource = runtime.resource();
			state.maxResource = runtime.maxResource();
			state.overflowRemainder = runtime.overflowRemainder();
			ClassBuild blueprint = runtime.presentationBuild();
			if (runtime.engine() != null) {
				ResourceState primary = new ResourceState();
				primary.engine = runtime.engine().name();
				primary.value = runtime.resource();
				primary.max = runtime.maxResource();
				primary.overflowRemainder = runtime.overflowRemainder();
				String id = blueprint.primaryResource() == null ? runtime.engine().name().toLowerCase()
						: blueprint.primaryResource().id;
				state.resourcePools.put(id, primary);
			}
			for (RuleResourceState value : runtime.additionalResourceStates()) {
				ResourceState extra = new ResourceState();
				extra.engine = value.engine.name();
				extra.value = value.value;
				extra.max = value.max;
				extra.overflowRemainder = value.overflowRemainder;
				state.resourcePools.put(value.id, extra);
			}
			for (CoreRuleVocabulary vocabulary : runtime.vocabularies()) state.vocabularies.add(vocabulary.name());
			for (RuleDefinition rule : runtime.rules()) {
				RuleState ruleState = new RuleState();
				ruleState.id = rule.id;
				ruleState.event = rule.trigger.event.name();
				ruleState.runtimeOrder = rule.runtimeOrder();
				ruleState.cooldown = rule.cooldownRemaining();
				ruleState.triggerCount = rule.triggerCount();
				state.activeRules.add(ruleState);
			}
			for (RuleDelayedPayload payload : runtime.delayedPayloads()) {
				DelayedState delayed = new DelayedState();
				delayed.id = payload.payloadId();
				delayed.remainingTurns = payload.remainingTurns();
				delayed.effect = payload.effectId();
				state.delayedPayloads.add(delayed);
			}
			for (Restriction restriction : Restriction.values()) {
				if (runtime.hasRestriction(restriction)) state.restrictions.add(restriction.name());
			}
		}
		return state;
	}

	private static MobState mobState(Mob mob) {
		MobState state = new MobState();
		state.type = mob.getClass().getSimpleName();
		state.id = mob.id();
		state.pos = mob.pos;
		state.hp = mob.HP;
		state.maxHp = mob.HT;
		for (Buff buff : mob.buffs()) {
			state.buffs.add(buff.getClass().getSimpleName());
			if (buff instanceof RuleMark) state.marks.add(markState((RuleMark)buff));
		}
		Collections.sort(state.buffs);
		Collections.sort(state.marks);
		return state;
	}

	private static String markState(RuleMark mark) {
		return mark.mark().name() + ":" + mark.stacks() + ":" + mark.sourceId()
				+ ":" + mark.ownerId() + ":" + mark.transferDepth() + ":" + mark.remainingTurns();
	}

	private static void addItem(ArrayList<String> values, Item item) {
		if (item != null) values.add(item.getClass().getSimpleName());
	}

	private static void addTile(QaSnapshot snapshot, int cell) {
		if (cell >= 0 && cell < Dungeon.level.length()) snapshot.levelTiles.put(Integer.toString(cell), Dungeon.level.map[cell]);
	}

	public boolean sameSavedRuleState(QaSnapshot other) {
		if (other == null || hero == null || other.hero == null) return false;
		if (hero.hp != other.hero.hp || hero.maxHp != other.hero.maxHp || hero.pos != other.hero.pos
				|| hero.resource != other.hero.resource || hero.maxResource != other.hero.maxResource
				|| hero.overflowRemainder != other.hero.overflowRemainder
				|| !equals(hero.resourceType, other.hero.resourceType)
				|| !equals(hero.customName, other.hero.customName)
				|| !equals(hero.classLaw, other.hero.classLaw)
				|| !hero.classLaws.equals(other.hero.classLaws)
				|| !sameResources(hero.resourcePools, other.hero.resourcePools)
				|| !hero.restrictions.equals(other.hero.restrictions)
				|| !hero.vocabularies.equals(other.hero.vocabularies)
				|| !hero.marks.equals(other.hero.marks)
				|| hero.activeRules.size() != other.hero.activeRules.size()
				|| hero.delayedPayloads.size() != other.hero.delayedPayloads.size()) return false;
		for (int i = 0; i < hero.activeRules.size(); i++) {
			RuleState a = hero.activeRules.get(i);
			RuleState b = other.hero.activeRules.get(i);
			if (!equals(a.id, b.id) || !equals(a.event, b.event) || a.runtimeOrder != b.runtimeOrder
					|| Math.abs(a.cooldown - b.cooldown) > 0.001f || a.triggerCount != b.triggerCount) return false;
		}
		for (int i = 0; i < hero.delayedPayloads.size(); i++) {
			DelayedState a = hero.delayedPayloads.get(i);
			DelayedState b = other.hero.delayedPayloads.get(i);
			if (a.id != b.id || a.remainingTurns != b.remainingTurns || !equals(a.effect, b.effect)) return false;
		}
		return true;
	}

	private static boolean sameResources(LinkedHashMap<String, ResourceState> a,
			LinkedHashMap<String, ResourceState> b) {
		if (a.size() != b.size() || !a.keySet().equals(b.keySet())) return false;
		for (String key : a.keySet()) {
			ResourceState x = a.get(key), y = b.get(key);
			if (y == null || !equals(x.engine, y.engine) || x.value != y.value || x.max != y.max
					|| x.overflowRemainder != y.overflowRemainder) return false;
		}
		return true;
	}

	private static boolean equals(Object a, Object b) { return a == null ? b == null : a.equals(b); }
}
