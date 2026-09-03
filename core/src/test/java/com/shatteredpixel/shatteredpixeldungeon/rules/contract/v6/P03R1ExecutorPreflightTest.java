package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.CompiledSkill;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.SkillCompiler;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.*;
import com.watabou.utils.SparseArray;
import org.junit.*;

import java.util.*;

import static org.junit.Assert.*;

public class P03R1ExecutorPreflightTest {
	private TestLevel level;private Hero hero;
	@Before public void setup(){Actor.clear();Actor.resetNextID();level=new TestLevel();Dungeon.level=level;Dungeon.depth=1;hero=new Hero();hero.HT=hero.HP=100;hero.pos=6;hero.damageInterrupt=false;Dungeon.hero=hero;level.occupyCell(hero);Actor.init();}
	@After public void teardown(){Actor.clear();Dungeon.hero=null;Dungeon.level=null;}

	@Test public void preflightDistinguishesFailuresWithoutMutation(){
		ClassCompilePlan plan=new SkillCompiler(EffectExecutorRegistry.standard()).compile(P03TestBuilds.directDamage("p03-r1-preflight",7,false,false).finalizeOrThrow());CompiledSkill skill=plan.skills().get(0);CompiledSkill.Effect effect=skill.effectChain().primary();EffectExecutorRegistry registry=EffectExecutorRegistry.standard();int[] gatewayCalls={0};
		Rat ready=rat(new Rat(),7,20);RuntimeExecutionContext readyContext=context(skill,ready,gatewayCalls);int heroHp=hero.HP,targetHp=ready.HP,heroPos=hero.pos,targetPos=ready.pos;
		assertEquals(EffectPreflightResult.Status.READY,registry.preflight(effect,ready,readyContext).status());assertEquals(heroHp,hero.HP);assertEquals(targetHp,ready.HP);assertEquals(heroPos,hero.pos);assertEquals(targetPos,ready.pos);assertEquals(0,gatewayCalls[0]);assertSame(hero,Actor.findById(hero.id()));assertSame(ready,Actor.findById(ready.id()));
		Rat dead=rat(new Rat(),8,0);assertEquals(EffectPreflightResult.Status.TARGET_UNAVAILABLE,registry.preflight(effect,dead,context(skill,dead,gatewayCalls)).status());
		ImmuneRat immune=(ImmuneRat)rat(new ImmuneRat(),11,20);assertEquals(EffectPreflightResult.Status.IMMUNE,registry.preflight(effect,immune,context(skill,immune,gatewayCalls)).status());
		CompiledSkill.DirectDamageEffect unsupported=new CompiledSkill.DirectDamageEffect(effect.effectId(),0,CompiledSkill.DamageType.UNTYPED,CompiledSkill.DefensePolicy.SPD_NATIVE);assertEquals(EffectPreflightResult.Status.UNSUPPORTED_PARAMETERS,registry.preflight(unsupported,ready,readyContext).status());
		CompiledSkill.Effect wrongType=new WrongDirectDamageEffect(effect.effectId());assertEquals(EffectPreflightResult.Status.TYPE_MISMATCH,registry.preflight(wrongType,ready,readyContext).status());
		assertEquals(EffectPreflightResult.Status.MISSING_EXECUTOR,EffectExecutorRegistry.empty().preflight(effect,ready,readyContext).status());
		SkillExecutionResult blocked=new V6RuleRuntime(plan,registry).execute(skill.skillId(),context(skill,immune,gatewayCalls));assertEquals(SkillExecutionResult.Status.IMMUNE,blocked.status());assertEquals(20,immune.HP);assertEquals(100,hero.HP);assertEquals(0,gatewayCalls[0]);assertFalse(blocked.trace().serialize(),blocked.trace().serialize().contains("cost|"));
	}
	private RuntimeExecutionContext context(CompiledSkill skill,Rat selected,int[] calls){GameplayEventContext event=new GameplayEventContext(501,0,GameplayEventContext.RuleEventType.ACTIVE,hero.id(),hero.id(),selected.id(),hero.pos,hero.pos,selected.pos,skill.skillId());return new RuntimeExecutionContext(event,(effect,source,target,provenance)->{calls[0]++;int before=target.HP;target.damage(effect.amount(),source);return new RuntimeExecutionContext.DamageOutcome(before,target.HP);});}
	private <T extends Rat>T rat(T target,int cell,int hp){target.pos=cell;target.HT=Math.max(1,hp);target.HP=hp;target.EXP=0;target.maxLvl=-1;level.add(target);Actor.add(target);return target;}
	private static final class ImmuneRat extends Rat {@Override public boolean isInvulnerable(Class effect){return effect==Hero.class||super.isInvulnerable(effect);}}
	private static final class WrongDirectDamageEffect implements CompiledSkill.Effect {private final com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId id;private WrongDirectDamageEffect(com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId id){this.id=id;}@Override public com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId effectId(){return id;}@Override public CompiledSkill.EffectVariant variant(){return CompiledSkill.EffectVariant.DIRECT_DAMAGE;}}
	private static final class TestLevel extends Level {private TestLevel(){mobs=new HashSet<>();heaps=new SparseArray<>();blobs=new HashMap<>();plants=new SparseArray<>();traps=new SparseArray<>();customTiles=new ArrayList<>();customWalls=new ArrayList<>();transitions=new ArrayList<>();setSize(5,5);Arrays.fill(map,Terrain.EMPTY);Arrays.fill(passable,true);Arrays.fill(openSpace,true);Arrays.fill(solid,false);Arrays.fill(losBlocking,false);cleanWalls();}private void add(Rat value){mobs.add(value);}@Override protected boolean build(){return true;}@Override protected void createMobs(){}@Override protected void createItems(){}}
}
