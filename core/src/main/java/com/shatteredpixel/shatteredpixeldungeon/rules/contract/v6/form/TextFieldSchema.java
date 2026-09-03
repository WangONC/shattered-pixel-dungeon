package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

public final class TextFieldSchema extends AbstractFormFieldSchema {
	private final int maximumCodePoints;
	public TextFieldSchema(String fieldKey, String labelKey, boolean required, int maximumCodePoints) {
		super(fieldKey, labelKey, required);
		if (maximumCodePoints < 1) throw new IllegalArgumentException("text maximum must be positive");
		this.maximumCodePoints = maximumCodePoints;
	}
	@Override public Kind kind() { return Kind.TEXT; }
	public int maximumCodePoints() { return maximumCodePoints; }
}
