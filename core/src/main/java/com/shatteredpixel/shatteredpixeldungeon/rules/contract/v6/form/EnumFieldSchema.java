package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EnumFieldSchema extends AbstractFormFieldSchema {
	private final List<String> optionKeys;
	public EnumFieldSchema(String fieldKey, String labelKey, boolean required, List<String> optionKeys) {
		super(fieldKey, labelKey, required);
		if (optionKeys == null || optionKeys.isEmpty()) throw new IllegalArgumentException("enum options are required");
		this.optionKeys = Collections.unmodifiableList(new ArrayList<>(optionKeys));
	}
	@Override public Kind kind() { return Kind.ENUM; }
	public List<String> optionKeys() { return optionKeys; }
	public boolean contains(String value) { return optionKeys.contains(value); }
}
