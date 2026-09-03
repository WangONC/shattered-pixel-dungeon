package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.form;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.ref.TypedRef;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.StableTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Typed picker data. Missing current refs remain visible as explicit error cards. */
public final class ReferencePicker {
	public static final class Option {
		private final String targetId;
		private final String displayName;
		private final boolean unresolved;
		private Option(String targetId, String displayName, boolean unresolved) {
			this.targetId = targetId;
			this.displayName = displayName;
			this.unresolved = unresolved;
		}
		public String targetId() { return targetId; }
		public String displayName() { return displayName; }
		public boolean unresolved() { return unresolved; }
		public String shortId() { return targetId.length() <= 12 ? targetId : targetId.substring(targetId.length() - 8); }
	}

	private final List<Option> options;
	private ReferencePicker(List<Option> options) { this.options = Collections.unmodifiableList(options); }
	public List<Option> options() { return options; }

	public static ReferencePicker from(ClassBuildSpec build, ReferenceFieldSchema schema, TypedRef current) {
		if (build == null || schema == null) throw new IllegalArgumentException("build and reference schema are required");
		List<Option> result = new ArrayList<>();
		boolean currentFound = current == null;
		for (StableTarget target : build.allTargets()) {
			if (target.refKind() == schema.expectedKind()) {
				result.add(new Option(target.id().value(), target.displayName().text(), false));
				if (current != null && current.targetId().equals(target.id())) currentFound = true;
			}
		}
		if (!currentFound) result.add(0, new Option(current.targetId().value(), current.lastKnownDisplayName(), true));
		return new ReferencePicker(result);
	}
}
