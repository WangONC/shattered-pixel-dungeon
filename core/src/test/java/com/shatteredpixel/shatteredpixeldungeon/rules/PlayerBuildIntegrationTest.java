package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleTemporaryHP;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RuleOwnedEntity;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.qa.LawTraitVocabularyAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.PlayerBuildEquivalenceAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.GameplayComponentCoverageAudit;
import com.shatteredpixel.shatteredpixeldungeon.qa.PlayerArchetypeReconstructionAudit;
import com.shatteredpixel.shatteredpixeldungeon.ui.ClassActionBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.ClassResourceHUD;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndCreateClass;
import com.watabou.utils.Bundle;
import com.watabou.noosa.Game;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class PlayerBuildIntegrationTest {
	private static HeadlessApplication app;
	@BeforeClass public static void language(){Game.version="3.3.8-INDEV-player-builder-test";Game.versionCode=896;app=new HeadlessApplication(new ApplicationAdapter(){},new HeadlessApplicationConfiguration());Messages.setup(Languages.ENGLISH);}
	@AfterClass public static void stopHeadless(){if(app!=null)app.exit();}

	@Test public void oneBudgetAuthorityUpgradesLegacySixteen(){ClassBuild build=new ClassBuild();assertEquals(35,build.maxBudget());Bundle bundle=new Bundle();build.storeInBundle(bundle);bundle.put("class_build_schema_version",1);bundle.put("base_budget",16);ClassBuild restored=new ClassBuild();restored.restoreFromBundle(bundle);assertEquals(ClassBudgetPolicy.newBuildBudget(),restored.baseBudget);assertEquals(35,restored.maxBudget());}

	@Test public void validationReportsSeveralConcreteFields(){ClassBuild build=new ClassBuild();build.name="validation";build.baseBudget=1;SkillSpec skill=new SkillSpec();skill.primary=new EffectSpec(EffectFamily.MOVEMENT,EffectSpec.Operation.MOVE_PUSH,50);skill.delivery=SkillDelivery.SELF;skill.targeting=new TargetingSpec();skill.cost=new RuleCost(RuleCost.Type.RESOURCE,3);skill.cost.resourceId="ammo";ArrayList<PlayerFacingValidationIssue> issues=PlayerFacingBuildValidator.skillIssues(build,skill,-1);String codes=codes(issues);assertTrue(codes,has(issues,PlayerFacingValidationIssue.Code.INCOMPATIBLE_TARGET));assertTrue(codes,has(issues,PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY));assertTrue(codes,has(issues,PlayerFacingValidationIssue.Code.SKILL_BUDGET_OVERFLOW));assertFalse(PlayerFacingBuildValidator.summary(issues,true).isEmpty());}

	@Test public void pendingResourceCostCanBeSavedThenAutoBound(){ClassBuild build=new ClassBuild();build.name="pending";SkillSpec skill=new SkillSpec();skill.id="pending_cost";skill.primary=new EffectSpec(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,2);skill.delivery=SkillDelivery.DIRECT_TARGET;skill.targeting.selector=TargetingSpec.Selector.SELECTED_ACTOR;skill.targeting.filter=TargetingSpec.Filter.ENEMY;skill.cost=new RuleCost(RuleCost.Type.RESOURCE,2);skill.cost.resourceEngine=ResourceEngine.MANUAL;ArrayList<PlayerFacingValidationIssue> pending=PlayerFacingBuildValidator.skillIssues(build,skill,-1);assertFalse(pending.isEmpty());for(PlayerFacingValidationIssue issue:pending)assertEquals(PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY,issue.code);build.skills.add(skill);build.resources.add(PlayerBuildAssembler.preset(ResourceEngine.MANUAL,"ammo","Ammo"));build.resolvePendingBindings();assertEquals("ammo",skill.cost.resourceId);assertTrue(PlayerFacingBuildValidator.skillIssues(build,skill,0).isEmpty());}

	@Test public void formalVocabularyAndPlayerEquivalenceContractsPass(){LawTraitVocabularyAudit.Result vocabulary=LawTraitVocabularyAudit.run();assertTrue(vocabulary.failures.toString(),vocabulary.passed);assertEquals(5,vocabulary.laws);assertEquals(20,vocabulary.traits);assertEquals(0,vocabulary.neverTriggered);PlayerBuildEquivalenceAudit.Result equivalence=PlayerBuildEquivalenceAudit.run();assertTrue(equivalence.results.toString(),equivalence.passed);assertEquals(0,equivalence.qaBuildNotPlayerConstructible);}

	@Test public void statusAndMovementResourcesUseParameterizedTraitBindings(){ClassBuild build=new ClassBuild();build.name="trait_runtime";ResourceSpec mana=new ResourceSpec(ResourceEngine.MANA);mana.id="mana";build.resources.add(mana);build.skills.add(new SkillSpec());build.traits.add(TraitSpec.resource(CoreRuleVocabulary.STATUS_FEEDBACK,"mana"));build.traits.add(TraitSpec.resource(CoreRuleVocabulary.MOBILE_CHARGE,"mana"));Hero hero=new Hero();hero.HT=hero.HP=20;RuleRuntime runtime=new RuleRuntime(build);hero.setRuleRuntime(runtime);runtime.setResourceForDebug(hero,"mana",ResourceEngine.MANA,0);RuleContext status=new RuleContext(RuleEvent.ON_STATUS_APPLIED,hero);status.status=new Poison();runtime.dispatch(status);assertEquals(2,runtime.resourceValue("mana",ResourceEngine.MANA));for(int i=0;i<3;i++)runtime.dispatch(new RuleContext(RuleEvent.ON_MOVE,hero));assertEquals(3,runtime.resourceValue("mana",ResourceEngine.MANA));}

	@Test public void temporaryHealthCanPayOnlyWithItsTrait(){ClassBuild build=new ClassBuild();build.name="temp_cost";build.skills.add(new SkillSpec());build.traits.add(TraitSpec.of(CoreRuleVocabulary.TEMP_HP_PAYMENT));Hero hero=new Hero();hero.HT=hero.HP=20;RuleRuntime runtime=new RuleRuntime(build);hero.setRuleRuntime(runtime);RuleTemporaryHP temp=com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(hero,RuleTemporaryHP.class);temp.grant(5,5,0);RuleCost cost=new RuleCost(RuleCost.Type.HP,3);assertTrue(cost.canPay(runtime,hero));cost.pay(runtime,hero);assertEquals(20,hero.HP);assertEquals(2,temp.shielding());}

	@Test public void editableResourceFlowExecutesAndRoundtrips(){ClassBuild build=new ClassBuild();build.name="custom_economy";ResourceSpec pool=PlayerBuildAssembler.preset(ResourceEngine.MANUAL,"drive","Drive");pool.capacity=8;pool.initialValue=1;build.resources.add(pool);build.gameplayComponents.add(flow("hit_drive","drive",ResourceFlowSpec.Trigger.HIT,ResourceFlowSpec.Operation.GAIN,2,1,0));build.gameplayComponents.add(flow("wait_drive","drive",ResourceFlowSpec.Trigger.WAIT,ResourceFlowSpec.Operation.CLEAR,0,1,0));Hero hero=new Hero();hero.HT=hero.HP=20;RuleRuntime runtime=new RuleRuntime(build);hero.setRuleRuntime(runtime);RuleContext hit=new RuleContext(RuleEvent.ON_HIT,hero);hit.melee=true;runtime.dispatch(hit);assertEquals(3,runtime.resourceValue("drive",ResourceEngine.MANUAL));Bundle saved=new Bundle();runtime.storeInBundle(saved);RuleRuntime restored=new RuleRuntime();restored.restoreFromBundle(saved);assertEquals(3,restored.resourceValue("drive",ResourceEngine.MANUAL));assertEquals(2,countFlows(restored.presentationBuild(),"drive"));restored.dispatch(new RuleContext(RuleEvent.ON_WAIT,hero));assertEquals(0,restored.resourceValue("drive",ResourceEngine.MANUAL));}

	@Test public void quickResourcePresetsExpandIntoEditablePoolsAndComponents(){
		assertEquals(6,ResourceRegistry.presets().size());
		for(ResourceRegistry.Preset preset:ResourceRegistry.presets()){
			ResourceRegistry.Recipe recipe=preset.createRecipe("pool_"+preset.idBase());ResourceSpec value=recipe.pool;
			assertTrue(preset.name(),value.valid());
			value.name="Edited";value.capacity=Math.max(2,value.capacity);value.initialValue=1;
			recipe.components.add(flow("kill_"+value.id,value.id,ResourceFlowSpec.Trigger.KILL,ResourceFlowSpec.Operation.GAIN,1,1,0));
			assertTrue(preset.name()+" remains editable",value.valid());
			Bundle saved=new Bundle();value.storeInBundle(saved);ResourceSpec restored=new ResourceSpec();restored.restoreFromBundle(saved);
			assertEquals("Edited",restored.displayName());assertEquals(value.capacity,restored.capacity);
			for(ClassGameplayComponentSpec component:recipe.components){Bundle componentBundle=new Bundle();component.storeInBundle(componentBundle);ClassGameplayComponentSpec restoredComponent=new ClassGameplayComponentSpec();restoredComponent.restoreFromBundle(componentBundle);assertEquals(component.type,restoredComponent.type);assertEquals(value.id,restoredComponent.resourceId);}
		}
		ResourceSpec legacyBlood=new ResourceSpec(ResourceEngine.BLOOD);Bundle legacy=new Bundle();legacyBlood.storeInBundle(legacy);
		ResourceSpec restoredLegacy=new ResourceSpec();restoredLegacy.restoreFromBundle(legacy);
		assertEquals(ResourceEngine.MANUAL,restoredLegacy.engine);assertTrue(restoredLegacy.valid());
	}

	@Test public void resourceIdsSurviveRenameAndDeletionLeavesExplicitUnresolvedReferences(){
		ClassBuild build=new ClassBuild();build.name="resource_refs";
		for(String id:new String[]{"ammo","heat","focus","rage"})build.resources.add(pool(id,id.toUpperCase(),10,5));
		SkillSpec skill=new SkillSpec();skill.id="multi_pool_skill";skill.activation=RuleEvent.ACTIVE;
		skill.primary=new EffectSpec(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,4);
		skill.primary.scalingSource=EffectSpec.ScalingSource.CURRENT_RESOURCE;skill.primary.resourceId="rage";
		skill.secondary=new EffectSpec(EffectFamily.RESOURCE_OPERATION,EffectSpec.Operation.RESOURCE_GAIN,2);skill.secondary.resourceId="heat";
		skill.delivery=SkillDelivery.DIRECT_TARGET;skill.targeting.selector=TargetingSpec.Selector.SELECTED_ACTOR;skill.targeting.filter=TargetingSpec.Filter.ENEMY;
		skill.cost=new RuleCost(RuleCost.Type.RESOURCE,1);skill.cost.resourceId="ammo";
		RuleCondition focus=new RuleCondition(RuleCondition.Type.RESOURCE_AT_LEAST,3);focus.reference="focus";skill.conditions.clear();skill.conditions.add(focus);build.skills.add(skill);
		ClassGameplayComponentSpec conversion=flow("focus_to_heat","focus",ResourceFlowSpec.Trigger.WAIT,ResourceFlowSpec.Operation.CONVERT,2,1,0);conversion.targetResourceId="heat";conversion.targetAmount=1;build.gameplayComponents.add(conversion);
		assertTrue(build.renameResource("ammo","弹药"));assertEquals("ammo",skill.cost.resourceId);assertEquals("弹药",build.resource("ammo").displayName());
		assertEquals("rage",skill.primary.resourceId);assertEquals("heat",skill.secondary.resourceId);assertEquals("focus",focus.reference);
		assertTrue(build.removeResource("focus"));assertEquals("focus",focus.reference);assertEquals("focus",conversion.resourceId);
		build.resolvePendingBindings();assertEquals("focus",focus.reference);assertEquals(ComponentDependency.State.UNRESOLVED,conversion.dependency(build).state);
		assertTrue(has(PlayerFacingBuildValidator.classIssues(build),PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY));
	}

	@Test public void playerPresetPathCreatesOnlyGenericPoolAndEditableComponents(){
		PlayerBuildAssembler assembler=new PlayerBuildAssembler("preset_path").addResourcePreset(ResourceRegistry.Preset.RAGE,"drive");
		ClassBuild build=assembler.build();assertTrue(assembler.failures().toString(),assembler.failures().isEmpty());
		assertEquals(ResourceEngine.MANUAL,build.resource("drive").engine);assertTrue(countFlows(build,"drive")>=3);
		ClassGameplayComponentSpec added=flow("custom_wait","drive",ResourceFlowSpec.Trigger.WAIT,ResourceFlowSpec.Operation.GAIN,3,1,0);
		assertTrue(GameplayComponentRegistry.playerExposed(added));build.gameplayComponents.add(added);assertNotNull(build.gameplayComponent("custom_wait"));
	}

	@Test public void everyClassComponentRoundtripsAsIndependentBudgetedObject(){
		ClassBuild build=new ClassBuild();build.name="component_roundtrip";build.resources.add(pool("a","A",10,5));build.resources.add(pool("b","B",10,0));
		ArrayList<ClassGameplayComponentSpec> values=new ArrayList<>();values.add(ClassGameplayComponentSpec.basicAttack(BasicAttackProfile.FULL));
		values.add(flow("gain","a",ResourceFlowSpec.Trigger.DAMAGED,ResourceFlowSpec.Operation.GAIN,2,1,0));
		ClassGameplayComponentSpec convert=flow("convert","a",ResourceFlowSpec.Trigger.WAIT,ResourceFlowSpec.Operation.CONVERT,2,1,0);convert.targetResourceId="b";convert.targetAmount=1;values.add(convert);
		values.add(ClassGameplayComponentSpec.activeRefill("refill","a",0,2f));
		values.add(new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.OWNERSHIP,"owner"));
		for(ClassGameplayComponentSpec.Type type:new ClassGameplayComponentSpec.Type[]{ClassGameplayComponentSpec.Type.ENTITY_CAPACITY,ClassGameplayComponentSpec.Type.PERSISTENCE,ClassGameplayComponentSpec.Type.COMMAND,ClassGameplayComponentSpec.Type.RECYCLE}){ClassGameplayComponentSpec c=new ClassGameplayComponentSpec(type,type.name().toLowerCase());c.entityFilter=ClassGameplayComponentSpec.EntityFilter.OWNED_ACTOR;c.capacity=2;c.lifetime=7;c.resourceId="a";c.amount=2;c.actionTime=2f;values.add(c);}
		ClassGameplayComponentSpec modes=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.MODE_ENGINE,"modes");modes.modes.add("assault");modes.modes.add("guard");modes.actionTime=2f;values.add(modes);
		ClassGameplayComponentSpec limit=new ClassGameplayComponentSpec(ClassGameplayComponentSpec.Type.GLOBAL_CONSTRAINT,"limit");limit.restriction=Restriction.NO_TRADITIONAL_HEALING;values.add(limit);
		for(ClassGameplayComponentSpec value:values){Bundle saved=new Bundle();value.storeInBundle(saved);ClassGameplayComponentSpec restored=new ClassGameplayComponentSpec();restored.restoreFromBundle(saved);assertEquals(value.type,restored.type);assertEquals(value.id,restored.id);assertEquals(value.budgetCost(build),restored.budgetCost(build));assertTrue(GameplayComponentRegistry.playerExposed(restored));}
	}

	@Test public void playerCanSpendMarkAccumulationAndChargeStacks(){
		ClassBuild build=new ClassBuild();build.name="state_costs";
		for(RuleMark.Type expected:new RuleMark.Type[]{RuleMark.Type.HUNTED,RuleMark.Type.ACCUMULATION,RuleMark.Type.CHARGED}){
			boolean found=false;
			for(CostRegistry.Entry entry:CostRegistry.exposed(build))if(entry.cost.type==RuleCost.Type.STATE
					&&expected.name().equals(entry.cost.reference)&&entry.cost.amount==2){
				found=true;assertTrue(entry.cost.referenceValid());assertFalse(entry.name(build).isEmpty());break;
			}
			assertTrue("missing player state cost "+expected,found);
		}
	}

	@Test public void ownedEntityRemovalCanBeRecycledWithoutPretendingItIsAKill(){ClassBuild build=new ClassBuild();build.name="recycler";ResourceSpec pool=PlayerBuildAssembler.preset(ResourceEngine.MANUAL,"scrap","Scrap");pool.capacity=8;pool.initialValue=0;build.resources.add(pool);build.gameplayComponents.add(flow("scrap_removed","scrap",ResourceFlowSpec.Trigger.OWNED_ENTITY_REMOVED,ResourceFlowSpec.Operation.GAIN,2,1,0));Hero hero=new Hero();hero.HT=hero.HP=20;RuleRuntime runtime=new RuleRuntime(build);hero.setRuleRuntime(runtime);RuleOwnedEntity entity=new RuleOwnedEntity().configure(RuleOwnedEntity.Kind.DEVICE,hero,1,2,1,1,null);runtime.onOwnedEntityRemoved(hero,entity);assertEquals(2,runtime.resourceValue("scrap",ResourceEngine.MANUAL));}

	@Test public void resourceEventIntervalSurvivesSaveLoad(){ClassBuild build=new ClassBuild();build.name="paced_hits";ResourceSpec pool=PlayerBuildAssembler.preset(ResourceEngine.MANUAL,"charge","Charge");pool.capacity=8;pool.initialValue=0;build.resources.add(pool);build.gameplayComponents.add(flow("paced_hits","charge",ResourceFlowSpec.Trigger.HIT,ResourceFlowSpec.Operation.GAIN,1,3,0));Hero hero=new Hero();hero.HT=hero.HP=20;RuleRuntime runtime=new RuleRuntime(build);hero.setRuleRuntime(runtime);RuleContext hit=new RuleContext(RuleEvent.ON_HIT,hero);hit.melee=true;runtime.dispatch(hit);runtime.dispatch(hit);assertEquals(0,runtime.resourceValue("charge",ResourceEngine.MANUAL));Bundle saved=new Bundle();runtime.storeInBundle(saved);RuleRuntime restored=new RuleRuntime();restored.restoreFromBundle(saved);restored.dispatch(hit);assertEquals(1,restored.resourceValue("charge",ResourceEngine.MANUAL));}

	@Test public void unresolvedDependenciesRemainSelectableAndShareValidation(){ClassBuild empty=new ClassBuild();ComponentDependency dependency=LawTraitRegistry.lawDependency(empty,ClassLaw.FORCED_MOVEMENT_COUNTS_AS_MOVE);assertEquals(ComponentDependency.State.UNRESOLVED,dependency.state);assertTrue(dependency.playerSelectable());for(LawTraitRegistry.TraitOption option:LawTraitRegistry.traitOptions(empty))if(option.dependency.state==ComponentDependency.State.UNRESOLVED)assertTrue(option.playerSelectable());ClassBuild overdraft=new ClassBuild();overdraft.name="overdraft";overdraft.resources.add(PlayerBuildAssembler.preset(ResourceEngine.MANUAL,"ammo","Ammo"));overdraft.laws.add(ClassLaw.RESOURCE_OVERDRAFT_USES_HP);assertEquals(ComponentDependency.State.UNRESOLVED,LawTraitRegistry.lawDependency(overdraft,ClassLaw.RESOURCE_OVERDRAFT_USES_HP).state);assertTrue(has(PlayerFacingBuildValidator.classIssues(overdraft),PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY));}

	@Test public void componentCoverageAndPlayerReconstructionPass(){GameplayComponentCoverageAudit.Result coverage=GameplayComponentCoverageAudit.run();assertTrue(coverage.failures.toString(),coverage.passed);assertEquals(0,coverage.discussedComponentNotPlayerExposed);PlayerArchetypeReconstructionAudit.Result reconstruction=PlayerArchetypeReconstructionAudit.run();assertTrue(reconstruction.builds.toString(),reconstruction.passed);assertEquals(10,reconstruction.archetypes);assertEquals(0,reconstruction.qaBuildNotPlayerConstructible);assertEquals(0,reconstruction.dependencyMissingButDisabled);assertEquals(0,reconstruction.dependencyCopyValidationMismatch);assertEquals(0,reconstruction.runtimeFailures);}

	@Test public void gunnerPlayerPathProducesReloadActionHudAndSavedNames(){
		ResourceSpec ammo=PlayerBuildAssembler.preset(ResourceEngine.MANUAL,"ammo","弹药");ammo.capacity=6;ammo.initialValue=6;
		SkillSpec shot=new SkillSpec();shot.id="piercing_shot";shot.name="穿甲射击";shot.activation=RuleEvent.ACTIVE;
		shot.primary=new EffectSpec(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,6);shot.delivery=SkillDelivery.PROJECTILE;
		shot.targeting.selector=TargetingSpec.Selector.SELECTED_ACTOR;shot.targeting.filter=TargetingSpec.Filter.ENEMY;shot.targeting.range=6;
		shot.modifier=new RuleModifier(RuleModifier.Type.PIERCE);shot.cost=new RuleCost(RuleCost.Type.RESOURCE,1);shot.cost.resourceId="ammo";shot.cost.resourceEngine=ResourceEngine.MANUAL;
		PlayerBuildAssembler assembler=new PlayerBuildAssembler("火线游击").baseBudget(ClassBudgetPolicy.newBuildBudget())
				.basicAttack(BasicAttackProfile.WEAK).addResource(ammo).activeRefill("ammo",0,1f).addSkill(shot);
		ClassBuild build=assembler.build();assertTrue(assembler.failures().toString(),assembler.failures().isEmpty());
		assertNotNull(build.operation("reload_ammo"));assertNull(findSkill(build,"reload"));
		ArrayList<String> actions=ClassActionBar.actionIds(build);assertTrue(actions.contains("skill:piercing_shot"));assertTrue(actions.contains("operation:reload_ammo"));
		ArrayList<String> labels=ClassActionBar.actionLabels(build);assertTrue(labels.contains("穿甲射击"));assertTrue(labels.contains("Reload"));
		Hero hero=new Hero();hero.HT=hero.HP=20;RuleRuntime runtime=new RuleRuntime(build);hero.setRuleRuntime(runtime);runtime.setResourceForDebug(hero,"ammo",ResourceEngine.MANUAL,2);
		assertTrue(ClassResourceHUD.readout(runtime),ClassResourceHUD.readout(runtime).contains("弹药 2/6"));
		assertTrue(ClassOperationRuntime.execute(hero,runtime.classOperation("reload_ammo"),0));assertEquals(6,runtime.resourceValue("ammo",ResourceEngine.MANUAL));
		Bundle saved=new Bundle();build.storeInBundle(saved);ClassBuild restored=new ClassBuild();restored.restoreFromBundle(saved);
		assertEquals("弹药",restored.resource("ammo").displayName());assertEquals("穿甲射击",restored.skills.get(0).name);assertNotNull(restored.operation("reload_ammo"));
	}

	@Test public void commandAndModeAreClassOperationsNotRequiredSkills(){
		ClassBuild summon=new PlayerBuildAssembler("caller").ownership(true).entityCapacity(3).command(true).build();
		assertNotNull(summon.operation("command"));assertNull(findSkill(summon,"command"));
		ClassBuild modes=new PlayerBuildAssembler("shifter").modeEngine("offense","defense").build();
		assertNotNull(modes.operation("mode_switch"));assertNull(findSkill(modes,"mode_switch"));
		Hero hero=new Hero();hero.HT=hero.HP=20;RuleRuntime runtime=new RuleRuntime(modes);hero.setRuleRuntime(runtime);
		assertTrue(ClassOperationRuntime.execute(hero,runtime.classOperation("mode_switch"),0));
		assertEquals("offense",hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMode.class).modeId());
	}

	@Test public void classActionBarIsSafeDuringConstructionAndForEmptyOrSparseBuilds(){
		Hero previous= Dungeon.hero;
		try {
			Dungeon.hero=null;
			ClassActionBar noHero=new ClassActionBar();
			assertTrue("a hidden action bar must remain active so GameScene can perform its first rebuild",noHero.active);
			noHero.update();

			Dungeon.hero=new Hero();
			ClassActionBar vanilla=new ClassActionBar();
			vanilla.update();

			ClassBuild sparse=new ClassBuild();sparse.name="sparse";
			sparse.skills.add(null);sparse.operations.add(null);
			Hero custom=new Hero();custom.HT=custom.HP=20;custom.setRuleRuntime(new RuleRuntime(sparse));
			Dungeon.hero=custom;
			ClassActionBar customBar=new ClassActionBar();
			customBar.update();
			assertTrue(ClassActionBar.actionIds(custom.ruleRuntime().classBuild()).isEmpty());
			assertTrue(ClassActionBar.actionLabels(custom.ruleRuntime().classBuild()).isEmpty());
		} finally { Dungeon.hero=previous; }
	}

	@Test public void builderHotfixMessagesResolveEveryFormatPlaceholder(){
		for(Languages language:new Languages[]{Languages.ENGLISH,Languages.CHI_SMPL}){
			Messages.setup(language);
			assertNoFormatPlaceholder(Messages.get(WndCreateClass.class,"section_synergies_count",1,2,3));
			assertNoFormatPlaceholder(Messages.get(WndCreateClass.class,"basic_attack_option","Full",4,"Keeps normal attacks."));
			assertNoFormatPlaceholder(Messages.get(WndCreateClass.class,"generated_operation","Reload","Spend one turn to refill."));
			assertNoFormatPlaceholder(Messages.get(WndCreateClass.class,"gameplay_param_convert",2,"Ammo","Focus",1,1));
			assertNoFormatPlaceholder(Messages.get(WndCreateClass.class,"gameplay_param_general",3,4,5));
		}
		Messages.setup(Languages.ENGLISH);
	}

	@Test public void fullBasicAttackCostTracksIndependentOffense(){
		ClassBuild martial=new ClassBuild();martial.name="martial";martial.gameplayComponents.clear();martial.gameplayComponents.add(ClassGameplayComponentSpec.basicAttack(BasicAttackProfile.FULL));
		assertEquals(0,martial.basicAttackProfile().budgetCost(martial));
		ClassBuild ranged=new ClassBuild();ranged.name="hybrid";ranged.gameplayComponents.clear();ranged.gameplayComponents.add(ClassGameplayComponentSpec.basicAttack(BasicAttackProfile.FULL));
		SkillSpec shot=new SkillSpec();shot.id="shot";shot.primary=new EffectSpec(EffectFamily.DAMAGE,EffectSpec.Operation.DAMAGE_STANDARD,4);shot.delivery=SkillDelivery.PROJECTILE;shot.targeting.selector=TargetingSpec.Selector.SELECTED_ACTOR;shot.targeting.filter=TargetingSpec.Filter.ENEMY;ranged.skills.add(shot);
		assertTrue(ranged.basicAttackProfile().budgetCost(ranged)>=3);ranged.gameplayComponents.clear();ranged.gameplayComponents.add(ClassGameplayComponentSpec.basicAttack(BasicAttackProfile.WEAK));assertEquals(0,ranged.basicAttackProfile().budgetCost(ranged));
		Hero vanilla=new Hero();assertEquals(1f,new RuleRuntime(martial).basicAttackDamageMultiplier(),0f);assertEquals(1f,vanilla.ruleRuntime()==null?1f:vanilla.ruleRuntime().basicAttackDamageMultiplier(),0f);
	}

	private static ClassGameplayComponentSpec flow(String id,String resourceId,ResourceFlowSpec.Trigger trigger,ResourceFlowSpec.Operation operation,int amount,int interval,int delay){return ClassGameplayComponentSpec.resourceFlow(id,resourceId,trigger,operation,amount,interval,delay);}
	private static ResourceSpec pool(String id,String name,int maximum,int initial){ResourceSpec value=new ResourceSpec();value.id=id;value.name=name;value.minimum=0;value.capacity=maximum;value.initialValue=initial;value.current=initial;return value;}
	private static int countFlows(ClassBuild build,String resourceId){int count=0;for(ClassGameplayComponentSpec component:build.gameplayComponents)if(component.type==ClassGameplayComponentSpec.Type.RESOURCE_FLOW&&resourceId.equals(component.resourceId))count++;return count;}

	private static boolean has(ArrayList<PlayerFacingValidationIssue> issues,PlayerFacingValidationIssue.Code code){for(PlayerFacingValidationIssue issue:issues)if(issue.code==code)return true;return false;}
	private static void assertNoFormatPlaceholder(String text){assertFalse(text,text.matches("(?s).*%(?:\\d+\\$)?[sd].*"));}
	private static String codes(ArrayList<PlayerFacingValidationIssue> issues){StringBuilder out=new StringBuilder();for(PlayerFacingValidationIssue issue:issues)out.append(issue.code).append(':').append(issue.shortMessage).append(';');return out.toString();}
	private static SkillSpec findSkill(ClassBuild build,String token){for(SkillSpec skill:build.skills)if(skill.id!=null&&skill.id.contains(token))return skill;return null;}
}
