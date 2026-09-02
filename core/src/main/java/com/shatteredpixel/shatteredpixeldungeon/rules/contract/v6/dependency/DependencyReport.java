package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency;
import java.util.ArrayList;import java.util.Collections;import java.util.List;
public final class DependencyReport {
	private final List<DependencyDiagnostic> diagnostics;
	public DependencyReport(List<DependencyDiagnostic> diagnostics){this.diagnostics=Collections.unmodifiableList(new ArrayList<>(diagnostics));}
	public List<DependencyDiagnostic> diagnostics(){return diagnostics;}
	public boolean has(DependencyState state){for(DependencyDiagnostic value:diagnostics)if(value.state()==state)return true;return false;}
	public DependencyState aggregateState(){if(has(DependencyState.HARD_CONFLICT))return DependencyState.HARD_CONFLICT;if(has(DependencyState.UNRESOLVED))return DependencyState.UNRESOLVED;if(has(DependencyState.UNSUPPORTED))return DependencyState.UNSUPPORTED;return DependencyState.RESOLVED;}
	public boolean finalizationAllowed(){return !has(DependencyState.UNRESOLVED)&&!has(DependencyState.HARD_CONFLICT)&&!has(DependencyState.UNSUPPORTED);}
}
