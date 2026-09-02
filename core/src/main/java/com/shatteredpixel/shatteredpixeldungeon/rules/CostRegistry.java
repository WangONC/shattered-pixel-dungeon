package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;
import java.util.List;

/** Builds concrete player costs from the resources and states declared by the current ClassBuild. */
public final class CostRegistry {
	private static final RuleMark.Type[] PLAYER_COST_STATES = {
			RuleMark.Type.HUNTED, RuleMark.Type.ACCUMULATION, RuleMark.Type.CHARGED
	};

	public static final class Entry {
		public final RuleCost cost;
		public final String id;
		private Entry(String id, RuleCost cost) { this.id = id; this.cost = cost; }
		public String name(ClassBuild build) { return describe(cost, build); }
		public String summary() { return Messages.get(CostRegistry.class, id + "_summary"); }
	}
	private CostRegistry() {}
	public static List<Entry> exposed(ClassBuild build) {
		ArrayList<Entry> values = new ArrayList<>();
		values.add(new Entry("none", new RuleCost(RuleCost.Type.NONE, 0)));
		for(int amount=1;amount<=6;amount++) values.add(new Entry("hp",new RuleCost(RuleCost.Type.HP,amount)));
		for(int amount=1;amount<=3;amount++) values.add(new Entry("action",new RuleCost(RuleCost.Type.ACTION,amount)));
		for(int amount=2;amount<=10;amount++) values.add(new Entry("cooldown",new RuleCost(RuleCost.Type.COOLDOWN,amount)));
		for(int amount=1;amount<=3;amount++){RuleCost consumable=new RuleCost(RuleCost.Type.CONSUMABLE,amount);
			consumable.reference=PotionOfHealing.class.getName();values.add(new Entry("consumable",consumable));}
		for (RuleMark.Type mark : PLAYER_COST_STATES) for (int amount = 1; amount <= 3; amount++) {
			RuleCost state = new RuleCost(RuleCost.Type.STATE, amount);
			state.reference = mark.name();
			values.add(new Entry("state", state));
		}
		for (ResourceSpec resource : build.resources) {
			for(int amount=1;amount<=6;amount++){RuleCost cost=new RuleCost(RuleCost.Type.RESOURCE,amount);
				cost.resourceId=resource.id;cost.resourceEngine=resource.engine;values.add(new Entry("resource",cost));}
		}
		if (build.resources.isEmpty()) for(int amount=1;amount<=6;amount++){
			RuleCost pending=new RuleCost(RuleCost.Type.RESOURCE,amount);
			pending.resourceId=""; pending.resourceEngine=ResourceEngine.MANUAL;
			values.add(new Entry("resource",pending));
		}
		return values;
	}
	public static String describe(RuleCost cost, ClassBuild build) {
		if (cost == null || cost.type == RuleCost.Type.NONE) return Messages.get(CostRegistry.class, "none_name");
		switch (cost.type) {
			case HP: return Messages.get(CostRegistry.class, "hp_name", cost.amount);
			case ACTION: return Messages.get(CostRegistry.class, "action_name", cost.amount);
			case COOLDOWN: return Messages.get(CostRegistry.class, "cooldown_name", cost.amount);
			case CONSUMABLE: return Messages.get(CostRegistry.class, "consumable_name", cost.amount);
			case STATE:
				RuleMark.Type mark;
				try { mark = RuleMark.Type.valueOf(cost.reference); }
				catch (Exception ignored) { mark = RuleMark.Type.CHARGED; }
				return Messages.get(CostRegistry.class, "state_name", cost.amount,
						Messages.get(RuleMark.class, mark.name().toLowerCase() + "_name"));
			case RESOURCE:
				ResourceSpec resource = build == null ? null : build.resource(cost.resourceId);
				return Messages.get(CostRegistry.class, "resource_name", cost.amount,
						resource == null ? Messages.get(CostRegistry.class, "resource_generic") : resource.displayName());
			default: return Messages.get(CostRegistry.class, "none_name");
		}
	}
	public static boolean compatible(Entry entry, ClassBuild build, SkillSpec skill) {
		if (entry.cost.type == RuleCost.Type.ACTION && skill.activation != RuleEvent.ACTIVE) return false;
		if (entry.cost.type == RuleCost.Type.COOLDOWN && skill.constraint.variant == SkillConstraint.Variant.COOLDOWN) return false;
		return entry.cost.referenceValid();
	}
}
