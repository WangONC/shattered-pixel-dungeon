package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/** Single player-language formatter shared by Builder, Hero Select, action HUD, and Build Sheet. */
public final class PlayerFacingClassBuildFormatter {
	private PlayerFacingClassBuildFormatter() {}
	private static String msg(String key, Object... args) {
		// Keep the existing bundle namespace while retiring ClassBuildFormatter from player call paths.
		return Messages.get(ClassBuildFormatter.class, key, args);
	}

	public static String shortSummary(ClassBuild build) {
		if (build == null) return msg("unconfigured");
		String resource = build.resources.isEmpty() ? msg("no_resource") : build.resources.get(0).displayName();
		String style = msg("style_support");
		for (SkillSpec skill : build.skills) {
			if (skill.primary.family == EffectFamily.WORLD_TERRAIN) style = msg("style_terrain");
			else if (skill.primary.family == EffectFamily.MOVEMENT) style = msg("style_movement");
			else if (skill.primary.family == EffectFamily.STATUS) style = msg("style_status");
		}
		return msg("summary", resource, style, build.skills.size(), build.traits.size() + build.laws.size());
	}

	public static String builderStatus(ClassBuild build) {
		if (build == null) return msg("unconfigured");
		return msg("builder_status", build.name, build.resources.size(), build.skills.size(),
				build.laws.size() + build.traits.size(), build.allRestrictions().size(), build.usedBudget(), build.maxBudget());
	}

	/** Deterministic gameplay signals, not an archetype label or hidden class domain. */
	public static String gameplayOverview(ClassBuild build) {
		if (build == null) return msg("unconfigured");
		build.syncClassOperations();
		StringBuilder out = new StringBuilder();
		boolean ranged=false, terrain=false, marks=false, owned=false, carrier=false, reactive=false, defense=false, modes=build.hasModeEngine();
		boolean forced=false, hpCost=false, actionCost=false, projectileAffix=false;
		for (SkillSpec skill : build.skills) {
			ranged |= skill.delivery == SkillDelivery.PROJECTILE || skill.delivery == SkillDelivery.TRACE_BEAM;
			terrain |= skill.primary.family == EffectFamily.WORLD_TERRAIN;
			marks |= skill.primary.family == EffectFamily.MARK_ACCUMULATION;
			owned |= skill.primary.family == EffectFamily.CREATE_ENTITY;
			carrier |= skill.delivery == SkillDelivery.PERSISTENT_CARRIER
					|| skill.primary.operation == EffectSpec.Operation.CREATE_DEVICE
					|| skill.primary.operation == EffectSpec.Operation.CREATE_FIELD
					|| skill.primary.operation == EffectSpec.Operation.CREATE_TRAP;
			defense |= skill.primary.family == EffectFamily.RECOVERY_DEFENSE;
			modes |= skill.primary.operation == EffectSpec.Operation.TRANSFORM_MODE;
			forced |= skill.primary.operation == EffectSpec.Operation.MOVE_PUSH
					|| skill.primary.operation == EffectSpec.Operation.MOVE_PULL
					|| skill.primary.operation == EffectSpec.Operation.MOVE_THROW;
			hpCost |= skill.cost.type == RuleCost.Type.HP;
			actionCost |= skill.cost.type == RuleCost.Type.ACTION;
			projectileAffix |= skill.modifier.type == RuleModifier.Type.PIERCE
					|| skill.modifier.type == RuleModifier.Type.BOUNCE;
			reactive |= skill.activation != RuleEvent.ACTIVE;
		}
		appendOverview(out, build.basicAttackProfile() == BasicAttackProfile.WEAK
				? "overview_weak_basic_attack" : "overview_full_basic_attack", build.basicAttackProfile().budgetCost(build));
		appendOverview(out, ranged ? "overview_ranged" : "overview_close");
		for (ResourceSpec resource : build.resources) appendOverview(out, "overview_resource_pool",
				resource.displayName(), resource.initialValue, resource.capacity, resourceComponentCount(build, resource.id));
		for (ClassGameplayComponentSpec component : build.gameplayComponents) if (component != null
				&& component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL) appendOverview(out,
				"overview_active_refill", resourceName(build, component.resourceId), component.actionTime);
		if (actionCost) appendOverview(out, "overview_action_cost");
		if (build.hasGameplayComponent(ClassGameplayComponentSpec.Type.OWNERSHIP)) appendOverview(out, "overview_ownership",
				build.ownedCapacity(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.ACTOR),
				build.ownedCapacity(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity.Kind.DEVICE));
		if (build.hasGameplayComponent(ClassGameplayComponentSpec.Type.COMMAND)) appendOverview(out, "overview_command");
		for (ClassGameplayComponentSpec component : build.gameplayComponents) if (component != null
				&& component.type == ClassGameplayComponentSpec.Type.RECYCLE) appendOverview(out,
				"overview_recycle", resourceName(build, component.resourceId));
		if (build.hasModeEngine()) appendOverview(out, "overview_mode_engine", build.modes().size());
		if (marks) appendOverview(out, "overview_marks");
		if (owned) appendOverview(out, "overview_owned");
		if (carrier) appendOverview(out, "overview_carrier");
		if (terrain) appendOverview(out, "overview_terrain");
		if (forced) appendOverview(out, "overview_forced_movement");
		if (projectileAffix) appendOverview(out, "overview_projectile_affix");
		if (modes) appendOverview(out, "overview_modes");
		if (defense) appendOverview(out, "overview_defense");
		if (hpCost) appendOverview(out, "overview_hp_cost");
		if (reactive) appendOverview(out, "overview_reactive");
		return out.toString();
	}

	public static String compactPreview(ClassBuild build) {
		if (build == null) return msg("unconfigured");
		return msg("compact_preview", build.name, resourceNames(build), skillNames(build), lawNames(build),
				traitNames(build), restrictionNames(build), build.startingKit.displayName(),
				build.usedBudget(), build.maxBudget());
	}

	public static String buildSheet(ClassBuild build) {
		if (build == null) return msg("unconfigured");
		StringBuilder out = new StringBuilder();
		out.append('_').append(build.name).append("_\n\n");
		section(out, msg("resources"), resourceDetails(build));
		section(out, msg("class_mechanics"), classMechanicDetails(build));
		section(out, msg("class_operations"), operationDetails(build));
		section(out, msg("gameplay_overview"), gameplayOverview(build));
		section(out, msg("skills"), skillDetails(build));
		section(out, msg("laws"), lawDetails(build));
		section(out, msg("traits"), traitDetails(build));
		section(out, msg("restrictions"), restrictionDetails(build));
		section(out, msg("starting_kit"), build.startingKit.displayName() + "\n" + build.startingKit.description());
		section(out, msg("budget"), msg("budget_value", build.usedBudget(), build.maxBudget(),
				Math.max(0, build.maxBudget() - build.usedBudget())));
		section(out, msg("progression"), progressionDetails(build));
		return out.toString().trim();
	}

	public static String skillName(SkillSpec skill) {
		return skill == null || skill.primary == null ? msg("none")
				: skill.name != null && !skill.name.trim().isEmpty() ? skill.name.trim()
				: msg("skill_name", new RuleTrigger(skill.activation).description(), skill.primary.displayName());
	}

	public static String skillShort(SkillSpec skill, ClassBuild build) {
		if (skill == null) return msg("none");
		return msg("skill_player_summary", new RuleTrigger(skill.activation).description(),
				skill.primary.description(), deliveryDetail(skill), skill.targeting.displayName(),
				CostRegistry.describe(skill.cost, build));
	}

	public static String skillDetail(SkillSpec skill, ClassBuild build) {
		if (skill == null) return msg("none");
		String secondary = skill.secondary == null ? msg("none") : effectDetail(skill.secondary);
		String modifier = ModifierRegistry.name(skill.modifier.type) + " — "
				+ ModifierRegistry.summary(skill.modifier.type);
		return msg("skill_detail", skillName(skill), skillShort(skill, build), deliveryDetail(skill),
				targetDetail(skill.targeting), conditionDetails(skill), effectDetail(skill.primary), secondary,
				modifier, CostRegistry.describe(skill.cost, build), skill.constraint.displayName());
	}

	private static String deliveryDetail(SkillSpec skill) {
		if (skill.delivery == SkillDelivery.PERSISTENT_CARRIER) return msg("delivery_carrier",
				skill.delivery.displayName(), skill.primary.lifetime, skill.primary.period);
		if (skill.delivery == SkillDelivery.ACTION_ATTACHMENT) return msg("delivery_attachment",
				skill.delivery.displayName(), new RuleTrigger(skill.attachmentEvent).description(), skill.attachmentCharges);
		return skill.delivery.displayName();
	}

	public static String ruleName(RuleDefinition rule, ResourceEngine engine) {
		return skillName(SkillSpec.fromRule(rule, engine));
	}

	public static String ruleDescription(RuleDefinition rule, ResourceEngine engine, ClassBuild build) {
		return skillShort(SkillSpec.fromRule(rule, engine), build == null ? new ClassBuild() : build);
	}

	private static String targetDetail(TargetingSpec target) {
		return msg("targeting_player_value", target.selectorName(), target.coverageName(), target.filterName(),
				target.parameterSummary());
	}

	private static String effectDetail(EffectSpec effect) {
		EffectVocabularyRegistry.EffectEntry entry = EffectVocabularyRegistry.find(effect);
		String parameters = entry == null || !entry.hasParameters() ? msg("no_extra_parameters") : effect.parameterSummary();
		return msg("effect_value_parameters", effect.family.displayName(), effect.displayName(), parameters);
	}

	private static String conditionDetails(SkillSpec skill) {
		StringBuilder out = new StringBuilder();
		for (RuleCondition value : skill.conditions) {
			if (value == null || value.type == RuleCondition.Type.ALWAYS) continue;
			if (out.length() > 0) out.append(msg("condition_separator")).append(' ');
			out.append(value.type == RuleCondition.Type.MODE_IS
					? msg("condition_mode", modeDisplayName(value.reference)) : value.description());
		}
		return out.length() == 0 ? msg("no_condition") : out.toString();
	}

	private static String resourceDetails(ClassBuild build) {
		if (build.resources.isEmpty()) return msg("no_resource_desc");
		StringBuilder out = new StringBuilder();
		for (ResourceSpec value : build.resources) {
			StringBuilder detail = new StringBuilder(msg("resource_pool_detail", value.initialValue, value.capacity));
			for (ClassGameplayComponentSpec component : build.gameplayComponents) if (component != null
					&& value.id.equals(component.resourceId)
					&& (component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW
					|| component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL)) {
				detail.append("\n• ").append(component.description(build));
			}
			appendComponent(out, value.displayName(), detail.toString());
		}
		return out.toString();
	}

	private static String classMechanicDetails(ClassBuild build) {
		StringBuilder out = new StringBuilder();
		for (ClassGameplayComponentSpec component : build.gameplayComponents) if (component != null) {
			appendComponent(out, component.displayName(build), component.description(build)
					+ "\n" + msg("component_budget_cost", component.budgetCost(build)));
		}
		return out.toString();
	}

	private static String operationDetails(ClassBuild build) {
		build.syncClassOperations();
		if (build.operations.isEmpty()) return msg("no_class_operations");
		StringBuilder out = new StringBuilder();
		for (ClassOperationSpec operation : build.operations) appendComponent(out,
				operation.displayName(build), operation.description(build));
		return out.toString();
	}

	private static String resourceName(ClassBuild build, String id) {
		ResourceSpec value = build.resource(id); return value == null ? msg("unconfigured") : value.displayName();
	}

	private static String join(java.util.List<String> values) {
		StringBuilder out = new StringBuilder(); for (String value : values) appendName(out, value); return out.toString();
	}

	private static String skillDetails(ClassBuild build) {
		if (build.skills.isEmpty()) return msg("no_skills_desc");
		StringBuilder out = new StringBuilder();
		for (SkillSpec skill : build.skills) {
			if (out.length() > 0) out.append("\n\n");
			out.append(skillDetail(skill, build));
		}
		return out.toString();
	}

	private static String lawDetails(ClassBuild build) {
		if (build.laws.isEmpty()) return msg("no_laws_desc");
		StringBuilder out = new StringBuilder();
		for (ClassLaw law : build.laws) appendComponent(out, law.displayName(), law.detail());
		return out.toString();
	}

	private static String traitDetails(ClassBuild build) {
		if (build.traits.isEmpty()) return msg("no_traits_desc");
		StringBuilder out = new StringBuilder();
		for (TraitSpec trait : build.traits) appendComponent(out, trait.displayName(build), trait.detail(build));
		return out.toString();
	}

	private static String restrictionDetails(ClassBuild build) {
		if (build.allRestrictions().isEmpty()) return msg("no_restrictions_desc");
		StringBuilder out = new StringBuilder();
		for (Restriction value : build.allRestrictions()) appendComponent(out, value.displayName(), value.description());
		return out.toString();
	}

	private static String progressionDetails(ClassBuild build) {
		StringBuilder out = new StringBuilder(msg("progression_value",
				Messages.get(ClassProgression.class, build.progression.tier.name().toLowerCase()),
				build.progression.budgetBonus));
		for (String addition : build.progression.additions) out.append("\n• ").append(addition);
		return out.toString();
	}

	private static String resourceNames(ClassBuild build) { StringBuilder out=new StringBuilder(); for(ResourceSpec v:build.resources)appendName(out,v.displayName());return empty(out); }
	private static String skillNames(ClassBuild build) { StringBuilder out=new StringBuilder(); for(SkillSpec v:build.skills)appendName(out,skillName(v));return empty(out); }
	private static String lawNames(ClassBuild build) { StringBuilder out=new StringBuilder(); for(ClassLaw v:build.laws)appendName(out,v.displayName());return empty(out); }
	private static String traitNames(ClassBuild build) { StringBuilder out=new StringBuilder(); for(TraitSpec v:build.traits)appendName(out,v.displayName(build));return empty(out); }
	private static String restrictionNames(ClassBuild build) { StringBuilder out=new StringBuilder(); for(Restriction v:build.allRestrictions())appendName(out,v.displayName());return empty(out); }
	private static int resourceComponentCount(ClassBuild build,String id){int result=0;for(ClassGameplayComponentSpec c:build.gameplayComponents)if(c!=null&&id.equals(c.resourceId)&&(c.type==ClassGameplayComponentSpec.Type.RESOURCE_FLOW||c.type==ClassGameplayComponentSpec.Type.ACTIVE_REFILL))result++;return result;}
	private static String modeNames(java.util.List<String> modes) { StringBuilder out=new StringBuilder(); for(String mode:modes)appendName(out,modeDisplayName(mode));return empty(out); }
	private static String modeDisplayName(String id) { if("offense".equals(id))return msg("mode_offense");if("defense".equals(id))return msg("mode_defense");return id==null||id.isEmpty()?msg("unconfigured"):id; }
	private static String empty(StringBuilder out) { return out.length()==0?msg("none"):out.toString(); }
	private static void appendName(StringBuilder out,String name){if(out.length()>0)out.append(msg("separator"));out.append(name);}
	private static void appendOverview(StringBuilder out,String key,Object...args){if(out.length()>0)out.append('\n');out.append("• ").append(msg(key,args));}
	private static void appendComponent(StringBuilder out,String name,String desc){if(out.length()>0)out.append("\n\n");out.append('_').append(name).append("_\n").append(desc);}
	private static void section(StringBuilder out,String title,String body){out.append('_').append(title).append("_\n").append(body).append("\n\n");}
}
