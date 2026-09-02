package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

public class RuleCost implements RuleModule, Bundlable {
	public enum Type { NONE, RESOURCE, HP, ACTION, COOLDOWN, CONSUMABLE, STATE }

	public Type type = Type.NONE;
	public int amount;
	/** Stable ClassBuild pool identity; empty means the runtime's primary pool for legacy rules. */
	public String resourceId = "";
	public ResourceEngine resourceEngine;
	/** Item class name for CONSUMABLE, or RuleMark.Type name for STATE. */
	public String reference = "";

	public RuleCost() {}

	public RuleCost(Type type, int amount) {
		this.type = type;
		this.amount = amount;
	}

	/** Time paid by the normal ACTIVE gameplay action after a successful execution. */
	public float actionTime() {
		return type == Type.ACTION ? Math.max(1, amount) : 1f;
	}

	/** Conservative, centralized budget rebate. Runtime payment and power pricing stay separate. */
	public int budgetRebate() {
		switch (type) {
			case HP: return amount >= 2 ? 1 : 0;
			case RESOURCE: return amount >= 3 ? 1 : 0;
			case ACTION: return amount >= 3 ? 2 : amount >= 2 ? 1 : 0;
			case COOLDOWN: return amount >= 5 ? 2 : amount >= 3 ? 1 : 0;
			case CONSUMABLE: return amount > 0 ? 2 : 0;
			case STATE: return amount > 0 ? 1 : 0;
			default: return 0;
		}
	}

	/** Cooldown cost rebate discounted when the effect naturally has a long replacement cadence. */
	public int effectiveBudgetRebate(EffectSpec effect) {
		int result = budgetRebate();
		if (type != Type.COOLDOWN || effect == null) return result;
		int naturalInterval = effect.family == EffectFamily.CREATE_ENTITY ? Math.max(3, effect.lifetime)
				: effect.family == EffectFamily.RECOVERY_DEFENSE || effect.family == EffectFamily.TRANSFORM ? 4
				: effect.family == EffectFamily.WORLD_TERRAIN ? 2 : 1;
		if (amount <= naturalInterval) return 0;
		if (amount <= naturalInterval + 2) return Math.min(1, result);
		return result;
	}

	public boolean referenceValid() {
		if (type == Type.STATE) return stateType() != null;
		if (type != Type.CONSUMABLE) return true;
		if (reference == null || reference.isEmpty()) return false;
		try { return Item.class.isAssignableFrom(Class.forName(reference)); }
		catch (ClassNotFoundException ignored) { return false; }
	}

	public boolean canPay(RuleRuntime runtime, Hero hero) {
		switch (type) {
			case RESOURCE:
				return runtime.canPayResourceCost(hero, amount, resourceId, resourceEngine);
			case HP:
				return runtime.canPayHpCost(hero, amount);
			case CONSUMABLE:
				return consumable(hero) != null && consumable(hero).quantity() >= Math.max(1, amount);
			case STATE:
				return stateType() != null && RuleMark.has(hero, stateType(), Math.max(1, amount));
			case ACTION:
			case COOLDOWN:
			case NONE:
			default:
				return true;
		}
	}

	public void pay(RuleRuntime runtime, Hero hero) {
		if (type == Type.RESOURCE) {
			runtime.payResourceCost(hero, amount, resourceId, resourceEngine);
		} else if (type == Type.HP) {
			int healthPaid = runtime.payHpCost(hero, amount);
			if (hero.sprite != null) hero.sprite.showStatus(CharSprite.NEGATIVE,
					Messages.get(RuleCost.class, "hp_status", healthPaid));
		} else if (type == Type.CONSUMABLE) {
			Item item = consumable(hero);
			for (int i = 0; item != null && i < Math.max(1, amount); i++) {
				item.detach(hero.belongings.backpack);
			}
		} else if (type == Type.STATE && stateType() != null) {
			RuleMark.consume(hero, stateType(), Math.max(1, amount));
		}
	}

	@SuppressWarnings("unchecked")
	private Item consumable(Hero hero) {
		if (hero == null || reference == null || reference.isEmpty()) return null;
		try {
			Class<?> raw = Class.forName(reference);
			if (!Item.class.isAssignableFrom(raw)) return null;
			java.util.ArrayList<? extends Item> values = hero.belongings.getAllItems((Class<? extends Item>)raw);
			return values.isEmpty() ? null : values.get(0);
		} catch (ClassNotFoundException ignored) {
			return null;
		}
	}

	private RuleMark.Type stateType() {
		try { return RuleMark.Type.valueOf(reference); }
		catch (Exception ignored) { return null; }
	}

	@Override
	public int capacityCost() {
		return 0;
	}

	@Override
	public String description() {
		return description(null);
	}

	public String description(ResourceEngine engine) {
		switch (type) {
			case RESOURCE:
				return Messages.get(RuleCost.class, "resource",
						amount, engine == null ? Messages.get(RuleCost.class, "generic_resource") : engine.displayName());
			case HP:
				return Messages.get(RuleCost.class, "hp", amount);
			case ACTION:
				return Messages.get(RuleCost.class, "action", amount);
			case COOLDOWN:
				return Messages.get(RuleCost.class, "cooldown", amount);
			case CONSUMABLE:
				return Messages.get(RuleCost.class, "consumable", amount);
			case STATE:
				return Messages.get(RuleCost.class, "state", amount);
			case NONE:
			default:
				return Messages.get(RuleCost.class, "none");
		}
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("type", type);
		bundle.put("amount", amount);
		bundle.put("resource_id", resourceId);
		if (resourceEngine != null) bundle.put("resource_engine", resourceEngine);
		bundle.put("reference", reference);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		type = bundle.getEnum("type", Type.class);
		amount = bundle.getInt("amount");
		resourceId = bundle.contains("resource_id") ? bundle.getString("resource_id") : "";
		resourceEngine = bundle.contains("resource_engine")
				? bundle.getEnum("resource_engine", ResourceEngine.class) : null;
		reference = bundle.contains("reference") ? bundle.getString("reference") : "";
	}
}
