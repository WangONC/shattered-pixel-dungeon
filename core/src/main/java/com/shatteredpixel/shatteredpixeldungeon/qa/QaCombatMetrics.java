package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleTemporaryHP;

/** Optional, thread-local combat observation hook. It is inert outside headless QA. */
public final class QaCombatMetrics {
	public interface Listener {
		void onDamage(Char target, Object source, String attribution, int requested,
				int hpLost, int shieldLost, int temporaryHpLost, int mitigated);
		default void onForcedMovement(Char target, int from, int to) {}
	}

	private static final ThreadLocal<Listener> LISTENER = new ThreadLocal<>();
	private static final ThreadLocal<String> ATTRIBUTION = new ThreadLocal<>();
	private QaCombatMetrics() {}

	public static void install(Listener listener) { LISTENER.set(listener); }
	public static void clear() { LISTENER.remove(); ATTRIBUTION.remove(); }
	public static boolean active() { return LISTENER.get() != null; }
	public static void attribution(String value) {
		if (value == null || value.isEmpty()) ATTRIBUTION.remove(); else ATTRIBUTION.set(value);
	}

	public static int temporaryHp(Char target) {
		RuleTemporaryHP value=target==null?null:target.buff(RuleTemporaryHP.class);
		return value==null?0:Math.max(0,value.shielding());
	}

	public static void record(Char target,Object source,int requested,int hpBefore,int shieldBefore,int temporaryBefore) {
		Listener listener=LISTENER.get();
		if(listener==null||target==null)return;
		int hpLost=Math.max(0,hpBefore-target.HP);
		int shieldLost=Math.max(0,shieldBefore-target.shielding());
		int temporaryLost=Math.min(shieldLost,Math.max(0,temporaryBefore-temporaryHp(target)));
		int mitigated=Math.max(0,requested-hpLost-shieldLost);
		listener.onDamage(target,source,ATTRIBUTION.get(),requested,hpLost,shieldLost,temporaryLost,mitigated);
	}

	public static void recordForcedMovement(Char target, int from, int to) {
		Listener listener = LISTENER.get();
		if (listener != null && target != null && from != to) listener.onForcedMovement(target, from, to);
	}
}
