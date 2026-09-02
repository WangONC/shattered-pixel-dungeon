package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/** Versioned, slot-free custom class blueprint. Runtime instances remain RuleDefinitions. */
public class ClassBuild implements Bundlable {
	public static final int SCHEMA_VERSION = 5;
	public static final int BASE_BUDGET = ClassBudgetPolicy.FOUNDATION_BUDGET;

	public int schemaVersion = SCHEMA_VERSION;
	public String name = "custom";
	public int baseBudget = ClassBudgetPolicy.newBuildBudget();
	public final ArrayList<ResourceSpec> resources = new ArrayList<>();
	/** Authoritative, slot-free class-level mechanisms. */
	public final ArrayList<ClassGameplayComponentSpec> gameplayComponents = new ArrayList<>();
	public final ArrayList<SkillSpec> skills = new ArrayList<>();
	public final ArrayList<ClassLaw> laws = new ArrayList<>();
	public final ArrayList<TraitSpec> traits = new ArrayList<>();
	public final ArrayList<Restriction> restrictions = new ArrayList<>();
	public final ArrayList<ClassOperationSpec> operations = new ArrayList<>();
	public StartingKitSpec startingKit = new StartingKitSpec();
	public ClassProgression progression = new ClassProgression();

	public ClassBuild() {
		gameplayComponents.add(ClassGameplayComponentSpec.basicAttack(BasicAttackProfile.WEAK));
	}

	public ClassBuild copy() {
		ClassBuild result = new ClassBuild();
		result.schemaVersion = schemaVersion;
		result.name = name;
		result.baseBudget = baseBudget;
		for (ResourceSpec value : resources) result.resources.add(value.copy());
		result.gameplayComponents.clear();
		for (ClassGameplayComponentSpec value : gameplayComponents) if (value != null) result.gameplayComponents.add(value.copy());
		// Old/corrupt saves can contain sparse collections.  The action HUD asks the
		// runtime for a defensive copy, so a null entry must not turn HUD rebuild into
		// a crash before normal build validation gets a chance to report the problem.
		for (SkillSpec value : skills) if (value != null) result.skills.add(value.copy());
		result.laws.addAll(laws);
		for (TraitSpec value : traits) result.traits.add(value.copy());
		result.restrictions.addAll(restrictions);
		for (ClassOperationSpec value : operations) if (value != null) result.operations.add(value.copy());
		result.startingKit = startingKit.copy();
		result.progression = progression.copy();
		return result;
	}

	public ArrayList<RuleDefinition> compileRules() {
		ArrayList<RuleDefinition> result = new ArrayList<>();
		for (SkillSpec skill : skills) if (skill != null) result.add(skill.compile());
		return result;
	}

	public ResourceSpec primaryResource() { return resources.isEmpty() ? null : resources.get(0); }

	public ResourceSpec resource(String id) {
		for (ResourceSpec value : resources) if (value != null && value.id.equals(id)) return value;
		return null;
	}

	/** Rename presentation only. Stable-id references intentionally remain untouched. */
	public boolean renameResource(String id, String displayName) {
		ResourceSpec value = resource(id);
		if (value == null || displayName == null || displayName.trim().isEmpty()) return false;
		value.name = displayName.trim();
		return true;
	}

	/**
	 * Remove only the pool declaration. References are preserved so validation can report an
	 * UNRESOLVED dependency and the player can repair or re-create the pool without silent data loss.
	 */
	public boolean removeResource(String id) {
		for (int i = 0; i < resources.size(); i++) if (resources.get(i) != null
				&& resources.get(i).id.equals(id)) {
			resources.remove(i);
			syncClassOperations();
			return true;
		}
		return false;
	}

	public ClassGameplayComponentSpec gameplayComponent(String id) {
		for (ClassGameplayComponentSpec value : gameplayComponents) if (value != null && value.id.equals(id)) return value;
		return null;
	}

	public boolean hasGameplayComponent(ClassGameplayComponentSpec.Type type) {
		for (ClassGameplayComponentSpec value : gameplayComponents) if (value != null && value.type == type) return true;
		return false;
	}

	public ArrayList<ClassGameplayComponentSpec> gameplayComponents(ClassGameplayComponentSpec.Type type) {
		ArrayList<ClassGameplayComponentSpec> result = new ArrayList<>();
		for (ClassGameplayComponentSpec value : gameplayComponents) if (value != null && value.type == type) result.add(value);
		return result;
	}

	public BasicAttackProfile basicAttackProfile() {
		for (ClassGameplayComponentSpec value : gameplayComponents) if (value != null
				&& value.type == ClassGameplayComponentSpec.Type.BASIC_ATTACK) return value.basicAttack;
		return BasicAttackProfile.WEAK;
	}

	public ArrayList<String> modes() {
		for (ClassGameplayComponentSpec value : gameplayComponents) if (value != null
				&& value.type == ClassGameplayComponentSpec.Type.MODE_ENGINE) return new ArrayList<>(value.modes);
		return new ArrayList<>();
	}

	public boolean hasModeEngine() { return modes().size() >= 2; }

	public int ownedCapacity(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind kind) {
		ClassGameplayComponentSpec.EntityFilter wanted = kind == com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.ACTOR
				? ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR
				: kind == com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.FIELD
				? ClassGameplayComponentSpec.EntityFilter.OWNED_CARRIER
				: ClassGameplayComponentSpec.EntityFilter.OWNED_DEVICE;
		int result = 0;
		for (ClassGameplayComponentSpec value : gameplayComponents) if (value != null
				&& value.type == ClassGameplayComponentSpec.Type.ENTITY_CAPACITY
				&& (value.entityFilter == wanted || value.entityFilter == ClassGameplayComponentSpec.EntityFilter.OWNED_ENTITY)) {
			result += value.capacity;
		}
		return result;
	}

	public int persistenceLifetime(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind kind) {
		ClassGameplayComponentSpec.EntityFilter wanted = kind == com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.ACTOR
				? ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR
				: kind == com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.FIELD
				? ClassGameplayComponentSpec.EntityFilter.OWNED_CARRIER
				: ClassGameplayComponentSpec.EntityFilter.OWNED_DEVICE;
		int result = 0;
		for (ClassGameplayComponentSpec value : gameplayComponents) if (value != null
				&& value.type == ClassGameplayComponentSpec.Type.PERSISTENCE
				&& (value.entityFilter == wanted || value.entityFilter == ClassGameplayComponentSpec.EntityFilter.OWNED_ENTITY)) {
			result = result == 0 ? value.lifetime : Math.min(result, value.lifetime);
		}
		return result;
	}

	public ClassOperationSpec operation(String id) {
		for (ClassOperationSpec value : operations) if (value != null && value.id.equals(id)) return value;
		return null;
	}

	/** Keep generated operation identity in sync without discarding a player-edited display name. */
	public void syncClassOperations() {
		ArrayList<ClassOperationSpec> desired = new ArrayList<>();
		for (ClassGameplayComponentSpec component : gameplayComponents) {
			if (component == null) continue;
			if (component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL) {
				ClassOperationSpec op = generatedOperation(ClassOperationSpec.Type.RELOAD,
						operationId("reload_" + component.resourceId, component, desired));
				op.sourceComponentId = component.id; op.resourceId = component.resourceId;
				op.amount = component.amount; op.actionTime = component.actionTime; desired.add(op);
			} else if (component.type == ClassGameplayComponentSpec.Type.COMMAND) {
				ClassOperationSpec op = generatedOperation(ClassOperationSpec.Type.COMMAND,
						operationId("command", component, desired));
				op.sourceComponentId = component.id; op.entityFilter = component.entityFilter;
				op.actionTime = component.actionTime; desired.add(op);
			} else if (component.type == ClassGameplayComponentSpec.Type.MODE_ENGINE) {
				ClassOperationSpec op = generatedOperation(ClassOperationSpec.Type.MODE_SWITCH,
						operationId("mode_switch", component, desired));
				op.sourceComponentId = component.id; op.actionTime = component.actionTime; desired.add(op);
			} else if (component.type == ClassGameplayComponentSpec.Type.RECYCLE) {
				ClassOperationSpec op = generatedOperation(ClassOperationSpec.Type.RECYCLE,
						operationId("recycle", component, desired));
				op.sourceComponentId = component.id; op.resourceId = component.resourceId;
				op.entityFilter = component.entityFilter; op.amount = component.amount;
				op.actionTime = component.actionTime; desired.add(op);
			}
		}
		operations.clear(); operations.addAll(desired);
	}

	private String operationId(String base, ClassGameplayComponentSpec component, ArrayList<ClassOperationSpec> desired) {
		for (ClassOperationSpec value : desired) if (base.equals(value.id)) return base + "_" + component.id;
		return base;
	}

	private ClassOperationSpec generatedOperation(ClassOperationSpec.Type type, String id) {
		ClassOperationSpec old = operation(id);
		ClassOperationSpec result = old == null ? new ClassOperationSpec(type, id) : old.copy();
		result.type = type; result.id = id; return result;
	}

	public boolean producesEffect(RuleEffect.Type type) {
		for (SkillSpec skill : skills) {
			if (skill.primary != null && skill.primary.variant == type) return true;
			if (skill.secondary != null && skill.secondary.variant == type) return true;
		}
		return false;
	}

	public boolean producesOperation(EffectSpec.Operation operation) {
		for (SkillSpec skill : skills) {
			if (skill.primary != null && skill.primary.operation == operation) return true;
			if (skill.secondary != null && skill.secondary.operation == operation) return true;
		}
		return false;
	}

	public boolean producesFamily(EffectFamily family) {
		for (SkillSpec skill : skills) {
			if (skill.primary != null && skill.primary.family == family) return true;
			if (skill.secondary != null && skill.secondary.family == family) return true;
		}
		return false;
	}

	public boolean producesMode(String modeId) {
		if (modeId == null || modeId.isEmpty()) return false;
		if (modes().contains(modeId)) return true;
		for (SkillSpec skill : skills) for (EffectSpec effect : new EffectSpec[]{skill.primary, skill.secondary}) {
			if (effect != null && effect.operation == EffectSpec.Operation.TRANSFORM_MODE
					&& modeId.equals(effect.stateId)) return true;
		}
		return false;
	}

	public int nominalUsedBudget() {
		normalizeLegacyComponents();
		int result = startingKit == null ? 0 : startingKit.powerCost();
		result += Math.max(0, resources.size() - 1);
		for (ResourceSpec resource : resources) result += resource.componentPowerCost();
		for (ClassGameplayComponentSpec component : gameplayComponents) if (component != null) {
			result += component.budgetCost(this);
		}
		for (ClassLaw law : laws) result += law.capacityCost;
		for (TraitSpec trait : traits) if (trait != null && trait.type != CoreRuleVocabulary.NONE) result += trait.budgetCost();
		for (SkillSpec skill : skills) result += skill.nominalPowerCost();
		return result;
	}

	public int constraintRebate() {
		int result = 0;
		for (SkillSpec skill : skills) if (skill.constraint != null) result += skill.constraint.effectiveRebate(this, skill);
		return result;
	}

	public int usedBudget() { return Math.max(0, nominalUsedBudget() - constraintRebate()); }

	public int maxBudget() {
		int result = baseBudget + (progression == null ? 0 : progression.budgetBonus);
		// Schema-4 restrictions are retained only until normalizeLegacyComponents promotes them.
		for (Restriction restriction : restrictions) result += restriction.capacityBonus;
		return result;
	}

	public boolean structurallyValid() {
		normalizeLegacyComponents();
		if (schemaVersion != SCHEMA_VERSION || name == null || name.trim().isEmpty()
				|| startingKit == null || progression == null) return false;
		HashSet<String> componentIds = new HashSet<>();
		int basicAttacks = 0;
		for (ClassGameplayComponentSpec component : gameplayComponents) {
			if (component == null || !component.structurallyValid() || !componentIds.add(component.id)
					|| !component.dependency(this).resolvedState()) return false;
			if (component.type == ClassGameplayComponentSpec.Type.BASIC_ATTACK) basicAttacks++;
		}
		if (basicAttacks != 1) return false;
		HashSet<String> ids = new HashSet<>();
		for (ResourceSpec resource : resources) {
			if (resource == null || !resource.valid()
					|| !ids.add(resource.id)) return false;
		}
		ids.clear();
		for (SkillSpec skill : skills) {
			if (skill == null || skill.id == null || skill.id.isEmpty() || !ids.add(skill.id)
					|| !skill.structurallyValid()) return false;
			if (skill.cost.type == RuleCost.Type.RESOURCE
					&& resource(skill.cost.resourceId) == null) return false;
			for (RuleCondition condition : skill.conditions) if (condition != null
					&& condition.type == RuleCondition.Type.RESOURCE_AT_LEAST
					&& resource(condition.reference) == null) return false;
			for (EffectSpec effect : new EffectSpec[]{skill.primary, skill.secondary}) if (effect != null) {
				if ((effect.family == EffectFamily.RESOURCE_OPERATION
						|| effect.family == EffectFamily.DAMAGE
						&& effect.scalingSource == EffectSpec.ScalingSource.CURRENT_RESOURCE)
						&& resource(effect.resourceId) == null) return false;
				if (effect.operation == EffectSpec.Operation.RESOURCE_CONVERT
						&& resource(effect.targetResourceId) == null) return false;
			}
		}
		HashSet<String> traitIds = new HashSet<>();
		for (TraitSpec trait : traits) {
			if (trait == null || trait.type == null || trait.type == CoreRuleVocabulary.NONE
					|| !traitIds.add(trait.stableId()) || !trait.bindingValid(this)) return false;
		}
		HashSet<String> operationIds = new HashSet<>();
		for (ClassOperationSpec operation : operations) {
			if (operation == null || operation.id == null || operation.id.isEmpty() || operation.type == null
					|| !operationIds.add(operation.id)) return false;
			if ((operation.type == ClassOperationSpec.Type.RELOAD || operation.type == ClassOperationSpec.Type.RECYCLE)
					&& resource(operation.resourceId) == null) return false;
		}
		return new HashSet<>(laws).size() == laws.size()
				&& new HashSet<>(restrictions).size() == restrictions.size();
	}

	public boolean budgetValid() { return usedBudget() <= maxBudget(); }
	public boolean valid() { return structurallyValid() && budgetValid(); }

	public ArrayList<Restriction> allRestrictions() {
		ArrayList<Restriction> result = new ArrayList<>(restrictions);
		for (ClassGameplayComponentSpec component : gameplayComponents) if (component != null
				&& component.type == ClassGameplayComponentSpec.Type.GLOBAL_CONSTRAINT
				&& component.restriction != null && component.restriction != Restriction.NONE
				&& !result.contains(component.restriction)) result.add(component.restriction);
		return result;
	}

	/**
	 * One-way compatibility adapter for schema-4 fixed fields and preset-owned resource economies.
	 * New saves contain only generic pools plus independent component specs.
	 */
	public void normalizeLegacyComponents() {
		if (!hasGameplayComponent(ClassGameplayComponentSpec.Type.BASIC_ATTACK)) {
			gameplayComponents.add(0, ClassGameplayComponentSpec.basicAttack(BasicAttackProfile.WEAK));
		}
		for (int i = 0; i < resources.size(); i++) {
			ResourceSpec resource = resources.get(i);
			if (resource == null || !resource.hasLegacyEconomy()) continue;
			if (resource.engine == ResourceEngine.BLOOD) {
				for (SkillSpec skill : skills) if (skill != null && skill.cost != null
						&& (skill.cost.resourceEngine == ResourceEngine.BLOOD
						|| resource.id.equals(skill.cost.resourceId))) {
					skill.cost.type = RuleCost.Type.HP; skill.cost.resourceId = ""; skill.cost.resourceEngine = null;
				}
				resources.remove(i--);
				continue;
			}
			boolean alreadyExpanded = false;
			for (ClassGameplayComponentSpec component : gameplayComponents) if (component != null
					&& (component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW
					|| component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL)
					&& resource.id.equals(component.resourceId)) { alreadyExpanded = true; break; }
			ResourceRegistry.Recipe recipe = ResourceRegistry.migrateLegacy(resource);
			resources.set(i, recipe.pool);
			if (!alreadyExpanded) for (ClassGameplayComponentSpec component : recipe.components) {
				if (gameplayComponent(component.id) == null) gameplayComponents.add(component);
			}
		}
		if (!restrictions.isEmpty()) {
			for (Restriction restriction : new ArrayList<>(restrictions)) {
				if (restriction == null || restriction == Restriction.NONE) continue;
				String id = "constraint_" + restriction.name().toLowerCase();
				if (gameplayComponent(id) == null) {
					ClassGameplayComponentSpec component = new ClassGameplayComponentSpec(
							ClassGameplayComponentSpec.Type.GLOBAL_CONSTRAINT, id);
					component.restriction = restriction; gameplayComponents.add(component);
				}
			}
			restrictions.clear();
		}
	}

	/** Resolve placeholder bindings after the player later adds the missing pool or mode. */
	public void resolvePendingBindings() {
		normalizeLegacyComponents();
		ResourceSpec first = resources.isEmpty() ? null : resources.get(0);
		for (ClassGameplayComponentSpec component : gameplayComponents) {
			if (component == null) continue;
			if ((component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW
					|| component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL
					|| component.type == ClassGameplayComponentSpec.Type.RECYCLE)
					&& (component.resourceId == null || component.resourceId.isEmpty()) && first != null) {
				component.resourceId = first.id;
			}
			if (component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW
					&& component.resourceOperation == ResourceFlowSpec.Operation.CONVERT
					&& (component.targetResourceId == null || component.targetResourceId.isEmpty())) {
				for (ResourceSpec resource : resources) if (!resource.id.equals(component.resourceId)) {
					component.targetResourceId = resource.id; break;
				}
			}
		}
		for (TraitSpec trait : traits) {
			if (trait == null || trait.type == null) continue;
			if (trait.type.requiresResource() && (trait.resourceId == null || trait.resourceId.isEmpty())
					&& !resources.isEmpty()) trait.resourceId = resources.get(0).id;
			if (trait.type.requiresMode() && (trait.stateId == null || trait.stateId.isEmpty())) {
				for (SkillSpec skill : skills) for (EffectSpec effect : new EffectSpec[]{skill.primary, skill.secondary}) {
					if (effect != null && effect.operation == EffectSpec.Operation.TRANSFORM_MODE) {
						trait.stateId = effect.stateId; break;
					}
					if (trait.stateId != null && !trait.stateId.isEmpty()) break;
				}
			}
		}
		for (SkillSpec skill : skills) {
			if (skill == null) continue;
			if (skill.cost != null && skill.cost.type == RuleCost.Type.RESOURCE
					&& (skill.cost.resourceId == null || skill.cost.resourceId.isEmpty()) && first != null) {
				skill.cost.resourceId = first.id; skill.cost.resourceEngine = null;
			}
			for (EffectSpec effect : new EffectSpec[]{skill.primary, skill.secondary}) {
				if (effect == null || effect.family != EffectFamily.RESOURCE_OPERATION) continue;
				if ((effect.resourceId == null || effect.resourceId.isEmpty()) && first != null) effect.resourceId = first.id;
				if (effect.operation == EffectSpec.Operation.RESOURCE_CONVERT
						&& (effect.targetResourceId == null || effect.targetResourceId.isEmpty())) {
					for (ResourceSpec resource : resources) if (!resource.id.equals(effect.resourceId)) {
						effect.targetResourceId = resource.id; break;
					}
				}
			}
			for (RuleCondition condition : skill.conditions) if (condition != null
					&& (condition.reference == null || condition.reference.isEmpty())) {
				if (condition.type == RuleCondition.Type.RESOURCE_AT_LEAST && first != null) {
					condition.reference = first.id;
				} else if (condition.type == RuleCondition.Type.MODE_IS) {
					if (!modes().isEmpty()) condition.reference = modes().get(0);
					else for (SkillSpec source : skills) for (EffectSpec effect : new EffectSpec[]{source.primary, source.secondary}) {
						if (effect != null && effect.operation == EffectSpec.Operation.TRANSFORM_MODE) { condition.reference = effect.stateId; break; }
						if (condition.reference != null && !condition.reference.isEmpty()) break;
					}
				}
			}
		}
		syncClassOperations();
	}

	@Override public void storeInBundle(Bundle bundle) {
		normalizeLegacyComponents();
		bundle.put("class_build_schema_version", schemaVersion);
		bundle.put("name", name);
		bundle.put("base_budget", baseBudget);
		bundle.put("resources", resources);
		bundle.put("gameplay_components", gameplayComponents);
		bundle.put("skills", skills);
		bundle.put("laws", names(laws));
		bundle.put("trait_specs", traits);
		bundle.put("restrictions", names(restrictions));
		bundle.put("class_operations", operations);
		bundle.put("starting_kit", startingKit);
		bundle.put("progression", progression);
	}

	private static String[] names(Collection<? extends Enum<?>> values) {
		String[] result = new String[values.size()];
		int i = 0;
		for (Enum<?> value : values) result[i++] = value.name();
		return result;
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		int storedVersion = bundle.getInt("class_build_schema_version");
		if (storedVersion < 1 || storedVersion > SCHEMA_VERSION) {
			throw new IllegalStateException("unsupported class build schema " + storedVersion);
		}
		schemaVersion = SCHEMA_VERSION;
		name = bundle.getString("name");
		int storedBudget = bundle.contains("base_budget") ? bundle.getInt("base_budget") : BASE_BUDGET;
		baseBudget = ClassBudgetPolicy.migrateClassBuildBudget(storedVersion, storedBudget);
		resources.clear();
		if (bundle.contains("resources")) for (Bundlable value : bundle.getCollection("resources")) {
			if (value instanceof ResourceSpec) resources.add((ResourceSpec)value);
		}
		gameplayComponents.clear();
		if (storedVersion >= 5 && bundle.contains("gameplay_components")) {
			for (Bundlable value : bundle.getCollection("gameplay_components")) {
				if (value instanceof ClassGameplayComponentSpec) gameplayComponents.add((ClassGameplayComponentSpec)value);
			}
		}
		skills.clear();
		if (bundle.contains("skills")) for (Bundlable value : bundle.getCollection("skills")) {
			if (value instanceof SkillSpec) skills.add((SkillSpec)value);
		}
		laws.clear();
		traits.clear();
		restrictions.clear();
		ArrayList<ClassLaw> storedLaws = new ArrayList<>();
		restoreEnums(bundle, "laws", ClassLaw.class, storedLaws);
		for (ClassLaw value : storedLaws) migrateLaw(value);
		if (bundle.contains("trait_specs")) {
			for (Bundlable value : bundle.getCollection("trait_specs")) {
				if (value instanceof TraitSpec) traits.add((TraitSpec)value);
			}
		} else if (bundle.contains("traits")) {
			String[] values = bundle.getStringArray("traits");
			if (values != null) for (String name : values) try {
				migrateTrait(CoreRuleVocabulary.valueOf(name));
			} catch (IllegalArgumentException ignored) {}
		}
		restoreEnums(bundle, "restrictions", Restriction.class, restrictions);
		if (storedVersion < 5) {
			ClassGameplaySpec legacy = bundle.contains("gameplay")
					? (ClassGameplaySpec)bundle.get("gameplay") : inferLegacyGameplay();
			migrateLegacyGameplay(legacy);
		}
		operations.clear();
		if (bundle.contains("class_operations")) for (Bundlable value : bundle.getCollection("class_operations")) {
			if (value instanceof ClassOperationSpec) operations.add((ClassOperationSpec)value);
		}
		startingKit = bundle.contains("starting_kit") ? (StartingKitSpec)bundle.get("starting_kit") : new StartingKitSpec();
		progression = bundle.contains("progression") ? (ClassProgression)bundle.get("progression") : new ClassProgression();
		resolvePendingBindings();
	}

	private ClassGameplaySpec inferLegacyGameplay() {
		ClassGameplaySpec legacy = new ClassGameplaySpec(); legacy.basicAttack = BasicAttackProfile.FULL;
		for (SkillSpec skill : skills) for (EffectSpec effect : new EffectSpec[]{skill.primary, skill.secondary}) {
			if (effect == null) continue;
			if (effect.operation == EffectSpec.Operation.CREATE_ACTOR) {
				legacy.ownership = true; legacy.entityCapacity = Math.max(legacy.entityCapacity, Math.max(1, effect.count));
			} else if (effect.operation == EffectSpec.Operation.CREATE_DEVICE
					|| effect.operation == EffectSpec.Operation.CREATE_FIELD
					|| skill.delivery == SkillDelivery.PERSISTENT_CARRIER) {
				legacy.ownership = true; legacy.deviceCapacity = Math.max(legacy.deviceCapacity, Math.max(1, effect.count));
			}
		}
		return legacy;
	}

	private void migrateLegacyGameplay(ClassGameplaySpec legacy) {
		if (legacy == null) legacy = inferLegacyGameplay();
		gameplayComponents.add(ClassGameplayComponentSpec.basicAttack(legacy.basicAttack));
		if (legacy.ownership) gameplayComponents.add(new ClassGameplayComponentSpec(
				ClassGameplayComponentSpec.Type.OWNERSHIP, "ownership"));
		if (legacy.entityCapacity > 0) {
			ClassGameplayComponentSpec value = new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.ENTITY_CAPACITY, "actor_capacity");
			value.entityFilter = ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR; value.capacity = legacy.entityCapacity;
			gameplayComponents.add(value);
		}
		if (legacy.deviceCapacity > 0) {
			ClassGameplayComponentSpec value = new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.ENTITY_CAPACITY, "device_capacity");
			value.entityFilter = ClassGameplayComponentSpec.EntityFilter.OWNED_DEVICE; value.capacity = legacy.deviceCapacity;
			gameplayComponents.add(value);
		}
		if (legacy.command) gameplayComponents.add(new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.COMMAND, "command"));
		if (legacy.recycle) {
			ClassGameplayComponentSpec value = new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.RECYCLE, "recycle");
			value.resourceId = legacy.recycleResourceId; value.amount = legacy.recycleAmount; gameplayComponents.add(value);
		}
		if (legacy.hasModeEngine()) {
			ClassGameplayComponentSpec value = new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.MODE_ENGINE, "mode_engine");
			value.modes.addAll(legacy.modes); gameplayComponents.add(value);
		}
	}

	private String primaryResourceId() {
		return resources.isEmpty() ? "" : resources.get(0).id;
	}

	private void migrateLaw(ClassLaw value) {
		if (value == null) return;
		switch (value) {
			case RESOURCE_OVERFLOW_TO_SHIELD: traits.add(TraitSpec.resource(CoreRuleVocabulary.OVERFLOW, primaryResourceId())); break;
			case STATUS_ABSORPTION: traits.add(TraitSpec.resource(CoreRuleVocabulary.STATUS_FEEDBACK, primaryResourceId())); break;
			case WATER_AFFINITY: traits.add(TraitSpec.resource(CoreRuleVocabulary.WATER_FLOW, primaryResourceId())); break;
			case KILL_ACCELERATES_RULES: traits.add(TraitSpec.of(CoreRuleVocabulary.KILL_TEMPO)); break;
			default: laws.add(value);
		}
	}

	private void migrateTrait(CoreRuleVocabulary value) {
		if (value == null || value == CoreRuleVocabulary.NONE) return;
		switch (value) {
			case OVERDRAW: if (!laws.contains(ClassLaw.RESOURCE_OVERDRAFT_USES_HP)) laws.add(ClassLaw.RESOURCE_OVERDRAFT_USES_HP); break;
			case INERTIA_BRIDGE: if (!laws.contains(ClassLaw.FORCED_MOVEMENT_COUNTS_AS_MOVE)) laws.add(ClassLaw.FORCED_MOVEMENT_COUNTS_AS_MOVE); break;
			case TRANSLOCATION_BRIDGE: if (!laws.contains(ClassLaw.TRANSLOCATION_COUNTS_AS_ENTER_TILE)) laws.add(ClassLaw.TRANSLOCATION_COUNTS_AS_ENTER_TILE); break;
			default: traits.add(TraitSpec.of(value));
		}
	}

	private static <T extends Enum<T>> void restoreEnums(Bundle bundle, String key, Class<T> type, ArrayList<T> target) {
		if (!bundle.contains(key)) return;
		String[] values = bundle.getStringArray(key);
		if (values == null) return;
		for (String value : values) try { target.add(Enum.valueOf(type, value)); } catch (IllegalArgumentException ignored) {}
	}
}
