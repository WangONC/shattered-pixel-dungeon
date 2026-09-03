package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

abstract class AbstractFormFieldSchema implements FormFieldSchema {
	private final String fieldKey;
	private final String labelKey;
	private final boolean required;

	AbstractFormFieldSchema(String fieldKey, String labelKey, boolean required) {
		if (fieldKey == null || fieldKey.isEmpty() || labelKey == null || labelKey.isEmpty()) {
			throw new IllegalArgumentException("form field key and label key are required");
		}
		this.fieldKey = fieldKey;
		this.labelKey = labelKey;
		this.required = required;
	}

	@Override public final String fieldKey() { return fieldKey; }
	@Override public final String labelKey() { return labelKey; }
	@Override public final boolean required() { return required; }
}
