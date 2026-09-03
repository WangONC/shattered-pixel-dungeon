package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.builder.BuilderCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MultiEnumSelector {

	private final String ownerId;
	private final String variantKey;
	private final EnumListFieldSchema schema;
	private final LinkedHashSet<String> selected;

	public MultiEnumSelector(String ownerId, String variantKey, EnumListFieldSchema schema,
			Collection<String> selected) {
		this.ownerId = ownerId;
		this.variantKey = variantKey;
		this.schema = schema;
		this.selected = new LinkedHashSet<>(selected);
	}

	public Set<String> selected() {
		return new LinkedHashSet<>(selected);
	}

	public List<String> options() {
		return schema.optionKeys();
	}

	public BuilderCommand toggle(String optionKey) {
		if (!schema.optionKeys().contains(optionKey)) {
			throw new IllegalArgumentException("Unknown enum-list option: " + optionKey);
		}
		LinkedHashSet<String> next = new LinkedHashSet<>(selected);
		if (!next.add(optionKey)) {
			if (next.size() <= schema.minimumSelections()) {
				throw new IllegalArgumentException("At least " + schema.minimumSelections()
						+ " selection(s) are required");
			}
			next.remove(optionKey);
		}
		return new BuilderCommand.SetFieldValue(ownerId, variantKey, schema.fieldKey(), join(next));
	}

	private static String join(Collection<String> values) {
		return String.join(",", new ArrayList<>(values));
	}
}
