package com.shatteredpixel.shatteredpixeldungeon.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Player-visible delivery list; unsupported enum values can never leak into the builder. */
public final class DeliveryRegistry {
	private static final List<SkillDelivery> VALUES = Collections.unmodifiableList(Arrays.asList(
			SkillDelivery.SELF, SkillDelivery.CONTACT_ATTACK, SkillDelivery.DIRECT_TARGET,
			SkillDelivery.PROJECTILE, SkillDelivery.TRACE_BEAM, SkillDelivery.GROUND_PLACEMENT,
			SkillDelivery.PERSISTENT_CARRIER, SkillDelivery.ACTION_ATTACHMENT));
	private DeliveryRegistry() {}
	public static final List<RuleEvent> ATTACHMENT_EVENTS = Collections.unmodifiableList(Arrays.asList(
			RuleEvent.ON_ATTACK, RuleEvent.ON_HIT, RuleEvent.ON_MOVE, RuleEvent.ON_DAMAGED,
			RuleEvent.ON_WAIT, RuleEvent.ON_ITEM_USE));
	public static List<SkillDelivery> exposed() { return VALUES; }
	public static boolean compatible(SkillDelivery delivery, SkillSpec skill) {
		return delivery != SkillDelivery.ACTION_ATTACHMENT || skill.activation == RuleEvent.ACTIVE;
	}
	public static boolean parametersValid(SkillSpec skill) {
		if (skill == null || skill.delivery == null || skill.primary == null) return false;
		if (skill.delivery == SkillDelivery.PERSISTENT_CARRIER) {
			return skill.primary.lifetime >= 1 && skill.primary.lifetime <= 16
					&& skill.primary.period >= 1 && skill.primary.period <= 4;
		}
		if (skill.delivery == SkillDelivery.ACTION_ATTACHMENT) {
			return ATTACHMENT_EVENTS.contains(skill.attachmentEvent)
					&& skill.attachmentCharges >= 1 && skill.attachmentCharges <= 5;
		}
		return true;
	}
}
