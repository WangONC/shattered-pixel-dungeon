package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.FormFieldSchema;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.FormSchema;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.NumberFieldSchema;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.NumberStepper;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.V6FormSchemas;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.RandomIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeGroupSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Real P02 player window foundation. Every button dispatches a BuilderCommand; this adapter never
 * constructs or mutates a declaration object.
 */
public final class WndCreateClassV6 {
	private WndCreateClassV6() {}

	public static void show() { show(PlayerBuildSession.empty(new RandomIdGenerator())); }
	public static void show(PlayerBuildSession session) {
		if (session == null) throw new IllegalArgumentException("player build session is required");
		showRoot(session);
	}

	private static void add(Window window) { ShatteredPixelDungeon.scene().addToFront(window); }

	private static void showRoot(final PlayerBuildSession session) {
		final List<Action> actions = new ArrayList<>();
		actions.add(new Action("+ Resource", "Create a stable Resource declaration", true, new Runnable(){@Override public void run(){name(session,"Resource",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateResource(name);}});}}));
		actions.add(new Action("+ Mark", "Create a dynamic Mark declaration", true, new Runnable(){@Override public void run(){name(session,"Mark",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateMark(name);}});}}));
		actions.add(new Action("+ Mode Group", "Create a Mode group", true, new Runnable(){@Override public void run(){name(session,"Mode Group",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateModeGroup(name);}});}}));
		actions.add(new Action("+ Mode", "Choose its group explicitly", !session.state().draft().modeGroups().isEmpty(), new Runnable(){@Override public void run(){chooseModeGroup(session);}}));
		actions.add(new Action("+ Entity Capacity", "Create an Entity capacity", true, new Runnable(){@Override public void run(){name(session,"Entity Capacity",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateEntityCapacity(name,"DEVICE",1,"REJECT_NEW");}});}}));
		actions.add(new Action("+ Entity", "Create a deferred Entity declaration", true, new Runnable(){@Override public void run(){name(session,"Entity",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateEntity(name,"DEVICE",null);}});}}));
		actions.add(new Action("+ Ability Pool", "Create an Ability Pool declaration", true, new Runnable(){@Override public void run(){name(session,"Ability Pool",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateAbilityPool(name);}});}}));
		actions.add(new Action("+ Property", "Create a Property declaration", true, new Runnable(){@Override public void run(){name(session,"Property",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateProperty(name);}});}}));
		actions.add(new Action("+ Recipe", "Create a deferred Synthesis Recipe", true, new Runnable(){@Override public void run(){name(session,"Recipe",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateRecipe(name,"P02_DEFERRED_OUTPUT");}});}}));
		actions.add(new Action("+ Class Component", "Declaration shell only in P02", true, new Runnable(){@Override public void run(){name(session,"Class Component",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateClassComponent(name,"P02_DEFERRED_COMPONENT");}});}}));
		actions.add(new Action("+ Skill", "Typed Skill editing starts in P03", true, new Runnable(){@Override public void run(){name(session,"Skill",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateSkill(name,"P02_DEFERRED_SKILL");}});}}));
		for(final StableTarget target:session.state().draft().allTargets())actions.add(new Action(target.displayName().text(),shortId(target.id().value()),true,new Runnable(){@Override public void run(){showDeclaration(session,target.id().value());}}));
		actions.add(new Action("Undo", "Undo one reducer edit", session.state().history().canUndo(), new Runnable(){@Override public void run(){session.dispatch(new BuilderCommand.Undo());showRoot(session);}}));
		actions.add(new Action("Redo", "Redo one reducer edit", session.state().history().canRedo(), new Runnable(){@Override public void run(){session.dispatch(new BuilderCommand.Redo());showRoot(session);}}));
		actions.add(new Action("Save Draft", "Save canonical draft in this session", true, new Runnable(){@Override public void run(){session.dispatch(new BuilderCommand.SaveDraft("ui"));showRoot(session);}}));
		actions.add(new Action("Load Draft", "Load the explicit UI draft slot", session.state().savedDrafts().containsKey("ui"), new Runnable(){@Override public void run(){session.dispatch(new BuilderCommand.LoadDraft("ui"));showRoot(session);}}));
		actions.add(new Action("Finalize", "Run fail-closed finalization", true, new Runnable(){@Override public void run(){session.dispatch(new BuilderCommand.FinalizeBuild());showRoot(session);}}));
		showActions("Gameplay Components v6", "P02 command-based builder", session, actions, new Runnable(){@Override public void run(){}});
	}

	private static void name(final PlayerBuildSession session,final String kind,final NamedCommand command){
		add(new WndTextInput("Create "+kind,"Enter a display name","",24,false,"Create","Cancel"){
			@Override public void onSelect(boolean positive,String text){if(positive&&text!=null&&!text.trim().isEmpty())session.dispatch(command.create(text));showRoot(session);}
		});
	}

	private static void chooseModeGroup(final PlayerBuildSession session){final List<Action> groups=new ArrayList<>();for(final ModeGroupSpec group:session.state().draft().modeGroups())groups.add(new Action(group.displayName().text(),shortId(group.id().value()),true,new Runnable(){@Override public void run(){
		name(session,"Mode",new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateMode(name,new ModeGroupRef(group.id(),group.displayName().text()));}});
	}}));showActions("Choose Mode Group","This explicit choice becomes the typed reference",session,groups,new Runnable(){@Override public void run(){showRoot(session);}});}

	private static void showDeclaration(final PlayerBuildSession session,final String targetId){StableTarget target=find(session,targetId);if(target==null){showRoot(session);return;}final String variant=variant(target);final List<Action> actions=new ArrayList<>();
		actions.add(new Action("Rename","Stable ID remains "+shortId(targetId),true,new Runnable(){@Override public void run(){add(new WndTextInput("Rename declaration","Identity does not change",find(session,targetId).displayName().text(),24,false,"Save","Cancel"){@Override public void onSelect(boolean positive,String text){if(positive)session.dispatch(new BuilderCommand.RenameDeclaration(targetId,text));showDeclaration(session,targetId);}});}}));
		if(target instanceof ResourceSpec){ResourceSpec resource=(ResourceSpec)target;addNumber(actions,session,targetId,variant,"minimum",resource.minimum());addNumber(actions,session,targetId,variant,"maximum",resource.maximum());addNumber(actions,session,targetId,variant,"initial_value",resource.initialValue());}
		if(target instanceof com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeSpec)actions.add(new Action("Rebind group","Choose a Mode Group explicitly",true,new Runnable(){@Override public void run(){rebindModeGroup(session,targetId);}}));
		for(FormFieldSchema field:V6FormSchemas.require(variant).fields())actions.add(new Action(field.fieldKey(),field.kind().name(),field.kind()!=FormFieldSchema.Kind.READ_ONLY_DIAGNOSTIC,new Runnable(){@Override public void run(){showDeclaration(session,targetId);}}));
		actions.add(new Action("Delete","References remain unresolved until explicit rebind",true,new Runnable(){@Override public void run(){session.dispatch(new BuilderCommand.DeleteDeclaration(targetId));showRoot(session);}}));
		showActions(target.displayName().text(),variant+" · "+shortId(targetId),session,actions,new Runnable(){@Override public void run(){showRoot(session);}});}

	private static void addNumber(List<Action> actions,final PlayerBuildSession session,final String targetId,final String variant,final String field,final int current){actions.add(new Action(field,Integer.toString(current),true,new Runnable(){@Override public void run(){showNumber(session,targetId,variant,field,current);}}));}
	private static void showNumber(final PlayerBuildSession session,final String targetId,final String variant,final String field,final int current){FormSchema form=V6FormSchemas.require(variant);final NumberStepper stepper=new NumberStepper((NumberFieldSchema)form.requireField(field),current);final List<Action> actions=new ArrayList<>();actions.add(new Action("−",Integer.toString(stepper.decrement()),true,new Runnable(){@Override public void run(){session.dispatch(stepper.command(targetId,variant,-1,1));showDeclaration(session,targetId);}}));actions.add(new Action(Integer.toString(current),"min="+stepper.schema().minimum()+", max="+stepper.schema().maximum(),false,new Runnable(){@Override public void run(){}}));actions.add(new Action("+",Integer.toString(stepper.increment()),true,new Runnable(){@Override public void run(){session.dispatch(stepper.command(targetId,variant,1,1));showDeclaration(session,targetId);}}));showActions(field,"[-] "+current+" [+]",session,actions,new Runnable(){@Override public void run(){showDeclaration(session,targetId);}});}

	private static void rebindModeGroup(final PlayerBuildSession session,final String modeId){final List<Action> actions=new ArrayList<>();for(final ModeGroupSpec group:session.state().draft().modeGroups())actions.add(new Action(group.displayName().text(),shortId(group.id().value()),true,new Runnable(){@Override public void run(){session.dispatch(new BuilderCommand.RebindReference(modeId,"group",new ModeGroupRef(group.id(),group.displayName().text())));showDeclaration(session,modeId);}}));showActions("Rebind Mode Group","No same-name or first-item fallback",session,actions,new Runnable(){@Override public void run(){showDeclaration(session,modeId);}});}

	private static void showActions(String title,String explanation,final PlayerBuildSession session,final List<Action> actions,final Runnable previous){WndBuilderStep.Option[] options=new WndBuilderStep.Option[actions.size()];for(int i=0;i<actions.size();i++){Action action=actions.get(i);options[i]=new WndBuilderStep.Option(action.label,action.label,action.detail,action.enabled,action.enabled?null:"Unavailable until explicitly configured");}add(new WndBuilderStep(title,explanation,status(session.state()),diagnostics(session.state()),session.state().commandDiagnostics().isEmpty(),options,actions.isEmpty()?-1:0,"Back","Open"){@Override protected void onSelect(int index){}@Override protected void onNext(int index){actions.get(index).run.run();}@Override protected void onPrevious(){previous.run();}});}
	private static String status(BuilderState state){return state.draft().allTargets().size()+" declarations · budget "+state.budget().spent()+"/"+state.budget().limit();}
	private static String diagnostics(BuilderState state){if(!state.commandDiagnostics().isEmpty())return state.commandDiagnostics().get(0);if(state.finalization()!=null&&!state.finalization().allowed())return state.finalization().diagnostics().toString();return state.dependencies().aggregateState().name();}
	private static StableTarget find(PlayerBuildSession session,String id){for(StableTarget target:session.state().draft().allTargets())if(target.id().value().equals(id))return target;return null;}
	private static String variant(StableTarget target){if(target instanceof ResourceSpec)return V6FormSchemas.RESOURCE;if(target instanceof com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.MarkSpec)return V6FormSchemas.MARK;if(target instanceof ModeGroupSpec)return V6FormSchemas.MODE_GROUP;if(target instanceof com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeSpec)return V6FormSchemas.MODE;if(target instanceof com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntityCapacitySpec)return V6FormSchemas.ENTITY_CAPACITY;if(target instanceof com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntitySpec)return V6FormSchemas.ENTITY;if(target instanceof com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.AbilityPoolSpec)return V6FormSchemas.ABILITY_POOL;if(target instanceof com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.PropertySpec)return V6FormSchemas.PROPERTY;if(target instanceof com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.SynthesisRecipeSpec)return V6FormSchemas.RECIPE;return V6FormSchemas.CONTRACT_NODE;}
	private static String shortId(String id){return id.length()<=8?id:id.substring(id.length()-8);}
	private interface NamedCommand{BuilderCommand create(String name);}
	private static final class Action{final String label,detail;final boolean enabled;final Runnable run;Action(String label,String detail,boolean enabled,Runnable run){this.label=label;this.detail=detail;this.enabled=enabled;this.run=run;}}
}
