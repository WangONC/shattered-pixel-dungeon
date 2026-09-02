package com.shatteredpixel.shatteredpixeldungeon.levels.traps;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleContext;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrace;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillEffectRuntime;
import com.watabou.utils.Bundle;

/** A visible, saved Level trap carrying an arbitrary validated EffectSpec. */
public class RuleCarrierTrap extends Trap {
	private int ownerId=-1;
	private EffectSpec payload;
	public RuleCarrierTrap(){color=TEAL;shape=CROSSHAIR;visible=true;canBeHidden=false;}
	public RuleCarrierTrap configure(Hero owner,EffectSpec payload){ownerId=owner==null?-1:owner.id();this.payload=payload==null?null:payload.copy();return this;}
	@Override public void activate(){
		Actor a=Actor.findById(ownerId);Hero owner=a instanceof Hero?(Hero)a:Dungeon.hero;Char target=Actor.findChar(pos);
		if(owner!=null&&payload!=null){RuleContext c=new RuleContext(RuleEvent.ON_ENTER_TILE,owner);c.source=owner;c.target=target;c.cell=pos;boolean ok=SkillEffectRuntime.apply(payload,owner.ruleRuntime(),c,target,pos,new RuleModifier());if(ok&&owner.ruleRuntime()!=null)owner.ruleRuntime().onCarrierTriggered(owner);RuleTrace.record("CARRIER","trap cell="+pos+" applied="+ok);}
	}
	@Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("owner",ownerId);if(payload!=null)b.put("payload",payload);}
	@Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);ownerId=b.getInt("owner");payload=b.contains("payload")?(EffectSpec)b.get("payload"):null;}
}
