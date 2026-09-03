package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.DirectDamageEffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.FixedValueSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.SkillSpec;

import java.util.ArrayList;
import java.util.List;

/** Builder budget policies are immutable projections of a typed draft. */
public interface BuilderBudgetPolicy {
	BuilderBudgetLedger evaluate(ClassBuildSpec draft);

	final class DeclarationOnly implements BuilderBudgetPolicy {
		@Override public BuilderBudgetLedger evaluate(ClassBuildSpec draft) {
			return new BuilderBudgetLedger(draft.budgetMetadata().priceVersion(), 0, draft.budgetMetadata().baseBudget());
		}
	}

	/** P03's deliberately small, reviewable price catalog. P09 will replace it with full balance data. */
	final class P03TypedSkill implements BuilderBudgetPolicy {
		public static final String PRICE_VERSION = "v6-p03-1";
		@Override public BuilderBudgetLedger evaluate(ClassBuildSpec draft) {
			List<BuilderBudgetLedger.Entry> entries = new ArrayList<>();
			int spent = 0;
			for (SkillSpec skill : draft.skills()) {
				if (!skill.typed() || !(skill.effects().primary() instanceof DirectDamageEffectSpec)) continue;
				String owner = skill.id().value();
				addZero(entries, owner, "skill.trigger.active");
				addZero(entries, owner, "skill.condition.all_of");
				addZero(entries, owner, "skill.condition.always");
				addZero(entries, owner, "skill.chain.primary");
				addZero(entries, owner, "skill.delivery.direct");
				addZero(entries, owner, "skill.target.selector.selected_actor");
				addZero(entries, owner, "skill.target.coverage.single");
				addZero(entries, owner, "skill.target.filter.enemy");
				addZero(entries, owner, "skill.cost.none");
				DirectDamageEffectSpec damage = (DirectDamageEffectSpec) skill.effects().primary();
				if (!(damage.amount() instanceof FixedValueSpec)) continue;
				int amount = 2 + ((((FixedValueSpec) damage.amount()).value() - 1) / 5);
				entries.add(new BuilderBudgetLedger.Entry(owner, "skill.effect.direct_damage", amount));
				spent += amount;
				if (skill.effects().secondary() != null && skill.effects().secondary().effect() instanceof DirectDamageEffectSpec
						&& ((DirectDamageEffectSpec) skill.effects().secondary().effect()).amount() instanceof FixedValueSpec) {
					int secondary = 1 + ((((FixedValueSpec) ((DirectDamageEffectSpec) skill.effects().secondary().effect()).amount()).value() - 1) / 5);
					entries.add(new BuilderBudgetLedger.Entry(owner, "skill.chain.immediate_secondary", secondary));
					spent += secondary;
				}
			}
			return new BuilderBudgetLedger(PRICE_VERSION, spent, draft.budgetMetadata().baseBudget(), entries);
		}
		private static void addZero(List<BuilderBudgetLedger.Entry> entries, String owner, String priceKey) {
			entries.add(new BuilderBudgetLedger.Entry(owner, priceKey, 0));
		}
	}
}
