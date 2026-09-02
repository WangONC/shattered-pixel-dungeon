package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMode;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GrippingTrap;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleHooks;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrace;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Callback;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runs one archetype through one standardized combat scenario using a shared semantic policy. */
public final class ArchetypeStressHarness implements AutoCloseable, QaCombatMetrics.Listener {
	public enum ScenarioId {
		SINGLE_NORMAL, HIGH_HP_SINGLE, ENEMY_SWARM, RANGED_PRESSURE, NARROW_CORRIDOR,
		OPEN_ROOM, RESOURCE_STARVATION, SUSTAINED_ENCOUNTER, LOW_HP_START,
		TERRAIN_POOR, TERRAIN_RICH, MIXED_THREAT
	}

	private static final int WIDTH=11, HEIGHT=11;
	private final ArchetypeStressReport.BehaviorMetrics metrics=new ArchetypeStressReport.BehaviorMetrics();
	private final QaTraceCollector trace=new QaTraceCollector();
	private final HashSet<Integer> controlledEnemies=new HashSet<>();
	private final HashSet<Integer> alteredCells=new HashSet<>();
	private final HashMap<String,Integer> lastSkillUseTurn=new HashMap<>();
	private final HashMap<String,Integer> lastOperationUseTurn=new HashMap<>();
	private ClassBuild build;
	private QaFixedLevel level;
	private Hero hero;
	private RuleRuntime runtime;
	private ScenarioId scenario;
	private int turn,wavesRemaining;
	private String previousMode="",modeBeforePrevious="";
	private int lastModeSwitchTurn=-100;
	private int executeOpportunityTarget=-1;
	private boolean executeUsedThisTurn;

	public ArchetypeStressReport.RunResult run(ArchetypeReferenceBuilds.Id buildId,ScenarioId scenarioId,long seed) {
		return run(ArchetypeReferenceBuilds.build(buildId), buildId.name(), scenarioId, seed);
	}

	/** Executes a build reconstructed through the player registries, without replacing the shared policy. */
	public ArchetypeStressReport.RunResult run(ClassBuild playerBuild, String buildId, ScenarioId scenarioId, long seed) {
		ArchetypeStressReport.RunResult result=new ArchetypeStressReport.RunResult();
		result.buildId=buildId;result.scenarioId=scenarioId.name();result.seed=seed;
		try {
			setup(playerBuild,scenarioId,seed);
			result.usedBudget=build.usedBudget();result.maxBudget=build.maxBudget();
			result.integrityClassification=new RuleBuildAnalyzer().analyze(RuleBuild.from(build)).classification.name();
			while(hero.isAlive()&&turn<maxTurns()&&(hasEnemies()||wavesRemaining>0)){
				if(!hasEnemies()&&wavesRemaining>0)spawnNextWave();
				takeTurn();
			}
			result.victory=!hasEnemies()&&wavesRemaining==0;
			result.survived=hero.isAlive();
			result.turns=turn;result.turnsToVictory=result.victory?turn:-1;
			metrics.turnsSurvived=turn;
			metrics.enemiesControlled=controlledEnemies.size();
			metrics.terrainCellsAltered=alteredCells.size();
			parseTraceMetrics();
			result.metrics=metrics;
		}catch(Throwable error){
			StringWriter stack=new StringWriter();error.printStackTrace(new PrintWriter(stack));
			result.runtimeFailure=true;result.failure=stack.toString();
			result.survived=hero!=null&&hero.isAlive();result.turns=turn;result.metrics=metrics;
		}finally{close();}
		return result;
	}

	private void setup(ClassBuild playerBuild,ScenarioId scenarioId,long seed){
		Random.pushGenerator(seed);Actor.clear();Actor.resetNextID();RuleTrace.install(trace);QaCombatMetrics.install(this);Badges.resetForTesting();
		Dungeon.seed=seed;Dungeon.depth=1;Dungeon.daily=true;scenario=scenarioId;build=playerBuild.copy();
		level=new QaFixedLevel(WIDTH,HEIGHT);Dungeon.level=level;configureGeometry();
		hero=new Hero();hero.heroClass=HeroClass.ROGUE;Talent.initClassTalents(hero);hero.HT=100;hero.HP=scenario==ScenarioId.LOW_HP_START?28:100;
		hero.pos=heroCell();hero.damageInterrupt=false;hero.sprite=new NoOpCharSprite(hero);runtime=new RuleRuntime(build);hero.setRuleRuntime(runtime);Dungeon.hero=hero;level.occupyCell(hero);configureHazards();
		level.updateFieldOfView(hero,level.heroFOV);
		spawnInitial();Actor.init();
		if(scenario==ScenarioId.RESOURCE_STARVATION)for(ResourceSpec resource:build.resources)runtime.setResourceForDebug(hero,resource.id,resource.engine,0);
	}

	private void configureGeometry(){
		if(scenario==ScenarioId.NARROW_CORRIDOR){
			for(int y=1;y<HEIGHT-1;y++)for(int x=1;x<WIDTH-1;x++)level.setQaTerrain(x+y*WIDTH,y==HEIGHT/2?Terrain.EMPTY:Terrain.WALL);
		}else if(scenario==ScenarioId.TERRAIN_POOR){
			for(int y=1;y<HEIGHT-1;y++)for(int x=1;x<WIDTH-1;x++)if((x+y)%3==0)level.setQaTerrain(x+y*WIDTH,Terrain.EMPTY_SP);
		}else if(scenario==ScenarioId.TERRAIN_RICH){
			for(int x=2;x<=4;x++)level.setQaTerrain(x+5*WIDTH,Terrain.WATER);
			for(int y=2;y<=7;y++)if(y!=5)level.setQaTerrain(6+y*WIDTH,Terrain.WALL);
			level.setQaTerrain(5+4*WIDTH,Terrain.HIGH_GRASS);
		}
	}

	/** Real SPD hazards, deliberately absent from TERRAIN_POOR. */
	private void configureHazards(){
		if(scenario!=ScenarioId.TERRAIN_RICH)return;
		// Hazards sit directly behind two ordinary swarm approaches relative to the hero. This
		// makes the scenario genuinely exploitable by any hazard-aware forced-movement build,
		// instead of merely containing decorative hazards off the combat lines.
		int[] trapCells={8+2*WIDTH,8+8*WIDTH};
		for(int trapCell:trapCells){level.setTrap(new GrippingTrap().set(trapCell).reveal(),trapCell);level.setQaTerrain(trapCell,Terrain.TRAP);}
		// A durable fire lane rewards keeping or throwing enemies in the prepared choke. The
		// previous 14-volume patch expired before a control build could convert positioning into
		// enemy resolution, making "terrain rich" functionally harder rather than richer.
		Blob fire=Blob.seed(9+5*WIDTH,42,Fire.class);Actor.add(fire);
	}

	private int heroCell(){return scenario==ScenarioId.NARROW_CORRIDOR?2+(HEIGHT/2)*WIDTH:2+5*WIDTH;}

	private void spawnInitial(){
		switch(scenario){
			case SINGLE_NORMAL: spawn("RAT",6+5*WIDTH,22);break;
			case HIGH_HP_SINGLE: spawn("RAT",7+5*WIDTH,110);break;
			case ENEMY_SWARM: spawnSwarm(5,16);break;
			case RANGED_PRESSURE: spawn("DM100",8+5*WIDTH,28);break;
			case NARROW_CORRIDOR: spawn("RAT",8+5*WIDTH,30);spawn("RAT",7+5*WIDTH,22);break;
			case OPEN_ROOM: spawnSwarm(4,20);break;
			case RESOURCE_STARVATION: spawn("RAT",7+5*WIDTH,45);break;
			case SUSTAINED_ENCOUNTER: wavesRemaining=2;spawnSwarm(3,18);break;
			case LOW_HP_START: spawn("RAT",6+5*WIDTH,32);break;
			// Equal pressure, but enough durability that prepared hazards can create a
			// measurable advantage instead of both scenarios saturating at an easy 100%.
			case TERRAIN_POOR: spawnSwarm(4,26);break;
			case TERRAIN_RICH: spawnSwarm(4,26);break;
			case MIXED_THREAT: spawn("DM100",8+3*WIDTH,28);spawn("RAT",7+6*WIDTH,65);spawn("SNAKE",6+4*WIDTH,18);spawn("RAT",8+7*WIDTH,18);break;
		}
	}

	private void spawnNextWave(){wavesRemaining--;spawnSwarm(3,20+5*(2-wavesRemaining));}

	private void spawnSwarm(int count,int hp){int[] cells={7+3*WIDTH,8+5*WIDTH,7+7*WIDTH,5+3*WIDTH,5+7*WIDTH,8+4*WIDTH};for(int i=0;i<count;i++)spawn(i%2==0?"RAT":"SNAKE",legalSpawn(cells[i]),hp);}
	private int legalSpawn(int desired){if(validEmpty(desired))return desired;for(int c=0;c<level.length();c++)if(validEmpty(c)&&level.distance(heroCell(),c)>3)return c;throw new IllegalStateException("no spawn cell");}

	private void spawn(String type,int cell,int hp){
		Mob mob="DM100".equals(type)?new DM100():"SNAKE".equals(type)?new Snake():new Rat();cell=legalSpawn(cell);mob.pos=cell;mob.HT=mob.HP=hp;mob.EXP=0;mob.maxLvl=-3;mob.sprite=new NoOpCharSprite(mob);level.mobs.add(mob);Actor.add(mob);mob.aggro(hero);
	}

	private void takeTurn(){
		turn++;level.updateFieldOfView(hero,level.heroFOV);increment(metrics.modeUptime,mode().isEmpty()?"none":mode(),1);
		LinkedHashMap<String,Integer> resourceBefore=resourceValues();int hpBefore=hero.HP;int marksBefore=markStacks();int entitiesBefore=ownedCount();String modeBefore=mode();int[] mapBefore=level.map.clone();
		executeOpportunityTarget=-1;executeUsedThisTurn=false;
		RuleHooks.onTurnStart(hero);
		ArrayList<Mob> enemies=aliveEnemies();
		ClassOperationSpec operation=chooseAndUseOperation(enemies);
		RuleDefinition used=operation==null?chooseAndUseSkill(enemies):null;
		if(used==null&&operation==null){Mob adjacent=adjacentEnemy();if(adjacent!=null)normalAttack(adjacent);else if(shouldWait())waitTurn();else moveToward(nearestEnemy());}
		int immediateHealing=Math.max(0,hero.HP-hpBefore);metrics.healing+=immediateHealing;
		finishTurn();
		if(executeOpportunityTarget>=0&&!executeUsedThisTurn){Actor target=Actor.findById(executeOpportunityTarget);if(!(target instanceof Char)||!((Char)target).isAlive())metrics.targetKilledBeforeExecute++;}
		collectResourceDelta(resourceBefore,resourceValues());collectStateDelta(marksBefore,entitiesBefore,modeBefore,mapBefore);
	}

	private ClassOperationSpec chooseAndUseOperation(ArrayList<Mob> enemies){
		ClassOperationSpec best=null;int bestScore=-1;
		for(ClassOperationSpec operation:runtime.classOperations()){
			if(operation==null||ClassOperationRuntime.unavailableReason(hero,operation)!=null)continue;
			int value=operationScore(operation,enemies);if(value>bestScore){bestScore=value;best=operation;}
		}
		int skillScore=-1;for(RuleDefinition skill:runtime.activeTechniques())skillScore=Math.max(skillScore,score(skill,enemies,false));
		if(best==null||bestScore<0||bestScore<skillScore)return null;
		int cell=hero.pos;Mob target=nearestEnemy();
		if(best.type==ClassOperationSpec.Type.COMMAND&&target!=null)cell=target.pos;
		else if(best.type==ClassOperationSpec.Type.RECYCLE){Char owned=nearestOwned();if(owned!=null)cell=owned.pos;}
		if(!ClassOperationRuntime.execute(hero,best,cell)){metrics.failedSkillAttempts++;return null;}
		hero.spend(best.actionTime);metrics.actionCostTurns+=Math.max(1,Math.round(best.actionTime));
		increment(metrics.componentSignals,"CLASS_OPERATION:"+best.type.name(),1);
		lastOperationUseTurn.put(best.id,turn);
		if(best.type==ClassOperationSpec.Type.MODE_SWITCH)metrics.switchActionCost+=Math.max(1,Math.round(best.actionTime));
		return best;
	}

	private int operationScore(ClassOperationSpec operation,ArrayList<Mob> enemies){
		switch(operation.type){
			case RELOAD:
				int current=runtime.resourceValue(operation.resourceId,null),maximum=runtime.resourceMax(operation.resourceId,null);
				if(maximum<=0||current>=maximum)return-1;
				return current==0?155:current*2<=maximum?120:40;
			case MODE_SWITCH:
				String desired=desiredMode(skillsForModeEvaluation());
				return desired.equals(mode())?-1:135;
			case COMMAND:
				if(enemies.isEmpty()||ownedCount()==0)return-1;
				return turn-lastOperationUseTurn.getOrDefault(operation.id,-99)>=4?105:-1;
			case RECYCLE:
				if(ownedCount()==0)return-1;
				int value=runtime.resourceValue(operation.resourceId,null),max=runtime.resourceMax(operation.resourceId,null);
				return max>0&&value*2<=max?118:-1;
			default:return-1;
		}
	}

	private RuleDefinition chooseAndUseSkill(ArrayList<Mob> enemies){
		ArrayList<RuleDefinition> skills=runtime.activeTechniques();evaluateExecuteOpportunity(skills,enemies);
		int bestLegal=-1;for(RuleDefinition rule:skills){if(rule.cooldownRemaining()>0)increment(metrics.componentSignals,"COOLDOWN_UPTIME:"+rule.id,1);else bestLegal=Math.max(bestLegal,score(rule,enemies,false));}
		for(RuleDefinition rule:skills)if(rule.cooldownRemaining()>0){int desired=score(rule,enemies,true);if(desired>=Math.max(70,bestLegal)){increment(metrics.cooldownBlockedOpportunities,rule.id,1);increment(metrics.componentSignals,"COOLDOWN_BLOCKED:"+rule.id,1);}}
		// Focus-style economies deliberately reward preparing while enemies are still distant.
		// Do not spend that safe window on a low-value fallback when one WAIT will unlock a
		// resource-gated damage, status, or terrain action. This is generic resource-aware
		// policy, not an archetype script.
		if(shouldWait()&&hasDesiredResourceBlockedSkill(skills,enemies))return null;
		Collections.sort(skills,new Comparator<RuleDefinition>(){@Override public int compare(RuleDefinition a,RuleDefinition b){int compared=Integer.compare(score(b,enemies,false),score(a,enemies,false));return compared!=0?compared:Integer.compare(a.runtimeOrder(),b.runtimeOrder());}});
		int selectedScore=-1;
		for(RuleDefinition rule:skills){int value=score(rule,enemies,false);if(value<0)continue;int cell=targetCell(rule,enemies);String activeMode=mode().isEmpty()?"none":mode();QaCombatMetrics.attribution(rule.id);boolean fired;try{fired=RuleHooks.triggerActive(hero,cell,rule.id);}finally{QaCombatMetrics.attribution(null);}
			if(fired){selectedScore=value;increment(metrics.skillUsage,rule.id,1);metrics.skillUsageCount++;increment(metrics.skillsUsedByMode,activeMode+":"+rule.id,1);increment(metrics.costPayments,rule.cost.type.name(),Math.max(1,rule.cost.amount));if(rule.cost.type==RuleCost.Type.HP)metrics.hpCostPaid+=rule.cost.amount;float time=rule.cost.actionTime();hero.spend(time);if(rule.cost.type==RuleCost.Type.ACTION)metrics.actionCostTurns+=Math.max(1,rule.cost.amount);
				Integer previous=lastSkillUseTurn.put(rule.id,turn);if(previous!=null){increment(metrics.cooldownReuseIntervalTotal,rule.id,turn-previous);increment(metrics.cooldownReuseSamples,rule.id,1);}if(rule.effectSpec.operation==EffectSpec.Operation.DAMAGE_EXECUTE){metrics.executeUsedTurns++;executeUsedThisTurn=true;Mob target=bestEnemy(rule,enemies);if(target!=null)metrics.executeOverkillValue+=Math.max(0,rule.effectSpec.power-target.HP);}if(rule.effectSpec.operation==EffectSpec.Operation.TRANSFORM_MODE)metrics.switchActionCost+=Math.max(1,Math.round(time));
				if(executeOpportunityTarget>=0&&!executeUsedThisTurn)metrics.executeMissedOpportunities++;
				for(RuleDefinition other:skills)if(other!=rule&&other.cooldownRemaining()<=0&&score(other,enemies,false)>=selectedScore)increment(metrics.cooldownLegalNotSelected,other.id,1);return rule;}
			increment(metrics.skillFailures,rule.id,1);metrics.failedSkillAttempts++;
		}
		if(executeOpportunityTarget>=0)metrics.executeMissedOpportunities++;
		return null;
	}

	private boolean hasDesiredResourceBlockedSkill(ArrayList<RuleDefinition> skills,ArrayList<Mob> enemies){
		for(RuleDefinition rule:skills){
			if(rule==null||rule.effectSpec==null||rule.cooldownRemaining()>0
					||rule.cost.type!=RuleCost.Type.RESOURCE||rule.cost.canPay(runtime,hero))continue;
			EffectFamily family=rule.effectSpec.family;
			if((family==EffectFamily.DAMAGE||family==EffectFamily.STATUS
					||family==EffectFamily.WORLD_TERRAIN)&&(!enemies.isEmpty()||family==EffectFamily.WORLD_TERRAIN))return true;
		}
		return false;
	}

	private int score(RuleDefinition rule,ArrayList<Mob> enemies,boolean ignoreCooldown){
		if(rule==null||rule.effectSpec==null||!ignoreCooldown&&rule.cooldownRemaining()>0||rule.usesRemaining()==0||!modeConditionsMet(rule))return-1;
		if(!rule.cost.canPay(runtime,hero))return-1;
		EffectSpec e=rule.effectSpec;Mob target=bestEnemy(rule,enemies);
		if(e.operation==EffectSpec.Operation.RESOURCE_GAIN){int value=runtime.resourceValue(e.resourceId,null),max=runtime.resourceMax(e.resourceId,null);return value>=max?-1:value<=max/2?140:35;}
		if(e.operation==EffectSpec.Operation.TRANSFORM_MODE){String desired=desiredMode(skillsForModeEvaluation());return desired.equals(e.stateId)&&!desired.equals(mode())?130:-1;}
		if(e.operation==EffectSpec.Operation.RECOVER_HEAL&&hero.HP>=hero.HT)return-1;
		if((e.operation==EffectSpec.Operation.DEFENSE_BARRIER||e.operation==EffectSpec.Operation.DEFENSE_TEMP_HP||e.operation==EffectSpec.Operation.DEFENSE_MITIGATE)&&hero.shielding()>10&&hero.HP>65)return-1;
		if(e.operation==EffectSpec.Operation.DAMAGE_EXECUTE){if(target==null)return-1;int threshold=Math.max(1,e.secondaryParameter);return target.HP*100<=target.HT*threshold?155:-1;}
		if(e.operation==EffectSpec.Operation.WORLD_WATER){if(level.water[hero.pos])return-1;return waterDependentBuild()?135:65;}
		// Persistent damaging ground that can cover a swarm is worth the setup action before a
		// single forced-movement attempt. This value is coverage-driven and applies to every
		// build using damaging terrain.
		if(e.operation==EffectSpec.Operation.WORLD_TOXIC_GAS||e.operation==EffectSpec.Operation.WORLD_FIRE)
			return 100+Math.min(48,enemies.size()*12);
		if(e.family==EffectFamily.CREATE_ENTITY||rule.delivery==SkillDelivery.PERSISTENT_CARRIER){if(ownedCount()>=3)return-1;int prior=metrics.skillUsage.containsKey(rule.id)?metrics.skillUsage.get(rule.id):0;return(hero.HP<35?45:enemies.size()>1?115:100)-prior*15;}
		if(e.family==EffectFamily.RELATION_CONTROL||e.family==EffectFamily.TRANSFER_COPY)return ownedCount()==0&&e.operation!=EffectSpec.Operation.COPY_STATUS?-1:70;
		if(enemies.isEmpty()&&e.family!=EffectFamily.RECOVERY_DEFENSE)return-1;
		if(target==null&&(e.family==EffectFamily.DAMAGE||e.family==EffectFamily.STATUS||e.family==EffectFamily.MARK_ACCUMULATION||e.family==EffectFamily.MOVEMENT))return-1;
		switch(e.family){case RECOVERY_DEFENSE:return hero.HP<55?125:45;case DAMAGE:int damageScore=target!=null&&target.HP*100/target.HT<35?115:95;if(enemies.size()>2&&(rule.modifier.repeats()>1||rule.targetingSpec.coverage!=TargetingSpec.Coverage.SINGLE))damageScore+=25;return damageScore;case STATUS:return enemies.size()>1?100:75;case MARK_ACCUMULATION:return 88;case MOVEMENT:return forcedMovementHazardValue(target,e.power)>0?135:hero.HP<40?105:65;case WORLD_TERRAIN:return scenario==ScenarioId.TERRAIN_RICH?105:75;default:return 60;}
	}

	private int targetCell(RuleDefinition rule,ArrayList<Mob> enemies){
		TargetingSpec t=rule.targetingSpec;EffectSpec e=rule.effectSpec;Mob enemy=bestEnemy(rule,enemies);
		if(t==null||t.selector==TargetingSpec.Selector.SELF)return hero.pos;
		if(t.filter==TargetingSpec.Filter.OWNED_ENTITY){if(e.operation==EffectSpec.Operation.RELATION_COMMAND_ATTACK&&enemy!=null)return enemy.pos;Char owned=nearestOwned();return owned==null?hero.pos:owned.pos;}
		if(t.selector==TargetingSpec.Selector.SELECTED_CELL){
			if(e.operation==EffectSpec.Operation.MOVE_DASH)return dashDestination(enemy,hero.HP<45);
			if(e.operation==EffectSpec.Operation.WORLD_WATER&&waterDependentBuild())return hero.pos;
			if(e.operation==EffectSpec.Operation.CREATE_ACTOR||e.operation==EffectSpec.Operation.CREATE_DEVICE||e.operation==EffectSpec.Operation.CREATE_TRAP||rule.delivery==SkillDelivery.PERSISTENT_CARRIER)return deploymentCell(enemy);
			return enemy==null?hero.pos:enemy.pos;
		}
		return enemy==null?hero.pos:enemy.pos;
	}

	private Mob bestEnemy(RuleDefinition rule,ArrayList<Mob> enemies){EffectSpec e=rule==null?null:rule.effectSpec;Mob result=null;int bestHazard=-1;for(Mob mob:enemies){if(rule!=null&&!matchesFilter(mob,rule.targetingSpec))continue;int hazard=e!=null&&e.family==EffectFamily.MOVEMENT?forcedMovementHazardValue(mob,e.power):0;if(result==null||hazard>bestHazard||hazard==bestHazard&&e!=null&&(e.operation==EffectSpec.Operation.DAMAGE_EXECUTE||e.operation==EffectSpec.Operation.DAMAGE_MISSING_HP)&&mob.HP*result.HT<result.HP*mob.HT||hazard==bestHazard&&(e==null||e.operation!=EffectSpec.Operation.DAMAGE_EXECUTE&&e.operation!=EffectSpec.Operation.DAMAGE_MISSING_HP)&&level.distance(hero.pos,mob.pos)<level.distance(hero.pos,result.pos)){result=mob;bestHazard=hazard;}}return result;}
	private boolean matchesFilter(Mob mob,TargetingSpec spec){if(spec==null)return true;switch(spec.filter){case MARKED:int stacks=0;for(RuleMark mark:mob.buffs(RuleMark.class))stacks=Math.max(stacks,mark.stacks());return stacks>=Math.max(1,spec.filterParameter);case HAS_STATUS:for(Buff buff:mob.buffs())if(buff.type==Buff.buffType.NEGATIVE)return true;return false;case HP_THRESHOLD:return mob.HP*100<=mob.HT*Math.max(1,spec.filterParameter);case ALLY:case SELF:case OWNED_ENTITY:return false;default:return true;}}
	private int dashDestination(Mob enemy,boolean retreat){int best=hero.pos,bestDistance=retreat?-1:Integer.MAX_VALUE;for(int c=0;c<level.length();c++)if(validEmpty(c)&&level.distance(hero.pos,c)<=6){int d=enemy==null?0:level.distance(c,enemy.pos);if(retreat?d>bestDistance:d<bestDistance){best=c;bestDistance=d;}}return best;}
	private int emptyNear(int center){if(validEmpty(center))return center;int best=-1,d=Integer.MAX_VALUE;for(int c=0;c<level.length();c++)if(validEmpty(c)){int value=level.distance(center,c);if(value<d){d=value;best=c;}}return best<0?hero.pos:best;}
	private int deploymentCell(Mob enemy){if(enemy==null)return emptyNear(hero.pos);int best=-1,bestScore=Integer.MIN_VALUE;for(int delta:PathFinder.NEIGHBOURS8){int cell=enemy.pos+delta;if(!validEmpty(cell))continue;int score=-level.distance(hero.pos,cell)*3-level.distance(enemy.pos,cell);if(onLineBetween(hero.pos,cell,enemy.pos))score+=18;if(level.traps.get(cell)!=null)score-=20;if(score>bestScore){bestScore=score;best=cell;}}return best<0?emptyNear(enemy.pos):best;}
	private boolean onLineBetween(int from,int cell,int to){return level.distance(from,cell)+level.distance(cell,to)<=level.distance(from,to)+1;}
	private int forcedMovementHazardValue(Mob target,int power){if(target==null)return 0;int width=level.width(),dx=Integer.signum(target.pos%width-hero.pos%width),dy=Integer.signum(target.pos/width-hero.pos/width),cell=target.pos,best=0;for(int i=0;i<Math.max(1,power);i++){int next=cell+dx+dy*width;if(next<0||next>=level.length()||level.solid[next]||Actor.findChar(next)!=null)break;cell=next;best=Math.max(best,hazardValue(cell));}return best;}
	private int hazardValue(int cell){if(cell<0||cell>=level.length())return 0;if(level.pit[cell])return 8;if(level.traps.get(cell)!=null&&level.traps.get(cell).active)return 6;Blob fire=level.blobs.get(Fire.class);if(fire!=null&&fire.cur!=null&&fire.cur[cell]>0)return 5;for(Blob blob:level.blobs.values())if(blob!=null&&blob.cur!=null&&blob.cur[cell]>0)return 4;return 0;}
	private boolean waterDependentBuild(){if(build.laws.contains(ClassLaw.WATER_AFFINITY))return true;for(com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec skill:build.skills)if(skill.constraint!=null&&skill.constraint.variant==com.shatteredpixel.shatteredpixeldungeon.rules.SkillConstraint.Variant.SELF_IN_WATER)return true;return false;}
	private boolean modeConditionsMet(RuleDefinition rule){for(RuleCondition condition:rule.conditions)if(condition!=null&&condition.type==RuleCondition.Type.MODE_IS&&!condition.reference.equals(mode()))return false;return true;}
	private ArrayList<RuleDefinition> skillsForModeEvaluation(){return runtime.activeTechniques();}
	private String desiredMode(ArrayList<RuleDefinition> skills){LinkedHashMap<String,Integer> values=new LinkedHashMap<>();boolean pressured=hero.HP<50||adjacentEnemyCount()>=2;for(RuleDefinition skill:skills){String required="";for(RuleCondition condition:skill.conditions)if(condition!=null&&condition.type==RuleCondition.Type.MODE_IS)required=condition.reference;if(required.isEmpty())continue;int value=0;switch(skill.effectSpec.family){case RECOVERY_DEFENSE:value=pressured?120:35;break;case MOVEMENT:value=pressured?95:55;break;case DAMAGE:value=pressured?55:120;break;case STATUS:value=pressured?80:100;break;default:value=60;}values.put(required,(values.containsKey(required)?values.get(required):0)+value);}String best=mode();int score=Integer.MIN_VALUE;for(String key:values.keySet())if(values.get(key)>score){best=key;score=values.get(key);}return best;}
	private void evaluateExecuteOpportunity(ArrayList<RuleDefinition> skills,ArrayList<Mob> enemies){for(RuleDefinition rule:skills)if(rule.effectSpec!=null&&rule.effectSpec.operation==EffectSpec.Operation.DAMAGE_EXECUTE){Mob target=bestEnemy(rule,enemies);boolean markReady=target!=null;if(markReady&&(rule.cooldownRemaining()>0||!rule.cost.canPay(runtime,hero)))metrics.marksReadyButExecuteUnavailable++;if(target!=null&&target.HP*100<=target.HT*Math.max(1,rule.effectSpec.secondaryParameter)&&rule.cooldownRemaining()<=0&&rule.cost.canPay(runtime,hero)&&modeConditionsMet(rule)){metrics.executeLegalTurns++;executeOpportunityTarget=target.id();}}}

	private void normalAttack(Mob target){QaCombatMetrics.attribution("NORMAL_ATTACK");try{hero.performBasicAttack(target);}finally{QaCombatMetrics.attribution(null);}hero.spend(hero.attackDelay());metrics.normalAttackCount++;}
	private void waitTurn(){RuleHooks.onWait(hero);hero.spend(Actor.TICK);metrics.waitCount++;}
	private boolean shouldWait(){Mob enemy=nearestEnemy();int distance=enemy==null?99:level.distance(hero.pos,enemy.pos);if(build.primaryResource()!=null&&build.primaryResource().engine==ResourceEngine.FOCUS&&runtime.resource()<runtime.maxResource()/2&&distance>2)return true;if(hasAutomatedOwned()&&distance>1)return true;return enemy==null;}
	private void moveToward(Mob target){if(target==null){waitTurn();return;}boolean kite=hasAutomatedOwned();int best=-1,bestScore=Integer.MIN_VALUE;for(int delta:PathFinder.NEIGHBOURS8){int cell=hero.pos+delta;if(!validEmpty(cell)||hazardValue(cell)>0)continue;int distance=level.distance(cell,target.pos);int score=(kite?distance:-distance)*10;if(waterDependentBuild()&&level.water[cell])score+=18;if(score>bestScore){bestScore=score;best=cell;}}if(best<0){waitTurn();return;}int from=hero.pos;hero.move(best,false);hero.spend(Actor.TICK);metrics.moveCount++;increment(metrics.movementByMode,mode().isEmpty()?"none":mode(),level.distance(from,best));}
	private boolean hasAutomatedOwned(){for(Char ch:Actor.chars())if(ch instanceof RuleOwnedEntity&&((RuleOwnedEntity)ch).ownerId()==hero.id()&&ch.isAlive()&&((RuleOwnedEntity)ch).kind()!=RuleOwnedEntity.Kind.ACTOR)return true;return false;}

	private void finishTurn(){Actor.processUntilTurn(hero,10000);int controlled=0;for(Mob mob:aliveEnemies())if(controlled(mob)){controlled++;controlledEnemies.add(mob.id());}metrics.controlTurns+=controlled;if(controlled>0)increment(metrics.componentControl,"CONTROL_STATE",controlled);metrics.entityLifetimeTurns+=ownedCount();}
	private boolean controlled(Mob mob){return mob.buff(Slow.class)!=null||mob.buff(Paralysis.class)!=null||mob.buff(Roots.class)!=null||mob.buff(Amok.class)!=null||mob.buff(Terror.class)!=null;}

	private LinkedHashMap<String,Integer> resourceValues(){LinkedHashMap<String,Integer> result=new LinkedHashMap<>();for(ResourceSpec r:build.resources)result.put(r.id,runtime.resourceValue(r.id,r.engine));return result;}
	private void collectResourceDelta(LinkedHashMap<String,Integer> before,LinkedHashMap<String,Integer> after){for(String id:after.keySet()){int old=before.containsKey(id)?before.get(id):0,now=after.get(id);if(now>old){metrics.resourceGenerated+=now-old;increment(metrics.resourceGeneratedByPool,id,now-old);increment(metrics.componentResource,id,now-old);if(level.water[hero.pos])increment(metrics.componentSignals,"WATER_AFFINITY_CONTEXT",now-old);}else if(now<old){metrics.resourceSpent+=old-now;increment(metrics.resourceSpentByPool,id,old-now);}}}
	private void collectStateDelta(int marksBefore,int entitiesBefore,String modeBefore,int[] mapBefore){int marksAfter=markStacks();if(marksAfter>marksBefore)metrics.marksApplied+=marksAfter-marksBefore;else if(marksAfter<marksBefore)metrics.marksConsumed+=marksBefore-marksAfter;int entities=ownedCount();if(entities>entitiesBefore){metrics.entitiesCreated+=entities-entitiesBefore;increment(metrics.componentEntity,"CREATE_ENTITY",entities-entitiesBefore);}String current=mode();if(!current.equals(modeBefore)){metrics.modeSwitches++;if(current.equals(modeBeforePrevious)&&turn-lastModeSwitchTurn<=2)metrics.immediateSwitchBacks++;if(current.equals(previousMode))metrics.redundantModeSwitches++;modeBeforePrevious=modeBefore;previousMode=current;lastModeSwitchTurn=turn;}int changed=0;for(int i=0;i<mapBefore.length;i++)if(mapBefore[i]!=level.map[i]){alteredCells.add(i);changed++;}if(changed>0)increment(metrics.componentTerrain,"ALTER_TERRAIN",changed);}

	private int markStacks(){int result=0;for(Char ch:Actor.chars())for(RuleMark mark:ch.buffs(RuleMark.class))result+=mark.stacks();return result;}
	private String mode(){RuleMode value=hero.buff(RuleMode.class);return value==null?"":value.modeId();}
	private int ownedCount(){int result=0;for(Char ch:Actor.chars())if(ch instanceof RuleOwnedEntity&&((RuleOwnedEntity)ch).ownerId()==hero.id()&&ch.isAlive())result++;return result;}
	private Char nearestOwned(){Char result=null;int distance=Integer.MAX_VALUE;for(Char ch:Actor.chars())if(ch instanceof RuleOwnedEntity&&((RuleOwnedEntity)ch).ownerId()==hero.id()&&ch.isAlive()){int d=level.distance(hero.pos,ch.pos);if(d<distance){distance=d;result=ch;}}return result;}
	private Mob adjacentEnemy(){for(Mob mob:aliveEnemies())if(level.adjacent(hero.pos,mob.pos))return mob;return null;}
	private int adjacentEnemyCount(){int result=0;for(Mob mob:aliveEnemies())if(level.adjacent(hero.pos,mob.pos))result++;return result;}
	private Mob nearestEnemy(){return bestEnemy(null,aliveEnemies());}
	private ArrayList<Mob> aliveEnemies(){ArrayList<Mob> result=new ArrayList<>();for(Mob mob:level.mobs)if(mob.isAlive()&&mob.alignment==Char.Alignment.ENEMY)result.add(mob);Collections.sort(result,new Comparator<Mob>(){@Override public int compare(Mob a,Mob b){return Integer.compare(a.id(),b.id());}});return result;}
	private boolean hasEnemies(){return!aliveEnemies().isEmpty();}
	private boolean validEmpty(int cell){return cell>=0&&cell<level.length()&&(level.passable[cell]||level.avoid[cell])&&!level.pit[cell]&&Actor.findChar(cell)==null;}
	private int maxTurns(){switch(scenario){case HIGH_HP_SINGLE:case SUSTAINED_ENCOUNTER:case MIXED_THREAT:return 100;case ENEMY_SWARM:case TERRAIN_RICH:return 80;default:return 60;}}

	private void parseTraceMetrics(){String text=trace.text();metrics.attachmentTriggers=count(text,"ATTACHMENT trigger");Matcher matcher=Pattern.compile("overflow=(\\d+)").matcher(text);while(matcher.find())metrics.resourceOverflowWaste+=Integer.parseInt(matcher.group(1));String[] signals={"PROPAGATION","PHASE_SHIFT","SHIELD_BACKLASH","TRANSLOCATION_BACKLASH","BRIDGE","COMPENSATION","OVERFLOW","OVERDRAW","STATUS_FEEDBACK","WATER_FLOW","KILL_TEMPO","KINETIC_MARK","MOBILE_CHARGE","TEMP_HP_PAYMENT","PIERCING_MARK","OWNED_RESOURCE_FEEDBACK","CARRIER_RESOURCE_FEEDBACK","HAZARD_FEEDBACK","MODE_GUARD","TRANSFER_FEEDBACK","STATUS_CHAIN","COMMAND","CARRIER","REDIRECT","OWNED_ACTOR","OWNED_ATTACK"};for(String signal:signals){int amount=count(text,signal);if(amount>0)metrics.componentSignals.put(signal,amount);}metrics.skillUsage.clear();metrics.skillUsageCount=0;for(RuleDefinition rule:runtime.rules())if(rule.triggerCount()>0){metrics.skillUsage.put(rule.id,rule.triggerCount());metrics.skillUsageCount+=rule.triggerCount();}}
	private static int count(String text,String token){int count=0,from=0;while((from=text.indexOf(token,from))>=0){count++;from+=token.length();}return count;}
	private static void increment(LinkedHashMap<String,Integer> values,String key,int amount){values.put(key,(values.containsKey(key)?values.get(key):0)+amount);}

	@Override public void onDamage(Char target,Object source,String attribution,int requested,int hpLost,int shieldLost,int temporaryHpLost,int mitigated){
		if(target==hero){metrics.damageReceived+=hpLost+shieldLost;metrics.temporaryHpConsumed+=temporaryHpLost;metrics.barrierConsumed+=Math.max(0,shieldLost-temporaryHpLost);metrics.damageMitigated+=mitigated;String activeMode=mode().isEmpty()?"none":mode();if(mitigated+shieldLost>0)increment(metrics.mitigationByMode,activeMode,mitigated+shieldLost);if(temporaryHpLost>0)increment(metrics.componentMitigation,"TEMPORARY_HP",temporaryHpLost);if(shieldLost-temporaryHpLost>0)increment(metrics.componentMitigation,"BARRIER",shieldLost-temporaryHpLost);if(mitigated>0)increment(metrics.componentMitigation,"MITIGATION",mitigated);return;}
		if(target==null||target.alignment!=Char.Alignment.ENEMY)return;int actual=hpLost+shieldLost;if(actual<=0)return;
		increment(metrics.damageByMode,mode().isEmpty()?"none":mode(),actual);
		if(source instanceof RuleOwnedEntity){RuleOwnedEntity entity=(RuleOwnedEntity)source;if(entity.kind()==RuleOwnedEntity.Kind.ACTOR)metrics.entityDamage+=actual;else metrics.carrierDamage+=actual;increment(metrics.componentDamage,"OWNED_"+entity.kind().name(),actual);increment(metrics.componentEntity,"DAMAGE_"+entity.kind().name(),actual);}
		else if(attribution==null&&(source instanceof Poison||source instanceof Burning||source instanceof Bleeding||source instanceof Blob))metrics.statusDamage+=actual;
		else{metrics.playerDirectDamage+=actual;increment(metrics.componentDamage,attribution==null?"UNATTRIBUTED_RULE":attribution,actual);}
	}

	@Override public void onForcedMovement(Char target,int from,int to){
		if(level==null||target==null||from==to)return;
		int cells=Math.max(1,level.distance(from,to));
		metrics.forcedMovementCells+=cells;
		increment(metrics.componentControl,"FORCED_MOVEMENT",cells);
	}

	@Override public void close(){QaCombatMetrics.clear();RuleTrace.clear();Actor.clear();Dungeon.hero=null;Dungeon.level=null;Dungeon.daily=false;try{Random.popGenerator();}catch(Throwable ignored){}}

	private static class NoOpCharSprite extends CharSprite {
		NoOpCharSprite(Char owner){ch=owner;visible=false;}
		@Override public void showStatus(int color,String text,Object...args){}
		@Override public void showStatusWithIcon(int color,String text,int icon,Object...args){}
		@Override public void place(int cell){}
		@Override public void move(int from,int to){}
		@Override public void turnTo(int from,int to){}
		@Override public synchronized void attack(int cell,Callback callback){if(callback!=null)callback.call();else ch.onAttackComplete();}
		@Override public synchronized void operate(int cell,Callback callback){if(callback!=null)callback.call();else ch.onOperateComplete();}
		@Override public synchronized void zap(int cell,Callback callback){if(callback!=null)callback.call();else ch.onAttackComplete();}
		@Override public void jump(int from,int to,Callback callback){ch.pos=to;if(callback!=null)callback.call();}
		@Override public void jump(int from,int to,float height,float duration,Callback callback){ch.pos=to;if(callback!=null)callback.call();}
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
