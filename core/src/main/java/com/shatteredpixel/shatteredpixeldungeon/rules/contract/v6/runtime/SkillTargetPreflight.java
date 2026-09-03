package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;

/** Native actor/level targeting preflight for the exact P03 supported combination. */
public final class SkillTargetPreflight {
	public PreflightResult resolve(SkillSpec skill, GameplayEventContext context, RuntimeTrace trace) {
		if (!(skill.targeting().selector() instanceof SelectedActorSelector)
				|| !(skill.targeting().coverage() instanceof SingleCoverageSpec)
				|| !(skill.targeting().filter() instanceof RelationFilterSpec)
				|| !(skill.delivery() instanceof DirectDeliverySpec)) {
			trace.record("preflight", "status=UNSUPPORTED reason=targeting_or_delivery");
			return PreflightResult.failed(PreflightResult.Status.UNSUPPORTED, "preflight.targeting_or_delivery_unsupported");
		}
		Char target = context.selectedActor();
		if (target == null) {
			trace.record("preflight", "status=NO_TARGET reason=selected_actor_absent");
			return PreflightResult.failed(PreflightResult.Status.NO_TARGET, "preflight.no_selected_actor");
		}
		if (!target.isAlive()) {
			trace.record("preflight", "status=BLOCKED reason=target_not_alive actor=" + target.id());
			return PreflightResult.failed(PreflightResult.Status.BLOCKED, "preflight.target_not_alive");
		}
		RelationFilterSpec filter = (RelationFilterSpec) skill.targeting().filter();
		boolean enemy = target.alignment != context.classOwner().alignment;
		if (filter.relationToClassOwner() != RelationFilterSpec.RelationAlignment.ENEMY
				|| !enemy || (!filter.includeSelf() && target == context.classOwner())) {
			trace.record("preflight", "status=NO_TARGET reason=filter actor=" + target.id());
			return PreflightResult.failed(PreflightResult.Status.NO_TARGET, "preflight.filter_rejected");
		}
		if (Dungeon.level == null) {
			trace.record("preflight", "status=BLOCKED reason=level_absent");
			return PreflightResult.failed(PreflightResult.Status.BLOCKED, "preflight.level_absent");
		}
		int distance = Dungeon.level.distance(context.classOwner().pos, target.pos);
		if (distance > skill.targeting().range()) {
			trace.record("preflight", "status=NO_TARGET reason=range distance=" + distance);
			return PreflightResult.failed(PreflightResult.Status.NO_TARGET, "preflight.out_of_range");
		}
		DirectDeliverySpec delivery = (DirectDeliverySpec) skill.delivery();
		if (delivery.requiresLineOfSight()) {
			Ballistica path = new Ballistica(context.classOwner().pos, target.pos, Ballistica.PROJECTILE);
			if (path.collisionPos == null || path.collisionPos != target.pos) {
				trace.record("preflight", "status=BLOCKED reason=line_of_sight target_cell=" + target.pos);
				return PreflightResult.failed(PreflightResult.Status.BLOCKED, "preflight.line_of_sight_blocked");
			}
		}
		trace.record("preflight", "status=READY actor=" + target.id() + " cell=" + target.pos + " distance=" + distance);
		return PreflightResult.ready(target);
	}
}
