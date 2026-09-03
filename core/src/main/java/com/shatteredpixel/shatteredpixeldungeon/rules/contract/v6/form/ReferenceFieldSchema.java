package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.RefKind;

public final class ReferenceFieldSchema extends AbstractFormFieldSchema {
	private final RefKind expectedKind;
	private final String filterKey;
	public ReferenceFieldSchema(String fieldKey, String labelKey, boolean required,
			RefKind expectedKind, String filterKey) {
		super(fieldKey, labelKey, required);
		if (expectedKind == null || filterKey == null || filterKey.isEmpty()) {
			throw new IllegalArgumentException("reference kind and filter are required");
		}
		this.expectedKind = expectedKind;
		this.filterKey = filterKey;
	}
	@Override public Kind kind() { return Kind.REFERENCE; }
	public RefKind expectedKind() { return expectedKind; }
	public String filterKey() { return filterKey; }
}
