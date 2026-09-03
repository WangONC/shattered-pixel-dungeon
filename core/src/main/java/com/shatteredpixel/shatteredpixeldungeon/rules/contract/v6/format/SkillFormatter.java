package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.format;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;

/** Complete English/Simplified-Chinese formatter for the exposed P03 composition. */
public final class SkillFormatter {
	public enum Language { ENGLISH, SIMPLIFIED_CHINESE }
	public String format(SkillSpec skill, Language language) {
		if (skill == null || language == null) throw new IllegalArgumentException("skill and language are required");
		if (!skill.typed() || skill.implementationState() != com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ImplementationState.IMPLEMENTED) {
			return language == Language.ENGLISH ? "Unsupported skill" : "不支持的技能";
		}
		DirectDamageEffectSpec damage = (DirectDamageEffectSpec) skill.effects().primary();
		int amount = ((FixedValueSpec) damage.amount()).value();
		DirectDeliverySpec delivery = (DirectDeliverySpec) skill.delivery();
		String secondary = "";
		if (skill.effects().secondary() != null) {
			int secondaryAmount = ((FixedValueSpec) ((DirectDamageEffectSpec) skill.effects().secondary().effect()).amount()).value();
			secondary = language == Language.ENGLISH ? ", then immediately deal " + secondaryAmount + " more"
					: "，随后立即再造成 " + secondaryAmount + " 点伤害";
		}
		if (language == Language.ENGLISH) return skill.displayName().text() + ": Active; always; choose one enemy within "
				+ skill.targeting().range() + " tile(s); deal " + amount + " native direct damage" + secondary
				+ "; " + (delivery.requiresLineOfSight() ? "requires line of sight" : "line of sight not required") + "; no cost.";
		return skill.displayName().text() + "：主动；始终可用；选择 " + skill.targeting().range()
				+ " 格内一个敌人；造成 " + amount + " 点原生直接伤害" + secondary + "；"
				+ (delivery.requiresLineOfSight() ? "需要视线" : "不要求视线") + "；无消耗。";
	}
}
