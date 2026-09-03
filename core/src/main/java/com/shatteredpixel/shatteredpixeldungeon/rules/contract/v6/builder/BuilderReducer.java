package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.edit.BuildEditService;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.IdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalLoadResult;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The only state transition function used by P02 player UI, headless assembly, and trace replay. */
public final class BuilderReducer {
	private final IdGenerator ids;
	private final BuilderBudgetPolicy budgetPolicy;
	private final BuilderDraftEditor fields = new BuilderDraftEditor();
	private final CanonicalBuildCodec codec = new CanonicalBuildCodec();

	public BuilderReducer(IdGenerator ids) { this(ids, new BuilderBudgetPolicy.DeclarationOnly()); }
	public BuilderReducer(IdGenerator ids, BuilderBudgetPolicy budgetPolicy) {
		if (ids == null || budgetPolicy == null) throw new IllegalArgumentException("id generator and budget policy are required");
		this.ids = ids; this.budgetPolicy = budgetPolicy;
	}

	public BuilderState initial(ClassBuildSpec draft) {
		if (draft == null) throw new IllegalArgumentException("initial draft is required");
		return refresh(draft, BuilderNavigationState.root(), UndoRedoState.empty(),
				Collections.<String,String>emptyMap(), Collections.<String>emptyList(), null);
	}

	public BuilderState apply(BuilderState state, BuilderCommand command) {
		if (state == null || command == null) throw new IllegalArgumentException("state and command are required");
		try {
			if (command instanceof BuilderCommand.Undo) return undo(state);
			if (command instanceof BuilderCommand.Redo) return redo(state);
			if (command instanceof BuilderCommand.SaveDraft) return save(state, (BuilderCommand.SaveDraft) command);
			if (command instanceof BuilderCommand.LoadDraft) return load(state, (BuilderCommand.LoadDraft) command);
			if (command instanceof BuilderCommand.Navigate) {
				BuilderCommand.Navigate value = (BuilderCommand.Navigate) command;
				return refresh(state.draft(), new BuilderNavigationState(value.route(), value.targetId(), value.fieldKey()),
						state.history(), state.savedDrafts(), Collections.<String>emptyList(), null);
			}
			if (command instanceof BuilderCommand.FinalizeBuild) {
				return refresh(state.draft(), state.navigation(), state.history(), state.savedDrafts(),
						Collections.<String>emptyList(), finalizeBuild(state));
			}
			ClassBuildSpec next = mutate(state.draft(), command);
			return refresh(next, state.navigation(), state.history().push(state.draft()), state.savedDrafts(),
					Collections.<String>emptyList(), null);
		} catch (RuntimeException error) {
			return refresh(state.draft(), state.navigation(), state.history(), state.savedDrafts(),
					Collections.singletonList(command.typeKey() + ": " + safeMessage(error)), null);
		}
	}

	private ClassBuildSpec mutate(ClassBuildSpec build, BuilderCommand command) {
		ClassBuildSpec.Builder out = build.toBuilder();
		if (command instanceof BuilderCommand.CreateResource) {
			BuilderCommand.CreateResource value=(BuilderCommand.CreateResource)command;
			out.addResource(new ResourceSpec(next("res",build),DisplayName.of(value.displayName()),0,10,0,
					ResourceSpec.ResourceOverflowPolicy.FAIL,new ResourceSpec.ResourceHudSpec(true,build.resources().size(),"")));
		} else if (command instanceof BuilderCommand.CreateMark) {
			BuilderCommand.CreateMark value=(BuilderCommand.CreateMark)command;
			out.addMark(new MarkSpec(next("mark",build),DisplayName.of(value.displayName()),MarkSpec.MarkKind.MARK,0,1,0,
					MarkSpec.MarkDurationPolicy.PERMANENT_UNTIL_REMOVED,0,MarkSpec.MarkRefreshPolicy.KEEP_LONGER,
					MarkSpec.MarkOverflowPolicy.CLAMP,MarkSpec.MarkProvenancePolicy.NONE));
		} else if (command instanceof BuilderCommand.CreateModeGroup) {
			BuilderCommand.CreateModeGroup value=(BuilderCommand.CreateModeGroup)command;
			out.addModeGroup(new ModeGroupSpec(next("modegrp",build),DisplayName.of(value.displayName()),ModeGroupSpec.ModeGroupPolicy.EXCLUSIVE));
		} else if (command instanceof BuilderCommand.CreateMode) {
			BuilderCommand.CreateMode value=(BuilderCommand.CreateMode)command; requireKind(value.group(),RefKind.MODE_GROUP);
			out.addMode(new ModeSpec(next("mode",build),DisplayName.of(value.displayName()),
					new ModeGroupRef(value.group().targetId(),value.group().lastKnownDisplayName()),false,ModeSpec.ModeDurationPolicy.PERSISTENT,0));
		} else if (command instanceof BuilderCommand.CreateEntityCapacity) {
			BuilderCommand.CreateEntityCapacity value=(BuilderCommand.CreateEntityCapacity)command;
			out.addCapacity(new EntityCapacitySpec(next("capacity",build),DisplayName.of(value.displayName()),
					entityTypes(value.entityTypes()),value.maximum(),en(EntityCapacitySpec.CapacityOverflowPolicy.class,value.overflowPolicy())));
		} else if (command instanceof BuilderCommand.CreateEntity) {
			BuilderCommand.CreateEntity value=(BuilderCommand.CreateEntity)command;CapacityRef capacity=null;
			if(value.capacity()!=null){requireKind(value.capacity(),RefKind.CAPACITY);capacity=new CapacityRef(value.capacity().targetId(),value.capacity().lastKnownDisplayName());}
			out.addEntity(new EntitySpec(next("entity",build),DisplayName.of(value.displayName()),en(EntityType.class,value.entityType()),capacity,ImplementationState.DEFERRED));
		} else if (command instanceof BuilderCommand.CreateAbilityPool) {
			BuilderCommand.CreateAbilityPool value=(BuilderCommand.CreateAbilityPool)command;
			out.addAbilityPool(new AbilityPoolSpec(next("abilitypool",build),DisplayName.of(value.displayName()),1,AbilityPoolSpec.AbilityOverflowPolicy.REJECT_NEW));
		} else if (command instanceof BuilderCommand.CreateProperty) {
			BuilderCommand.CreateProperty value=(BuilderCommand.CreateProperty)command;
			out.addProperty(new PropertySpec(next("property",build),DisplayName.of(value.displayName()),PropertySpec.PropertyValueKind.MATERIAL,1));
		} else if (command instanceof BuilderCommand.CreateRecipe) {
			BuilderCommand.CreateRecipe value=(BuilderCommand.CreateRecipe)command;
			out.addRecipe(new SynthesisRecipeSpec(next("recipe",build),DisplayName.of(value.displayName()),
					Collections.<SynthesisRecipeSpec.PropertyInput>emptyList(),value.outputVariantKey(),ImplementationState.DEFERRED));
		} else if (command instanceof BuilderCommand.CreateClassComponent) {
			BuilderCommand.CreateClassComponent value=(BuilderCommand.CreateClassComponent)command;
			out.addClassComponent(node(build,"component",value.displayName(),ContractNodeSpec.NodeKind.COMPONENT,value.variantKey()));
		} else if (command instanceof BuilderCommand.CreateClassConstraint) {
			BuilderCommand.CreateClassConstraint value=(BuilderCommand.CreateClassConstraint)command;
			out.addClassConstraint(node(build,"constraint",value.displayName(),ContractNodeSpec.NodeKind.CONSTRAINT,value.variantKey()));
		} else if (command instanceof BuilderCommand.CreateClassOperation) {
			BuilderCommand.CreateClassOperation value=(BuilderCommand.CreateClassOperation)command;
			out.addClassOperation(node(build,"op",value.displayName(),ContractNodeSpec.NodeKind.OPERATION,value.variantKey()));
		} else if (command instanceof BuilderCommand.CreateSkill) {
			BuilderCommand.CreateSkill value=(BuilderCommand.CreateSkill)command;
			out.addSkill(node(build,"skill",value.displayName(),ContractNodeSpec.NodeKind.SKILL,value.variantKey()));
		} else if (command instanceof BuilderCommand.SetFieldValue) {
			BuilderCommand.SetFieldValue value=(BuilderCommand.SetFieldValue)command;
			return fields.setField(build,value.ownerId(),value.variantKey(),value.fieldKey(),value.value());
		} else if (command instanceof BuilderCommand.EditResourceField) {
			BuilderCommand.EditResourceField value=(BuilderCommand.EditResourceField)command;
			return fields.setField(build,value.resourceId(),com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.V6FormSchemas.RESOURCE,value.fieldKey(),value.value());
		} else if (command instanceof BuilderCommand.EditMarkField) {
			BuilderCommand.EditMarkField value=(BuilderCommand.EditMarkField)command;
			return fields.setField(build,value.markId(),com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.V6FormSchemas.MARK,value.fieldKey(),value.value());
		} else if (command instanceof BuilderCommand.SetReference) {
			BuilderCommand.SetReference value=(BuilderCommand.SetReference)command;
			return fields.setReference(build,value.ownerId(),value.variantKey(),value.fieldKey(),value.reference());
		} else if (command instanceof BuilderCommand.RenameDeclaration) {
			BuilderCommand.RenameDeclaration value=(BuilderCommand.RenameDeclaration)command;
			return new BuildEditService().rename(build,StableId.fromStored(value.targetId()),DisplayName.of(value.displayName()));
		} else if (command instanceof BuilderCommand.DeleteDeclaration) {
			BuilderCommand.DeleteDeclaration value=(BuilderCommand.DeleteDeclaration)command;
			BuilderDraftEditor.requireTarget(build,StableId.fromStored(value.targetId()));
			return new BuildEditService().delete(build,StableId.fromStored(value.targetId()));
		} else if (command instanceof BuilderCommand.RebindReference) {
			BuilderCommand.RebindReference value=(BuilderCommand.RebindReference)command;
			StableTarget owner=BuilderDraftEditor.requireTarget(build,StableId.fromStored(value.ownerId()));
			return fields.setReference(build,value.ownerId(),BuilderDraftEditor.variantOf(owner),value.fieldKey(),value.newTarget());
		} else throw new IllegalArgumentException("unsupported P02 builder command " + command.typeKey());
		return out.build();
	}

	private BuilderState undo(BuilderState state){UndoRedoState.UndoResult value=state.history().undo(state.draft());return refresh(value.draft,state.navigation(),value.history,state.savedDrafts(),Collections.<String>emptyList(),null);}
	private BuilderState redo(BuilderState state){UndoRedoState.UndoResult value=state.history().redo(state.draft());return refresh(value.draft,state.navigation(),value.history,state.savedDrafts(),Collections.<String>emptyList(),null);}
	private BuilderState save(BuilderState state,BuilderCommand.SaveDraft command){Map<String,String> slots=new LinkedHashMap<>(state.savedDrafts());slots.put(command.slotKey(),codec.serialize(state.draft()));return refresh(state.draft(),state.navigation(),state.history(),slots,Collections.<String>emptyList(),null);}
	private BuilderState load(BuilderState state,BuilderCommand.LoadDraft command){String raw=state.savedDrafts().get(command.slotKey());if(raw==null)throw new IllegalArgumentException("draft slot not found: "+command.slotKey());CanonicalLoadResult<ClassBuildSpec> loaded=codec.deserialize(raw);if(loaded.value()==null)throw new IllegalArgumentException("draft load failed: "+loaded.diagnostics());return refresh(loaded.value(),state.navigation(),state.history().push(state.draft()),state.savedDrafts(),Collections.<String>emptyList(),null);}

	private BuilderState refresh(ClassBuildSpec draft,BuilderNavigationState navigation,UndoRedoState history,
			Map<String,String> saves,List<String> commandDiagnostics,FinalizationReport finalization){
		DependencyReport dependencies=new DependencyResolver().resolve(draft);
		BuilderValidationReport validation=new BuilderValidationReport(new ClassBuildValidator().validate(draft));
		return new BuilderState(draft,dependencies,validation,budgetPolicy.evaluate(draft),navigation,history,saves,commandDiagnostics,finalization);
	}

	private FinalizationReport finalizeBuild(BuilderState state){List<String> diagnostics=new ArrayList<>();
		for(DependencyDiagnostic diagnostic:state.dependencies().diagnostics())if(diagnostic.state()==DependencyState.UNRESOLVED||diagnostic.state()==DependencyState.HARD_CONFLICT)diagnostics.add("dependency:"+diagnostic.messageKey()+":"+diagnostic.fieldPath());
		for(DependencyDiagnostic diagnostic:state.validation().diagnostics())if(diagnostic.state()==DependencyState.UNRESOLVED||diagnostic.state()==DependencyState.HARD_CONFLICT)diagnostics.add("validation:"+diagnostic.messageKey()+":"+diagnostic.fieldPath());
		for(StableTarget target:state.draft().allTargets()){
			if(target.implementationState()==ImplementationState.PLAYER_EXPOSED)diagnostics.add("runtime.unsupported_player_exposed:"+target.id().value());
			else if(target.implementationState()==ImplementationState.UNSUPPORTED||target.implementationState()==ImplementationState.LEGACY_ONLY)diagnostics.add("runtime.unsupported:"+target.id().value());
		}
		if(state.budget().overBudget())diagnostics.add("budget.over_limit:"+state.budget().spent()+">"+state.budget().limit());
		if(!hasImplementedAction(state.draft()))diagnostics.add("finalization.no_gameplay_action");
		return new FinalizationReport(diagnostics.isEmpty()?state.draft():null,diagnostics);
	}
	private static boolean hasImplementedAction(ClassBuildSpec build){for(ContractNodeSpec value:build.classComponents())if(value.implementationState()==ImplementationState.IMPLEMENTED)return true;for(ContractNodeSpec value:build.classOperations())if(value.implementationState()==ImplementationState.IMPLEMENTED)return true;for(ContractNodeSpec value:build.skills())if(value.implementationState()==ImplementationState.IMPLEMENTED)return true;return false;}
	private ContractNodeSpec node(ClassBuildSpec build,String prefix,String name,ContractNodeSpec.NodeKind kind,String variant){return new ContractNodeSpec(next(prefix,build),DisplayName.of(name),kind,variant,ImplementationState.DEFERRED);}
	private StableId next(String prefix,ClassBuildSpec build){for(int attempt=0;attempt<1024;attempt++){StableId id=ids.nextId(prefix);boolean used=build.buildId().equals(id);for(StableTarget target:build.allTargets())used|=target.id().equals(id);if(!used)return id;}throw new IllegalStateException("id generator did not produce a unique "+prefix+" id");}
	private static void requireKind(TypedRef ref,RefKind expected){if(ref.kind()!=expected)throw new IllegalArgumentException("reference kind mismatch: expected "+expected+" got "+ref.kind());}
	private static EnumSet<EntityType> entityTypes(String raw){EnumSet<EntityType> result=EnumSet.noneOf(EntityType.class);for(String value:raw.split(","))result.add(en(EntityType.class,value.trim()));if(result.isEmpty())throw new IllegalArgumentException("entity types are required");return result;}
	private static <E extends Enum<E>>E en(Class<E> type,String raw){try{return Enum.valueOf(type,raw);}catch(IllegalArgumentException error){throw new IllegalArgumentException("unknown "+type.getSimpleName()+" value "+raw,error);}}
	private static String safeMessage(RuntimeException error){return error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();}
}
