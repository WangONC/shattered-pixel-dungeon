package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;

/** Transient execution-only Actor resolution and native damage boundary. */
public final class RuntimeExecutionContext {
	public interface DamageGateway {
		DamageOutcome apply(CompiledSkill.DirectDamageEffect effect,Char source,Char target,GameplayEventContext event);
	}
	public static final class DamageOutcome {
		private final int hpBefore;private final int hpAfter;
		public DamageOutcome(int hpBefore,int hpAfter){this.hpBefore=hpBefore;this.hpAfter=hpAfter;}
		public int hpBefore(){return hpBefore;}public int hpAfter(){return hpAfter;}public int appliedAmount(){return Math.max(0,hpBefore-hpAfter);}
	}
	private final GameplayEventContext event;private final transient DamageGateway damageGateway;
	private final transient Char classOwner;private final transient Char sourceActor;private final transient Char selectedActor;
	public RuntimeExecutionContext(GameplayEventContext event,DamageGateway damageGateway){
		if(event==null||damageGateway==null)throw new IllegalArgumentException("runtime event and damage gateway required");
		this.event=event;this.damageGateway=damageGateway;this.classOwner=resolve(event.classOwnerActorId());this.sourceActor=resolve(event.sourceActorId());this.selectedActor=resolve(event.selectedActorId());
	}
	private static Char resolve(int actorId){Actor actor=actorId<=0?null:Actor.findById(actorId);return actor instanceof Char?(Char)actor:null;}
	public GameplayEventContext event(){return event;}public Char classOwner(){return classOwner;}public Char sourceActor(){return sourceActor;}public Char selectedActor(){return selectedActor;}
	public DamageOutcome applyDamage(CompiledSkill.DirectDamageEffect effect,Char target){return damageGateway.apply(effect,sourceActor,target,event);}
}
