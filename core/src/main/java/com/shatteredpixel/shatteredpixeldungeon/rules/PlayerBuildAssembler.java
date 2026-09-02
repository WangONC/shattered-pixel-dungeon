package com.shatteredpixel.shatteredpixeldungeon.rules;

import java.util.ArrayList;

/**
 * Auditable equivalent of player Builder selections.  It has no QA-only components: every accepted
 * value must be exposed by the same registries and parameter ranges used by WndCreateClass.
 */
public final class PlayerBuildAssembler {
	private final ClassBuild build = new ClassBuild();
	private final ArrayList<String> failures = new ArrayList<>();

	public PlayerBuildAssembler(String name) { build.name = name; }
	public ClassBuild build() { build.resolvePendingBindings(); build.syncClassOperations(); return build; }
	public ArrayList<String> failures() { return new ArrayList<>(failures); }

	public PlayerBuildAssembler baseBudget(int value) {
		if (value != ClassBudgetPolicy.newBuildBudget()) failures.add("BUDGET_AUTHORITY:" + value);
		else build.baseBudget = value;
		return this;
	}

	public PlayerBuildAssembler startingKit(StartingKitSpec value) {
		if (value == null || value.mode == null) failures.add("STARTING_KIT");
		else build.startingKit = value.copy();
		return this;
	}

	public PlayerBuildAssembler addResource(ResourceSpec value) {
		if (!resourceExposed(value)) failures.add("RESOURCE:" + (value == null ? "null" : value.id));
		else if (value.hasLegacyEconomy()) ResourceRegistry.migrateLegacy(value).addTo(build);
		else {
			ResourceSpec copy = value.copy();
			copy.current = copy.initialValue;
			build.resources.add(copy);
		}
		return this;
	}

	/** The same recipe expansion used by the normal Builder's "quick preset" list. */
	public PlayerBuildAssembler addResourcePreset(ResourceRegistry.Preset preset, String id) {
		if (preset == null || !ResourceRegistry.presets().contains(preset)) {
			failures.add("RESOURCE_PRESET:" + preset);
			return this;
		}
		ResourceRegistry.Recipe recipe = preset.createRecipe(id);
		addResource(recipe.pool);
		for (ClassGameplayComponentSpec component : recipe.components) addGameplayComponent(component);
		return this;
	}

	public PlayerBuildAssembler basicAttack(BasicAttackProfile value) {
		if (value == null) failures.add("BASIC_ATTACK");
		else {
			for (int i = build.gameplayComponents.size() - 1; i >= 0; i--) if (build.gameplayComponents.get(i).type
					== ClassGameplayComponentSpec.Type.BASIC_ATTACK) build.gameplayComponents.remove(i);
			build.gameplayComponents.add(0, ClassGameplayComponentSpec.basicAttack(value));
		}
		return this;
	}

	public PlayerBuildAssembler activeRefill(String resourceId, int amount, float actionTime) {
		ResourceSpec pool = build.resource(resourceId);
		if (pool == null || amount < 0 || amount > pool.capacity || actionTime < 1f) failures.add("ACTIVE_REFILL:" + resourceId);
		else addGameplayComponent(ClassGameplayComponentSpec.activeRefill("refill_" + resourceId,
				resourceId, amount, actionTime));
		return this;
	}

	public PlayerBuildAssembler ownership(boolean enabled) {
		removeType(ClassGameplayComponentSpec.Type.OWNERSHIP);
		if (enabled) addGameplayComponent(new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.OWNERSHIP, "ownership"));
		return this;
	}
	public PlayerBuildAssembler entityCapacity(int value) {
		removeId("actor_capacity"); if (value < 0 || value > 6) failures.add("ENTITY_CAPACITY:" + value);
		else if (value > 0) { ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.ENTITY_CAPACITY,"actor_capacity");c.entityFilter=ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR;c.capacity=value;addGameplayComponent(c); } return this;
	}
	public PlayerBuildAssembler deviceCapacity(int value) {
		removeId("device_capacity"); if (value < 0 || value > 6) failures.add("DEVICE_CAPACITY:" + value);
		else if (value > 0) { ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.ENTITY_CAPACITY,"device_capacity");c.entityFilter=ClassGameplayComponentSpec.EntityFilter.OWNED_DEVICE;c.capacity=value;addGameplayComponent(c); } return this;
	}
	public PlayerBuildAssembler command(boolean enabled) { removeType(ClassGameplayComponentSpec.Type.COMMAND);if(enabled)addGameplayComponent(new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.COMMAND,"command"));return this; }
	public PlayerBuildAssembler recycle(String resourceId, int amount) {
		removeType(ClassGameplayComponentSpec.Type.RECYCLE);ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.RECYCLE,"recycle");c.resourceId=resourceId==null?"":resourceId;c.amount=Math.max(1,amount);addGameplayComponent(c);return this;
	}
	public PlayerBuildAssembler modeEngine(String... modes) {
		removeType(ClassGameplayComponentSpec.Type.MODE_ENGINE);ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.MODE_ENGINE,"mode_engine");if(modes!=null)for(String mode:modes)if(mode!=null&&!mode.trim().isEmpty())c.modes.add(mode.trim());
		if(c.modes.size()<2)failures.add("MODE_ENGINE");else addGameplayComponent(c);return this;
	}

	public PlayerBuildAssembler addGameplayComponent(ClassGameplayComponentSpec value) {
		if (!GameplayComponentRegistry.playerExposed(value)) failures.add("COMPONENT:" + (value == null ? "null" : value.id));
		else build.gameplayComponents.add(value.copy());
		return this;
	}

	private void removeType(ClassGameplayComponentSpec.Type type){for(int i=build.gameplayComponents.size()-1;i>=0;i--)if(build.gameplayComponents.get(i).type==type)build.gameplayComponents.remove(i);}
	private void removeId(String id){for(int i=build.gameplayComponents.size()-1;i>=0;i--)if(id.equals(build.gameplayComponents.get(i).id))build.gameplayComponents.remove(i);}

	public PlayerBuildAssembler addSkill(SkillSpec value) {
		if (!skillExposed(build, value)) failures.add("SKILL:" + (value == null ? "null" : value.id));
		else build.skills.add(value.copy());
		return this;
	}

	public PlayerBuildAssembler addLaw(ClassLaw value) {
		ComponentDependency dependency = LawTraitRegistry.lawDependency(build, value);
		if (!dependency.playerSelectable()) failures.add("LAW:" + value);
		else build.laws.add(value);
		return this;
	}

	public PlayerBuildAssembler addTrait(TraitSpec value) {
		boolean found = false;
		for (LawTraitRegistry.TraitOption option : LawTraitRegistry.traitOptions(build)) {
			if (option.playerSelectable() && option.spec.stableId().equals(value.stableId())) { found = true; break; }
		}
		if (!found) failures.add("TRAIT:" + (value == null ? "null" : value.stableId()));
		else build.traits.add(value.copy());
		return this;
	}

	public PlayerBuildAssembler addRestriction(Restriction value) {
		if (value == null || !RestrictionRegistry.exposed().contains(value)) failures.add("RESTRICTION:" + value);
		else { ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.GLOBAL_CONSTRAINT,"constraint_"+value.name().toLowerCase());c.restriction=value;addGameplayComponent(c); }
		return this;
	}

	/** MIGRATION_ONLY fixture helper. Player-path QA uses addResourcePreset or a generic pool. */
	@Deprecated public static ResourceSpec preset(ResourceEngine engine, String id, String name) {
		if (!ResourceRegistry.exposed().contains(engine)) throw new IllegalArgumentException("resource not exposed");
		ResourceSpec result = new ResourceSpec(engine); result.id = id;
		if (name != null && !name.trim().isEmpty()) result.name = name.trim();
		return result;
	}

	public static boolean resourceExposed(ResourceSpec value) {
		return value != null && value.valid() && value.minimum >= 0 && value.capacity >= 1
				&& value.capacity <= 30 && value.initialValue >= value.minimum && value.initialValue <= value.capacity;
	}

	public static boolean skillExposed(ClassBuild build, SkillSpec skill) {
		if (skill == null || skill.primary == null || !EffectVocabularyRegistry.exposed(skill.primary.operation)
				|| !EffectVocabularyRegistry.playerReachable(skill.primary, build)) return false;
		if (skill.secondary != null && (!EffectVocabularyRegistry.exposed(skill.secondary.operation)
				|| !EffectVocabularyRegistry.playerReachable(skill.secondary, build))) return false;
		if (!DeliveryRegistry.exposed().contains(skill.delivery)
				|| !DeliveryRegistry.parametersValid(skill)
				|| !TargetingRegistry.SELECTORS.contains(skill.targeting.selector)
				|| !TargetingRegistry.COVERAGES.contains(skill.targeting.coverage)
				|| !TargetingRegistry.FILTERS.contains(skill.targeting.filter)
				|| skill.targeting.range < 1 || skill.targeting.range > 10
				|| skill.targeting.magnitude < 1 || skill.targeting.magnitude > 4
				|| skill.targeting.maxTargets < 1 || skill.targeting.maxTargets > 8) return false;
		boolean modifier = false; for (RuleModifier value : ModifierRegistry.options()) if (same(value, skill.modifier)) modifier = true;
		boolean cost = false; for (CostRegistry.Entry value : CostRegistry.exposed(build)) if (same(value.cost, skill.cost)) cost = true;
		boolean constraint = false; for (SkillConstraint value : ConstraintRegistry.exposed())
			if (value.variant == skill.constraint.variant && value.parameter == skill.constraint.parameter) constraint = true;
		return modifier && cost && constraint && skill.structurallyValid();
	}

	public static PlayerBuildAssembler reconstruct(ClassBuild source) {
		source.normalizeLegacyComponents();
		PlayerBuildAssembler result = new PlayerBuildAssembler(source.name).baseBudget(source.baseBudget).startingKit(source.startingKit);
		result.build.gameplayComponents.clear();
		for (ResourceSpec value : source.resources) result.addResource(value);
		for (ClassGameplayComponentSpec value : source.gameplayComponents) result.addGameplayComponent(value);
		for (SkillSpec value : source.skills) result.addSkill(value);
		for (ClassLaw value : source.laws) result.addLaw(value);
		for (TraitSpec value : source.traits) result.addTrait(value);
		for (Restriction value : source.allRestrictions()) if (!containsConstraint(result.build, value)) result.addRestriction(value);
		result.build.progression = source.progression.copy();
		return result;
	}

	private static boolean same(RuleModifier a, RuleModifier b) {
		return a != null && b != null && a.type == b.type && a.magnitude == b.magnitude;
	}

	private static boolean same(RuleCost a, RuleCost b) {
		return a != null && b != null && a.type == b.type && a.amount == b.amount
				&& same(a.resourceId, b.resourceId) && same(a.reference, b.reference);
	}

	private static boolean same(String a, String b) { return a == null ? b == null : a.equals(b); }
	private static boolean containsConstraint(ClassBuild build,Restriction value){for(ClassGameplayComponentSpec c:build.gameplayComponents)if(c.type==ClassGameplayComponentSpec.Type.GLOBAL_CONSTRAINT&&c.restriction==value)return true;return false;}
}
