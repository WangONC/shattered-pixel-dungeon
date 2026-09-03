package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

public final class ListFieldSchema extends AbstractFormFieldSchema {
	private final FormFieldSchema elementSchema;
	private final int maximumEntries;
	public ListFieldSchema(String fieldKey, String labelKey, boolean required,
			FormFieldSchema elementSchema, int maximumEntries) {
		super(fieldKey, labelKey, required);
		if (elementSchema == null || maximumEntries < 0) throw new IllegalArgumentException("invalid list schema");
		this.elementSchema = elementSchema;
		this.maximumEntries = maximumEntries;
	}
	@Override public Kind kind() { return Kind.LIST; }
	public FormFieldSchema elementSchema() { return elementSchema; }
	public int maximumEntries() { return maximumEntries; }
}
