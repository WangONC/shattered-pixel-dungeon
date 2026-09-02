package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;

import java.util.ArrayList;

/** Versioned, deterministic, JSON-friendly gameplay scenario. */
public class QaScenario {
	public static final String SCHEMA = "ruleqa-scenario-1";

	public String schema = SCHEMA;
	public String id;
	public long seed;
	public HeroSpec hero = new HeroSpec();
	public LevelSpec level = new LevelSpec();
	public final ArrayList<Action> actions = new ArrayList<>();
	public final ArrayList<String> expectedFindings = new ArrayList<>();
	public boolean staticOnly;

	public static class HeroSpec {
		public String preset;
		public String qaRuntimePreset;
		public CustomClassConfig build;
		public ClassBuild classBuild;
		public int hp = 100;
		public int maxHp = 100;
	}

	public static class LevelSpec {
		public String type = "fixed_test_level";
		public int width = 7;
		public int height = 7;
		public int heroCell = 24;
		public int[] waterCells = new int[0];
	}

	public static class Action {
		public enum Type {
			WAIT, MOVE, ATTACK, USE_ACTIVE_RULE, USE_CLASS_OPERATION,
			SET_HP, SET_RESOURCE, APPLY_STATUS, CLEAR_STATUS,
			SPAWN_MOB, SPAWN_ITEM, SET_TILE, SAVE_RELOAD
		}

		public Type type;
		public int repeat = 1;
		public int cell = -1;
		public int value;
		public int targetId = -1;
		public String status;
		public String mob;
		public String item;
		public String tile;
		public String resourceId;
		public String skillId;
		public String operationId;

		public static Action of(Type type) {
			Action action = new Action();
			action.type = type;
			return action;
		}

		public Action repeat(int value) { repeat = value; return this; }
		public Action cell(int value) { cell = value; return this; }
		public Action value(int amount) { value = amount; return this; }
		public Action target(int actorId) { targetId = actorId; return this; }
		public Action status(String name) { status = name; return this; }
		public Action mob(String name) { mob = name; return this; }
		public Action item(String name) { item = name; return this; }
		public Action tile(String name) { tile = name; return this; }
		public Action resource(String id) { resourceId = id; return this; }
		public Action skill(String id) { skillId = id; return this; }
		public Action operation(String id) { operationId = id; return this; }
	}

	public String toJson() {
		Json json = new Json(JsonWriter.OutputType.json);
		json.setUsePrototypes(false);
		return json.prettyPrint(this);
	}

	public static QaScenario fromJson(String value) {
		Json json = new Json();
		QaScenario scenario = json.fromJson(QaScenario.class, value);
		if (scenario == null || !SCHEMA.equals(scenario.schema)) {
			throw new IllegalArgumentException("unsupported scenario schema");
		}
		return scenario;
	}
}
