package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormSchema {
	private final String variantKey;
	private final List<FormFieldSchema> fields;
	private final Map<String, FormFieldSchema> byKey;
	public FormSchema(String variantKey, List<FormFieldSchema> fields) {
		if (variantKey == null || variantKey.isEmpty() || fields == null) {
			throw new IllegalArgumentException("form variant and fields are required");
		}
		this.variantKey = variantKey;
		this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
		Map<String, FormFieldSchema> index = new LinkedHashMap<>();
		for (FormFieldSchema field : fields) {
			if (index.put(field.fieldKey(), field) != null) {
				throw new IllegalArgumentException("duplicate form field " + field.fieldKey());
			}
		}
		this.byKey = Collections.unmodifiableMap(index);
	}
	public String variantKey() { return variantKey; }
	public List<FormFieldSchema> fields() { return fields; }
	public FormFieldSchema field(String fieldKey) { return byKey.get(fieldKey); }
	public FormFieldSchema requireField(String fieldKey) {
		FormFieldSchema result = field(fieldKey);
		if (result == null) throw new IllegalArgumentException("field is not in schema: " + variantKey + "." + fieldKey);
		return result;
	}
}
