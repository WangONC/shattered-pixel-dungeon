package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.TypedRef;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Player command algebra. Payloads are primitives or typed refs; no command accepts a completed
 * declaration, ClassBuildSpec, SkillSpec, EffectSpec, or reducer result.
 */
public interface BuilderCommand {
	String typeKey();
	List<String> traceArguments();

	abstract class NamedCreate implements BuilderCommand {
		private final String displayName;
		NamedCreate(String displayName) { this.displayName = required(displayName, "display name"); }
		public final String displayName() { return displayName; }
		@Override public List<String> traceArguments() { return Collections.singletonList(displayName); }
	}

	final class CreateResource extends NamedCreate {
		public CreateResource(String displayName) { super(displayName); }
		@Override public String typeKey() { return "CreateResource"; }
	}
	final class CreateMark extends NamedCreate {
		public CreateMark(String displayName) { super(displayName); }
		@Override public String typeKey() { return "CreateMark"; }
	}
	final class CreateModeGroup extends NamedCreate {
		public CreateModeGroup(String displayName) { super(displayName); }
		@Override public String typeKey() { return "CreateModeGroup"; }
	}
	final class CreateAbilityPool extends NamedCreate {
		public CreateAbilityPool(String displayName) { super(displayName); }
		@Override public String typeKey() { return "CreateAbilityPool"; }
	}
	final class CreateProperty extends NamedCreate {
		public CreateProperty(String displayName) { super(displayName); }
		@Override public String typeKey() { return "CreateProperty"; }
	}
	final class CreateRecipe extends NamedCreate {
		private final String outputVariantKey;
		public CreateRecipe(String displayName, String outputVariantKey) {
			super(displayName); this.outputVariantKey = required(outputVariantKey, "output variant key");
		}
		public String outputVariantKey() { return outputVariantKey; }
		@Override public String typeKey() { return "CreateRecipe"; }
		@Override public List<String> traceArguments() { return Arrays.asList(displayName(), outputVariantKey); }
	}
	final class CreateMode extends NamedCreate {
		private final TypedRef group;
		public CreateMode(String displayName, TypedRef group) {
			super(displayName); this.group = required(group, "mode group ref");
		}
		public TypedRef group() { return group; }
		@Override public String typeKey() { return "CreateMode"; }
		@Override public List<String> traceArguments() { return withRef(displayName(), group); }
	}
	final class CreateEntityCapacity extends NamedCreate {
		private final String entityTypes;
		private final int maximum;
		private final String overflowPolicy;
		public CreateEntityCapacity(String displayName, String entityTypes, int maximum, String overflowPolicy) {
			super(displayName); this.entityTypes = required(entityTypes, "entity types"); this.maximum = maximum;
			this.overflowPolicy = required(overflowPolicy, "overflow policy");
		}
		public String entityTypes() { return entityTypes; }
		public int maximum() { return maximum; }
		public String overflowPolicy() { return overflowPolicy; }
		@Override public String typeKey() { return "CreateEntityCapacity"; }
		@Override public List<String> traceArguments() {
			return Arrays.asList(displayName(), entityTypes, Integer.toString(maximum), overflowPolicy);
		}
	}
	final class CreateEntity extends NamedCreate {
		private final String entityType;
		private final TypedRef capacity;
		public CreateEntity(String displayName, String entityType, TypedRef capacity) {
			super(displayName); this.entityType = required(entityType, "entity type"); this.capacity = capacity;
		}
		public String entityType() { return entityType; }
		public TypedRef capacity() { return capacity; }
		@Override public String typeKey() { return "CreateEntity"; }
		@Override public List<String> traceArguments() {
			return capacity == null ? Arrays.asList(displayName(), entityType, "", "", "")
					: withRef(displayName(), entityType, capacity);
		}
	}
	final class CreateClassComponent extends NodeCreate {
		public CreateClassComponent(String displayName, String variantKey) { super(displayName, variantKey); }
		@Override public String typeKey() { return "CreateClassComponent"; }
	}
	final class CreateClassConstraint extends NodeCreate {
		public CreateClassConstraint(String displayName, String variantKey) { super(displayName, variantKey); }
		@Override public String typeKey() { return "CreateClassConstraint"; }
	}
	final class CreateClassOperation extends NodeCreate {
		public CreateClassOperation(String displayName, String variantKey) { super(displayName, variantKey); }
		@Override public String typeKey() { return "CreateClassOperation"; }
	}
	final class CreateSkill extends NodeCreate {
		public CreateSkill(String displayName, String variantKey) { super(displayName, variantKey); }
		public CreateSkill(String displayName) { this(displayName, "SKILL_V0_2"); }
		@Override public String typeKey() { return "CreateSkill"; }
	}
	abstract class NodeCreate extends NamedCreate {
		private final String variantKey;
		NodeCreate(String displayName, String variantKey) {
			super(displayName); this.variantKey = required(variantKey, "variant key");
		}
		public final String variantKey() { return variantKey; }
		@Override public List<String> traceArguments() { return Arrays.asList(displayName(), variantKey); }
	}

	final class SelectTriggerVariant extends StringCommand2 {
		public SelectTriggerVariant(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SelectTriggerVariant";}
	}
	final class SelectConditionVariant extends StringCommand2 {
		public SelectConditionVariant(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SelectConditionVariant";}
	}
	final class SelectEffectFamily extends StringCommand3 {
		public SelectEffectFamily(String skillId,String effectSlot,String familyKey){super(skillId,effectSlot,familyKey);}
		public String skillId(){return first();} public String effectSlot(){return second();} public String familyKey(){return third();}
		@Override public String typeKey(){return "SelectEffectFamily";}
	}
	final class SelectEffectVariant extends StringCommand3 {
		public SelectEffectVariant(String skillId,String effectSlot,String variantKey){super(skillId,effectSlot,variantKey);}
		public String skillId(){return first();} public String effectSlot(){return second();} public String variantKey(){return third();}
		@Override public String typeKey(){return "SelectEffectVariant";}
	}
	final class SetTargetingSelector extends StringCommand2 {
		public SetTargetingSelector(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SetTargetingSelector";}
	}
	final class SetTargetingCoverage extends StringCommand2 {
		public SetTargetingCoverage(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SetTargetingCoverage";}
	}
	final class SetTargetingFilter extends StringCommand2 {
		public SetTargetingFilter(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SetTargetingFilter";}
	}
	final class SetDelivery extends StringCommand2 {
		public SetDelivery(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SetDelivery";}
	}
	final class SetModifier extends StringCommand2 {
		public SetModifier(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SetModifier";}
	}
	final class SetCost extends StringCommand2 {
		public SetCost(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SetCost";}
	}
	final class SetSkillConstraint extends StringCommand2 {
		public SetSkillConstraint(String skillId,String variantKey){super(skillId,variantKey);}
		public String skillId(){return first();} public String variantKey(){return second();}
		@Override public String typeKey(){return "SetSkillConstraint";}
	}
	final class SetTypedSkillField implements BuilderCommand {
		private final String skillId,ownerPath,variantKey,fieldKey,value;
		public SetTypedSkillField(String skillId,String ownerPath,String variantKey,String fieldKey,String value){
			this.skillId=required(skillId,"skill id");this.ownerPath=required(ownerPath,"owner path");
			this.variantKey=required(variantKey,"variant key");this.fieldKey=required(fieldKey,"field key");this.value=required(value,"field value");
		}
		public String skillId(){return skillId;}public String ownerPath(){return ownerPath;}public String variantKey(){return variantKey;}public String fieldKey(){return fieldKey;}public String value(){return value;}
		@Override public String typeKey(){return "SetTypedSkillField";}
		@Override public List<String> traceArguments(){return Arrays.asList(skillId,ownerPath,variantKey,fieldKey,value);}
	}
	final class SetTypedSkillReference implements BuilderCommand {
		private final String skillId,ownerPath,variantKey,fieldKey;private final TypedRef reference;
		public SetTypedSkillReference(String skillId,String ownerPath,String variantKey,String fieldKey,TypedRef reference){
			this.skillId=required(skillId,"skill id");this.ownerPath=required(ownerPath,"owner path");this.variantKey=required(variantKey,"variant key");this.fieldKey=required(fieldKey,"field key");this.reference=required(reference,"typed reference");
		}
		public String skillId(){return skillId;}public String ownerPath(){return ownerPath;}public String variantKey(){return variantKey;}public String fieldKey(){return fieldKey;}public TypedRef reference(){return reference;}
		@Override public String typeKey(){return "SetTypedSkillReference";}
		@Override public List<String> traceArguments(){return withRef(new String[]{skillId,ownerPath,variantKey,fieldKey},reference);}
	}
	abstract class StringCommand2 implements BuilderCommand {
		private final String first,second;
		StringCommand2(String first,String second){this.first=required(first,"first argument");this.second=required(second,"second argument");}
		final String first(){return first;}final String second(){return second;}
		@Override public List<String> traceArguments(){return Arrays.asList(first,second);}
	}
	abstract class StringCommand3 implements BuilderCommand {
		private final String first,second,third;
		StringCommand3(String first,String second,String third){this.first=required(first,"first argument");this.second=required(second,"second argument");this.third=required(third,"third argument");}
		final String first(){return first;}final String second(){return second;}final String third(){return third;}
		@Override public List<String> traceArguments(){return Arrays.asList(first,second,third);}
	}

	final class SetFieldValue implements BuilderCommand {
		private final String ownerId;
		private final String variantKey;
		private final String fieldKey;
		private final String value;
		public SetFieldValue(String ownerId, String variantKey, String fieldKey, String value) {
			this.ownerId = required(ownerId, "owner id"); this.variantKey = required(variantKey, "variant key");
			this.fieldKey = required(fieldKey, "field key"); this.value = required(value, "field value");
		}
		public String ownerId() { return ownerId; }
		public String variantKey() { return variantKey; }
		public String fieldKey() { return fieldKey; }
		public String value() { return value; }
		@Override public String typeKey() { return "SetFieldValue"; }
		@Override public List<String> traceArguments() { return Arrays.asList(ownerId, variantKey, fieldKey, value); }
	}
	final class EditResourceField implements BuilderCommand {
		private final String resourceId, fieldKey, value;
		public EditResourceField(String resourceId, String fieldKey, String value) {
			this.resourceId = required(resourceId, "resource id"); this.fieldKey = required(fieldKey, "field key");
			this.value = required(value, "field value");
		}
		public String resourceId() { return resourceId; } public String fieldKey() { return fieldKey; } public String value() { return value; }
		@Override public String typeKey() { return "EditResourceField"; }
		@Override public List<String> traceArguments() { return Arrays.asList(resourceId, fieldKey, value); }
	}
	final class EditMarkField implements BuilderCommand {
		private final String markId, fieldKey, value;
		public EditMarkField(String markId, String fieldKey, String value) {
			this.markId = required(markId, "mark id"); this.fieldKey = required(fieldKey, "field key");
			this.value = required(value, "field value");
		}
		public String markId() { return markId; } public String fieldKey() { return fieldKey; } public String value() { return value; }
		@Override public String typeKey() { return "EditMarkField"; }
		@Override public List<String> traceArguments() { return Arrays.asList(markId, fieldKey, value); }
	}
	final class SetReference implements BuilderCommand {
		private final String ownerId, variantKey, fieldKey;
		private final TypedRef reference;
		public SetReference(String ownerId, String variantKey, String fieldKey, TypedRef reference) {
			this.ownerId = required(ownerId, "owner id"); this.variantKey = required(variantKey, "variant key");
			this.fieldKey = required(fieldKey, "field key"); this.reference = required(reference, "typed reference");
		}
		public String ownerId() { return ownerId; } public String variantKey() { return variantKey; }
		public String fieldKey() { return fieldKey; } public TypedRef reference() { return reference; }
		@Override public String typeKey() { return "SetReference"; }
		@Override public List<String> traceArguments() { return withRef(ownerId, variantKey, fieldKey, reference); }
	}
	final class RenameDeclaration implements BuilderCommand {
		private final String targetId, displayName;
		public RenameDeclaration(String targetId, String displayName) {
			this.targetId = required(targetId, "target id"); this.displayName = required(displayName, "display name");
		}
		public String targetId() { return targetId; } public String displayName() { return displayName; }
		@Override public String typeKey() { return "RenameDeclaration"; }
		@Override public List<String> traceArguments() { return Arrays.asList(targetId, displayName); }
	}
	final class DeleteDeclaration implements BuilderCommand {
		private final String targetId;
		public DeleteDeclaration(String targetId) { this.targetId = required(targetId, "target id"); }
		public String targetId() { return targetId; }
		@Override public String typeKey() { return "DeleteDeclaration"; }
		@Override public List<String> traceArguments() { return Collections.singletonList(targetId); }
	}
	final class RebindReference implements BuilderCommand {
		private final String ownerId, fieldKey;
		private final TypedRef newTarget;
		public RebindReference(String ownerId, String fieldKey, TypedRef newTarget) {
			this.ownerId = required(ownerId, "owner id"); this.fieldKey = required(fieldKey, "field key");
			this.newTarget = required(newTarget, "new target ref");
		}
		public String ownerId() { return ownerId; } public String fieldKey() { return fieldKey; }
		public TypedRef newTarget() { return newTarget; }
		@Override public String typeKey() { return "RebindReference"; }
		@Override public List<String> traceArguments() { return withRef(ownerId, fieldKey, newTarget); }
	}
	final class SaveDraft implements BuilderCommand {
		private final String slotKey;
		public SaveDraft(String slotKey) { this.slotKey = required(slotKey, "draft slot key"); }
		public String slotKey() { return slotKey; }
		@Override public String typeKey() { return "SaveDraft"; }
		@Override public List<String> traceArguments() { return Collections.singletonList(slotKey); }
	}
	final class LoadDraft implements BuilderCommand {
		private final String slotKey;
		public LoadDraft(String slotKey) { this.slotKey = required(slotKey, "draft slot key"); }
		public String slotKey() { return slotKey; }
		@Override public String typeKey() { return "LoadDraft"; }
		@Override public List<String> traceArguments() { return Collections.singletonList(slotKey); }
	}
	final class Navigate implements BuilderCommand {
		private final String route, targetId, fieldKey;
		public Navigate(String route, String targetId, String fieldKey) {
			this.route = required(route, "route"); this.targetId = targetId == null ? "" : targetId;
			this.fieldKey = fieldKey == null ? "" : fieldKey;
		}
		public String route() { return route; } public String targetId() { return targetId; } public String fieldKey() { return fieldKey; }
		@Override public String typeKey() { return "Navigate"; }
		@Override public List<String> traceArguments() { return Arrays.asList(route, targetId, fieldKey); }
	}
	final class FinalizeBuild implements BuilderCommand {
		@Override public String typeKey() { return "FinalizeBuild"; }
		@Override public List<String> traceArguments() { return Collections.emptyList(); }
	}
	final class Undo implements BuilderCommand {
		@Override public String typeKey() { return "Undo"; }
		@Override public List<String> traceArguments() { return Collections.emptyList(); }
	}
	final class Redo implements BuilderCommand {
		@Override public String typeKey() { return "Redo"; }
		@Override public List<String> traceArguments() { return Collections.emptyList(); }
	}

	static String required(String value, String label) {
		if (value == null || value.isEmpty()) throw new IllegalArgumentException(label + " is required");
		return value;
	}
	static <T> T required(T value, String label) {
		if (value == null) throw new IllegalArgumentException(label + " is required");
		return value;
	}
	static List<String> withRef(String first, TypedRef ref) { return withRef(new String[]{first}, ref); }
	static List<String> withRef(String first, String second, TypedRef ref) { return withRef(new String[]{first, second}, ref); }
	static List<String> withRef(String first, String second, String third, TypedRef ref) {
		return withRef(new String[]{first, second, third}, ref);
	}
	static List<String> withRef(String[] values, TypedRef ref) {
		java.util.ArrayList<String> result = new java.util.ArrayList<>(Arrays.asList(values));
		result.add(ref.kind().name()); result.add(ref.targetId().value()); result.add(ref.lastKnownDisplayName());
		return result;
	}
}
