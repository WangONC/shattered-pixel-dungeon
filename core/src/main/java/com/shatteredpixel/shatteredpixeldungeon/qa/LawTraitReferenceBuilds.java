package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.rules.*;

import java.util.LinkedHashMap;

/** Small QA-only builds composed exclusively from the same registries used by CREATE A CLASS. */
public final class LawTraitReferenceBuilds {
	private LawTraitReferenceBuilds() {}

	public static LinkedHashMap<String,ClassBuild> all() {
		LinkedHashMap<String,ClassBuild> result=new LinkedHashMap<>();
		for(ClassLaw law:LawTraitRegistry.laws())result.put("LAW:"+law.name(),forLaw(law));
		for(CoreRuleVocabulary trait:LawTraitRegistry.traits())result.put("TRAIT:"+trait.name(),forTrait(trait));
		return result;
	}

	public static ClassBuild forLaw(ClassLaw law) {
		ClassBuild build=base("law_"+law.name().toLowerCase());
		build.laws.add(law);
		switch(law){
			case HEALING_TO_SHIELD: build.skills.add(skill("heal",EffectSpec.Operation.RECOVER_HEAL,SkillDelivery.SELF,self())); break;
			case FORCED_MOVEMENT_COUNTS_AS_MOVE: build.skills.add(skill("push",EffectSpec.Operation.MOVE_PUSH,SkillDelivery.DIRECT_TARGET,enemy())); break;
			case TRANSLOCATION_COUNTS_AS_ENTER_TILE: build.skills.add(skill("dash",EffectSpec.Operation.MOVE_DASH,SkillDelivery.GROUND_PLACEMENT,cell())); break;
			case RESOURCE_OVERDRAFT_USES_HP: addResource(build,"mana",ResourceEngine.MANA); build.skills.add(resourceSkill("bolt",EffectSpec.Operation.DAMAGE_STANDARD,"mana")); break;
			case OWNED_ACTIONS_COUNT_AS_YOURS: owned(build,ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR,2);build.skills.add(skill("summon",EffectSpec.Operation.CREATE_ACTOR,SkillDelivery.GROUND_PLACEMENT,cell())); break;
			default: break;
		}
		return build;
	}

	public static ClassBuild forTrait(CoreRuleVocabulary type) {
		ClassBuild build=base("trait_"+type.name().toLowerCase());
		String resource="mana";
		if(type.requiresResource())addResource(build,resource,ResourceEngine.MANA);
		switch(type){
			case ACCUMULATION:{SkillSpec move=skill("move_guard",EffectSpec.Operation.DEFENSE_BARRIER,SkillDelivery.SELF,self());move.activation=RuleEvent.ON_MOVE;build.skills.add(move);break;}
			case OVERFLOW:
			case WATER_FLOW:{EffectSpec gain=new EffectSpec(EffectFamily.RESOURCE_OPERATION,EffectSpec.Operation.RESOURCE_GAIN,4);gain.resourceId=resource;build.skills.add(active("fill",gain,SkillDelivery.SELF,self()));break;}
			case COMPENSATION: build.skills.add(skill("heal",EffectSpec.Operation.RECOVER_HEAL,SkillDelivery.SELF,self())); break;
			case HUNT_MARK:{SkillSpec hit=skill("hit",EffectSpec.Operation.DAMAGE_STANDARD,SkillDelivery.CONTACT_ATTACK,enemy());hit.activation=RuleEvent.ON_HIT;build.skills.add(hit);break;}
			case PROPAGATION: build.traits.add(TraitSpec.of(CoreRuleVocabulary.HUNT_MARK)); break;
			case STATUS_FEEDBACK: build.skills.add(skill("poison",EffectSpec.Operation.STATUS_POISON,SkillDelivery.DIRECT_TARGET,enemy())); break;
			case KINETIC_MARK: build.skills.add(skill("push",EffectSpec.Operation.MOVE_PUSH,SkillDelivery.DIRECT_TARGET,enemy())); break;
			case TEMP_HP_PAYMENT:{build.skills.add(skill("shell",EffectSpec.Operation.DEFENSE_TEMP_HP,SkillDelivery.SELF,self()));SkillSpec hp=skill("blood_hit",EffectSpec.Operation.DAMAGE_STANDARD,SkillDelivery.CONTACT_ATTACK,enemy());hp.cost=new RuleCost(RuleCost.Type.HP,2);build.skills.add(hp);break;}
			case PIERCING_MARK:{SkillSpec shot=skill("shot",EffectSpec.Operation.DAMAGE_STANDARD,SkillDelivery.PROJECTILE,enemy());shot.modifier=new RuleModifier(RuleModifier.Type.PIERCE);build.skills.add(shot);break;}
			case OWNED_RESOURCE_FEEDBACK: owned(build,ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR,2);build.skills.add(skill("summon",EffectSpec.Operation.CREATE_ACTOR,SkillDelivery.GROUND_PLACEMENT,cell())); break;
			case CARRIER_RESOURCE_FEEDBACK: owned(build,ClassGameplayComponentSpec.EntityFilter.OWNED_CARRIER,2);build.skills.add(skill("device",EffectSpec.Operation.DAMAGE_STANDARD,SkillDelivery.PERSISTENT_CARRIER,cell())); break;
			case HAZARD_FEEDBACK: build.skills.add(skill("push",EffectSpec.Operation.MOVE_PUSH,SkillDelivery.DIRECT_TARGET,enemy()));build.skills.add(skill("fire",EffectSpec.Operation.WORLD_FIRE,SkillDelivery.GROUND_PLACEMENT,cell()));break;
			case MODE_GUARD:{ClassGameplayComponentSpec modes=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.MODE_ENGINE,"mode_engine");modes.modes.add("guard");modes.modes.add("assault");build.gameplayComponents.add(modes);break;}
			case TRANSFER_FEEDBACK: build.skills.add(skill("copy",EffectSpec.Operation.COPY_STATUS,SkillDelivery.DIRECT_TARGET,enemy())); break;
			case STATUS_CHAIN: build.skills.add(skill("poison",EffectSpec.Operation.STATUS_POISON,SkillDelivery.DIRECT_TARGET,enemy()));build.skills.add(skill("mark",EffectSpec.Operation.MARK_STACK,SkillDelivery.DIRECT_TARGET,enemy()));break;
			default: break;
		}
		TraitSpec trait=type.requiresResource()?TraitSpec.resource(type,resource)
				:type.requiresMode()?TraitSpec.state(type,"guard"):TraitSpec.of(type);
		build.traits.add(trait);
		return build;
	}

	/** Real, deterministic actions used by lawTraitVocabularyQa; no fixture-only runtime path. */
	public static QaScenario scenarioForLaw(ClassLaw law) {
		QaScenario q=scenario("law_"+law.name().toLowerCase(),forLaw(law),93000+law.ordinal());
		switch(law){
			case HEALING_TO_SHIELD:
				q.actions.add(action(QaScenario.Action.Type.SET_HP).value(50));
				q.actions.add(active("heal",24)); break;
			case FORCED_MOVEMENT_COUNTS_AS_MOVE:
				q.actions.add(spawn(25,20)); q.actions.add(active("push",25)); break;
			case TRANSLOCATION_COUNTS_AS_ENTER_TILE:
				q.actions.add(active("dash",23)); break;
			case RESOURCE_OVERDRAFT_USES_HP:
				q.actions.add(action(QaScenario.Action.Type.SET_RESOURCE).resource("mana").value(0));
				q.actions.add(spawn(25,20)); q.actions.add(active("bolt",25)); break;
			case OWNED_ACTIONS_COUNT_AS_YOURS:
				q.actions.add(spawn(22,30)); q.actions.add(active("summon",23));
				q.actions.add(action(QaScenario.Action.Type.WAIT).repeat(5)); break;
			default: break;
		}
		return q;
	}

	public static QaScenario scenarioForTrait(CoreRuleVocabulary type) {
		QaScenario q=scenario("trait_"+type.name().toLowerCase(),forTrait(type),94000+type.ordinal());
		switch(type){
			case ACCUMULATION:
			case MOBILE_CHARGE:
				q.actions.add(move(23));q.actions.add(move(24));q.actions.add(move(23));break;
			case OVERFLOW:
				q.actions.add(action(QaScenario.Action.Type.SET_RESOURCE).resource("mana").value(10));q.actions.add(active("fill",24));break;
			case COMPENSATION: q.actions.add(active("heal",24));break;
			case PHASE_SHIFT: q.actions.add(action(QaScenario.Action.Type.SET_HP).value(20));q.actions.add(spawn(25,20));q.actions.add(active("basic_strike",25));break;
			case ECHO: q.actions.add(spawn(25,30));q.actions.add(active("basic_strike",25));q.actions.add(action(QaScenario.Action.Type.WAIT).repeat(3));break;
			case HUNT_MARK: q.actions.add(spawn(25,20));q.actions.add(action(QaScenario.Action.Type.ATTACK).repeat(2));break;
			case PROPAGATION: q.actions.add(spawn(25,1));q.actions.add(spawn(17,40));q.actions.add(action(QaScenario.Action.Type.ATTACK).target(2).repeat(10));break;
			case STATUS_FEEDBACK: q.actions.add(spawn(25,20));q.actions.add(active("poison",25));break;
			case WATER_FLOW: q.actions.add(action(QaScenario.Action.Type.SET_TILE).cell(24).tile("WATER"));q.actions.add(action(QaScenario.Action.Type.SET_RESOURCE).resource("mana").value(0));q.actions.add(active("fill",24));break;
			case KILL_TEMPO: q.actions.add(spawn(25,1));q.actions.add(active("basic_strike",25));break;
			case KINETIC_MARK: q.actions.add(spawn(25,20));q.actions.add(active("push",25));break;
			case TEMP_HP_PAYMENT: q.actions.add(active("shell",24));q.actions.add(spawn(25,20));q.actions.add(active("blood_hit",25));break;
			case PIERCING_MARK: q.actions.add(spawn(25,20));q.actions.add(active("shot",25));break;
			case OWNED_RESOURCE_FEEDBACK: q.actions.add(spawn(22,30));q.actions.add(active("summon",23));q.actions.add(action(QaScenario.Action.Type.WAIT).repeat(5));break;
			case CARRIER_RESOURCE_FEEDBACK: q.actions.add(spawn(22,30));q.actions.add(active("device",23));q.actions.add(action(QaScenario.Action.Type.WAIT).repeat(5));break;
			case HAZARD_FEEDBACK: q.actions.add(action(QaScenario.Action.Type.SET_TILE).cell(26).tile("WATER"));q.actions.add(spawn(25,20));q.actions.add(active("push",25));break;
			case MODE_GUARD: q.actions.add(action(QaScenario.Action.Type.USE_CLASS_OPERATION).operation("mode_switch").cell(24));break;
			case TRANSFER_FEEDBACK: q.actions.add(action(QaScenario.Action.Type.APPLY_STATUS).status("POISON").value(5));q.actions.add(spawn(25,20));q.actions.add(active("copy",25));break;
			case STATUS_CHAIN: q.actions.add(spawn(25,20));q.actions.add(active("mark",25));q.actions.add(active("poison",25));break;
			default: break;
		}
		return q;
	}

	public static String lawEvidence(ClassLaw law){switch(law){
		case HEALING_TO_SHIELD:return"HEALING_TO_SHIELD";case FORCED_MOVEMENT_COUNTS_AS_MOVE:return"FORCED_MOVEMENT cause=";
		case TRANSLOCATION_COUNTS_AS_ENTER_TILE:return"TRANSLOCATION cause=";case RESOURCE_OVERDRAFT_USES_HP:return"OVERDRAW missing=";
		case OWNED_ACTIONS_COUNT_AS_YOURS:return"OWNED_ACTIONS_COUNT_AS_YOURS";default:return"";}}
	public static String traitEvidence(CoreRuleVocabulary type){switch(type){
		case ECHO:return"ECHO schedule";case HUNT_MARK:return"HUNT_MARK target=";case PROPAGATION:return"PROPAGATION from=";
		case ACCUMULATION:return"ACCUMULATION event=";case COMPENSATION:return"COMPENSATION applied=";case PHASE_SHIFT:return"PHASE_SHIFT enter";
		default:return type.name();}}

	private static QaScenario scenario(String id,ClassBuild build,long seed){QaScenario q=new QaScenario();q.id=id;q.seed=seed;q.hero.classBuild=build;return q;}
	private static QaScenario.Action action(QaScenario.Action.Type type){return QaScenario.Action.of(type);}
	private static QaScenario.Action active(String id,int cell){return action(QaScenario.Action.Type.USE_ACTIVE_RULE).skill(id).cell(cell);}
	private static QaScenario.Action spawn(int cell,int hp){return action(QaScenario.Action.Type.SPAWN_MOB).mob("RAT").cell(cell).value(hp);}
	private static QaScenario.Action move(int cell){return action(QaScenario.Action.Type.MOVE).cell(cell);}

	private static ClassBuild base(String name){ClassBuild build=new ClassBuild();build.name=name;
		build.skills.add(skill("basic_strike",EffectSpec.Operation.DAMAGE_STANDARD,SkillDelivery.CONTACT_ATTACK,enemy()));return build;}
	private static void addResource(ClassBuild build,String id,ResourceEngine engine){ResourceSpec value=new ResourceSpec(engine);value.id=id;build.resources.add(value);}
	private static void owned(ClassBuild build,ClassGameplayComponentSpec.EntityFilter filter,int capacity){build.gameplayComponents.add(new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.OWNERSHIP,"ownership"));ClassGameplayComponentSpec value=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.ENTITY_CAPACITY,"capacity_"+filter.name().toLowerCase());value.entityFilter=filter;value.capacity=capacity;build.gameplayComponents.add(value);}
	private static SkillSpec resourceSkill(String id,EffectSpec.Operation operation,String resource){SkillSpec skill=skill(id,operation,SkillDelivery.DIRECT_TARGET,enemy());skill.cost=new RuleCost(RuleCost.Type.RESOURCE,2);skill.cost.resourceId=resource;skill.cost.resourceEngine=ResourceEngine.MANA;return skill;}
	private static SkillSpec active(String id,EffectSpec effect,SkillDelivery delivery,TargetingSpec target){return skill(id,EffectSpec.Operation.LEGACY,delivery,target,effect);}
	private static SkillSpec skill(String id,EffectSpec.Operation ignored,SkillDelivery delivery,TargetingSpec target,EffectSpec effect){SkillSpec skill=new SkillSpec();skill.id=id;skill.primary=effect;skill.delivery=delivery;skill.targeting=target;return skill;}
	private static SkillSpec skill(String id,EffectSpec.Operation operation,SkillDelivery delivery,TargetingSpec target){SkillSpec skill=new SkillSpec();skill.id=id;skill.primary=new EffectSpec(EffectSpec.familyFor(operation),operation,2);skill.delivery=delivery;skill.targeting=target;return skill;}
	private static TargetingSpec self(){TargetingSpec target=new TargetingSpec();return target;}
	private static TargetingSpec enemy(){return new TargetingSpec(RuleTarget.Type.SELECTED_TARGET);}
	private static TargetingSpec cell(){return new TargetingSpec(RuleTarget.Type.SELECTED_CELL);}
}
