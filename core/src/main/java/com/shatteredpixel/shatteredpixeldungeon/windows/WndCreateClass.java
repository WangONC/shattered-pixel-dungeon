package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleMark;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.qa.BuildAnalysis;
import com.shatteredpixel.shatteredpixeldungeon.qa.RuleBuild;
import com.shatteredpixel.shatteredpixeldungeon.qa.RuleBuildAnalyzer;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ComponentDependency;
import com.shatteredpixel.shatteredpixeldungeon.rules.BasicAttackProfile;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassGameplayComponentSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.ConstraintRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.CostRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.DeliveryRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectVocabularyRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassLaw;
import com.shatteredpixel.shatteredpixeldungeon.rules.CoreRuleVocabulary;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily;
import com.shatteredpixel.shatteredpixeldungeon.rules.LawTraitRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.GameplayComponentRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.ModifierRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerFacingClassBuildFormatter;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerFacingBuildValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerFacingValidationIssue;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerBuildAssembler;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceFlowSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.Restriction;
import com.shatteredpixel.shatteredpixeldungeon.rules.RestrictionRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCondition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillConstraint;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.StartingKitSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingRegistry;
import com.shatteredpixel.shatteredpixeldungeon.rules.TraitSpec;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;

import java.util.ArrayList;

/** Slot-free, editable player build sheet. Skill details use a local editor, not a class wizard. */
public final class WndCreateClass {
	private WndCreateClass() {}

	private enum EntryType {
		NAME, OVERVIEW, CORE_COMPONENTS, SKILLS_SECTION, RULES_SECTION, STARTING_CONFIG,
		ADD_RESOURCE, RESOURCE, ADD_SKILL, SKILL, ADD_LAW, LAW, ADD_TRAIT, TRAIT,
		ADD_RESTRICTION, RESTRICTION, STARTING_KIT, PREVIEW, CREATE
	}

	private static final class Entry {
		final EntryType type;
		final int index;
		final String name;
		final String description;
		final boolean enabled;

		Entry(EntryType type, int index, String name, String description, boolean enabled) {
			this.type = type;
			this.index = index;
			this.name = name;
			this.description = description;
			this.enabled = enabled;
		}
	}

	private static final class State {
		final ClassBuild build;
		State(ClassBuild build) { this.build = build; }
	}

	public static void show() { show(null); }

	public static void show(CustomClassConfig existing) {
		ClassBuild build = existing == null ? new ClassBuild() : existing.toClassBuild().copy();
		if (existing == null) build.name = msg("default_name");
		showSheet(new State(build));
	}

	private static String msg(String key, Object... args) {
		return Messages.get(WndCreateClass.class, key, args);
	}

	private static void add(Window window) { ShatteredPixelDungeon.scene().addToFront(window); }

	private static void showSheet(final State state) {
		state.build.resolvePendingBindings();
		final ArrayList<Entry> entries = new ArrayList<>();
		entries.add(new Entry(EntryType.NAME, -1, msg("sheet_name", state.build.name),
				msg("sheet_name_desc"), true));
		entries.add(new Entry(EntryType.OVERVIEW, -1, msg("gameplay_overview"),
				PlayerFacingClassBuildFormatter.gameplayOverview(state.build), true));
		entries.add(new Entry(EntryType.CORE_COMPONENTS, -1, msg("section_components"), msg("section_components_desc"), true));
		entries.add(new Entry(EntryType.SKILLS_SECTION, -1, msg("section_skills"),
				msg("section_skills_count", state.build.skills.size()), true));
		entries.add(new Entry(EntryType.RULES_SECTION, -1, msg("section_synergies"),
				msg("section_synergies_count", state.build.laws.size(), state.build.traits.size(),
						state.build.allRestrictions().size()), true));
		entries.add(new Entry(EntryType.STARTING_CONFIG, -1, msg("section_starting"),
				msg("starting_kit_entry", state.build.startingKit.displayName()), true));
		entries.add(new Entry(EntryType.PREVIEW, -1, msg("preview_entry"), msg("preview_entry_desc"), true));

		final BuildAnalysis analysis = analyze(state.build);
		final ArrayList<PlayerFacingValidationIssue> buildIssues = PlayerFacingBuildValidator.classIssues(state.build);
		boolean createEnabled = buildIssues.isEmpty();
		entries.add(new Entry(EntryType.CREATE, -1, msg("create"), msg("create_entry_desc"), createEnabled));

		WndBuilderStep.Option[] options = new WndBuilderStep.Option[entries.size()];
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			String reason = null;
			if (!entry.enabled) reason = entry.type == EntryType.CREATE
					? PlayerFacingBuildValidator.summary(buildIssues, false) : msg("disabled_no_more_resources");
			options[i] = option(entry.name, firstSentence(entry.description), entry.description, entry.enabled, reason);
		}
		add(new WndBuilderStep(msg("sheet_title"), msg("sheet_desc"),
				PlayerFacingClassBuildFormatter.builderStatus(state.build),
				buildIssues.isEmpty() ? integrityStatus(analysis)
						: capacityStatus(state.build) + "\n" + PlayerFacingBuildValidator.summary(buildIssues, false), createEnabled,
				options, 0, msg("cancel"), msg("edit_selected")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) { openEntry(state, entries.get(index)); }
			@Override protected void onPrevious() {}
		});
	}

	private static void openEntry(final State state, Entry entry) {
		switch (entry.type) {
		case NAME: showName(state); break;
			case OVERVIEW: showGameplayOverview(state); break;
			case CORE_COMPONENTS: showGameplayComponents(state); break;
			case SKILLS_SECTION: showSkills(state); break;
			case RULES_SECTION: showRulesAndSynergies(state); break;
			case STARTING_CONFIG: showStartingKit(state); break;
			case ADD_RESOURCE: showResourceEditor(state, -1); break;
			case RESOURCE: showResourceEditor(state, entry.index); break;
			case ADD_SKILL: showSkillEditor(state, -1); break;
			case SKILL: showSkillEditor(state, entry.index); break;
			case ADD_LAW: showLawEditor(state, -1); break;
			case LAW: showLawEditor(state, entry.index); break;
			case ADD_TRAIT: showTraitEditor(state, -1); break;
			case TRAIT: showTraitEditor(state, entry.index); break;
			case ADD_RESTRICTION: showRestrictionEditor(state, -1); break;
			case RESTRICTION: showRestrictionEditor(state, entry.index); break;
			case STARTING_KIT: showStartingKit(state); break;
			case PREVIEW: showPreview(state); break;
			case CREATE: start(state.build); break;
		}
	}

	private static void showGameplayOverview(final State state) {
		add(new WndBuilderStep(msg("gameplay_overview"), msg("gameplay_overview_desc"),
				PlayerFacingClassBuildFormatter.gameplayOverview(state.build), capacityStatus(state.build), true,
				new WndBuilderStep.Option[]{option(msg("section_components"), msg("section_components_desc"),
						msg("section_components_desc"), true)}, 0, msg("previous"), msg("edit_selected")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) { showGameplayComponents(state); }
			@Override protected void onPrevious() { showSheet(state); }
		});
	}

	private static void showGameplayComponents(final State state) {
		state.build.syncClassOperations();
		final ArrayList<String> entries = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		entries.add("add");
		options.add(option(msg("add_gameplay_component"), msg("add_gameplay_component_desc"),
				msg("add_gameplay_component_detail"), true));
		for (int i=0;i<state.build.resources.size();i++) {
			ResourceSpec resource=state.build.resources.get(i);entries.add("resource:"+i);
			options.add(option(resource.displayName(),resourceSummary(state.build,resource),resourceSummary(state.build,resource),true));
		}
		for (int i=0;i<state.build.gameplayComponents.size();i++) {
			ClassGameplayComponentSpec component=state.build.gameplayComponents.get(i);entries.add("component:"+i);
			ComponentDependency dependency = component.dependency(state.build);
			String description = component.description(state.build);
			if (dependency.state == ComponentDependency.State.UNRESOLVED) {
				description += "\n\n" + msg("component_unresolved", componentDependencyReason(dependency.code));
			}
			options.add(option(costName(component.displayName(state.build), component.budgetCost(state.build)),
					firstSentence(description), description, true));
		}
		add(new WndBuilderStep(msg("gameplay_components_title"),msg("gameplay_components_desc"),
				PlayerFacingClassBuildFormatter.gameplayOverview(state.build),capacityStatus(state.build),true,
				options.toArray(new WndBuilderStep.Option[0]),0,msg("previous"),msg("edit_selected")){
			@Override protected void onSelect(int selected){}
			@Override protected void onNext(int selected){
				String entry=entries.get(selected);
				if ("add".equals(entry)) showGameplayComponentCategories(state);
				else if(entry.startsWith("resource:"))showResourceEditor(state,Integer.parseInt(entry.substring(9)));
				else showGameplayComponentSheet(state,Integer.parseInt(entry.substring(10)));
			}
			@Override protected void onPrevious(){showSheet(state);}
		});
	}

	private static void showGameplayComponentCategories(final State state) {
		final GameplayComponentRegistry.Category[] values = GameplayComponentRegistry.Category.values();
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.length];
		for (int i=0;i<values.length;i++) options[i] = option(msg("component_category_" + values[i].name().toLowerCase()),
				msg("component_category_" + values[i].name().toLowerCase() + "_desc"),
				msg("component_category_" + values[i].name().toLowerCase() + "_desc"), true);
		add(new WndBuilderStep(msg("add_gameplay_component"), msg("add_gameplay_component_desc"), "",
				capacityStatus(state.build), true, options, 0, msg("previous"), msg("edit_selected")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) { showGameplayComponentTemplates(state, values[selected]); }
			@Override protected void onPrevious() { showGameplayComponents(state); }
		});
	}

	private static void showGameplayComponentTemplates(final State state, final GameplayComponentRegistry.Category category) {
		final ArrayList<GameplayComponentRegistry.Template> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (GameplayComponentRegistry.Template value : GameplayComponentRegistry.templates(category)) {
			if (value == GameplayComponentRegistry.Template.BASIC_ATTACK
					&& state.build.hasGameplayComponent(ClassGameplayComponentSpec.Type.BASIC_ATTACK)) continue;
			values.add(value);
			options.add(option(value.displayName(), value.description(), value.description(), true));
		}
		add(new WndBuilderStep(msg("component_category_" + category.name().toLowerCase()),
				msg("component_category_" + category.name().toLowerCase() + "_desc"), "", capacityStatus(state.build), true,
				options.toArray(new WndBuilderStep.Option[0]), 0, msg("previous"), msg("save")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) {
				if(values.get(selected)==GameplayComponentRegistry.Template.RESOURCE_POOL){showResourceEditor(state,-1);return;}
				ClassGameplayComponentSpec component = GameplayComponentRegistry.create(values.get(selected),
						nextGameplayComponentId(state.build, values.get(selected).name().toLowerCase()));
				state.build.gameplayComponents.add(component);
				state.build.resolvePendingBindings();
				showGameplayComponentSheet(state, state.build.gameplayComponents.size() - 1);
			}
			@Override protected void onPrevious() { showGameplayComponentCategories(state); }
		});
	}

	private static String nextGameplayComponentId(ClassBuild build, String base) {
		String id = base; int suffix = 2;
		while (build.gameplayComponent(id) != null) id = base + "_" + suffix++;
		return id;
	}

	private static void showGameplayComponentSheet(final State state, final int index) {
		if (index < 0 || index >= state.build.gameplayComponents.size()) { showGameplayComponents(state); return; }
		final ClassGameplayComponentSpec component = state.build.gameplayComponents.get(index);
		final ArrayList<String> fields = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		switch (component.type) {
			case BASIC_ATTACK:
				fields.add("basic_attack"); options.add(option(component.basicAttack.displayName(), component.basicAttack.description(), component.basicAttack.description(), true)); break;
			case RESOURCE_FLOW:
			case ACTIVE_REFILL:
				fields.add("resource"); options.add(option(msg("component_resource_binding", resourceName(state.build, component.resourceId)),
						msg("component_resource_binding_desc"), msg("component_resource_binding_desc"), true));
				if (component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW) {
					ResourceFlowSpec previewFlow = component.asResourceFlow();
					fields.add("trigger"); options.add(option(previewFlow.displayName(), previewFlow.description(), previewFlow.description(), true));
					if (component.resourceOperation == ResourceFlowSpec.Operation.LOSE || component.resourceOperation == ResourceFlowSpec.Operation.CLEAR) {
						fields.add("loss_mode"); options.add(option(component.resourceOperation == ResourceFlowSpec.Operation.CLEAR ? msg("component_loss_clear") : msg("component_loss_fixed"), msg("component_loss_mode_desc"), msg("component_loss_mode_desc"), true));
					}
					if (component.trigger == ResourceFlowSpec.Trigger.HIT) {
						fields.add("hit_scope"); options.add(option(component.meleeOnly ? msg("component_hit_melee") : msg("component_hit_any"), msg("component_hit_scope_desc"), msg("component_hit_scope_desc"), true));
					}
					if (component.resourceOperation == ResourceFlowSpec.Operation.CONVERT) {
						fields.add("target_resource"); options.add(option(msg("component_target_resource", resourceName(state.build, component.targetResourceId)),
								msg("component_target_resource_desc"), msg("component_target_resource_desc"), true));
						fields.add("target_amount"); options.add(option(msg("component_target_amount", component.targetAmount), msg("component_target_amount_desc"), msg("component_target_amount_desc"), true));
					}
					fields.add("interval"); options.add(option(msg("resource_flow_interval", component.interval), msg("resource_flow_interval_desc"), msg("resource_flow_interval_desc"), true));
					fields.add("delay"); options.add(option(msg("resource_flow_delay", component.delay), msg("resource_flow_delay_desc"), msg("resource_flow_delay_desc"), true));
				}
				fields.add("amount"); options.add(option(component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL && component.amount <= 0
						? msg("resource_refill_full") : msg("component_amount", component.amount), msg("component_amount_desc"), msg("component_amount_desc"), true));
				if (component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL) {
					fields.add("action_time"); options.add(option(msg("component_action_time", (int)component.actionTime), msg("component_action_time_desc"), msg("component_action_time_desc"), true));
				}
				break;
			case ENTITY_CAPACITY:
			case PERSISTENCE:
			case COMMAND:
			case RECYCLE:
				fields.add("entity_filter"); options.add(option(msg("component_entity_filter", component.entityFilterName()), msg("component_entity_filter_desc"), msg("component_entity_filter_desc"), true));
				if (component.type == ClassGameplayComponentSpec.Type.ENTITY_CAPACITY) { fields.add("capacity"); options.add(option(msg("component_capacity", component.capacity), msg("component_capacity_desc"), msg("component_capacity_desc"), true)); }
				if (component.type == ClassGameplayComponentSpec.Type.PERSISTENCE) { fields.add("lifetime"); options.add(option(msg("component_lifetime", component.lifetime), msg("component_lifetime_desc"), msg("component_lifetime_desc"), true)); }
				if (component.type == ClassGameplayComponentSpec.Type.RECYCLE) {
					fields.add("resource"); options.add(option(msg("component_resource_binding", resourceName(state.build, component.resourceId)), msg("component_resource_binding_desc"), msg("component_resource_binding_desc"), true));
					fields.add("amount"); options.add(option(msg("component_amount", component.amount), msg("component_amount_desc"), msg("component_amount_desc"), true));
				}
				if (component.type == ClassGameplayComponentSpec.Type.COMMAND
						|| component.type == ClassGameplayComponentSpec.Type.RECYCLE) {
					fields.add("action_time"); options.add(option(msg("component_action_time", (int)component.actionTime),
							msg("component_action_time_desc"), msg("component_action_time_desc"), true));
				}
				break;
			case MODE_ENGINE:
				for (int i = 0; i < component.modes.size(); i++) {
					fields.add("mode_" + i); options.add(option(msg("component_mode", i + 1, component.modes.get(i)),
							msg("component_mode_desc"), msg("component_mode_desc"), true));
				}
				fields.add("add_mode"); options.add(option(msg("component_add_mode"), msg("component_add_mode_desc"), msg("component_add_mode_desc"), true));
				if (component.modes.size() > 2) {
					fields.add("remove_mode"); options.add(option(msg("component_remove_mode"), msg("component_remove_mode_desc"), msg("component_remove_mode_desc"), true));
				}
				fields.add("action_time"); options.add(option(msg("component_action_time", (int)component.actionTime),
						msg("component_action_time_desc"), msg("component_action_time_desc"), true));
				break;
			case GLOBAL_CONSTRAINT:
				fields.add("restriction"); options.add(option(component.restriction.displayName(), component.restriction.description(), component.restriction.description(), true)); break;
			default: break;
		}
		fields.add("remove"); options.add(removeOption(msg("remove_gameplay_component_desc")));
		ComponentDependency dependency = component.dependency(state.build);
		String status = dependency.state == ComponentDependency.State.RESOLVED ? capacityStatus(state.build)
				: capacityStatus(state.build) + "\n" + msg("component_unresolved", componentDependencyReason(dependency.code));
		add(new WndBuilderStep(component.displayName(state.build), component.description(state.build), "", status, true,
				options.toArray(new WndBuilderStep.Option[0]), 0, msg("previous"), msg("edit_selected")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) {
				String field = fields.get(selected);
				if ("remove".equals(field)) { state.build.gameplayComponents.remove(index); state.build.syncClassOperations(); showGameplayComponents(state); return; }
				showGameplayComponentField(state, index, field);
			}
			@Override protected void onPrevious() { state.build.syncClassOperations(); showGameplayComponents(state); }
		});
	}

	private static void showGameplayComponentField(final State state, final int index, final String field) {
		final ClassGameplayComponentSpec component = state.build.gameplayComponents.get(index);
		if ("basic_attack".equals(field)) { showBasicAttack(state, index); return; }
		if ("resource".equals(field) || "target_resource".equals(field)) { showGameplayResourceBinding(state, index, "target_resource".equals(field)); return; }
		if ("trigger".equals(field)) { showGameplayTrigger(state, index); return; }
		if ("loss_mode".equals(field)) { component.resourceOperation = component.resourceOperation == ResourceFlowSpec.Operation.CLEAR ? ResourceFlowSpec.Operation.LOSE : ResourceFlowSpec.Operation.CLEAR; if(component.resourceOperation==ResourceFlowSpec.Operation.CLEAR)component.amount=0;else if(component.amount<=0)component.amount=1; showGameplayComponentSheet(state,index); return; }
		if ("hit_scope".equals(field)) { component.meleeOnly=!component.meleeOnly;showGameplayComponentSheet(state,index);return; }
		if ("entity_filter".equals(field)) { showGameplayEntityFilter(state, index); return; }
		if ("restriction".equals(field)) { showGameplayRestriction(state, index); return; }
		if ("add_mode".equals(field)) { showGameplayAddMode(state, index); return; }
		if ("remove_mode".equals(field)) { if (component.modes.size() > 2) component.modes.remove(component.modes.size()-1); showGameplayComponentSheet(state,index); return; }
		if (field.startsWith("mode_")) { showGameplayModeName(state, index, Integer.parseInt(field.substring(5))); return; }
		int min = ("delay".equals(field) || "amount".equals(field) && component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL) ? 0 : 1;
		int max = "capacity".equals(field) ? 8 : "lifetime".equals(field) ? 20 : 10;
		int current = "amount".equals(field) ? component.amount : "target_amount".equals(field) ? component.targetAmount
				: "interval".equals(field) ? component.interval : "delay".equals(field) ? component.delay
				: "capacity".equals(field) ? component.capacity : "lifetime".equals(field) ? component.lifetime : (int)component.actionTime;
		showGameplayComponentNumber(state, index, field, min, max, current);
	}

	private static void showBasicAttack(final State state, final int componentIndex) {
		final BasicAttackProfile[] values = BasicAttackProfile.values(); WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.length];
		for (int i=0;i<values.length;i++) { ClassBuild preview=state.build.copy(); preview.gameplayComponents.get(componentIndex).basicAttack=values[i]; int cost=values[i].budgetCost(preview);
			options[i]=option(msg("basic_attack_option",values[i].displayName(),cost,values[i].description()),values[i].description(),values[i].description(),true); }
		add(new WndBuilderStep(msg("basic_attack_title"),msg("basic_attack_desc"),"",capacityStatus(state.build),true,options,
				state.build.gameplayComponents.get(componentIndex).basicAttack.ordinal(),msg("previous"),msg("save")){
			@Override protected void onSelect(int selected){}
			@Override protected void onNext(int selected){state.build.gameplayComponents.get(componentIndex).basicAttack=values[selected];showGameplayComponentSheet(state,componentIndex);}
			@Override protected void onPrevious(){showGameplayComponentSheet(state,componentIndex);}
		});
	}

	private static void showGameplayResourceBinding(final State state, final int componentIndex, final boolean target) {
		if (state.build.resources.isEmpty()) {
			showResourceEditor(state, -1);
			return;
		}
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[state.build.resources.size()];
		for (int i=0;i<options.length;i++) { ResourceSpec pool=state.build.resources.get(i); options[i]=option(pool.displayName(), resourceSummary(state.build,pool), resourceSummary(state.build,pool), true); }
		add(new WndBuilderStep(target?msg("component_target_resource_title"):msg("component_resource_title"), msg("component_resource_binding_desc"), "", capacityStatus(state.build), true, options, 0, msg("previous"), msg("save")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) { if (target) state.build.gameplayComponents.get(componentIndex).targetResourceId=state.build.resources.get(selected).id; else state.build.gameplayComponents.get(componentIndex).resourceId=state.build.resources.get(selected).id; showGameplayComponentSheet(state,componentIndex); }
			@Override protected void onPrevious() { showGameplayComponentSheet(state,componentIndex); }
		});
	}

	private static void showGameplayTrigger(final State state, final int componentIndex) {
		final ResourceFlowSpec.Trigger[] values=ResourceFlowSpec.Trigger.values(); WndBuilderStep.Option[] options=new WndBuilderStep.Option[values.length];
		for(int i=0;i<values.length;i++){ResourceFlowSpec flow=new ResourceFlowSpec(values[i],state.build.gameplayComponents.get(componentIndex).resourceOperation,1,1,0);options[i]=option(flow.displayName(),flow.description(),flow.description(),true);}
		add(new WndBuilderStep(msg("component_trigger_title"),msg("component_trigger_desc"),"",capacityStatus(state.build),true,options,0,msg("previous"),msg("save")){
			@Override protected void onSelect(int selected){} @Override protected void onNext(int selected){state.build.gameplayComponents.get(componentIndex).trigger=values[selected];showGameplayComponentSheet(state,componentIndex);} @Override protected void onPrevious(){showGameplayComponentSheet(state,componentIndex);}
		});
	}

	private static void showGameplayEntityFilter(final State state, final int componentIndex) {
		final ClassGameplayComponentSpec.EntityFilter[] values=ClassGameplayComponentSpec.EntityFilter.values(); WndBuilderStep.Option[] options=new WndBuilderStep.Option[values.length];
		for(int i=0;i<values.length;i++){ClassGameplayComponentSpec copy=state.build.gameplayComponents.get(componentIndex).copy();copy.entityFilter=values[i];options[i]=option(copy.entityFilterName(),msg("component_entity_filter_desc"),msg("component_entity_filter_desc"),true);}
		add(new WndBuilderStep(msg("component_entity_filter_title"),msg("component_entity_filter_desc"),"",capacityStatus(state.build),true,options,0,msg("previous"),msg("save")){
			@Override protected void onSelect(int selected){} @Override protected void onNext(int selected){state.build.gameplayComponents.get(componentIndex).entityFilter=values[selected];showGameplayComponentSheet(state,componentIndex);} @Override protected void onPrevious(){showGameplayComponentSheet(state,componentIndex);}
		});
	}

	private static void showGameplayRestriction(final State state, final int componentIndex) {
		final ArrayList<Restriction> values=new ArrayList<>(); for(Restriction value:RestrictionRegistry.exposed()) if(value!=Restriction.NONE) values.add(value);
		WndBuilderStep.Option[] options=new WndBuilderStep.Option[values.size()]; for(int i=0;i<values.size();i++)options[i]=option(values.get(i).displayName(),values.get(i).description(),values.get(i).description(),true);
		add(new WndBuilderStep(msg("component_restriction_title"),msg("component_restriction_desc"),"",capacityStatus(state.build),true,options,0,msg("previous"),msg("save")){
			@Override protected void onSelect(int selected){} @Override protected void onNext(int selected){state.build.gameplayComponents.get(componentIndex).restriction=values.get(selected);showGameplayComponentSheet(state,componentIndex);} @Override protected void onPrevious(){showGameplayComponentSheet(state,componentIndex);}
		});
	}

	private static void showGameplayModeName(final State state, final int componentIndex, final int modeIndex) {
		final ClassGameplayComponentSpec component=state.build.gameplayComponents.get(componentIndex);
		add(new WndTextInput(msg("component_mode_title"),msg("component_mode_desc"),component.modes.get(modeIndex),16,false,msg("save"),msg("cancel")){
			@Override public void onSelect(boolean positive,String text){if(positive&&text!=null&&!text.trim().isEmpty())component.modes.set(modeIndex,text.trim());showGameplayComponentSheet(state,componentIndex);}
		});
	}

	private static void showGameplayAddMode(final State state, final int componentIndex) {
		final ClassGameplayComponentSpec component=state.build.gameplayComponents.get(componentIndex);
		add(new WndTextInput(msg("component_add_mode"),msg("component_add_mode_desc"),"",16,false,msg("save"),msg("cancel")){
			@Override public void onSelect(boolean positive,String text){if(positive&&text!=null&&!text.trim().isEmpty()
					&&!component.modes.contains(text.trim()))component.modes.add(text.trim());showGameplayComponentSheet(state,componentIndex);}
		});
	}

	private static void showGameplayComponentNumber(final State state, final int componentIndex, final String field, int min, int max, int current) {
		final ArrayList<Integer> values=new ArrayList<>(); ArrayList<WndBuilderStep.Option> options=new ArrayList<>();
		for(int value=min;value<=max;value++){values.add(value);options.add(option(String.valueOf(value),msg("component_number_desc",value),msg("component_number_desc",value),true));}
		add(new WndBuilderStep(msg("component_number_title"),msg("component_number_desc",current),"",capacityStatus(state.build),true,options.toArray(new WndBuilderStep.Option[0]),Math.max(0,values.indexOf(current)),msg("previous"),msg("save")){
			@Override protected void onSelect(int selected){} @Override protected void onNext(int selected){ClassGameplayComponentSpec c=state.build.gameplayComponents.get(componentIndex);int value=values.get(selected);if("amount".equals(field))c.amount=value;else if("target_amount".equals(field))c.targetAmount=value;else if("interval".equals(field))c.interval=value;else if("delay".equals(field))c.delay=value;else if("capacity".equals(field))c.capacity=value;else if("lifetime".equals(field))c.lifetime=value;else c.actionTime=value;showGameplayComponentSheet(state,componentIndex);} @Override protected void onPrevious(){showGameplayComponentSheet(state,componentIndex);}
		});
	}

	private static void showSkills(final State state) {
		final ArrayList<Entry> entries = new ArrayList<>();
		entries.add(new Entry(EntryType.ADD_SKILL, -1, msg("add_skill"), msg("add_skill_desc"), true));
		for (int i = 0; i < state.build.skills.size(); i++) {
			SkillSpec skill = state.build.skills.get(i);
			entries.add(new Entry(EntryType.SKILL, i, PlayerFacingClassBuildFormatter.skillName(skill),
					PlayerFacingClassBuildFormatter.skillShort(skill, state.build), true));
		}
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[entries.size()];
		for (int i = 0; i < entries.size(); i++) options[i] = option(entries.get(i).name,
				firstSentence(entries.get(i).description), entries.get(i).description, true);
		add(new WndBuilderStep(msg("section_skills"), msg("section_skills_desc"),
				msg("section_skills_count", state.build.skills.size()), capacityStatus(state.build), true,
				options, 0, msg("previous"), msg("edit_selected")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) { openEntry(state, entries.get(selected)); }
			@Override protected void onPrevious() { showGameplayComponents(state); }
		});
	}

	private static void showRulesAndSynergies(final State state) {
		final ArrayList<Entry> entries = new ArrayList<>();
		entries.add(new Entry(EntryType.ADD_LAW, -1, msg("add_law"), msg("add_law_desc"), true));
		for (int i=0;i<state.build.laws.size();i++) { ClassLaw value=state.build.laws.get(i); entries.add(new Entry(EntryType.LAW,i,value.displayName(),value.description(),true)); }
		entries.add(new Entry(EntryType.ADD_TRAIT, -1, msg("add_trait"), msg("add_trait_desc"), true));
		for (int i=0;i<state.build.traits.size();i++) { TraitSpec value=state.build.traits.get(i); entries.add(new Entry(EntryType.TRAIT,i,value.displayName(state.build),value.shortSummary(state.build),true)); }
		WndBuilderStep.Option[] options=new WndBuilderStep.Option[entries.size()];
		for(int i=0;i<entries.size();i++) options[i]=option(entries.get(i).name,firstSentence(entries.get(i).description),entries.get(i).description,true);
		add(new WndBuilderStep(msg("section_synergies"),msg("section_synergies_desc"),
				msg("section_synergies_count",state.build.laws.size(),state.build.traits.size(),state.build.allRestrictions().size()),
				capacityStatus(state.build),true,options,0,msg("previous"),msg("edit_selected")){
			@Override protected void onSelect(int selected){}
			@Override protected void onNext(int selected){openEntry(state,entries.get(selected));}
			@Override protected void onPrevious(){showSheet(state);}
		});
	}

	private static void showName(final State state) {
		add(new WndTextInput(msg("title"), msg("name_desc"), state.build.name, 24, false,
				msg("save"), msg("cancel")) {
			@Override public void onSelect(boolean positive, String text) {
				if (positive && text != null && !text.trim().isEmpty()) state.build.name = text.trim();
				showSheet(state);
			}
		});
	}

	private static void showResourceEditor(final State state, final int editIndex) {
		if (editIndex >= 0) { showResourceSheet(state, editIndex); return; }
		final ArrayList<ResourceRegistry.Preset> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (ResourceRegistry.Preset value : ResourceRegistry.presets()) {
			values.add(value);
			options.add(option(value.displayName(), firstSentence(value.description()), value.description(), true));
		}
		add(new WndBuilderStep(msg("resource_editor_title"), msg("resource_editor_desc"),
				PlayerFacingClassBuildFormatter.builderStatus(state.build), capacityStatus(state.build), state.build.budgetValid(),
				options.toArray(new WndBuilderStep.Option[0]), -1, msg("previous"), msg("save")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) {
				ResourceRegistry.Preset preset = values.get(index);
				ResourceRegistry.Recipe recipe = preset.createRecipe(nextResourceId(state.build, preset.idBase()));
				recipe.addTo(state.build);
				showResourceSheet(state, state.build.resources.size() - 1);
			}
			@Override protected void onPrevious() { showGameplayComponents(state); }
		});
	}

	private static String nextResourceId(ClassBuild build, String base) {
		String root = base == null || base.isEmpty() ? "resource" : base;
		String id = root; int suffix = 2;
		while (build.resource(id) != null) id = root + "_" + suffix++;
		return id;
	}

	private static void showResourceSheet(final State state, final int index) {
		final ResourceSpec resource = state.build.resources.get(index);
		final ArrayList<String> fields = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		fields.add("name"); options.add(option(msg("resource_name", resource.displayName()), msg("resource_name_desc"), msg("resource_name_desc"), true));
		fields.add("preset"); options.add(option(msg("resource_preset_apply"), msg("resource_preset_desc"), msg("resource_preset_desc"), true));
		fields.add("minimum"); options.add(option(msg("resource_minimum", resource.minimum), msg("resource_minimum_desc"), msg("resource_minimum_desc"), true));
		fields.add("capacity"); options.add(option(msg("resource_capacity", resource.capacity), msg("resource_capacity_desc"), msg("resource_capacity_desc"), true));
		fields.add("initial"); options.add(option(msg("resource_initial", resource.initialValue), msg("resource_initial_desc"), msg("resource_initial_desc"), true));
		fields.add("remove"); options.add(removeOption(msg("remove_resource_desc")));
		add(new WndBuilderStep(msg("resource_sheet_title", resource.displayName()), msg("resource_sheet_desc"), resourceSummary(state.build, resource),
				capacityStatus(state.build), state.build.budgetValid(), options.toArray(new WndBuilderStep.Option[0]), 0,
				msg("previous"), msg("edit_selected")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) {
				String field = fields.get(selected);
				if ("name".equals(field)) showResourceName(state, index);
				else if ("preset".equals(field)) showResourcePreset(state, index);
				else if ("minimum".equals(field)) showResourceNumber(state, index, "minimum", 0, Math.max(0, resource.capacity-1), resource.minimum);
				else if ("capacity".equals(field)) showResourceNumber(state, index, "capacity", Math.max(1,resource.minimum+1), 30, resource.capacity);
				else if ("initial".equals(field)) showResourceNumber(state, index, "initial", resource.minimum, resource.capacity, resource.initialValue);
				else { state.build.removeResource(resource.id); showGameplayComponents(state); }
			}
			@Override protected void onPrevious() { showGameplayComponents(state); }
		});
	}

	private static void showResourceName(final State state, final int index) {
		final ResourceSpec resource = state.build.resources.get(index);
		add(new WndTextInput(msg("resource_name_title"), msg("resource_name_desc"), resource.displayName(), 20, false, msg("save"), msg("cancel")) {
			@Override public void onSelect(boolean positive, String text) { if (positive) state.build.renameResource(resource.id, text); showResourceSheet(state, index); }
		});
	}

	private static void showResourcePreset(final State state, final int index) {
		final ResourceSpec resource = state.build.resources.get(index); final java.util.List<ResourceRegistry.Preset> values = ResourceRegistry.presets();
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.size()];
		for (int i = 0; i < values.size(); i++) options[i] = option(values.get(i).displayName(), firstSentence(values.get(i).description()), values.get(i).description(), true);
		add(new WndBuilderStep(msg("resource_preset_title"), msg("resource_preset_desc"), resource.displayName(), capacityStatus(state.build), true,
				options, -1, msg("previous"), msg("save")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) {
				String id = resource.id; String name = resource.name;
				for (int i=state.build.gameplayComponents.size()-1;i>=0;i--) {
					ClassGameplayComponentSpec component=state.build.gameplayComponents.get(i);
					if ((component.type==ClassGameplayComponentSpec.Type.RESOURCE_FLOW||component.type==ClassGameplayComponentSpec.Type.ACTIVE_REFILL)
							&& id.equals(component.resourceId)) state.build.gameplayComponents.remove(i);
				}
				ResourceRegistry.Recipe recipe = values.get(selected).createRecipe(id);
				resource.minimum = recipe.pool.minimum; resource.capacity = recipe.pool.capacity; resource.initialValue = recipe.pool.initialValue;
				resource.current = resource.initialValue;
				resource.name = name == null || name.trim().isEmpty() ? recipe.pool.name : name;
				for (ClassGameplayComponentSpec component : recipe.components) state.build.gameplayComponents.add(component.copy());
				showResourceSheet(state, index);
			}
			@Override protected void onPrevious() { showResourceSheet(state, index); }
		});
	}

	private static void showResourceNumber(final State state, final int resourceIndex, final String field, int min, int max, int current) {
		final ArrayList<Integer> values = new ArrayList<>(); ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (int value = min; value <= max; value++) { values.add(value); options.add(option(String.valueOf(value), msg("resource_number_desc", value), msg("resource_number_desc", value), true)); }
		add(new WndBuilderStep(msg("resource_number_title"), msg("resource_number_desc", current), "", capacityStatus(state.build), true,
				options.toArray(new WndBuilderStep.Option[0]), values.indexOf(current), msg("previous"), msg("save")) {
			@Override protected void onSelect(int selected) {}
			@Override protected void onNext(int selected) { ResourceSpec resource = state.build.resources.get(resourceIndex); if ("minimum".equals(field)) { resource.minimum=values.get(selected); resource.initialValue=Math.max(resource.initialValue,resource.minimum); resource.current=Math.max(resource.current,resource.minimum); } else if ("capacity".equals(field)) { resource.capacity = values.get(selected); resource.initialValue = Math.min(resource.initialValue, resource.capacity); resource.current=Math.min(resource.current,resource.capacity); } else {resource.initialValue = values.get(selected);resource.current=resource.initialValue;} showResourceSheet(state, resourceIndex); }
			@Override protected void onPrevious() { showResourceSheet(state, resourceIndex); }
		});
	}

	private static String resourceSummary(ClassBuild build, ResourceSpec resource) {
		int gains = 0, losses = 0;
		for (ClassGameplayComponentSpec component : build.gameplayComponents) if (resource.id.equals(component.resourceId)) {
			if (component.type == ClassGameplayComponentSpec.Type.ACTIVE_REFILL
					|| component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW && component.resourceOperation == ResourceFlowSpec.Operation.GAIN) gains++;
			else if (component.type == ClassGameplayComponentSpec.Type.RESOURCE_FLOW) losses++;
		}
		return msg("resource_component_summary", resource.initialValue, resource.capacity, gains, losses);
	}

	private static void showLawEditor(final State state, final int editIndex) {
		final ArrayList<ClassLaw> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (ClassLaw value : LawTraitRegistry.laws()) {
			if (state.build.laws.contains(value) && (editIndex < 0 || state.build.laws.get(editIndex) != value)) continue;
			values.add(value);
			ComponentDependency dependency = LawTraitRegistry.lawDependency(state.build, value);
			String unresolved = dependency.state == ComponentDependency.State.UNRESOLVED
					? traitCompatibilityReason(dependency.code) : null;
			String reason = dependency.playerSelectable() ? null : traitCompatibilityReason(dependency.code);
			if (reason == null) reason = lawBudgetReason(state.build, value, editIndex);
			options.add(option(costName(value.displayName(), value.capacityCost), value.summary(),
					value.detail() + (unresolved == null ? "" : "\n\n" + msg("unresolved_dependency", unresolved)), reason == null, reason));
		}
		if (editIndex >= 0) { values.add(null); options.add(removeOption(msg("remove_law_desc"))); }
		showLawOptions(state, editIndex, values, options);
	}

	private static void showLawOptions(final State state, final int editIndex,
			final ArrayList<ClassLaw> values, ArrayList<WndBuilderStep.Option> options) {
		add(new WndBuilderStep(msg("law_editor_title"), msg("law_editor_desc"),
				PlayerFacingClassBuildFormatter.builderStatus(state.build), capacityStatus(state.build), state.build.budgetValid(),
				options.toArray(new WndBuilderStep.Option[0]), -1, msg("previous"), msg("save")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) {
				ClassLaw value = values.get(index);
				if (value == null) state.build.laws.remove(editIndex);
				else if (editIndex < 0) state.build.laws.add(value); else state.build.laws.set(editIndex, value);
				showSheet(state);
			}
			@Override protected void onPrevious() { showSheet(state); }
		});
	}

	private static void showTraitEditor(final State state, final int editIndex) {
		final ArrayList<TraitSpec> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (LawTraitRegistry.TraitOption traitOption : LawTraitRegistry.traitOptions(state.build)) {
			TraitSpec value = traitOption.spec;
			if (traitUsedElsewhere(state.build, value, editIndex)) continue;
			values.add(value);
			String unresolved = traitOption.dependency.state == ComponentDependency.State.UNRESOLVED
					? traitCompatibilityReason(traitOption.issue) : null;
			String reason = traitOption.playerSelectable() ? null : traitCompatibilityReason(traitOption.issue);
			if (reason == null) reason = traitBudgetReason(state.build, value, editIndex);
			options.add(option(costName(value.displayName(state.build), value.budgetCost()), value.shortSummary(state.build),
					value.detail(state.build) + (unresolved == null ? "" : "\n\n" + msg("unresolved_dependency", unresolved)), reason == null, reason));
		}
		if (editIndex >= 0) { values.add(null); options.add(removeOption(msg("remove_trait_desc"))); }
		add(new WndBuilderStep(msg("trait_editor_title"), msg("trait_editor_desc"),
				PlayerFacingClassBuildFormatter.builderStatus(state.build), capacityStatus(state.build), state.build.budgetValid(),
				options.toArray(new WndBuilderStep.Option[0]), -1, msg("previous"), msg("save")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) {
				TraitSpec value = values.get(index);
				if (value == null) state.build.traits.remove(editIndex);
				else if (editIndex < 0) state.build.traits.add(value.copy()); else state.build.traits.set(editIndex, value.copy());
				showSheet(state);
			}
			@Override protected void onPrevious() { showSheet(state); }
		});
	}

	private static void showRestrictionEditor(final State state, final int editIndex) {
		final ArrayList<Restriction> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (Restriction value : RestrictionRegistry.exposed()) {
			if (state.build.restrictions.contains(value)
					&& (editIndex < 0 || state.build.restrictions.get(editIndex) != value)) continue;
			values.add(value);
			options.add(option(rebateName(value.displayName(), value.capacityBonus),
					firstSentence(value.description()), value.description(), true));
		}
		if (editIndex >= 0) { values.add(null); options.add(removeOption(msg("remove_restriction_desc"))); }
		add(new WndBuilderStep(msg("restriction_editor_title"), msg("restriction_editor_desc"),
				PlayerFacingClassBuildFormatter.builderStatus(state.build), capacityStatus(state.build), state.build.budgetValid(),
				options.toArray(new WndBuilderStep.Option[0]), -1, msg("previous"), msg("save")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) {
				Restriction value = values.get(index);
				if (value == null) state.build.restrictions.remove(editIndex);
				else if (editIndex < 0) state.build.restrictions.add(value);
				else state.build.restrictions.set(editIndex, value);
				showSheet(state);
			}
			@Override protected void onPrevious() { showSheet(state); }
		});
	}

	private static void showStartingKit(final State state) {
		final StartingKitSpec.Mode[] values = StartingKitSpec.Mode.values();
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.length];
		for (int i = 0; i < values.length; i++) {
			StartingKitSpec preview = new StartingKitSpec(); preview.mode = values[i];
			options[i] = option(preview.displayName(), firstSentence(preview.description()), preview.description(), true);
		}
		add(new WndBuilderStep(msg("starting_kit_title"), msg("starting_kit_desc"),
				PlayerFacingClassBuildFormatter.builderStatus(state.build), capacityStatus(state.build), state.build.budgetValid(),
				options, state.build.startingKit.mode.ordinal(), msg("previous"), msg("save")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) { state.build.startingKit.mode = values[index]; showSheet(state); }
			@Override protected void onPrevious() { showSheet(state); }
		});
	}

	private static void showSkillEditor(final State state, final int editIndex) {
		final SkillSpec draft = editIndex < 0 ? newSkill(state.build) : state.build.skills.get(editIndex).copy();
		showSkillSheet(state, editIndex, draft);
	}

	private static SkillSpec newSkill(ClassBuild build) {
		SkillSpec skill = new SkillSpec();
		int number = 1;
		while (hasSkillId(build, "skill_" + number)) number++;
		skill.id = "skill_" + number;
		skill.activation = RuleEvent.ACTIVE;
		skill.delivery = SkillDelivery.DIRECT_TARGET;
		skill.targeting = targetingSelectedEnemy();
		skill.cost = new RuleCost(RuleCost.Type.NONE, 0);
		return skill;
	}

	private static boolean hasSkillId(ClassBuild build, String id) {
		for (SkillSpec skill : build.skills) if (id.equals(skill.id)) return true;
		return false;
	}

	private static void showSkillSheet(final State state, final int editIndex, final SkillSpec draft) {
		final ArrayList<String> fields = new ArrayList<>(java.util.Arrays.asList("name", "activation", "condition", "primary",
				"primary_parameters", "secondary", "secondary_parameters", "delivery", "targeting", "modifier", "cost", "constraint"));
		ArrayList<WndBuilderStep.Option> list = new ArrayList<>();
		list.add(option(msg("skill_name_entry", PlayerFacingClassBuildFormatter.skillName(draft)),
				msg("skill_name_entry_desc"), msg("skill_name_entry_detail"), true));
		list.add(option(msg("skill_activation", new com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrigger(draft.activation).description()),
				msg("skill_activation_desc"), msg("skill_activation_desc"), true));
		RuleCondition condition = draft.conditions.isEmpty() ? new RuleCondition() : draft.conditions.get(0);
		list.add(option(msg("skill_condition", conditionName(condition)), msg("skill_condition_desc"), msg("skill_condition_desc"), true));
		list.add(option(msg("skill_primary", draft.primary.displayName()),
				draft.primary.description(), effectGameplayDetail(draft.primary), true));
		list.add(option(msg("skill_primary_parameters", effectParameterLabel(draft.primary)),
				effectParameterGameplay(draft.primary, state.build, draft), effectGameplayDetail(draft.primary), true));
		list.add(option(msg("skill_secondary", draft.secondary == null ? msg("none")
				: draft.secondary.displayName()), msg("skill_secondary_desc"), msg("skill_secondary_desc"), true));
		boolean hasSecondary = draft.secondary != null;
		list.add(option(msg("skill_secondary_parameters", hasSecondary ? effectParameterLabel(draft.secondary) : msg("none")),
				hasSecondary ? effectParameterGameplay(draft.secondary, state.build, draft) : msg("disabled_choose_secondary"),
				hasSecondary ? effectGameplayDetail(draft.secondary) : msg("disabled_choose_secondary"), hasSecondary,
				hasSecondary ? null : msg("disabled_choose_secondary")));
		list.add(option(msg("skill_delivery", draft.delivery.displayName()), firstSentence(draft.delivery.description()), draft.delivery.description(), true));
		if (draft.delivery == SkillDelivery.PERSISTENT_CARRIER) {
			fields.add(8, "carrier_lifetime"); fields.add(9, "carrier_period");
			list.add(option(msg("carrier_lifetime", draft.primary.lifetime), msg("carrier_lifetime_desc", draft.primary.lifetime),
					msg("carrier_lifetime_desc", draft.primary.lifetime), true));
			list.add(option(msg("carrier_period", draft.primary.period), msg("carrier_period_desc", draft.primary.period),
					msg("carrier_period_desc", draft.primary.period), true));
		} else if (draft.delivery == SkillDelivery.ACTION_ATTACHMENT) {
			fields.add(8, "attachment_event"); fields.add(9, "attachment_charges");
			list.add(option(msg("attachment_event", new com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrigger(draft.attachmentEvent).description()),
					msg("attachment_event_desc"), msg("attachment_event_desc"), true));
			list.add(option(msg("attachment_charges", draft.attachmentCharges), msg("attachment_charges_desc", draft.attachmentCharges),
					msg("attachment_charges_desc", draft.attachmentCharges), true));
		}
		list.add(option(msg("skill_targeting", draft.targeting.displayName()), msg("skill_targeting_desc"), msg("skill_targeting_desc"), true));
		list.add(option(msg("skill_modifier", ModifierRegistry.name(draft.modifier.type)), msg("skill_modifier_desc"), msg("skill_modifier_desc"), true));
		list.add(option(msg("skill_cost", CostRegistry.describe(draft.cost, state.build)), msg("skill_cost_desc"), msg("skill_cost_desc"), true));
		list.add(option(msg("skill_constraint", draft.constraint.displayName()), firstSentence(draft.constraint.description()), draft.constraint.description(), true));
		final ArrayList<PlayerFacingValidationIssue> skillIssues = PlayerFacingBuildValidator.skillIssues(state.build, draft, editIndex);
		String saveReason = PlayerFacingBuildValidator.summary(skillIssues, true);
		final boolean draftSaveable = onlyUnresolved(skillIssues);
		fields.add("save");
		list.add(option(msg("save_skill"), PlayerFacingClassBuildFormatter.skillShort(draft, state.build),
				PlayerFacingClassBuildFormatter.skillDetail(draft, state.build), draftSaveable,
				draftSaveable ? null : saveReason));
		if (editIndex >= 0) { fields.add("delete"); list.add(removeOption(msg("delete_skill_desc"))); }
		add(new WndBuilderStep(msg("skill_editor_title"), msg("skill_editor_desc"),
				PlayerFacingClassBuildFormatter.skillName(draft), skillBudgetStatus(state.build, draft, editIndex)
						+ (skillIssues.isEmpty() ? "" : "\n\n" + saveReason), draftSaveable,
				list.toArray(new WndBuilderStep.Option[0]), 0, msg("previous"), msg("edit_selected")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) {
				String field = fields.get(index);
				if ("name".equals(field)) showSkillName(state, editIndex, draft);
				else if ("activation".equals(field)) showActivationPicker(state, editIndex, draft);
				else if ("condition".equals(field)) showConditionPicker(state, editIndex, draft);
				else if ("primary".equals(field)) showEffectFamilyPicker(state, editIndex, draft, false);
				else if ("primary_parameters".equals(field)) showEffectParameterPicker(state, editIndex, draft, false);
				else if ("secondary".equals(field)) showEffectFamilyPicker(state, editIndex, draft, true);
				else if ("secondary_parameters".equals(field)) showEffectParameterPicker(state, editIndex, draft, true);
				else if ("delivery".equals(field)) showDeliveryPicker(state, editIndex, draft);
				else if ("carrier_lifetime".equals(field)) showCarrierNumberPicker(state, editIndex, draft, true);
				else if ("carrier_period".equals(field)) showCarrierNumberPicker(state, editIndex, draft, false);
				else if ("attachment_event".equals(field)) showAttachmentEventPicker(state, editIndex, draft);
				else if ("attachment_charges".equals(field)) showAttachmentChargesPicker(state, editIndex, draft);
				else if ("targeting".equals(field)) showTargetingPicker(state, editIndex, draft);
				else if ("modifier".equals(field)) showModifierPicker(state, editIndex, draft);
				else if ("cost".equals(field)) showCostPicker(state, editIndex, draft);
				else if ("constraint".equals(field)) showConstraintPicker(state, editIndex, draft);
				else if ("save".equals(field)) {
					if (editIndex < 0) state.build.skills.add(draft); else state.build.skills.set(editIndex, draft);
					showSheet(state);
				} else if ("delete".equals(field)) { state.build.skills.remove(editIndex); showSheet(state); }
			}
			@Override protected void onPrevious() { showSheet(state); }
		});
	}

	private static void showSkillName(final State state, final int editIndex, final SkillSpec draft) {
		add(new WndTextInput(msg("skill_name_title"), msg("skill_name_help"),
				draft.name == null || draft.name.isEmpty() ? PlayerFacingClassBuildFormatter.skillName(draft) : draft.name,
				24, false, msg("save"), msg("cancel")) {
			@Override public void onSelect(boolean positive, String text) {
				if (positive && text != null && !text.trim().isEmpty()) draft.name = text.trim();
				showSkillSheet(state, editIndex, draft);
			}
		});
	}

	private static void showCarrierNumberPicker(final State state, final int editIndex, final SkillSpec draft, final boolean lifetime) {
		final int max = lifetime ? 16 : 4; final ArrayList<Integer> values = new ArrayList<>();
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[max];
		for (int value = 1; value <= max; value++) { values.add(value); options[value - 1] = option(String.valueOf(value),
				lifetime ? msg("carrier_lifetime_desc", value) : msg("carrier_period_desc", value),
				lifetime ? msg("carrier_lifetime_desc", value) : msg("carrier_period_desc", value), true); }
		showSkillPicker(state, editIndex, draft, lifetime ? msg("carrier_lifetime_title") : msg("carrier_period_title"),
				lifetime ? msg("carrier_lifetime_help") : msg("carrier_period_help"), options,
				values.indexOf(lifetime ? draft.primary.lifetime : draft.primary.period), new SkillChoice() {
			@Override public void apply(int index) { if (lifetime) draft.primary.lifetime = values.get(index); else draft.primary.period = values.get(index); }
		});
	}

	private static void showAttachmentEventPicker(final State state, final int editIndex, final SkillSpec draft) {
		final java.util.List<RuleEvent> values = DeliveryRegistry.ATTACHMENT_EVENTS;
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.size()];
		for (int i = 0; i < values.size(); i++) { String name = new com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrigger(values.get(i)).description();
			options[i] = option(name, msg("attachment_event_option_desc", name), msg("attachment_event_option_desc", name), true); }
		showSkillPicker(state, editIndex, draft, msg("attachment_event_title"), msg("attachment_event_desc"), options,
				values.indexOf(draft.attachmentEvent), new SkillChoice() { @Override public void apply(int index) { draft.attachmentEvent = values.get(index); } });
	}

	private static void showAttachmentChargesPicker(final State state, final int editIndex, final SkillSpec draft) {
		final ArrayList<Integer> values = new ArrayList<>(); WndBuilderStep.Option[] options = new WndBuilderStep.Option[5];
		for (int value = 1; value <= 5; value++) { values.add(value); options[value - 1] = option(String.valueOf(value),
				msg("attachment_charges_desc", value), msg("attachment_charges_desc", value), true); }
		showSkillPicker(state, editIndex, draft, msg("attachment_charges_title"), msg("attachment_charges_help"), options,
				values.indexOf(draft.attachmentCharges), new SkillChoice() { @Override public void apply(int index) { draft.attachmentCharges = values.get(index); } });
	}

	private static void showEffectParameterPicker(final State state, final int editIndex,
			final SkillSpec draft, final boolean secondary) {
		final EffectSpec current = secondary ? draft.secondary : draft.primary;
		final EffectVocabularyRegistry.EffectEntry registryEntry = EffectVocabularyRegistry.find(current);
		if (current == null || registryEntry == null) { showSkillSheet(state, editIndex, draft); return; }
		final ArrayList<EffectVocabularyRegistry.ParameterOption> values = new ArrayList<>(
				EffectVocabularyRegistry.parameterOptions(current, state.build));
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (EffectVocabularyRegistry.ParameterOption value : values) {
			options.add(option(value.name, value.summary(), value.detail(), true));
		}
		if(options.isEmpty())options.add(option(msg("parameter_unavailable"), msg("parameter_unavailable_desc"),
				msg("parameter_unavailable_desc"), false, parameterDisabledReason(current, state.build)));
		showSkillPicker(state,editIndex,draft,msg("parameters_title"),msg("skill_parameters_desc"),
				options.toArray(new WndBuilderStep.Option[0]),-1,new SkillChoice(){@Override public void apply(int index){
			if(values.isEmpty())return;if(secondary)draft.secondary=values.get(index).spec.copy();else draft.primary=values.get(index).spec.copy();}});
	}

	private static void showActivationPicker(final State state, final int editIndex, final SkillSpec draft) {
		final RuleEvent[] values = RuleEvent.values();
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.length];
		for (int i = 0; i < values.length; i++) {
			String name = new com.shatteredpixel.shatteredpixeldungeon.rules.RuleTrigger(values[i]).description();
			options[i] = option(name, msg("activation_option_desc", name), msg("activation_option_desc", name), true);
		}
		showSkillPicker(state, editIndex, draft, msg("activation_title"), msg("skill_activation_desc"), options,
				draft.activation.ordinal(), new SkillChoice() { @Override public void apply(int index) {
				draft.activation = values[index];
				if (draft.delivery == null || draft.delivery == SkillDelivery.ACTION_ATTACHMENT
						&& draft.activation != RuleEvent.ACTIVE) draft.delivery = SkillDelivery.DIRECT_TARGET;
			} });
	}

	private static void showConditionPicker(final State state, final int editIndex, final SkillSpec draft) {
		final RuleCondition.Type[] values = RuleCondition.Type.values();
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.length];
		for (int i = 0; i < values.length; i++) {
			RuleCondition value = new RuleCondition(values[i]);
			String name = values[i] == RuleCondition.Type.ALWAYS ? msg("no_condition") : value.description();
			options[i] = option(name, msg("condition_option_desc", name), msg("condition_option_desc", name), true);
		}
		showSkillPicker(state, editIndex, draft, msg("condition_title"), msg("skill_condition_desc"), options, -1,
				new SkillChoice() { @Override public void apply(int index) {
				RuleCondition condition = new RuleCondition(values[index]); draft.conditions.clear(); draft.conditions.add(condition);
				if (values[index] == RuleCondition.Type.MODE_IS) showModeConditionPicker(state, editIndex, draft, condition);
				else if (values[index] == RuleCondition.Type.SELF_HP_BELOW || values[index] == RuleCondition.Type.TARGET_HP_BELOW
						|| values[index] == RuleCondition.Type.DISTANCE_AT_LEAST
						|| values[index] == RuleCondition.Type.ADJACENT_ENEMIES_AT_LEAST
						|| values[index] == RuleCondition.Type.RESOURCE_AT_LEAST) showConditionNumberPicker(state, editIndex, draft, condition);
				else showSkillSheet(state, editIndex, draft);
			} }, false);
	}

	private static void showModeConditionPicker(final State state, final int editIndex, final SkillSpec draft, final RuleCondition condition) {
		final ArrayList<String> values = new ArrayList<>();
		for (String mode : state.build.modes()) if (mode != null && !mode.isEmpty() && !values.contains(mode)) values.add(mode);
		for (SkillSpec skill : state.build.skills) for (EffectSpec effect : new EffectSpec[]{skill.primary, skill.secondary})
			if (effect != null && effect.operation == EffectSpec.Operation.TRANSFORM_MODE && !values.contains(effect.stateId)) values.add(effect.stateId);
		for (EffectSpec effect : new EffectSpec[]{draft.primary, draft.secondary})
			if (effect != null && effect.operation == EffectSpec.Operation.TRANSFORM_MODE && !values.contains(effect.stateId)) values.add(effect.stateId);
		if (values.isEmpty()) { condition.reference = ""; showSkillSheet(state, editIndex, draft); return; }
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.size()];
		for (int i = 0; i < values.size(); i++) options[i] = option(modeDisplayName(values.get(i)), msg("condition_mode_desc", modeDisplayName(values.get(i))), msg("condition_mode_desc", modeDisplayName(values.get(i))), true);
		showSkillPicker(state, editIndex, draft, msg("condition_mode_title"), msg("condition_mode_picker_desc"), options,
				values.indexOf(condition.reference), new SkillChoice() { @Override public void apply(int index) { condition.reference = values.get(index); } });
	}

	private static String modeDisplayName(String id) {
		if ("offense".equals(id)) return msg("default_mode_offense");
		if ("defense".equals(id)) return msg("default_mode_defense");
		return id;
	}

	private static void showConditionNumberPicker(final State state, final int editIndex, final SkillSpec draft, final RuleCondition condition) {
		final ArrayList<Integer> values = new ArrayList<>(); ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		int min=1,max=condition.type==RuleCondition.Type.SELF_HP_BELOW||condition.type==RuleCondition.Type.TARGET_HP_BELOW?90:10;
		int step=max==90?5:1;
		for(int value=min;value<=max;value+=step){values.add(value);RuleCondition candidate=new RuleCondition(condition.type,value);options.add(option(candidate.description(),msg("condition_value_desc",candidate.description()),msg("condition_value_desc",candidate.description()),true));}
		final boolean needsResource = condition.type == RuleCondition.Type.RESOURCE_AT_LEAST;
		showSkillPicker(state,editIndex,draft,msg("condition_value_title"),msg("condition_value_picker_desc"),
				options.toArray(new WndBuilderStep.Option[0]),values.indexOf(condition.parameter),new SkillChoice(){@Override public void apply(int index){condition.parameter=values.get(index);if(needsResource)showConditionResourcePicker(state,editIndex,draft,condition);}},!needsResource);
	}

	private static void showConditionResourcePicker(final State state, final int editIndex, final SkillSpec draft, final RuleCondition condition) {
		if (state.build.resources.isEmpty()) { condition.reference=""; showSkillSheet(state,editIndex,draft); return; }
		WndBuilderStep.Option[] options=new WndBuilderStep.Option[state.build.resources.size()];
		for(int i=0;i<options.length;i++){ResourceSpec resource=state.build.resources.get(i);options[i]=option(resource.displayName(),msg("condition_resource_desc",resource.displayName()),msg("condition_resource_desc",resource.displayName()),true);}
		showSkillPicker(state,editIndex,draft,msg("condition_resource_title"),msg("condition_resource_picker_desc"),options,
				resourceIndex(state.build,condition.reference),new SkillChoice(){@Override public void apply(int index){condition.reference=state.build.resources.get(index).id;}});
	}

	private static int resourceIndex(ClassBuild build,String id){for(int i=0;i<build.resources.size();i++)if(build.resources.get(i).id.equals(id))return i;return -1;}

	private static void showEffectFamilyPicker(final State state, final int editIndex,
			final SkillSpec draft, final boolean secondary) {
		final ArrayList<EffectFamily> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		if (secondary) {
			values.add(null);
			options.add(option(msg("none"), msg("no_secondary_desc"), msg("no_secondary_desc"), true));
		}
		for (EffectVocabularyRegistry.FamilyEntry entry : EffectVocabularyRegistry.families()) {
			values.add(entry.family);
			options.add(option(entry.name(), entry.summary(), entry.detail(), true));
		}
		showSkillPicker(state, editIndex, draft,
				secondary ? msg("secondary_family_title") : msg("primary_family_title"),
				msg("effect_family_desc"), options.toArray(new WndBuilderStep.Option[0]), -1,
				new SkillChoice() { @Override public void apply(int index) {
					EffectFamily family = values.get(index);
					if (family == null) {
						draft.secondary = null;
						showSkillSheet(state, editIndex, draft);
					} else showEffectVariantPicker(state, editIndex, draft, secondary, family);
				} }, false);
	}

	private static void showEffectVariantPicker(final State state, final int editIndex,
			final SkillSpec draft, final boolean secondary, final EffectFamily family) {
		final java.util.List<EffectVocabularyRegistry.EffectEntry> values =
				EffectVocabularyRegistry.variants(family);
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.size()];
		for (int i = 0; i < values.size(); i++) {
			EffectVocabularyRegistry.EffectEntry entry = values.get(i);
			EffectSpec candidate = entry.create();
			options[i] = option(costName(entry.name(), candidate.powerCost()), entry.summary(),
					entry.detail(), true);
		}
		add(new WndBuilderStep(msg("effect_variant_title", family.displayName()), msg("effect_variant_desc"),
				PlayerFacingClassBuildFormatter.skillName(draft), skillBudgetStatus(state.build, draft, editIndex), true,
				options, -1, msg("previous"), msg("save")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) {
				EffectSpec selected = values.get(index).create();
				if (secondary) draft.secondary = selected; else draft.primary = selected;
				showSkillSheet(state, editIndex, draft);
			}
			@Override protected void onPrevious() { showEffectFamilyPicker(state, editIndex, draft, secondary); }
		});
	}

	private static void showDeliveryPicker(final State state, final int editIndex, final SkillSpec draft) {
		final ArrayList<SkillDelivery> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (SkillDelivery value : DeliveryRegistry.exposed()) {
			boolean compatible = DeliveryRegistry.compatible(value, draft);
			values.add(value); options.add(option(value.displayName(), firstSentence(value.description()),
					value.description(), compatible, compatible ? null : msg("disabled_delivery_incompatible")));
		}
		showSkillPicker(state, editIndex, draft, msg("delivery_title"), msg("skill_delivery_desc"),
				options.toArray(new WndBuilderStep.Option[0]), values.indexOf(draft.delivery), new SkillChoice() {
					@Override public void apply(int index) { draft.delivery = values.get(index); }
				});
	}

	private static void showTargetingPicker(final State state, final int editIndex, final SkillSpec draft) {
		final String[] fields = {"selector", "coverage", "filter", "parameters"};
		WndBuilderStep.Option[] options = {
				option(msg("target_selector", draft.targeting.selectorName()),
						msg("target_selector_desc"), msg("target_selector_desc"), true),
				option(msg("target_coverage", draft.targeting.coverageName()),
						msg("target_coverage_desc"), msg("target_coverage_desc"), true),
				option(msg("target_filter", draft.targeting.filterName()),
						msg("target_filter_desc"), msg("target_filter_desc"), true),
				option(msg("target_parameters", draft.targeting.parameterSummary()),
						msg("target_parameters_desc"), msg("target_parameters_desc"), true)
		};
		add(new WndBuilderStep(msg("targeting_title"), msg("skill_targeting_desc"),
				PlayerFacingClassBuildFormatter.skillName(draft), skillBudgetStatus(state.build, draft, editIndex), true,
				options, 0, msg("previous"), msg("edit_selected")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) {
				if ("selector".equals(fields[index])) showSelectorPicker(state, editIndex, draft);
				else if ("coverage".equals(fields[index])) showCoveragePicker(state, editIndex, draft);
				else if ("filter".equals(fields[index])) showFilterPicker(state, editIndex, draft);
				else showTargetParametersPicker(state, editIndex, draft);
			}
			@Override protected void onPrevious() { showSkillSheet(state, editIndex, draft); }
		});
	}

	private static void showTargetParametersPicker(final State state, final int editIndex, final SkillSpec draft) {
		final java.util.List<TargetingRegistry.ParameterOption> values = TargetingRegistry.parameterOptions(draft.targeting);
		WndBuilderStep.Option[] options=new WndBuilderStep.Option[values.size()];
		for(int i=0;i<values.size();i++)options[i]=option(values.get(i).name,values.get(i).summary(),
				values.get(i).summary(),true);
		showTargetPartPicker(state,editIndex,draft,msg("target_parameters_title"),msg("target_parameters_desc"),
				options,-1,new SkillChoice(){@Override public void apply(int index){draft.targeting=values.get(index).target.copy();}});
	}

	private static void showSelectorPicker(final State state, final int editIndex, final SkillSpec draft) {
		final java.util.List<TargetingSpec.Selector> values = TargetingRegistry.SELECTORS;
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.size()];
		for (int i = 0; i < values.size(); i++) {
			TargetingSpec candidate = draft.targeting.copy(); candidate.selector = values.get(i);
			boolean compatible = TargetingRegistry.compatible(candidate, draft);
			options[i] = option(candidate.selectorName(), TargetingRegistry.selectorSummary(values.get(i)),
					TargetingRegistry.selectorSummary(values.get(i)), compatible,
					compatible ? null : msg("disabled_target_incompatible"));
		}
		showTargetPartPicker(state, editIndex, draft, msg("target_selector_title"), msg("target_selector_desc"),
				options, values.indexOf(draft.targeting.selector), new SkillChoice() { @Override public void apply(int index) {
				draft.targeting.selector = values.get(index);
			} });
	}

	private static void showCoveragePicker(final State state, final int editIndex, final SkillSpec draft) {
		final java.util.List<TargetingSpec.Coverage> values = TargetingRegistry.COVERAGES;
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.size()];
		for (int i = 0; i < values.size(); i++) {
			TargetingSpec candidate = draft.targeting.copy(); candidate.coverage = values.get(i);
			boolean compatible = TargetingRegistry.compatible(candidate, draft);
			options[i] = option(candidate.coverageName(), TargetingRegistry.coverageSummary(values.get(i)),
					TargetingRegistry.coverageSummary(values.get(i)), compatible,
					compatible ? null : msg("disabled_target_incompatible"));
		}
		showTargetPartPicker(state, editIndex, draft, msg("target_coverage_title"), msg("target_coverage_desc"),
				options, values.indexOf(draft.targeting.coverage), new SkillChoice() { @Override public void apply(int index) {
				draft.targeting.coverage = values.get(index);
			} });
	}

	private static void showFilterPicker(final State state, final int editIndex, final SkillSpec draft) {
		final java.util.List<TargetingSpec.Filter> values = TargetingRegistry.FILTERS;
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.size()];
		for (int i = 0; i < values.size(); i++) {
			TargetingSpec candidate = draft.targeting.copy(); candidate.filter = values.get(i);
			boolean compatible = TargetingRegistry.compatible(candidate, draft);
			options[i] = option(candidate.filterName(), TargetingRegistry.filterSummary(values.get(i)),
					TargetingRegistry.filterSummary(values.get(i)), compatible,
					compatible ? null : msg("disabled_target_incompatible"));
		}
		showTargetPartPicker(state, editIndex, draft, msg("target_filter_title"), msg("target_filter_desc"),
				options, values.indexOf(draft.targeting.filter), new SkillChoice() { @Override public void apply(int index) {
				draft.targeting.filter = values.get(index);
			} });
	}

	private static void showTargetPartPicker(final State state, final int editIndex, final SkillSpec draft,
			String title, String description, WndBuilderStep.Option[] options, int selected, final SkillChoice choice) {
		add(new WndBuilderStep(title, description, PlayerFacingClassBuildFormatter.skillName(draft),
				skillBudgetStatus(state.build, draft, editIndex), true, options, selected, msg("previous"), msg("save")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) { choice.apply(index); showTargetingPicker(state, editIndex, draft); }
			@Override protected void onPrevious() { showTargetingPicker(state, editIndex, draft); }
		});
	}

	private static void showModifierPicker(final State state, final int editIndex, final SkillSpec draft) {
		final ArrayList<RuleModifier> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (RuleModifier value : ModifierRegistry.options()) {
			boolean compatible = ModifierRegistry.compatible(value.type, draft);
			values.add(value); options.add(option(ModifierRegistry.name(value), ModifierRegistry.summary(value.type),
					ModifierRegistry.summary(value.type), compatible,
					compatible ? null : msg("disabled_affix_incompatible")));
		}
		showSkillPicker(state, editIndex, draft, msg("modifier_title"), msg("skill_modifier_desc"),
				options.toArray(new WndBuilderStep.Option[0]), -1, new SkillChoice() {
					@Override public void apply(int index) { draft.modifier = values.get(index).copy(); }
				});
	}

	private static void showCostPicker(final State state, final int editIndex, final SkillSpec draft) {
		final ArrayList<CostRegistry.Entry> values = new ArrayList<>();
		ArrayList<WndBuilderStep.Option> options = new ArrayList<>();
		for (CostRegistry.Entry entry : CostRegistry.exposed(state.build)) {
			boolean compatible = CostRegistry.compatible(entry, state.build, draft);
			values.add(entry);
			options.add(option(entry.name(state.build), entry.summary(), entry.summary(), compatible,
					compatible ? null : costDisabledReason(entry, state.build, draft)));
		}
		showSkillPicker(state, editIndex, draft, msg("cost_title"), msg("skill_cost_desc"),
				options.toArray(new WndBuilderStep.Option[0]), -1, new SkillChoice() { @Override public void apply(int index) {
			RuleCost source = values.get(index).cost; draft.cost = new RuleCost(source.type, source.amount);
			draft.cost.resourceId = source.resourceId; draft.cost.resourceEngine = source.resourceEngine;
			draft.cost.reference = source.reference;
		} });
	}

	private static void showConstraintPicker(final State state, final int editIndex, final SkillSpec draft) {
		final java.util.List<SkillConstraint> values = ConstraintRegistry.exposed();
		WndBuilderStep.Option[] options = new WndBuilderStep.Option[values.size()];
		for (int i = 0; i < values.size(); i++) {
			SkillConstraint value = values.get(i);
			boolean compatible = ConstraintRegistry.compatible(value, draft);
			options[i] = option(rebateName(value.displayName(), value.effectiveRebate(state.build)),
					firstSentence(value.description()), value.description(), compatible,
					compatible ? null : msg("disabled_constraint_incompatible"));
		}
		showSkillPicker(state, editIndex, draft, msg("constraint_title"), msg("skill_constraint_desc"), options, -1,
				new SkillChoice() { @Override public void apply(int index) { draft.constraint = values.get(index).copy(); } });
	}

	private interface SkillChoice { void apply(int index); }

	private static void showSkillPicker(final State state, final int editIndex, final SkillSpec draft,
			String title, String explanation, WndBuilderStep.Option[] options, int selected, final SkillChoice choice) {
		showSkillPicker(state, editIndex, draft, title, explanation, options, selected, choice, true);
	}

	private static void showSkillPicker(final State state, final int editIndex, final SkillSpec draft,
			String title, String explanation, WndBuilderStep.Option[] options, int selected,
			final SkillChoice choice, final boolean returnToSheet) {
		add(new WndBuilderStep(title, explanation, PlayerFacingClassBuildFormatter.skillName(draft),
				skillBudgetStatus(state.build, draft, editIndex), draft.structurallyValid(), options, selected,
				msg("previous"), msg("save")) {
			@Override protected void onSelect(int index) {}
			@Override protected void onNext(int index) {
				choice.apply(index);
				if (returnToSheet) showSkillSheet(state, editIndex, draft);
			}
			@Override protected void onPrevious() { showSkillSheet(state, editIndex, draft); }
		});
	}

	private static TargetingSpec targetingSelectedEnemy() {
		TargetingSpec result = new TargetingSpec();
		result.selector = TargetingSpec.Selector.SELECTED_ACTOR;
		result.filter = TargetingSpec.Filter.ENEMY;
		return result;
	}

	private static void showPreview(final State state) {
		final BuildAnalysis analysis = analyze(state.build);
		final boolean valid = state.build.valid() && analysis.classification != BuildAnalysis.Classification.BROKEN;
		add(new WndBuilderSummary(msg("summary_title"), valid ? msg("summary_desc") : msg("summary_invalid"),
				PlayerFacingClassBuildFormatter.compactPreview(state.build), integrityStatus(analysis), valid,
				msg("previous"), msg("return_modify"), msg("view_details"), msg("create_short")) {
			@Override protected void onPrevious() { showSheet(state); }
			@Override protected void onModify() { showSheet(state); }
			@Override protected void onDetails() { add(new WndClassOverview(state.build)); }
			@Override protected void onCreate() { start(state.build); }
		});
	}

	private static BuildAnalysis analyze(ClassBuild build) { return new RuleBuildAnalyzer().analyze(RuleBuild.from(build)); }

	private static String integrityStatus(BuildAnalysis value) {
		return msg("integrity_" + value.classification.name().toLowerCase(), value.findings.size());
	}

	private static String capacityStatus(ClassBuild build) {
		return build.usedBudget() <= build.maxBudget()
				? msg("class_budget", build.usedBudget(), build.maxBudget(), build.maxBudget() - build.usedBudget())
				: msg("class_budget_over", build.usedBudget(), build.maxBudget(), build.usedBudget() - build.maxBudget());
	}

	private static String skillBudgetStatus(ClassBuild build, SkillSpec draft, int editIndex) {
		int currentUsed = build.usedBudget();
		if (editIndex >= 0) currentUsed = Math.max(0, currentUsed - build.skills.get(editIndex).nominalPowerCost()
				+ build.skills.get(editIndex).constraint.effectiveRebate(build, build.skills.get(editIndex)));
		if (!draft.structurallyValid()) return msg("skill_budget_pending", currentUsed, build.maxBudget(),
				Math.max(0, build.maxBudget() - currentUsed));
		ClassBuild candidate = build.copy();
		if (editIndex < 0) candidate.skills.add(draft.copy()); else candidate.skills.set(editIndex, draft.copy());
		int remaining = candidate.maxBudget() - candidate.usedBudget();
		return remaining >= 0
				? msg("skill_budget_status", draft.nominalPowerCost(), currentUsed, candidate.usedBudget(), candidate.maxBudget(), remaining)
				: msg("skill_budget_status_over", draft.nominalPowerCost(), currentUsed, candidate.usedBudget(), candidate.maxBudget(), -remaining);
	}

	private static String costName(String name, int cost) { return msg("component_cost", name, cost); }
	private static boolean onlyUnresolved(ArrayList<PlayerFacingValidationIssue> issues) {
		for (PlayerFacingValidationIssue issue : issues) if (issue.code != PlayerFacingValidationIssue.Code.UNRESOLVED_DEPENDENCY) return false;
		return true;
	}
	private static String rebateName(String name, int value) { return value <= 0 ? name : msg("component_rebate", name, value); }

	private static WndBuilderStep.Option option(String name, String shortDescription, String detail, boolean enabled) {
		return new WndBuilderStep.Option(msg("sheet_option", name, shortDescription), name, detail, enabled);
	}

	private static WndBuilderStep.Option option(String name, String shortDescription, String detail,
			boolean enabled, String disabledReason) {
		return new WndBuilderStep.Option(msg("sheet_option", name, shortDescription), name, detail,
				enabled, disabledReason);
	}

	private static WndBuilderStep.Option removeOption(String description) {
		return option(msg("remove_component"), description, description, true);
	}

	private static String conditionName(RuleCondition condition) {
		if (condition == null || condition.type == RuleCondition.Type.ALWAYS) return msg("no_condition");
		return condition.description();
	}

	private static String effectParameterLabel(EffectSpec effect) {
		EffectVocabularyRegistry.EffectEntry entry = EffectVocabularyRegistry.find(effect);
		return entry != null && !entry.hasParameters() ? msg("no_extra_parameters") : effect.parameterSummary();
	}

	private static String effectGameplayDetail(EffectSpec effect) {
		EffectVocabularyRegistry.EffectEntry entry = EffectVocabularyRegistry.find(effect);
		if (entry == null) return effect.description();
		return entry.detail() + (entry.hasParameters() ? "\n\n" + effect.parameterSummary() : "");
	}

	private static String effectParameterGameplay(EffectSpec effect, ClassBuild build, SkillSpec skill) {
		if (effect == null) return msg("no_extra_parameters_desc");
		switch (effect.operation) {
			case MOVE_PUSH: return msg("gameplay_param_push", effect.power);
			case MOVE_PULL: return msg("gameplay_param_pull", effect.power);
			case MOVE_THROW: return msg("gameplay_param_throw", effect.power + 1);
			case MOVE_DASH:
			case MOVE_TELEPORT: return msg("gameplay_param_travel", effect.power);
			case STATUS_POISON:
			case STATUS_BURNING:
			case STATUS_BLEEDING: return msg("gameplay_param_dot", effect.power, effect.duration);
			case DEFENSE_TEMP_HP: return msg("gameplay_param_temp_hp", effect.power, effect.duration);
			case CREATE_ACTOR: return msg("gameplay_param_actor", effect.count, effect.lifetime, effect.power);
			case CREATE_DEVICE:
			case CREATE_FIELD: return msg("gameplay_param_carrier", effect.lifetime, effect.period, effect.power);
			case CREATE_TRAP: return msg("gameplay_param_trap", effect.power);
			case RESOURCE_CONVERT: return msg("gameplay_param_convert",
					effect.power, resourceName(build, effect.resourceId),
					resourceName(build, effect.targetResourceId), 1, 1);
			case RESOURCE_GAIN:
			case RESOURCE_DRAIN:
			case RESOURCE_RESERVE:
			case RESOURCE_SUPPRESS: return msg("gameplay_param_resource", resourceName(build, effect.resourceId), effect.power);
			case TRANSFORM_MODE: return msg("gameplay_param_mode", effect.stateId, effect.duration);
			default:
				EffectVocabularyRegistry.EffectEntry entry = EffectVocabularyRegistry.find(effect);
				return entry != null && !entry.hasParameters() ? msg("no_extra_parameters_desc")
						: msg("gameplay_param_general", effect.power, effect.duration,
							skill == null || skill.targeting == null ? 1 : skill.targeting.range);
		}
	}

	private static String resourceName(ClassBuild build, String id) {
		ResourceSpec resource = build == null ? null : build.resource(id == null ? "" : id);
		return resource == null ? msg("unnamed_resource") : resource.displayName();
	}

	private static String parameterDisabledReason(EffectSpec effect, ClassBuild build) {
		if (effect.operation == EffectSpec.Operation.RESOURCE_CONVERT && build.resources.size() < 2)
			return msg("disabled_need_two_resources");
		if (effect.family == EffectFamily.RESOURCE_OPERATION && build.resources.isEmpty())
			return msg("disabled_need_resource");
		return msg("disabled_no_compatible_parameter");
	}

	private static String lawBudgetReason(ClassBuild build, ClassLaw value, int editIndex) {
		ClassBuild candidate = build.copy();
		if (editIndex < 0) candidate.laws.add(value); else candidate.laws.set(editIndex, value);
		return budgetReason(candidate);
	}

	private static String traitBudgetReason(ClassBuild build, TraitSpec value, int editIndex) {
		ClassBuild candidate = build.copy();
		if (editIndex < 0) candidate.traits.add(value.copy()); else candidate.traits.set(editIndex, value.copy());
		return budgetReason(candidate);
	}

	private static boolean traitUsedElsewhere(ClassBuild build, TraitSpec value, int except) {
		for (int i = 0; i < build.traits.size(); i++) if (i != except
				&& build.traits.get(i).stableId().equals(value.stableId())) return true;
		return false;
	}

	private static String lawCompatibilityReason(ClassBuild build, ClassLaw value) {
		String issue = LawTraitRegistry.lawIssue(build, value);
		return issue == null ? null : traitCompatibilityReason(issue);
	}

	private static String traitCompatibilityReason(String issue) {
		return msg("trait_issue_" + (issue == null ? "unsupported" : issue));
	}

	private static String componentDependencyReason(String issue) {
		return msg("component_issue_" + (issue == null ? "invalid_component_parameters" : issue));
	}

	private static String budgetReason(ClassBuild candidate) {
		return candidate.budgetValid() ? null : msg("disabled_budget_need",
				candidate.usedBudget() - candidate.maxBudget(), Math.max(0, candidate.maxBudget() - candidate.usedBudget()));
	}

	private static String costDisabledReason(CostRegistry.Entry entry, ClassBuild build, SkillSpec skill) {
		if (entry.cost.type == RuleCost.Type.ACTION) return msg("disabled_action_active_only");
		if (entry.cost.type == RuleCost.Type.COOLDOWN) return msg("disabled_duplicate_cooldown");
		return msg("disabled_cost_incompatible");
	}

	private static String firstSentence(String text) {
		if (text == null) return "";
		int english = text.indexOf('.');
		int chinese = text.indexOf('。');
		int end = english < 0 ? chinese : chinese < 0 ? english : Math.min(english, chinese);
		return end < 0 ? text : text.substring(0, end + 1);
	}

	private static void start(ClassBuild build) {
		CustomClassConfig.setPending(CustomClassConfig.fromClassBuild(build.copy()));
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.randomizedClass = false;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		Dungeon.initSeed();
		ActionIndicator.clearAction();
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		Game.switchScene(InterlevelScene.class);
	}
}
