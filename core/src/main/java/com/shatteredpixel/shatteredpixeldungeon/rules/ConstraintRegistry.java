package com.shatteredpixel.shatteredpixeldungeon.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Player-visible, implemented constraints with conservative default parameters. */
public final class ConstraintRegistry {
	private static final List<SkillConstraint> VALUES;
	static {
		java.util.ArrayList<SkillConstraint> values=new java.util.ArrayList<>();values.add(new SkillConstraint());
		for(int amount=1;amount<=3;amount++)values.add(new SkillConstraint(SkillConstraint.Variant.TARGET_MARKED,amount));
		for(int percent:new int[]{20,30,35,40,45,50})values.add(new SkillConstraint(SkillConstraint.Variant.SELF_LOW_HP,percent));
		values.add(new SkillConstraint(SkillConstraint.Variant.SELF_IN_WATER,0));
		for(int turns=2;turns<=10;turns++)values.add(new SkillConstraint(SkillConstraint.Variant.COOLDOWN,turns));
		for(int amount=1;amount<=5;amount++)values.add(new SkillConstraint(SkillConstraint.Variant.HP_COMMITMENT,amount));
		for(int uses=1;uses<=6;uses++)values.add(new SkillConstraint(SkillConstraint.Variant.LIMITED_USE,uses));
		VALUES=Collections.unmodifiableList(values);
	}
	private ConstraintRegistry() {}
	public static List<SkillConstraint> exposed() { return VALUES; }
	public static boolean compatible(SkillConstraint value, SkillSpec skill) {
		return value.implemented()
				&& (value.variant != SkillConstraint.Variant.HP_COMMITMENT || skill.cost.type == RuleCost.Type.HP)
				&& (value.variant != SkillConstraint.Variant.COOLDOWN || skill.cost.type != RuleCost.Type.COOLDOWN);
	}
}
