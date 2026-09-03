package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NestedVariantFieldSchema extends AbstractFormFieldSchema {
	private final List<String> allowedVariantKeys;
	public NestedVariantFieldSchema(String fieldKey, String labelKey, boolean required, List<String> allowedVariantKeys) {
		super(fieldKey, labelKey, required);
		if (allowedVariantKeys == null) throw new IllegalArgumentException("variant keys are required");
		this.allowedVariantKeys = Collections.unmodifiableList(new ArrayList<>(allowedVariantKeys));
	}
	@Override public Kind kind() { return Kind.NESTED_VARIANT; }
	public List<String> allowedVariantKeys() { return allowedVariantKeys; }
}
