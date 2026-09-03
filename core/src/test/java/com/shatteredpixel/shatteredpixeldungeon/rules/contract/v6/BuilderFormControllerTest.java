package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.PlayerBuildSession;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.BuilderFormController;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.EnumListFieldSchema;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.FormFieldSchema;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save.CanonicalBuildCodec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntityCapacitySpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntitySpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.EntityType;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeGroupSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BuilderFormControllerTest {

	private final CanonicalBuildCodec codec = new CanonicalBuildCodec();

	@Test public void everyEnabledFieldProducesARealCommandChangesStateAndSupportsUndoRedo() {
		PlayerBuildSession session = completeSession("p02-r1-enabled");
		BuilderFormController controller = new BuilderFormController(session);
		int enabledFields = 0;

		List<String> targetIds = new ArrayList<>();
		for (StableTarget target : session.state().draft().allTargets()) targetIds.add(target.id().value());
		for (String targetId : targetIds) {
			BuilderFormController.FormModel form = controller.form(targetId);
			for (BuilderFormController.FieldModel field : form.fields()) {
				if (!field.enabled()) {
					assertFalse("disabled field needs a reason", field.disabledReason().isEmpty());
					continue;
				}
				enabledFields++;
				assertNotNull("enabled field needs a usable suggestion: " + form.variantKey() + "." + field.fieldKey(),
						field.suggestedValue());
				assertNotNull(controller.commandForValue(targetId, field.fieldKey(), field.suggestedValue()));

				String before = canonical(session);
				controller.dispatchValue(targetId, field.fieldKey(), field.suggestedValue());
				String changed = canonical(session);
				assertNotEquals("enabled action must not be a no-op: " + form.variantKey() + "." + field.fieldKey(),
						before, changed);
				controller.dispatch(new BuilderCommand.Undo());
				assertEquals(before, canonical(session));
				controller.dispatch(new BuilderCommand.Redo());
				assertEquals(changed, canonical(session));
				controller.dispatch(new BuilderCommand.Undo());
				assertEquals(before, canonical(session));
			}
		}
		assertTrue("test fixture must exercise a substantial real form", enabledFields >= 30);
	}

	@Test public void deletedReferenceIsAnExplicitUnresolvedCardUntilPlayerRebinds() {
		PlayerBuildSession session = PlayerBuildSession.empty(new DeterministicIdGenerator("p02-r1-unresolved"));
		session.dispatch(new BuilderCommand.CreateModeGroup("Original Stance"));
		ModeGroupSpec original = session.state().draft().modeGroups().get(0);
		session.dispatch(new BuilderCommand.CreateMode("Guard", new ModeGroupRef(original.id(), original.displayName().text())));
		String modeId = session.state().draft().modes().get(0).id().value();
		session.dispatch(new BuilderCommand.DeleteDeclaration(original.id().value()));
		session.dispatch(new BuilderCommand.CreateModeGroup("Replacement Stance"));
		ModeGroupSpec replacement = session.state().draft().modeGroups().get(0);

		BuilderFormController controller = new BuilderFormController(session);
		BuilderFormController.FieldModel group = controller.form(modeId).requireField("group");
		assertTrue(group.unresolved());
		assertTrue(group.enabled());
		assertEquals("Original Stance", group.lastKnownDisplayName());
		assertEquals(shortId(original.id().value()), group.shortTargetId());
		assertTrue(group.detail().contains("UNRESOLVED"));
		assertEquals("deletion must not auto-select the first available declaration",
				original.id(), session.state().draft().modes().get(0).group().targetId());

		controller.dispatchValue(modeId, "group", replacement.id().value());
		assertEquals(replacement.id(), session.state().draft().modes().get(0).group().targetId());
		assertFalse(controller.form(modeId).requireField("group").unresolved());
	}

	@Test public void referenceFilterUsesCurrentEntityTypeAndNeverOffersIncompatibleCapacity() {
		PlayerBuildSession session = PlayerBuildSession.empty(new DeterministicIdGenerator("p02-r1-filter"));
		session.dispatch(new BuilderCommand.CreateEntityCapacity("Device only", "DEVICE", 1, "REJECT_NEW"));
		session.dispatch(new BuilderCommand.CreateEntityCapacity("Trap only", "TRAP", 1, "REJECT_NEW"));
		session.dispatch(new BuilderCommand.CreateEntityCapacity("Shared", "DEVICE,TRAP", 1, "REJECT_NEW"));
		session.dispatch(new BuilderCommand.CreateEntity("Turret", "DEVICE", null));
		EntitySpec entity = session.state().draft().entities().get(0);
		List<EntityCapacitySpec> capacities = session.state().draft().capacities();
		BuilderFormController controller = new BuilderFormController(session);

		List<String> offered = enabledValues(controller.form(entity.id().value()).requireField("capacity"));
		assertTrue(offered.contains(capacities.get(0).id().value()));
		assertFalse(offered.contains(capacities.get(1).id().value()));
		assertTrue(offered.contains(capacities.get(2).id().value()));

		controller.dispatchValue(entity.id().value(), "entity_type", EntityType.TRAP.name());
		offered = enabledValues(controller.form(entity.id().value()).requireField("capacity"));
		assertFalse(offered.contains(capacities.get(0).id().value()));
		assertTrue(offered.contains(capacities.get(1).id().value()));
		assertTrue(offered.contains(capacities.get(2).id().value()));
	}

	@Test public void entityCapacityMultiTypeCanBeCompletedThroughPlayerFormPath() {
		PlayerBuildSession session = PlayerBuildSession.empty(new DeterministicIdGenerator("p02-r1-multi"));
		session.dispatch(new BuilderCommand.CreateEntityCapacity("Mixed capacity", "DEVICE", 2, "REJECT_NEW"));
		EntityCapacitySpec capacity = session.state().draft().capacities().get(0);
		BuilderFormController controller = new BuilderFormController(session);
		BuilderFormController.FieldModel field = controller.form(capacity.id().value()).requireField("entity_types");
		assertTrue(field.schema() instanceof EnumListFieldSchema);
		String addTrap = null;
		for (BuilderFormController.Choice choice : field.choices()) {
			if (choice.enabled() && choice.label().equals("Add TRAP")) addTrap = choice.value();
		}
		assertNotNull(addTrap);

		String before = canonical(session);
		controller.dispatchValue(capacity.id().value(), "entity_types", addTrap);
		assertTrue(session.state().draft().capacities().get(0).entityTypes().contains(EntityType.DEVICE));
		assertTrue(session.state().draft().capacities().get(0).entityTypes().contains(EntityType.TRAP));
		String after = canonical(session);
		controller.dispatch(new BuilderCommand.Undo());
		assertEquals(before, canonical(session));
		controller.dispatch(new BuilderCommand.Redo());
		assertEquals(after, canonical(session));
	}

	@Test public void unsupportedNestedListAndRuntimeFieldsAreDisabledNotFakeActions() {
		PlayerBuildSession session = completeSession("p02-r1-deferred");
		BuilderFormController controller = new BuilderFormController(session);
		EntitySpec entity = session.state().draft().entities().get(0);
		assertDisabled(controller.form(entity.id().value()).requireField("facets"), FormFieldSchema.Kind.NESTED_VARIANT);
		assertDisabled(controller.form(entity.id().value()).requireField("capabilities"), FormFieldSchema.Kind.LIST);
		assertFalse(controller.form(session.state().draft().recipes().get(0).id().value())
				.requireField("output_variant").enabled());
		assertFalse(controller.form(session.state().draft().classComponents().get(0).id().value())
				.requireField("variant_key").enabled());
	}

	private static PlayerBuildSession completeSession(String seed) {
		PlayerBuildSession session = PlayerBuildSession.empty(new DeterministicIdGenerator(seed));
		session.dispatch(new BuilderCommand.CreateResource("Rage"));
		session.dispatch(new BuilderCommand.CreateMark("Scorch"));
		session.dispatch(new BuilderCommand.CreateModeGroup("Stance A"));
		session.dispatch(new BuilderCommand.CreateModeGroup("Stance B"));
		ModeGroupSpec group = session.state().draft().modeGroups().get(0);
		session.dispatch(new BuilderCommand.CreateMode("Guard",new ModeGroupRef(group.id(),group.displayName().text())));
		session.dispatch(new BuilderCommand.CreateEntityCapacity("Device capacity","DEVICE",2,"REJECT_NEW"));
		session.dispatch(new BuilderCommand.CreateEntityCapacity("Shared capacity","DEVICE,TRAP",2,"REJECT_NEW"));
		session.dispatch(new BuilderCommand.CreateEntity("Turret","DEVICE",null));
		session.dispatch(new BuilderCommand.CreateAbilityPool("Observed"));
		session.dispatch(new BuilderCommand.CreateProperty("Ember"));
		session.dispatch(new BuilderCommand.CreateRecipe("Forge Ember","P02_DEFERRED_OUTPUT"));
		session.dispatch(new BuilderCommand.CreateClassComponent("Internal shell","P02_DEFERRED_COMPONENT"));
		return session;
	}

	private String canonical(PlayerBuildSession session) { return codec.serialize(session.state().draft()); }
	private static List<String> enabledValues(BuilderFormController.FieldModel field) {
		List<String> result = new ArrayList<>();
		for (BuilderFormController.Choice choice : field.choices()) if (choice.enabled()) result.add(choice.value());
		return result;
	}
	private static void assertDisabled(BuilderFormController.FieldModel field, FormFieldSchema.Kind kind) {
		assertEquals(kind, field.schema().kind());
		assertFalse(field.enabled());
		assertFalse(field.disabledReason().isEmpty());
	}
	private static String shortId(String id) { return id.length() <= 12 ? id : id.substring(id.length() - 8); }
}
