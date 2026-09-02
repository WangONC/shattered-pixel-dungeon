package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.Restriction;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.StartingKitSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TraitSpec;

import java.util.ArrayList;
import java.util.List;

/** Detached build input shared by the analyzer, scenario runner, and fuzzer. */
public class RuleBuild {
	public String id;
	public ClassBuild classBuild;
	public ResourceEngine resource;
	public ClassLaw law;
	public Restriction restriction;
	public StartingKitSpec startingKit;
	public boolean syntacticallyValid;
	public final ArrayList<RuleDefinition> rules = new ArrayList<>();
	public final ArrayList<CoreRuleVocabulary> vocabularies = new ArrayList<>();
	public final ArrayList<ResourceSpec> resources = new ArrayList<>();
	public final ArrayList<ClassLaw> laws = new ArrayList<>();
	public final ArrayList<Restriction> restrictions = new ArrayList<>();

	public static RuleBuild from(CustomClassConfig config) {
		return from(config.toClassBuild());
	}

	public static RuleBuild from(ClassBuild config) {
		RuleBuild build = new RuleBuild();
		build.classBuild = config.copy();
		build.classBuild.normalizeLegacyComponents();
		build.classBuild.resolvePendingBindings();
		ClassBuild source = build.classBuild;
		build.id = source.name;
		build.resources.addAll(source.resources);
		build.laws.addAll(source.laws);
		build.restrictions.addAll(source.allRestrictions());
		build.resource = source.resources.isEmpty() ? null : source.resources.get(0).engine;
		build.law = source.laws.isEmpty() ? null : source.laws.get(0);
		build.restriction = build.restrictions.isEmpty() ? Restriction.NONE : build.restrictions.get(0);
		build.startingKit = source.startingKit;
		build.syntacticallyValid = source.valid();
		for (TraitSpec trait : source.traits) if (trait != null && trait.type != null) {
			build.vocabularies.add(trait.type);
		}
		build.rules.addAll(source.compileRules());
		return build;
	}

	public static RuleBuild of(String id, ResourceEngine resource, ClassLaw law,
			Restriction restriction, List<RuleDefinition> rules) {
		RuleBuild build = new RuleBuild();
		build.id = id;
		build.resource = resource;
		if (resource != null) build.resources.add(new ResourceSpec(resource));
		build.law = law;
		if (law != null) build.laws.add(law);
		build.restriction = restriction;
		if (restriction != null && restriction != Restriction.NONE) build.restrictions.add(restriction);
		build.startingKit = new StartingKitSpec();
		build.syntacticallyValid = true;
		if (rules != null) build.rules.addAll(rules);
		return build;
	}

	public String fingerprint() {
		StringBuilder out = new StringBuilder();
		for (ResourceSpec value : resources) {
			out.append(value.id).append(':').append(value.name).append(':').append(value.engine)
					.append(':').append(value.minimum).append(':').append(value.initialValue).append(':').append(value.current).append('/').append(value.capacity);
			out.append('&');
		}
		out.append('|');
		for (ClassLaw value : laws) out.append(value).append('&');
		out.append('|');
		for (Restriction value : restrictions) out.append(value).append('&');
		out.append('|');
		for (CoreRuleVocabulary vocabulary : vocabularies) out.append(vocabulary).append('&');
		if (classBuild != null) for (TraitSpec trait : classBuild.traits) {
			out.append("T:").append(trait.stableId()).append('&');
		}
		if (classBuild != null) {
			out.append("|GAMEPLAY:");
			for (com.shatteredpixel.shatteredpixeldungeon.rules.ClassGameplayComponentSpec component : classBuild.gameplayComponents) {
				out.append(component.id).append(':').append(component.type).append(':').append(component.basicAttack)
						.append(':').append(component.resourceId).append(':').append(component.targetResourceId)
						.append(':').append(component.trigger).append(':').append(component.resourceOperation)
						.append(':').append(component.amount).append(':').append(component.targetAmount)
						.append(':').append(component.interval).append(':').append(component.delay)
						.append(':').append(component.entityFilter).append(':').append(component.capacity)
						.append(':').append(component.lifetime).append(':').append(component.modes).append('&');
			}
			for (ClassOperationSpec operation : classBuild.operations) out.append("|COP:").append(operation.id)
					.append(':').append(operation.name).append(':').append(operation.type).append(':')
					.append(operation.resourceId).append(':').append(operation.amount).append(':').append(operation.actionTime);
			for (SkillSpec skill : classBuild.skills) out.append("|SKILL_NAME:").append(skill.id).append(':').append(skill.name);
		}
		for (RuleDefinition rule : rules) {
			out.append("||").append(rule.trigger.event).append('|');
			for (RuleCondition condition : rule.conditions) {
				out.append(condition.type).append(':').append(condition.parameter).append('&');
			}
			out.append('|').append(rule.cost.type).append(':').append(rule.cost.amount)
					.append(':').append(rule.cost.resourceId)
					.append('|').append(rule.target.type).append('|').append(rule.effect.type)
					.append(':').append(rule.effect.power).append('|').append(rule.modifier.type)
					.append(':').append(rule.modifier.magnitude);
			if (rule.effectSpec != null) out.append("|OP:").append(rule.effectSpec.operation)
					.append(':').append(rule.effectSpec.duration).append(':').append(rule.effectSpec.count)
					.append(':').append(rule.effectSpec.lifetime).append(':').append(rule.effectSpec.stateId)
					.append(':').append(rule.effectSpec.damageType).append(':').append(rule.effectSpec.scalingSource)
					.append(':').append(rule.effectSpec.statusStacking).append(':').append(rule.effectSpec.resourceId);
			if (rule.delivery != null) out.append("|DEL:").append(rule.delivery);
			if (rule.targetingSpec != null) out.append("|TGT:").append(rule.targetingSpec.selector)
					.append(':').append(rule.targetingSpec.coverage).append(':').append(rule.targetingSpec.filter)
					.append(':').append(rule.targetingSpec.range).append(':').append(rule.targetingSpec.maxTargets);
		}
		return out.toString();
	}
}
