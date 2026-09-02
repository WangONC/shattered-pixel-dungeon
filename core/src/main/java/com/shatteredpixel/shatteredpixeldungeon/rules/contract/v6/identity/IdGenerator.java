package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity;

public interface IdGenerator {
	StableId nextId(String prefix);
}
