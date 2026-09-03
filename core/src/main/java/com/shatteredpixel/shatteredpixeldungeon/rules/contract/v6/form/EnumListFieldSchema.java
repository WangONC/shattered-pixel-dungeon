package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EnumListFieldSchema implements FormFieldSchema {

	private final String fieldKey;
	private final String labelKey;
	private final boolean required;
	private final List<String> optionKeys;
	private final int minimumSelections;

	public EnumListFieldSchema(String fieldKey, String labelKey, boolean required,
			List<String> optionKeys, int minimumSelections) {
		if (minimumSelections < 0) {
			throw new IllegalArgumentException("minimumSelections must be non-negative");
		}
		this.fieldKey = fieldKey;
		this.labelKey = labelKey;
		this.required = required;
		this.optionKeys = Collections.unmodifiableList(new ArrayList<>(optionKeys));
		this.minimumSelections = minimumSelections;
	}

	@Override
	public String fieldKey() {
		return fieldKey;
	}

	@Override
	public String labelKey() {
		return labelKey;
	}

	@Override
	public Kind kind() {
		return Kind.ENUM_LIST;
	}

	@Override
	public boolean required() {
		return required;
	}

	public List<String> optionKeys() {
		return optionKeys;
	}

	public int minimumSelections() {
		return minimumSelections;
	}
}
