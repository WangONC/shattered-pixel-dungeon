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
		if (owner instanceof SkillSpec) return skillCommand((SkillSpec) owner, fieldKey, rawValue);
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

		switch (schema.kind()) {
			case TEXT:
				return enabled(schema, value, Collections.<Choice>emptyList(), alternateText(value,
						((TextFieldSchema) schema).maximumCodePoints()));
			case NUMBER:
				return numberField(owner, variant, (NumberFieldSchema) schema, value);
			case ENUM:
				return enumField(schema, value, ((EnumFieldSchema) schema).optionKeys());
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
			if("condition_variant".equals(key))return v.condition() instanceof AllOfCondition&&((AllOfCondition)v.condition()).children().isEmpty()?"ALWAYS":v.condition().variantKey();
			if("effect_primary_family".equals(key))return v.effects().primary().family()==null?"UNCONFIGURED":v.effects().primary().family().name();
			if("effect_primary_variant".equals(key))return v.effects().primary().variantKey().name();
			if("effect_primary_amount".equals(key))return v.effects().primary() instanceof DirectDamageEffectSpec?Integer.toString(((FixedValueSpec)((DirectDamageEffectSpec)v.effects().primary()).amount()).value()):"1";
			if("effect_primary_damage_type".equals(key))return v.effects().primary() instanceof DirectDamageEffectSpec?((DirectDamageEffectSpec)v.effects().primary()).damageType().name():"UNCONFIGURED";
			if("effect_primary_defense_policy".equals(key))return v.effects().primary() instanceof DirectDamageEffectSpec?((DirectDamageEffectSpec)v.effects().primary()).defensePolicy().name():"UNCONFIGURED";
			if("effect_secondary_family".equals(key))return v.effects().secondary()==null?"NONE":v.effects().secondary().effect().family()==null?"UNCONFIGURED":v.effects().secondary().effect().family().name();
			if("effect_secondary_variant".equals(key))return v.effects().secondary()==null?"UNCONFIGURED":v.effects().secondary().effect().variantKey().name();
			if("effect_secondary_amount".equals(key))return v.effects().secondary()!=null&&v.effects().secondary().effect() instanceof DirectDamageEffectSpec?Integer.toString(((FixedValueSpec)((DirectDamageEffectSpec)v.effects().secondary().effect()).amount()).value()):"1";
			if("effect_secondary_activation".equals(key))return v.effects().secondary()==null?"UNCONFIGURED":v.effects().secondary().activation().variantKey();
			if("delivery_variant".equals(key))return v.delivery().variantKey();
			if("delivery_requires_line_of_sight".equals(key))return Boolean.toString(v.delivery() instanceof DirectDeliverySpec&&((DirectDeliverySpec)v.delivery()).requiresLineOfSight());
			if("targeting_selector".equals(key))return v.targeting().selector().variantKey();
			if("targeting_coverage".equals(key))return v.targeting().coverage().variantKey();
			if("targeting_filter".equals(key))return v.targeting().filter() instanceof RelationFilterSpec?"RELATION_ENEMY_EXCLUDE_SELF":v.targeting().filter().variantKey();
			if("targeting_range".equals(key))return Integer.toString(v.targeting().range());
			if("targeting_maximum_targets".equals(key))return Integer.toString(v.targeting().maximumTargets());
			if("targeting_line_of_sight".equals(key))return v.targeting().lineOfSight().name();
			if("targeting_ordering".equals(key))return v.targeting().ordering().name();
			if("modifier_variant".equals(key))return v.modifier()==null?"NONE":v.modifier().variantKey();
			if("cost_variant".equals(key))return v.cost().variantKey();
			if("constraint_variant".equals(key))return v.constraint()==null?"NONE":v.constraint().variantKey();
		} else if (owner instanceof ContractNodeSpec) {
			if("variant_key".equals(key))return ((ContractNodeSpec)owner).variantKey();
		}
		if ("diagnostics".equals(key)) return "";
		throw new IllegalArgumentException("unknown form field value: " + owner.getClass().getSimpleName() + "." + key);
	}

	private static TypedRef currentReference(StableTarget owner, String key) {
		if (owner instanceof ModeSpec && "group".equals(key)) return ((ModeSpec) owner).group();
		if (owner instanceof EntitySpec && "capacity".equals(key)) return ((EntitySpec) owner).capacity();
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
		if(target instanceof ResourceSpec)return V6FormSchemas.RESOURCE;if(target instanceof MarkSpec)return V6FormSchemas.MARK;if(target instanceof ModeGroupSpec)return V6FormSchemas.MODE_GROUP;if(target instanceof ModeSpec)return V6FormSchemas.MODE;if(target instanceof EntityCapacitySpec)return V6FormSchemas.ENTITY_CAPACITY;if(target instanceof EntitySpec)return V6FormSchemas.ENTITY;if(target instanceof AbilityPoolSpec)return V6FormSchemas.ABILITY_POOL;if(target instanceof PropertySpec)return V6FormSchemas.PROPERTY;if(target instanceof SynthesisRecipeSpec)return V6FormSchemas.RECIPE;if(target instanceof SkillSpec)return ((SkillSpec)target).typed()?V6FormSchemas.SKILL:V6FormSchemas.CONTRACT_NODE;if(target instanceof ContractNodeSpec)return V6FormSchemas.CONTRACT_NODE;throw new IllegalArgumentException("unknown declaration type "+target.getClass().getName());
	}

	private static BuilderCommand skillCommand(SkillSpec skill,String key,String value){String id=skill.id().value();
		if("display_name".equals(key))return new BuilderCommand.RenameDeclaration(id,value);
		if("activation_variant".equals(key))return new BuilderCommand.SelectTriggerVariant(id,value);
		if("condition_variant".equals(key))return new BuilderCommand.SelectConditionVariant(id,value);
		if("effect_primary_family".equals(key))return new BuilderCommand.SelectEffectFamily(id,"PRIMARY",value);
		if("effect_primary_variant".equals(key))return new BuilderCommand.SelectEffectVariant(id,"PRIMARY",value);
		if("effect_primary_amount".equals(key))return new BuilderCommand.SetTypedSkillField(id,"effects.primary","DIRECT_DAMAGE","amount",value);
		if("effect_primary_damage_type".equals(key))return new BuilderCommand.SetTypedSkillField(id,"effects.primary","DIRECT_DAMAGE","damage_type",value);
		if("effect_primary_defense_policy".equals(key))return new BuilderCommand.SetTypedSkillField(id,"effects.primary","DIRECT_DAMAGE","defense_policy",value);
		if("effect_secondary_family".equals(key))return new BuilderCommand.SelectEffectFamily(id,"IMMEDIATE_SECONDARY",value);
		if("effect_secondary_variant".equals(key))return new BuilderCommand.SelectEffectVariant(id,"IMMEDIATE_SECONDARY",value);
		if("effect_secondary_amount".equals(key))return new BuilderCommand.SetTypedSkillField(id,"effects.secondary","DIRECT_DAMAGE","amount",value);
		if("delivery_variant".equals(key))return new BuilderCommand.SetDelivery(id,value);
		if("delivery_requires_line_of_sight".equals(key))return new BuilderCommand.SetTypedSkillField(id,"delivery","DIRECT","requires_line_of_sight",value);
		if("targeting_selector".equals(key))return new BuilderCommand.SetTargetingSelector(id,value);
		if("targeting_coverage".equals(key))return new BuilderCommand.SetTargetingCoverage(id,value);
		if("targeting_filter".equals(key))return new BuilderCommand.SetTargetingFilter(id,value);
		if("targeting_range".equals(key))return new BuilderCommand.SetTypedSkillField(id,"targeting","TARGETING","range",value);
		if("targeting_maximum_targets".equals(key))return new BuilderCommand.SetTypedSkillField(id,"targeting","TARGETING","maximum_targets",value);
		if("modifier_variant".equals(key))return new BuilderCommand.SetModifier(id,value);
		if("cost_variant".equals(key))return new BuilderCommand.SetCost(id,value);
		if("constraint_variant".equals(key))return new BuilderCommand.SetSkillConstraint(id,value);
		throw new IllegalArgumentException("P03 skill field is read-only: "+key);
	}

	private static String disabledSkillField(SkillSpec skill,String key){
		if("effect_primary_variant".equals(key)&&skill.effects().primary().family()!=EffectFamily.DAMAGE)return "Choose the DAMAGE family first.";
		if(("effect_primary_amount".equals(key)||"effect_primary_damage_type".equals(key)||"effect_primary_defense_policy".equals(key))&&!(skill.effects().primary() instanceof DirectDamageEffectSpec))return "Choose DIRECT_DAMAGE first.";
		if("effect_secondary_variant".equals(key)&&(skill.effects().secondary()==null||skill.effects().secondary().effect().family()!=EffectFamily.DAMAGE))return "Choose the secondary DAMAGE family first.";
		if(("effect_secondary_amount".equals(key)||"effect_secondary_activation".equals(key))&&(skill.effects().secondary()==null||!(skill.effects().secondary().effect() instanceof DirectDamageEffectSpec)))return "Choose the immediate secondary DIRECT_DAMAGE first.";
		if("delivery_requires_line_of_sight".equals(key)&&!(skill.delivery() instanceof DirectDeliverySpec))return "Choose DIRECT delivery first.";
		return null;
	}

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
