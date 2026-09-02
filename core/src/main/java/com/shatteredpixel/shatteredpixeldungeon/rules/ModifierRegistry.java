package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/** Skill-affix exposure backed by the real RuleModifier compatibility contract. */
public final class ModifierRegistry {
	private static final List<RuleModifier.Type> VALUES = Collections.unmodifiableList(Arrays.asList(
			RuleModifier.Type.NONE, RuleModifier.Type.INTENSITY, RuleModifier.Type.REPEAT,
			RuleModifier.Type.EXTEND_DURATION, RuleModifier.Type.PIERCE, RuleModifier.Type.BOUNCE,
			RuleModifier.Type.DELAY, RuleModifier.Type.AREA));
	private ModifierRegistry() {}
	public static List<RuleModifier.Type> exposed() { return VALUES; }
	public static List<RuleModifier> options() {
		ArrayList<RuleModifier> result=new ArrayList<>();
		result.add(new RuleModifier());
		for(RuleModifier.Type type:VALUES)if(type!=RuleModifier.Type.NONE){
			int first=type==RuleModifier.Type.AREA?1:2;
			for(int magnitude=first;magnitude<=4;magnitude++){RuleModifier value=new RuleModifier(type);value.magnitude=magnitude;result.add(value);}
		}
		return Collections.unmodifiableList(result);
	}
	public static String name(RuleModifier.Type value) { return Messages.get(ModifierRegistry.class,
			value.name().toLowerCase() + "_name"); }
	public static String name(RuleModifier value) { return value.type==RuleModifier.Type.NONE?name(value.type)
			:Messages.get(ModifierRegistry.class,"configured_name",name(value.type),value.magnitude); }
	public static String summary(RuleModifier.Type value) { return Messages.get(ModifierRegistry.class,
			value.name().toLowerCase() + "_summary"); }
	public static boolean compatible(RuleModifier.Type value, SkillSpec skill) {
		return new RuleModifier(value).compatible(skill.primary, skill.delivery);
	}
}
