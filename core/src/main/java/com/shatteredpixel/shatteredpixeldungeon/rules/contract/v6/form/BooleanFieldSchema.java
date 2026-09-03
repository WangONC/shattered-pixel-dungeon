package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

public final class BooleanFieldSchema extends AbstractFormFieldSchema {
	public BooleanFieldSchema(String fieldKey, String labelKey, boolean required) {
		super(fieldKey, labelKey, required);
	}
	@Override public Kind kind() { return Kind.BOOLEAN; }
}
