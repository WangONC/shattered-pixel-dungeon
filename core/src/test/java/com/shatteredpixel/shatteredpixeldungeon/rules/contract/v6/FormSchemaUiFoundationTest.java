package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form.*;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DeterministicIdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.ModeGroupRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ModeGroupSpec;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FormSchemaUiFoundationTest {
	@Test public void everyP02DeclarationHasTypedSchemaAndComplexFieldKinds() {
		assertEquals(10,V6FormSchemas.all().size());
		for(String key:Arrays.asList("RESOURCE","MARK","MODE_GROUP","MODE","ENTITY_CAPACITY","ENTITY","ABILITY_POOL","PROPERTY","RECIPE","CONTRACT_NODE"))assertFalse(V6FormSchemas.require(key).fields().isEmpty());
		assertTrue(V6FormSchemas.require("RESOURCE").requireField("maximum") instanceof NumberFieldSchema);
		assertTrue(V6FormSchemas.require("MARK").requireField("kind") instanceof EnumFieldSchema);
		assertTrue(V6FormSchemas.require("MODE").requireField("group") instanceof ReferenceFieldSchema);
		assertTrue(V6FormSchemas.require("ENTITY_CAPACITY").requireField("entity_types") instanceof EnumListFieldSchema);
		assertTrue(V6FormSchemas.require("ENTITY").requireField("facets") instanceof NestedVariantFieldSchema);
		assertTrue(V6FormSchemas.require("RECIPE").requireField("inputs") instanceof ListFieldSchema);
		assertTrue(V6FormSchemas.require("PROPERTY").requireField("diagnostics") instanceof ReadOnlyDiagnosticFieldSchema);
	}

	@Test public void numberStepperClampsAndEmitsOneScalarCommandWithoutFlatRange() {
		NumberFieldSchema schema=new NumberFieldSchema("maximum","v6.maximum",true,0,100,5);
		NumberStepper top=new NumberStepper(schema,100);assertEquals(100,top.increment());assertEquals(95,top.decrement());assertEquals(100,top.accelerated(1,50));
		BuilderCommand.SetFieldValue command=top.command("res_id","RESOURCE",-1,3);
		assertEquals("85",command.value());assertEquals("maximum",command.fieldKey());
	}

	@Test public void missingReferenceRemainsAsErrorCardWithLastKnownNameAndShortId() {
		DeterministicIdGenerator ids=new DeterministicIdGenerator("p02-picker");
		ClassBuildSpec build=ClassBuildSpec.builder(ids.nextId("build"),com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.DisplayName.of("Picker")).build();
		com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId missing=ids.nextId("modegrp");
		ReferencePicker picker=ReferencePicker.from(build,(ReferenceFieldSchema)V6FormSchemas.require("MODE").requireField("group"),new ModeGroupRef(missing,"旧姿态"));
		assertEquals(1,picker.options().size());assertTrue(picker.options().get(0).unresolved());assertEquals("旧姿态",picker.options().get(0).displayName());assertFalse(picker.options().get(0).shortId().isEmpty());
	}

	@Test public void realWindowAdapterOnlyDispatchesCommands() throws Exception {
		Path windows=repoRoot().resolve("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows");
		String entry=new String(Files.readAllBytes(windows.resolve("WndCreateClassV6.java")),StandardCharsets.UTF_8);
		String code=new String(Files.readAllBytes(windows.resolve("WndCreateClassV6ControllerView.java")),StandardCharsets.UTF_8);
		assertTrue(entry.contains("WndCreateClassV6ControllerView.show"));
		assertTrue(code.contains("controller.dispatchValue("));assertTrue(code.contains("BuilderFormController"));
		assertFalse(code.contains("new ResourceSpec("));assertFalse(code.contains(".toBuilder()"));assertFalse(code.contains("resolvePendingBindings"));
		assertFalse(code.contains("P02_DEFERRED_COMPONENT"));assertFalse(code.contains("P02_DEFERRED_SKILL"));
	}
	private static Path repoRoot(){Path current=Paths.get("").toAbsolutePath().normalize();for(int i=0;i<10&&current!=null;i++,current=current.getParent())if(Files.isRegularFile(current.resolve("settings.gradle")))return current;throw new AssertionError("repository root not found");}
}
