package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Orthogonal player exposure for target choice, affected area, and eligible targets. */
public final class TargetingRegistry {
	public static final class ParameterOption {
		public final TargetingSpec target;
		public final String name;
		private ParameterOption(TargetingSpec target, String name) { this.target=target; this.name=name; }
		public String summary() { return target.parameterSummary(); }
	}
	public static final List<TargetingSpec.Selector> SELECTORS = Collections.unmodifiableList(Arrays.asList(
			TargetingSpec.Selector.SELF, TargetingSpec.Selector.SELECTED_ACTOR,
			TargetingSpec.Selector.SELECTED_CELL, TargetingSpec.Selector.NEAREST,
			TargetingSpec.Selector.RANDOM, TargetingSpec.Selector.ALL_MATCHING));
	public static final List<TargetingSpec.Coverage> COVERAGES = Collections.unmodifiableList(Arrays.asList(
			TargetingSpec.Coverage.SINGLE, TargetingSpec.Coverage.ADJACENT,
			TargetingSpec.Coverage.RADIUS, TargetingSpec.Coverage.LINE,
			TargetingSpec.Coverage.CONE, TargetingSpec.Coverage.RING,
			TargetingSpec.Coverage.CHAIN));
	public static final List<TargetingSpec.Filter> FILTERS = Collections.unmodifiableList(Arrays.asList(
			TargetingSpec.Filter.ANY, TargetingSpec.Filter.ENEMY, TargetingSpec.Filter.ALLY,
			TargetingSpec.Filter.SELF, TargetingSpec.Filter.OWNED_ENTITY,
			TargetingSpec.Filter.MARKED, TargetingSpec.Filter.HAS_STATUS,
			TargetingSpec.Filter.HP_THRESHOLD, TargetingSpec.Filter.COMPATIBLE_ENTITY_TYPE));
	private TargetingRegistry() {}
	public static String selectorSummary(TargetingSpec.Selector value) { return msg("selector", value.name()); }
	public static String coverageSummary(TargetingSpec.Coverage value) { return msg("coverage", value.name()); }
	public static String filterSummary(TargetingSpec.Filter value) { return msg("filter", value.name()); }
	private static String msg(String kind, String value) {
		return Messages.get(TargetingRegistry.class, kind + "_" + value.toLowerCase() + "_summary");
	}
	public static boolean compatible(TargetingSpec candidate, SkillSpec skill) {
		return candidate.implemented(skill.activation) && skill.primary.compatibleTargeting(candidate)
				&& (skill.secondary == null || skill.secondary.compatibleTargeting(candidate))
				&& (skill.delivery != SkillDelivery.SELF || candidate.selector == TargetingSpec.Selector.SELF)
				&& (skill.delivery != SkillDelivery.GROUND_PLACEMENT
				|| candidate.selector == TargetingSpec.Selector.SELECTED_CELL
				|| candidate.selector == TargetingSpec.Selector.SELF);
	}
	public static List<ParameterOption> parameterOptions(TargetingSpec current) {
		ArrayList<ParameterOption> result=new ArrayList<>();
		for(int value=1;value<=10;value++){TargetingSpec copy=current.copy();copy.range=value;
			result.add(new ParameterOption(copy,Messages.get(TargetingRegistry.class,"parameter_range",value)));}
		if(current.coverage!=TargetingSpec.Coverage.SINGLE){
			for(int value=1;value<=4;value++){TargetingSpec copy=current.copy();copy.magnitude=value;
				result.add(new ParameterOption(copy,Messages.get(TargetingRegistry.class,"parameter_size",value)));}
			for(int value=1;value<=8;value++){TargetingSpec copy=current.copy();copy.maxTargets=value;
				result.add(new ParameterOption(copy,Messages.get(TargetingRegistry.class,"parameter_targets",value)));}
		}
		if(current.filter==TargetingSpec.Filter.HP_THRESHOLD){
			for(int value:new int[]{10,20,30,40,50,70}){TargetingSpec copy=current.copy();copy.filterParameter=value;
				result.add(new ParameterOption(copy,Messages.get(TargetingRegistry.class,"parameter_hp",value)));}
		}
		return Collections.unmodifiableList(result);
	}
}
