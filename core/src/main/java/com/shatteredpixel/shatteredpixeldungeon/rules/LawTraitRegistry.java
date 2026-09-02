package com.shatteredpixel.shatteredpixeldungeon.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Single formal player registry for the small Law set and parameterized Trait vocabulary. */
public final class LawTraitRegistry {
	private static final List<ClassLaw> LAWS = Collections.unmodifiableList(Arrays.asList(
			ClassLaw.HEALING_TO_SHIELD,
			ClassLaw.FORCED_MOVEMENT_COUNTS_AS_MOVE,
			ClassLaw.TRANSLOCATION_COUNTS_AS_ENTER_TILE,
			ClassLaw.RESOURCE_OVERDRAFT_USES_HP,
			ClassLaw.OWNED_ACTIONS_COUNT_AS_YOURS));

	private static final List<CoreRuleVocabulary> TRAITS = Collections.unmodifiableList(Arrays.asList(
			CoreRuleVocabulary.ACCUMULATION, CoreRuleVocabulary.OVERFLOW,
			CoreRuleVocabulary.COMPENSATION, CoreRuleVocabulary.PHASE_SHIFT,
			CoreRuleVocabulary.ECHO, CoreRuleVocabulary.HUNT_MARK,
			CoreRuleVocabulary.PROPAGATION, CoreRuleVocabulary.STATUS_FEEDBACK,
			CoreRuleVocabulary.WATER_FLOW, CoreRuleVocabulary.KILL_TEMPO,
			CoreRuleVocabulary.KINETIC_MARK, CoreRuleVocabulary.MOBILE_CHARGE,
			CoreRuleVocabulary.TEMP_HP_PAYMENT, CoreRuleVocabulary.PIERCING_MARK,
			CoreRuleVocabulary.OWNED_RESOURCE_FEEDBACK, CoreRuleVocabulary.CARRIER_RESOURCE_FEEDBACK,
			CoreRuleVocabulary.HAZARD_FEEDBACK, CoreRuleVocabulary.MODE_GUARD,
			CoreRuleVocabulary.TRANSFER_FEEDBACK, CoreRuleVocabulary.STATUS_CHAIN));

	private LawTraitRegistry() {}

	public static List<ClassLaw> laws() { return LAWS; }
	public static List<CoreRuleVocabulary> traits() { return TRAITS; }
	public static boolean exposed(ClassLaw value) { return LAWS.contains(value); }
	public static boolean exposed(CoreRuleVocabulary value) { return TRAITS.contains(value); }

	public static String lawIssue(ClassBuild build, ClassLaw law) {
		return lawDependency(build, law).code;
	}

	public static ComponentDependency lawDependency(ClassBuild build, ClassLaw law) {
		if (build == null || law == null || !exposed(law)) return ComponentDependency.unsupported();
		String issue = null;
		switch (law) {
			case FORCED_MOVEMENT_COUNTS_AS_MOVE:
				issue = hasAny(build, EffectSpec.Operation.MOVE_PUSH, EffectSpec.Operation.MOVE_PULL,
						EffectSpec.Operation.MOVE_THROW) ? null : "needs_forced_movement"; break;
			case TRANSLOCATION_COUNTS_AS_ENTER_TILE:
				issue = hasAny(build, EffectSpec.Operation.MOVE_TELEPORT, EffectSpec.Operation.MOVE_DASH,
						EffectSpec.Operation.MOVE_SWAP) ? null : "needs_translocation"; break;
			case RESOURCE_OVERDRAFT_USES_HP:
				issue = hasSpendableResourceCost(build) ? null : "needs_spendable_resource"; break;
			case OWNED_ACTIONS_COUNT_AS_YOURS:
				issue = build.producesOperation(EffectSpec.Operation.CREATE_ACTOR) ? null : "needs_owned_actor"; break;
			default: break;
		}
		return issue == null ? ComponentDependency.resolved() : ComponentDependency.unresolved(issue);
	}

	public static final class TraitOption {
		public final TraitSpec spec;
		public final String issue;
		public final ComponentDependency dependency;
		TraitOption(TraitSpec spec, String issue) {
			this.spec = spec; this.issue = issue;
			this.dependency = issue == null ? ComponentDependency.resolved()
					: "unsupported".equals(issue) ? ComponentDependency.unsupported()
					: ComponentDependency.unresolved(issue);
		}
		public boolean compatible() { return issue == null; }
		public boolean playerSelectable() { return dependency.playerSelectable(); }
	}

	/** Expands resource/mode bindings without creating resource-specific behavior enums. */
	public static List<TraitOption> traitOptions(ClassBuild build) {
		ArrayList<TraitOption> result = new ArrayList<>();
		for (CoreRuleVocabulary type : TRAITS) {
			if (type.requiresResource()) {
				if (build.resources.isEmpty()) result.add(new TraitOption(TraitSpec.of(type), "needs_resource"));
				else for (ResourceSpec resource : build.resources) {
					TraitSpec spec = TraitSpec.resource(type, resource.id);
					result.add(new TraitOption(spec, traitIssue(build, spec)));
				}
			} else if (type.requiresMode()) {
				LinkedHashSet<String> modes = modes(build);
				if (modes.isEmpty()) result.add(new TraitOption(TraitSpec.of(type), "needs_mode"));
				else for (String mode : modes) {
					TraitSpec spec = TraitSpec.state(type, mode);
					result.add(new TraitOption(spec, traitIssue(build, spec)));
				}
			} else {
				TraitSpec spec = TraitSpec.of(type);
				result.add(new TraitOption(spec, traitIssue(build, spec)));
			}
		}
		return result;
	}

	public static String traitIssue(ClassBuild build, TraitSpec spec) {
		return traitDependency(build, spec).code;
	}

	public static ComponentDependency traitDependency(ClassBuild build, TraitSpec spec) {
		if (build == null || spec == null || spec.type == null || !exposed(spec.type)) return ComponentDependency.unsupported();
		if (!spec.bindingValid(build)) return ComponentDependency.unresolved(
				spec.type.requiresMode() ? "needs_mode" : "needs_resource");
		String issue = null;
		switch (spec.type) {
			case ACCUMULATION:
				for (SkillSpec skill : build.skills) if (CoreRuleVocabulary.supportsAccumulation(skill.activation)) return ComponentDependency.resolved();
				issue = "needs_accumulation_event"; break;
			case COMPENSATION: issue = hasFailureSensitiveEffect(build) ? null : "needs_failable_effect"; break;
			case PROPAGATION: issue = hasTrait(build, CoreRuleVocabulary.HUNT_MARK) ? null : "needs_hunt_mark"; break;
			case STATUS_FEEDBACK: issue = build.producesFamily(EffectFamily.STATUS) ? null : "needs_status"; break;
			case KINETIC_MARK:
				issue = hasAny(build, EffectSpec.Operation.MOVE_PUSH, EffectSpec.Operation.MOVE_PULL,
						EffectSpec.Operation.MOVE_THROW) ? null : "needs_forced_movement"; break;
			case HAZARD_FEEDBACK:
				if (!hasAny(build, EffectSpec.Operation.MOVE_PUSH, EffectSpec.Operation.MOVE_PULL,
						EffectSpec.Operation.MOVE_THROW)) issue = "needs_forced_movement";
				else issue = build.producesFamily(EffectFamily.WORLD_TERRAIN) ? null : "needs_hazard";
				break;
			case TEMP_HP_PAYMENT:
				issue = build.producesOperation(EffectSpec.Operation.DEFENSE_TEMP_HP) && hasHpCost(build)
						? null : "needs_temp_hp_and_hp_cost"; break;
			case PIERCING_MARK:
				for (SkillSpec skill : build.skills) if (skill.delivery == SkillDelivery.PROJECTILE
						|| skill.delivery == SkillDelivery.TRACE_BEAM) return ComponentDependency.resolved();
				issue = "needs_projectile_or_beam"; break;
			case OWNED_RESOURCE_FEEDBACK:
				issue = build.producesOperation(EffectSpec.Operation.CREATE_ACTOR) ? null : "needs_owned_actor"; break;
			case CARRIER_RESOURCE_FEEDBACK:
				for (SkillSpec skill : build.skills) if (skill.delivery == SkillDelivery.PERSISTENT_CARRIER
						|| skill.primary.operation == EffectSpec.Operation.CREATE_DEVICE
						|| skill.primary.operation == EffectSpec.Operation.CREATE_FIELD
						|| skill.primary.operation == EffectSpec.Operation.CREATE_TRAP) return ComponentDependency.resolved();
				issue = "needs_carrier"; break;
			case TRANSFER_FEEDBACK:
				issue = build.producesFamily(EffectFamily.TRANSFER_COPY) ? null : "needs_transfer"; break;
			case STATUS_CHAIN:
				issue = build.producesFamily(EffectFamily.STATUS) && hasMarkSource(build)
						? null : "needs_status_and_mark"; break;
			default: break;
		}
		return issue == null ? ComponentDependency.resolved() : ComponentDependency.unresolved(issue);
	}

	private static boolean hasSpendableResourceCost(ClassBuild build) {
		for (SkillSpec skill : build.skills) if (skill.cost != null && skill.cost.type == RuleCost.Type.RESOURCE) {
			ResourceSpec resource = build.resource(skill.cost.resourceId);
			if (resource != null && resource.engine != ResourceEngine.BLOOD) return true;
		}
		return false;
	}

	public static boolean hasTrait(ClassBuild build, CoreRuleVocabulary type) {
		if (build == null) return false;
		for (TraitSpec spec : build.traits) if (spec != null && spec.type == type) return true;
		return false;
	}

	private static boolean hasHpCost(ClassBuild build) {
		for (SkillSpec skill : build.skills) if (skill.cost.type == RuleCost.Type.HP) return true;
		return false;
	}

	private static boolean hasMarkSource(ClassBuild build) {
		if (hasTrait(build, CoreRuleVocabulary.HUNT_MARK) || hasTrait(build, CoreRuleVocabulary.KINETIC_MARK)
				|| hasTrait(build, CoreRuleVocabulary.PIERCING_MARK)) return true;
		return build.producesOperation(EffectSpec.Operation.MARK_APPLY)
				|| build.producesOperation(EffectSpec.Operation.MARK_STACK);
	}

	private static boolean hasFailureSensitiveEffect(ClassBuild build) {
		for (SkillSpec skill : build.skills) {
			for (EffectSpec effect : new EffectSpec[]{skill.primary, skill.secondary}) {
				if(effect==null)continue;EffectSpec.Operation operation=effect.operation;
				if (operation == EffectSpec.Operation.RECOVER_HEAL || operation == EffectSpec.Operation.DEFENSE_CLEANSE
						|| operation == EffectSpec.Operation.MOVE_PUSH || operation == EffectSpec.Operation.MOVE_PULL
						|| operation == EffectSpec.Operation.MOVE_THROW || operation == EffectSpec.Operation.MOVE_TELEPORT
						|| operation == EffectSpec.Operation.MOVE_SWAP || operation == EffectSpec.Operation.WORLD_WATER) return true;
			}
		}
		return false;
	}

	private static boolean hasAny(ClassBuild build, EffectSpec.Operation... operations) {
		for (EffectSpec.Operation operation : operations) if (build.producesOperation(operation)) return true;
		return false;
	}

	private static LinkedHashSet<String> modes(ClassBuild build) {
		LinkedHashSet<String> result = new LinkedHashSet<>(build.modes());
		for (SkillSpec skill : build.skills) for (EffectSpec effect : new EffectSpec[]{skill.primary, skill.secondary}) {
			if (effect != null && effect.operation == EffectSpec.Operation.TRANSFORM_MODE
					&& effect.stateId != null && !effect.stateId.isEmpty()) result.add(effect.stateId);
		}
		return result;
	}
}
