package com.shatteredpixel.shatteredpixeldungeon.rules;

/** MIGRATION_ONLY adapter from the removed fixed-slot save schema into authoritative ClassBuild. */
public final class ClassBuildMigrator {
	private ClassBuildMigrator() {}

	public static ClassBuild fromLegacy(CustomClassConfig legacy) {
		ClassBuild result = new ClassBuild();
		result.name = legacy.name;
		result.baseBudget = ClassBudgetPolicy.migrateLegacyBudget(CustomClassConfig.BASE_CAPACITY
				+ (legacy.restriction == null ? 0 : legacy.restriction.capacityBonus));
		if (legacy.resource != null) result.resources.add(new ResourceSpec(legacy.resource));
		migrateLaw(result, legacy.law);
		migrateTrait(result, legacy.vocabulary1);
		if (legacy.vocabulary2 != null && legacy.vocabulary2 != CoreRuleVocabulary.NONE
				&& legacy.vocabulary2 != legacy.vocabulary1) migrateTrait(result, legacy.vocabulary2);
		if (legacy.restriction != null && legacy.restriction != Restriction.NONE) result.restrictions.add(legacy.restriction);
		ResourceEngine engine = legacy.resource;
		result.skills.add(SkillSpec.fromRule(legacy.buildActiveRuleLegacy(), engine));
		result.skills.add(SkillSpec.fromRule(legacy.buildReactionRuleLegacy(), engine));
		if (result.restrictions.contains(Restriction.NO_ORDINARY_WEAPONS)) {
			result.startingKit.mode = StartingKitSpec.Mode.UNARMED;
		}
		return result;
	}

	private static void migrateLaw(ClassBuild result, ClassLaw value) {
		if (value == null) return;
		String resource = result.resources.isEmpty() ? "" : result.resources.get(0).id;
		switch (value) {
			case RESOURCE_OVERFLOW_TO_SHIELD: result.traits.add(TraitSpec.resource(CoreRuleVocabulary.OVERFLOW, resource)); break;
			case STATUS_ABSORPTION: result.traits.add(TraitSpec.resource(CoreRuleVocabulary.STATUS_FEEDBACK, resource)); break;
			case WATER_AFFINITY: result.traits.add(TraitSpec.resource(CoreRuleVocabulary.WATER_FLOW, resource)); break;
			case KILL_ACCELERATES_RULES: result.traits.add(TraitSpec.of(CoreRuleVocabulary.KILL_TEMPO)); break;
			default: result.laws.add(value);
		}
	}

	private static void migrateTrait(ClassBuild result, CoreRuleVocabulary value) {
		if (value == null || value == CoreRuleVocabulary.NONE) return;
		switch (value) {
			case OVERDRAW: result.laws.add(ClassLaw.RESOURCE_OVERDRAFT_USES_HP); break;
			case INERTIA_BRIDGE: result.laws.add(ClassLaw.FORCED_MOVEMENT_COUNTS_AS_MOVE); break;
			case TRANSLOCATION_BRIDGE: result.laws.add(ClassLaw.TRANSLOCATION_COUNTS_AS_ENTER_TILE); break;
			default: result.traits.add(TraitSpec.of(value));
		}
	}

}
