package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** P04 target/delivery preflight. It freezes and validates the whole target set before any cost. */
public final class SkillTargetPreflight {
	public PreflightResult resolve(CompiledSkill skill,RuntimeExecutionContext context,RuntimeTrace trace){
		CompiledSkill.Targeting targeting=skill.targeting();CompiledSkill.Delivery delivery=skill.delivery();
		if(!supported(targeting,delivery)){trace.record("target_preflight","status=UNSUPPORTED reason=targeting_or_delivery");return PreflightResult.failed(PreflightResult.Status.UNSUPPORTED,"preflight.targeting_or_delivery_unsupported");}
		Char owner=context.classOwner();if(owner==null){trace.record("target_preflight","status=BLOCKED reason=owner_unavailable");return PreflightResult.failed(PreflightResult.Status.BLOCKED,"preflight.owner_unavailable");}
		if(Dungeon.level==null){trace.record("target_preflight","status=BLOCKED reason=level_absent");return PreflightResult.failed(PreflightResult.Status.BLOCKED,"preflight.level_absent");}
		Char selected=context.selectedActor();int targetCell=targeting.selector()==CompiledSkill.SelectorVariant.SELF?owner.pos:context.event().selectedCell();
		if(targeting.selector()==CompiledSkill.SelectorVariant.SELECTED_ACTOR&&selected==null){trace.record("target_preflight","status=NO_TARGET reason=selected_actor_absent cell="+targetCell);return PreflightResult.failed(PreflightResult.Status.NO_TARGET,"preflight.no_selected_actor");}
		if(targetCell<0||targetCell>=Dungeon.level.length())return PreflightResult.failed(PreflightResult.Status.NO_TARGET,"preflight.selected_cell_invalid");
		int distance=Dungeon.level.distance(owner.pos,targetCell);if(distance>targeting.range()){trace.record("target_preflight","status=NO_TARGET reason=range distance="+distance);return PreflightResult.failed(PreflightResult.Status.NO_TARGET,"preflight.out_of_range");}
		if(delivery.variant()==CompiledSkill.DeliveryVariant.CONTACT&&distance>1)return PreflightResult.failed(PreflightResult.Status.BLOCKED,"preflight.contact_out_of_range");
		if(delivery.requiresLineOfSight()){Ballistica path=new Ballistica(owner.pos,targetCell,Ballistica.PROJECTILE);if(path.collisionPos==null||path.collisionPos!=targetCell){trace.record("target_preflight","status=BLOCKED reason=line_of_sight target_cell="+targetCell);return PreflightResult.failed(PreflightResult.Status.BLOCKED,"preflight.line_of_sight_blocked");}}
		List<Char> accepted=new ArrayList<>();for(Char candidate:candidates(skill,owner,selected,targetCell))if(candidate!=null&&candidate.isAlive()&&accepted(targeting.filter(),owner,candidate))accepted.add(candidate);
		Collections.sort(accepted,Comparator.comparingInt((Char value)->Dungeon.level.distance(value.pos,targetCell)).thenComparingInt(value->value.pos).thenComparingInt(Char::id));
		int maximum=targeting.maximumTargets();if(skill.modifier().variant()==CompiledSkill.ModifierVariant.PIERCE)maximum+=skill.modifier().first();if(skill.modifier().variant()==CompiledSkill.ModifierVariant.BOUNCE)maximum=Math.max(maximum,1+skill.modifier().first());
		if(accepted.size()>maximum)accepted=new ArrayList<>(accepted.subList(0,maximum));
		if(accepted.isEmpty()){trace.record("target_preflight","status=NO_TARGET reason=filter_or_coverage cell="+targetCell+" filter="+targeting.filter());return PreflightResult.failed(PreflightResult.Status.NO_TARGET,"preflight.filter_rejected");}
		trace.record("target_preflight","status=READY actors="+actorIds(accepted)+" cell="+targetCell+" distance="+distance+" coverage="+targeting.coverage()+" filter="+targeting.filter());return PreflightResult.ready(accepted);
	}
	private static List<Char> candidates(CompiledSkill skill,Char owner,Char selected,int targetCell){CompiledSkill.Targeting targeting=skill.targeting();List<Char> out=new ArrayList<>();if(targeting.coverage()==CompiledSkill.CoverageVariant.SINGLE){Char target=targeting.selector()==CompiledSkill.SelectorVariant.SELF?owner:targeting.selector()==CompiledSkill.SelectorVariant.SELECTED_CELL?(selected==null?owner:selected):selected;if(target!=null)out.add(target);if(skill.modifier().variant()==CompiledSkill.ModifierVariant.PIERCE||skill.modifier().variant()==CompiledSkill.ModifierVariant.BOUNCE)for(Char candidate:Actor.chars())if(!out.contains(candidate)&&(skill.modifier().variant()==CompiledSkill.ModifierVariant.PIERCE||Dungeon.level.distance(candidate.pos,targetCell)<=skill.modifier().second()))out.add(candidate);return out;}Ballistica line=targeting.coverage()==CompiledSkill.CoverageVariant.LINE?new Ballistica(owner.pos,targetCell,Ballistica.STOP_TARGET):null;for(Char candidate:Actor.chars()){boolean include;switch(targeting.coverage()){case ADJACENT:include=Dungeon.level.distance(candidate.pos,targetCell)<=1;break;case RADIUS:include=Dungeon.level.distance(candidate.pos,targetCell)<=targeting.coverageFirst();break;case LINE:include=line.path.contains(candidate.pos);break;default:include=false;}if(include)out.add(candidate);}if(selected!=null&&!out.contains(selected))out.add(selected);return out;}
	private static boolean supported(CompiledSkill.Targeting t,CompiledSkill.Delivery d){return t.selector()!=null&&t.coverage()!=null&&t.filter()!=null&&d.variant()!=null&&t.maximumTargets()>0;}
	private static boolean accepted(CompiledSkill.FilterVariant filter,Char owner,Char target){switch(filter){case ANY_ACTOR:return true;case SELF:return target==owner;case ENEMY_EXCLUDE_SELF:return target!=owner&&target.alignment==Char.Alignment.ENEMY;case ALLY_EXCLUDE_SELF:return target!=owner&&target.alignment==owner.alignment;case ALLY_INCLUDE_SELF:return target==owner||target.alignment==owner.alignment;default:return false;}}
	private static String actorIds(List<Char> values){StringBuilder out=new StringBuilder();for(Char value:values){if(out.length()>0)out.append(',');out.append(value.id());}return out.toString();}
}
