package com.shatteredpixel.shatteredpixeldungeon.rules;

/** A small composable part of a rule. Modules are intentionally Java data objects, not scripts. */
public interface RuleModule {
	int capacityCost();
	String description();
}
