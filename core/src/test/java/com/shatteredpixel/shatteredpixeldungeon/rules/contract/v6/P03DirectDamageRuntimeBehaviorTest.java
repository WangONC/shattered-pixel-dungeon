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
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.ClassCompilePlan;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.compile.SkillCompiler;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.*;
import com.watabou.noosa.Game;
import com.watabou.utils.SparseArray;
import org.junit.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.*;

/** Layer D/F: the compiled v6 skill mutates a real SPD Rat through Char.damage. */
public class P03DirectDamageRuntimeBehaviorTest {
	private static HeadlessApplication app;
	private TestLevel level;
	private Hero hero;

	@BeforeClass public static void startHeadless(){Game.version="3.3.8-INDEV-v6-p03";Game.versionCode=903;app=new HeadlessApplication(new ApplicationAdapter(){},new HeadlessApplicationConfiguration());}
	@AfterClass public static void stopHeadless(){if(app!=null)app.exit();}
	@Before public void setup(){Actor.clear();Actor.resetNextID();level=new TestLevel();Dungeon.level=level;Dungeon.depth=1;hero=new Hero();hero.HT=hero.HP=100;hero.pos=6;hero.damageInterrupt=false;Dungeon.hero=hero;level.occupyCell(hero);Actor.init();}
	@After public void teardown(){Actor.clear();Dungeon.hero=null;Dungeon.level=null;}

	@Test public void compiledPrimaryAndImmediateSecondaryDealRealDamageAndTraceEveryStage() throws Exception {
		PlayerBuildSession session=P03TestBuilds.directDamage("p03-runtime",7,false,true);ClassCompilePlan plan=new SkillCompiler(EffectExecutorRegistry.standard()).compile(session.finalizeOrThrow());assertTrue(plan.ready());
		Rat target=rat(7,30);SkillExecutionResult result=new V6RuleRuntime(plan,EffectExecutorRegistry.standard()).execute(session.state().draft().skills().get(0).id(),GameplayEventContext.active(101,hero,target));
		assertEquals(SkillExecutionResult.Status.APPLIED,result.status());assertEquals(20,target.HP);assertEquals(10,result.appliedAmount());String trace=result.trace().serialize();
		assertTrue(trace,trace.contains("preflight|status=READY"));assertTrue(trace,trace.contains("variant=DIRECT_DAMAGE"));assertTrue(trace,trace.contains("primary=APPLIED"));assertTrue(trace,trace.contains("secondary=APPLIED"));assertTrue(trace,trace.contains("result|status=APPLIED"));writeArtifact("P03_RUNTIME_TRACE.txt",trace);
	}

	@Test public void noTargetBlockedMissingExecutorAndUnsupportedAreDifferentStatuses() {
		PlayerBuildSession session=P03TestBuilds.directDamage("p03-failure-semantics",7,false,false);ClassCompilePlan plan=new SkillCompiler(EffectExecutorRegistry.standard()).compile(session.finalizeOrThrow());
		V6RuleRuntime runtime=new V6RuleRuntime(plan,EffectExecutorRegistry.standard());
		assertEquals(SkillExecutionResult.Status.NO_TARGET,runtime.execute(session.state().draft().skills().get(0).id(),GameplayEventContext.active(201,hero,null)).status());
		Rat dead=rat(7,10);dead.HP=0;assertEquals(SkillExecutionResult.Status.BLOCKED,runtime.execute(session.state().draft().skills().get(0).id(),GameplayEventContext.active(202,hero,dead)).status());
		Rat target=rat(8,10);assertEquals(SkillExecutionResult.Status.MISSING_EXECUTOR,new V6RuleRuntime(plan,EffectExecutorRegistry.empty()).execute(session.state().draft().skills().get(0).id(),GameplayEventContext.active(203,hero,target)).status());
		assertEquals(SkillExecutionResult.Status.UNSUPPORTED,runtime.execute(new DeterministicIdGenerator("missing-skill").nextId("skill"),GameplayEventContext.active(204,hero,target)).status());
	}

	@Test public void nativeLineOfSightBlockReturnsBlockedWithoutDamage() {
		PlayerBuildSession session=P03TestBuilds.directDamage("p03-los",7,true,false);ClassCompilePlan plan=new SkillCompiler(EffectExecutorRegistry.standard()).compile(session.finalizeOrThrow());Rat target=rat(8,20);level.wall(7);SkillExecutionResult result=new V6RuleRuntime(plan,EffectExecutorRegistry.standard()).execute(session.state().draft().skills().get(0).id(),GameplayEventContext.active(301,hero,target));assertEquals(SkillExecutionResult.Status.BLOCKED,result.status());assertEquals(20,target.HP);assertTrue(result.trace().serialize().contains("line_of_sight"));
	}

	private Rat rat(int cell,int hp){Rat target=new Rat();target.pos=cell;target.HT=target.HP=hp;target.EXP=0;target.maxLvl=-1;level.add(target);Actor.add(target);return target;}
	private static void writeArtifact(String name,String value)throws Exception{String root=System.getenv("P03_ARTIFACT_DIR");if(root==null||root.isEmpty())return;Path dir=Paths.get(root);Files.createDirectories(dir);Files.write(dir.resolve(name),value.getBytes(StandardCharsets.UTF_8));}
	private static final class TestLevel extends Level {
		private TestLevel(){mobs=new HashSet<>();heaps=new SparseArray<>();blobs=new HashMap<>();plants=new SparseArray<>();traps=new SparseArray<>();customTiles=new ArrayList<>();customWalls=new ArrayList<>();transitions=new ArrayList<>();setSize(5,5);Arrays.fill(map,Terrain.EMPTY);Arrays.fill(passable,true);Arrays.fill(openSpace,true);Arrays.fill(solid,false);Arrays.fill(losBlocking,false);cleanWalls();}
		private void add(Char value){mobs.add((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob)value);}
		private void wall(int cell){map[cell]=Terrain.WALL;solid[cell]=true;losBlocking[cell]=true;passable[cell]=false;openSpace[cell]=false;cleanWalls();}
		@Override protected boolean build(){return true;}@Override protected void createMobs(){}@Override protected void createItems(){}
	}
}
