package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;

import java.util.List;

public final class EnumSelector {
	private final EnumFieldSchema schema;
	public EnumSelector(EnumFieldSchema schema) {
		if (schema == null) throw new IllegalArgumentException("enum schema is required");
		this.schema = schema;
	}
	public List<String> options() { return schema.optionKeys(); }
	public BuilderCommand.SetFieldValue command(String ownerId, String variantKey, String optionKey) {
		if (!schema.contains(optionKey)) throw new IllegalArgumentException("enum option is not in schema: " + optionKey);
		return new BuilderCommand.SetFieldValue(ownerId, variantKey, schema.fieldKey(), optionKey);
	}
}
