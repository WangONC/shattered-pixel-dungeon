package com.shatteredpixel.shatteredpixeldungeon.qa;

import java.util.List;

/** Conservative trend detector for action sequences with no hostile or finite input. */
public final class PositiveLoopDetector {
	private PositiveLoopDetector() {}

	public static String detect(List<QaSnapshot> samples, boolean hostileInteraction, boolean finiteInput) {
		if (hostileInteraction || finiteInput || samples.size() < 6) return null;
		QaSnapshot first = samples.get(0);
		QaSnapshot last = samples.get(samples.size() - 1);
		if (monotonicShield(samples) && last.hero.shield - first.hero.shield >= 50) {
			return "UNBOUNDED_POWER_LOOP";
		}
		if (last.hero.resource > last.hero.maxResource) return "UNBOUNDED_RESOURCE_LOOP";
		if (monotonicActors(samples) && last.actorCount - first.actorCount >= 20) {
			return "UNBOUNDED_ACTOR_LOOP";
		}
		if (monotonicMaxHp(samples) && last.hero.maxHp - first.hero.maxHp >= 20) {
			return "UNBOUNDED_PERMANENT_STAT_LOOP";
		}
		return null;
	}

	private static boolean monotonicShield(List<QaSnapshot> samples) {
		int previous = Integer.MIN_VALUE;
		for (QaSnapshot sample : samples) {
			if (sample.hero.shield < previous) return false;
			previous = sample.hero.shield;
		}
		return true;
	}

	private static boolean monotonicActors(List<QaSnapshot> samples) {
		int previous = Integer.MIN_VALUE;
		for (QaSnapshot sample : samples) {
			if (sample.actorCount < previous) return false;
			previous = sample.actorCount;
		}
		return true;
	}

	private static boolean monotonicMaxHp(List<QaSnapshot> samples) {
		int previous = Integer.MIN_VALUE;
		for (QaSnapshot sample : samples) {
			if (sample.hero.maxHp < previous) return false;
			previous = sample.hero.maxHp;
		}
		return true;
	}
}
