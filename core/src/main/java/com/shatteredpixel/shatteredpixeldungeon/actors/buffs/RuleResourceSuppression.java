package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.watabou.utils.Bundle;

/** Timed suppression of positive rule-resource gain. Spending remains legal. */
public class RuleResourceSuppression extends Buff {
	private String resourceId=""; private int turns;
	{type=buffType.NEGATIVE;}
	public void set(String id,int duration){resourceId=id==null?"":id;turns=Math.max(1,duration);spend(TICK);}
	public boolean suppresses(String id){return resourceId.isEmpty()||resourceId.equals(id);}
	@Override public boolean act(){if(--turns<=0)detach();else spend(TICK);return true;}
	@Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("resource",resourceId);b.put("turns",turns);}
	@Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);resourceId=b.getString("resource");turns=b.getInt("turns");}
}
