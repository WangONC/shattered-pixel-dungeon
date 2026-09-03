package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.component.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.resource.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure player-form boundary. It turns declarative schemas into UI models and is the only
 * field-editing bridge from WndCreateClassV6 to PlayerBuildSession.dispatch().
 */
public final class BuilderFormController {

	public static final String DEFERRED_NESTED_REASON =
			"Nested and list editors are deferred to their implementation phase.";
	public static final String DEFERRED_RUNTIME_REASON =
			"This runtime-backed field is not implemented in P02.";

	public static final class Choice {
		private final String value;
		private final String label;
		private final String detail;
		private final boolean enabled;

		Choice(String value, String label, String detail, boolean enabled) {
			this.value = value;
			this.label = label;
			this.detail = detail;
			this.enabled = enabled;
		}

		public String value() { return value; }
		public String label() { return label; }
		public String detail() { return detail; }
		public boolean enabled() { return enabled; }
	}

	public static final class FieldModel {
		private final FormFieldSchema schema;
		private final String currentValue;
		private final boolean enabled;
		private final String disabledReason;
		private final List<Choice> choices;
		private final String suggestedValue;
		private final boolean unresolved;
		private final String lastKnownDisplayName;
		private final String shortTargetId;
		private final String detail;

		FieldModel(FormFieldSchema schema, String currentValue, boolean enabled,
				String disabledReason, List<Choice> choices, String suggestedValue,
				boolean unresolved, String lastKnownDisplayName, String shortTargetId,
				String detail) {
			this.schema = schema;
			this.currentValue = currentValue;
			this.enabled = enabled;
			this.disabledReason = disabledReason;
			this.choices = Collections.unmodifiableList(new ArrayList<>(choices));
			this.suggestedValue = suggestedValue;
			this.unresolved = unresolved;
			this.lastKnownDisplayName = lastKnownDisplayName;
			this.shortTargetId = shortTargetId;
			this.detail = detail;
		}

		public FormFieldSchema schema() { return schema; }
		public String fieldKey() { return schema.fieldKey(); }
		public String currentValue() { return currentValue; }
		public boolean enabled() { return enabled; }
		public String disabledReason() { return disabledReason; }
		public List<Choice> choices() { return choices; }
		public String suggestedValue() { return suggestedValue; }
		public boolean unresolved() { return unresolved; }
		public String lastKnownDisplayName() { return lastKnownDisplayName; }
		public String shortTargetId() { return shortTargetId; }
		public String detail() { return detail; }
	}

	public static final class FormModel {
		private final String targetId;
		private final String title;
		private final String variantKey;
		private final List<FieldModel> fields;

		FormModel(String targetId, String title, String variantKey, List<FieldModel> fields) {
			this.targetId = targetId;
			this.title = title;
			this.variantKey = variantKey;
			this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
		}

		public String targetId() { return targetId; }
		public String title() { return title; }
		public String variantKey() { return variantKey; }
		public List<FieldModel> fields() { return fields; }
		public FieldModel requireField(String key) {
			for (FieldModel field : fields) if (field.fieldKey().equals(key)) return field;
			throw new IllegalArgumentException("field is not present in UI model: " + variantKey + "." + key);
		}
	}

	private final PlayerBuildSession session;
	private final CanonicalBuildCodec codec = new CanonicalBuildCodec();

	public BuilderFormController(PlayerBuildSession session) {
		if (session == null) throw new IllegalArgumentException("player build session is required");
		this.session = session;
	}

	public PlayerBuildSession session() { return session; }
	public BuilderState state() { return session.state(); }
	public BuilderState dispatch(BuilderCommand command) { return session.dispatch(command); }

	public FormModel form(String targetId) {
		StableTarget owner = requireTarget(targetId);
		String variant = variantOf(owner);
		List<FieldModel> fields = new ArrayList<>();
		for (FormFieldSchema schema : V6FormSchemas.require(variant).fields()) {
			fields.add(field(owner, variant, schema));
		}
		return new FormModel(targetId, owner.displayName().text(), variant, fields);
	}

	public BuilderCommand commandForValue(String targetId, String fieldKey, String rawValue) {
		FormModel form = form(targetId);
		FieldModel field = form.requireField(fieldKey);
		if (!field.enabled()) {
			throw new IllegalStateException("field is disabled: " + fieldKey + " — " + field.disabledReason());
		}
		if (rawValue == null || rawValue.equals(field.currentValue())) {
			throw new IllegalArgumentException("field action must change the current value: " + fieldKey);
		}

		FormFieldSchema schema = field.schema();
		StableTarget owner = requireTarget(targetId);
		if (owner instanceof SkillSpec) {
			if (schema.kind() == FormFieldSchema.Kind.REFERENCE) {
				Choice choice=requireEnabledChoice(field,rawValue);ReferenceFieldSchema reference=(ReferenceFieldSchema)schema;
				return skillReferenceCommand((SkillSpec)owner,fieldKey,typedRef(reference.expectedKind(),choice.value(),choice.label()));
			}
			return skillCommand((SkillSpec) owner, fieldKey, rawValue);
		}
		switch (schema.kind()) {
			case TEXT:
				return new BuilderCommand.SetFieldValue(targetId, form.variantKey(), fieldKey, rawValue);
			case NUMBER: {
				Choice choice = requireEnabledChoice(field, rawValue);
				int current = Integer.parseInt(field.currentValue());
				int next = Integer.parseInt(choice.value());
				NumberFieldSchema number = (NumberFieldSchema) schema;
				int direction = next > current ? 1 : -1;
				return new NumberStepper(number, current).command(targetId, form.variantKey(), direction, 1);
			}
			case ENUM:
				requireEnabledChoice(field, rawValue);
				return new EnumSelector((EnumFieldSchema) schema).command(targetId, form.variantKey(), rawValue);
			case ENUM_LIST: {
				requireEnabledChoice(field, rawValue);
				EnumListFieldSchema list = (EnumListFieldSchema) schema;
				MultiEnumSelector selector = new MultiEnumSelector(targetId, form.variantKey(), list,
						csv(field.currentValue()));
				for (String option : list.optionKeys()) {
					try {
						BuilderCommand candidate = selector.toggle(option);
						if (((BuilderCommand.SetFieldValue) candidate).value().equals(rawValue)) return candidate;
					} catch (IllegalArgumentException ignored) {
						// A required final selection cannot be removed.
					}
				}
				throw new IllegalArgumentException("enum-list choice is not a single valid toggle: " + rawValue);
			}
			case BOOLEAN:
				requireEnabledChoice(field, rawValue);
				return new BuilderCommand.SetFieldValue(targetId, form.variantKey(), fieldKey, rawValue);
			case REFERENCE: {
				Choice choice = requireEnabledChoice(field, rawValue);
				ReferenceFieldSchema reference = (ReferenceFieldSchema) schema;
				return new BuilderCommand.RebindReference(targetId, fieldKey,
						typedRef(reference.expectedKind(), choice.value(), choice.label()));
			}
			default:
				throw new IllegalStateException("field kind cannot produce a player command: " + schema.kind());
		}
	}

	public BuilderState dispatchValue(String targetId, String fieldKey, String rawValue) {
		String before = codec.serialize(state().draft());
		BuilderState next = dispatch(commandForValue(targetId, fieldKey, rawValue));
		if (before.equals(codec.serialize(next.draft()))) {
			throw new IllegalStateException("enabled form action was a no-op: " + targetId + "." + fieldKey);
		}
		return next;
	}

	private FieldModel field(StableTarget owner, String variant, FormFieldSchema schema) {
		String value = valueOf(owner, schema.fieldKey());
		if (schema.kind() == FormFieldSchema.Kind.READ_ONLY_DIAGNOSTIC) {
			return disabled(schema, diagnosticText(owner), "Diagnostics are read-only.");
		}
		if (schema.kind() == FormFieldSchema.Kind.NESTED_VARIANT || schema.kind() == FormFieldSchema.Kind.LIST) {
			return disabled(schema, value, DEFERRED_NESTED_REASON);
		}
		if (owner instanceof ContractNodeSpec && "variant_key".equals(schema.fieldKey())
				|| owner instanceof SynthesisRecipeSpec && "output_variant".equals(schema.fieldKey())) {
			return disabled(schema, value, DEFERRED_RUNTIME_REASON);
		}
		if ("default_duration_turns".equals(schema.fieldKey()) && !usesTurnDuration(owner)) {
			return disabled(schema, value, "Enable TURN_BASED duration before editing turn count.");
		}
		if (owner instanceof SkillSpec) {
			String reason = disabledSkillField((SkillSpec) owner, schema.fieldKey());
			if (reason != null) return disabled(schema, value, reason);
		}
		String classNodeReason=disabledClassNodeField(owner,schema.fieldKey());
		if(classNodeReason!=null)return disabled(schema,value,classNodeReason);

		switch (schema.kind()) {
			case TEXT:
				return enabled(schema, value, Collections.<Choice>emptyList(), alternateText(value,
						((TextFieldSchema) schema).maximumCodePoints()));
			case NUMBER:
				return numberField(owner, variant, (NumberFieldSchema) schema, value);
			case ENUM:
				List<String> options=((EnumFieldSchema)schema).optionKeys();
				if(owner instanceof SkillSpec)options=skillEnumOptions((SkillSpec)owner,schema.fieldKey(),options);
				return enumField(schema, value, options);
			case ENUM_LIST:
				return enumListField(owner, variant, (EnumListFieldSchema) schema, value);
			case BOOLEAN:
				return enabled(schema, value, Collections.singletonList(
						new Choice(Boolean.toString(!Boolean.parseBoolean(value)), "Set " + !Boolean.parseBoolean(value), "", true)),
						Boolean.toString(!Boolean.parseBoolean(value)));
			case REFERENCE:
				return referenceField(owner, (ReferenceFieldSchema) schema, currentReference(owner, schema.fieldKey()));
			default:
				return disabled(schema, value, "No P02 player editor exists for this field kind.");
		}
	}

	private FieldModel numberField(StableTarget owner, String variant, NumberFieldSchema schema, String raw) {
		int current = Integer.parseInt(raw);
		NumberStepper stepper = new NumberStepper(schema, current);
		List<Choice> choices = new ArrayList<>();
		int lower = stepper.decrement();
		if (lower != current && validNumberCandidate(owner, schema.fieldKey(), lower)) {
			choices.add(new Choice(Integer.toString(lower), "−  " + lower, "Number stepper", true));
		}
		int upper = stepper.increment();
		if (upper != current && validNumberCandidate(owner, schema.fieldKey(), upper)) {
			choices.add(new Choice(Integer.toString(upper), "+  " + upper, "Number stepper", true));
		}
		if (choices.isEmpty()) return disabled(schema, raw, "No valid adjacent value is available.");
		return enabled(schema, raw, choices, choices.get(0).value());
	}

	private FieldModel enumField(FormFieldSchema schema, String value, List<String> options) {
		List<Choice> choices = new ArrayList<>();
		for (String option : options) {
			choices.add(new Choice(option, option, option.equals(value) ? "Current value" : "", !option.equals(value)));
		}
		String suggested = firstEnabled(choices);
		return suggested == null ? disabled(schema, value, "No alternative enum value is available.")
				: enabled(schema, value, choices, suggested);
	}

	private FieldModel enumListField(StableTarget owner, String variant, EnumListFieldSchema schema, String value) {
		Set<String> selected = csv(value);
		MultiEnumSelector selector = new MultiEnumSelector(owner.id().value(), variant, schema, selected);
		List<Choice> choices = new ArrayList<>();
		for (String option : schema.optionKeys()) {
			try {
				BuilderCommand.SetFieldValue command = (BuilderCommand.SetFieldValue) selector.toggle(option);
				choices.add(new Choice(command.value(), (selected.contains(option) ? "Remove " : "Add ") + option,
						selected.contains(option) ? "Selected" : "Not selected", true));
			} catch (IllegalArgumentException error) {
				choices.add(new Choice(value, "Keep " + option, error.getMessage(), false));
			}
		}
		String suggested = firstEnabled(choices);
		return suggested == null ? disabled(schema, value, "At least one entity type must remain selected.")
				: enabled(schema, value, choices, suggested);
	}

	private FieldModel referenceField(StableTarget owner, ReferenceFieldSchema schema, TypedRef current) {
		ReferencePicker picker = ReferencePicker.from(state().draft(), schema, current, owner);
		List<Choice> choices = new ArrayList<>();
		boolean unresolved = false;
		for (ReferencePicker.Option option : picker.options()) {
			boolean selected = current != null && current.targetId().value().equals(option.targetId());
			boolean canSelect = !option.unresolved() && !selected;
			String detail = option.unresolved() ? "UNRESOLVED · " + option.displayName() + " · #" + option.shortId()
					: selected ? "Current reference" : "Rebind reference";
			choices.add(new Choice(option.targetId(), option.displayName(), detail, canSelect));
			if (selected && option.unresolved()) unresolved = true;
		}
		String currentValue = current == null ? "None" : current.targetId().value();
		String suggested = firstEnabled(choices);
		String lastKnown = unresolved ? current.lastKnownDisplayName() : "";
		String shortId = unresolved ? shortId(current.targetId().value()) : "";
		String detail = unresolved ? "UNRESOLVED · " + lastKnown + " · #" + shortId + " · Choose a replacement below." : "";
		if (suggested == null) {
			return new FieldModel(schema, currentValue, false, "No compatible replacement declaration is available.",
					choices, null, unresolved, lastKnown, shortId, detail);
		}
		return new FieldModel(schema, currentValue, true, "", choices, suggested,
				unresolved, lastKnown, shortId, detail);
	}

	private static Choice requireEnabledChoice(FieldModel field, String rawValue) {
		for (Choice choice : field.choices()) {
			if (choice.enabled() && choice.value().equals(rawValue)) return choice;
		}
		throw new IllegalArgumentException("value is not an enabled UI choice for " + field.fieldKey() + ": " + rawValue);
	}

	private static FieldModel enabled(FormFieldSchema schema, String value, List<Choice> choices, String suggested) {
		return new FieldModel(schema, value, true, "", choices, suggested, false, "", "", "");
	}

	private static FieldModel disabled(FormFieldSchema schema, String value, String reason) {
		return new FieldModel(schema, value, false, reason, Collections.<Choice>emptyList(), null,
				false, "", "", value);
	}

	private String diagnosticText(StableTarget owner) {
		StringBuilder result = new StringBuilder();
		for (DependencyDiagnostic diagnostic : state().validation().diagnostics()) {
			if (diagnostic.ownerNodeId().equals(owner.id())) {
				if (result.length() > 0) result.append('\n');
				result.append(diagnostic.state()).append(" · ").append(diagnostic.fieldPath())
						.append(" · ").append(diagnostic.messageKey());
				if (diagnostic.targetId() != null) result.append(" · #").append(shortId(diagnostic.targetId().value()));
			}
		}
		for (String diagnostic : state().commandDiagnostics()) {
			if (result.length() > 0) result.append('\n');
			result.append("COMMAND · ").append(diagnostic);
		}
		return result.length() == 0 ? "No diagnostics." : result.toString();
	}

	private static boolean validNumberCandidate(StableTarget owner, String key, int value) {
		if (owner instanceof SkillSpec) {
			if ("targeting_range".equals(key)) return value >= 1 && value <= 20;
			if ("targeting_maximum_targets".equals(key)) return value == 1;
			if ("effect_primary_amount".equals(key) || "effect_secondary_amount".equals(key)) return value >= 1 && value <= 999;
		}
		if (owner instanceof ResourceSpec) {
			ResourceSpec v = (ResourceSpec) owner;
			if ("minimum".equals(key)) return value < v.maximum() && value <= v.initialValue();
			if ("maximum".equals(key)) return value > v.minimum() && value >= v.initialValue();
			if ("initial_value".equals(key)) return value >= v.minimum() && value <= v.maximum();
		}
		if (owner instanceof MarkSpec) {
			MarkSpec v = (MarkSpec) owner;
			if ("minimum".equals(key)) return value <= v.maximum() && value <= v.initialValue();
			if ("maximum".equals(key)) return value >= 1 && value >= v.minimum() && value >= v.initialValue();
			if ("initial_value".equals(key)) return value >= v.minimum() && value <= v.maximum();
			if ("default_duration_turns".equals(key)) return value >= 1;
		}
		if (owner instanceof ModeSpec && "default_duration_turns".equals(key)) return value >= 1;
		return true;
	}

	private static boolean usesTurnDuration(StableTarget owner) {
		return owner instanceof MarkSpec && ((MarkSpec) owner).durationPolicy() == MarkSpec.MarkDurationPolicy.TURN_BASED
				|| owner instanceof ModeSpec && ((ModeSpec) owner).durationPolicy() == ModeSpec.ModeDurationPolicy.TURN_BASED;
	}

	private static String valueOf(StableTarget owner, String key) {
		if ("display_name".equals(key)) return owner.displayName().text();
		if (owner instanceof ResourceSpec) {
			ResourceSpec v=(ResourceSpec)owner;if("minimum".equals(key))return Integer.toString(v.minimum());if("maximum".equals(key))return Integer.toString(v.maximum());if("initial_value".equals(key))return Integer.toString(v.initialValue());if("default_overflow_policy".equals(key))return v.defaultOverflowPolicy().name();if("hud.visible".equals(key))return Boolean.toString(v.hud().visible());if("hud.order".equals(key))return Integer.toString(v.hud().order());if("hud.presentation_key".equals(key))return v.hud().presentationKey();
		} else if (owner instanceof MarkSpec) {
			MarkSpec v=(MarkSpec)owner;if("kind".equals(key))return v.kind().name();if("minimum".equals(key))return Integer.toString(v.minimum());if("maximum".equals(key))return Integer.toString(v.maximum());if("initial_value".equals(key))return Integer.toString(v.initialValue());if("duration_policy".equals(key))return v.durationPolicy().name();if("default_duration_turns".equals(key))return Integer.toString(v.defaultDurationTurns());if("refresh_policy".equals(key))return v.refreshPolicy().name();if("overflow_policy".equals(key))return v.overflowPolicy().name();if("provenance_policy".equals(key))return v.provenancePolicy().name();
		} else if (owner instanceof ModeGroupSpec) {
			if("policy".equals(key))return ((ModeGroupSpec)owner).policy().name();
		} else if (owner instanceof ModeSpec) {
			ModeSpec v=(ModeSpec)owner;if("group".equals(key))return v.group().targetId().value();if("initial".equals(key))return Boolean.toString(v.initial());if("duration_policy".equals(key))return v.durationPolicy().name();if("default_duration_turns".equals(key))return Integer.toString(v.defaultDurationTurns());
		} else if (owner instanceof EntityCapacitySpec) {
			EntityCapacitySpec v=(EntityCapacitySpec)owner;if("entity_types".equals(key))return entityTypes(v.entityTypes());if("maximum".equals(key))return Integer.toString(v.maximum());if("overflow_policy".equals(key))return v.overflowPolicy().name();
		} else if (owner instanceof EntitySpec) {
			EntitySpec v=(EntitySpec)owner;if("entity_type".equals(key))return v.type().name();if("capacity".equals(key))return v.capacity()==null?"None":v.capacity().targetId().value();if("facets".equals(key))return "Deferred facets";if("capabilities".equals(key))return v.capabilities().size()+" item(s)";
		} else if (owner instanceof AbilityPoolSpec) {
			AbilityPoolSpec v=(AbilityPoolSpec)owner;if("capacity".equals(key))return Integer.toString(v.capacity());if("overflow_policy".equals(key))return v.overflowPolicy().name();
		} else if (owner instanceof PropertySpec) {
			PropertySpec v=(PropertySpec)owner;if("value_kind".equals(key))return v.valueKind().name();if("maximum_stack".equals(key))return Integer.toString(v.maximumStack());
		} else if (owner instanceof SynthesisRecipeSpec) {
			SynthesisRecipeSpec v=(SynthesisRecipeSpec)owner;if("inputs".equals(key))return v.inputs().size()+" item(s)";if("output_variant".equals(key))return v.outputVariantKey();
		} else if (owner instanceof SkillSpec) {
			SkillSpec v=(SkillSpec)owner;
			if(!v.typed()){if("variant_key".equals(key))return v.variantKey();if("diagnostics".equals(key))return "";throw new IllegalArgumentException("legacy skill envelope field is read-only: "+key);}
			if("activation_variant".equals(key))return v.activation().variantKey();
			if("condition_variant".equals(key)){if(v.condition() instanceof UnconfiguredConditionExpr)return UnconfiguredConditionExpr.VARIANT;ConditionExpr leaf=conditionLeaf(v);return leaf==null?"ALWAYS":leaf.variantKey();}
			if(key.startsWith("condition_")&&!"condition_variant".equals(key))return conditionValue(v,key.substring("condition_".length()));
			if("effect_primary_family".equals(key))return v.effects().primary().family()==null?"UNCONFIGURED":v.effects().primary().family().name();
			if("effect_primary_variant".equals(key))return v.effects().primary().variantKey().name();
			if(key.startsWith("effect_primary_")&&!"effect_primary_family".equals(key)&&!"effect_primary_variant".equals(key))return effectValue(v.effects().primary(),key.substring("effect_primary_".length()));
			if("effect_primary_damage_type".equals(key))return v.effects().primary() instanceof DirectDamageEffectSpec?((DirectDamageEffectSpec)v.effects().primary()).damageType().name():"UNCONFIGURED";
			if("effect_primary_defense_policy".equals(key))return v.effects().primary() instanceof DirectDamageEffectSpec?((DirectDamageEffectSpec)v.effects().primary()).defensePolicy().name():"UNCONFIGURED";
			if("effect_secondary_family".equals(key))return v.effects().secondary()==null?"NONE":v.effects().secondary().effect().family()==null?"UNCONFIGURED":v.effects().secondary().effect().family().name();
			if("effect_secondary_variant".equals(key))return v.effects().secondary()==null?"UNCONFIGURED":v.effects().secondary().effect().variantKey().name();
			if("effect_secondary_activation".equals(key))return v.effects().secondary()==null?"UNCONFIGURED":v.effects().secondary().activation().variantKey();
			if(key.startsWith("effect_secondary_")&&!"effect_secondary_family".equals(key)&&!"effect_secondary_variant".equals(key)&&!"effect_secondary_activation".equals(key))return effectValue(v.effects().secondary()==null?null:v.effects().secondary().effect(),key.substring("effect_secondary_".length()));
			if("delivery_variant".equals(key))return v.delivery().variantKey();
			if("delivery_requires_line_of_sight".equals(key))return Boolean.toString(v.delivery() instanceof DirectDeliverySpec&&((DirectDeliverySpec)v.delivery()).requiresLineOfSight());
			if("delivery_trace_width".equals(key))return Integer.toString(v.delivery() instanceof TraceDeliverySpec?((TraceDeliverySpec)v.delivery()).width():1);
			if("delivery_trace_stops_at_blocking_cell".equals(key))return Boolean.toString(!(v.delivery() instanceof TraceDeliverySpec)||((TraceDeliverySpec)v.delivery()).stopsAtFirstBlockingCell());
			if("delivery_ground_requires_visible_cell".equals(key))return Boolean.toString(!(v.delivery() instanceof GroundDeliverySpec)||((GroundDeliverySpec)v.delivery()).requiresVisibleCell());
			if("targeting_selector".equals(key))return v.targeting().selector().variantKey();
			if("targeting_coverage".equals(key))return v.targeting().coverage().variantKey();
			if("targeting_filter".equals(key))return filterVariant(v.targeting().filter());
			if("targeting_range".equals(key))return Integer.toString(v.targeting().range());
			if("targeting_maximum_targets".equals(key))return Integer.toString(v.targeting().maximumTargets());
			if("targeting_radius".equals(key))return Integer.toString(v.targeting().coverage() instanceof RadiusCoverageSpec?((RadiusCoverageSpec)v.targeting().coverage()).radius():1);
			if("targeting_line_length".equals(key))return Integer.toString(v.targeting().coverage() instanceof LineCoverageSpec?((LineCoverageSpec)v.targeting().coverage()).length():3);
			if("targeting_line_width".equals(key))return Integer.toString(v.targeting().coverage() instanceof LineCoverageSpec?((LineCoverageSpec)v.targeting().coverage()).width():1);
			if("targeting_line_of_sight".equals(key))return v.targeting().lineOfSight().name();
			if("targeting_ordering".equals(key))return v.targeting().ordering().name();
			if("modifier_variant".equals(key))return v.modifier()==null?"NONE":v.modifier().variantKey();
			if(key.startsWith("modifier_"))return modifierValue(v.modifier(),key.substring("modifier_".length()));
			if("cost_variant".equals(key))return v.cost().variantKey();
			if("cost_amount".equals(key))return costAmount(v.cost());
			if("cost_item_category".equals(key))return v.cost() instanceof ItemCostSpec?((ItemCostSpec)v.cost()).itemFilter().category().name():"ANY_CONSUMABLE";
			if("cost_resource".equals(key))return v.cost() instanceof ResourceCostSpec?((ResourceCostSpec)v.cost()).resource().targetId().value():"None";
			if("constraint_variant".equals(key))return v.constraint()==null?"NONE":v.constraint().variantKey();
		} else if(owner instanceof BasicAttackComponentSpec){BasicAttackComponentSpec v=(BasicAttackComponentSpec)owner;if("availability".equals(key))return v.availability().name();if("damage_numerator".equals(key))return Integer.toString(v.damageNumerator());if("damage_denominator".equals(key))return Integer.toString(v.damageDenominator());if("allowed_weapons".equals(key))return v.allowedWeapons().category().name();if("action_time_turns".equals(key))return Integer.toString(v.actionTimeTurns());
		} else if(owner instanceof ResourceFlowComponentSpec){ResourceFlowComponentSpec v=(ResourceFlowComponentSpec)owner;if("event_type".equals(key))return v.trigger() instanceof EventTriggerSpec?((EventTriggerSpec)v.trigger()).event().name():"ACTIVE";String operation=operationValue(v.operation(),key);if(operation!=null)return operation;
		} else if(owner instanceof ActiveResourceOperationComponentSpec){ActiveResourceOperationComponentSpec v=(ActiveResourceOperationComponentSpec)owner;if("action_time_turns".equals(key))return Integer.toString(v.actionTimeTurns());if("cost_variant".equals(key))return v.cost().variantKey();if("cost_amount".equals(key))return costAmount(v.cost());if("cost.resource".equals(key))return v.cost() instanceof ResourceCostSpec?((ResourceCostSpec)v.cost()).resource().targetId().value():"None";String operation=operationValue(v.operation(),key);if(operation!=null)return operation;
		} else if(owner instanceof ResourceClassOperationSpec){ResourceClassOperationSpec v=(ResourceClassOperationSpec)owner;if("action_time_turns".equals(key))return Integer.toString(v.actionTimeTurns());if("hud_visible".equals(key))return Boolean.toString(v.hudVisible());if("hud_order".equals(key))return Integer.toString(v.hudOrder());if("cost_variant".equals(key))return v.cost().variantKey();if("cost_amount".equals(key))return costAmount(v.cost());if("cost.resource".equals(key))return v.cost() instanceof ResourceCostSpec?((ResourceCostSpec)v.cost()).resource().targetId().value():"None";String operation=operationValue(v.operation(),key);if(operation!=null)return operation;
		} else if (owner instanceof ContractNodeSpec) {
			if("variant_key".equals(key))return ((ContractNodeSpec)owner).variantKey();
		}
		if ("diagnostics".equals(key)) return "";
		throw new IllegalArgumentException("unknown form field value: " + owner.getClass().getSimpleName() + "." + key);
	}

	private static String conditionValue(SkillSpec skill,String field){
		ConditionExpr leaf=conditionLeaf(skill);if("subject".equals(field))return leaf instanceof BuiltinStatCompareCondition?((BuiltinStatCompareCondition)leaf).subject().name():"CLASS_OWNER";
		if("builtin_stat".equals(field))return leaf instanceof BuiltinStatCompareCondition?((BuiltinStatCompareCondition)leaf).stat().name():"HP_PERCENT";
		if("operator".equals(field))return leaf instanceof BuiltinStatCompareCondition?((BuiltinStatCompareCondition)leaf).operator().name():leaf instanceof ResourceCompareCondition?((ResourceCompareCondition)leaf).operator().name():"GTE";
		if("value".equals(field))return leaf instanceof BuiltinStatCompareCondition?Integer.toString(fixed(((BuiltinStatCompareCondition)leaf).value())):leaf instanceof ResourceCompareCondition?Integer.toString(((ResourceCompareCondition)leaf).value()):"1";
		if("resource".equals(field))return leaf instanceof ResourceCompareCondition?((ResourceCompareCondition)leaf).resource().targetId().value():"None";
		throw new IllegalArgumentException("unknown condition form field "+field);
	}
	private static ConditionExpr conditionLeaf(SkillSpec skill){return skill.condition() instanceof AllOfCondition&&!((AllOfCondition)skill.condition()).children().isEmpty()?((AllOfCondition)skill.condition()).children().get(0):null;}
	private static String effectValue(EffectSpec effect,String field){
		if(effect instanceof ResourceOperationEffectSpec&&field.startsWith("operation_"))return operationValue(((ResourceOperationEffectSpec)effect).operation(),"operation."+field.substring("operation_".length()));
		if("damage_type".equals(field)){if(effect instanceof DirectDamageEffectSpec)return ((DirectDamageEffectSpec)effect).damageType().name();if(effect instanceof MissingHpDamageEffectSpec)return ((MissingHpDamageEffectSpec)effect).damageType().name();return "UNTYPED";}
		if("defense_policy".equals(field))return effect instanceof DirectDamageEffectSpec?((DirectDamageEffectSpec)effect).defensePolicy().name():"SPD_NATIVE";
		if("amount".equals(field)){ValueSpec value=null;if(effect instanceof DirectDamageEffectSpec)value=((DirectDamageEffectSpec)effect).amount();else if(effect instanceof HealEffectSpec)value=((HealEffectSpec)effect).amount();else if(effect instanceof BarrierEffectSpec)value=((BarrierEffectSpec)effect).amount();else if(effect instanceof TemporaryHpEffectSpec)value=((TemporaryHpEffectSpec)effect).amount();return Integer.toString(value==null?1:fixed(value));}
		if("percent".equals(field)){if(effect instanceof PercentMaxHpDamageEffectSpec)return Integer.toString(((PercentMaxHpDamageEffectSpec)effect).percent());if(effect instanceof MitigateEffectSpec)return Integer.toString(((MitigateEffectSpec)effect).percent());if(effect instanceof RedirectDamageEffectSpec)return Integer.toString(((RedirectDamageEffectSpec)effect).percent());return "10";}
		if("absolute_cap".equals(field)){if(effect instanceof PercentMaxHpDamageEffectSpec)return Integer.toString(((PercentMaxHpDamageEffectSpec)effect).absoluteCap());if(effect instanceof MissingHpDamageEffectSpec)return Integer.toString(((MissingHpDamageEffectSpec)effect).absoluteCap());return "0";}
		if("base_amount".equals(field))return Integer.toString(effect instanceof MissingHpDamageEffectSpec?fixed(((MissingHpDamageEffectSpec)effect).baseAmount()):1);
		if("missing_hp_numerator".equals(field))return Integer.toString(effect instanceof MissingHpDamageEffectSpec?((MissingHpDamageEffectSpec)effect).missingHpNumerator():1);
		if("missing_hp_denominator".equals(field))return Integer.toString(effect instanceof MissingHpDamageEffectSpec?((MissingHpDamageEffectSpec)effect).missingHpDenominator():2);
		if("hp_percent_threshold".equals(field))return Integer.toString(effect instanceof ExecuteEffectSpec?((ExecuteEffectSpec)effect).hpPercentThreshold():20);
		if("status".equals(field))return effect instanceof ApplyStatusEffectSpec?((ApplyStatusEffectSpec)effect).status().name():"POISON";
		if("intensity".equals(field))return Integer.toString(effect instanceof ApplyStatusEffectSpec?fixed(((ApplyStatusEffectSpec)effect).intensity()):1);
		if("duration_turns".equals(field)){DurationSpec duration=effect instanceof ApplyStatusEffectSpec?((ApplyStatusEffectSpec)effect).duration():effect instanceof TemporaryHpEffectSpec?((TemporaryHpEffectSpec)effect).duration():effect instanceof MitigateEffectSpec?((MitigateEffectSpec)effect).duration():effect instanceof RedirectDamageEffectSpec?((RedirectDamageEffectSpec)effect).duration():null;return Integer.toString(duration==null?3:duration.turns());}
		if("recipient".equals(field))return effect instanceof RedirectDamageEffectSpec?((RedirectDamageEffectSpec)effect).recipient().name():"CLASS_OWNER";
		if("distance".equals(field)){if(effect instanceof PushEffectSpec)return Integer.toString(((PushEffectSpec)effect).distance());if(effect instanceof PullEffectSpec)return Integer.toString(((PullEffectSpec)effect).distance());if(effect instanceof ThrowEffectSpec)return Integer.toString(((ThrowEffectSpec)effect).distance());return "1";}
		if("maximum_distance".equals(field))return Integer.toString(effect instanceof DashEffectSpec?((DashEffectSpec)effect).maximumDistance():3);
		if("maximum_range".equals(field))return Integer.toString(effect instanceof TeleportEffectSpec?((TeleportEffectSpec)effect).maximumRange():3);
		if("maximum_count".equals(field))return Integer.toString(effect instanceof CleanseEffectSpec?((CleanseEffectSpec)effect).maximumCount():1);
		return "1";
	}
	private static String modifierValue(ModifierSpec modifier,String field){if("repeat_count".equals(field))return Integer.toString(modifier instanceof RepeatModifierSpec?((RepeatModifierSpec)modifier).repeatCount():1);if("intensity_numerator".equals(field))return Integer.toString(modifier instanceof IntensityModifierSpec?((IntensityModifierSpec)modifier).numerator():2);if("intensity_denominator".equals(field))return Integer.toString(modifier instanceof IntensityModifierSpec?((IntensityModifierSpec)modifier).denominator():1);if("additional_turns".equals(field))return Integer.toString(modifier instanceof ExtendDurationModifierSpec?((ExtendDurationModifierSpec)modifier).additionalTurns():1);if("additional_targets".equals(field))return Integer.toString(modifier instanceof PierceModifierSpec?((PierceModifierSpec)modifier).additionalTargets():1);if("bounces".equals(field))return Integer.toString(modifier instanceof BounceModifierSpec?((BounceModifierSpec)modifier).bounces():1);if("bounce_range".equals(field))return Integer.toString(modifier instanceof BounceModifierSpec?((BounceModifierSpec)modifier).bounceRange():2);throw new IllegalArgumentException("unknown modifier form field "+field);}
	private static String filterVariant(EntityFilterExpr filter){if(filter instanceof RelationFilterSpec){RelationFilterSpec relation=(RelationFilterSpec)filter;return relation.relationToClassOwner()==RelationFilterSpec.RelationAlignment.ENEMY?"RELATION_ENEMY_EXCLUDE_SELF":relation.includeSelf()?"RELATION_ALLY_INCLUDE_SELF":"RELATION_ALLY_EXCLUDE_SELF";}return filter.variantKey();}
	private static int fixed(ValueSpec value){if(value instanceof FixedValueSpec)return ((FixedValueSpec)value).value();if(value instanceof ScaledValueSpec)return ((ScaledValueSpec)value).base();return 0;}

	private static TypedRef currentReference(StableTarget owner, String key) {
		if (owner instanceof ModeSpec && "group".equals(key)) return ((ModeSpec) owner).group();
		if (owner instanceof EntitySpec && "capacity".equals(key)) return ((EntitySpec) owner).capacity();
		if(owner instanceof SkillSpec){SkillSpec skill=(SkillSpec)owner;if("condition_resource".equals(key)&&conditionLeaf(skill) instanceof ResourceCompareCondition)return ((ResourceCompareCondition)conditionLeaf(skill)).resource();if("cost_resource".equals(key)&&skill.cost() instanceof ResourceCostSpec)return ((ResourceCostSpec)skill.cost()).resource();if(key.startsWith("effect_primary_operation_")&&skill.effects().primary() instanceof ResourceOperationEffectSpec)return operationReference(((ResourceOperationEffectSpec)skill.effects().primary()).operation(),"operation."+key.substring("effect_primary_operation_".length()));if(key.startsWith("effect_secondary_operation_")&&skill.effects().secondary()!=null&&skill.effects().secondary().effect() instanceof ResourceOperationEffectSpec)return operationReference(((ResourceOperationEffectSpec)skill.effects().secondary().effect()).operation(),"operation."+key.substring("effect_secondary_operation_".length()));}
		if(owner instanceof ResourceFlowComponentSpec)return operationReference(((ResourceFlowComponentSpec)owner).operation(),key);
		if(owner instanceof ActiveResourceOperationComponentSpec){ActiveResourceOperationComponentSpec value=(ActiveResourceOperationComponentSpec)owner;if("cost.resource".equals(key)&&value.cost() instanceof ResourceCostSpec)return ((ResourceCostSpec)value.cost()).resource();return operationReference(value.operation(),key);}
		if(owner instanceof ResourceClassOperationSpec){ResourceClassOperationSpec value=(ResourceClassOperationSpec)owner;if("cost.resource".equals(key)&&value.cost() instanceof ResourceCostSpec)return ((ResourceCostSpec)value.cost()).resource();return operationReference(value.operation(),key);}
		return null;
	}

	private static TypedRef typedRef(RefKind kind, String targetId, String displayName) {
		StableId id = StableId.fromStored(targetId);
		switch (kind) {
			case RESOURCE: return new ResourceRef(id, displayName);
			case MARK: return new MarkRef(id, displayName);
			case MODE_GROUP: return new ModeGroupRef(id, displayName);
			case MODE: return new ModeRef(id, displayName);
			case ENTITY: return new EntitySpecRef(id, displayName);
			case CAPACITY: return new CapacityRef(id, displayName);
			case COMPONENT: return new ComponentRef(id, displayName);
			case ABILITY_POOL: return new AbilityPoolRef(id, displayName);
			case PROPERTY: return new PropertyRef(id, displayName);
			case RECIPE: return new SynthesisRecipeRef(id, displayName);
			default: throw new AssertionError(kind);
		}
	}

	private StableTarget requireTarget(String targetId) {
		StableId id = StableId.fromStored(targetId);
		for (StableTarget target : state().draft().allTargets()) if (target.id().equals(id)) return target;
		throw new IllegalArgumentException("declaration not found: " + targetId);
	}

	private static String variantOf(StableTarget target) {
		if(target instanceof ResourceSpec)return V6FormSchemas.RESOURCE;if(target instanceof MarkSpec)return V6FormSchemas.MARK;if(target instanceof ModeGroupSpec)return V6FormSchemas.MODE_GROUP;if(target instanceof ModeSpec)return V6FormSchemas.MODE;if(target instanceof EntityCapacitySpec)return V6FormSchemas.ENTITY_CAPACITY;if(target instanceof EntitySpec)return V6FormSchemas.ENTITY;if(target instanceof AbilityPoolSpec)return V6FormSchemas.ABILITY_POOL;if(target instanceof PropertySpec)return V6FormSchemas.PROPERTY;if(target instanceof SynthesisRecipeSpec)return V6FormSchemas.RECIPE;if(target instanceof SkillSpec)return ((SkillSpec)target).typed()?V6FormSchemas.SKILL:V6FormSchemas.CONTRACT_NODE;if(target instanceof BasicAttackComponentSpec)return V6FormSchemas.BASIC_ATTACK;if(target instanceof ResourceFlowComponentSpec)return V6FormSchemas.RESOURCE_FLOW;if(target instanceof ActiveResourceOperationComponentSpec)return V6FormSchemas.ACTIVE_RESOURCE_OPERATION;if(target instanceof ResourceClassOperationSpec)return V6FormSchemas.RESOURCE_CLASS_OPERATION;if(target instanceof ContractNodeSpec)return V6FormSchemas.CONTRACT_NODE;throw new IllegalArgumentException("unknown declaration type "+target.getClass().getName());
	}

	private static String operationValue(ResourceOperationSpec operation,String key){if("operation_variant".equals(key)||"operation.variant".equals(key))return operation.variant().name();if("operation.amount".equals(key)){if(operation instanceof GainResourceSpec)return Integer.toString(fixed(((GainResourceSpec)operation).amount()));if(operation instanceof DrainResourceSpec)return Integer.toString(fixed(((DrainResourceSpec)operation).amount()));if(operation instanceof SetResourceSpec)return Integer.toString(fixed(((SetResourceSpec)operation).value()));if(operation instanceof ReserveResourceSpec)return Integer.toString(((ReserveResourceSpec)operation).amount());return "1";}if("operation.source_amount".equals(key))return Integer.toString(operation instanceof ConvertResourceSpec?((ConvertResourceSpec)operation).sourceAmount():1);if("operation.target_amount".equals(key))return Integer.toString(operation instanceof ConvertResourceSpec?((ConvertResourceSpec)operation).targetAmount():1);if("operation.duration_turns".equals(key)){if(operation instanceof ReserveResourceSpec)return Integer.toString(((ReserveResourceSpec)operation).durationTurns());if(operation instanceof SuppressResourceSpec)return Integer.toString(((SuppressResourceSpec)operation).durationTurns());return "1";}if("operation.resource".equals(key)||"operation.source".equals(key)||"operation.target".equals(key)){TypedRef ref=operationReference(operation,key);return ref==null?"None":ref.targetId().value();}return null;}
	private static TypedRef operationReference(ResourceOperationSpec operation,String key){if("operation.source".equals(key)&&operation instanceof ConvertResourceSpec)return ((ConvertResourceSpec)operation).source();if("operation.target".equals(key)&&operation instanceof ConvertResourceSpec)return ((ConvertResourceSpec)operation).target();if("operation.resource".equals(key)&&!(operation instanceof ConvertResourceSpec)&&!operation.referencedResources().isEmpty())return operation.referencedResources().get(0);return null;}
	private static String costAmount(CostSpec cost){if(cost instanceof ResourceCostSpec)return Integer.toString(((ResourceCostSpec)cost).amount());if(cost instanceof HpCostSpec)return Integer.toString(((HpCostSpec)cost).amount());if(cost instanceof ActionTimeCostSpec)return Integer.toString(((ActionTimeCostSpec)cost).turns());if(cost instanceof CooldownCostSpec)return Integer.toString(((CooldownCostSpec)cost).turns());return "1";}

	private static BuilderCommand skillCommand(SkillSpec skill,String key,String value){String id=skill.id().value();
		if("display_name".equals(key))return new BuilderCommand.RenameDeclaration(id,value);
		if("activation_variant".equals(key))return new BuilderCommand.SelectTriggerVariant(id,value);
		if("condition_variant".equals(key))return new BuilderCommand.SelectConditionVariant(id,value);
		if(key.startsWith("condition_")&&!"condition_resource".equals(key)){ConditionExpr leaf=conditionLeaf(skill);if(leaf==null)throw new IllegalArgumentException("condition is not configured");String field=key.substring("condition_".length());if("builtin_stat".equals(field))field="stat";return new BuilderCommand.SetTypedSkillField(id,"condition.0",leaf.variantKey(),field,value);}
		if("effect_primary_family".equals(key))return new BuilderCommand.SelectEffectFamily(id,"PRIMARY",value);
		if("effect_primary_variant".equals(key))return new BuilderCommand.SelectEffectVariant(id,"PRIMARY",value);
		if(key.startsWith("effect_primary_"))return effectFieldCommand(id,"effects.primary",skill.effects().primary(),key.substring("effect_primary_".length()),value);
		if("effect_secondary_family".equals(key))return new BuilderCommand.SelectEffectFamily(id,"IMMEDIATE_SECONDARY",value);
		if("effect_secondary_variant".equals(key))return new BuilderCommand.SelectEffectVariant(id,"IMMEDIATE_SECONDARY",value);
		if(key.startsWith("effect_secondary_")&&!"effect_secondary_activation".equals(key)){if(skill.effects().secondary()==null)throw new IllegalArgumentException("secondary effect absent");return effectFieldCommand(id,"effects.secondary",skill.effects().secondary().effect(),key.substring("effect_secondary_".length()),value);}
		if("delivery_variant".equals(key))return new BuilderCommand.SetDelivery(id,value);
		if("delivery_requires_line_of_sight".equals(key))return new BuilderCommand.SetTypedSkillField(id,"delivery","DIRECT","requires_line_of_sight",value);
		if("delivery_trace_width".equals(key))return new BuilderCommand.SetTypedSkillField(id,"delivery","TRACE","width",value);
		if("delivery_trace_stops_at_blocking_cell".equals(key))return new BuilderCommand.SetTypedSkillField(id,"delivery","TRACE","stops_at_first_blocking_cell",value);
		if("delivery_ground_requires_visible_cell".equals(key))return new BuilderCommand.SetTypedSkillField(id,"delivery","GROUND","requires_visible_cell",value);
		if("targeting_selector".equals(key))return new BuilderCommand.SetTargetingSelector(id,value);
		if("targeting_coverage".equals(key))return new BuilderCommand.SetTargetingCoverage(id,value);
		if("targeting_filter".equals(key))return new BuilderCommand.SetTargetingFilter(id,value);
		if("targeting_range".equals(key))return new BuilderCommand.SetTypedSkillField(id,"targeting","TARGETING","range",value);
		if("targeting_maximum_targets".equals(key))return new BuilderCommand.SetTypedSkillField(id,"targeting","TARGETING","maximum_targets",value);
		if("targeting_radius".equals(key))return new BuilderCommand.SetTypedSkillField(id,"targeting","TARGETING","radius",value);
		if("targeting_line_length".equals(key))return new BuilderCommand.SetTypedSkillField(id,"targeting","TARGETING","line_length",value);
		if("targeting_line_width".equals(key))return new BuilderCommand.SetTypedSkillField(id,"targeting","TARGETING","line_width",value);
		if("modifier_variant".equals(key))return new BuilderCommand.SetModifier(id,value);
		if(key.startsWith("modifier_")){String field=key.substring("modifier_".length());if(field.startsWith("intensity_"))field=field.substring("intensity_".length());return new BuilderCommand.SetTypedSkillField(id,"modifier",skill.modifier().variantKey(),field,value);}
		if("cost_variant".equals(key))return new BuilderCommand.SetCost(id,value);
		if("cost_amount".equals(key)){String field=skill.cost() instanceof ActionTimeCostSpec||skill.cost() instanceof CooldownCostSpec?"turns":skill.cost() instanceof ItemCostSpec?"count":"amount";return new BuilderCommand.SetTypedSkillField(id,"cost",skill.cost().variantKey(),field,value);}
		if("cost_item_category".equals(key))return new BuilderCommand.SetTypedSkillField(id,"cost",skill.cost().variantKey(),"item_category",value);
		if("constraint_variant".equals(key))return new BuilderCommand.SetSkillConstraint(id,value);
		throw new IllegalArgumentException("P04 skill field is read-only: "+key);
	}
	private static BuilderCommand effectFieldCommand(String id,String path,EffectSpec effect,String field,String value){if(effect==null)throw new IllegalArgumentException("effect absent");if(field.startsWith("operation_")){String operationField=field.substring("operation_".length());field="variant".equals(operationField)?"operation_variant":operationField;}return new BuilderCommand.SetTypedSkillField(id,path,effect.variantKey().name(),field,value);}
	private static BuilderCommand skillReferenceCommand(SkillSpec skill,String key,TypedRef reference){String id=skill.id().value();if("condition_resource".equals(key))return new BuilderCommand.SetTypedSkillReference(id,"condition.0","RESOURCE_COMPARE","resource",reference);if("cost_resource".equals(key))return new BuilderCommand.SetTypedSkillReference(id,"cost","RESOURCE","resource",reference);if(key.startsWith("effect_primary_operation_"))return new BuilderCommand.SetTypedSkillReference(id,"effects.primary","RESOURCE_OPERATION",key.substring("effect_primary_operation_".length()),reference);if(key.startsWith("effect_secondary_operation_"))return new BuilderCommand.SetTypedSkillReference(id,"effects.secondary","RESOURCE_OPERATION",key.substring("effect_secondary_operation_".length()),reference);throw new IllegalArgumentException("unknown P04 skill reference field "+key);}

	private static String disabledSkillField(SkillSpec skill,String key){
		ConditionExpr leaf=conditionLeaf(skill);if(key.startsWith("condition_")&&!"condition_variant".equals(key)){if("condition_resource".equals(key)&&!(leaf instanceof ResourceCompareCondition))return "Choose RESOURCE_COMPARE first.";if(("condition_subject".equals(key)||"condition_builtin_stat".equals(key))&&!(leaf instanceof BuiltinStatCompareCondition))return "Choose BUILTIN_STAT_COMPARE first.";if(("condition_operator".equals(key)||"condition_value".equals(key))&&!(leaf instanceof BuiltinStatCompareCondition)&&!(leaf instanceof ResourceCompareCondition))return "Choose a typed condition first.";}
		if("effect_primary_variant".equals(key)&&skill.effects().primary().family()==null)return "Choose an effect family first.";
		if(key.startsWith("effect_primary_")&&!"effect_primary_family".equals(key)&&!"effect_primary_variant".equals(key)&&!effectParameterApplies(skill.effects().primary(),key.substring("effect_primary_".length())))return "This parameter does not belong to the selected primary effect.";
		if("effect_secondary_variant".equals(key)&&(skill.effects().secondary()==null||skill.effects().secondary().effect().family()==null))return "Choose a secondary effect family first.";
		if("effect_secondary_activation".equals(key)&&skill.effects().secondary()==null)return "Choose a secondary effect first.";
		if(key.startsWith("effect_secondary_")&&!"effect_secondary_family".equals(key)&&!"effect_secondary_variant".equals(key)&&!"effect_secondary_activation".equals(key)&&(skill.effects().secondary()==null||!effectParameterApplies(skill.effects().secondary().effect(),key.substring("effect_secondary_".length()))))return "This parameter does not belong to the selected secondary effect.";
		if("delivery_requires_line_of_sight".equals(key)&&!(skill.delivery() instanceof DirectDeliverySpec))return "Choose DIRECT delivery first.";
		if(("delivery_trace_width".equals(key)||"delivery_trace_stops_at_blocking_cell".equals(key))&&!(skill.delivery() instanceof TraceDeliverySpec))return "Choose TRACE delivery first.";
		if("delivery_ground_requires_visible_cell".equals(key)&&!(skill.delivery() instanceof GroundDeliverySpec))return "Choose GROUND delivery first.";
		if("targeting_radius".equals(key)&&!(skill.targeting().coverage() instanceof RadiusCoverageSpec))return "Choose RADIUS coverage first.";
		if(("targeting_line_length".equals(key)||"targeting_line_width".equals(key))&&!(skill.targeting().coverage() instanceof LineCoverageSpec))return "Choose LINE coverage first.";
		if(key.startsWith("modifier_")&&!"modifier_variant".equals(key)&&!modifierParameterApplies(skill.modifier(),key.substring("modifier_".length())))return "This parameter does not belong to the selected modifier.";
		if("cost_resource".equals(key)&&!(skill.cost() instanceof ResourceCostSpec))return "Choose RESOURCE cost first.";
		if("cost_amount".equals(key)&&(skill.cost() instanceof NoCostSpec))return "Choose a priced cost first.";
		if("cost_item_category".equals(key)&&!(skill.cost() instanceof ItemCostSpec))return "Choose ITEM cost first.";
		return null;
	}
	private static boolean effectParameterApplies(EffectSpec effect,String field){if(effect==null||effect instanceof UnconfiguredEffectSpec)return false;if(field.startsWith("operation_"))return effect instanceof ResourceOperationEffectSpec;if("amount".equals(field))return effect instanceof DirectDamageEffectSpec||effect instanceof HealEffectSpec||effect instanceof BarrierEffectSpec||effect instanceof TemporaryHpEffectSpec;if("damage_type".equals(field))return effect instanceof DirectDamageEffectSpec||effect instanceof MissingHpDamageEffectSpec;if("defense_policy".equals(field))return effect instanceof DirectDamageEffectSpec;if("percent".equals(field))return effect instanceof PercentMaxHpDamageEffectSpec||effect instanceof MitigateEffectSpec||effect instanceof RedirectDamageEffectSpec;if("absolute_cap".equals(field))return effect instanceof PercentMaxHpDamageEffectSpec||effect instanceof MissingHpDamageEffectSpec;if("base_amount".equals(field)||"missing_hp_numerator".equals(field)||"missing_hp_denominator".equals(field))return effect instanceof MissingHpDamageEffectSpec;if("hp_percent_threshold".equals(field))return effect instanceof ExecuteEffectSpec;if("status".equals(field)||"intensity".equals(field))return effect instanceof ApplyStatusEffectSpec;if("duration_turns".equals(field))return effect instanceof ApplyStatusEffectSpec||effect instanceof TemporaryHpEffectSpec||effect instanceof MitigateEffectSpec||effect instanceof RedirectDamageEffectSpec;if("recipient".equals(field))return effect instanceof RedirectDamageEffectSpec;if("distance".equals(field))return effect instanceof PushEffectSpec||effect instanceof PullEffectSpec||effect instanceof ThrowEffectSpec;if("maximum_distance".equals(field))return effect instanceof DashEffectSpec;if("maximum_range".equals(field))return effect instanceof TeleportEffectSpec;if("maximum_count".equals(field))return effect instanceof CleanseEffectSpec;return false;}
	private static boolean modifierParameterApplies(ModifierSpec modifier,String field){return "repeat_count".equals(field)&&modifier instanceof RepeatModifierSpec||("intensity_numerator".equals(field)||"intensity_denominator".equals(field))&&modifier instanceof IntensityModifierSpec||"additional_turns".equals(field)&&modifier instanceof ExtendDurationModifierSpec||"additional_targets".equals(field)&&modifier instanceof PierceModifierSpec||("bounces".equals(field)||"bounce_range".equals(field))&&modifier instanceof BounceModifierSpec;}
	private static List<String> skillEnumOptions(SkillSpec skill,String key,List<String> options){if("effect_primary_variant".equals(key))return effectVariantOptions(skill.effects().primary().family(),options);if("effect_secondary_variant".equals(key))return effectVariantOptions(skill.effects().secondary()==null?null:skill.effects().secondary().effect().family(),options);return options;}
	private static List<String> effectVariantOptions(EffectFamily family,List<String> options){if(family==null)return Collections.emptyList();List<String> result=new ArrayList<>();for(String option:options)try{EffectVariantKey key=EffectVariantKey.valueOf(option);if(key!=EffectVariantKey.UNCONFIGURED&&effectFamily(key)==family)result.add(option);}catch(IllegalArgumentException ignored){}return result;}
	private static EffectFamily effectFamily(EffectVariantKey key){switch(key){case DIRECT_DAMAGE:case PERCENT_MAX_HP_DAMAGE:case MISSING_HP_DAMAGE:case EXECUTE:return EffectFamily.DAMAGE;case APPLY_STATUS:return EffectFamily.STATUS;case PUSH:case PULL:case THROW:case DASH:case TELEPORT:case SWAP_POSITION:return EffectFamily.MOVEMENT;case HEAL:case BARRIER:case TEMPORARY_HP:case MITIGATE:case REDIRECT_DAMAGE:case CLEANSE:return EffectFamily.RECOVERY_DEFENSE;case RESOURCE_OPERATION:return EffectFamily.RESOURCE_OPERATION;default:return null;}}
	private static String disabledClassNodeField(StableTarget owner,String key){ResourceOperationSpec operation=null;CostSpec cost=null;if(owner instanceof ResourceFlowComponentSpec)operation=((ResourceFlowComponentSpec)owner).operation();else if(owner instanceof ActiveResourceOperationComponentSpec){operation=((ActiveResourceOperationComponentSpec)owner).operation();cost=((ActiveResourceOperationComponentSpec)owner).cost();}else if(owner instanceof ResourceClassOperationSpec){operation=((ResourceClassOperationSpec)owner).operation();cost=((ResourceClassOperationSpec)owner).cost();}else return null;if(key.startsWith("operation.")){String field=key.substring("operation.".length());boolean convert=operation instanceof ConvertResourceSpec;if(("source".equals(field)||"target".equals(field)||"source_amount".equals(field)||"target_amount".equals(field))&&!convert)return "Choose CONVERT first.";if("resource".equals(field)&&convert)return "CONVERT uses explicit source and target references.";if("amount".equals(field)&&(operation instanceof ClearResourceSpec||operation instanceof SuppressResourceSpec||convert))return "The selected operation has no single amount field.";if("duration_turns".equals(field)&&!(operation instanceof ReserveResourceSpec)&&!(operation instanceof SuppressResourceSpec))return "Choose RESERVE or SUPPRESS first.";}if("cost.resource".equals(key)&&!(cost instanceof ResourceCostSpec))return "Choose RESOURCE cost first.";if("cost_amount".equals(key)&&(cost==null||cost instanceof NoCostSpec))return "Choose a priced cost first.";return null;}

	private static Set<String> csv(String value) {
		LinkedHashSet<String> result = new LinkedHashSet<>();
		if (value == null || value.isEmpty()) return result;
		for (String part : value.split(",")) result.add(part.trim());
		return result;
	}

	private static String entityTypes(Set<EntityType> values) {
		List<String> result = new ArrayList<>();
		for (EntityType type : EntityType.values()) if (values.contains(type)) result.add(type.name());
		return String.join(",", result);
	}

	private static String firstEnabled(List<Choice> choices) {
		for (Choice choice : choices) if (choice.enabled()) return choice.value();
		return null;
	}

	private static String alternateText(String value, int maximumCodePoints) {
		if (value.isEmpty()) return "x";
		if (value.codePointCount(0, value.length()) < maximumCodePoints) return value + "*";
		int end = value.offsetByCodePoints(0, value.codePointCount(0, value.length()) - 1);
		return value.substring(0, end) + (value.endsWith("x") ? "y" : "x");
	}

	private static String shortId(String value) {
		return value.length() <= 12 ? value : value.substring(value.length() - 8);
	}
}
