package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency;
import java.util.ArrayList;import java.util.Collections;import java.util.List;
public final class DependencyReport {
	private final List<DependencyDiagnostic> diagnostics;
	public DependencyReport(List<DependencyDiagnostic> diagnostics){this.diagnostics=Collections.unmodifiableList(new ArrayList<>(diagnostics));}
	public List<DependencyDiagnostic> diagnostics(){return diagnostics;}
	public boolean has(DependencyState state){for(DependencyDiagnostic value:diagnostics)if(value.state()==state)return true;return false;}
	public boolean finalizationAllowed(){return !has(DependencyState.UNRESOLVED)&&!has(DependencyState.HARD_CONFLICT)&&!has(DependencyState.UNSUPPORTED);}
}
