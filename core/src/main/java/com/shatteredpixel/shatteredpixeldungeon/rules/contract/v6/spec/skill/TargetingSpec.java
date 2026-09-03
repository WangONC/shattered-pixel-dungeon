package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;

public final class TargetingSpec {
	public enum LineOfSightPolicy { DELIVERY }
	public enum TargetOrdering { DISTANCE_CELL_ACTOR_ID }

	private final StableId nodeId;
	private final SelectorSpec selector;
	private final CoverageSpec coverage;
	private final EntityFilterExpr filter;
	private final int range;
	private final int maximumTargets;
	private final LineOfSightPolicy lineOfSight;
	private final TargetOrdering ordering;

	public TargetingSpec(StableId nodeId, SelectorSpec selector, CoverageSpec coverage,
			EntityFilterExpr filter, int range, int maximumTargets,
			LineOfSightPolicy lineOfSight, TargetOrdering ordering) {
		if (nodeId == null || selector == null || coverage == null || filter == null
				|| lineOfSight == null || ordering == null) {
			throw new IllegalArgumentException("targeting fields are required");
		}
		if (range < 1 || range > 99 || maximumTargets < 1 || maximumTargets > 99) {
			throw new IllegalArgumentException("targeting range/count outside 1..99");
		}
		this.nodeId = nodeId;
		this.selector = selector;
		this.coverage = coverage;
		this.filter = filter;
		this.range = range;
		this.maximumTargets = maximumTargets;
		this.lineOfSight = lineOfSight;
		this.ordering = ordering;
	}
	public StableId nodeId() { return nodeId; }
	public SelectorSpec selector() { return selector; }
	public CoverageSpec coverage() { return coverage; }
	public EntityFilterExpr filter() { return filter; }
	public int range() { return range; }
	public int maximumTargets() { return maximumTargets; }
	public LineOfSightPolicy lineOfSight() { return lineOfSight; }
	public TargetOrdering ordering() { return ordering; }
	public TargetingSpec withSelector(SelectorSpec value) { return copy(value, coverage, filter, range, maximumTargets); }
	public TargetingSpec withCoverage(CoverageSpec value) { return copy(selector, value, filter, range, maximumTargets); }
	public TargetingSpec withFilter(EntityFilterExpr value) { return copy(selector, coverage, value, range, maximumTargets); }
	public TargetingSpec withRange(int value) { return copy(selector, coverage, filter, value, maximumTargets); }
	public TargetingSpec withMaximumTargets(int value) { return copy(selector, coverage, filter, range, value); }
	private TargetingSpec copy(SelectorSpec s, CoverageSpec c, EntityFilterExpr f, int r, int m) {
		return new TargetingSpec(nodeId, s, c, f, r, m, lineOfSight, ordering);
	}
}
