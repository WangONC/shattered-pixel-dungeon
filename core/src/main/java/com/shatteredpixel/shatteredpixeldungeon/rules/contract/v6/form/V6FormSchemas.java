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
				enumerationKeys("activation_variant","ACTIVE"), enumerationKeys("condition_variant","ALWAYS"),
				enumerationKeys("effect_primary_family","DAMAGE"), enumerationKeys("effect_primary_variant","DIRECT_DAMAGE"),
				number("effect_primary_amount",1,999,1), enumerationKeys("effect_primary_damage_type","UNTYPED"),
				enumerationKeys("effect_primary_defense_policy","SPD_NATIVE"),
				enumerationKeys("effect_secondary_family","NONE","DAMAGE"), enumerationKeys("effect_secondary_variant","DIRECT_DAMAGE"),
				number("effect_secondary_amount",1,999,1), enumerationKeys("effect_secondary_activation","IMMEDIATE_ON_PRIMARY_SUCCESS"),
				enumerationKeys("delivery_variant","DIRECT"), bool("delivery_requires_line_of_sight"),
				enumerationKeys("targeting_selector","SELECTED_ACTOR"), enumerationKeys("targeting_coverage","SINGLE"),
				enumerationKeys("targeting_filter","RELATION_ENEMY_EXCLUDE_SELF"), number("targeting_range",1,20,1),
				number("targeting_maximum_targets",1,1,1), enumerationKeys("targeting_line_of_sight","DELIVERY"),
				enumerationKeys("targeting_ordering","DISTANCE_CELL_ACTOR_ID"), enumerationKeys("modifier_variant","NONE"),
				enumerationKeys("cost_variant","NO_COST"), enumerationKeys("constraint_variant","NONE"), diagnostic()));
		return Collections.unmodifiableMap(result);
	}

	private static FormSchema schema(String key, FormFieldSchema... fields) {
		return new FormSchema(key, Arrays.asList(fields));
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
