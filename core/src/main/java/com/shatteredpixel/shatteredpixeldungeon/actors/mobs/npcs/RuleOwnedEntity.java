package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleOwnership;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleContext;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrace;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillEffectRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RatSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

/** Minimal real Actor-backed owned creature/device using normal Mob scheduling and Level persistence. */
public class RuleOwnedEntity extends DirectableAlly {
	public enum Kind { ACTOR, DEVICE, FIELD }
	private Kind kind = Kind.ACTOR;
	private int lifetime = 8;
	private int ownerId = -1;
	private int period = 1;
	private EffectSpec payload;
	private EffectSpec secondaryPayload;
	private TargetingSpec.Filter payloadFilter = TargetingSpec.Filter.ENEMY;
	private boolean removalReported;
	{
		spriteClass = RatSprite.class;
		HP = HT = 8;
		damage = 2;
		defenseSkill = 4;
		EXP = 0;
		maxLvl = -1;
	}
	private int damage;
	public RuleOwnedEntity configure(Kind kind, Hero owner, int lifetime, int health, int power, int period, EffectSpec payload) {
		return configure(kind,owner,lifetime,health,power,period,payload,TargetingSpec.Filter.ENEMY);
	}
	public RuleOwnedEntity configure(Kind kind, Hero owner, int lifetime, int health, int power, int period, EffectSpec payload, TargetingSpec.Filter filter) {
		return configure(kind,owner,lifetime,health,power,period,payload,null,filter);
	}
	public RuleOwnedEntity configure(Kind kind, Hero owner, int lifetime, int health, int power, int period, EffectSpec payload, EffectSpec secondary, TargetingSpec.Filter filter) {
		this.kind=kind; ownerId=owner==null?-1:owner.id(); this.lifetime=Math.max(1,lifetime);
		HT=HP=Math.max(1,health); damage=Math.max(1,power); this.period=Math.max(1,period);
		this.payload=payload==null?null:payload.copy();secondaryPayload=secondary==null?null:secondary.copy();payloadFilter=filter==null?TargetingSpec.Filter.ENEMY:filter; attacksAutomatically=kind==Kind.ACTOR;
		if(owner!=null)RuleOwnership.assign(this,owner);
		if (owner == null || owner.sprite == null || owner.sprite.parent == null) sprite = new SilentSprite(this);
		return this;
	}
	public int ownerId(){return ownerId;} public Kind kind(){return kind;} public int lifetime(){return lifetime;}
	@Override public int damageRoll(){return damage;}
	@Override public int attackSkill(Char target){return 8+damage*2;}
	@Override public boolean attack(Char target,float damageMultiplier,float damageBonus,float accuracyMultiplier){
		RuleTrace.record("OWNED_ATTACK","actor=#"+id()+" target=#"+(target==null?-1:target.id()));
		boolean result=super.attack(target,damageMultiplier,damageBonus,accuracyMultiplier);
		Hero owner=owner();
		if(result&&owner!=null&&owner.ruleRuntime()!=null)owner.ruleRuntime().onOwnedEntityAction(owner,this,target);
		return result;
	}
	@Override protected boolean act(){
		if(owner()==null||--lifetime<=0){die(null);return true;}
		if(kind==Kind.DEVICE||kind==Kind.FIELD){
			Hero owner=owner(); Char target=nearestEnemy();
			if(owner!=null&&payload!=null&&kind==Kind.FIELD){for(Char ch:Actor.chars())if(ch.isAlive()&&Dungeon.level.distance(pos,ch.pos)<=1&&ch!=this&&matches(ch,owner)){RuleContext c=new RuleContext(RuleEvent.ON_TURN_START,owner);c.source=this;c.target=ch;c.cell=ch.pos;boolean ok=SkillEffectRuntime.apply(payload,owner.ruleRuntime(),c,ch,ch.pos,new RuleModifier());if(ok&&secondaryPayload!=null)SkillEffectRuntime.apply(secondaryPayload,owner.ruleRuntime(),c,ch,ch.pos,new RuleModifier());}}
			else if(owner!=null&&target!=null&&payload!=null){RuleContext c=new RuleContext(RuleEvent.ON_TURN_START,owner);c.source=this;c.target=target;c.cell=target.pos;boolean ok=SkillEffectRuntime.apply(payload,owner.ruleRuntime(),c,target,target.pos,new RuleModifier());if(ok&&secondaryPayload!=null)SkillEffectRuntime.apply(secondaryPayload,owner.ruleRuntime(),c,target,target.pos,new RuleModifier());}
			spend(period);return true;
		}
		// Acquire a deterministic target, then let DirectableAlly/Mob perform the real attack,
		// movement, timing, evasion and damage path. Guard orders are not overridden at range.
		Char automaticTarget=nearestEnemy();
		if(enemy==null&&automaticTarget!=null&&(defendingPos==-1||canAttack(automaticTarget)))targetChar(automaticTarget);
		RuleTrace.record("OWNED_ACTOR","turn actor=#"+id()+" pos="+pos+" target="+(automaticTarget==null?"none":"#"+automaticTarget.id()+"@"+automaticTarget.pos)+" adjacent="+(automaticTarget!=null&&Dungeon.level.adjacent(pos,automaticTarget.pos)));
		return super.act();
	}
	private Hero owner(){Actor a=Actor.findById(ownerId);return a instanceof Hero&&((Hero)a).isAlive()?(Hero)a:null;}
	private Char nearestEnemy(){Char best=null;int dist=Integer.MAX_VALUE;if(Dungeon.level==null)return null;for(Char ch:Actor.chars())if(ch.isAlive()&&ch.alignment==Alignment.ENEMY){int d=Dungeon.level.distance(pos,ch.pos);if(d<dist||(d==dist&&best!=null&&ch.id()<best.id())){best=ch;dist=d;}}return best;}
	private boolean matches(Char ch,Hero owner){switch(payloadFilter){case SELF:return ch==owner;case ALLY:return ch.alignment==owner.alignment;case OWNED_ENTITY:return RuleOwnership.isOwnedBy(ch,owner);case ANY:return true;case ENEMY:default:return ch.alignment==Alignment.ENEMY;}}
	@Override public void die(Object cause){
		Hero owner=owner();
		if(!removalReported&&owner!=null&&owner.ruleRuntime()!=null){
			removalReported=true;
			owner.ruleRuntime().onOwnedEntityRemoved(owner,this);
		}
		super.die(cause);
	}
	@Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("kind",kind);b.put("lifetime",lifetime);b.put("owner",ownerId);b.put("period",period);b.put("damage",damage);b.put("payload_filter",payloadFilter);if(payload!=null)b.put("payload",payload);if(secondaryPayload!=null)b.put("secondary_payload",secondaryPayload);}
	@Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);kind=b.getEnum("kind",Kind.class);lifetime=b.getInt("lifetime");ownerId=b.getInt("owner");period=Math.max(1,b.getInt("period"));damage=Math.max(1,b.getInt("damage"));payload=b.contains("payload")?(EffectSpec)b.get("payload"):null;secondaryPayload=b.contains("secondary_payload")?(EffectSpec)b.get("secondary_payload"):null;payloadFilter=b.contains("payload_filter")?b.getEnum("payload_filter",TargetingSpec.Filter.class):TargetingSpec.Filter.ENEMY;attacksAutomatically=kind==Kind.ACTOR;}
	private static class SilentSprite extends CharSprite {
		SilentSprite(Char owner){ch=owner;visible=false;}
		@Override public void showStatus(int color,String text,Object...args){}
		@Override public void place(int cell){}
		@Override public void move(int from,int to){}
		@Override public void turnTo(int from,int to){}
		@Override public synchronized void attack(int cell,Callback callback){if(callback!=null)callback.call();else ch.onAttackComplete();}
		@Override public synchronized void operate(int cell,Callback callback){if(callback!=null)callback.call();else ch.onOperateComplete();}
		@Override public synchronized void zap(int cell,Callback callback){if(callback!=null)callback.call();else ch.onAttackComplete();}
		@Override public void interruptMotion(){isMoving=false;}
		@Override public void die(){}
		@Override public void bloodBurstA(PointF from,int damage){}
		@Override public void flash(){}
		@Override public void showSleep(){}
		@Override public void hideSleep(){}
		@Override public void showAlert(){}
		@Override public void hideAlert(){}
		@Override public void showInvestigate(){}
		@Override public void hideInvestigate(){}
		@Override public void showLost(){}
		@Override public void hideLost(){}
	}
}
