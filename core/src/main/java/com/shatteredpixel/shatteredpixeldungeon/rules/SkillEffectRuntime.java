package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.RuleCarrierTrap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

import java.util.ArrayList;

/** Concrete executor for non-legacy EffectSpec operations. Unknown/protected operations fail closed. */
public final class SkillEffectRuntime {
	private SkillEffectRuntime() {}

	public static boolean apply(EffectSpec spec, RuleRuntime runtime, RuleContext context,
			Char target, int cell, RuleModifier modifier) {
		if (spec == null || !spec.implemented() || context == null || context.hero == null) return false;
		if (spec.operation == EffectSpec.Operation.LEGACY) return spec.compile().apply(context,target,cell,modifier);
		int power=Math.max(1,Math.round(spec.power*(modifier==null?1f:modifier.powerMultiplier)));
		int duration=Math.max(1,Math.round(spec.duration*(modifier==null?1f:modifier.durationMultiplier)));
		int damagePower=scaledDamagePower(spec,runtime,context,power);
		boolean applied;
		switch(spec.operation){
			case DAMAGE_STANDARD: applied=damage(context,target,damagePower,spec.damageType);break;
			case DAMAGE_PERCENT: applied=damage(context,target,Math.max(1,target==null?0:target.HT*Math.min(25,damagePower)/100),spec.damageType);break;
			case DAMAGE_MISSING_HP: applied=damage(context,target,damagePower+(target==null?0:(target.HT-target.HP)*Math.min(100,spec.secondaryParameter)/100),spec.damageType);break;
			case DAMAGE_EXECUTE:
				if(target==null)return false;
				boolean protectedTarget=Char.hasProp(target,Char.Property.BOSS)||Char.hasProp(target,Char.Property.MINIBOSS);
				int threshold=Math.min(protectedTarget?10:40,Math.max(1,spec.secondaryParameter));
				applied=target.HP*100<=target.HT*threshold&&damage(context,target,protectedTarget?Math.min(target.HP-1,damagePower):target.HP,spec.damageType);break;
			case STATUS_POISON: applied=status(context,target,Poison.class,duration,power,spec.statusStacking);break;
			case STATUS_BURNING: applied=status(context,target,Burning.class,duration,power,spec.statusStacking);break;
			case STATUS_BLEEDING: applied=status(context,target,Bleeding.class,duration,power,spec.statusStacking);break;
			case STATUS_SLOW: applied=status(context,target,Slow.class,duration,power,spec.statusStacking);break;
			case STATUS_HASTE: applied=status(context,target,Haste.class,duration,power,spec.statusStacking);break;
			case STATUS_PARALYSIS: applied=status(context,target,Paralysis.class,Math.min(3,duration),power,spec.statusStacking);break;
			case STATUS_ROOTS: applied=status(context,target,Roots.class,duration,power,spec.statusStacking);break;
			case STATUS_AMOK: applied=status(context,target,Amok.class,duration,power,spec.statusStacking);break;
			case STATUS_TERROR: applied=status(context,target,Terror.class,duration,power,spec.statusStacking);break;
			case STATUS_VULNERABLE: applied=status(context,target,Vulnerable.class,duration,power,spec.statusStacking);break;
			case MOVE_PUSH: applied=new RuleEffect(RuleEffect.Type.PUSH,power).apply(context,target,cell,modifier);break;
			case MOVE_PULL: applied=new RuleEffect(RuleEffect.Type.PULL,power).apply(context,target,cell,modifier);break;
			case MOVE_THROW: applied=new RuleEffect(RuleEffect.Type.PUSH,power+1).apply(context,target,cell,modifier);break;
			case MOVE_DASH:
			case MOVE_TELEPORT: applied=new RuleEffect(RuleEffect.Type.TELEPORT,power).apply(context,target,cell,modifier);break;
			case MOVE_SWAP: applied=new RuleEffect(RuleEffect.Type.SWAP_POSITION,power).apply(context,target,cell,modifier);break;
			case RECOVER_HEAL: applied=new RuleEffect(RuleEffect.Type.HEAL,power).apply(context,target,cell,modifier);break;
			case DEFENSE_BARRIER: applied=new RuleEffect(RuleEffect.Type.SHIELD,power).apply(context,target,cell,modifier);break;
			case DEFENSE_CLEANSE: applied=new RuleEffect(RuleEffect.Type.CLEANSE,power).apply(context,target,cell,modifier);break;
			case DEFENSE_TEMP_HP:
				if(target==null)target=context.hero; RuleTemporaryHP temp=Buff.affect(target,RuleTemporaryHP.class);temp.grant(power,duration,spec.secondaryParameter);applied=true;break;
			case DEFENSE_MITIGATE:
				if(target==null)target=context.hero;Buff.affect(target,RuleMitigation.class).set(Math.min(75,power*5),duration);applied=true;break;
			case DEFENSE_REDIRECT:
				Char recipient=context.source!=null&&context.source!=target?context.source:nearestOwned(context.hero,target);
				if(target==null)target=context.hero;if(recipient==null||recipient==target)return false;
				Buff.affect(target,RuleDamageRedirect.class).set(recipient,Math.min(75,power*10),duration);applied=true;break;
			case RESOURCE_GAIN: applied=runtime!=null&&runtime.changeResource(context.hero,spec.resourceId,null,power,true)>0;break;
			case RESOURCE_DRAIN: applied=runtime!=null&&runtime.changeResource(context.hero,spec.resourceId,null,-power,true)<0;break;
			case RESOURCE_CONVERT: applied=convert(runtime,context.hero,spec,power);break;
			case RESOURCE_RESERVE: applied=reserve(runtime,context.hero,spec,power,duration);break;
			case RESOURCE_SUPPRESS:
				if(target==null)target=context.hero;Buff.affect(target,RuleResourceSuppression.class).set(spec.resourceId,duration);applied=true;break;
			case MARK_APPLY:
			case MARK_STACK:
			case MARK_COUNTER: applied=target!=null&&RuleMark.apply(target,markType(spec.stateId),context.hero,power,duration)!=null;break;
			case MARK_CONSUME: applied=target!=null&&RuleMark.consume(target,markType(spec.stateId),power);break;
			case MARK_SPREAD: applied=spreadMark(target,context.hero,spec,power,duration);break;
			case CREATE_ACTOR: applied=spawn(context,spec,cell,RuleOwnedEntity.Kind.ACTOR);break;
			case CREATE_DEVICE: applied=spawn(context,spec,cell,RuleOwnedEntity.Kind.DEVICE);break;
			case CREATE_FIELD: applied=spawn(context,spec,cell,RuleOwnedEntity.Kind.FIELD);break;
			case CREATE_TRAP: applied=placeTrap(context,spec,cell);break;
			case WORLD_WATER: applied=new RuleEffect(RuleEffect.Type.CREATE_WATER,power).apply(context,target,cell,modifier);break;
			case WORLD_TOXIC_GAS: applied=new RuleEffect(RuleEffect.Type.CREATE_GAS,power).apply(context,target,cell,modifier);break;
			case WORLD_FIRE: applied=new RuleEffect(RuleEffect.Type.FIRE,power).apply(context,target,cell,modifier);break;
			case WORLD_GRASS: applied=setTerrain(cell,WorldCapability.REPLACEABLE,Terrain.GRASS);break;
			case WORLD_DESTROY: applied=setTerrain(cell,WorldCapability.DESTRUCTIBLE,Terrain.EMPTY);break;
			case WORLD_CLEAR_HAZARD: applied=clearHazard(cell);break;
			case RELATION_OWNERSHIP: applied=target!=null&&RuleOwnership.assign(target,context.hero)!=null;break;
			case RELATION_LINK:
				if(target==null)return false;RuleRelation link=Buff.append(target,RuleRelation.class);link.set(RuleRelation.Type.LINK,context.source==null?context.hero:context.source,spec.stateId,duration);applied=true;break;
			case RELATION_COMMAND_FOLLOW: applied=command(target,context.hero,0,context.cell);break;
			case RELATION_COMMAND_ATTACK: applied=command(target,context.hero,1,context.cell);break;
			case RELATION_COMMAND_GUARD: applied=command(target,context.hero,2,context.cell);break;
			case RELATION_INHERIT: applied=inheritWhitelistedCapability(context.hero,target,spec.stateId,duration);break;
			case RELATION_BREAK: applied=breakRelations(target);break;
			case TRANSFER_RESOURCE: applied=transferResource(runtime,context,target,spec,power);break;
			case COPY_STATUS: applied=copyStatus(context.source==null?context.hero:context.source,target,duration);break;
			case TRANSFER_MARK: applied=transferMark(context.source==null?context.hero:context.source,target,spec,power,duration);break;
			case SWAP_BARRIER: applied=swapBarrier(context.source==null?context.hero:context.source,target);break;
			case TRANSFORM_MODE:
				if(target==null)target=context.hero;Buff.affect(target,RuleMode.class).set(spec.stateId,duration);applied=true;break;
			case TRANSFORM_CAPABILITY:
			case TRANSFORM_BEHAVIOR:
				return false;
			default:return false;
		}
		if(applied){
			RuleTrace.record("SKILL_EFFECT",spec.operation+" target="+(target==null?cell:target.id())+" power="+power);
			if(runtime!=null)runtime.onEffectSpecSuccess(spec,context,target);
		}
		return applied;
	}

	private static int scaledDamagePower(EffectSpec spec,RuleRuntime runtime,RuleContext context,int base){
		if(spec.family!=EffectFamily.DAMAGE)return base;
		if(spec.scalingSource==EffectSpec.ScalingSource.HERO_LEVEL)return base+Math.max(0,context.hero.lvl-1)/3;
		if(spec.scalingSource==EffectSpec.ScalingSource.CURRENT_RESOURCE&&runtime!=null)return base+Math.max(0,runtime.resourceValue(spec.resourceId,null))/4;
		return base;
	}
	private static boolean damage(RuleContext c,Char t,int amount,EffectSpec.DamageType damageType){
		if(t==null||amount<=0)return false;
		int beforeHp=t.HP,beforeShield=t.shielding();
		RuleHooks.beginRuleDamage(c);
		Object source;
		switch(damageType==null?EffectSpec.DamageType.UNTYPED:damageType){
			case FIRE:source=new Burning();break;
			case POISON:source=new Poison();break;
			case BLEEDING:source=new Bleeding();break;
			case UNTYPED:
			default:source=c.source==null?c.hero:c.source;
		}
		try{t.damage(amount,source);}finally{RuleHooks.endRuleDamage();}
		return t.HP<beforeHp||t.shielding()<beforeShield||!t.isAlive();
	}
	private static boolean status(RuleContext c,Char t,Class<? extends Buff> type,int duration,int power){return status(c,t,type,duration,power,EffectSpec.StatusStacking.NATIVE);}
	private static boolean status(RuleContext c,Char t,Class<? extends Buff> type,int duration,int power,EffectSpec.StatusStacking stacking){
		if(t==null||t.isImmune(type))return false;Buff old=t.buff(type);Buff value;
		RuleHooks.beginRuleStatusApplication();try{
			if(stacking==EffectSpec.StatusStacking.REPLACE&&old!=null){old.detach();old=null;}
			if(type==Poison.class){Poison b=Buff.affect(t,Poison.class);if(stacking==EffectSpec.StatusStacking.EXTEND&&old!=null)b.extend(duration);else b.set(duration);value=b;}
			else if(type==Burning.class){Burning b=Buff.affect(t,Burning.class);if(stacking==EffectSpec.StatusStacking.EXTEND&&old!=null)b.extend(duration);else b.reignite(t,duration);value=b;}
			else if(type==Bleeding.class){Bleeding b=Buff.affect(t,Bleeding.class);if(stacking==EffectSpec.StatusStacking.EXTEND&&old!=null)b.extend(power);else b.set(power);value=b;}
			else value=stacking==EffectSpec.StatusStacking.EXTEND&&old!=null?Buff.prolong(t,(Class)type,duration):Buff.affect(t,(Class)type,duration);
		}finally{RuleHooks.endRuleStatusApplication();}
		value=t.buff(type);if(old==null&&value!=null)RuleHooks.onRuleStatusApplied(c,t,value);return value!=null;
	}
	private static boolean convert(RuleRuntime r,Hero h,EffectSpec s,int amount){if(r==null||s.resourceId.equals(s.targetResourceId))return false;int available=r.resourceValue(s.resourceId,null);int spent=Math.min(available,amount);if(spent<=0)return false;r.changeResource(h,s.resourceId,null,-spent,true);return r.changeResource(h,s.targetResourceId,null,spent,true)>0;}
	private static boolean reserve(RuleRuntime r,Hero h,EffectSpec s,int amount,int duration){if(r==null||r.resourceValue(s.resourceId,null)<amount)return false;r.changeResource(h,s.resourceId,null,-amount,true);RuleResourceReserve b=Buff.append(h,RuleResourceReserve.class);b.set(s.resourceId,amount,duration);return true;}
	private static RuleMark.Type markType(String id){try{return RuleMark.Type.valueOf(id==null?"HUNTED":id.toUpperCase());}catch(Exception e){return RuleMark.Type.HUNTED;}}
	private static boolean spreadMark(Char from,Hero owner,EffectSpec s,int stacks,int duration){RuleMark mark=from==null?null:RuleMark.get(from,markType(s.stateId));if(mark==null||Dungeon.level==null)return false;Char best=null;int distance=Integer.MAX_VALUE;for(Char ch:Actor.chars())if(ch!=from&&ch.alignment==Char.Alignment.ENEMY&&ch.isAlive()){int d=Dungeon.level.distance(from.pos,ch.pos);if(d<distance){best=ch;distance=d;}}return best!=null&&RuleMark.transfer(mark,best,stacks,duration,Math.max(1,s.secondaryParameter))!=null;}
	private static boolean spawn(RuleContext c,EffectSpec s,int cell,RuleOwnedEntity.Kind kind){
		if(Dungeon.level==null||cell<0||cell>=Dungeon.level.length())return false;
		RuleRuntime runtime=c.hero.ruleRuntime();
		if(runtime!=null&&runtime.classBuild()!=null&&!runtime.classBuild().hasGameplayComponent(ClassGameplayComponentSpec.Type.OWNERSHIP))return false;
		int made=0;int cap=runtime==null?6:runtime.ownedCapacity(kind);int room=Math.max(0,cap-ownedCount(c.hero,kind));
		int configuredLifetime=s.lifetime;if(runtime!=null&&runtime.persistenceLifetime(kind)>0)configuredLifetime=runtime.persistenceLifetime(kind);
		for(int i=0;i<Math.min(room,Math.min(3,s.count));i++){int pos=findPlacement(cell);if(pos<0)break;RuleOwnedEntity e=new RuleOwnedEntity().configure(kind,c.hero,configuredLifetime,Math.max(3,s.power*3),s.power,s.period,kind==RuleOwnedEntity.Kind.ACTOR?null:carrierPayload(s));e.pos=pos;Dungeon.level.mobs.add(e);if(c.hero.sprite!=null&&c.hero.sprite.parent!=null)GameScene.add(e);else Actor.add(e);made++;}return made>0;
	}
	public static boolean createPersistentCarrier(RuleContext c,EffectSpec payload,EffectSpec secondary,int cell,int lifetime,int period,TargetingSpec.Filter filter){
		if(c==null||c.hero==null||payload==null)return false;
		RuleRuntime runtime=c.hero.ruleRuntime();
		if(runtime!=null&&runtime.classBuild()!=null&&!runtime.classBuild().hasGameplayComponent(ClassGameplayComponentSpec.Type.OWNERSHIP))return false;
		if(ownedCount(c.hero,RuleOwnedEntity.Kind.FIELD)>=(runtime==null?6:runtime.ownedCapacity(RuleOwnedEntity.Kind.FIELD)))return false;
		int pos=findPlacement(cell);if(pos<0)return false;
		int configuredLifetime=runtime==null||runtime.persistenceLifetime(RuleOwnedEntity.Kind.FIELD)<=0?lifetime:runtime.persistenceLifetime(RuleOwnedEntity.Kind.FIELD);
		RuleOwnedEntity e=new RuleOwnedEntity().configure(RuleOwnedEntity.Kind.FIELD,c.hero,configuredLifetime,
				Math.max(3,payload.power*2),1,period,payload,secondary,filter);e.pos=pos;Dungeon.level.mobs.add(e);
		if(c.hero.sprite!=null&&c.hero.sprite.parent!=null)GameScene.add(e);else Actor.add(e);
		RuleTrace.record("CARRIER","field actor=#"+e.id()+" cell="+pos+" lifetime="+lifetime);return true;
	}
	private static int ownedCount(Hero owner,RuleOwnedEntity.Kind kind){int count=0;for(Char ch:Actor.chars())if(RuleOwnership.isOwnedBy(ch,owner)&&(!(ch instanceof RuleOwnedEntity)||((RuleOwnedEntity)ch).kind()==kind))count++;return count;}
	private static int findPlacement(int center){if(center>=0&&center<Dungeon.level.length()&&Dungeon.level.passable[center]&&Actor.findChar(center)==null)return center;for(int cell=0;cell<Dungeon.level.length();cell++)if(Dungeon.level.distance(center,cell)<=2&&Dungeon.level.passable[cell]&&Actor.findChar(cell)==null)return cell;return-1;}
	private static EffectSpec carrierPayload(EffectSpec s){String id=s.stateId==null?"":s.stateId.toLowerCase();int payloadPower=Math.max(1,(s.power+1)/2);if("heal".equals(id))return new EffectSpec(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.RECOVER_HEAL,payloadPower);if("poison".equals(id))return new EffectSpec(EffectFamily.STATUS,EffectSpec.Operation.STATUS_POISON,payloadPower);return new EffectSpec(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,payloadPower);}
	private static boolean placeTrap(RuleContext c,EffectSpec s,int cell){if(!WorldCapabilityValidator.canAlterTerrain(Dungeon.level,cell,WorldCapability.TRAP_PLACEABLE))return false;Dungeon.level.setTrap(new RuleCarrierTrap().configure(c.hero,carrierPayload(s)),cell);Level.set(cell,Terrain.TRAP);return true;}
	private static boolean setTerrain(int cell,WorldCapability capability,int terrain){if(!WorldCapabilityValidator.canAlterTerrain(Dungeon.level,cell,capability))return false;Level.set(cell,terrain);return true;}
	private static boolean clearHazard(int cell){if(!WorldCapabilityValidator.canAlterTerrain(Dungeon.level,cell,WorldCapability.HAZARD_CLEARABLE))return false;boolean changed=false;for(Blob blob:new ArrayList<>(Dungeon.level.blobs.values())){if(blob.volume>0){blob.clear(cell);changed=true;}}if(Dungeon.level.traps.get(cell)!=null){Dungeon.level.disarmTrap(cell);changed=true;}return changed;}
	private static Char nearestOwned(Hero owner,Char exclude){if(Dungeon.level==null)return null;Char best=null;int dist=Integer.MAX_VALUE;for(Char ch:Actor.chars())if(ch!=exclude&&RuleOwnership.isOwnedBy(ch,owner)&&ch.isAlive()){int d=Dungeon.level.distance(owner.pos,ch.pos);if(d<dist){best=ch;dist=d;}}return best;}
	private static boolean command(Char target,Hero owner,int type,int cell){if(!(target instanceof DirectableAlly)||!RuleOwnership.isOwnedBy(target,owner))return false;DirectableAlly ally=(DirectableAlly)target;if(type==0)ally.followHero();else if(type==1){Char enemy=Actor.findChar(cell);if(enemy==null||enemy.alignment!=Char.Alignment.ENEMY)return false;ally.targetChar(enemy);}else{if(Dungeon.level==null||cell<0||cell>=Dungeon.level.length()||Dungeon.level.solid[cell])return false;ally.defendPos(cell);}RuleTrace.record("COMMAND",(type==0?"follow":type==1?"attack":"guard")+" actor=#"+target.id()+" cell="+cell);return true;}
	/**
	 * Inheritance is deliberately whitelist based.  It copies one real, bounded gameplay state
	 * from the owner and records the saved relation only after that copy succeeds; it never clones
	 * the owner's RuleRuntime or arbitrary Buff graph.
	 */
	private static boolean inheritWhitelistedCapability(Hero owner,Char target,String capability,int duration){
		if(owner==null||target==null||!RuleOwnership.isOwnedBy(target,owner))return false;
		String id=capability==null?"mode":capability.toLowerCase();
		boolean copied=false;
		if("mode".equals(id)){
			RuleMode source=owner.buff(RuleMode.class);
			if(source!=null){int inheritedDuration=source.persistent()?duration:Math.min(duration,Math.max(1,source.remainingTurns()));Buff.affect(target,RuleMode.class).set(source.modeId(),inheritedDuration);copied=true;}
		}else if("haste".equals(id)){
			if(owner.buff(Haste.class)!=null){Buff.prolong(target,Haste.class,duration);copied=true;}
		}else if("mitigation".equals(id)){
			RuleMitigation source=owner.buff(RuleMitigation.class);
			if(source!=null){Buff.affect(target,RuleMitigation.class).set(source.percent(),Math.min(duration,Math.max(1,source.remainingTurns())));copied=true;}
		}
		if(!copied)return false;
		RuleRelation relation=Buff.append(target,RuleRelation.class);
		relation.set(RuleRelation.Type.INHERIT_CAPABILITY,owner,id,duration);
		return true;
	}
	private static boolean breakRelations(Char target){if(target==null)return false;boolean changed=false;for(RuleRelation b:new ArrayList<>(target.buffs(RuleRelation.class))){b.detach();changed=true;}RuleDamageRedirect redirect=target.buff(RuleDamageRedirect.class);if(redirect!=null){redirect.detach();changed=true;}return changed;}
	private static boolean transferResource(RuleRuntime runtime,RuleContext c,Char target,EffectSpec s,int amount){if(runtime==null||!(target instanceof Hero)||target==c.hero)return false;RuleRuntime other=((Hero)target).ruleRuntime();if(other==null)return false;int spent=Math.min(amount,runtime.resourceValue(s.resourceId,null));if(spent<=0)return false;runtime.changeResource(c.hero,s.resourceId,null,-spent,true);return other.changeResource((Hero)target,s.targetResourceId,null,spent,true)>0;}
	private static boolean copyStatus(Char source,Char target,int duration){if(source==null||target==null||source==target)return false;for(Buff b:source.buffs()){if(b instanceof Poison)return status(new RuleContext(RuleEvent.ACTIVE,Dungeon.hero),target,Poison.class,duration,1);if(b instanceof Burning)return status(new RuleContext(RuleEvent.ACTIVE,Dungeon.hero),target,Burning.class,duration,1);if(b instanceof Slow)return status(new RuleContext(RuleEvent.ACTIVE,Dungeon.hero),target,Slow.class,duration,1);if(b instanceof Haste)return status(new RuleContext(RuleEvent.ACTIVE,Dungeon.hero),target,Haste.class,duration,1);}return false;}
	private static boolean transferMark(Char source,Char target,EffectSpec s,int stacks,int duration){RuleMark mark=source==null?null:RuleMark.get(source,markType(s.stateId));if(mark==null||target==null)return false;int count=Math.min(stacks,mark.stacks());if(RuleMark.transfer(mark,target,count,duration,Math.max(1,s.secondaryParameter))==null)return false;RuleMark.consume(source,mark.mark(),count);return true;}
	private static boolean swapBarrier(Char a,Char b){if(a==null||b==null||a==b)return false;Barrier aa=a.buff(Barrier.class),bb=b.buff(Barrier.class);int av=aa==null?0:aa.shielding(),bv=bb==null?0:bb.shielding();if(aa!=null)aa.detach();if(bb!=null)bb.detach();if(bv>0)Buff.affect(a,Barrier.class).setShield(bv);if(av>0)Buff.affect(b,Barrier.class).setShield(av);return av!=bv;}
}
