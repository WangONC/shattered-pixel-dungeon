package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Explicit typed rebuilds for P02 fields. Deliberately contains no reflection or auto-binding. */
final class BuilderDraftEditor {
	ClassBuildSpec setField(ClassBuildSpec build, String ownerId, String variantKey, String fieldKey, String rawValue) {
		StableId id = StableId.fromStored(ownerId);
		StableTarget target = requireTarget(build, id);
		String actual = variantOf(target);
		if (!actual.equals(variantKey)) throw new IllegalArgumentException("variant mismatch: expected " + actual + " got " + variantKey);
		FormFieldSchema schema = V6FormSchemas.require(variantKey).requireField(fieldKey);
		validate(schema, rawValue);
		if ("display_name".equals(fieldKey)) {
			return new com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.edit.BuildEditService()
					.rename(build, id, DisplayName.of(rawValue));
		}
		ClassBuildSpec.Builder out = build.toBuilder();
		if (target instanceof ResourceSpec) out.resources(replace(build.resources(), id, resource((ResourceSpec) target, fieldKey, rawValue)));
		else if (target instanceof MarkSpec) out.marks(replace(build.marks(), id, mark((MarkSpec) target, fieldKey, rawValue)));
		else if (target instanceof ModeGroupSpec) out.modeGroups(replace(build.modeGroups(), id, modeGroup((ModeGroupSpec) target, fieldKey, rawValue)));
		else if (target instanceof ModeSpec) out.modes(replace(build.modes(), id, mode((ModeSpec) target, fieldKey, rawValue)));
		else if (target instanceof EntityCapacitySpec) out.capacities(replace(build.capacities(), id, capacity((EntityCapacitySpec) target, fieldKey, rawValue)));
		else if (target instanceof EntitySpec) out.entities(replace(build.entities(), id, entity((EntitySpec) target, fieldKey, rawValue)));
		else if (target instanceof AbilityPoolSpec) out.abilityPools(replace(build.abilityPools(), id, abilityPool((AbilityPoolSpec) target, fieldKey, rawValue)));
		else if (target instanceof PropertySpec) out.properties(replace(build.properties(), id, property((PropertySpec) target, fieldKey, rawValue)));
		else if (target instanceof SynthesisRecipeSpec) out.recipes(replace(build.recipes(), id, recipe((SynthesisRecipeSpec) target, fieldKey, rawValue)));
		else if (target instanceof ContractNodeSpec) {
			ContractNodeSpec node = contractNode((ContractNodeSpec) target, fieldKey, rawValue);
			replaceNode(out, build, node);
		} else throw new IllegalArgumentException("unsupported P02 declaration " + target.getClass().getName());
		return out.build();
	}

	ClassBuildSpec setReference(ClassBuildSpec build, String ownerId, String variantKey, String fieldKey, TypedRef reference) {
		StableId id = StableId.fromStored(ownerId);
		StableTarget owner = requireTarget(build, id);
		if (!variantOf(owner).equals(variantKey)) throw new IllegalArgumentException("reference owner variant mismatch");
		StableTarget target = requireTarget(build, reference.targetId());
		if (target.refKind() != reference.kind()) throw new IllegalArgumentException("reference target type mismatch");
		ClassBuildSpec.Builder out = build.toBuilder();
		if (owner instanceof ModeSpec && "group".equals(fieldKey)) {
			requireKind(reference, RefKind.MODE_GROUP);
			ModeSpec value = (ModeSpec) owner;
			out.modes(replace(build.modes(), id, value.withGroup(new ModeGroupRef(target.id(), target.displayName().text()))));
		} else if (owner instanceof EntitySpec && "capacity".equals(fieldKey)) {
			requireKind(reference, RefKind.CAPACITY);
			EntitySpec value = (EntitySpec) owner;
			out.entities(replace(build.entities(), id, value.withCapacity(new CapacityRef(target.id(), target.displayName().text()))));
		} else if (owner instanceof SynthesisRecipeSpec && fieldKey.matches("inputs\\[[0-9]+]\\.property")) {
			requireKind(reference, RefKind.PROPERTY);
			SynthesisRecipeSpec value = (SynthesisRecipeSpec) owner;
			int index = index(fieldKey, "inputs[");
			if (index < 0 || index >= value.inputs().size()) throw new IllegalArgumentException("recipe input index out of bounds");
			List<SynthesisRecipeSpec.PropertyInput> inputs = new ArrayList<>(value.inputs());
			SynthesisRecipeSpec.PropertyInput old = inputs.get(index);
			inputs.set(index, new SynthesisRecipeSpec.PropertyInput(new PropertyRef(target.id(), target.displayName().text()), old.amount()));
			out.recipes(replace(build.recipes(), id, new SynthesisRecipeSpec(value.id(), value.displayName(), inputs,
					value.outputVariantKey(), value.implementationState())));
		} else if (owner instanceof EntitySpec && fieldKey.matches("capabilities\\[[0-9]+]\\.slots\\[[0-9]+]\\.resource")) {
			requireKind(reference, RefKind.RESOURCE);
			EntitySpec value = (EntitySpec) owner;
			int capabilityIndex = index(fieldKey, "capabilities[");
			int slotIndex = index(fieldKey, "slots[");
			if (capabilityIndex < 0 || capabilityIndex >= value.capabilities().size()
					|| !(value.capabilities().get(capabilityIndex) instanceof ResourceStorageCapability)) {
				throw new IllegalArgumentException("resource storage capability index out of bounds");
			}
			List<EntityCapabilitySpec> capabilities = new ArrayList<>(value.capabilities());
			ResourceStorageCapability storage = (ResourceStorageCapability) capabilities.get(capabilityIndex);
			if (slotIndex < 0 || slotIndex >= storage.slots().size()) throw new IllegalArgumentException("resource slot index out of bounds");
			List<EntityResourceSlotSpec> slots = new ArrayList<>(storage.slots());
			EntityResourceSlotSpec old = slots.get(slotIndex);
			slots.set(slotIndex, new EntityResourceSlotSpec(new ResourceRef(target.id(), target.displayName().text()),
					old.initialValue(), old.maximumOverride()));
			capabilities.set(capabilityIndex, storage.withSlots(slots));
			out.entities(replace(build.entities(), id, value.withCapabilities(capabilities)));
		} else throw new IllegalArgumentException("unknown reference field " + ownerId + "." + fieldKey);
		return out.build();
	}

	static StableTarget requireTarget(ClassBuildSpec build, StableId id) {
		if (build.buildId().equals(id)) throw new IllegalArgumentException("build root is not a declaration target");
		for (StableTarget value : build.allTargets()) if (value.id().equals(id)) return value;
		throw new IllegalArgumentException("declaration not found: " + id.value());
	}

	static String variantOf(StableTarget target) {
		if (target instanceof ResourceSpec) return V6FormSchemas.RESOURCE;
		if (target instanceof MarkSpec) return V6FormSchemas.MARK;
		if (target instanceof ModeGroupSpec) return V6FormSchemas.MODE_GROUP;
		if (target instanceof ModeSpec) return V6FormSchemas.MODE;
		if (target instanceof EntityCapacitySpec) return V6FormSchemas.ENTITY_CAPACITY;
		if (target instanceof EntitySpec) return V6FormSchemas.ENTITY;
		if (target instanceof AbilityPoolSpec) return V6FormSchemas.ABILITY_POOL;
		if (target instanceof PropertySpec) return V6FormSchemas.PROPERTY;
		if (target instanceof SynthesisRecipeSpec) return V6FormSchemas.RECIPE;
		if (target instanceof ContractNodeSpec) return V6FormSchemas.CONTRACT_NODE;
		throw new IllegalArgumentException("unknown declaration type " + target.getClass().getName());
	}

	private static ResourceSpec resource(ResourceSpec v, String key, String raw) {
		int min=v.minimum(), max=v.maximum(), initial=v.initialValue(), order=v.hud().order(); boolean visible=v.hud().visible();
		String presentation=v.hud().presentationKey(); ResourceSpec.ResourceOverflowPolicy overflow=v.defaultOverflowPolicy();
		if("minimum".equals(key))min=integer(raw);else if("maximum".equals(key))max=integer(raw);else if("initial_value".equals(key))initial=integer(raw);
		else if("default_overflow_policy".equals(key))overflow=en(ResourceSpec.ResourceOverflowPolicy.class,raw);
		else if("hud.visible".equals(key))visible=bool(raw);else if("hud.order".equals(key))order=integer(raw);
		else if("hud.presentation_key".equals(key))presentation=raw;else unknown(key);
		return new ResourceSpec(v.id(),v.displayName(),min,max,initial,overflow,new ResourceSpec.ResourceHudSpec(visible,order,presentation));
	}
	private static MarkSpec mark(MarkSpec v,String key,String raw){MarkSpec.MarkKind kind=v.kind();int min=v.minimum(),max=v.maximum(),initial=v.initialValue(),turns=v.defaultDurationTurns();MarkSpec.MarkDurationPolicy duration=v.durationPolicy();MarkSpec.MarkRefreshPolicy refresh=v.refreshPolicy();MarkSpec.MarkOverflowPolicy overflow=v.overflowPolicy();MarkSpec.MarkProvenancePolicy provenance=v.provenancePolicy();
		if("kind".equals(key))kind=en(MarkSpec.MarkKind.class,raw);else if("minimum".equals(key))min=integer(raw);else if("maximum".equals(key))max=integer(raw);else if("initial_value".equals(key))initial=integer(raw);else if("duration_policy".equals(key))duration=en(MarkSpec.MarkDurationPolicy.class,raw);else if("default_duration_turns".equals(key))turns=integer(raw);else if("refresh_policy".equals(key))refresh=en(MarkSpec.MarkRefreshPolicy.class,raw);else if("overflow_policy".equals(key))overflow=en(MarkSpec.MarkOverflowPolicy.class,raw);else if("provenance_policy".equals(key))provenance=en(MarkSpec.MarkProvenancePolicy.class,raw);else unknown(key);
		return new MarkSpec(v.id(),v.displayName(),kind,min,max,initial,duration,turns,refresh,overflow,provenance);}
	private static ModeGroupSpec modeGroup(ModeGroupSpec v,String key,String raw){if(!"policy".equals(key))unknown(key);return new ModeGroupSpec(v.id(),v.displayName(),en(ModeGroupSpec.ModeGroupPolicy.class,raw));}
	private static ModeSpec mode(ModeSpec v,String key,String raw){boolean initial=v.initial();ModeSpec.ModeDurationPolicy duration=v.durationPolicy();int turns=v.defaultDurationTurns();if("initial".equals(key))initial=bool(raw);else if("duration_policy".equals(key))duration=en(ModeSpec.ModeDurationPolicy.class,raw);else if("default_duration_turns".equals(key))turns=integer(raw);else unknown(key);return new ModeSpec(v.id(),v.displayName(),v.group(),initial,duration,turns);}
	private static EntityCapacitySpec capacity(EntityCapacitySpec v,String key,String raw){EnumSet<EntityType> types=EnumSet.copyOf(v.entityTypes());int max=v.maximum();EntityCapacitySpec.CapacityOverflowPolicy overflow=v.overflowPolicy();if("entity_types".equals(key))types=entityTypes(raw);else if("maximum".equals(key))max=integer(raw);else if("overflow_policy".equals(key))overflow=en(EntityCapacitySpec.CapacityOverflowPolicy.class,raw);else unknown(key);return new EntityCapacitySpec(v.id(),v.displayName(),types,max,overflow);}
	private static EntitySpec entity(EntitySpec v,String key,String raw){if(!"entity_type".equals(key))unknown(key);return new EntitySpec(v.id(),v.displayName(),en(EntityType.class,raw),v.body(),v.spawnPolicy(),v.ownership(),v.relation(),v.capacity(),v.persistence(),v.capabilities(),v.implementationState());}
	private static AbilityPoolSpec abilityPool(AbilityPoolSpec v,String key,String raw){int capacity=v.capacity();AbilityPoolSpec.AbilityOverflowPolicy overflow=v.overflowPolicy();if("capacity".equals(key))capacity=integer(raw);else if("overflow_policy".equals(key))overflow=en(AbilityPoolSpec.AbilityOverflowPolicy.class,raw);else unknown(key);return new AbilityPoolSpec(v.id(),v.displayName(),capacity,overflow);}
	private static PropertySpec property(PropertySpec v,String key,String raw){PropertySpec.PropertyValueKind kind=v.valueKind();int max=v.maximumStack();if("value_kind".equals(key))kind=en(PropertySpec.PropertyValueKind.class,raw);else if("maximum_stack".equals(key))max=integer(raw);else unknown(key);return new PropertySpec(v.id(),v.displayName(),kind,max);}
	private static SynthesisRecipeSpec recipe(SynthesisRecipeSpec v,String key,String raw){if(!"output_variant".equals(key))unknown(key);return new SynthesisRecipeSpec(v.id(),v.displayName(),v.inputs(),raw,v.implementationState());}
	private static ContractNodeSpec contractNode(ContractNodeSpec v,String key,String raw){if(!"variant_key".equals(key))unknown(key);return new ContractNodeSpec(v.id(),v.displayName(),v.nodeKind(),raw,v.implementationState());}
	private static void replaceNode(ClassBuildSpec.Builder out,ClassBuildSpec build,ContractNodeSpec node){switch(node.nodeKind()){case COMPONENT:out.classComponents(replace(build.classComponents(),node.id(),node));break;case CONSTRAINT:out.classConstraints(replace(build.classConstraints(),node.id(),node));break;case OPERATION:out.classOperations(replace(build.classOperations(),node.id(),node));break;case SKILL:out.skills(replace(build.skills(),node.id(),node));break;default:throw new AssertionError(node.nodeKind());}}
	private static <T extends StableTarget> List<T> replace(List<T> values,StableId id,T replacement){List<T> result=new ArrayList<>();boolean found=false;for(T value:values){if(value.id().equals(id)){result.add(replacement);found=true;}else result.add(value);}if(!found)throw new IllegalArgumentException("declaration not found: "+id.value());return result;}

	private static void validate(FormFieldSchema schema,String raw){if(schema.kind()==FormFieldSchema.Kind.NUMBER){NumberFieldSchema number=(NumberFieldSchema)schema;int value=integer(raw);if(value<number.minimum()||value>number.maximum())throw new IllegalArgumentException("number outside schema bounds: "+schema.fieldKey());}else if(schema.kind()==FormFieldSchema.Kind.ENUM){if(!((EnumFieldSchema)schema).contains(raw))throw new IllegalArgumentException("enum value outside schema: "+raw);}else if(schema.kind()==FormFieldSchema.Kind.BOOLEAN){bool(raw);}else if(schema.kind()==FormFieldSchema.Kind.TEXT){TextFieldSchema text=(TextFieldSchema)schema;if(schema.required()&&raw.isEmpty()||raw.codePointCount(0,raw.length())>text.maximumCodePoints())throw new IllegalArgumentException("text outside schema bounds: "+schema.fieldKey());}else throw new IllegalArgumentException("field cannot be set as a scalar: "+schema.fieldKey());}
	private static EnumSet<EntityType> entityTypes(String raw){EnumSet<EntityType> result=EnumSet.noneOf(EntityType.class);for(String part:raw.split(","))result.add(en(EntityType.class,part.trim()));if(result.isEmpty())throw new IllegalArgumentException("entity type set cannot be empty");return result;}
	private static void requireKind(TypedRef ref,RefKind expected){if(ref.kind()!=expected)throw new IllegalArgumentException("reference kind mismatch: expected "+expected+" got "+ref.kind());}
	private static int index(String field,String marker){int start=field.indexOf(marker);if(start<0)return-1;start+=marker.length();int end=field.indexOf(']',start);return end<0?-1:integer(field.substring(start,end));}
	private static int integer(String raw){try{return Integer.parseInt(raw);}catch(NumberFormatException error){throw new IllegalArgumentException("integer value required: "+raw,error);}}
	private static boolean bool(String raw){if("true".equals(raw))return true;if("false".equals(raw))return false;throw new IllegalArgumentException("boolean value required: "+raw);}
	private static <E extends Enum<E>> E en(Class<E> type,String raw){try{return Enum.valueOf(type,raw);}catch(IllegalArgumentException error){throw new IllegalArgumentException("unknown "+type.getSimpleName()+" value "+raw,error);}}
	private static void unknown(String key){throw new IllegalArgumentException("unknown P02 field "+key);}
}
