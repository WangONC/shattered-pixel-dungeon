package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

/**
 * MIGRATION_ONLY shell for deserializing V0.2 saves which contained the old shared spellbook.
 *
 * New ClassBuilds never create this item. RuleRuntime removes a restored instance and every
 * player action is exposed by ClassActionBar, so keeping the serialized class name cannot bring
 * the obsolete "one book switches every skill" interaction back into the game.
 */
@Deprecated
public class RuleAbility extends Item {
	public static final boolean MIGRATION_ONLY = true;

	{
		image = ItemSpriteSheet.ARTIFACT_SPELLBOOK;
		defaultAction = null;
		unique = true;
		bones = false;
		usesTargeting = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		return new ArrayList<>();
	}

	@Override
	public boolean isIdentified() { return true; }

	@Override
	public boolean isUpgradable() { return false; }

	@Override
	public int value() { return 0; }
}
