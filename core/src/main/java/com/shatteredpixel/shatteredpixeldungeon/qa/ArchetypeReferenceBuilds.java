package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBudgetPolicy;
import com.shatteredpixel.shatteredpixeldungeon.rules.BasicAttackProfile;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassGameplayComponentSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillConstraint;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.StartingKitSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TraitSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;

/**
 * Slot-free QA builds made exclusively from components exposed by CREATE A CLASS.  Names are
 * archetype labels for DEV/QA only; no runtime code branches on them.
 */
public final class ArchetypeReferenceBuilds {
	public enum Id {
		MARTIAL_DEFENDER, BLOOD_BERSERKER, AMMO_GUNNER, AREA_CASTER, MARK_ASSASSIN,
		OWNED_SUMMONER, DEVICE_ENGINEER, TERRAIN_CONTROLLER, STATUS_CONTROLLER, MODE_SHIFTER,
		REACTION_ONLY, RESOURCELESS_COOLDOWN, DUAL_RESOURCE, TRANSFER_SUPPORT, PURE_CONTROL
	}

	private ArchetypeReferenceBuilds() {}

	public static ArrayList<Id> primaryIds() {
		return new ArrayList<>(Arrays.asList(Id.MARTIAL_DEFENDER, Id.BLOOD_BERSERKER,
				Id.AMMO_GUNNER, Id.AREA_CASTER, Id.MARK_ASSASSIN, Id.OWNED_SUMMONER,
				Id.DEVICE_ENGINEER, Id.TERRAIN_CONTROLLER, Id.STATUS_CONTROLLER, Id.MODE_SHIFTER));
	}

	public static ArrayList<Id> allIds() { return new ArrayList<>(Arrays.asList(Id.values())); }

	public static String displayName(Id id) {
		return Messages.get(ArchetypeReferenceBuilds.class, id.name().toLowerCase()+"_name");
	}

	public static String playstyle(Id id) {
		return Messages.get(ArchetypeReferenceBuilds.class, id.name().toLowerCase()+"_desc");
	}

	public static ClassBuild build(Id id) {
		ClassBuild result;
		switch (id) {
			case MARTIAL_DEFENDER: result=martialDefender(); break;
			case BLOOD_BERSERKER: result=bloodBerserker(); break;
			case AMMO_GUNNER: result=ammoGunner(); break;
			case AREA_CASTER: result=areaCaster(); break;
			case MARK_ASSASSIN: result=markAssassin(); break;
			case OWNED_SUMMONER: result=ownedSummoner(); break;
			case DEVICE_ENGINEER: result=deviceEngineer(); break;
			case TERRAIN_CONTROLLER: result=terrainController(); break;
			case STATUS_CONTROLLER: result=statusController(); break;
			case MODE_SHIFTER: result=modeShifter(); break;
			case REACTION_ONLY: result=reactionOnly(); break;
			case RESOURCELESS_COOLDOWN: result=resourcelessCooldown(); break;
			case DUAL_RESOURCE: result=dualResource(); break;
			case TRANSFER_SUPPORT: result=transferSupport(); break;
			case PURE_CONTROL: result=pureControl(); break;
			default: throw new IllegalArgumentException(String.valueOf(id));
		}
		result.resolvePendingBindings();
		result.syncClassOperations();
		return result;
	}

	public static EnumMap<Id, Integer> budgets() {
		EnumMap<Id,Integer> result=new EnumMap<>(Id.class);
		for(Id id:Id.values())result.put(id,build(id).usedBudget());
		return result;
	}

	private static ClassBuild martialDefender() {
		ClassBuild b=base(Id.MARTIAL_DEFENDER);
		basic(b,BasicAttackProfile.FULL);
		b.skills.add(active("brace", effect(EffectFamily.RECOVERY_DEFENSE, EffectSpec.Operation.DEFENSE_TEMP_HP,8), self(), SkillDelivery.SELF, cooldown(4)));
		SkillSpec bash=active("shield_bash",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,5),enemy(),SkillDelivery.CONTACT_ATTACK,cooldown(3));
		bash.secondary=effect(EffectFamily.MOVEMENT,EffectSpec.Operation.MOVE_THROW,2);b.skills.add(bash);
		SkillSpec counter=react("counter",RuleEvent.ON_DAMAGED,effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,6),enemy(),SkillDelivery.CONTACT_ATTACK);
		counter.secondary=effect(EffectFamily.MOVEMENT,EffectSpec.Operation.MOVE_THROW,2);counter.modifier=modifier(RuleModifier.Type.INTENSITY,2);b.skills.add(counter);
		b.skills.add(react("impact_guard",RuleEvent.ON_HIT,effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_BARRIER,4),self(),SkillDelivery.SELF));
		b.skills.add(active("mitigate",effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_MITIGATE,4),self(),SkillDelivery.SELF,action(2)));
		return b;
	}

	private static ClassBuild bloodBerserker() {
		ClassBuild b=base(Id.BLOOD_BERSERKER);
		basic(b,BasicAttackProfile.FULL);
		SkillSpec cleave=active("blood_cleave",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_MISSING_HP,8),enemy(),SkillDelivery.CONTACT_ATTACK,hp(4));
		cleave.constraint=new SkillConstraint(SkillConstraint.Variant.HP_COMMITMENT,4);cleave.modifier=modifier(RuleModifier.Type.INTENSITY,2);b.skills.add(cleave);
		SkillSpec shell=active("blood_shell",effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_TEMP_HP,10),self(),SkillDelivery.SELF,hp(3));
		shell.primary.duration=4;shell.constraint=new SkillConstraint(SkillConstraint.Variant.SELF_LOW_HP,45);b.skills.add(shell);
		SkillSpec frenzy=react("pain_haste",RuleEvent.ON_DAMAGED,effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_HASTE,3),self(),SkillDelivery.SELF);
		frenzy.primary.duration=3;frenzy.conditions.add(new RuleCondition(RuleCondition.Type.SELF_HP_BELOW,45));b.skills.add(frenzy);
		SkillSpec finish=active("desperate_finish",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_EXECUTE,6),enemy(),SkillDelivery.DIRECT_TARGET,hp(3));
		finish.primary.secondaryParameter=30;finish.constraint=new SkillConstraint(SkillConstraint.Variant.SELF_LOW_HP,35);b.skills.add(finish);
		b.skills.add(react("kill_surge",RuleEvent.ON_KILL,effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_BARRIER,5),self(),SkillDelivery.SELF));
		b.traits.add(TraitSpec.of(CoreRuleVocabulary.PHASE_SHIFT));
		return b;
	}

	private static ClassBuild ammoGunner() {
		ClassBuild b=base(Id.AMMO_GUNNER);ResourceSpec ammo=resource(ResourceEngine.MANUAL,"ammo");
		ammo.name="Ammo";ammo.capacity=6;ammo.initialValue=6;ammo.current=6;b.resources.add(ammo);refill(b,"ammo",0,1f);
		SkillSpec shot=active("piercing_shot",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,8),enemy(),SkillDelivery.PROJECTILE,resource("ammo",ResourceEngine.MANUAL,1));
		shot.targeting.range=8;shot.modifier=modifier(RuleModifier.Type.PIERCE,2);shot.secondary=effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_SLOW,2);shot.secondary.duration=2;b.skills.add(shot);
		SkillSpec burst=active("controlled_burst",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,5),enemy(),SkillDelivery.PROJECTILE,resource("ammo",ResourceEngine.MANUAL,4));
		burst.modifier=modifier(RuleModifier.Type.REPEAT,3);burst.targeting.range=7;burst.constraint=new SkillConstraint(SkillConstraint.Variant.COOLDOWN,3);b.skills.add(burst);
		b.traits.add(TraitSpec.of(CoreRuleVocabulary.KILL_TEMPO));
		return b;
	}

	private static ClassBuild areaCaster() {
		ClassBuild b=base(Id.AREA_CASTER);b.resources.add(resource(ResourceEngine.MANA,"mana"));
		SkillSpec bolt=active("arcane_bolt",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,6),enemy(),SkillDelivery.PROJECTILE,resource("mana",ResourceEngine.MANA,2));bolt.targeting.range=6;b.skills.add(bolt);
		TargetingSpec line=enemy();line.coverage=TargetingSpec.Coverage.LINE;line.maxTargets=5;line.range=6;
		SkillSpec beam=active("searing_line",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,5),line,SkillDelivery.TRACE_BEAM,resource("mana",ResourceEngine.MANA,5));beam.modifier=modifier(RuleModifier.Type.PIERCE,2);b.skills.add(beam);
		TargetingSpec radius=cell();radius.coverage=TargetingSpec.Coverage.RADIUS;radius.magnitude=2;radius.maxTargets=4;
		SkillSpec field=active("binding_field",effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_SLOW,2),radius,SkillDelivery.GROUND_PLACEMENT,resource("mana",ResourceEngine.MANA,4));field.primary.duration=5;b.skills.add(field);
		b.skills.add(active("arcane_ward",effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_BARRIER,4),self(),SkillDelivery.SELF,resource("mana",ResourceEngine.MANA,3)));
		return b;
	}

	private static ClassBuild markAssassin() {
		ClassBuild b=base(Id.MARK_ASSASSIN);
		EffectSpec mark=effect(EffectFamily.MARK_ACCUMULATION,EffectSpec.Operation.MARK_STACK,1);mark.stateId="HUNTED";mark.duration=8;
		SkillSpec tag=active("hunt_mark",mark,enemy(),SkillDelivery.PROJECTILE,cooldown(2));b.skills.add(tag);
		SkillSpec dash=active("shadow_step",effect(EffectFamily.MOVEMENT,EffectSpec.Operation.MOVE_DASH,1),cell(),SkillDelivery.GROUND_PLACEMENT,cooldown(3));b.skills.add(dash);
		SkillSpec finish=active("marked_finish",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_MISSING_HP,3),markedEnemy(),SkillDelivery.CONTACT_ATTACK,hp(2));finish.primary.secondaryParameter=20;
		finish.constraint=new SkillConstraint(SkillConstraint.Variant.TARGET_MARKED,1);b.skills.add(finish);
		SkillSpec execute=active("execute",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_EXECUTE,5),markedEnemy(),SkillDelivery.DIRECT_TARGET,cooldown(5));execute.primary.secondaryParameter=30;execute.constraint=new SkillConstraint(SkillConstraint.Variant.TARGET_MARKED,1);execute.secondary=effect(EffectFamily.MARK_ACCUMULATION,EffectSpec.Operation.MARK_CONSUME,1);execute.secondary.stateId="HUNTED";b.skills.add(execute);
		b.skills.add(active("afterimage",effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_TEMP_HP,6),self(),SkillDelivery.SELF,cooldown(3)));
		return b;
	}

	private static ClassBuild ownedSummoner() {
		ClassBuild b=base(Id.OWNED_SUMMONER);
		ownership(b);capacity(b,ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR,3);command(b);
		EffectSpec summon=effect(EffectFamily.CREATE_ENTITY,EffectSpec.Operation.CREATE_ACTOR,3);summon.count=1;summon.lifetime=8;summon.period=1;
		SkillSpec call=active("call_pack",summon,cell(),SkillDelivery.GROUND_PLACEMENT,action(2));call.constraint=new SkillConstraint(SkillConstraint.Variant.COOLDOWN,9);b.skills.add(call);
		EffectSpec shield=effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_BARRIER,4);
		b.skills.add(active("guard_pack",shield,owned(),SkillDelivery.DIRECT_TARGET,cooldown(3)));
		EffectSpec haste=effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_HASTE,2);haste.duration=5;
		b.skills.add(active("pack_haste",haste,self(),SkillDelivery.SELF,cooldown(4)));
		EffectSpec inherit=effect(EffectFamily.RELATION_CONTROL,EffectSpec.Operation.RELATION_INHERIT,1);inherit.stateId="haste";
		b.skills.add(active("inherit_haste",inherit,owned(),SkillDelivery.DIRECT_TARGET,action(2)));
		return b;
	}

	private static ClassBuild deviceEngineer() {
		ClassBuild b=base(Id.DEVICE_ENGINEER);ResourceSpec fuel=resource(ResourceEngine.MANUAL,"fuel");
		fuel.name="Fuel";fuel.capacity=8;fuel.initialValue=8;fuel.current=8;b.resources.add(fuel);refill(b,"fuel",3,1f);
		ownership(b);capacity(b,ClassGameplayComponentSpec.EntityFilter.OWNED_DEVICE,2);recycle(b,"fuel",2);
		EffectSpec device=effect(EffectFamily.CREATE_ENTITY,EffectSpec.Operation.CREATE_DEVICE,2);device.stateId="fire";device.lifetime=9;device.period=2;
		b.skills.add(active("deploy_device",device,cell(),SkillDelivery.GROUND_PLACEMENT,resource("fuel",ResourceEngine.MANUAL,3)));
		EffectSpec field=effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_SLOW,1);field.lifetime=8;field.period=2;field.duration=4;
		b.skills.add(active("deploy_field",field,cell(),SkillDelivery.PERSISTENT_CARRIER,resource("fuel",ResourceEngine.MANUAL,2)));
		EffectSpec trap=effect(EffectFamily.CREATE_ENTITY,EffectSpec.Operation.CREATE_TRAP,3);trap.stateId="poison";b.skills.add(active("deploy_trap",trap,cell(),SkillDelivery.GROUND_PLACEMENT,resource("fuel",ResourceEngine.MANUAL,2)));
		EffectSpec link=effect(EffectFamily.RELATION_CONTROL,EffectSpec.Operation.RELATION_LINK,1);link.stateId="signal";b.skills.add(active("link_device",link,owned(),SkillDelivery.DIRECT_TARGET,cooldown(3)));
		return b;
	}

	private static ClassBuild terrainController() {
		ClassBuild b=base(Id.TERRAIN_CONTROLLER);b.resources.add(resource(ResourceEngine.FOCUS,"focus"));b.traits.add(TraitSpec.resource(CoreRuleVocabulary.WATER_FLOW,"focus"));
		b.skills.add(active("shape_water",effect(EffectFamily.WORLD_TERRAIN,EffectSpec.Operation.WORLD_WATER,1),cell(),SkillDelivery.GROUND_PLACEMENT,cooldown(3)));
		SkillSpec gas=active("toxic_ground",effect(EffectFamily.WORLD_TERRAIN,EffectSpec.Operation.WORLD_TOXIC_GAS,4),cell(),SkillDelivery.GROUND_PLACEMENT,resource("focus",ResourceEngine.FOCUS,4));gas.targeting.coverage=TargetingSpec.Coverage.RADIUS;gas.targeting.magnitude=1;gas.secondary=effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_POISON,2);gas.secondary.duration=6;gas.constraint=new SkillConstraint(SkillConstraint.Variant.SELF_IN_WATER,0);b.skills.add(gas);
		SkillSpec shove=active("tidal_push",effect(EffectFamily.MOVEMENT,EffectSpec.Operation.MOVE_THROW,6),enemy(),SkillDelivery.DIRECT_TARGET,cooldown(3));shove.constraint=new SkillConstraint(SkillConstraint.Variant.SELF_IN_WATER,0);b.skills.add(shove);
		EffectSpec trap=effect(EffectFamily.CREATE_ENTITY,EffectSpec.Operation.CREATE_TRAP,2);trap.stateId="poison";b.skills.add(active("water_trap",trap,cell(),SkillDelivery.GROUND_PLACEMENT,cooldown(4)));
		SkillSpec waterGuard=react("water_guard",RuleEvent.ON_DAMAGED,effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_BARRIER,3),self(),SkillDelivery.SELF);
		waterGuard.conditions.add(new RuleCondition(RuleCondition.Type.SELF_IN_WATER));b.skills.add(waterGuard);
		return b;
	}

	private static ClassBuild statusController() {
		ClassBuild b=base(Id.STATUS_CONTROLLER);b.resources.add(resource(ResourceEngine.AFFLICTION,"affliction"));b.traits.add(TraitSpec.of(CoreRuleVocabulary.PROPAGATION));b.traits.add(TraitSpec.of(CoreRuleVocabulary.HUNT_MARK));
		EffectSpec embrace=effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_POISON,1);embrace.duration=3;b.skills.add(active("embrace_affliction",embrace,self(),SkillDelivery.SELF,cooldown(5)));
		TargetingSpec chain=enemy();chain.coverage=TargetingSpec.Coverage.CHAIN;chain.magnitude=3;chain.maxTargets=3;
		SkillSpec poison=active("chain_poison",effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_POISON,2),chain,SkillDelivery.DIRECT_TARGET,resource("affliction",ResourceEngine.AFFLICTION,3));poison.primary.duration=6;b.skills.add(poison);
		EffectSpec mark=effect(EffectFamily.MARK_ACCUMULATION,EffectSpec.Operation.MARK_STACK,1);mark.stateId="HUNTED";mark.duration=8;b.skills.add(react("infected_mark",RuleEvent.ON_HIT,mark,enemy(),SkillDelivery.CONTACT_ATTACK));
		SkillSpec spread=active("spread_mark",effect(EffectFamily.MARK_ACCUMULATION,EffectSpec.Operation.MARK_SPREAD,1),markedEnemy(),SkillDelivery.DIRECT_TARGET,cooldown(3));spread.primary.stateId="HUNTED";b.skills.add(spread);
		SkillSpec fear=active("network_terror",effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_TERROR,2),markedEnemy(),SkillDelivery.DIRECT_TARGET,resource("affliction",ResourceEngine.AFFLICTION,3));fear.primary.duration=4;fear.constraint=new SkillConstraint(SkillConstraint.Variant.TARGET_MARKED,1);b.skills.add(fear);
		b.traits.add(TraitSpec.resource(CoreRuleVocabulary.STATUS_FEEDBACK,"affliction"));
		return b;
	}

	private static ClassBuild modeShifter() {
		ClassBuild b=base(Id.MODE_SHIFTER);
		modes(b,"offense","defense");
		SkillSpec assault=active("assault_beam",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,9),enemy(),SkillDelivery.TRACE_BEAM,cooldown(2));assault.conditions.add(mode("offense"));b.skills.add(assault);
		SkillSpec guard=active("guard_shell",effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_TEMP_HP,10),self(),SkillDelivery.SELF,cooldown(2));guard.primary.duration=4;guard.conditions.add(mode("defense"));b.skills.add(guard);
		SkillSpec retreat=active("guard_dash",effect(EffectFamily.MOVEMENT,EffectSpec.Operation.MOVE_DASH,1),cell(),SkillDelivery.GROUND_PLACEMENT,cooldown(3));retreat.conditions.add(mode("defense"));b.skills.add(retreat);
		return b;
	}

	private static ClassBuild reactionOnly() {
		ClassBuild b=base(Id.REACTION_ONLY);
		b.skills.add(react("hurt_barrier",RuleEvent.ON_DAMAGED,effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_BARRIER,4),self(),SkillDelivery.SELF));
		b.skills.add(react("hit_poison",RuleEvent.ON_HIT,effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_POISON,3),enemy(),SkillDelivery.CONTACT_ATTACK));
		b.skills.add(react("kill_heal",RuleEvent.ON_KILL,effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.RECOVER_HEAL,5),self(),SkillDelivery.SELF));
		b.skills.add(react("move_guard",RuleEvent.ON_MOVE,effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_TEMP_HP,3),self(),SkillDelivery.SELF));
		return b;
	}

	private static ClassBuild resourcelessCooldown() {
		ClassBuild b=base(Id.RESOURCELESS_COOLDOWN);
		b.skills.add(active("cooldown_bolt",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,7),enemy(),SkillDelivery.PROJECTILE,cooldown(3)));
		SkillSpec ring=active("control_ring",effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_SLOW,2),enemy(),SkillDelivery.DIRECT_TARGET,cooldown(5));ring.targeting.coverage=TargetingSpec.Coverage.RING;ring.targeting.magnitude=2;ring.targeting.maxTargets=6;b.skills.add(ring);
		SkillSpec limited=active("limited_barrier",effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_BARRIER,8),self(),SkillDelivery.SELF,none());limited.constraint=new SkillConstraint(SkillConstraint.Variant.LIMITED_USE,3);b.skills.add(limited);
		return b;
	}

	private static ClassBuild dualResource() {
		ClassBuild b=base(Id.DUAL_RESOURCE);b.resources.add(resource(ResourceEngine.MOMENTUM,"momentum"));b.resources.add(resource(ResourceEngine.FOCUS,"focus"));
		b.skills.add(active("momentum_dash",effect(EffectFamily.MOVEMENT,EffectSpec.Operation.MOVE_DASH,1),cell(),SkillDelivery.GROUND_PLACEMENT,resource("momentum",ResourceEngine.MOMENTUM,3)));
		b.skills.add(active("focus_beam",effect(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,7),enemy(),SkillDelivery.TRACE_BEAM,resource("focus",ResourceEngine.FOCUS,4)));
		b.skills.add(react("wait_focus_guard",RuleEvent.ON_WAIT,effect(EffectFamily.RECOVERY_DEFENSE,EffectSpec.Operation.DEFENSE_BARRIER,3),self(),SkillDelivery.SELF));
		return b;
	}

	private static ClassBuild transferSupport() {
		ClassBuild b=base(Id.TRANSFER_SUPPORT);
		ownership(b);capacity(b,ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR,2);
		EffectSpec summon=effect(EffectFamily.CREATE_ENTITY,EffectSpec.Operation.CREATE_ACTOR,2);summon.count=1;summon.lifetime=8;b.skills.add(active("support_actor",summon,cell(),SkillDelivery.GROUND_PLACEMENT,action(2)));
		EffectSpec poison=effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_POISON,1);poison.duration=5;b.skills.add(active("seed_status",poison,enemy(),SkillDelivery.DIRECT_TARGET,cooldown(3)));
		b.skills.add(active("copy_status",effect(EffectFamily.TRANSFER_COPY,EffectSpec.Operation.COPY_STATUS,1),enemy(),SkillDelivery.DIRECT_TARGET,cooldown(2)));
		b.skills.add(active("swap_barrier",effect(EffectFamily.TRANSFER_COPY,EffectSpec.Operation.SWAP_BARRIER,1),owned(),SkillDelivery.DIRECT_TARGET,cooldown(3)));
		return b;
	}

	private static ClassBuild pureControl() {
		ClassBuild b=base(Id.PURE_CONTROL);
		TargetingSpec cone=enemy();cone.coverage=TargetingSpec.Coverage.CONE;cone.maxTargets=6;SkillSpec push=active("control_cone",effect(EffectFamily.MOVEMENT,EffectSpec.Operation.MOVE_THROW,5),cone,SkillDelivery.DIRECT_TARGET,cooldown(3));b.skills.add(push);
		TargetingSpec chain=enemy();chain.coverage=TargetingSpec.Coverage.CHAIN;chain.magnitude=3;chain.maxTargets=4;SkillSpec roots=active("root_chain",effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_ROOTS,2),chain,SkillDelivery.DIRECT_TARGET,cooldown(4));roots.primary.duration=4;b.skills.add(roots);
		SkillSpec terror=active("terror_line",effect(EffectFamily.STATUS,EffectSpec.Operation.STATUS_TERROR,2),enemy(),SkillDelivery.TRACE_BEAM,cooldown(5));terror.targeting.coverage=TargetingSpec.Coverage.LINE;terror.targeting.maxTargets=5;terror.primary.duration=4;b.skills.add(terror);
		return b;
	}

	private static ClassBuild base(Id id) { ClassBuild b=new ClassBuild();b.name=id.name().toLowerCase();b.baseBudget=ClassBudgetPolicy.newBuildBudget();b.startingKit.mode=StartingKitSpec.Mode.UNARMED;basic(b,BasicAttackProfile.WEAK);return b; }
	private static void basic(ClassBuild b,BasicAttackProfile value){for(ClassGameplayComponentSpec c:b.gameplayComponents)if(c.type==ClassGameplayComponentSpec.Type.BASIC_ATTACK){c.basicAttack=value;return;}b.gameplayComponents.add(ClassGameplayComponentSpec.basicAttack(value));}
	private static void refill(ClassBuild b,String resourceId,int amount,float actionTime){b.gameplayComponents.add(ClassGameplayComponentSpec.activeRefill("refill_"+resourceId,resourceId,amount,actionTime));}
	private static void ownership(ClassBuild b){b.gameplayComponents.add(new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.OWNERSHIP,"ownership"));}
	private static void capacity(ClassBuild b,ClassGameplayComponentSpec.EntityFilter filter,int value){ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.ENTITY_CAPACITY,"capacity_"+filter.name().toLowerCase());c.entityFilter=filter;c.capacity=value;b.gameplayComponents.add(c);}
	private static void command(ClassBuild b){b.gameplayComponents.add(new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.COMMAND,"command"));}
	private static void recycle(ClassBuild b,String resourceId,int amount){ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.RECYCLE,"recycle");c.resourceId=resourceId;c.amount=amount;b.gameplayComponents.add(c);}
	private static void modes(ClassBuild b,String... values){ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.MODE_ENGINE,"mode_engine");c.modes.addAll(Arrays.asList(values));b.gameplayComponents.add(c);}
	private static ResourceSpec resource(ResourceEngine engine,String id){ResourceSpec r=new ResourceSpec(engine);r.id=id;return r;}
	private static EffectSpec effect(EffectFamily family,EffectSpec.Operation operation,int power){return new EffectSpec(family,operation,power);}
	private static SkillSpec active(String id,EffectSpec effect,TargetingSpec target,SkillDelivery delivery,RuleCost cost){return skill(id,RuleEvent.ACTIVE,effect,target,delivery,cost);}
	private static SkillSpec react(String id,RuleEvent event,EffectSpec effect,TargetingSpec target,SkillDelivery delivery){return skill(id,event,effect,target,delivery,none());}
	private static SkillSpec skill(String id,RuleEvent event,EffectSpec effect,TargetingSpec target,SkillDelivery delivery,RuleCost cost){SkillSpec s=new SkillSpec();s.id=id;s.activation=event;s.primary=effect;s.targeting=target;s.delivery=delivery;s.cost=cost;s.conditions.clear();s.conditions.add(new RuleCondition());return s;}
	private static RuleCondition mode(String id){RuleCondition c=new RuleCondition(RuleCondition.Type.MODE_IS);c.reference=id;return c;}
	private static TargetingSpec self(){return new TargetingSpec();}
	private static TargetingSpec enemy(){TargetingSpec t=new TargetingSpec();t.selector=TargetingSpec.Selector.SELECTED_ACTOR;t.filter=TargetingSpec.Filter.ENEMY;return t;}
	private static TargetingSpec markedEnemy(){TargetingSpec t=enemy();t.filter=TargetingSpec.Filter.MARKED;t.filterParameter=1;return t;}
	private static TargetingSpec cell(){TargetingSpec t=new TargetingSpec();t.selector=TargetingSpec.Selector.SELECTED_CELL;t.filter=TargetingSpec.Filter.ANY;return t;}
	private static TargetingSpec owned(){TargetingSpec t=new TargetingSpec();t.selector=TargetingSpec.Selector.ALL_MATCHING;t.filter=TargetingSpec.Filter.OWNED_ENTITY;t.coverage=TargetingSpec.Coverage.SINGLE;t.maxTargets=6;return t;}
	private static RuleCost none(){return new RuleCost(RuleCost.Type.NONE,0);}
	private static RuleCost hp(int amount){return new RuleCost(RuleCost.Type.HP,amount);}
	private static RuleCost action(int amount){return new RuleCost(RuleCost.Type.ACTION,amount);}
	private static RuleCost cooldown(int amount){return new RuleCost(RuleCost.Type.COOLDOWN,amount);}
	private static RuleCost resource(String id,ResourceEngine engine,int amount){RuleCost c=new RuleCost(RuleCost.Type.RESOURCE,amount);c.resourceId=id;c.resourceEngine=engine;return c;}
	private static RuleModifier modifier(RuleModifier.Type type,int magnitude){RuleModifier m=new RuleModifier(type);m.magnitude=magnitude;return m;}
}
