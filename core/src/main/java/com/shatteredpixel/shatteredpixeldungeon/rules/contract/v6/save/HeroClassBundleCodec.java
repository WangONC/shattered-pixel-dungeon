package com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.save;

import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.ClassBuildValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyDiagnostic;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyReport;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyResolver;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.DependencyState;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.dependency.RuntimeStateValidator;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.spec.ClassBuildSpec;
import com.shatteredpixel.shatteredpixeldungeon.rules.contract.v6.state.ClassRuntimeState;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Authoritative, read-only v6 Hero save/load pipeline. */
public final class HeroClassBundleCodec {
	public static final String CLASS_BUILD_SPEC = "class_build_spec_v6";
	public static final String CLASS_RUNTIME_STATE = "class_runtime_state_v6";

	private final CanonicalBuildCodec builds = new CanonicalBuildCodec();
	private final CanonicalRuntimeCodec runtime = new CanonicalRuntimeCodec();

	public void store(Bundle heroBundle, ClassBuildSpec build, ClassRuntimeState state) {
		if (heroBundle == null) throw new IllegalArgumentException("hero bundle is required");
		Payloads payloads = payloads(build, state);
		heroBundle.put(CLASS_BUILD_SPEC, payloads.build);
		heroBundle.put(CLASS_RUNTIME_STATE, payloads.runtime);
	}

	public void store(V6PayloadHost host, ClassBuildSpec build, ClassRuntimeState state) {
		if (host == null) throw new IllegalArgumentException("payload host is required");
		Payloads payloads = payloads(build, state);
		host.setGameplayComponentsV6Payloads(payloads.build(), payloads.runtime());
	}

	public LoadPair load(Bundle heroBundle) {
		if (heroBundle == null || !heroBundle.contains(CLASS_BUILD_SPEC)
				|| !heroBundle.contains(CLASS_RUNTIME_STATE)) return missing();
		return loadPayloads(heroBundle.getString(CLASS_BUILD_SPEC), heroBundle.getString(CLASS_RUNTIME_STATE));
	}

	public LoadPair load(V6PayloadHost host) {
		if (host == null) return missing();
		return loadPayloads(host.gameplayComponentsV6BuildPayload(),
				host.gameplayComponentsV6RuntimePayload());
	}

	public Payloads payloads(ClassBuildSpec build, ClassRuntimeState state) {
		if (build == null || state == null) throw new IllegalArgumentException("build and state are required");
		if (!build.buildId().equals(state.buildId())) throw new IllegalArgumentException("runtime build id mismatch");
		return new Payloads(builds.serialize(build), runtime.serialize(state));
	}

	public LoadPair loadPayloads(String buildPayload, String runtimePayload) {
		if (buildPayload == null || runtimePayload == null) return missing();
		CanonicalLoadResult<ClassBuildSpec> buildLoad = builds.deserialize(buildPayload);
		if (buildLoad.value() == null) return new LoadPair(buildLoad.state(), null, null,
				new DependencyReport(Collections.<DependencyDiagnostic>emptyList()), buildLoad.diagnostics().toString());

		ClassBuildSpec build = buildLoad.value();
		List<DependencyDiagnostic> diagnostics = new ArrayList<>();
		diagnostics.addAll(new DependencyResolver().resolve(build).diagnostics());
		diagnostics.addAll(new ClassBuildValidator().validate(build).diagnostics());

		CanonicalLoadResult<ClassRuntimeState> runtimeLoad = runtime.deserialize(runtimePayload);
		if (runtimeLoad.value() == null) {
			DependencyReport report = new DependencyReport(diagnostics);
			return new LoadPair(worst(runtimeLoad.state(), report.aggregateState()), build, null, report,
					runtimeLoad.diagnostics().toString());
		}

		ClassRuntimeState state = runtimeLoad.value();
		diagnostics.addAll(new RuntimeStateValidator().validate(build, state).diagnostics());
		DependencyReport report = new DependencyReport(diagnostics);
		DependencyState loadState = worst(worst(buildLoad.state(), runtimeLoad.state()), report.aggregateState());
		return new LoadPair(loadState, build, state, report, "");
	}

	private static LoadPair missing() {
		return new LoadPair(DependencyState.UNRESOLVED, null, null,
				new DependencyReport(Collections.<DependencyDiagnostic>emptyList()), "v6 hero bundle payload missing");
	}

	private static DependencyState worst(DependencyState left, DependencyState right) {
		return severity(left) >= severity(right) ? left : right;
	}

	private static int severity(DependencyState state) {
		switch (state) {
			case HARD_CONFLICT: return 3;
			case UNRESOLVED: return 2;
			case UNSUPPORTED: return 1;
			case RESOLVED: return 0;
			default: throw new AssertionError(state);
		}
	}

	public static final class Payloads {
		private final String build;
		private final String runtime;
		private Payloads(String build, String runtime) { this.build = build; this.runtime = runtime; }
		public String build() { return build; }
		public String runtime() { return runtime; }
	}

	public static final class LoadPair {
		private final DependencyState state;
		private final ClassBuildSpec build;
		private final ClassRuntimeState runtime;
		private final DependencyReport report;
		private final String diagnostic;
		private LoadPair(DependencyState state, ClassBuildSpec build, ClassRuntimeState runtime,
				DependencyReport report, String diagnostic) {
			this.state = state;
			this.build = build;
			this.runtime = runtime;
			this.report = report;
			this.diagnostic = diagnostic;
		}
		public DependencyState state() { return state; }
		public ClassBuildSpec build() { return build; }
		public ClassRuntimeState runtime() { return runtime; }
		public DependencyReport report() { return report; }
		public String diagnostic() { return diagnostic; }
	}
}
