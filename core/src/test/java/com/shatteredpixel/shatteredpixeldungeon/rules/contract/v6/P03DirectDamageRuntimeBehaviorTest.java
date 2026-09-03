package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.rules.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.utils.SparseArray;
import org.junit.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;

/** Real production entry evidence: RuleHooks -> installed bridge -> compiled runtime -> typed executor. */
public class P03DirectDamageRuntimeBehaviorTest {
	private static HeadlessApplication app;private TestLevel level;private Hero hero;
	@BeforeClass public static void startHeadless(){Game.version="3.3.8-INDEV-v6-p03-r1";Game.versionCode=9031;app=new HeadlessApplication(new ApplicationAdapter(){},new HeadlessApplicationConfiguration());}
	@AfterClass public static void stopHeadless(){if(app!=null)app.exit();}
	@Before public void setup(){Actor.clear();Actor.resetNextID();level=new TestLevel();Dungeon.level=level;Dungeon.depth=1;hero=new Hero();hero.HT=hero.HP=100;hero.pos=6;hero.damageInterrupt=false;Dungeon.hero=hero;level.occupyCell(hero);Actor.init();}
	@After public void teardown(){RuleTrace.clear();Actor.clear();Dungeon.hero=null;Dungeon.level=null;}

	@Test public void compiledPrimaryAndImmediateSecondaryDealRealDamageAndTraceEveryStage() throws Exception {
		PlayerBuildSession session=P03TestBuilds.directDamage("p03-runtime",7,false,true);String skillId=session.state().draft().skills().get(0).id().value();
		V6RuleRuntimeBridge bridge=V6RuleRuntimeBridge.install(hero,session.finalizeOrThrow());Rat target=rat(7,30);
		assertTrue(RuleHooks.triggerActive(hero,target.pos,skillId));SkillExecutionResult result=bridge.lastExecution();
		assertEquals(SkillExecutionResult.Status.APPLIED,result.status());assertEquals(20,target.HP);assertEquals(10,result.appliedAmount());String trace=result.trace().serialize();
		assertTrue(trace,trace.contains("target_preflight|status=READY"));assertTrue(trace,trace.contains("effect_preflight|slot=primary"));assertTrue(trace,trace.contains("after_effect_preflight=true"));assertTrue(trace,trace.contains("primary=APPLIED"));assertTrue(trace,trace.contains("secondary=APPLIED"));assertTrue(trace,trace.contains("result|status=APPLIED"));
	}

	@Test public void installedBuildExecutesThroughRuleHooksAndPreservesKillCausality() throws Exception {
		PlayerBuildSession session=P03TestBuilds.directDamage("p03-r1-kill",7,false,false);String skillId=session.state().draft().skills().get(0).id().value();
		session.dispatch(new BuilderCommand.SaveDraft("kill-ready"));session.dispatch(new BuilderCommand.LoadDraft("kill-ready"));
		RuleRuntime legacy=new RuleRuntime();RuleDefinition killObserver=new RuleDefinition();killObserver.id="kill-observer";killObserver.trigger.event=RuleEvent.ON_KILL;killObserver.effect.type=RuleEffect.Type.SHIELD;killObserver.effect.power=1;assertTrue(legacy.addRule(killObserver,true));hero.setRuleRuntime(legacy);
		V6RuleRuntimeBridge bridge=V6RuleRuntimeBridge.install(hero,session.finalizeOrThrow());TrackingRat target=trackingRat(7,5);List<String> ruleTrace=new ArrayList<>();RuleTrace.install((category,message)->ruleTrace.add(category+"|"+message));
		assertTrue(RuleHooks.triggerActive(hero,target.pos,skillId));SkillExecutionResult result=bridge.lastExecution();String trace=result.trace().serialize();
		assertFalse(target.isAlive());assertSame("Mob.die attribution must be the installed Hero",hero,target.deathCause);assertEquals(1,killObserver.triggerCount());
		assertEquals(1,ruleTrace.stream().filter(value->value.startsWith("EVENT|")&&value.contains(" ON_KILL ")).count());
		assertTrue(ruleTrace.toString(),ruleTrace.stream().anyMatch(value->value.contains("parent=#1")&&value.contains("sourceRule="+skillId)));
		assertTrue(trace,trace.contains("event_id=1 cause_event_id=0"));assertTrue(trace,trace.contains("originating_skill="+skillId));assertTrue(trace,trace.contains("source_actor="+hero.id()));assertTrue(trace,trace.contains("result|status=APPLIED"));
		writeArtifact("P03_R1_RUNTIME_TRACE.txt",trace+"\nRULE_TRACE\n"+String.join("\n",ruleTrace));
	}

	@Test public void noTargetAndLineOfSightFailuresDoNotDamage() {
		PlayerBuildSession missing=P03TestBuilds.directDamage("p03-no-target",7,false,false);String missingId=missing.state().draft().skills().get(0).id().value();V6RuleRuntimeBridge missingBridge=V6RuleRuntimeBridge.install(hero,missing.finalizeOrThrow());
		assertFalse(RuleHooks.triggerActive(hero,8,missingId));assertEquals(SkillExecutionResult.Status.NO_TARGET,missingBridge.lastExecution().status());
		PlayerBuildSession los=P03TestBuilds.directDamage("p03-los",7,true,false);String losId=los.state().draft().skills().get(0).id().value();V6RuleRuntimeBridge losBridge=V6RuleRuntimeBridge.install(hero,los.finalizeOrThrow());Rat target=rat(8,20);level.wall(7);
		assertFalse(RuleHooks.triggerActive(hero,target.pos,losId));assertEquals(SkillExecutionResult.Status.BLOCKED,losBridge.lastExecution().status());assertEquals(20,target.HP);assertTrue(losBridge.lastExecution().trace().serialize().contains("line_of_sight"));
	}

	private Rat rat(int cell,int hp){Rat target=new Rat();prepare(target,cell,hp);return target;}
	private TrackingRat trackingRat(int cell,int hp){TrackingRat target=new TrackingRat();prepare(target,cell,hp);return target;}
	private void prepare(Rat target,int cell,int hp){target.pos=cell;target.HT=target.HP=hp;target.EXP=0;target.maxLvl=-1;target.sprite=new CharSprite(){@Override public void die(){}};level.add(target);Actor.add(target);}
	private static void writeArtifact(String name,String value)throws Exception{String root=System.getenv("P03_ARTIFACT_DIR");if(root==null||root.isEmpty())return;Path dir=Paths.get(root);Files.createDirectories(dir);Files.write(dir.resolve(name),value.getBytes(StandardCharsets.UTF_8));}
	private static final class TrackingRat extends Rat {private Object deathCause;@Override public void die(Object cause){deathCause=cause;super.die(cause);}}
	private static final class TestLevel extends Level {
		private TestLevel(){mobs=new HashSet<>();heaps=new SparseArray<>();blobs=new HashMap<>();plants=new SparseArray<>();traps=new SparseArray<>();customTiles=new ArrayList<>();customWalls=new ArrayList<>();transitions=new ArrayList<>();setSize(5,5);Arrays.fill(map,Terrain.EMPTY);Arrays.fill(passable,true);Arrays.fill(openSpace,true);Arrays.fill(solid,false);Arrays.fill(losBlocking,false);cleanWalls();}
		private void add(Rat value){mobs.add(value);}private void wall(int cell){map[cell]=Terrain.WALL;solid[cell]=true;losBlocking[cell]=true;passable[cell]=false;openSpace[cell]=false;cleanWalls();}
		@Override protected boolean build(){return true;}@Override protected void createMobs(){}@Override protected void createItems(){}
	}
}
