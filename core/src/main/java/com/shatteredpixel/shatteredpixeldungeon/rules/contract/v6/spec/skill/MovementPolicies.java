package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.skill;
public final class MovementPolicies { private MovementPolicies(){} public enum CollisionPolicy{STOP_BEFORE_BLOCKED,SPD_NATIVE_COLLISION}public enum PathPolicy{REQUIRE_CLEAR_PATH,ALLOW_PASSABLE_PATH}public enum DestinationPolicy{EXACT_CELL_OR_FAIL,NEAREST_VALID_CELL}public enum SwapLegalityPolicy{BOTH_CELLS_PASSABLE} }
