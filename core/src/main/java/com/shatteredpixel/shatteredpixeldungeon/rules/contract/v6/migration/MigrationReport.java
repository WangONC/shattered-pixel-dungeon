package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.migration;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import java.util.ArrayList;import java.util.Collections;import java.util.List;

public final class MigrationReport {
	public static final class Change {
		private final String path,kind,detail;
		public Change(String path,String kind,String detail){if(path==null||kind==null||detail==null)throw new IllegalArgumentException("migration change fields are required");this.path=path;this.kind=kind;this.detail=detail;}
		public String path(){return path;}public String kind(){return kind;}public String detail(){return detail;}
	}
	public static final class Warning {
		private final String path,code,detail;
		public Warning(String path,String code,String detail){if(path==null||code==null||detail==null)throw new IllegalArgumentException("migration warning fields are required");this.path=path;this.code=code;this.detail=detail;}
		public String path(){return path;}public String code(){return code;}public String detail(){return detail;}
	}
	private final int sourceSchema,targetSchema;private final List<Change> changes;private final List<DependencyDiagnostic> unresolved;
	private final List<Warning> warnings;private final boolean behaviorPreserved;
	public MigrationReport(int sourceSchema,int targetSchema,List<Change> changes,List<DependencyDiagnostic> unresolved,List<Warning> warnings,boolean behaviorPreserved){this.sourceSchema=sourceSchema;this.targetSchema=targetSchema;this.changes=Collections.unmodifiableList(new ArrayList<>(changes));this.unresolved=Collections.unmodifiableList(new ArrayList<>(unresolved));this.warnings=Collections.unmodifiableList(new ArrayList<>(warnings));this.behaviorPreserved=behaviorPreserved;}
	public int sourceSchema(){return sourceSchema;}public int targetSchema(){return targetSchema;}public List<Change> changes(){return changes;}
	public List<DependencyDiagnostic> unresolved(){return unresolved;}public List<Warning> warnings(){return warnings;}public boolean behaviorPreserved(){return behaviorPreserved;}
}
