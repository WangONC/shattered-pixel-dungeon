package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleDamageRedirect;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMitigation;

import java.util.HashSet;

/** Single damage-pipeline integration for mitigation and redirect; vanilla is untouched without these buffs. */
public final class RuleDefenseRuntime {
	private static final ThreadLocal<HashSet<Integer>> REDIRECT_CHAIN = new ThreadLocal<>();
	private RuleDefenseRuntime() {}
	public static int beforeDamage(Char target, int damage, Object source) {
		if (target == null || damage <= 0) return damage;
		RuleMitigation mitigation = target.buff(RuleMitigation.class);
		if (mitigation != null) damage = mitigation.reduce(damage);
		RuleDamageRedirect redirect = target.buff(RuleDamageRedirect.class);
		Char recipient = redirect == null ? null : redirect.recipient();
		if (redirect == null || recipient == null || !recipient.isAlive() || recipient == target) return damage;
		HashSet<Integer> chain = REDIRECT_CHAIN.get();
		boolean root = chain == null;
		if (root) { chain = new HashSet<>(); REDIRECT_CHAIN.set(chain); }
		try {
			if (!chain.add(target.id()) || chain.contains(recipient.id())) {
				RuleTrace.record("REDIRECT_GUARD", target.id()+"->"+recipient.id());
				return damage;
			}
			int sent = Math.max(1, damage * redirect.percent() / 100);
			recipient.damage(sent, new RedirectSource(target.id()));
			RuleTrace.record("REDIRECT", target.id()+"->"+recipient.id()+" amount="+sent);
			return Math.max(0, damage - sent);
		} finally {
			chain.remove(target.id());
			if (root) REDIRECT_CHAIN.remove();
		}
	}
	public static final class RedirectSource { public final int sourceId; RedirectSource(int sourceId){this.sourceId=sourceId;} }
}
