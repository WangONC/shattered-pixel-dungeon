package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.utils.Bundle;

/** Generic saved directional relation. Concrete effects may query it, but it carries no hidden payload. */
public class RuleRelation extends Buff {
	public enum Type { LINK, INHERIT_CAPABILITY }
	private Type relationType = Type.LINK;
	private int sourceId = -1;
	private String capability = "";
	private int turns;
	{ type = buffType.NEUTRAL; announced = false; }
	public void set(Type type, Char source, String capability, int duration) {
		this.relationType=type; sourceId=source==null?-1:source.id(); this.capability=capability==null?"":capability;
		turns=Math.max(1,duration); spend(TICK);
	}
	public Type relationType(){return relationType;} public int sourceId(){return sourceId;}
	public Char source(){Actor a=Actor.findById(sourceId); return a instanceof Char?(Char)a:null;}
	public String capability(){return capability;}
	@Override public boolean act(){Char currentSource=source();if(--turns<=0||currentSource==null||!currentSource.isAlive())detach();else spend(TICK);return true;}
	@Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("relation",relationType);b.put("source",sourceId);b.put("capability",capability);b.put("turns",turns);}
	@Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);relationType=b.getEnum("relation",Type.class);sourceId=b.getInt("source");capability=b.getString("capability");turns=b.getInt("turns");}
}
