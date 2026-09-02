package com.shatteredpixel.shatteredpixeldungeon.qa;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/** Small deterministic Level using the real Level arrays and occupancy rules. */
public class QaFixedLevel extends Level {
	public QaFixedLevel() {}

	public QaFixedLevel(int width, int height) {
		mobs = new HashSet<>();
		heaps = new SparseArray<>();
		blobs = new HashMap<>();
		plants = new SparseArray<>();
		traps = new SparseArray<>();
		customTiles = new ArrayList<>();
		customWalls = new ArrayList<>();
		transitions = new ArrayList<>();
		setSize(Math.max(5, width), Math.max(5, height));
		Arrays.fill(map, Terrain.WALL);
		Arrays.fill(solid, true);
		Arrays.fill(losBlocking, true);
		Arrays.fill(passable, false);
		Arrays.fill(avoid, false);
		Arrays.fill(pit, false);
		Arrays.fill(water, false);
		Arrays.fill(openSpace, false);
		for (int y = 1; y < height() - 1; y++) {
			for (int x = 1; x < width() - 1; x++) setQaTerrain(x + y * width(), Terrain.EMPTY);
		}
		entrance = width() + 1;
		exit = length() - width() - 2;
		// Real Hero/Mob paths consult this array when combat causes visibility updates.
		// Generated levels populate it during level construction; this fixed level must do
		// the same instead of relying on renderer/fog initialization.
		cleanWalls();
	}

	public void setQaTerrain(int cell, int terrain) {
		if (cell < 0 || cell >= length()) return;
		map[cell] = terrain;
		boolean wall = terrain == Terrain.WALL || terrain == Terrain.WALL_DECO;
		solid[cell] = wall;
		losBlocking[cell] = wall;
		passable[cell] = !wall;
		openSpace[cell] = !wall;
		water[cell] = terrain == Terrain.WATER;
		pit[cell] = terrain == Terrain.CHASM;
	}

	@Override protected boolean build() { return true; }
	@Override protected void createMobs() {}
	@Override protected void createItems() {}
}
