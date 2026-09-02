package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.BasicAttackProfile;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassGameplayComponentSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.ComponentDependency;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.Restriction;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEffect;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTarget;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.StartingKitSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.LawTraitRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.TraitSpec;

import java.util.HashSet;

import static com.shatteredpixel.shatteredpixeldungeon.qa.BuildAnalysis.Classification.BROKEN;
import static com.shatteredpixel.shatteredpixeldungeon.qa.BuildAnalysis.Classification.RISKY;

/** Fast conservative integrity analysis. It reports structural playability, never "fun". */
public final class RuleBuildAnalyzer {

	public BuildAnalysis analyze(RuleBuild build) {
		BuildAnalysis result = new BuildAnalysis();
		result.buildId = build == null ? "null" : build.id;
		if (build == null) {
			result.add(BROKEN, "MISSING_BUILD", "No class build was supplied.", null);
			return result;
		}
		if (build.startingKit == null) {
			result.add(BROKEN, "MISSING_STARTING_KIT", "Build has no declared starting kit.", null);
		}
		if (!build.syntacticallyValid) {
			String code = build.classBuild != null && !build.classBuild.budgetValid()
					? "BUDGET_OVERFLOW" : "SYNTAX_INCOMPATIBLE";
			result.add(BROKEN, code, "Budget or module compatibility validation failed.", null);
		}
		if (build.classBuild != null) {
			for (ClassLaw law : build.classBuild.laws) {
				String issue=LawTraitRegistry.lawIssue(build.classBuild,law);
				if(issue!=null)result.add(BROKEN,"LAW_DEPENDENCY_MISSING",issue,law.name());
			}
			for (TraitSpec trait : build.classBuild.traits) {
				String issue=LawTraitRegistry.traitIssue(build.classBuild,trait);
				if(issue!=null)result.add(BROKEN,"TRAIT_DEPENDENCY_MISSING",issue,trait.stableId());
			}
		}

		for (ClassLaw law : build.laws) result.capabilities.addAll(RuleSemanticMetadata.capabilities(law));
		for (CoreRuleVocabulary vocabulary : build.vocabularies) {
			result.capabilities.addAll(RuleSemanticMetadata.capabilities(vocabulary));
		}
		for (Restriction restriction : build.restrictions) {
			result.capabilities.removeAll(RuleSemanticMetadata.removedCapabilities(restriction));
		}
		if (!build.restrictions.contains(Restriction.NO_ORDINARY_WEAPONS)
				&& (build.startingKit == null
				|| build.startingKit.mode != StartingKitSpec.Mode.UNARMED)) {
			// WEAK is deliberately a poor but genuine fallback combat path.  Its lower damage is a
			// budget/balance concern, not the same thing as having no way to resolve an enemy.
			result.capabilities.add(RuleQaCapability.DIRECT_DAMAGE);
		}

		HashSet<String> resourceSources = new HashSet<>();
		HashSet<String> resourceSinks = new HashSet<>();
		boolean internalAfflictionSource = false;
		HashSet<String> producedStates = new HashSet<>();
		HashSet<String> ruleFingerprints = new HashSet<>();
		if (build.classBuild != null) for (ClassGameplayComponentSpec component : build.classBuild.gameplayComponents) {
			if (component == null) continue;
			ComponentDependency dependency = component.dependency(build.classBuild);
			if (!dependency.resolvedState()) {
				result.add(BROKEN, "COMPONENT_DEPENDENCY_MISSING", dependency.code, component.id);
				continue;
			}
			if (component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL
					|| component.type == ClassGameplayComponentSpec.Type.RECYCLE) {
				resourceSources.add(component.resourceId);
				result.capabilities.add(RuleQaCapability.RESOURCE_SOURCE);
			} else if (component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW) {
				switch (component.resourceOperation) {
					case GAIN: resourceSources.add(component.resourceId); result.capabilities.add(RuleQaCapability.RESOURCE_SOURCE); break;
					case LOSE:
					case CLEAR: resourceSinks.add(component.resourceId); result.capabilities.add(RuleQaCapability.RESOURCE_SINK); break;
					case CONVERT:
						resourceSinks.add(component.resourceId); resourceSources.add(component.targetResourceId);
						result.capabilities.add(RuleQaCapability.RESOURCE_SOURCE); result.capabilities.add(RuleQaCapability.RESOURCE_SINK); break;
					default: break;
				}
			}
		}

		for (RuleDefinition rule : build.rules) {
			if (rule == null || rule.trigger == null || rule.target == null || rule.effect == null
					|| rule.modifier == null || rule.cost == null) {
				result.add(BROKEN, "NO_OP_RULE", "Rule is missing an executable module.", rule == null ? null : rule.id);
				continue;
			}
			boolean structuredPrimary = rule.effectSpec != null
					&& rule.effectSpec.operation != EffectSpec.Operation.LEGACY;
			boolean structuredSecondary = rule.secondaryEffectSpec != null
					&& rule.secondaryEffectSpec.operation != EffectSpec.Operation.LEGACY;
			if (!structuredPrimary && !RuleTarget.availableForEvent(rule.target.type, rule.trigger.event)) {
				result.add(BROKEN, "INVALID_TARGET_CONTEXT", "Activation cannot provide the selected target context.", rule.id);
			}
			if (!structuredPrimary && (!RuleEffect.compatibleTarget(rule.effect.type, rule.target.type)
					|| !rule.modifier.compatible(rule.effect.type)
					|| rule.secondaryEffect != null && !structuredSecondary
					&& !RuleEffect.compatibleTarget(rule.secondaryEffect.type, rule.target.type))) {
				result.add(BROKEN, "NO_OP_RULE", "Effect, target, or modifier is incompatible.", rule.id);
			}
			if (rule.cost.type == RuleCost.Type.RESOURCE && rule.cost.amount > 0) {
				String resourceId = normalizedResourceId(build, rule.cost);
				if (resourceId == null || !hasResource(build, resourceId, rule.cost.resourceEngine)) {
					result.add(BROKEN, "COST_WITHOUT_SOURCE", "Technique spends a resource pool the build does not declare.", rule.id);
				} else {
					resourceSinks.add(resourceId);
					result.capabilities.add(RuleQaCapability.RESOURCE_SINK);
				}
			}
			if ((rule.cost.type == RuleCost.Type.CONSUMABLE || rule.cost.type == RuleCost.Type.STATE)
					&& !rule.cost.referenceValid()) {
				result.add(BROKEN, "COST_SOURCE_INVALID", "Technique cost names an unavailable item or state.", rule.id);
			}
			if (rule.cost.type == RuleCost.Type.CONSUMABLE) {
				result.add(RISKY, "EXTERNAL_COST_REQUIRED",
						"Technique consumes a finite item supplied by the dungeon or starting kit.", rule.id);
			}
			if (rule.cost.type == RuleCost.Type.STATE
					&& "CHARGED".equals(rule.cost.reference)
					&& !build.vocabularies.contains(CoreRuleVocabulary.ACCUMULATION)) {
				result.add(BROKEN, "COST_WITHOUT_SOURCE",
						"Technique consumes a charged state the build cannot create.", rule.id);
			}
			if (structuredPrimary) collectSpecCapability(result, rule.effectSpec);
			else collectEffect(result, producedStates, rule.effect.type, rule.target.type);
			if (rule.secondaryEffect != null) {
				if (structuredSecondary) collectSpecCapability(result, rule.secondaryEffectSpec);
				else collectEffect(result, producedStates, rule.secondaryEffect.type, rule.target.type);
			}
			if ((rule.effect.type == RuleEffect.Type.POISON || rule.effect.type == RuleEffect.Type.FIRE)
					&& !structuredPrimary && rule.target.type == RuleTarget.Type.SELF
					&& rule.cost.type != RuleCost.Type.RESOURCE) internalAfflictionSource = true;
			if (build.vocabularies.contains(CoreRuleVocabulary.TRANSLOCATION_BACKLASH)
					&& (rule.effect.type == RuleEffect.Type.TELEPORT
					|| rule.effect.type == RuleEffect.Type.SWAP_POSITION)) internalAfflictionSource = true;
			if (build.vocabularies.contains(CoreRuleVocabulary.SHIELD_BACKLASH)
					&& rule.effect.type == RuleEffect.Type.SHIELD) internalAfflictionSource = true;

			String fingerprint = ruleFingerprint(rule);
			if (!ruleFingerprints.add(fingerprint)) {
				result.add(RISKY, "DUPLICATE_RULE", "Technique duplicates an earlier executable rule.", rule.id);
			}

			for (RuleCondition condition : rule.conditions) {
				if (condition == null) continue;
				if (condition.type == RuleCondition.Type.RESOURCE_AT_LEAST) {
					String conditionId = normalizedResourceId(build, condition.reference);
					ResourceSpec checked = conditionId == null ? null : resource(build, conditionId);
					if (checked == null) {
						result.add(BROKEN, "UNREACHABLE_RULE", "Resource condition exists in a zero-resource class.", rule.id);
					} else if (condition.parameter > checked.capacity) {
						result.add(BROKEN, "UNREACHABLE_RULE", "Resource condition exceeds the pool maximum.", rule.id);
					}
				}
			}
		}

		// Structured operations must contribute their real economy/state semantics, never those of
		// the legacy RuleEffect adapter used only for old-save compatibility and presentation.
		if (build.classBuild != null) for (SkillSpec skill : build.classBuild.skills) {
			for (EffectSpec spec : new EffectSpec[]{skill.primary, skill.secondary}) {
				if (spec == null || spec.operation == EffectSpec.Operation.LEGACY) continue;
				if (spec.operation == EffectSpec.Operation.STATUS_POISON) producedStates.add("POISON");
				if (spec.operation == EffectSpec.Operation.STATUS_BURNING) producedStates.add("BURNING");
				if ((spec.operation == EffectSpec.Operation.STATUS_POISON
						|| spec.operation == EffectSpec.Operation.STATUS_BURNING)
						&& skill.targeting.filter == TargetingSpec.Filter.SELF
						&& skill.cost.type != RuleCost.Type.RESOURCE) internalAfflictionSource = true;
				if (spec.family == EffectFamily.DAMAGE
						&& spec.scalingSource == EffectSpec.ScalingSource.CURRENT_RESOURCE) {
					String scalingId=normalizedResourceId(build,spec.resourceId);
					if(scalingId==null||!hasResource(build,scalingId,null))result.add(BROKEN,"DAMAGE_SCALING_SOURCE_MISSING","Damage scaling names a resource pool the build does not declare.",skill.id);
					else resourceSinks.add(scalingId);
				}
				if (spec.family != EffectFamily.RESOURCE_OPERATION) continue;
				String sourceId = normalizedResourceId(build, spec.resourceId);
				if (sourceId == null || !hasResource(build, sourceId, null)) {
					result.add(BROKEN, "RESOURCE_OPERATION_WITHOUT_POOL",
							"Resource operation names a pool the build does not declare.", skill.id);
					continue;
				}
				if (spec.operation == EffectSpec.Operation.RESOURCE_DRAIN
						|| spec.operation == EffectSpec.Operation.RESOURCE_RESERVE
						|| spec.operation == EffectSpec.Operation.RESOURCE_CONVERT) resourceSinks.add(sourceId);
				if (spec.operation == EffectSpec.Operation.RESOURCE_GAIN) resourceSources.add(sourceId);
				if (spec.operation == EffectSpec.Operation.RESOURCE_GAIN
						&& resource(build, sourceId) != null
						&& resource(build, sourceId).engine == ResourceEngine.AFFLICTION) internalAfflictionSource = true;
				if (spec.operation == EffectSpec.Operation.RESOURCE_CONVERT) {
					String targetId = normalizedResourceId(build, spec.targetResourceId);
					if (targetId == null || targetId.equals(sourceId) || !hasResource(build, targetId, null)) {
						result.add(BROKEN, "RESOURCE_CONVERSION_TARGET_INVALID",
								"Conversion requires a different declared destination pool.", skill.id);
					} else resourceSources.add(targetId);
				}
			}
		}

		for (ResourceSpec declared : build.resources) {
			if (declared == null) continue;
			boolean source = resourceSources.contains(declared.id);
			boolean sink = resourceSinks.contains(declared.id);
			if (source && !sink) result.add(RISKY, "DEAD_RESOURCE", "Resource " + declared.id + " has a source but no gameplay sink.", null);
			if (sink && !source) {
				if (declared.initialValue > declared.minimum) result.add(RISKY, "FINITE_RESOURCE_ONLY",
						"Resource " + declared.id + " can be spent but has no renewable source.", null);
				else if (build.vocabularies.contains(CoreRuleVocabulary.OVERDRAW)) result.add(RISKY, "OVERDRAW_ONLY_ECONOMY",
						"Resource " + declared.id + " relies on health overdraw.", null);
				else result.add(BROKEN, "RESOURCE_WITHOUT_SOURCE",
						"Resource " + declared.id + " is spent but has no usable source.", null);
			}
		}

		if (build.vocabularies.contains(CoreRuleVocabulary.PROPAGATION)
				&& !build.vocabularies.contains(CoreRuleVocabulary.HUNT_MARK)) {
			result.add(BROKEN, "VOCABULARY_SOURCE_MISSING",
					"Propagation requires Hunt Mark in the same build.", null);
		}
		if (build.vocabularies.contains(CoreRuleVocabulary.COMPENSATION)
				&& !hasFailureSensitiveEffect(build)) {
			result.add(RISKY, "VOCABULARY_SOURCE_MISSING",
					"The selected effects have no reliable synchronous failure state for Compensation.", null);
		}
		if (build.vocabularies.contains(CoreRuleVocabulary.HUNT_MARK)
				&& build.restrictions.contains(Restriction.NO_ORDINARY_WEAPONS)
				&& !hasTrigger(build, RuleEvent.ON_HIT)) {
			result.add(RISKY, "EXTERNAL_STATE_REQUIRED",
					"Hunt Mark depends on repeated hits while ordinary weapons are unavailable.", null);
		}

		for (RuleDefinition rule : build.rules) {
			if (rule == null) continue;
			for (RuleCondition condition : rule.conditions) {
				String required = condition == null ? null : RuleSemanticMetadata.requiredState(condition.type);
				if (required == null || "RESOURCE".equals(required) || producedStates.contains(required)) continue;
				String code = ("POISON".equals(required) || "BURNING".equals(required))
						? "CONDITION_SOURCE_MISSING" : "EXTERNAL_STATE_REQUIRED";
				result.add(RISKY, code, "Condition requires " + required
						+ " but the build itself does not produce it.", rule.id);
			}
		}

		if (build.classBuild != null) {
			boolean producesOwnedEntity=false;
			boolean producesMode=build.classBuild.hasModeEngine();
			int redirectCount=0;
			int linkCount=0;
			for (SkillSpec skill : build.classBuild.skills) {
				EffectSpec[] effects={skill.primary,skill.secondary};
				for(EffectSpec spec:effects)if(spec!=null){
					collectSpecCapability(result,spec);
					if(spec.family==EffectFamily.CREATE_ENTITY)producesOwnedEntity=true;
					if(spec.operation==EffectSpec.Operation.TRANSFORM_MODE)producesMode=true;
					if(spec.operation==EffectSpec.Operation.DEFENSE_REDIRECT)redirectCount++;
					if(spec.operation==EffectSpec.Operation.RELATION_LINK)linkCount++;
				}
			}
			for (SkillSpec skill : build.classBuild.skills) {
				if (!skill.structurallyValid()) {
					result.add(BROKEN, "UNSUPPORTED_ENGINE_CAPABILITY",
							"Skill requests an effect, delivery, or targeting path not implemented by the current SPD adapter.", skill.id);
				}
				if (skill.constraint != null && skill.constraint.automaticallySatisfied(build.classBuild)) {
					result.add(RISKY, "FAKE_CONSTRAINT_REBATE",
							"A build component makes this limitation easy to satisfy; its effective rebate was reduced.", skill.id);
				}
				if (skill.requiredCarrier != null && !skill.requiredCarrier.isEmpty()
						&& (build.startingKit == null
						|| !skill.requiredCarrier.equals(build.startingKit.requiredCarrier))) {
					result.add(BROKEN, "MISSING_CARRIER_DEPENDENCY",
							"Skill requires a starting carrier which the starting kit does not provide.", skill.id);
				}
				EffectSpec spec=skill.primary;
				if(spec==null)continue;
				boolean free=skill.cost.type==RuleCost.Type.NONE&&skill.modifier.cooldown<=0;
				if(spec.family==EffectFamily.CREATE_ENTITY&&free&&skill.activation==RuleEvent.ON_TURN_START){
					result.add(BROKEN,"ENTITY_PRODUCTION_LOOP","A turn-start Skill creates Actors without a finite cost or cooldown.",skill.id);
				}else if(spec.family==EffectFamily.CREATE_ENTITY&&free&&skill.activation==RuleEvent.ACTIVE){
					result.add(BROKEN,"UNLIMITED_ACTOR_PRODUCTION","An active entity producer has no resource, action, item, or cooldown gate.",skill.id);
				}
				if(skill.delivery==SkillDelivery.PERSISTENT_CARRIER&&free&&spec.lifetime>=10){
					result.add(BROKEN,"PERMANENT_CARRIER_WITHOUT_COST","A long-lived carrier has no finite cost or cooldown.",skill.id);
				}
				if((spec.operation==EffectSpec.Operation.RELATION_COMMAND_ATTACK
						||spec.operation==EffectSpec.Operation.RELATION_COMMAND_FOLLOW
						||spec.operation==EffectSpec.Operation.RELATION_COMMAND_GUARD
						||spec.operation==EffectSpec.Operation.RELATION_INHERIT)&&!producesOwnedEntity){
					result.add(BROKEN,"OWNERSHIP_ORPHAN","Command has no owned-entity producer in the build.",skill.id);
				}
				if(spec.operation==EffectSpec.Operation.RELATION_INHERIT&&"mode".equalsIgnoreCase(spec.stateId)&&!producesMode){
					result.add(BROKEN,"INHERITANCE_SOURCE_MISSING","Mode inheritance has no mode-producing Skill in the build.",skill.id);
				}
				if(skill.delivery==SkillDelivery.ACTION_ATTACHMENT&&skill.attachmentEvent==skill.activation){
					result.add(RISKY,"ACTION_ATTACHMENT_RECURSION","Attachment listens to its own activation event; runtime consumes it first but the combination is fragile.",skill.id);
				}
				if(spec.family==EffectFamily.WORLD_TERRAIN&&skill.targeting.selector==TargetingSpec.Selector.SELF
						&&spec.operation==EffectSpec.Operation.WORLD_DESTROY){
					result.add(BROKEN,"TERRAIN_INVALID_OPERATION","Destroying the occupied self tile is rejected by terrain capability validation.",skill.id);
				}
				for(RuleCondition condition:skill.conditions)if(condition!=null&&condition.type==RuleCondition.Type.MODE_IS&&!producesMode){
					result.add(BROKEN,"TRANSFORM_UNREACHABLE_MODE","Mode condition has no class Mode Engine or mode-producing Skill.",skill.id);
				}
			}
			if(redirectCount>1)result.add(RISKY,"INFINITE_REDIRECT_RISK","Multiple redirect relations can form a cycle; runtime guard will stop it.",null);
			if(linkCount>1)result.add(RISKY,"LINK_CYCLE_RISK","Multiple generic links may form a relation cycle.",null);
		}

		boolean resolution = result.capabilities.contains(RuleQaCapability.DIRECT_DAMAGE)
				|| result.capabilities.contains(RuleQaCapability.DOT_DAMAGE)
				|| result.capabilities.contains(RuleQaCapability.ENVIRONMENT_KILL)
				|| result.capabilities.contains(RuleQaCapability.SUMMON_DAMAGE)
				|| result.capabilities.contains(RuleQaCapability.DEVICE_DAMAGE)
				|| result.capabilities.contains(RuleQaCapability.BYPASS);
		if (!resolution) {
			result.add(BROKEN, "NO_VICTORY_PATH",
					"No implemented enemy-resolution capability remains after restrictions and starting-kit choices.", null);
		}

		if (hasResourceEngine(build, ResourceEngine.FOCUS)) {
			for (RuleDefinition rule : build.rules) {
				if (rule.trigger.event == RuleEvent.ON_WAIT
						&& (rule.effect.type == RuleEffect.Type.SHIELD
						|| rule.effect.type == RuleEffect.Type.HEAL && build.laws.contains(ClassLaw.HEALING_TO_SHIELD))
						&& (rule.cost.type == RuleCost.Type.NONE
						|| rule.cost.type == RuleCost.Type.RESOURCE && rule.cost.amount <= 2)) {
					result.add(RISKY, "FREE_REPEATABLE_POWER_LOOP",
							"WAIT can fund repeatable shielding without hostile or finite input.", rule.id);
				}
			}
		}
		return result;
	}

	private static void collectSpecCapability(BuildAnalysis result,EffectSpec spec){
		if(spec==null||!spec.implemented())return;
		switch(spec.family){
			case DAMAGE:result.capabilities.add(RuleQaCapability.DIRECT_DAMAGE);break;
			case STATUS:
				result.capabilities.add(RuleQaCapability.STATUS_APPLICATION);
				if(spec.operation==EffectSpec.Operation.STATUS_POISON||spec.operation==EffectSpec.Operation.STATUS_BURNING
						||spec.operation==EffectSpec.Operation.STATUS_BLEEDING)result.capabilities.add(RuleQaCapability.DOT_DAMAGE);break;
			case MOVEMENT:result.capabilities.add(RuleQaCapability.MOBILITY);if(spec.operation==EffectSpec.Operation.MOVE_THROW)result.capabilities.add(RuleQaCapability.FORCED_MOVEMENT);break;
			case RECOVERY_DEFENSE:result.capabilities.add(RuleQaCapability.SURVIVAL);result.capabilities.add(RuleQaCapability.SHIELDING);break;
			case RESOURCE_OPERATION:
				if(spec.operation==EffectSpec.Operation.RESOURCE_GAIN)result.capabilities.add(RuleQaCapability.RESOURCE_SOURCE);
				if(spec.operation==EffectSpec.Operation.RESOURCE_DRAIN||spec.operation==EffectSpec.Operation.RESOURCE_RESERVE
						||spec.operation==EffectSpec.Operation.RESOURCE_CONVERT)result.capabilities.add(RuleQaCapability.RESOURCE_SINK);
				break;
			case CREATE_ENTITY:
				if(spec.operation==EffectSpec.Operation.CREATE_ACTOR)result.capabilities.add(RuleQaCapability.SUMMON_DAMAGE);
				if(spec.operation==EffectSpec.Operation.CREATE_DEVICE||spec.operation==EffectSpec.Operation.CREATE_TRAP
						||spec.operation==EffectSpec.Operation.CREATE_FIELD)result.capabilities.add(RuleQaCapability.DEVICE_DAMAGE);break;
			case WORLD_TERRAIN:result.capabilities.add(RuleQaCapability.ENVIRONMENT_CONTROL);if(spec.operation==EffectSpec.Operation.WORLD_FIRE||spec.operation==EffectSpec.Operation.WORLD_TOXIC_GAS)result.capabilities.add(RuleQaCapability.ENVIRONMENT_KILL);break;
			default:break;
		}
	}

	private static void collectEffect(BuildAnalysis result, HashSet<String> states,
			RuleEffect.Type effect, RuleTarget.Type target) {
		java.util.EnumSet<RuleQaCapability> capabilities = RuleSemanticMetadata.capabilities(effect);
		if (target == RuleTarget.Type.SELF) capabilities.remove(RuleQaCapability.DOT_DAMAGE);
		result.capabilities.addAll(capabilities);
		String produced = RuleSemanticMetadata.producedState(effect);
		if (produced != null) states.add(produced);
	}

	private static String normalizedResourceId(RuleBuild build, RuleCost cost) {
		if (cost.resourceId != null && !cost.resourceId.isEmpty()) return cost.resourceId;
		if (cost.resourceEngine != null) {
			for (ResourceSpec resource : build.resources) if (resource.engine == cost.resourceEngine) return resource.id;
		}
		return build.resources.isEmpty() ? null : build.resources.get(0).id;
	}

	private static String normalizedResourceId(RuleBuild build, String id) {
		return id != null && !id.isEmpty() ? id : build.resources.isEmpty() ? null : build.resources.get(0).id;
	}

	private static boolean hasResource(RuleBuild build, String id, ResourceEngine engine) {
		for (ResourceSpec value : build.resources) {
			if (value != null && (id != null && id.equals(value.id) || engine != null && engine == value.engine)) return true;
		}
		return false;
	}

	private static ResourceSpec resource(RuleBuild build, String id) {
		for (ResourceSpec value : build.resources) if (value != null && id.equals(value.id)) return value;
		return null;
	}

	private static boolean hasResourceEngine(RuleBuild build, ResourceEngine engine) {
		for (ResourceSpec value : build.resources) if (value != null && value.engine == engine) return true;
		return false;
	}

	private static boolean hasTrigger(RuleBuild build, RuleEvent event) {
		for (RuleDefinition rule : build.rules) if (rule != null && rule.trigger.event == event) return true;
		return false;
	}

	private static boolean hasFailureSensitiveEffect(RuleBuild build) {
		for (RuleDefinition rule : build.rules) {
			if (rule == null || rule.effect == null) continue;
			switch (rule.effect.type) {
				case HEAL:
				case CLEANSE:
				case PUSH:
				case PULL:
				case TELEPORT:
				case SWAP_POSITION:
				case CREATE_WATER:
					return true;
				default:
			}
		}
		return false;
	}

	private static String ruleFingerprint(RuleDefinition rule) {
		StringBuilder out = new StringBuilder();
		out.append(rule.trigger.event).append('|');
		for (RuleCondition condition : rule.conditions) {
			out.append(condition.type).append(':').append(condition.parameter).append('&');
		}
		out.append('|').append(rule.cost.type).append(':').append(rule.cost.amount)
				.append(':').append(rule.cost.resourceId)
				.append('|').append(rule.target.type).append('|').append(rule.effect.type)
				.append(':').append(rule.effect.power);
		if (rule.secondaryEffect != null) out.append('+').append(rule.secondaryEffect.type)
				.append(':').append(rule.secondaryEffect.power);
		out.append('|').append(rule.modifier.type).append(':').append(rule.modifier.magnitude);
		if(rule.effectSpec!=null)out.append("|SPEC:").append(rule.effectSpec.operation).append(':').append(rule.effectSpec.power)
				.append(':').append(rule.effectSpec.duration).append(':').append(rule.effectSpec.damageType)
				.append(':').append(rule.effectSpec.scalingSource).append(':').append(rule.effectSpec.statusStacking)
				.append(':').append(rule.effectSpec.resourceId).append(':').append(rule.effectSpec.stateId);
		if(rule.targetingSpec!=null)out.append("|TARGETING:").append(rule.targetingSpec.selector).append(':')
				.append(rule.targetingSpec.coverage).append(':').append(rule.targetingSpec.filter).append(':')
				.append(rule.targetingSpec.range).append(':').append(rule.targetingSpec.maxTargets);
		if(rule.delivery!=null)out.append("|DELIVERY:").append(rule.delivery);
		return out.toString();
	}
}
