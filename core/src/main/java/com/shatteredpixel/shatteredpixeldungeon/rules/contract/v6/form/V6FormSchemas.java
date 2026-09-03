package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.AbilityPoolSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntityCapacitySpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntityType;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.MarkSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeGroupSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.PropertySpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ResourceSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Closed P02 declaration-form catalog. It contains metadata, never declaration snapshots. */
public final class V6FormSchemas {
	public static final String RESOURCE = "RESOURCE";
	public static final String MARK = "MARK";
	public static final String MODE_GROUP = "MODE_GROUP";
	public static final String MODE = "MODE";
	public static final String ENTITY_CAPACITY = "ENTITY_CAPACITY";
	public static final String ENTITY = "ENTITY";
	public static final String ABILITY_POOL = "ABILITY_POOL";
	public static final String PROPERTY = "PROPERTY";
	public static final String RECIPE = "RECIPE";
	public static final String CONTRACT_NODE = "CONTRACT_NODE";
	public static final String SKILL = "SKILL_V0_2";
	public static final String BASIC_ATTACK = "BASIC_ATTACK";
	public static final String RESOURCE_FLOW = "RESOURCE_FLOW";
	public static final String ACTIVE_RESOURCE_OPERATION = "ACTIVE_RESOURCE_OPERATION";
	public static final String RESOURCE_CLASS_OPERATION = "RESOURCE_OPERATION";

	private static final Map<String, FormSchema> SCHEMAS = createSchemas();

	private V6FormSchemas() {}

	public static FormSchema require(String variantKey) {
		FormSchema result = SCHEMAS.get(variantKey);
		if (result == null) throw new IllegalArgumentException("unknown P02 form variant " + variantKey);
		return result;
	}

	public static List<FormSchema> all() {
		return Collections.unmodifiableList(new ArrayList<>(SCHEMAS.values()));
	}

	private static Map<String, FormSchema> createSchemas() {
		Map<String, FormSchema> result = new LinkedHashMap<>();
		result.put(RESOURCE, schema(RESOURCE,
				text("display_name"), number("minimum", -999, 999, 1), number("maximum", -998, 1000, 1),
				number("initial_value", -999, 1000, 1), enumeration("default_overflow_policy", ResourceSpec.ResourceOverflowPolicy.values()),
				bool("hud.visible"), number("hud.order", 0, 99, 1), new TextFieldSchema("hud.presentation_key", label("hud.presentation_key"), false, 64), diagnostic()));
		result.put(MARK, schema(MARK,
				text("display_name"), enumeration("kind", MarkSpec.MarkKind.values()), number("minimum", -999, 999, 1),
				number("maximum", -998, 1000, 1), number("initial_value", -999, 1000, 1),
				enumeration("duration_policy", MarkSpec.MarkDurationPolicy.values()), number("default_duration_turns", 0, 999, 1),
				enumeration("refresh_policy", MarkSpec.MarkRefreshPolicy.values()), enumeration("overflow_policy", MarkSpec.MarkOverflowPolicy.values()),
				enumeration("provenance_policy", MarkSpec.MarkProvenancePolicy.values()), diagnostic()));
		result.put(MODE_GROUP, schema(MODE_GROUP, text("display_name"),
				enumeration("policy", ModeGroupSpec.ModeGroupPolicy.values()), diagnostic()));
		result.put(MODE, schema(MODE, text("display_name"), reference("group", RefKind.MODE_GROUP, "all_mode_groups"),
				bool("initial"), enumeration("duration_policy", ModeSpec.ModeDurationPolicy.values()),
				number("default_duration_turns", 0, 999, 1), diagnostic()));
		result.put(ENTITY_CAPACITY, schema(ENTITY_CAPACITY, text("display_name"),
				enumerationList("entity_types", EntityType.values()), number("maximum", 1, 99, 1),
				enumeration("overflow_policy", EntityCapacitySpec.CapacityOverflowPolicy.values()), diagnostic()));
		result.put(ENTITY, schema(ENTITY, text("display_name"), enumeration("entity_type", EntityType.values()),
				new ReferenceFieldSchema("capacity", label("capacity"), false, RefKind.CAPACITY, "compatible_entity_capacity"),
				new NestedVariantFieldSchema("facets", label("facets"), false, Collections.<String>emptyList()),
				new ListFieldSchema("capabilities", label("capabilities"), false,
						new NestedVariantFieldSchema("capability", label("capability"), false, Collections.<String>emptyList()), 32), diagnostic()));
		result.put(ABILITY_POOL, schema(ABILITY_POOL, text("display_name"), number("capacity", 1, 99, 1),
				enumeration("overflow_policy", AbilityPoolSpec.AbilityOverflowPolicy.values()), diagnostic()));
		result.put(PROPERTY, schema(PROPERTY, text("display_name"), enumeration("value_kind", PropertySpec.PropertyValueKind.values()),
				number("maximum_stack", 1, 999, 1), diagnostic()));
		result.put(RECIPE, schema(RECIPE, text("display_name"),
				new ListFieldSchema("inputs", label("inputs"), false,
						new ReferenceFieldSchema("property", label("property"), true, RefKind.PROPERTY, "all_properties"), 32),
				new TextFieldSchema("output_variant", label("output_variant"), true, 64), diagnostic()));
		result.put(CONTRACT_NODE, schema(CONTRACT_NODE, text("display_name"),
				new TextFieldSchema("variant_key", label("variant_key"), true, 64), diagnostic()));
		result.put(SKILL, schema(SKILL, text("display_name"),
				enumerationKeys("activation_variant","ACTIVE"), enumerationKeys("condition_variant","ALWAYS","BUILTIN_STAT_COMPARE","RESOURCE_COMPARE"),
				enumerationKeys("condition_subject","CLASS_OWNER","SELECTED_ACTOR"), enumerationKeys("condition_builtin_stat","HP_CURRENT","HP_MAX","HP_PERCENT","HP_MISSING","HERO_LEVEL","BARRIER","TEMPORARY_HP"),
				enumerationKeys("condition_operator","LT","LTE","EQ","GTE","GT"), number("condition_value",-999,999,1), reference("condition_resource",RefKind.RESOURCE,"all_resources"),
				enumerationKeys("effect_primary_family","DAMAGE","STATUS","MOVEMENT","RECOVERY_DEFENSE","RESOURCE_OPERATION"),
				enumerationKeys("effect_primary_variant","DIRECT_DAMAGE","PERCENT_MAX_HP_DAMAGE","MISSING_HP_DAMAGE","EXECUTE","APPLY_STATUS","PUSH","PULL","THROW","DASH","TELEPORT","SWAP_POSITION","HEAL","BARRIER","TEMPORARY_HP","MITIGATE","REDIRECT_DAMAGE","CLEANSE","RESOURCE_OPERATION"),
				number("effect_primary_amount",1,999,1), enumerationKeys("effect_primary_damage_type","UNTYPED"),
				enumerationKeys("effect_primary_defense_policy","SPD_NATIVE"),
				number("effect_primary_percent",1,100,1), number("effect_primary_absolute_cap",0,9999,1), number("effect_primary_base_amount",0,999,1),
				number("effect_primary_missing_hp_numerator",0,100,1), number("effect_primary_missing_hp_denominator",1,100,1), number("effect_primary_hp_percent_threshold",1,100,1),
				enumerationKeys("effect_primary_status","POISON","BURNING","BLEEDING","SLOW","HASTE","PARALYSIS","ROOTS","AMOK","TERROR","VULNERABLE"), number("effect_primary_intensity",1,999,1),
				number("effect_primary_duration_turns",1,999,1), enumerationKeys("effect_primary_recipient","CLASS_OWNER","EVENT_SOURCE","EVENT_TARGET","SELECTED_ACTOR"), number("effect_primary_distance",1,20,1), number("effect_primary_maximum_distance",1,20,1), number("effect_primary_maximum_range",1,20,1), number("effect_primary_maximum_count",1,20,1),
				enumerationKeys("effect_primary_operation_variant","GAIN","DRAIN","SET","CLEAR","CONVERT","RESERVE","SUPPRESS"),
				reference("effect_primary_operation_resource",RefKind.RESOURCE,"all_resources"), reference("effect_primary_operation_source",RefKind.RESOURCE,"all_resources"), reference("effect_primary_operation_target",RefKind.RESOURCE,"all_resources"),
				number("effect_primary_operation_amount",1,999,1), number("effect_primary_operation_source_amount",1,999,1), number("effect_primary_operation_target_amount",1,999,1), number("effect_primary_operation_duration_turns",1,999,1),
				enumerationKeys("effect_secondary_family","NONE","DAMAGE","STATUS","MOVEMENT","RECOVERY_DEFENSE","RESOURCE_OPERATION"), enumerationKeys("effect_secondary_variant","DIRECT_DAMAGE","PERCENT_MAX_HP_DAMAGE","MISSING_HP_DAMAGE","EXECUTE","APPLY_STATUS","PUSH","PULL","THROW","DASH","TELEPORT","SWAP_POSITION","HEAL","BARRIER","TEMPORARY_HP","MITIGATE","REDIRECT_DAMAGE","CLEANSE","RESOURCE_OPERATION"),
				number("effect_secondary_amount",1,999,1), enumerationKeys("effect_secondary_activation","IMMEDIATE_ON_PRIMARY_SUCCESS"),
				number("effect_secondary_percent",1,100,1), number("effect_secondary_absolute_cap",0,9999,1), number("effect_secondary_base_amount",0,999,1), number("effect_secondary_missing_hp_numerator",0,100,1), number("effect_secondary_missing_hp_denominator",1,100,1), number("effect_secondary_hp_percent_threshold",1,100,1),
				enumerationKeys("effect_secondary_status","POISON","BURNING","BLEEDING","SLOW","HASTE","PARALYSIS","ROOTS","AMOK","TERROR","VULNERABLE"), number("effect_secondary_intensity",1,999,1), number("effect_secondary_duration_turns",1,999,1), enumerationKeys("effect_secondary_recipient","CLASS_OWNER","EVENT_SOURCE","EVENT_TARGET","SELECTED_ACTOR"),
				number("effect_secondary_distance",1,20,1), number("effect_secondary_maximum_distance",1,20,1), number("effect_secondary_maximum_range",1,20,1), number("effect_secondary_maximum_count",1,20,1),
				enumerationKeys("effect_secondary_operation_variant","GAIN","DRAIN","SET","CLEAR","CONVERT","RESERVE","SUPPRESS"), reference("effect_secondary_operation_resource",RefKind.RESOURCE,"all_resources"), reference("effect_secondary_operation_source",RefKind.RESOURCE,"all_resources"), reference("effect_secondary_operation_target",RefKind.RESOURCE,"all_resources"), number("effect_secondary_operation_amount",1,999,1), number("effect_secondary_operation_source_amount",1,999,1), number("effect_secondary_operation_target_amount",1,999,1), number("effect_secondary_operation_duration_turns",1,999,1),
				enumerationKeys("delivery_variant","DIRECT","SELF","CONTACT","PROJECTILE","TRACE","GROUND"), bool("delivery_requires_line_of_sight"), number("delivery_trace_width",1,9,1), bool("delivery_trace_stops_at_blocking_cell"), bool("delivery_ground_requires_visible_cell"),
				enumerationKeys("targeting_selector","SELECTED_ACTOR","SELF","SELECTED_CELL"), enumerationKeys("targeting_coverage","SINGLE","ADJACENT","RADIUS","LINE"),
				enumerationKeys("targeting_filter","RELATION_ENEMY_EXCLUDE_SELF","RELATION_ALLY_EXCLUDE_SELF","RELATION_ALLY_INCLUDE_SELF","ANY_ACTOR","SELF"), number("targeting_range",1,20,1),
				number("targeting_maximum_targets",1,20,1), number("targeting_radius",1,20,1), number("targeting_line_length",1,20,1), number("targeting_line_width",1,9,1), enumerationKeys("targeting_line_of_sight","DELIVERY"),
				enumerationKeys("targeting_ordering","DISTANCE_CELL_ACTOR_ID"), enumerationKeys("modifier_variant","NONE","REPEAT","INTENSITY","EXTEND_DURATION","PIERCE","BOUNCE"),
				number("modifier_repeat_count",1,5,1), number("modifier_intensity_numerator",1,100,1), number("modifier_intensity_denominator",1,100,1), number("modifier_additional_turns",1,999,1), number("modifier_additional_targets",1,20,1), number("modifier_bounces",1,20,1), number("modifier_bounce_range",1,20,1),
				enumerationKeys("cost_variant","NO_COST","RESOURCE","HP","ACTION_TIME","COOLDOWN","ITEM"), reference("cost_resource",RefKind.RESOURCE,"all_resources"), number("cost_amount",1,999,1), enumerationKeys("cost_item_category","ANY_WEAPON","ANY_CONSUMABLE","POTION","SCROLL","SEED","RUNESTONE"), enumerationKeys("constraint_variant","NONE"), diagnostic()));
		result.put(BASIC_ATTACK,schema(BASIC_ATTACK,text("display_name"),enumerationKeys("availability","FULL","WEAK","NONE"),number("damage_numerator",0,100,1),number("damage_denominator",1,100,1),enumerationKeys("allowed_weapons","ANY_WEAPON"),number("action_time_turns",1,20,1),diagnostic()));
		result.put(RESOURCE_FLOW,schema(RESOURCE_FLOW,withOperation(
				new FormFieldSchema[]{text("display_name"),enumerationKeys("event_type","TURN_START","WAIT","MOVE","ATTACK_DECLARED","ATTACK_HIT","DAMAGED","KILL")},
				new FormFieldSchema[]{diagnostic()})));
		result.put(ACTIVE_RESOURCE_OPERATION,schema(ACTIVE_RESOURCE_OPERATION,withOperation(
				new FormFieldSchema[]{text("display_name")},new FormFieldSchema[]{enumerationKeys("cost_variant","NO_COST","RESOURCE","HP","ACTION_TIME","COOLDOWN"),reference("cost.resource",RefKind.RESOURCE,"all_resources"),number("cost_amount",1,999,1),number("action_time_turns",1,20,1),diagnostic()})));
		result.put(RESOURCE_CLASS_OPERATION,schema(RESOURCE_CLASS_OPERATION,withOperation(
				new FormFieldSchema[]{text("display_name")},new FormFieldSchema[]{enumerationKeys("cost_variant","NO_COST","RESOURCE","HP","ACTION_TIME","COOLDOWN"),reference("cost.resource",RefKind.RESOURCE,"all_resources"),number("cost_amount",1,999,1),number("action_time_turns",1,20,1),bool("hud_visible"),number("hud_order",0,99,1),diagnostic()})));
		return Collections.unmodifiableMap(result);
	}

	private static FormSchema schema(String key, FormFieldSchema... fields) {
		return new FormSchema(key, Arrays.asList(fields));
	}
	private static FormFieldSchema[] operationFields() {
		return new FormFieldSchema[]{enumerationKeys("operation_variant","GAIN","DRAIN","SET","CLEAR","CONVERT","RESERVE","SUPPRESS"),
				reference("operation.resource",RefKind.RESOURCE,"all_resources"),reference("operation.source",RefKind.RESOURCE,"all_resources"),reference("operation.target",RefKind.RESOURCE,"all_resources"),
				number("operation.amount",1,999,1),number("operation.source_amount",1,999,1),number("operation.target_amount",1,999,1),number("operation.duration_turns",1,999,1)};
	}
	private static FormFieldSchema[] withOperation(FormFieldSchema[] before,FormFieldSchema[] after) {
		FormFieldSchema[] operation=operationFields(),out=new FormFieldSchema[before.length+operation.length+after.length];
		System.arraycopy(before,0,out,0,before.length);System.arraycopy(operation,0,out,before.length,operation.length);System.arraycopy(after,0,out,before.length+operation.length,after.length);return out;
	}
	private static TextFieldSchema text(String key) { return new TextFieldSchema(key, label(key), true, 24); }
	private static NumberFieldSchema number(String key, int min, int max, int step) {
		return new NumberFieldSchema(key, label(key), true, min, max, step);
	}
	private static BooleanFieldSchema bool(String key) { return new BooleanFieldSchema(key, label(key), true); }
	private static ReferenceFieldSchema reference(String key, RefKind kind, String filter) {
		return new ReferenceFieldSchema(key, label(key), true, kind, filter);
	}
	private static EnumFieldSchema enumeration(String key, Enum<?>[] values) {
		List<String> names = new ArrayList<>();
		for (Enum<?> value : values) names.add(value.name());
		return new EnumFieldSchema(key, label(key), true, names);
	}
	private static EnumFieldSchema enumerationKeys(String key,String... values) {
		return new EnumFieldSchema(key,label(key),true,Arrays.asList(values));
	}
	private static EnumListFieldSchema enumerationList(String key, Enum<?>[] values) {
		List<String> names = new ArrayList<>();
		for (Enum<?> value : values) names.add(value.name());
		return new EnumListFieldSchema(key, label(key), true, names, 1);
	}
	private static ReadOnlyDiagnosticFieldSchema diagnostic() {
		return new ReadOnlyDiagnosticFieldSchema("diagnostics", label("diagnostics"));
	}
	private static String label(String key) { return "v6.form." + key; }
}
