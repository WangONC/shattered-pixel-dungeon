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
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.runtime.RuntimeExecutionContext;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.utils.SparseArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

abstract class P04RuntimeTestBase {
	private static HeadlessApplication app;
	protected TestLevel level;protected Hero hero;
	@BeforeClass public static void startP04Headless(){if(app==null){Game.version="3.3.8-INDEV-v6-p04";Game.versionCode=9040;app=new HeadlessApplication(new ApplicationAdapter(){},new HeadlessApplicationConfiguration());}}
	@AfterClass public static void stopP04Headless(){if(app!=null){app.exit();app=null;}}
	@Before public void setupP04Runtime(){Actor.clear();Actor.resetNextID();level=new TestLevel();Dungeon.level=level;Dungeon.depth=1;hero=new Hero();hero.HT=hero.HP=100;hero.pos=12;hero.damageInterrupt=false;Dungeon.hero=hero;level.occupyCell(hero);Actor.init();}
	@After public void teardownP04Runtime(){Actor.clear();Dungeon.hero=null;Dungeon.level=null;}
	protected Rat actor(int cell,int hp,Char.Alignment alignment){Rat target=new Rat();target.pos=cell;target.HT=Math.max(1,hp);target.HP=hp;target.alignment=alignment;target.EXP=0;target.maxLvl=-1;target.sprite=new CharSprite(){@Override public void die(){}};level.add(target);Actor.add(target);return target;}
	protected RuntimeExecutionContext.DamageGateway damageGateway(){return (effect,source,target,provenance)->{int before=target.HP;target.damage(effect.amount(),source);return new RuntimeExecutionContext.DamageOutcome(before,target.HP);};}
	protected static final class TestLevel extends Level {
		private TestLevel(){mobs=new HashSet<>();heaps=new SparseArray<>();blobs=new HashMap<>();plants=new SparseArray<>();traps=new SparseArray<>();customTiles=new ArrayList<>();customWalls=new ArrayList<>();transitions=new ArrayList<>();setSize(7,7);Arrays.fill(map,Terrain.EMPTY);Arrays.fill(passable,true);Arrays.fill(openSpace,true);Arrays.fill(solid,false);Arrays.fill(losBlocking,false);cleanWalls();}
		void add(Rat value){mobs.add(value);}void remove(Rat value){mobs.remove(value);}void wall(int cell){map[cell]=Terrain.WALL;solid[cell]=true;losBlocking[cell]=true;passable[cell]=false;openSpace[cell]=false;cleanWalls();}void clearWall(int cell){map[cell]=Terrain.EMPTY;solid[cell]=false;losBlocking[cell]=false;passable[cell]=true;openSpace[cell]=true;cleanWalls();}
		@Override protected boolean build(){return true;}@Override protected void createMobs(){}@Override protected void createItems(){}
	}
}
