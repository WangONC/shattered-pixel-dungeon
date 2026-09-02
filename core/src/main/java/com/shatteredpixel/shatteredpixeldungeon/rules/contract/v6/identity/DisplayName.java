package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity;

import java.text.Normalizer;

/** Validated player-authored text. It is never used as a message key or identity. */
public final class DisplayName {
	private final String text;
	private DisplayName(String text) { this.text = text; }

	public static DisplayName of(String input) {
		if (input == null) throw new IllegalArgumentException("display name is required");
		String normalized = Normalizer.normalize(input, Normalizer.Form.NFC).trim();
		int count = normalized.codePointCount(0, normalized.length());
		if (count < 1 || count > 24) throw new IllegalArgumentException("display name must contain 1..24 code points");
		for (int offset = 0; offset < normalized.length();) {
			int codePoint = normalized.codePointAt(offset);
			if (Character.isISOControl(codePoint)) throw new IllegalArgumentException("display name contains a control character");
			offset += Character.charCount(codePoint);
		}
		return new DisplayName(normalized);
	}

	public String text() { return text; }
	@Override public boolean equals(Object other) { return other instanceof DisplayName && text.equals(((DisplayName)other).text); }
	@Override public int hashCode() { return text.hashCode(); }
	@Override public String toString() { return text; }
}
