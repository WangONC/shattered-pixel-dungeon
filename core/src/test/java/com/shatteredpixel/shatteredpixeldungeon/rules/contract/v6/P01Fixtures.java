package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.*;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.*;
import java.util.Arrays;import java.util.Collections;import java.util.EnumSet;import java.util.LinkedHashMap;import java.util.Map;

final class P01Fixtures {
	final DeterministicIdGenerator ids=new DeterministicIdGenerator("p01-fixture");
	final ResourceSpec resource=new ResourceSpec(ids.nextId("res"),DisplayName.of("Rage"),0,10,5,ResourceSpec.ResourceOverflowPolicy.FAIL,new ResourceSpec.ResourceHudSpec(true,0,"rage"));
	final MarkSpec mark=new MarkSpec(ids.nextId("mark"),DisplayName.of("灼痕"),MarkSpec.MarkKind.STACK,0,10,0,MarkSpec.MarkDurationPolicy.TURN_BASED,8,MarkSpec.MarkRefreshPolicy.KEEP_LONGER,MarkSpec.MarkOverflowPolicy.CLAMP,MarkSpec.MarkProvenancePolicy.TRACK_LAST_SOURCE);
	final ModeGroupSpec modeGroup=new ModeGroupSpec(ids.nextId("modegrp"),DisplayName.of("姿态"),ModeGroupSpec.ModeGroupPolicy.EXCLUSIVE);
	final ModeSpec mode=new ModeSpec(ids.nextId("mode"),DisplayName.of("防守"),new ModeGroupRef(modeGroup.id(),modeGroup.displayName().text()),true,ModeSpec.ModeDurationPolicy.PERSISTENT,0);
	final EntityCapacitySpec capacity=new EntityCapacitySpec(ids.nextId("capacity"),DisplayName.of("装置容量"),EnumSet.of(EntityType.DEVICE,EntityType.TRAP),2,EntityCapacitySpec.CapacityOverflowPolicy.REJECT_NEW);
	final EntitySpec entity=new EntitySpec(ids.nextId("entity"),DisplayName.of("守卫装置"),EntityType.DEVICE,
			new DeferredEntityFacetSpec(EntityFacetKind.BODY,"DEVICE_BODY_P04",ImplementationState.DEFERRED),
			new DeferredEntityFacetSpec(EntityFacetKind.SPAWN_POLICY,"SPAWN_POLICY_P04",ImplementationState.DEFERRED),
			new DeferredEntityFacetSpec(EntityFacetKind.OWNERSHIP,"OWNERSHIP_P04",ImplementationState.DEFERRED),
			new DeferredEntityFacetSpec(EntityFacetKind.RELATION,"RELATION_P04",ImplementationState.DEFERRED),
			new CapacityRef(capacity.id(),capacity.displayName().text()),
			new DeferredEntityFacetSpec(EntityFacetKind.PERSISTENCE,"PERSISTENCE_P04",ImplementationState.DEFERRED),
			Arrays.asList(new ResourceStorageCapability(Collections.singletonList(new EntityResourceSlotSpec(new ResourceRef(resource.id(),resource.displayName().text()),2,8))),new DeferredEntityCapabilitySpec("PAYLOAD_CAPABILITY_P04",ImplementationState.UNSUPPORTED)),ImplementationState.DECLARED);
	final AbilityPoolSpec abilityPool=new AbilityPoolSpec(ids.nextId("abilitypool"),DisplayName.of("观察池"),3,AbilityPoolSpec.AbilityOverflowPolicy.REJECT_NEW);
	final PropertySpec property=new PropertySpec(ids.nextId("property"),DisplayName.of("炽热"),PropertySpec.PropertyValueKind.ELEMENT,20);
	final SynthesisRecipeSpec recipe=new SynthesisRecipeSpec(ids.nextId("recipe"),DisplayName.of("炽热合成"),Collections.singletonList(new SynthesisRecipeSpec.PropertyInput(new PropertyRef(property.id(),property.displayName().text()),2)),"UNSUPPORTED_P01_OUTPUT",ImplementationState.DECLARED);
	final ContractNodeSpec component=new ContractNodeSpec(ids.nextId("component"),DisplayName.of("声明组件"),ContractNodeSpec.NodeKind.COMPONENT,"P01_DECLARATION_ONLY",ImplementationState.UNSUPPORTED);
	final ContractNodeSpec constraint=new ContractNodeSpec(ids.nextId("constraint"),DisplayName.of("声明限制"),ContractNodeSpec.NodeKind.CONSTRAINT,"P01_DECLARATION_ONLY",ImplementationState.DEFERRED);
	final ContractNodeSpec operation=new ContractNodeSpec(ids.nextId("op"),DisplayName.of("声明操作"),ContractNodeSpec.NodeKind.OPERATION,"P01_DECLARATION_ONLY",ImplementationState.UNSUPPORTED);
	final ContractNodeSpec skill=new ContractNodeSpec(ids.nextId("skill"),DisplayName.of("声明技能"),ContractNodeSpec.NodeKind.SKILL,"P01_DECLARATION_ONLY",ImplementationState.UNSUPPORTED);
	final StableId buildId=ids.nextId("build");

	ClassBuildSpec build(){return ClassBuildSpec.builder(buildId,DisplayName.of("P01 完整样例")).addResource(resource).addMark(mark).addModeGroup(modeGroup).addMode(mode).addCapacity(capacity).addEntity(entity).addAbilityPool(abilityPool).addProperty(property).addRecipe(recipe).addClassComponent(component).addClassConstraint(constraint).addClassOperation(operation).addSkill(skill).budgetMetadata(new BudgetMetadata("p01-unpriced",12)).build();}
	ClassBuildSpec declarationsOnly(){return ClassBuildSpec.builder(buildId,DisplayName.of("P01 引用样例")).addResource(resource).addMark(mark).addModeGroup(modeGroup).addMode(mode).addCapacity(capacity).addEntity(entity).addAbilityPool(abilityPool).addProperty(property).addRecipe(recipe).build();}
	ClassRuntimeState runtime(){Map<StableId,Integer> cooldowns=new LinkedHashMap<>();cooldowns.put(skill.id(),4);Map<StableId,Integer> uses=new LinkedHashMap<>();uses.put(operation.id(),2);return ClassRuntimeState.builder(buildId)
			.addResource(new ResourceState(new ResourceRef(resource.id(),resource.displayName().text()),3,Collections.singletonList(new ResourceState.Reservation(11,1,2)),Collections.singletonList(new ResourceState.Suppression(12,3,"GAIN"))))
			.addMode(new ModeState(new ModeRef(mode.id(),mode.displayName().text()),7,-1)).addMark(new MarkState(new MarkRef(mark.id(),mark.displayName().text()),8,7,4,6,1))
			.addEntity(new EntityInstanceState(21,new EntitySpecRef(entity.id(),entity.displayName().text()),7,7,42,5,1,Collections.singletonList(new OpaqueRuntimeState(ids.nextId("component"),"RESOURCE_STORAGE_STATE_P04",ImplementationState.DEFERRED))))
			.scheduledPayloads(Collections.singletonList(new OpaqueRuntimeState(ids.nextId("effect"),"SCHEDULED_PAYLOAD_P06",ImplementationState.DEFERRED)))
			.attachments(Collections.singletonList(new OpaqueRuntimeState(ids.nextId("effect"),"ACTION_ATTACHMENT_P06",ImplementationState.UNSUPPORTED)))
			.snapshots(Collections.singletonList(new OpaqueRuntimeState(ids.nextId("snapshot"),"SNAPSHOT_P11",ImplementationState.DEFERRED)))
			.learnedAbilities(Collections.singletonList(new OpaqueRuntimeState(ids.nextId("ability"),"LEARNED_ABILITY_P11",ImplementationState.DEFERRED)))
			.cooldowns(cooldowns).usesThisFloor(uses).addProperty(new PropertyInventoryState(new PropertyRef(property.id(),property.displayName().text()),5,Arrays.asList("fixture:a","fixture:b")))
			.nextRuntimeEntityId(22).nextPayloadInstanceId(31).nextEventId(41).build();}
}
