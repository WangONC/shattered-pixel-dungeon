/*
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * The narrow presentation boundary used by rule effects. A live scene receives the normal SPD
 * feedback; gameplay-only callers with no attached scene get a no-op without changing semantics.
 */
public final class RulePresentation {
	private RulePresentation() {}

	private static boolean available() {
		return Dungeon.hero != null && Dungeon.hero.sprite != null && Dungeon.hero.sprite.parent != null;
	}

	public static void burst(int cell, int count, boolean centered) {
		if (!available()) return;
		if (centered) CellEmitter.center(cell).burst(Speck.factory(Speck.STAR), count);
		else CellEmitter.get(cell).burst(Speck.factory(Speck.STAR), count);
	}

	public static void blast(int cell) {
		if (available()) WandOfBlastWave.BlastWave.blast(cell);
	}

	public static void burningSound() {
		if (available()) Sample.INSTANCE.play(Assets.Sounds.BURNING);
	}

	public static void blastSound() {
		if (available()) Sample.INSTANCE.play(Assets.Sounds.BLAST);
	}

	public static void triggered(RuleContext context, RuleEffect effect) {
		if (!available()) return;
		if (context.hero.sprite != null) {
			context.hero.sprite.showStatus(CharSprite.POSITIVE, effect.description());
		}
		GLog.p(Messages.get(RuleEffect.class, "triggered", effect.description()));
	}
}
