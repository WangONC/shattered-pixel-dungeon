package com.shatteredpixel.shatteredpixeldungeon.rules;

/**
 * Single authority for the player, runtime, migration and QA class budget baseline.
 * Progression bonuses and explicit restriction rebates are applied by {@link ClassBuild}.
 */
public final class ClassBudgetPolicy {
	public static final int FOUNDATION_BUDGET = 35;
	public static final int LEGACY_FIXED_SLOT_BUDGET = 16;

	private ClassBudgetPolicy() {}

	public static int newBuildBudget() {
		return FOUNDATION_BUDGET;
	}

	/** Old fixed-slot capacity was a schema limit, not a permanent property of the migrated class. */
	public static int migrateLegacyBudget(int ignoredLegacyBudget) {
		return FOUNDATION_BUDGET;
	}

	/**
	 * The foundation budget is policy, not save-owned balance data. Current schemas always load the
	 * same baseline; progression and explicit restrictions remain the only saved ways to change it.
	 */
	public static int migrateClassBuildBudget(int schemaVersion, int savedBudget) {
		return FOUNDATION_BUDGET;
	}

	/** Single pricing entry for every independently editable class-level gameplay component. */
	public static int gameplayComponentCost(ClassGameplayComponentSpec value, ClassBuild build) {
		if (value == null || value.type == null) return 0;
			switch (value.type) {
			case BASIC_ATTACK: return value.basicAttack == null ? 0 : value.basicAttack.budgetCost(build);
			case RESOURCE_FLOW:
				ResourceFlowSpec flow = value.asResourceFlow();
				if (value.resourceOperation == ResourceFlowSpec.Operation.CONVERT) {
					return value.targetAmount > value.amount ? 2 : 1;
				}
				// Gain is power; decay/clear is a real class-wide tradeoff and must actually return
				// budget.  Keeping both positive would make a Rage/Momentum recipe pay extra for
				// its own downside and violate the shared constraint policy.
				return flow.gain() ? flow.nominalPower() : -flow.rebate();
			case ACTIVE_REFILL:
				return (value.actionTime <= 1f ? 1 : 0) + (value.amount <= 0 || value.amount >= 4 ? 1 : 0);
			case OWNERSHIP: return 1;
			case ENTITY_CAPACITY: return Math.max(0, value.capacity - 1);
			case PERSISTENCE: return Math.max(1, value.lifetime / 5);
			case COMMAND: return Math.max(2, 4 - Math.max(0, (int)value.actionTime - 1));
			case RECYCLE: return Math.max(0, 1 + Math.max(0, value.amount - 1)
					- Math.max(0, (int)value.actionTime - 1));
			case MODE_ENGINE: return Math.max(6, 10 + Math.max(0, value.modes.size() - 2)
					- Math.max(0, (int)value.actionTime - 1));
			case GLOBAL_CONSTRAINT: return value.restriction == null ? 0 : -value.restriction.capacityBonus;
			default: return 0;
		}
	}
}
