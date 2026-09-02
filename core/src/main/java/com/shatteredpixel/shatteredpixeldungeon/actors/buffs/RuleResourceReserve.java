package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.utils.Bundle;

/** Saved reservation which returns its held amount after the declared duration. */
public class RuleResourceReserve extends Buff {
	private String resourceId=""; private int amount; private int turns;
	{type=buffType.NEUTRAL;}
	public void set(String id,int amount,int duration){resourceId=id==null?"":id;this.amount=Math.max(1,amount);turns=Math.max(1,duration);spend(TICK);}
	@Override public boolean act(){
		if(--turns<=0){if(target instanceof Hero&&((Hero)target).ruleRuntime()!=null)((Hero)target).ruleRuntime().changeResource((Hero)target,resourceId,null,amount,false);detach();}
		else spend(TICK);return true;
	}
	@Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("resource",resourceId);b.put("amount",amount);b.put("turns",turns);}
	@Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);resourceId=b.getString("resource");amount=b.getInt("amount");turns=b.getInt("turns");}
}
