package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.qa.BuildAnalysis;
import com.shatteredpixel.shatteredpixeldungeon.qa.RuleBuild;
import com.shatteredpixel.shatteredpixeldungeon.qa.RuleBuildAnalyzer;

import java.util.ArrayList;
import java.util.List;

/** Produces field-specific, localized validation without collapsing failures into one boolean. */
public final class PlayerFacingBuildValidator {
	private PlayerFacingBuildValidator() {}

	private static PlayerFacingValidationIssue error(PlayerFacingValidationIssue.Code code,
			PlayerFacingValidationIssue.Field field, Object... args) {
		return new PlayerFacingValidationIssue(code, PlayerFacingValidationIssue.Severity.ERROR, field, args);
	}

	public static ArrayList<PlayerFacingValidationIssue> skillIssues(
			ClassBuild build, SkillSpec skill, int editIndex) {
		ArrayList<PlayerFacingValidationIssue> result = new ArrayList<>();
		if (skill == null || skill.primary == null) {
			result.add(error(PlayerFacingValidationIssue.Code.MISSING_REQUIRED_SELECTION,
					PlayerFacingValidationIssue.Field.EFFECT, Messages.get(PlayerFacingBuildValidator.class, "primary_effect")));
		} else if (!skill.primary.implemented() || !EffectVocabularyRegistry.exposed(skill.primary.operation)) {
			result.add(error(PlayerFacingValidationIssue.Code.UNSUPPORTED_RUNTIME_CAPABILITY,
					PlayerFacingValidationIssue.Field.EFFECT, skill.primary.displayName()));
		} else if (!skill.primary.configurationValid() && !missingResourceBinding(skill.primary, build)) {
			result.add(error(PlayerFacingValidationIssue.Code.MISSING_EFFECT_PARAMETER,
					PlayerFacingValidationIssue.Field.PARAMETERS, skill.primary.displayName()));
		}
		validateEffectResources(result, build, skill == null ? null : skill.primary,
				PlayerFacingValidationIssue.Field.PARAMETERS);
		if (skill != null) for (RuleCondition condition : skill.conditions) {
			if (condition == null) continue;
			if (condition.type == RuleCondition.Type.MODE_IS
					&& (condition.reference == null || condition.reference.isEmpty() || !build.producesMode(condition.reference))) {
				result.add(error(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
						PlayerFacingValidationIssue.Field.CONSTRAINT, Messages.get(PlayerFacingBuildValidator.class, "mode_source")));
			} else if (condition.type == RuleCondition.Type.RESOURCE_AT_LEAST
					&& (condition.reference == null || build.resource(condition.reference) == null)) {
				result.add(error(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
						PlayerFacingValidationIssue.Field.CONSTRAINT, Messages.get(PlayerFacingBuildValidator.class, "resource_pool")));
			} else if (condition.type == RuleCondition.Type.TARGET_OWNED && !build.producesOperation(EffectSpec.Operation.CREATE_ACTOR)) {
				result.add(error(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
						PlayerFacingValidationIssue.Field.CONSTRAINT, Messages.get(PlayerFacingBuildValidator.class, "owned_entity_source")));
			} else if (condition.type == RuleCondition.Type.LINK_EXISTS && !build.producesOperation(EffectSpec.Operation.RELATION_LINK)) {
				result.add(error(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
						PlayerFacingValidationIssue.Field.CONSTRAINT, Messages.get(PlayerFacingBuildValidator.class, "link_source")));
			}
		}
		if (skill.delivery == null || !skill.delivery.implemented()) {
			result.add(error(PlayerFacingValidationIssue.Code.MISSING_REQUIRED_SELECTION,
					PlayerFacingValidationIssue.Field.DELIVERY, Messages.get(PlayerFacingBuildValidator.class, "delivery")));
		} else if (skill.primary != null && !DeliveryRegistry.compatible(skill.delivery, skill)) {
			result.add(error(PlayerFacingValidationIssue.Code.INCOMPATIBLE_DELIVERY,
					PlayerFacingValidationIssue.Field.DELIVERY, skill.delivery.displayName(), skill.primary.displayName()));
		} else if (!DeliveryRegistry.parametersValid(skill)) {
			result.add(error(PlayerFacingValidationIssue.Code.MISSING_EFFECT_PARAMETER,
					PlayerFacingValidationIssue.Field.DELIVERY, skill.delivery.displayName()));
		}
		if (skill.targeting == null || !skill.targeting.implemented(skill.activation)) {
			result.add(error(PlayerFacingValidationIssue.Code.MISSING_REQUIRED_SELECTION,
					PlayerFacingValidationIssue.Field.TARGET, Messages.get(PlayerFacingBuildValidator.class, "target")));
		} else if (skill.primary != null && (!skill.primary.compatibleTargeting(skill.targeting)
				|| !TargetingRegistry.compatible(skill.targeting, skill))) {
			result.add(error(PlayerFacingValidationIssue.Code.INCOMPATIBLE_TARGET,
					PlayerFacingValidationIssue.Field.TARGET, skill.primary.displayName(), skill.targeting.displayName()));
		}
		if (skill.primary != null && (skill.modifier == null || !skill.modifier.compatible(skill.primary, skill.delivery))) {
			result.add(error(PlayerFacingValidationIssue.Code.INCOMPATIBLE_EFFECT,
					PlayerFacingValidationIssue.Field.MODIFIER, skill.primary.displayName(),
					skill.modifier == null ? Messages.get(PlayerFacingBuildValidator.class, "missing")
							: ModifierRegistry.name(skill.modifier.type)));
		}
		if (skill.cost == null || !skill.cost.referenceValid()) {
			result.add(error(PlayerFacingValidationIssue.Code.INVALID_COST,
					PlayerFacingValidationIssue.Field.COST, Messages.get(PlayerFacingBuildValidator.class, "invalid_payment")));
		} else if (skill.cost.type == RuleCost.Type.RESOURCE) {
			ResourceSpec pool = resource(build, skill.cost.resourceId, skill.cost.resourceEngine);
			if (pool == null) result.add(error(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
					PlayerFacingValidationIssue.Field.COST, Messages.get(PlayerFacingBuildValidator.class, "resource_pool")));
		} else if (skill.cost.type == RuleCost.Type.STATE && !hasChargeSource(build)) {
			result.add(error(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
					PlayerFacingValidationIssue.Field.COST, Messages.get(PlayerFacingBuildValidator.class, "charge_source")));
		}
		if (skill.constraint == null || !skill.constraint.implemented()
				|| !ConstraintRegistry.compatible(skill.constraint, skill)) {
			result.add(error(PlayerFacingValidationIssue.Code.INVALID_CONSTRAINT,
					PlayerFacingValidationIssue.Field.CONSTRAINT, skill.constraint == null
							? Messages.get(PlayerFacingBuildValidator.class, "missing") : skill.constraint.displayName()));
		}
		if (skill.secondary != null) {
			validateEffectResources(result, build, skill.secondary, PlayerFacingValidationIssue.Field.SECONDARY);
			if (!skill.secondary.implemented() || !EffectVocabularyRegistry.exposed(skill.secondary.operation)) {
				result.add(error(PlayerFacingValidationIssue.Code.UNSUPPORTED_RUNTIME_CAPABILITY,
						PlayerFacingValidationIssue.Field.SECONDARY, skill.secondary.displayName()));
			} else if (!skill.secondary.configurationValid() && !missingResourceBinding(skill.secondary, build)) {
				result.add(error(PlayerFacingValidationIssue.Code.MISSING_EFFECT_PARAMETER,
						PlayerFacingValidationIssue.Field.SECONDARY, skill.secondary.displayName()));
			} else if (skill.targeting != null && !skill.secondary.compatibleTargeting(skill.targeting)) {
				result.add(error(PlayerFacingValidationIssue.Code.SECONDARY_INCOMPATIBLE,
						PlayerFacingValidationIssue.Field.SECONDARY, skill.secondary.displayName(),
						skill.targeting.displayName()));
			}
		}
		if (skill.primary != null) {
			ClassBuild candidate = build.copy();
			if (editIndex < 0) candidate.skills.add(skill.copy()); else candidate.skills.set(editIndex, skill.copy());
			if (!candidate.budgetValid()) result.add(error(PlayerFacingValidationIssue.Code.SKILL_BUDGET_OVERFLOW,
					PlayerFacingValidationIssue.Field.BUDGET, candidate.usedBudget() - candidate.maxBudget(),
					skill.nominalPowerCost(), candidate.maxBudget() - build.usedBudget()));
		}
		return result;
	}

	public static ArrayList<PlayerFacingValidationIssue> classIssues(ClassBuild build) {
		ArrayList<PlayerFacingValidationIssue> result = new ArrayList<>();
		if (build == null || build.name == null || build.name.trim().isEmpty()) {
			result.add(error(PlayerFacingValidationIssue.Code.MISSING_REQUIRED_SELECTION,
					PlayerFacingValidationIssue.Field.NAME, Messages.get(PlayerFacingBuildValidator.class, "class_name")));
			return result;
		}
		build.normalizeLegacyComponents(); build.syncClassOperations();
		if (!build.hasGameplayComponent(ClassGameplayComponentSpec.Type.BASIC_ATTACK)) result.add(error(
				PlayerFacingValidationIssue.Code.MISSING_REQUIRED_SELECTION,
				PlayerFacingValidationIssue.Field.COMPONENT, Messages.get(PlayerFacingBuildValidator.class, "basic_attack_model")));
		for (ClassGameplayComponentSpec component : build.gameplayComponents) {
			if (component == null) continue;
			ComponentDependency dependency = component.dependency(build);
			if (!dependency.resolvedState()) result.add(error(
					dependency.state == ComponentDependency.State.UNSUPPORTED
							? PlayerFacingValidationIssue.Code.UNSUPPORTED_RUNTIME_CAPABILITY
							: PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
					PlayerFacingValidationIssue.Field.COMPONENT,
					Messages.get(PlayerFacingBuildValidator.class, "component_dependency",
							component.displayName(build), compatibilityText(dependency.code))));
		}
		boolean createsActor = build.producesOperation(EffectSpec.Operation.CREATE_ACTOR);
		boolean createsDevice = build.producesOperation(EffectSpec.Operation.CREATE_DEVICE)
				|| build.producesOperation(EffectSpec.Operation.CREATE_FIELD);
		if (createsActor && (!build.hasGameplayComponent(ClassGameplayComponentSpec.Type.OWNERSHIP)
				|| build.ownedCapacity(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.ACTOR) < 1)) result.add(error(
				PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
				PlayerFacingValidationIssue.Field.COMPONENT, Messages.get(PlayerFacingBuildValidator.class, "actor_capacity_component")));
		if (createsDevice && (!build.hasGameplayComponent(ClassGameplayComponentSpec.Type.OWNERSHIP)
				|| build.ownedCapacity(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.DEVICE) < 1
				&& build.ownedCapacity(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.FIELD) < 1)) result.add(error(
				PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
				PlayerFacingValidationIssue.Field.COMPONENT, Messages.get(PlayerFacingBuildValidator.class, "device_capacity_component")));
		for (int i = 0; i < build.skills.size(); i++) {
			ArrayList<PlayerFacingValidationIssue> skill = skillIssues(buildWithoutSkill(build, i), build.skills.get(i), -1);
			// The class sheet reports its single aggregate overflow below. Per-skill overflow is
			// still shown while editing that skill, but repeating the same class-wide error once
			// for every saved skill gives the player false "multiple problems" noise.
			for (PlayerFacingValidationIssue issue : skill) if (issue.code
					!= PlayerFacingValidationIssue.Code.SKILL_BUDGET_OVERFLOW) result.add(issue);
		}
		for (ClassLaw law : build.laws) {
			ComponentDependency dependency = LawTraitRegistry.lawDependency(build, law);
			if (!dependency.resolvedState()) {
				if (dependency.state == ComponentDependency.State.UNRESOLVED) result.add(error(
						PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
						PlayerFacingValidationIssue.Field.LAW,
						Messages.get(PlayerFacingBuildValidator.class, "component_dependency",
								law.displayName(), compatibilityText(dependency.code))));
				else result.add(error(PlayerFacingValidationIssue.Code.LAW_TRAIT_INCOMPATIBLE,
						PlayerFacingValidationIssue.Field.LAW, law.displayName(), compatibilityText(dependency.code)));
			}
		}
		for (TraitSpec trait : build.traits) {
			ComponentDependency dependency = LawTraitRegistry.traitDependency(build, trait);
			if (!dependency.resolvedState()) {
				if (dependency.state == ComponentDependency.State.UNRESOLVED) result.add(error(
						PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,
						PlayerFacingValidationIssue.Field.TRAIT,
						Messages.get(PlayerFacingBuildValidator.class, "component_dependency",
								trait.displayName(build), compatibilityText(dependency.code))));
				else result.add(error(PlayerFacingValidationIssue.Code.LAW_TRAIT_INCOMPATIBLE,
						PlayerFacingValidationIssue.Field.TRAIT, trait.displayName(build), compatibilityText(dependency.code)));
			}
		}
		if (!build.budgetValid()) result.add(error(PlayerFacingValidationIssue.Code.CLASS_BUDGET_OVERFLOW,
				PlayerFacingValidationIssue.Field.BUDGET, build.usedBudget() - build.maxBudget(),
				build.usedBudget(), build.maxBudget()));
		if (result.isEmpty()) {
			BuildAnalysis analysis = new RuleBuildAnalyzer().analyze(RuleBuild.from(build));
			if (analysis.classification == BuildAnalysis.Classification.BROKEN) {
				for (BuildAnalysis.Finding finding : analysis.findings) {
					if (finding.severity != BuildAnalysis.Classification.BROKEN) continue;
					result.add(error(PlayerFacingValidationIssue.Code.BUILD_INTEGRITY_BROKEN,
							PlayerFacingValidationIssue.Field.INTEGRITY, integrityText(finding.code)));
				}
			}
		}
		return result;
	}

	public static String summary(List<PlayerFacingValidationIssue> issues, boolean skill) {
		if (issues == null || issues.isEmpty()) return "";
		StringBuilder out = new StringBuilder(Messages.get(PlayerFacingBuildValidator.class,
				skill ? "skill_header" : "class_header", issues.size()));
		for (PlayerFacingValidationIssue issue : issues) out.append("\n• ").append(issue.shortMessage);
		return out.toString();
	}

	private static ClassBuild buildWithoutSkill(ClassBuild source, int index) {
		ClassBuild result = source.copy();
		result.skills.remove(index);
		return result;
	}

	private static ResourceSpec resource(ClassBuild build, String id, ResourceEngine engine) {
		if (build == null) return null;
		for (ResourceSpec value : build.resources) if (value != null
				&& (id != null && !id.isEmpty() && id.equals(value.id) || engine != null && engine == value.engine)) return value;
		return null;
	}

	private static void validateEffectResources(ArrayList<PlayerFacingValidationIssue> result,
			ClassBuild build, EffectSpec effect, PlayerFacingValidationIssue.Field field) {
		if (effect == null || !(effect.family == EffectFamily.RESOURCE_OPERATION
				|| effect.family == EffectFamily.DAMAGE && effect.scalingSource == EffectSpec.ScalingSource.CURRENT_RESOURCE)) return;
		if (effect.resourceId == null || effect.resourceId.isEmpty() || build.resource(effect.resourceId) == null) {
			result.add(error(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY, field,
					Messages.get(PlayerFacingBuildValidator.class, "resource_pool")));
		}
		if (effect.operation == EffectSpec.Operation.RESOURCE_CONVERT
				&& (effect.targetResourceId == null || effect.targetResourceId.isEmpty()
				|| build.resource(effect.targetResourceId) == null)) {
			result.add(error(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY, field,
					Messages.get(PlayerFacingBuildValidator.class, "second_resource_pool")));
		}
	}

	private static boolean missingResourceBinding(EffectSpec effect, ClassBuild build) {
		if (effect == null || !(effect.family == EffectFamily.RESOURCE_OPERATION
				|| effect.family == EffectFamily.DAMAGE && effect.scalingSource == EffectSpec.ScalingSource.CURRENT_RESOURCE)) return false;
		if (effect.resourceId == null || effect.resourceId.isEmpty() || build.resource(effect.resourceId) == null) return true;
		return effect.operation == EffectSpec.Operation.RESOURCE_CONVERT
				&& (effect.targetResourceId == null || effect.targetResourceId.isEmpty()
				|| build.resource(effect.targetResourceId) == null);
	}

	private static boolean hasChargeSource(ClassBuild build) {
		if (LawTraitRegistry.hasTrait(build, CoreRuleVocabulary.ACCUMULATION)) return true;
		return build.producesOperation(EffectSpec.Operation.MARK_APPLY)
				|| build.producesOperation(EffectSpec.Operation.MARK_STACK)
				|| build.producesOperation(EffectSpec.Operation.MARK_COUNTER);
	}

	private static String missingResourceName(RuleCost cost) {
		if (cost.resourceEngine != null) return cost.resourceEngine.displayName();
		return cost.resourceId == null || cost.resourceId.isEmpty()
				? Messages.get(PlayerFacingBuildValidator.class, "unnamed_resource") : cost.resourceId;
	}

	private static String compatibilityText(String issue) {
		return Messages.get(PlayerFacingBuildValidator.class, "compatibility_" + issue);
	}

	private static String integrityText(String code) {
		String key = "integrity_" + (code == null ? "unknown" : code.toLowerCase());
		String value = Messages.get(PlayerFacingBuildValidator.class, key);
		return Messages.NO_TEXT_FOUND.equals(value)
				? Messages.get(PlayerFacingBuildValidator.class, "integrity_unknown") : value;
	}
}
