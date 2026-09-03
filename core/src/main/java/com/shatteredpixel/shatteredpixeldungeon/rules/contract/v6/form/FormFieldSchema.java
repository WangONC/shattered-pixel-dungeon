package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

/** Declarative UI metadata only. Runtime support is intentionally not represented here. */
public interface FormFieldSchema {
	enum Kind { TEXT, NUMBER, ENUM, ENUM_LIST, REFERENCE, BOOLEAN, NESTED_VARIANT, LIST, READ_ONLY_DIAGNOSTIC }
	String fieldKey();
	String labelKey();
	Kind kind();
	boolean required();
}
