package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.RuleAbility;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.rules.BasicAttackProfile;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBudgetPolicy;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassBuild;
import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerBuildAssembler;
import com.shatteredpixel.shatteredpixeldungeon.rules.PlayerFacingBuildValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceEngine;
import com.shatteredpixel.shatteredpixeldungeon.rules.ResourceSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleCost;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleEvent;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleModifier;
import com.shatteredpixel.shatteredpixeldungeon.rules.RuleRuntime;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.TargetingSpec;
import com.shatteredpixel.shatteredpixeldungeon.ui.ClassActionBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.ClassActionExecutor;
import com.shatteredpixel.shatteredpixeldungeon.ui.ClassActionIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.ClassResourceHUD;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Player-path contract for class-level components.  Builds are assembled exclusively through the
 * same operations accepted by WndCreateClass, then compiled into the real RuleRuntime.
 */
public final class PlayerClassCoreAudit {
	public static final class Result {
		public String schema = "player-class-core-1";
		public boolean passed;
		public int checks, failures, playerTextMissing;
		public int foundationBudget, gunnerUsedBudget, gunnerFullAttackCost;
		public float weakBasicAttackMultiplier;
		public String resourceHud = "";
		public final ArrayList<String> gunnerBuilderPath = new ArrayList<>();
		public final ArrayList<String> gunnerHudActions = new ArrayList<>();
		public final ArrayList<String> gunnerHudLabels = new ArrayList<>();
		public final ArrayList<String> findings = new ArrayList<>();
	}

	private PlayerClassCoreAudit() {}

	public static Result run() {
		Result result = new Result();
		result.foundationBudget = ClassBudgetPolicy.newBuildBudget();
		result.gunnerBuilderPath.add("Basic Attack = WEAK");
		result.gunnerBuilderPath.add("Add Resource: id=ammo, name=弹药, capacity=6, initial=6");
		result.gunnerBuilderPath.add("Resource Refill = ACTIVE, amount=FULL, actionTime=1");
		result.gunnerBuilderPath.add("Add Skill: 穿甲射击 / Damage / Projectile / Selected Enemy / Range 6 / Pierce / Cost 1 Ammo");

		ResourceSpec ammo = PlayerBuildAssembler.preset(ResourceEngine.MANUAL, "ammo", "弹药");
		ammo.capacity = 6;
		ammo.initialValue = 6;
		SkillSpec shot = gunnerShot();
		PlayerBuildAssembler assembler = new PlayerBuildAssembler("火线游击")
				.baseBudget(ClassBudgetPolicy.newBuildBudget())
				.basicAttack(BasicAttackProfile.WEAK)
				.addResource(ammo)
				.activeRefill("ammo", 0, 1f)
				.addSkill(shot);
		ClassBuild gunner = assembler.build();
		result.gunnerUsedBudget = gunner.usedBudget();
		check(result, assembler.failures().isEmpty(), "GUNNER_BUILDER_PATH:" + assembler.failures());
		check(result, PlayerFacingBuildValidator.classIssues(gunner).isEmpty(),
				"GUNNER_VALIDATION:" + PlayerFacingBuildValidator.classIssues(gunner));
		check(result, gunner.operation("reload_ammo") != null, "RELOAD_OPERATION_MISSING");
		check(result, !hasSkillToken(gunner, "reload") && !hasSkillToken(gunner, "refill"), "RELOAD_IS_FAKE_SKILL");
		result.gunnerHudActions.addAll(ClassActionBar.actionIds(gunner));
		Messages.setup(Languages.CHI_SMPL);
		result.gunnerHudLabels.addAll(ClassActionBar.actionLabels(gunner));
		Messages.setup(Languages.ENGLISH);
		check(result, result.gunnerHudActions.contains("skill:piercing_shot"), "PIERCING_SHOT_HUD_MISSING");
		check(result, result.gunnerHudActions.contains("operation:reload_ammo"), "RELOAD_HUD_MISSING");
		check(result, result.gunnerHudActions.size() == 2, "GUNNER_HUD_NOT_TWO_INDEPENDENT_ACTIONS:" + result.gunnerHudActions);
		check(result, result.gunnerHudLabels.size() == 2 && "穿甲射击".equals(result.gunnerHudLabels.get(0))
				&& "装填".equals(result.gunnerHudLabels.get(1)), "GUNNER_HUD_LABELS_WRONG:" + result.gunnerHudLabels);
		check(result, RuleAbility.MIGRATION_ONLY && new RuleAbility().actions(new Hero()).isEmpty(), "LEGACY_BOOK_STILL_ACTIONABLE");
		check(result, ClassActionIcon.assetForSkill(shot) != ClassActionIcon.assetForOperation(gunner.operation("reload_ammo")),
				"SKILL_AND_RELOAD_SHARE_ICON");

		Hero hero = new Hero();
		hero.HT = hero.HP = 20;
		RuleRuntime runtime = new RuleRuntime(gunner);
		hero.setRuleRuntime(runtime);
		runtime.setResourceForDebug(hero, "ammo", ResourceEngine.MANUAL, 2);
		result.resourceHud = ClassResourceHUD.readout(runtime);
		check(result, result.resourceHud.contains("弹药 2/6"), "AMMO_HUD_WRONG:" + result.resourceHud);
		check(result, ClassOperationRuntime.execute(hero, runtime.classOperation("reload_ammo"), hero.pos), "RELOAD_RUNTIME_FAILED");
		check(result, runtime.resourceValue("ammo", ResourceEngine.MANUAL) == 6, "RELOAD_DID_NOT_FILL_AMMO");
		check(result, runtime.classOperation("reload_ammo").actionTime == 1f, "RELOAD_ACTION_TIME_NOT_ONE_TURN");
		check(result, ClassActionExecutor.usesVisibleProjectile(runtime.activeTechnique("piercing_shot")),
				"PROJECTILE_VISUAL_ROUTE_MISSING");
		check(result, roundtrip(gunner), "GUNNER_SAVE_LOAD_MISMATCH");

		ClassBuild fullHybrid = gunner.copy();
		fullHybrid.gameplayComponent("basic_attack").basicAttack = BasicAttackProfile.FULL;
		result.gunnerFullAttackCost = fullHybrid.basicAttackProfile().budgetCost(fullHybrid);
		result.weakBasicAttackMultiplier = BasicAttackProfile.WEAK.damageMultiplier();
		check(result, result.gunnerFullAttackCost > 0, "FULL_BASIC_ATTACK_NOT_DYNAMICALLY_CHARGED");
		ClassBuild martial = new ClassBuild();
		martial.gameplayComponent("basic_attack").basicAttack = BasicAttackProfile.FULL;
		check(result, martial.basicAttackProfile().budgetCost(martial) == 0, "MARTIAL_PRIMARY_ATTACK_WRONGLY_CHARGED");
		check(result, BasicAttackProfile.WEAK.damageMultiplier() < 1f && BasicAttackProfile.FULL.damageMultiplier() == 1f,
				"BASIC_ATTACK_MULTIPLIERS_INVALID");
		check(result, new Hero().ruleRuntime() == null, "VANILLA_HERO_HAS_CUSTOM_RUNTIME");
		checkPlayerText(result, gunner, Languages.ENGLISH, "EN");
		checkPlayerText(result, gunner, Languages.CHI_SMPL, "ZH");
		Messages.setup(Languages.ENGLISH);

		PlayerBuildAssembler summonAssembler = new PlayerBuildAssembler("唤兽者")
				.ownership(true).entityCapacity(3).command(true);
		ClassBuild summon = summonAssembler.build();
		check(result, summonAssembler.failures().isEmpty() && summon.operation("command") != null, "COMMAND_OPERATION_MISSING");
		check(result, !hasSkillToken(summon, "command"), "COMMAND_IS_FAKE_SKILL");

		PlayerBuildAssembler modeAssembler = new PlayerBuildAssembler("双态行者")
				.modeEngine("offense", "defense");
		ClassBuild modes = modeAssembler.build();
		check(result, modeAssembler.failures().isEmpty() && modes.operation("mode_switch") != null, "MODE_OPERATION_MISSING");
		check(result, !hasSkillToken(modes, "mode"), "MODE_SWITCH_IS_FAKE_SKILL");

		ResourceSpec scrap = PlayerBuildAssembler.preset(ResourceEngine.MANUAL, "scrap", "零件");
		PlayerBuildAssembler engineerAssembler = new PlayerBuildAssembler("装置师")
				.addResource(scrap).ownership(true).deviceCapacity(2).recycle("scrap", 1);
		ClassBuild engineer = engineerAssembler.build();
		check(result, engineerAssembler.failures().isEmpty() && engineer.operation("recycle") != null, "RECYCLE_OPERATION_MISSING");
		check(result, !hasSkillToken(engineer, "recycle"), "RECYCLE_IS_FAKE_SKILL");

		PlayerArchetypeReconstructionAudit.Result archetypes = PlayerArchetypeReconstructionAudit.run();
		check(result, archetypes.passed && archetypes.archetypes == 10
				&& archetypes.qaBuildNotPlayerConstructible == 0 && archetypes.runtimeFailures == 0,
				"ARCHETYPE_PLAYER_RECONSTRUCTION_FAILED");
		result.passed = result.failures == 0;
		return result;
	}

	private static SkillSpec gunnerShot() {
		SkillSpec shot = new SkillSpec();
		shot.id = "piercing_shot";
		shot.name = "穿甲射击";
		shot.activation = RuleEvent.ACTIVE;
		shot.primary = new EffectSpec(EffectFamily.DAMAGE, EffectSpec.Operation.DAMAGE_STANDARD, 6);
		shot.delivery = SkillDelivery.PROJECTILE;
		shot.targeting.selector = TargetingSpec.Selector.SELECTED_ACTOR;
		shot.targeting.filter = TargetingSpec.Filter.ENEMY;
		shot.targeting.range = 6;
		shot.modifier = new RuleModifier(RuleModifier.Type.PIERCE);
		shot.cost = new RuleCost(RuleCost.Type.RESOURCE, 1);
		shot.cost.resourceEngine = ResourceEngine.MANUAL;
		shot.cost.resourceId = "ammo";
		return shot;
	}

	private static void check(Result result, boolean value, String finding) {
		result.checks++;
		if (!value) { result.failures++; result.findings.add(finding); }
	}

	private static void checkPlayerText(Result result, ClassBuild build, Languages language, String prefix) {
		Messages.setup(language);
		for (BasicAttackProfile profile : BasicAttackProfile.values()) {
			text(result, prefix + "_BASIC_NAME_" + profile, profile.displayName());
			text(result, prefix + "_BASIC_DESC_" + profile, profile.description());
		}
		for (com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec.Type type
				: com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec.Type.values()) {
			com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec operation =
					new com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec(type,
							"text_" + type.name().toLowerCase());
			if (type == com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec.Type.RELOAD)
				operation.resourceId = "ammo";
			text(result, prefix + "_OP_NAME_" + type, operation.displayName(build));
			text(result, prefix + "_OP_DESC_" + type, operation.description(build));
		}
		for (String key : new String[]{"missing_skill", "missing_operation", "skill_unavailable",
				"operation_unavailable", "select_target", "reload_feedback"})
			text(result, prefix + "_ACTION_" + key, Messages.get(ClassActionExecutor.class, key));
	}

	private static void text(Result result, String id, String value) {
		boolean present = value != null && !value.trim().isEmpty() && !value.contains(Messages.NO_TEXT_FOUND);
		if (!present) result.playerTextMissing++;
		check(result, present, "PLAYER_TEXT_MISSING:" + id);
	}

	private static boolean hasSkillToken(ClassBuild build, String token) {
		for (SkillSpec skill : build.skills) if (skill.id != null && skill.id.toLowerCase().contains(token)) return true;
		return false;
	}

	private static boolean roundtrip(ClassBuild build) {
		try {
			Bundle data = new Bundle();
			build.storeInBundle(data);
			ClassBuild restored = new ClassBuild();
			restored.restoreFromBundle(data);
			return RuleBuild.from(build).fingerprint().equals(RuleBuild.from(restored).fingerprint())
					&& "弹药".equals(restored.resource("ammo").displayName())
					&& "穿甲射击".equals(restored.skills.get(0).name)
					&& restored.operation("reload_ammo") != null;
		} catch (Throwable error) {
			return false;
		}
	}
}
