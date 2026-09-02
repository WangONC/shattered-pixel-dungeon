package com.shatteredpixel.shatteredpixeldungeon.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Explicit restriction exposure for the free 0..N ClassBuild list. */
public final class RestrictionRegistry {
	private static final List<Restriction> VALUES = Collections.unmodifiableList(Arrays.asList(
			Restriction.NO_ORDINARY_WEAPONS, Restriction.WEAK_HEALING,
			Restriction.FRAIL, Restriction.NO_TRADITIONAL_HEALING,
			Restriction.WAIT_CLEARS_RESOURCE, Restriction.ACTIVE_COSTS_HP));
	private RestrictionRegistry() {}
	public static List<Restriction> exposed() { return VALUES; }
}
