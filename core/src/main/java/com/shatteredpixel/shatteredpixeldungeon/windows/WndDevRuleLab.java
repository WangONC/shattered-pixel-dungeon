package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RuleResourceBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Slime;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Thief;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.qa.ArchetypeReferenceBuilds;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleDefinition;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEffect;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleTestBuilds;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.Reflection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/** Debug-build-only rule and gameplay test console. The only entry is guarded in WndGame. */
public class WndDevRuleLab extends WndLocalizedOptions {

	public WndDevRuleLab() {
		super(msg("main_title"), msg("main_desc"), msg("main_resources"), msg("main_rules"),
				msg("main_status"), msg("main_mobs"), msg("main_items"), msg("main_travel"),
				msg("main_tools"), msg("main_presets"), msg("main_archetypes"), msg("close"));
	}

	private static String msg(String key, Object... args) {
		return Messages.get(WndDevRuleLab.class, key, args);
	}

	@Override
	protected void onSelect(int index) {
		switch (index) {
			case 0: resources(); break;
			case 1: rules(); break;
			case 2: status(); break;
			case 3: mobs(); break;
			case 4: items(); break;
			case 5: travelAndSave(); break;
			case 6: tools(); break;
			case 7: presets(); break;
			case 8: referenceBuilds(); break;
			default: break;
		}
	}

	private static Hero hero() {
		return Dungeon.hero;
	}

	private static RuleRuntime runtime() {
		if (hero().ruleRuntime() == null) RuleRuntime.installDebug(hero());
		return hero().ruleRuntime();
	}

	private static void show(Window window) {
		GameScene.show(window);
	}

	private static void reopen() {
		show(new WndDevRuleLab());
	}

	private static void resources() {
		String runtimeState = hero().ruleRuntime() == null ? msg("no_runtime_yet")
				: hero().ruleRuntime().stateDescription();
		show(new WndLocalizedOptions(msg("resources_title"),
				msg("resources_state", hero().HP, hero().HT, runtimeState),
				msg("set_hp_one"), msg("set_hp_half"), msg("heal_full"),
				msg("choose_engine"), msg("resource_plus"), msg("resource_minus")) {
			@Override
			protected void onSelect(int index) {
				RuleRuntime runtime = runtime();
				switch (index) {
					case 0: hero().HP = 1; break;
					case 1: hero().HP = Math.max(1, hero().HT / 2); break;
					case 2: hero().HP = hero().HT; break;
					case 3: chooseEngine(); return;
					case 4: runtime.changeResource(hero(), 1, true); break;
					case 5: runtime.changeResource(hero(), -1, true); break;
				}
				reopen();
			}
		});
	}

	private static void chooseEngine() {
		final ResourceEngine[] values = ResourceEngine.values();
		String[] names = new String[values.length];
		for (int i = 0; i < values.length; i++) names[i] = values[i].displayName();
		show(new WndLocalizedOptions(msg("choose_engine_title"), msg("choose_engine_desc"), names) {
			@Override protected void onSelect(int index) {
				RuleRuntime runtime = runtime();
				runtime.setEngineForDebug(hero(), values[index]);
				if (values[index] != ResourceEngine.BLOOD) runtime.setResourceForDebug(hero(), runtime.maxResource());
				reopen();
			}
		});
	}

	private static void presets() {
		final RuleTestBuilds.Preset[] values = RuleTestBuilds.Preset.values();
		String[] names = new String[values.length];
		for (int i = 0; i < values.length; i++) names[i] = RuleTestBuilds.displayName(values[i]);
		show(new WndLocalizedOptions(msg("presets_title"), msg("presets_desc"), names) {
			@Override protected void onSelect(int index) {
				com.shatteredpixel.shatteredpixeldungeon.rules.CustomClassConfig config = RuleTestBuilds.config(values[index]);
				config.name = RuleTestBuilds.displayName(values[index]);
				RuleRuntime.installDebug(hero(), config);
				GLog.p(msg("preset_loaded", RuleTestBuilds.displayName(values[index])));
				reopen();
			}
		});
	}

	private static void referenceBuilds() {
		final ArrayList<ArchetypeReferenceBuilds.Id> values = ArchetypeReferenceBuilds.allIds();
		String[] names = new String[values.size()];
		for (int i = 0; i < values.size(); i++) names[i] = ArchetypeReferenceBuilds.displayName(values.get(i));
		show(new WndLocalizedOptions(msg("archetypes_title"), msg("archetypes_desc"), names) {
			@Override protected void onSelect(int index) {
				ArchetypeReferenceBuilds.Id id = values.get(index);
				ClassBuild build = ArchetypeReferenceBuilds.build(id);
				build.name = ArchetypeReferenceBuilds.displayName(id);
				CustomClassConfig config = CustomClassConfig.fromClassBuild(build);
				RuleRuntime.installDebug(hero(), config);
				GLog.p(msg("archetype_loaded", build.name));
				reopen();
			}
		});
	}

	private static void rules() {
		String state = hero().ruleRuntime() == null ? msg("rules_no_runtime")
				: hero().ruleRuntime().stateDescription();
		show(new WndLocalizedOptions(msg("rules_title"), state,
				msg("add_rage_push"), msg("add_blood_fire"), msg("add_hit_poison"),
				msg("delete_rule"), msg("view_runtime")) {
			@Override
			protected void onSelect(int index) {
				RuleRuntime runtime = runtime();
				if (index == 0) {
					runtime.setEngineForDebug(hero(), ResourceEngine.RAGE);
					runtime.setResourceForDebug(hero(), runtime.maxResource());
					runtime.addRule(RuleDefinition.ragePushOnDamaged(), true);
				} else if (index == 1) {
					runtime.addRule(RuleDefinition.bloodFireActive(), true);
				} else if (index == 2) {
					runtime.addRule(RuleDefinition.create(RuleEvent.ON_HIT, runtime.engine(), RuleEffect.Type.POISON), true);
				} else if (index == 3) {
					runtime.removeLastRule();
				} else {
					show(new WndMessage(runtime.stateDescription()));
					return;
				}
				runtime.ensureVisuals(hero());
				reopen();
			}
		});
	}

	private static void status() {
		show(new WndLocalizedOptions(msg("status_title"), msg("status_desc"),
				msg("apply_poison"), msg("apply_burning"), msg("clear_debuffs"), msg("clear_buffs")) {
			@Override
			protected void onSelect(int index) {
				if (index == 0) Buff.affect(hero(), Poison.class).set(8);
				else if (index == 1) Buff.affect(hero(), Burning.class).reignite(hero(), 8f);
				else {
					for (Buff buff : new ArrayList<>(hero().buffs())) {
						if (buff instanceof Hunger || buff instanceof Regeneration || buff instanceof RuleResourceBuff) continue;
						if (index == 3 || buff.type == Buff.buffType.NEGATIVE) buff.detach();
					}
				}
			reopen();
			}
		});
	}

	private static void mobs() {
		show(new WndLocalizedOptions(msg("mobs_title"), msg("mobs_desc"),
				msg("mob_rat"), msg("mob_snake"), msg("mob_slime"), msg("mob_thief")) {
			@Override
			protected void onSelect(int index) {
				Class<? extends Mob>[] classes = new Class[]{Rat.class, Snake.class, Slime.class, Thief.class};
				spawnMob(classes[index]);
				reopen();
			}
		});
	}

	private static void spawnMob(Class<? extends Mob> type) {
		Mob mob = Reflection.newInstance(type);
		int cell = Dungeon.level.randomRespawnCell(mob);
		if (cell == -1) {
			GLog.w(msg("no_spawn_cell"));
			return;
		}
		mob.pos = cell;
		GameScene.add(mob);
		Dungeon.observe();
		GLog.p(msg("spawned", mob.name()));
	}

	private static void items() {
		show(new WndLocalizedOptions(msg("items_title"), msg("items_desc"),
				msg("item_healing"), msg("item_liquid_flame"), msg("item_weapon"), msg("item_random")) {
			@Override
			protected void onSelect(int index) {
				Item item;
				if (index == 0) item = new PotionOfHealing();
				else if (index == 1) item = new PotionOfLiquidFlame();
				else if (index == 2) item = Generator.randomWeapon();
				else item = Generator.random();
				Dungeon.level.drop(item, hero().pos).sprite.drop(hero().pos);
				GLog.p(msg("spawned", item.name()));
				reopen();
			}
		});
	}

	private static void travelAndSave() {
		show(new WndLocalizedOptions(msg("travel_title"), msg("current_depth", Dungeon.depth),
				msg("save_now"), msg("save_reload"), msg("next_depth"),
				msg("jump_depth", 1), msg("jump_depth", 5), msg("jump_depth", 10),
				msg("jump_depth", 15), msg("jump_depth", 20), msg("jump_depth", 25)) {
			@Override
			protected void onSelect(int index) {
				if (index == 0) {
					save();
					reopen();
				} else if (index == 1) {
					if (save()) {
						Dungeon.hero = null;
						ActionIndicator.clearAction();
						InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
						ShatteredPixelDungeon.switchScene(InterlevelScene.class);
					}
				} else {
					int[] depths = {Dungeon.depth + 1, 1, 5, 10, 15, 20, 25};
					jump(depths[index - 2]);
				}
			}
		});
	}

	private static boolean save() {
		try {
			Dungeon.saveAll();
			GLog.p(msg("saved"));
			return true;
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
			GLog.w(msg("save_failed", e.getMessage()));
			return false;
		}
	}

	private static void jump(int depth) {
		InterlevelScene.returnDepth = Math.max(1, Math.min(25, depth));
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = -1;
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		Game.switchScene(InterlevelScene.class);
	}

	private static void tools() {
		show(new WndLocalizedOptions(msg("tools_title"), msg("tools_desc"),
				msg("heal_full"), msg("kill_mobs"), msg("reveal_map"),
				msg("show_config"), msg("show_runtime")) {
			@Override
			protected void onSelect(int index) {
				if (index == 0) {
					hero().HP = hero().HT;
				} else if (index == 1) {
					for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
						if (mob.alignment == Mob.Alignment.ENEMY) mob.damage(mob.HP + mob.shielding(), hero());
					}
				} else if (index == 2) {
					Arrays.fill(Dungeon.level.mapped, true);
					Arrays.fill(Dungeon.level.visited, true);
					GameScene.updateFog();
				} else if (index == 3) {
					show(new WndMessage(hero().ruleRuntime() == null ? msg("vanilla_config")
							: hero().ruleRuntime().configDescription()));
					return;
				} else {
					show(new WndMessage(hero().ruleRuntime() == null ? msg("no_runtime_attached")
							: hero().ruleRuntime().stateDescription()));
					return;
				}
				reopen();
			}
		});
	}
}
