package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.migration;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
public final class MigrationResult {
	private final ClassBuildSpec build;private final ClassRuntimeState runtime;private final MigrationReport report;
	public MigrationResult(ClassBuildSpec build,ClassRuntimeState runtime,MigrationReport report){if(build==null||runtime==null||report==null)throw new IllegalArgumentException("migration result fields are required");this.build=build;this.runtime=runtime;this.report=report;}
	public ClassBuildSpec build(){return build;}public ClassRuntimeState runtime(){return runtime;}public MigrationReport report(){return report;}
}
