package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

public final class ReadOnlyDiagnosticFieldSchema extends AbstractFormFieldSchema {
	public ReadOnlyDiagnosticFieldSchema(String fieldKey, String labelKey) {
		super(fieldKey, labelKey, false);
	}
	@Override public Kind kind() { return Kind.READ_ONLY_DIAGNOSTIC; }
}
