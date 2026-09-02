package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.watabou.utils.Bundle;

/** Generic temporary mode. Skill conditions read the stable mode id; no entity class replacement occurs. */
public class RuleMode extends Buff {
	private String modeId = "default";
	private int turns;
	private boolean persistent;
	{ type = buffType.POSITIVE; announced = false; }
	public void set(String id, int duration) { modeId=id==null?"default":id; turns=Math.max(1,duration); persistent=false; spend(TICK); }
	/** Class-level Mode Engine states persist until the player performs Mode Switch again. */
	public void setPersistent(String id) { modeId=id==null?"default":id; turns=0; persistent=true; spend(TICK); }
	public String modeId() { return modeId; }
	public int remainingTurns() { return turns; }
	public boolean persistent() { return persistent; }
	@Override public boolean act(){ if (!persistent && --turns <= 0) detach(); else spend(TICK); return true; }
	@Override public void storeInBundle(Bundle b){ super.storeInBundle(b); b.put("mode",modeId); b.put("turns",turns); b.put("persistent",persistent); }
	@Override public void restoreFromBundle(Bundle b){ super.restoreFromBundle(b); modeId=b.getString("mode"); turns=b.getInt("turns"); persistent=b.getBoolean("persistent"); }
}
