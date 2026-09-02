package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity;

import java.security.SecureRandom;

public final class RandomIdGenerator implements IdGenerator {
	private final SecureRandom random;
	public RandomIdGenerator() { this(new SecureRandom()); }
	public RandomIdGenerator(SecureRandom random) { this.random = random; }

	@Override public synchronized StableId nextId(String prefix) {
		byte[] bytes = new byte[16];
		random.nextBytes(bytes);
		return StableId.of(prefix + "_" + hex(bytes));
	}

	static String hex(byte[] bytes) {
		StringBuilder result = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
		return result.toString();
	}
}
