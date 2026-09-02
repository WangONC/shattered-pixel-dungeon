package com.shatteredpixel.shatteredpixeldungeon.rules;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

/** Conservative boundary around the world mutations actually supported by SPD Level data. */
public final class WorldCapabilityValidator {
	private WorldCapabilityValidator() {}

	public static boolean supports(Level level, int cell, WorldCapability capability) {
		return canAlterTerrain(level, cell, capability);
	}

	public static boolean canAlterTerrain(Level level, int cell, WorldCapability capability) {
		if (level == null || cell < 0 || cell >= level.length() || capability == null) return false;
		int terrain = level.map[cell];
		int x = cell % level.width();
		int y = cell / level.width();
		boolean boundary = x == 0 || y == 0 || x == level.width()-1 || y == level.height()-1;
		boolean protectedStructure = terrain == Terrain.ENTRANCE || terrain == Terrain.EXIT
				|| terrain == Terrain.LOCKED_DOOR || terrain == Terrain.LOCKED_EXIT
				|| cell == level.entrance || cell == level.exit;
		if (protectedStructure) return false;
		switch (capability) {
			case BLOB_SEEDABLE: return !level.solid[cell];
			case REPLACEABLE: return !level.solid[cell] && terrain != Terrain.CHASM;
			case PLANTABLE: return !level.solid[cell] && terrain != Terrain.CHASM && !level.water[cell];
			case TRAP_PLACEABLE: return !level.solid[cell] && terrain != Terrain.CHASM;
			case HAZARD_CLEARABLE: return !level.solid[cell];
			case DESTRUCTIBLE:
				if (boundary || !(terrain == Terrain.WALL || terrain == Terrain.WALL_DECO
						|| (Terrain.flags[terrain] & Terrain.FLAMABLE) != 0)) return false;
				// Only expose an internal wall face; isolated protected masses remain intact.
				for (int dy=-1; dy<=1; dy++) for (int dx=-1; dx<=1; dx++) {
					if (dx==0 && dy==0) continue;
					int next=cell+dx+dy*level.width();
					if (next>=0 && next<level.length() && !level.solid[next]) return true;
				}
				return false;
			default: return false;
		}
	}
}
