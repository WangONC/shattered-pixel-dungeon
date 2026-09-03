package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;

/** Native target selection for the exact compiled P03 targeting tuple. */
public final class SkillTargetPreflight {
	public PreflightResult resolve(CompiledSkill skill,RuntimeExecutionContext context,RuntimeTrace trace){
		CompiledSkill.Targeting targeting=skill.targeting();CompiledSkill.Delivery delivery=skill.delivery();
		if(targeting.selector()!=CompiledSkill.SelectorVariant.SELECTED_ACTOR||targeting.coverage()!=CompiledSkill.CoverageVariant.SINGLE
				||targeting.filter()!=CompiledSkill.FilterVariant.ENEMY_EXCLUDE_SELF||delivery.variant()!=CompiledSkill.DeliveryVariant.DIRECT){
			trace.record("target_preflight","status=UNSUPPORTED reason=targeting_or_delivery");return PreflightResult.failed(PreflightResult.Status.UNSUPPORTED,"preflight.targeting_or_delivery_unsupported");}
		Char owner=context.classOwner();Char target=context.selectedActor();
		if(owner==null){trace.record("target_preflight","status=BLOCKED reason=owner_unavailable");return PreflightResult.failed(PreflightResult.Status.BLOCKED,"preflight.owner_unavailable");}
		if(target==null){trace.record("target_preflight","status=NO_TARGET reason=selected_actor_absent cell="+context.event().selectedCell());return PreflightResult.failed(PreflightResult.Status.NO_TARGET,"preflight.no_selected_actor");}
		if(!target.isAlive()){trace.record("target_preflight","status=BLOCKED reason=target_not_alive actor="+target.id());return PreflightResult.failed(PreflightResult.Status.BLOCKED,"preflight.target_not_alive");}
		if(target==owner||target.alignment==owner.alignment){trace.record("target_preflight","status=NO_TARGET reason=filter actor="+target.id());return PreflightResult.failed(PreflightResult.Status.NO_TARGET,"preflight.filter_rejected");}
		if(Dungeon.level==null){trace.record("target_preflight","status=BLOCKED reason=level_absent");return PreflightResult.failed(PreflightResult.Status.BLOCKED,"preflight.level_absent");}
		int distance=Dungeon.level.distance(owner.pos,target.pos);if(distance>targeting.range()){trace.record("target_preflight","status=NO_TARGET reason=range distance="+distance);return PreflightResult.failed(PreflightResult.Status.NO_TARGET,"preflight.out_of_range");}
		if(delivery.requiresLineOfSight()){Ballistica path=new Ballistica(owner.pos,target.pos,Ballistica.PROJECTILE);if(path.collisionPos==null||path.collisionPos!=target.pos){trace.record("target_preflight","status=BLOCKED reason=line_of_sight target_cell="+target.pos);return PreflightResult.failed(PreflightResult.Status.BLOCKED,"preflight.line_of_sight_blocked");}}
		trace.record("target_preflight","status=READY actor="+target.id()+" cell="+target.pos+" distance="+distance);return PreflightResult.ready(target);
	}
}
