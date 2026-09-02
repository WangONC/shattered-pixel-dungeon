package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

/** Orthogonal selector, coverage, and filter blueprint with a conservative RuleTarget adapter. */
public class TargetingSpec implements Bundlable {
	public enum Selector { SELF, SELECTED_ACTOR, SELECTED_CELL, NEAREST, RANDOM, ALL_MATCHING }
	public enum Coverage { SINGLE, ADJACENT, RADIUS, LINE, CONE, RING, CHAIN }
	public enum Filter { ANY, ENEMY, ALLY, SELF, OWNED_ENTITY, MARKED, HAS_STATUS, HP_THRESHOLD, COMPATIBLE_ENTITY_TYPE }

	public Selector selector = Selector.SELF;
	public Coverage coverage = Coverage.SINGLE;
	public Filter filter = Filter.SELF;
	public int magnitude = 1;
	public int range = 6;
	public int filterParameter = 1;
	public int maxTargets = 4;

	public TargetingSpec() {}

	public TargetingSpec(RuleTarget.Type legacy) {
		switch (legacy) {
			case SELECTED_TARGET: selector = Selector.SELECTED_ACTOR; filter = Filter.ENEMY; break;
			case SELECTED_CELL: selector = Selector.SELECTED_CELL; filter = Filter.ANY; break;
			case NEAREST_ENEMY: selector = Selector.NEAREST; filter = Filter.ENEMY; break;
			case ALL_ADJACENT_ENEMIES:
				selector = Selector.ALL_MATCHING; coverage = Coverage.ADJACENT; filter = Filter.ENEMY; break;
			case ATTACKER:
			case HIT_TARGET:
			case ATTACK_TARGET:
				selector = Selector.SELECTED_ACTOR; filter = Filter.ENEMY; break;
			case CURRENT_TILE: selector = Selector.SELECTED_CELL; filter = Filter.SELF; break;
			case SELF:
			default: selector = Selector.SELF; filter = Filter.SELF; break;
		}
	}

	public TargetingSpec copy() {
		TargetingSpec result = new TargetingSpec();
		result.selector = selector;
		result.coverage = coverage;
		result.filter = filter;
		result.magnitude = magnitude;
		result.range = range;
		result.filterParameter = filterParameter;
		result.maxTargets = maxTargets;
		return result;
	}

	public boolean implemented(RuleEvent event) {
		if (selector == null || coverage == null || filter == null
				|| range < 1 || maxTargets < 1 || magnitude < 1) return false;
		return selector != Selector.SELECTED_ACTOR || event == RuleEvent.ACTIVE
				|| RuleTarget.availableForEvent(compileType(event), event);
	}

	public RuleTarget.Type compileType(RuleEvent event) {
		if (event == RuleEvent.ON_ENTER_TILE && selector == Selector.SELECTED_CELL
				&& filter == Filter.SELF) return RuleTarget.Type.CURRENT_TILE;
		if (event == RuleEvent.ON_DAMAGED && selector == Selector.SELECTED_ACTOR) return RuleTarget.Type.ATTACKER;
		if ((event == RuleEvent.ON_HIT || event == RuleEvent.ON_KILL || event == RuleEvent.ON_ATTACK)
				&& selector == Selector.SELECTED_ACTOR) return RuleTarget.Type.HIT_TARGET;
		if (selector == Selector.SELECTED_ACTOR) return RuleTarget.Type.SELECTED_TARGET;
		if (selector == Selector.SELECTED_CELL) return RuleTarget.Type.SELECTED_CELL;
		if (selector == Selector.NEAREST) return RuleTarget.Type.NEAREST_ENEMY;
		if (selector == Selector.ALL_MATCHING || coverage == Coverage.ADJACENT) {
			return RuleTarget.Type.ALL_ADJACENT_ENEMIES;
		}
		return RuleTarget.Type.SELF;
	}

	public int powerCost() {
		int result = selector == Selector.SELF ? 0 : 1;
		if (coverage == Coverage.RADIUS || coverage == Coverage.ADJACENT) result += 2;
		else if (coverage == Coverage.LINE || coverage == Coverage.CONE || coverage == Coverage.RING) result += 3;
		else if (coverage == Coverage.CHAIN) result += 4;
		if (selector == Selector.RANDOM || selector == Selector.ALL_MATCHING) result += 1;
		if (filter == Filter.OWNED_ENTITY || filter == Filter.MARKED || filter == Filter.HAS_STATUS) result += 1;
		// Reach is both safety and target availability. Six cells is the neutral reference range;
		// longer reach must not remain a free parameter on projectile/beam skills.
		if (range > 6) result += 1 + (range - 7) / 3;
		if (coverage != Coverage.SINGLE && maxTargets > 4) result += (maxTargets - 3) / 3;
		if (magnitude > 2 && coverage != Coverage.SINGLE) result += (magnitude - 1) / 2;
		return result;
	}

	public String displayName() {
		return Messages.get(TargetingSpec.class, "summary", selectorName(), coverageName(), filterName());
	}

	public String selectorName() { return Messages.get(TargetingSpec.class, "selector_" + selector.name().toLowerCase()); }
	public String coverageName() { return Messages.get(TargetingSpec.class, "coverage_" + coverage.name().toLowerCase()); }
	public String filterName() { return Messages.get(TargetingSpec.class, "filter_" + filter.name().toLowerCase()); }
	public String parameterSummary(){return Messages.get(TargetingSpec.class,"parameters",range,magnitude,maxTargets);}

	@Override public void storeInBundle(Bundle bundle) {
		bundle.put("selector", selector);
		bundle.put("coverage", coverage);
		bundle.put("filter", filter);
		bundle.put("magnitude", magnitude);
		bundle.put("range", range);
		bundle.put("filter_parameter", filterParameter);
		bundle.put("max_targets", maxTargets);
	}

	@Override public void restoreFromBundle(Bundle bundle) {
		selector = bundle.getEnum("selector", Selector.class);
		coverage = bundle.getEnum("coverage", Coverage.class);
		filter = bundle.getEnum("filter", Filter.class);
		magnitude = Math.max(1, bundle.getInt("magnitude"));
		range = bundle.contains("range") ? Math.max(1, bundle.getInt("range")) : 6;
		filterParameter = bundle.contains("filter_parameter") ? bundle.getInt("filter_parameter") : 1;
		maxTargets = bundle.contains("max_targets") ? Math.max(1, bundle.getInt("max_targets")) : 4;
	}
}
