/**
 * Sole package boundary for future Legacy v5 to Contract v6 migration bridges.
 *
 * <p>P00-R1 defines the dependency boundary only. It contains no migration adapter and no
 * gameplay behavior. Production v6 code belongs under {@code rules.contract.v6}; only bridge
 * code placed in this package may depend on both legacy-v5 and contract-v6 symbols.</p>
 */
package com.shatteredpixel.shatteredpixeldungeon.rules.migration.v5;
