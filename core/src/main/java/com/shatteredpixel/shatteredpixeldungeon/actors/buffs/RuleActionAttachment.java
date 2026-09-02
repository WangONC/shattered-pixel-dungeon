package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.rules.*;
import com.watabou.utils.Bundle;

/** Saved generic payload attached to a future gameplay action. */
public class RuleActionAttachment extends Buff {
	private EffectSpec payload;
	private EffectSpec secondaryPayload;
	private RuleEvent event=RuleEvent.ON_HIT;
	private int charges=1;
	private String sourceRule="";
	{type=buffType.POSITIVE;announced=false;}
	public static boolean install(Hero hero,EffectSpec payload,RuleEvent event,int charges,String sourceRule){
		return install(hero,payload,null,event,charges,sourceRule);
	}
	public static boolean install(Hero hero,EffectSpec payload,EffectSpec secondary,RuleEvent event,int charges,String sourceRule){
		if(hero==null||payload==null||!payload.implemented())return false;
		RuleActionAttachment value=Buff.append(hero,RuleActionAttachment.class);value.payload=payload.copy();
		value.secondaryPayload=secondary==null?null:secondary.copy();
		value.event=event==null?RuleEvent.ON_HIT:event;value.charges=Math.max(1,charges);value.sourceRule=sourceRule==null?"":sourceRule;
		// Infinity is not legal JSON and used to corrupt headless/formal Bundle saves.
		// A one-tick dormant cadence is cheap, deterministic, and naturally serializable.
		value.spend(TICK);return true;
	}
	public static void trigger(Hero hero,RuleContext context){
		if(hero==null||context==null)return;
		for(RuleActionAttachment value:new java.util.ArrayList<>(hero.buffs(RuleActionAttachment.class))){
			if(value.event!=context.event||value.payload==null)continue;
			EffectSpec payload=value.payload.copy();String id=value.sourceRule;
			if(--value.charges<=0)value.detach();
			RuleContext child=context.causedByRule(id);boolean applied=SkillEffectRuntime.apply(payload,hero.ruleRuntime(),child,context.target,context.cell,new RuleModifier());
			boolean secondaryApplied=applied&&value.secondaryPayload!=null&&SkillEffectRuntime.apply(value.secondaryPayload,hero.ruleRuntime(),child,context.target,context.cell,new RuleModifier());
			RuleTrace.record("ATTACHMENT","rule="+id+" event="+context.event+" applied="+applied);
			if(value.secondaryPayload!=null)RuleTrace.record("SECONDARY","attachment rule="+id+" applied="+secondaryApplied);
		}
	}
	@Override public boolean act(){spend(TICK);return true;}
	@Override public void storeInBundle(Bundle b){super.storeInBundle(b);if(payload!=null)b.put("payload",payload);if(secondaryPayload!=null)b.put("secondary_payload",secondaryPayload);b.put("event",event);b.put("charges",charges);b.put("source_rule",sourceRule);}
	@Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);payload=b.contains("payload")?(EffectSpec)b.get("payload"):null;secondaryPayload=b.contains("secondary_payload")?(EffectSpec)b.get("secondary_payload"):null;event=b.getEnum("event",RuleEvent.class);charges=Math.max(1,b.getInt("charges"));sourceRule=b.getString("source_rule");}
}
