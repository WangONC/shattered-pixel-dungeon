package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.BuilderFormController;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.FormFieldSchema;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.TextFieldSchema;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeGroupSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

import java.util.ArrayList;
import java.util.List;

/** Thin rendering adapter. Form semantics live in BuilderFormController. */
final class WndCreateClassV6ControllerView {
	private WndCreateClassV6ControllerView() {}

	static void show(BuilderFormController controller) {
		showRoot(controller);
	}

	private static void add(Window window) {
		ShatteredPixelDungeon.scene().addToFront(window);
	}

	private static void showRoot(final BuilderFormController controller) {
		final List<Action> actions = new ArrayList<>();
		actions.add(create("+ Resource", "Create a stable Resource declaration", controller, "Resource",
				new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateResource(name);}}));
		actions.add(create("+ Mark", "Create a dynamic Mark declaration", controller, "Mark",
				new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateMark(name);}}));
		actions.add(create("+ Mode Group", "Create a Mode group", controller, "Mode Group",
				new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateModeGroup(name);}}));
		actions.add(new Action("+ Mode", "Choose its group explicitly",
				!controller.state().draft().modeGroups().isEmpty(), "Create a Mode Group first.",
				new Runnable(){@Override public void run(){chooseModeGroup(controller);}}));
		actions.add(create("+ Entity Capacity", "Create an Entity capacity", controller, "Entity Capacity",
				new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateEntityCapacity(name,"DEVICE",1,"REJECT_NEW");}}));
		actions.add(create("+ Entity", "Create a declared Entity shell", controller, "Entity",
				new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateEntity(name,"DEVICE",null);}}));
		actions.add(create("+ Ability Pool", "Create an Ability Pool declaration", controller, "Ability Pool",
				new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateAbilityPool(name);}}));
		actions.add(create("+ Property", "Create a Property declaration", controller, "Property",
				new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateProperty(name);}}));
		actions.add(create("+ Recipe", "Create a deferred Synthesis Recipe shell", controller, "Recipe",
				new NamedCommand(){@Override public BuilderCommand create(String name){return new BuilderCommand.CreateRecipe(name,"P02_DEFERRED_OUTPUT");}}));

		for (final StableTarget target : controller.state().draft().allTargets()) {
			actions.add(new Action(target.displayName().text(), shortId(target.id().value()), true, "",
					new Runnable(){@Override public void run(){showDeclaration(controller,target.id().value());}}));
		}
		actions.add(new Action("Undo", "Undo one reducer edit", controller.state().history().canUndo(),
				"There is no edit to undo.", new Runnable(){@Override public void run(){controller.dispatch(new BuilderCommand.Undo());showRoot(controller);}}));
		actions.add(new Action("Redo", "Redo one reducer edit", controller.state().history().canRedo(),
				"There is no edit to redo.", new Runnable(){@Override public void run(){controller.dispatch(new BuilderCommand.Redo());showRoot(controller);}}));
		actions.add(new Action("Save Draft", "Save canonical draft in this session", true, "",
				new Runnable(){@Override public void run(){controller.dispatch(new BuilderCommand.SaveDraft("ui"));showRoot(controller);}}));
		actions.add(new Action("Load Draft", "Load the explicit UI draft slot", controller.state().savedDrafts().containsKey("ui"),
				"Save the UI draft slot first.", new Runnable(){@Override public void run(){controller.dispatch(new BuilderCommand.LoadDraft("ui"));showRoot(controller);}}));
		actions.add(new Action("Finalize", "Run fail-closed finalization", true, "",
				new Runnable(){@Override public void run(){controller.dispatch(new BuilderCommand.FinalizeBuild());showRoot(controller);}}));
		showActions("Gameplay Components v6", "P02 command-based builder", controller, actions, new Runnable(){@Override public void run(){}});
	}

	private static Action create(String label, String detail, final BuilderFormController controller,
			final String kind, final NamedCommand command) {
		return new Action(label, detail, true, "", new Runnable(){@Override public void run(){name(controller,kind,command);}});
	}

	private static void name(final BuilderFormController controller, final String kind, final NamedCommand command) {
		add(new WndTextInput("Create " + kind, "Enter a display name", "", 24, false, "Create", "Cancel") {
			@Override public void onSelect(boolean positive, String text) {
				if (positive && text != null && !text.trim().isEmpty()) controller.dispatch(command.create(text));
				showRoot(controller);
			}
		});
	}

	private static void chooseModeGroup(final BuilderFormController controller) {
		final List<Action> groups = new ArrayList<>();
		for (final ModeGroupSpec group : controller.state().draft().modeGroups()) {
			groups.add(new Action(group.displayName().text(), shortId(group.id().value()), true, "",
					new Runnable(){@Override public void run(){
						name(controller,"Mode",new NamedCommand(){@Override public BuilderCommand create(String name){
							return new BuilderCommand.CreateMode(name,new ModeGroupRef(group.id(),group.displayName().text()));
						}});
					}}));
		}
		showActions("Choose Mode Group", "This explicit choice becomes the typed reference", controller, groups,
				new Runnable(){@Override public void run(){showRoot(controller);}});
	}

	private static void showDeclaration(final BuilderFormController controller, final String targetId) {
		final BuilderFormController.FormModel form;
		try {
			form = controller.form(targetId);
		} catch (IllegalArgumentException missing) {
			showRoot(controller);
			return;
		}
		final List<Action> actions = new ArrayList<>();
		for (final BuilderFormController.FieldModel field : form.fields()) {
			String label = field.fieldKey();
			String detail = field.currentValue();
			if (field.unresolved()) {
				label += " · UNRESOLVED";
				detail = field.lastKnownDisplayName() + " · #" + field.shortTargetId();
			}
			actions.add(new Action(label, detail, field.enabled(), field.disabledReason(),
					new Runnable(){@Override public void run(){editField(controller,form,field);}}));
		}
		actions.add(new Action("Delete", "References remain UNRESOLVED until explicit rebind", true, "",
				new Runnable(){@Override public void run(){controller.dispatch(new BuilderCommand.DeleteDeclaration(targetId));showRoot(controller);}}));
		showActions(form.title(), form.variantKey() + " · " + shortId(targetId), controller, actions,
				new Runnable(){@Override public void run(){showRoot(controller);}});
	}

	private static void editField(final BuilderFormController controller,
			final BuilderFormController.FormModel form, final BuilderFormController.FieldModel field) {
		if (field.schema().kind() == FormFieldSchema.Kind.TEXT) {
			TextFieldSchema text = (TextFieldSchema) field.schema();
			add(new WndTextInput(field.fieldKey(), "Edit through BuilderFormController", field.currentValue(),
					text.maximumCodePoints(), false, "Save", "Cancel") {
				@Override public void onSelect(boolean positive, String value) {
					if (positive && value != null && !value.equals(field.currentValue())
							&& (!field.schema().required() || !value.trim().isEmpty())) {
						controller.dispatchValue(form.targetId(), field.fieldKey(), value);
					}
					showDeclaration(controller, form.targetId());
				}
			});
			return;
		}

		final List<Action> choices = new ArrayList<>();
		for (final BuilderFormController.Choice choice : field.choices()) {
			choices.add(new Action(choice.label(), choice.detail(), choice.enabled(),
					choice.enabled() ? "" : choice.detail(), new Runnable(){@Override public void run(){
						controller.dispatchValue(form.targetId(), field.fieldKey(), choice.value());
						showDeclaration(controller, form.targetId());
					}}));
		}
		String help = field.unresolved() ? field.detail() : editorName(field.schema().kind()) + " · Current: " + field.currentValue();
		showActions(field.fieldKey(), help, controller, choices,
				new Runnable(){@Override public void run(){showDeclaration(controller,form.targetId());}});
	}

	private static String editorName(FormFieldSchema.Kind kind) {
		if (kind == FormFieldSchema.Kind.NUMBER) return "NumberStepper";
		if (kind == FormFieldSchema.Kind.ENUM) return "Enum selector";
		if (kind == FormFieldSchema.Kind.ENUM_LIST) return "Multi-enum selector";
		if (kind == FormFieldSchema.Kind.BOOLEAN) return "Boolean selector";
		if (kind == FormFieldSchema.Kind.REFERENCE) return "ReferencePicker / RebindReference";
		return kind.name();
	}

	private static void showActions(String title, String explanation, final BuilderFormController controller,
			final List<Action> actions, final Runnable previous) {
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[actions.size()];
		for (int i=0;i<actions.size();i++) {
			Action action=actions.get(i);
			options[i]=new WndBuilderStep.Option(action.label,action.label,action.detail,action.enabled,
					action.enabled?null:action.disabledReason);
		}
		add(new WndBuilderStep(title, explanation, status(controller.state()), diagnostics(controller.state()),
				controller.state().commandDiagnostics().isEmpty(), options, actions.isEmpty()?-1:0, "Back", "Open") {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) { if (actions.get(index).enabled) actions.get(index).run.run(); }
			@Override protected void onPrevious() { previous.run(); }
		});
	}

	private static String status(BuilderState state) {
		return state.draft().allTargets().size()+" declarations · budget "+state.budget().spent()+"/"+state.budget().limit();
	}

	private static String diagnostics(BuilderState state) {
		if(!state.commandDiagnostics().isEmpty())return state.commandDiagnostics().get(0);
		if(state.finalization()!=null&&!state.finalization().allowed())return state.finalization().diagnostics().toString();
		return state.dependencies().aggregateState().name();
	}

	private static String shortId(String id) { return id.length()<=8?id:id.substring(id.length()-8); }
	private interface NamedCommand { BuilderCommand create(String name); }
	private static final class Action {
		final String label, detail, disabledReason;
		final boolean enabled;
		final Runnable run;
		Action(String label,String detail,boolean enabled,String disabledReason,Runnable run) {
			this.label=label;this.detail=detail;this.enabled=enabled;this.disabledReason=disabledReason;this.run=run;
		}
	}
}
