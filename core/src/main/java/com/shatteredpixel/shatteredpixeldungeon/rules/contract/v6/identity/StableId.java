package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable opaque identity. Display text and collection order never participate in identity. */
public final class StableId implements Comparable<StableId> {
	private static final Pattern CANONICAL = Pattern.compile("[a-z]+_[0-9a-f]{32}");
	private static final Set<String> PREFIXES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			"build", "res", "mark", "mode", "modegrp", "entity", "capacity", "skill", "effect",
			"component", "op", "abilitypool", "ability", "property", "recipe", "constraint", "link",
			"snapshot")));
	private final String value;

	private StableId(String value) { this.value = value; }

	public static StableId of(String value) {
		if (!isCanonical(value)) throw new IllegalArgumentException("invalid canonical stable id: " + value);
		return new StableId(value);
	}

	/** Restores canonical IDs and non-empty v5 identities without changing their bytes. */
	public static StableId fromStored(String value) {
		if (isCanonical(value)) return new StableId(value);
		if (value == null || value.isEmpty() || value.length() > 128 || !value.equals(value.trim())) {
			throw new IllegalArgumentException("invalid stored stable id: " + value);
		}
		for (int i = 0; i < value.length(); i++) if (Character.isISOControl(value.charAt(i))) {
			throw new IllegalArgumentException("stored stable id contains control characters");
		}
		return new StableId(value);
	}

	public static boolean isCanonical(String value) {
		if (value == null || !CANONICAL.matcher(value).matches()) return false;
		return PREFIXES.contains(value.substring(0, value.indexOf('_')));
	}

	public String value() { return value; }
	public String prefix() { int split = value.indexOf('_'); return split < 0 ? "legacy" : value.substring(0, split); }

	@Override public int compareTo(StableId other) { return value.compareTo(other.value); }
	@Override public boolean equals(Object other) { return other instanceof StableId && value.equals(((StableId)other).value); }
	@Override public int hashCode() { return value.hashCode(); }
	@Override public String toString() { return value; }
}
