package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.component.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource.ResourceOperationSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ContractNodeSpec;

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

	/** Through-P04 review prices. P09 will replace these with final balance data. */
	final class P03TypedSkill implements BuilderBudgetPolicy {
		public static final String PRICE_VERSION = "v6-p04-1";
		@Override public BuilderBudgetLedger evaluate(ClassBuildSpec draft) {
			List<BuilderBudgetLedger.Entry> entries = new ArrayList<>();
			int spent = 0;
			for (SkillSpec skill : draft.skills()) {
				if (!skill.typed()) continue;
				String owner = skill.id().value();
				addZero(entries, owner,skill.activation() instanceof EventTriggerSpec?"component.trigger.event":"skill.trigger.active");
				condition(entries,owner,skill.condition());
				addZero(entries, owner, "skill.chain.primary");
				addZero(entries,owner,"skill.delivery."+skill.delivery().variantKey().toLowerCase());addZero(entries,owner,"skill.target.selector."+skill.targeting().selector().variantKey().toLowerCase());addZero(entries,owner,"skill.target.coverage."+skill.targeting().coverage().variantKey().toLowerCase());addZero(entries,owner,filterKey(skill.targeting().filter()));addZero(entries,owner,"skill.cost."+costKey(skill.cost()));
				int primary=effectPrice(skill.effects().primary());entries.add(new BuilderBudgetLedger.Entry(owner,"skill.effect."+effectKey(skill.effects().primary()),primary));spent+=primary;
				if(skill.modifier()!=null&&!(skill.modifier() instanceof UnconfiguredModifierSpec))addZero(entries,owner,"skill.modifier."+skill.modifier().variantKey().toLowerCase());
				if(skill.effects().secondary()!=null){EffectSpec effect=skill.effects().secondary().effect();int secondary=Math.max(1,effectPrice(effect)-1);entries.add(new BuilderBudgetLedger.Entry(owner,"skill.chain.immediate_secondary",secondary));spent+=secondary;}
			}
			for(ContractNodeSpec node:draft.classComponents()){String key=node.variantKey().toLowerCase();entries.add(new BuilderBudgetLedger.Entry(node.id().value(),"component."+key,1));spent++;if(node instanceof ResourceFlowComponentSpec){addZero(entries,node.id().value(),"component.trigger.event");addZero(entries,node.id().value(),"resource.operation."+((ResourceFlowComponentSpec)node).operation().variant().name().toLowerCase());}else if(node instanceof ActiveResourceOperationComponentSpec)addZero(entries,node.id().value(),"resource.operation."+((ActiveResourceOperationComponentSpec)node).operation().variant().name().toLowerCase());}
			for(ContractNodeSpec node:draft.classOperations())if(node instanceof ResourceClassOperationSpec){entries.add(new BuilderBudgetLedger.Entry(node.id().value(),"class_operation.resource",1));addZero(entries,node.id().value(),"resource.operation."+((ResourceClassOperationSpec)node).operation().variant().name().toLowerCase());spent++;}
			return new BuilderBudgetLedger(PRICE_VERSION, spent, draft.budgetMetadata().baseBudget(), entries);
		}
		private static void addZero(List<BuilderBudgetLedger.Entry> entries, String owner, String priceKey) {
			entries.add(new BuilderBudgetLedger.Entry(owner, priceKey, 0));
		}
		private static void condition(List<BuilderBudgetLedger.Entry> entries,String owner,ConditionExpr value){if(value instanceof AllOfCondition){addZero(entries,owner,"skill.condition.all_of");if(((AllOfCondition)value).children().isEmpty())addZero(entries,owner,"skill.condition.always");for(ConditionExpr child:((AllOfCondition)value).children())condition(entries,owner,child);}else if(value instanceof AlwaysCondition)addZero(entries,owner,"skill.condition.always");else if(value instanceof BuiltinStatCompareCondition)addZero(entries,owner,"skill.condition.builtin_stat_compare");else if(value instanceof ResourceCompareCondition)addZero(entries,owner,"skill.condition.resource_compare");}
		private static String filterKey(EntityFilterExpr value){if(value instanceof RelationFilterSpec){RelationFilterSpec x=(RelationFilterSpec)value;if(x.relationToClassOwner()==RelationFilterSpec.RelationAlignment.ENEMY)return "skill.target.filter.enemy";return x.includeSelf()?"skill.target.filter.ally_or_self":"skill.target.filter.ally";}return "skill.target.filter."+value.variantKey().toLowerCase();}
		private static String costKey(CostSpec value){return value instanceof NoCostSpec?"none":value.variantKey().toLowerCase();}
		private static String effectKey(EffectSpec value){return value.variantKey().name().toLowerCase();}
		private static int effectPrice(EffectSpec value){if(value instanceof DirectDamageEffectSpec&&((DirectDamageEffectSpec)value).amount() instanceof FixedValueSpec)return 2+Math.max(0,(((FixedValueSpec)((DirectDamageEffectSpec)value).amount()).value()-1)/5);return value instanceof ResourceOperationEffectSpec?1:2;}
	}
}
