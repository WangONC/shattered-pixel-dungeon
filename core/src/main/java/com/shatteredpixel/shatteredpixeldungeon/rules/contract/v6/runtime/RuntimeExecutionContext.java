package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleTemporaryHP;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ResourceRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ResourceState;

/** Transient execution-only Actor resolution and narrow host mutation gateways. */
public final class RuntimeExecutionContext {
	public interface DamageGateway { DamageOutcome apply(CompiledSkill.DirectDamageEffect effect,Char source,Char target,GameplayEventContext event); }
	public interface HostGateway {int classOwnerLevel();boolean hasItemCost(CompiledSkill.ItemCategory category,int count);boolean commitItemCost(CompiledSkill.ItemCategory category,int count);void spendActionTime(int turns);}
	private static final HostGateway NO_HOST=new HostGateway(){public int classOwnerLevel(){return 0;}public boolean hasItemCost(CompiledSkill.ItemCategory category,int count){return false;}public boolean commitItemCost(CompiledSkill.ItemCategory category,int count){return false;}public void spendActionTime(int turns){}};
	public static final class DamageOutcome {private final int hpBefore,hpAfter;public DamageOutcome(int before,int after){hpBefore=before;hpAfter=after;}public int hpBefore(){return hpBefore;}public int hpAfter(){return hpAfter;}public int appliedAmount(){return Math.max(0,hpBefore-hpAfter);}}
	private final GameplayEventContext event;private final transient DamageGateway damageGateway;private final transient HostGateway hostGateway;
	private final transient Char classOwner,sourceActor,selectedActor;private transient V6RuleRuntime runtime;private transient CompiledSkill activeSkill;
	public RuntimeExecutionContext(GameplayEventContext event,DamageGateway damageGateway){this(event,damageGateway,NO_HOST);}
	public RuntimeExecutionContext(GameplayEventContext event,DamageGateway damageGateway,HostGateway hostGateway){if(event==null||damageGateway==null||hostGateway==null)throw new IllegalArgumentException("runtime event and gateways required");this.event=event;this.damageGateway=damageGateway;this.hostGateway=hostGateway;classOwner=resolve(event.classOwnerActorId());sourceActor=resolve(event.sourceActorId());selectedActor=resolve(event.selectedActorId());}
	private static Char resolve(int actorId){Actor actor=actorId<=0?null:Actor.findById(actorId);return actor instanceof Char?(Char)actor:null;}
	public GameplayEventContext event(){return event;}public Char classOwner(){return classOwner;}public Char sourceActor(){return sourceActor;}public Char selectedActor(){return selectedActor;}
	public DamageOutcome applyDamage(CompiledSkill.DirectDamageEffect effect,Char target){return damageGateway.apply(effect,sourceActor,target,event);}public DamageOutcome applyNativeDamage(int amount,Char target){int before=target.HP;target.damage(Math.max(0,amount),sourceActor);return new DamageOutcome(before,target.HP);}
	void bind(V6RuleRuntime runtime,CompiledSkill skill){if(runtime==null||skill==null)throw new IllegalArgumentException("runtime and skill are required");this.runtime=runtime;activeSkill=skill;}void bind(V6RuleRuntime runtime){if(runtime==null)throw new IllegalArgumentException("runtime required");this.runtime=runtime;activeSkill=null;}
	public ClassCompilePlan plan(){return runtime==null?null:runtime.plan();}public ClassRuntimeState state(){return runtime==null?null:runtime.state();}public void publishState(ClassRuntimeState value){if(runtime==null)throw new IllegalStateException("runtime context is not bound");runtime.publishState(value);}
	public int resolve(CompiledSkill.Value value){int result=resolveRaw(value);if(activeSkill!=null&&activeSkill.modifier().variant()==CompiledSkill.ModifierVariant.INTENSITY)result=(int)Math.max(0,Math.min(Integer.MAX_VALUE,(long)result*activeSkill.modifier().first()/activeSkill.modifier().second()));return result;}
	public int resolveRaw(CompiledSkill.Value value){if(value==null)throw new IllegalArgumentException("compiled value required");if(value.variant()==CompiledSkill.ValueVariant.FIXED)return value.fixed();int source=sourceValue(value.source());long result=(long)value.base()+(long)source*value.numerator()/value.denominator();return (int)Math.max(value.minimum(),Math.min(value.maximum(),result));}
	public int duration(CompiledSkill.Duration duration){if(duration==null||duration.kind()!=CompiledSkill.DurationKind.TURN_BASED)return duration==null?0:duration.turns();int turns=duration.turns();if(activeSkill!=null&&activeSkill.modifier().variant()==CompiledSkill.ModifierVariant.EXTEND_DURATION)turns+=activeSkill.modifier().first();return turns;}
	private int sourceValue(CompiledSkill.ValueSource source){if(source.variant()==CompiledSkill.ValueSourceVariant.RESOURCE){ClassRuntimeState value=state();if(value==null)return 0;ResourceState resource=value.resources().get(new ResourceRef(source.resourceId(),""));return resource==null?0:resource.current();}Char subject=subject(source.subject());if(subject==null)return 0;switch(source.stat()){case HP_CURRENT:return subject.HP;case HP_MAX:return subject.HT;case HP_MISSING:return Math.max(0,subject.HT-subject.HP);case HP_PERCENT:return subject.HT<=0?0:subject.HP*100/subject.HT;case BARRIER:{Barrier b=subject.buff(Barrier.class);return b==null?0:b.shielding();}case TEMPORARY_HP:{RuleTemporaryHP b=subject.buff(RuleTemporaryHP.class);return b==null?0:b.shielding();}case HERO_LEVEL:return subject==classOwner?hostGateway.classOwnerLevel():0;case DAMAGE_TAKEN_CURRENT_EVENT:return 0;case DISTANCE_CURRENT_EVENT:return classOwner==null||selectedActor==null||Dungeon.level==null?0:Dungeon.level.distance(classOwner.pos,selectedActor.pos);default:return 0;}}
	public Char subject(CompiledSkill.Subject subject){if(subject==null)return null;switch(subject){case CLASS_OWNER:return classOwner;case EVENT_SOURCE:return sourceActor;case EVENT_TARGET:case SELECTED_ACTOR:return selectedActor;default:return null;}}
	public boolean hasItemCost(CompiledSkill.ItemCategory category,int count){return hostGateway.hasItemCost(category,count);}public boolean commitItemCost(CompiledSkill.ItemCategory category,int count){return hostGateway.commitItemCost(category,count);}public void spendActionTime(int turns){hostGateway.spendActionTime(turns);}
}
