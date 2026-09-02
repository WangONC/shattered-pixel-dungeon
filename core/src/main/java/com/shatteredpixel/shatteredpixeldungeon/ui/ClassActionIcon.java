package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.rules.ClassOperationSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.EffectFamily;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillDelivery;
import com.shatteredpixel.shatteredpixeldungeon.rules.SkillSpec;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Image;

/** Deterministic reuse of existing SPD assets; no skill falls back to the shared book icon. */
public final class ClassActionIcon {
	private ClassActionIcon() {}
	public static Image skill(SkillSpec skill) {
		return new ItemSprite(assetForSkill(skill));
	}
	public static int assetForSkill(SkillSpec skill) {
		int image = ItemSpriteSheet.WAND_MAGIC_MISSILE;
		if (skill != null) {
			if (skill.delivery == SkillDelivery.PROJECTILE) image = ItemSpriteSheet.DART;
			else if (skill.primary != null && skill.primary.family == EffectFamily.RECOVERY_DEFENSE) image = ItemSpriteSheet.HEALING_DART;
			else if (skill.primary != null && skill.primary.family == EffectFamily.CREATE_ENTITY) image = ItemSpriteSheet.ARTIFACT_HORN1;
			else if (skill.primary != null && skill.primary.family == EffectFamily.WORLD_TERRAIN) image = ItemSpriteSheet.TRAP_MECHANISM;
			else if (skill.primary != null && skill.primary.family == EffectFamily.MOVEMENT) image = ItemSpriteSheet.SCROLL_RAIDO;
		}
		return image;
	}
	public static Image operation(ClassOperationSpec operation) {
		return new ItemSprite(assetForOperation(operation));
	}
	public static int assetForOperation(ClassOperationSpec operation) {
		int image = ItemSpriteSheet.BEACON;
		if (operation != null) switch (operation.type) {
			case RELOAD: image = ItemSpriteSheet.SCROLL_NAUDIZ; break;
			case COMMAND: image = ItemSpriteSheet.ARTIFACT_HORN1; break;
			case MODE_SWITCH: image = ItemSpriteSheet.SCROLL_RAIDO; break;
			case RECYCLE: image = ItemSpriteSheet.RECLAIM_TRAP; break;
		}
		return image;
	}
}
