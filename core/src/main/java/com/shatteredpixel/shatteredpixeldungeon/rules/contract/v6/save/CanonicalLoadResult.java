package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import java.util.ArrayList;import java.util.Collections;import java.util.List;

public final class CanonicalLoadResult<T> {
	private final DependencyState state;private final T value;private final List<String> diagnostics;
	private CanonicalLoadResult(DependencyState state,T value,List<String> diagnostics){this.state=state;this.value=value;this.diagnostics=Collections.unmodifiableList(new ArrayList<>(diagnostics));}
	public static<T>CanonicalLoadResult<T> resolved(T value){return new CanonicalLoadResult<>(DependencyState.RESOLVED,value,Collections.emptyList());}
	public static<T>CanonicalLoadResult<T> failed(DependencyState state,String diagnostic){return new CanonicalLoadResult<>(state,null,Collections.singletonList(diagnostic));}
	public DependencyState state(){return state;}public T value(){return value;}public List<String> diagnostics(){return diagnostics;}
}
