package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Reproducible generator for tests and command replay. */
public final class DeterministicIdGenerator implements IdGenerator {
	private final String seed;
	private long counter;

	public DeterministicIdGenerator(String seed) {
		if (seed == null) throw new IllegalArgumentException("seed is required");
		this.seed = seed;
	}

	@Override public synchronized StableId nextId(String prefix) {
		return StableId.of(prefix + "_" + digest(seed + "\u0000" + prefix + "\u0000" + counter++));
	}

	static String digest(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			byte[] first = new byte[16];
			System.arraycopy(hash, 0, first, 0, first.length);
			return RandomIdGenerator.hex(first);
		} catch (NoSuchAlgorithmException impossible) {
			throw new IllegalStateException(impossible);
		}
	}
}
