package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.IdGenerator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/** Deterministic, UTF-8-safe command trace. The payload never embeds a completed spec. */
public final class BuilderCommandTrace {
	public static final String FORMAT = "SPD_GC_V6_BUILDER_TRACE_1";
	private final List<BuilderCommand> commands;

	public BuilderCommandTrace(List<BuilderCommand> commands) {
		if (commands == null) throw new IllegalArgumentException("commands are required");
		this.commands = Collections.unmodifiableList(new ArrayList<>(commands));
	}
	public List<BuilderCommand> commands() { return commands; }

	public String serialize() {
		StringBuilder out = new StringBuilder(FORMAT).append('\n');
		for (BuilderCommand command : commands) {
			out.append(command.typeKey());
			for (String argument : command.traceArguments()) out.append('\t').append(encode(argument));
			out.append('\n');
		}
		return out.toString();
	}

	public static BuilderCommandTrace deserialize(String raw) {
		if (raw == null) throw new IllegalArgumentException("trace is required");
		String[] lines = raw.replace("\r", "").split("\n", -1);
		if (lines.length == 0 || !FORMAT.equals(lines[0])) throw new IllegalArgumentException("unsupported builder trace format");
		List<BuilderCommand> commands = new ArrayList<>();
		for (int i = 1; i < lines.length; i++) {
			if (lines[i].isEmpty()) continue;
			String[] encoded = lines[i].split("\t", -1);
			String[] args = new String[encoded.length - 1];
			for (int j = 1; j < encoded.length; j++) args[j - 1] = decode(encoded[j]);
			commands.add(parse(encoded[0], args));
		}
		return new BuilderCommandTrace(commands);
	}

	public PlayerBuildSession replay(IdGenerator ids) {
		PlayerBuildSession result = PlayerBuildSession.empty(ids);
		for (BuilderCommand command : commands) result.dispatch(command);
		return result;
	}

	private static BuilderCommand parse(String type, String[] a) {
		switch (type) {
			case "CreateResource": count(type, a, 1); return new BuilderCommand.CreateResource(a[0]);
			case "CreateMark": count(type, a, 1); return new BuilderCommand.CreateMark(a[0]);
			case "CreateModeGroup": count(type, a, 1); return new BuilderCommand.CreateModeGroup(a[0]);
			case "CreateAbilityPool": count(type, a, 1); return new BuilderCommand.CreateAbilityPool(a[0]);
			case "CreateProperty": count(type, a, 1); return new BuilderCommand.CreateProperty(a[0]);
			case "CreateRecipe": count(type, a, 2); return new BuilderCommand.CreateRecipe(a[0], a[1]);
			case "CreateMode": count(type, a, 4); return new BuilderCommand.CreateMode(a[0], ref(a, 1));
			case "CreateEntityCapacity": count(type, a, 4); return new BuilderCommand.CreateEntityCapacity(a[0], a[1], integer(a[2]), a[3]);
			case "CreateEntity":
				count(type, a, 5); return new BuilderCommand.CreateEntity(a[0], a[1], a[2].isEmpty() ? null : ref(a, 2));
			case "CreateClassComponent": count(type, a, 2); return new BuilderCommand.CreateClassComponent(a[0], a[1]);
			case "CreateClassConstraint": count(type, a, 2); return new BuilderCommand.CreateClassConstraint(a[0], a[1]);
			case "CreateClassOperation": count(type, a, 2); return new BuilderCommand.CreateClassOperation(a[0], a[1]);
			case "CreateSkill": count(type, a, 2); return new BuilderCommand.CreateSkill(a[0], a[1]);
			case "SetFieldValue": count(type, a, 4); return new BuilderCommand.SetFieldValue(a[0], a[1], a[2], a[3]);
			case "EditResourceField": count(type, a, 3); return new BuilderCommand.EditResourceField(a[0], a[1], a[2]);
			case "EditMarkField": count(type, a, 3); return new BuilderCommand.EditMarkField(a[0], a[1], a[2]);
			case "SetReference": count(type, a, 6); return new BuilderCommand.SetReference(a[0], a[1], a[2], ref(a, 3));
			case "RenameDeclaration": count(type, a, 2); return new BuilderCommand.RenameDeclaration(a[0], a[1]);
			case "DeleteDeclaration": count(type, a, 1); return new BuilderCommand.DeleteDeclaration(a[0]);
			case "RebindReference": count(type, a, 5); return new BuilderCommand.RebindReference(a[0], a[1], ref(a, 2));
			case "SaveDraft": count(type, a, 1); return new BuilderCommand.SaveDraft(a[0]);
			case "LoadDraft": count(type, a, 1); return new BuilderCommand.LoadDraft(a[0]);
			case "Navigate": count(type, a, 3); return new BuilderCommand.Navigate(a[0], a[1], a[2]);
			case "FinalizeBuild": count(type, a, 0); return new BuilderCommand.FinalizeBuild();
			case "Undo": count(type, a, 0); return new BuilderCommand.Undo();
			case "Redo": count(type, a, 0); return new BuilderCommand.Redo();
			default: throw new IllegalArgumentException("unknown builder command " + type);
		}
	}

	private static TypedRef ref(String[] args, int offset) {
		RefKind kind;
		try { kind = RefKind.valueOf(args[offset]); }
		catch (RuntimeException error) { throw new IllegalArgumentException("invalid trace ref kind " + args[offset], error); }
		com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId id =
				com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity.StableId.fromStored(args[offset + 1]);
		String name = args[offset + 2];
		switch (kind) {
			case RESOURCE: return new ResourceRef(id, name);
			case MARK: return new MarkRef(id, name);
			case MODE_GROUP: return new ModeGroupRef(id, name);
			case MODE: return new ModeRef(id, name);
			case ENTITY: return new EntitySpecRef(id, name);
			case CAPACITY: return new CapacityRef(id, name);
			case COMPONENT: return new ComponentRef(id, name);
			case ABILITY_POOL: return new AbilityPoolRef(id, name);
			case PROPERTY: return new PropertyRef(id, name);
			case RECIPE: return new SynthesisRecipeRef(id, name);
			default: throw new IllegalArgumentException("unsupported trace ref kind " + kind);
		}
	}
	private static void count(String type, String[] args, int expected) {
		if (args.length != expected) throw new IllegalArgumentException(type + " expected " + expected + " arguments, got " + args.length);
	}
	private static int integer(String value) {
		try { return Integer.parseInt(value); }
		catch (NumberFormatException error) { throw new IllegalArgumentException("trace integer is invalid: " + value, error); }
	}
	private static String encode(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}
	private static String decode(String value) {
		try {
			byte[] decoded = Base64.getUrlDecoder().decode(value);
			return new String(decoded, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException error) {
			throw new IllegalArgumentException("malformed builder trace argument", error);
		}
	}
}
