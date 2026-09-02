package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.identity;

/** Path-addressed deterministic IDs for replayable v5 migration. */
public final class MigrationIdGenerator {
	private final String namespace;
	public MigrationIdGenerator(String namespace) {
		if (namespace == null || namespace.isEmpty()) throw new IllegalArgumentException("namespace is required");
		this.namespace = namespace;
	}
	public StableId idFor(String prefix, String oldPath) {
		if (oldPath == null || oldPath.isEmpty()) throw new IllegalArgumentException("old path is required");
		return StableId.of(prefix + "_" + DeterministicIdGenerator.digest(namespace + "\u0000" + oldPath));
	}
}
